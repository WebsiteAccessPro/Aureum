package com.example.aureum1.controller.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AlertDialog;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import com.example.aureum1.R;
import com.example.aureum1.controller.activities.Registro.RegistroGastoActivity;
import com.example.aureum1.controller.activities.Registro.RegistroIngresoActivity;
import com.example.aureum1.controller.activities.Registro.RegistroTransferenciaActivity;
import com.example.aureum1.controller.activities.Registro.SeleccionCategoriaActivity;
import com.example.aureum1.controller.activities.Registro.SeleccionCuentaActivity;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class AddRecordActivity extends AppCompatActivity {

    private TextView tvSigno, tvMonto, tvCuenta, tvCategoria, tvMoneda, tvMontoWarning;
    private Button btnIngreso, btnGasto, btnTransferencia;
    private ImageView btnNext;
    private TextView tvCuentaTitulo, tvCategoriaTitulo;

    // Variables de la calculadora
    private String input = "";
    private double lastValue = 0;
    private String currentOperator = "";
    private boolean isNewOperation = true;
    private String currentTab = "Ingreso";
    private String selectedCurrency = "PEN";
    // Selecciones independientes
    private String selectedCategoria = "";
    private String selectedCuentaDestino = "";
    // UID del destinatario externo (si aplica)
    private String selectedDestUid = "";

    // Cache de cuenta origen para validación en tiempo real
    private double saldoCuentaOrigenCache = 0.0;
    private String monedaCuentaOrigenCache = "PEN";

    private static final int REQ_SELECCION_CUENTA = 100;
    private static final int REQ_SELECCION_CATEGORIA = 200;
    private static final int REQ_SELECCION_CUENTA_DESTINO = 300;
    private static final int REQ_REG_INGRESO = 401;
    private static final int REQ_REG_GASTO = 402;
    private static final int REQ_REG_TRANSFER = 403;

    // Para recibir datos de RegistroXActivity
    private String nota = "";
    private String beneficiario = "";
    private String pagador = "";
    private String formaPago = "";
    private String estado = "";
    private String fecha = "";
    private String hora = "";
    private boolean registroCompletado = false;
    private boolean isSaving = false; // evita envíos duplicados

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_record);

        ImageView btnCheck = findViewById(R.id.btnCheck);
        btnCheck.setOnClickListener(v -> guardarRegistroEnFirebase());

        //Referencias UI
        tvSigno = findViewById(R.id.tvSigno);
        tvMonto = findViewById(R.id.tvMonto);
        tvMoneda = findViewById(R.id.tvMoneda);
        tvMontoWarning = findViewById(R.id.tvMontoWarning);
        btnIngreso = findViewById(R.id.btnIngreso);
        btnGasto = findViewById(R.id.btnGasto);
        btnTransferencia = findViewById(R.id.btnTransferencia);
        btnNext = findViewById(R.id.btnNext);
        tvCuenta = findViewById(R.id.tvCuenta);
        tvCategoria = findViewById(R.id.tvCategoria);
        tvCuentaTitulo = findViewById(R.id.tvCuentaTitulo);
        tvCategoriaTitulo = findViewById(R.id.tvCategoriaTitulo);

        // Estado inicial
        setSelectedTab(btnIngreso);
        tvSigno.setText("+");
        tvMonto.setText("0");
        tvCuentaTitulo.setText("Cuenta");
        tvCategoriaTitulo.setText("Categoría");

        // Configurar calculadora
        setupCalculator();

        // Moneda inicial y selector tocable
        tvMoneda.setText(selectedCurrency);
        tvMoneda.setOnClickListener(v -> {
            String[] opciones = new String[]{"PEN", "USD", "EUR"};
            new AlertDialog.Builder(this)
                    .setTitle("Moneda")
                    .setItems(opciones, (dialog, which) -> {
                        selectedCurrency = opciones[which];
                        tvMoneda.setText(selectedCurrency);
                        updateWarning();
                    })
                    .show();
        });

        // Tabs
        btnIngreso.setOnClickListener(v -> {
            setSelectedTab(btnIngreso);
            tvSigno.setText("+");
            currentTab = "Ingreso";
            tvCuentaTitulo.setText("Cuenta");
            tvCategoriaTitulo.setText("Categoría");
            // Mostrar selección de categoría (o placeholder) sin afectar selección de destino
            tvCategoria.setText(selectedCategoria == null || selectedCategoria.isEmpty() ? "Seleccionar" : selectedCategoria);
            updateWarning();
        });

        btnGasto.setOnClickListener(v -> {
            setSelectedTab(btnGasto);
            tvSigno.setText("−");
            currentTab = "Gasto";
            tvCuentaTitulo.setText("Cuenta");
            tvCategoriaTitulo.setText("Categoría");
            tvCategoria.setText(selectedCategoria == null || selectedCategoria.isEmpty() ? "Seleccionar" : selectedCategoria);
            updateWarning();
        });

        btnTransferencia.setOnClickListener(v -> {
            setSelectedTab(btnTransferencia);
            tvSigno.setText("↔");
            currentTab = "Transferencia";
            tvCuentaTitulo.setText("Cuenta origen");
            tvCategoriaTitulo.setText("Cuenta destino");
            // Mostrar selección de destino (o placeholder) independiente de categoría
            tvCategoria.setText(selectedCuentaDestino == null || selectedCuentaDestino.isEmpty() ? "Seleccionar" : selectedCuentaDestino);
            updateWarning();
        });

        // Redirección a registro según tipo
        btnNext.setOnClickListener(v -> {
            Intent intent;
            switch (currentTab) {
                case "Ingreso":
                    intent = new Intent(this, RegistroIngresoActivity.class);
                    break;
                case "Gasto":
                    intent = new Intent(this, RegistroGastoActivity.class);
                    break;
                default:
                    intent = new Intent(this, RegistroTransferenciaActivity.class);
                    break;
            }

            // Enviar datos ya ingresados
            intent.putExtra("MONTO", input);
            intent.putExtra("MONEDA", selectedCurrency);
            intent.putExtra("CUENTA", tvCuenta.getText().toString());
            intent.putExtra("CATEGORIA_O_DESTINO", tvCategoria.getText().toString());
            int req;
            switch (currentTab) {
                case "Ingreso": req = REQ_REG_INGRESO; break;
                case "Gasto": req = REQ_REG_GASTO; break;
                default: req = REQ_REG_TRANSFER; break;
            }
            startActivityForResult(intent, req);
            overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
        });

        // Selección cuenta / categoría
        tvCuenta.setOnClickListener(v -> {
            Intent i = new Intent(this, SeleccionCuentaActivity.class);
            startActivityForResult(i, REQ_SELECCION_CUENTA);
            overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
        });

        tvCategoria.setOnClickListener(v -> {
            if (currentTab.equals("Transferencia")) {
                Intent i = new Intent(this, com.example.aureum1.controller.activities.Registro.SeleccionCuentaExternaActivity.class);
                startActivityForResult(i, REQ_SELECCION_CUENTA_DESTINO);
            } else {
                Intent i = new Intent(this, SeleccionCategoriaActivity.class);
                i.putExtra("TIPO_CATEGORIA", currentTab);
                startActivityForResult(i, REQ_SELECCION_CATEGORIA);
            }
            overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
        });

        // Cerrar la pantalla con el botón de la barra superior
        findViewById(R.id.btnClose).setOnClickListener(v -> finish());

        // Inicializar valores por defecto: "Seleccionar" en categoría/destino y primera cuenta registrada
        inicializarDefaults();
    }

    // Establece por defecto la primera cuenta registrada y fija "Seleccionar" en categoría/destino
    private void inicializarDefaults() {
        // Siempre empezar con categoría/destino en "Seleccionar"
        tvCategoria.setText("Seleccionar");

        // Obtener usuario y cuentas para seleccionar la primera por defecto
        com.google.firebase.auth.FirebaseUser user = com.example.aureum1.Backend.FirebaseHelper.INSTANCE.getAuth().getCurrentUser();
        if (user == null) {
            tvCuenta.setText("Seleccionar");
            return;
        }

        com.google.firebase.firestore.FirebaseFirestore db = com.example.aureum1.Backend.FirebaseHelper.INSTANCE.getDb();
        db.collection("accounts").document(user.getUid()).get()
                .addOnSuccessListener(docSnap -> {
                    if (!docSnap.exists()) { tvCuenta.setText("Seleccionar"); return; }
                    java.util.List<java.util.Map<String, Object>> cuentas = (java.util.List<java.util.Map<String, Object>>) docSnap.get("cuentas");
                    if (cuentas != null && !cuentas.isEmpty()) {
                        Object nombreObj = cuentas.get(0).get("nombre");
                        String nombrePrimera = nombreObj != null ? nombreObj.toString() : null;
                        if (nombrePrimera != null && !nombrePrimera.isEmpty()) {
                            tvCuenta.setText(nombrePrimera);
                            actualizarMonedaPorCuenta(nombrePrimera);
                            return;
                        }
                    }
                    tvCuenta.setText("Seleccionar");
                })
                .addOnFailureListener(e -> tvCuenta.setText("Seleccionar"));
    }

    // ==================== Calculadora ====================
    private void setupCalculator() {
        Button[] numBtns = {
                findViewById(R.id.btn0), findViewById(R.id.btn1), findViewById(R.id.btn2),
                findViewById(R.id.btn3), findViewById(R.id.btn4), findViewById(R.id.btn5),
                findViewById(R.id.btn6), findViewById(R.id.btn7), findViewById(R.id.btn8),
                findViewById(R.id.btn9)
        };

        for (Button b : numBtns) {
            b.setOnClickListener(v -> appendNumber(b.getText().toString()));
        }

        findViewById(R.id.btnDot).setOnClickListener(v -> appendDot());
        findViewById(R.id.btnBack).setOnClickListener(v -> deleteLast());
        findViewById(R.id.btnPlus).setOnClickListener(v -> setOperator("+"));
        findViewById(R.id.btnMinus).setOnClickListener(v -> setOperator("−"));
        findViewById(R.id.btnMul).setOnClickListener(v -> setOperator("×"));
        findViewById(R.id.btnEqual).setOnClickListener(v -> calculateResult());
    }

    private void appendNumber(String num) {
        if (isNewOperation) { input = ""; isNewOperation = false; }
        if (input.equals("0") && !num.equals(".")) input = "";
        input += num;
        updateDisplay();
    }

    private void appendDot() {
        if (isNewOperation) { input = "0"; isNewOperation = false; }
        if (!input.contains(".")) { if (input.isEmpty()) input = "0"; input += "."; }
        updateDisplay();
    }

    private void deleteLast() {
        if (!input.isEmpty()) input = input.substring(0, input.length() - 1);
        if (input.isEmpty()) input = "0";
        updateDisplay();
    }

    private void setOperator(String op) {
        if (!input.isEmpty()) {
            try { lastValue = Double.parseDouble(input); } catch (NumberFormatException e) { lastValue = 0; }
            currentOperator = op;
            isNewOperation = true;
        }
    }

    private void calculateResult() {
        if (input.isEmpty() || currentOperator.isEmpty()) return;

        double secondValue;
        try { secondValue = Double.parseDouble(input); } catch (NumberFormatException e) { secondValue = 0; }

        double result = 0;
        switch (currentOperator) {
            case "+": result = lastValue + secondValue; break;
            case "−": result = lastValue - secondValue; break;
            case "×": result = lastValue * secondValue; break;
        }

        if (result % 1 == 0) input = String.valueOf((int) result);
        else input = String.format("%.2f", result);

        currentOperator = "";
        isNewOperation = true;
        updateDisplay();
    }

    private String formatNumberForDisplay(String raw) {
        if (raw == null || raw.isEmpty()) return "0";
        boolean endsWithDot = raw.endsWith(".");
        String[] parts = raw.split("\\.", -1);
        String intPart = parts[0].isEmpty() ? "0" : parts[0];
        String decPart = (parts.length > 1) ? parts[1] : null;
        intPart = intPart.replaceFirst("^0+(?!$)", "");
        if (intPart.isEmpty()) intPart = "0";

        String formattedInt;
        try {
            long intVal = Long.parseLong(intPart);
            NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);
            nf.setGroupingUsed(true);
            nf.setMaximumFractionDigits(0);
            formattedInt = nf.format(intVal);
        } catch (Exception e) { formattedInt = intPart; }

        if (endsWithDot) return formattedInt + ".";
        else if (decPart != null && !decPart.isEmpty()) return formattedInt + "." + decPart;
        else return formattedInt;
    }

    private void updateDisplay() {
        String toShow = input.isEmpty() ? "0" : formatNumberForDisplay(input);
        tvMonto.setText(toShow);

        // Ajuste dinámico basado en el ancho disponible para que el texto
        // se reduzca con números largos y vuelva a crecer al borrar.
        int availableWidth = tvMonto.getWidth();
        if (availableWidth <= 0) {
            // Si aún no está medido, postergar el cálculo hasta después del layout.
            tvMonto.post(this::updateDisplay);
            updateWarning();
            return;
        }

        float maxSp = 72f;
        float minSp = 24f;
        float stepSp = 2f;

        android.graphics.Paint paint = new android.graphics.Paint(tvMonto.getPaint());
        float scaledDensity = getResources().getDisplayMetrics().scaledDensity;

        float targetSp = maxSp;
        while (targetSp >= minSp) {
            paint.setTextSize(targetSp * scaledDensity);
            float textWidth = paint.measureText(toShow);
            if (textWidth <= availableWidth) {
                break;
            }
            targetSp -= stepSp;
        }
        tvMonto.setTextSize(targetSp);
        // Actualizar advertencia en tiempo real
        updateWarning();
    }

    // ==================== Advertencia en tiempo real ====================
    private void updateWarning() {
        if (tvMontoWarning == null) return;

        // Sólo aplica a Gasto y Transferencia
        if (!"Gasto".equals(currentTab) && !"Transferencia".equals(currentTab)) {
            tvMontoWarning.setVisibility(android.view.View.GONE);
            return;
        }

        // Necesitamos info de cuenta origen
        String cuentaOrigen = tvCuenta.getText().toString().trim();
        if (cuentaOrigen.isEmpty() || "Seleccionar".equalsIgnoreCase(cuentaOrigen)) {
            tvMontoWarning.setVisibility(android.view.View.GONE);
            return;
        }

        // Parsear monto actual
        double montoActual;
        try { montoActual = Double.parseDouble(input.isEmpty() ? "0" : input.replace(",", "")); } catch (Exception e) { montoActual = 0; }
        if (montoActual <= 0) {
            tvMontoWarning.setVisibility(android.view.View.GONE);
            return;
        }

        // Convertir a moneda de la cuenta origen
        double montoEnOrigen = convertirMoneda(montoActual, selectedCurrency, monedaCuentaOrigenCache);

        if (montoEnOrigen > saldoCuentaOrigenCache + 1e-9) {
            // Mostrar advertencia en rojo
            double disponibleReferencia = convertirMoneda(saldoCuentaOrigenCache, monedaCuentaOrigenCache, selectedCurrency);
            String mensaje = String.format(Locale.getDefault(),
                    "Tu saldo es insuficiente. Disponible ≈ %s %.2f",
                    selectedCurrency, disponibleReferencia);
            tvMontoWarning.setText(mensaje);
            tvMontoWarning.setVisibility(android.view.View.VISIBLE);
        } else {
            tvMontoWarning.setVisibility(android.view.View.GONE);
        }
    }

    private void setSelectedTab(Button selectedButton) {
        btnIngreso.setSelected(false);
        btnGasto.setSelected(false);
        btnTransferencia.setSelected(false);
        selectedButton.setSelected(true);
    }

    // ==================== Guardar Registro ====================
    private void guardarRegistroEnFirebase() {
        if (isSaving) { return; }
        String tipo = currentTab;
        String cuentaOrigen = tvCuenta.getText().toString().trim();
        // Usar selecciones independientes para validar y guardar
        String categoria = ("Transferencia".equals(tipo) ? selectedCuentaDestino : selectedCategoria);
        double monto;
        try { monto = Double.parseDouble(input.isEmpty() ? "0" : input.replace(",", "")); } catch (NumberFormatException e) { monto = 0; }

        // Para Transferencia
        String cuentaDestino = tipo.equals("Transferencia") ? categoria : null;

        String userId = com.example.aureum1.Backend.FirebaseHelper.INSTANCE
                .getAuth().getCurrentUser().getUid();

        final double montoFinal = monto;
        final String cuentaOrigenFinal = cuentaOrigen;
        final String cuentaDestinoFinal = cuentaDestino;
        final String tipoFinal = tipo;
        final String userIdFinal = userId;

        // Validaciones obligatorias
        if (monto <= 0) {
            Toast.makeText(this, "Ingresa un monto válido", Toast.LENGTH_SHORT).show();
            return;
        }
        // Cuenta origen no puede ser vacía ni "Seleccionar"
        if (cuentaOrigen.isEmpty() || "Seleccionar".equalsIgnoreCase(cuentaOrigen)) {
            Toast.makeText(this, "Selecciona una cuenta", Toast.LENGTH_SHORT).show();
            return;
        }
        // Categoría (o cuenta destino en transferencia) no puede ser vacía ni "Seleccionar"
        if (categoria.isEmpty() || "Seleccionar".equalsIgnoreCase(categoria)) {
            Toast.makeText(this, tipo.equals("Transferencia") ? "Selecciona la cuenta destino" : "Selecciona una categoría", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!registroCompletado) {
            Toast.makeText(this, "Completa los datos del registro antes de guardar", Toast.LENGTH_SHORT).show();
            return;
        }
        if (tipo.equals("Ingreso") && (pagador == null || pagador.trim().isEmpty())) {
            Toast.makeText(this, "Ingresa el pagador", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!tipo.equals("Ingreso") && (beneficiario == null || beneficiario.trim().isEmpty())) {
            Toast.makeText(this, "Ingresa el beneficiario", Toast.LENGTH_SHORT).show();
            return;
        }
        // Si es transferencia, validar saldo con conversión antes de guardar
        if ("Transferencia".equals(tipoFinal)) {
            validarYGuardarTransferencia(userIdFinal, cuentaOrigenFinal, cuentaDestinoFinal, montoFinal);
            return;
        }

        // Si es gasto, restringir si excede saldo disponible de la cuenta origen
        if ("Gasto".equals(tipoFinal)) {
            double montoEnOrigen = convertirMoneda(montoFinal, selectedCurrency, monedaCuentaOrigenCache);
            if (montoEnOrigen > saldoCuentaOrigenCache + 1e-9) {
                double disponibleReferencia = convertirMoneda(saldoCuentaOrigenCache, monedaCuentaOrigenCache, selectedCurrency);
                String mensaje = String.format(Locale.getDefault(),
                        "Tu saldo es insuficiente. Disponible ≈ %s %.2f",
                        selectedCurrency, disponibleReferencia);
                tvMontoWarning.setText(mensaje);
                tvMontoWarning.setVisibility(android.view.View.VISIBLE);
                return;
            }
        }

        // Flujo original para Ingreso/Gasto
        Map<String, Object> data = new HashMap<>();
        data.put("tipo", tipoFinal);
        data.put("monto", montoFinal);
        data.put("cuentaOrigen", cuentaOrigenFinal);
        data.put("cuentaDestino", cuentaDestinoFinal);
        data.put("nota", nota);
        if (tipoFinal.equals("Ingreso")) {
            data.put("pagador", pagador);
        } else {
            data.put("beneficiario", beneficiario);
        }
        data.put("fecha", fecha);
        data.put("hora", hora);
        data.put("formaPago", formaPago);
        data.put("estado", estado);
        data.put("moneda", selectedCurrency);
        data.put("categoria", categoria);
        data.put("createdAt", com.google.firebase.firestore.FieldValue.serverTimestamp());

        // Bloquear doble click mientras se guarda
        isSaving = true;
        btnNext.setEnabled(false);

        com.example.aureum1.Backend.FirebaseHelper.INSTANCE.addRegistro(
                userIdFinal,
                data,
                (success, error) -> {
                    if (success) {
                        // Actualizar cuentas según tipo
                        if (tipoFinal.equals("Ingreso")) {
                            procesarIngresoOEGasto(userIdFinal, cuentaOrigenFinal, montoFinal, true);
                        } else if (tipoFinal.equals("Gasto")) {
                            procesarIngresoOEGasto(userIdFinal, cuentaOrigenFinal, montoFinal, false);
                        }
                        runOnUiThread(() -> {
                            Toast.makeText(this, "Registro guardado correctamente", Toast.LENGTH_SHORT).show();
                            finish();
                        });
                    } else {
                        runOnUiThread(() -> Toast.makeText(this, "Error: " + error, Toast.LENGTH_SHORT).show());
                        // Rehabilitar si falla
                        isSaving = false;
                        btnNext.setEnabled(true);
                    }
                    return null;
                }
        );
    }

    // Validación de saldo y conversión para transferencias
    private void validarYGuardarTransferencia(String userId, String origenNombre, String destinoNombre, double monto) {
        com.google.firebase.firestore.FirebaseFirestore db =
                com.example.aureum1.Backend.FirebaseHelper.INSTANCE.getDb();

        db.collection("accounts").document(userId).get().addOnSuccessListener(docSnap -> {
            if (!docSnap.exists()) {
                Toast.makeText(this, "No se pudo validar las cuentas", Toast.LENGTH_SHORT).show();
                return;
            }

            java.util.List<Map<String, Object>> cuentas = (java.util.List<Map<String, Object>>) docSnap.get("cuentas");
            if (cuentas == null) {
                Toast.makeText(this, "No hay cuentas registradas", Toast.LENGTH_SHORT).show();
                return;
            }

            Map<String, Object> cuentaOrigenMap = null;
            Map<String, Object> cuentaDestinoMap = null;
            for (Map<String, Object> c : cuentas) {
                String nombre = (String) c.get("nombre");
                if (origenNombre.equals(nombre)) cuentaOrigenMap = c;
                if (destinoNombre != null && destinoNombre.equals(nombre)) cuentaDestinoMap = c;
            }

            if (cuentaOrigenMap == null) {
                Toast.makeText(this, "Cuenta origen no encontrada", Toast.LENGTH_SHORT).show();
                return;
            }
            // Si es destino interno (mismo usuario), debe existir en mis cuentas
            boolean destinoEsExterno = selectedDestUid != null && !selectedDestUid.isEmpty() && !selectedDestUid.equals(userId);
            if (!destinoEsExterno && cuentaDestinoMap == null) {
                Toast.makeText(this, "Cuenta destino no encontrada", Toast.LENGTH_SHORT).show();
                return;
            }

            String monedaOrigen = (String) cuentaOrigenMap.get("moneda");
            if (monedaOrigen == null) monedaOrigen = "PEN";
            double saldoOrigen = parseToDouble(cuentaOrigenMap.get("valorInicial"));

            // Convertir el monto ingresado (en selectedCurrency) a la moneda de la cuenta origen
            double montoEnOrigen = convertirMoneda(monto, selectedCurrency, monedaOrigen);

            if (montoEnOrigen > saldoOrigen + 1e-9) {
                // Mostrar advertencia en el UI en lugar de flotante
                double saldoReferencia = convertirMoneda(saldoOrigen, monedaOrigen, selectedCurrency);
                String mensaje = String.format(Locale.getDefault(),
                        "Tu saldo es insuficiente. Disponible ≈ %s %.2f",
                        selectedCurrency, saldoReferencia);
                tvMontoWarning.setText(mensaje);
                tvMontoWarning.setVisibility(android.view.View.VISIBLE);
                return;
            }

            // Validación pasada: proceder a guardar
            Map<String, Object> data = new HashMap<>();
            data.put("tipo", "Transferencia");
            data.put("monto", monto);
            data.put("cuentaOrigen", origenNombre);
            data.put("cuentaDestino", destinoNombre);
            data.put("nota", nota);
            data.put("beneficiario", beneficiario);
            data.put("fecha", fecha);
            data.put("hora", hora);
            data.put("formaPago", formaPago);
            data.put("estado", estado);
            data.put("moneda", selectedCurrency);
            data.put("categoria", destinoNombre);
            data.put("createdAt", com.google.firebase.firestore.FieldValue.serverTimestamp());
            // Marcar transferencia externa para que en la UI del emisor solo se muestre el egreso
            if (destinoEsExterno) {
                data.put("esExterna", true);
                data.put("extDirection", "out");
                data.put("destUid", selectedDestUid);
            }

            // Bloquear doble click mientras se guarda
            isSaving = true;
            btnNext.setEnabled(false);

            com.example.aureum1.Backend.FirebaseHelper.INSTANCE.addRegistro(
                    userId,
                    data,
                    (success, error) -> {
                        if (success) {
                            if (destinoEsExterno) {
                                // En transferencia externa, finalizar solo cuando el registro del destinatario esté creado
                                procesarTransferenciaExterna(userId, selectedDestUid, origenNombre, destinoNombre, monto);
                            } else {
                                // Transferencia interna: finalizar tras actualizar saldos
                                procesarTransferencia(userId, origenNombre, destinoNombre, monto);
                                runOnUiThread(() -> {
                                    Toast.makeText(this, "Registro guardado correctamente", Toast.LENGTH_SHORT).show();
                                    finish();
                                });
                            }
                        } else {
                            runOnUiThread(() -> Toast.makeText(this, "Error: " + error, Toast.LENGTH_SHORT).show());
                            // Rehabilitar si falla
                            isSaving = false;
                            btnNext.setEnabled(true);
                        }
                        return null;
                    }
            );
        }).addOnFailureListener(e -> Toast.makeText(this, "Error validando cuentas: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    // ==================== Procesar Ingreso / Gasto ====================
    private void procesarIngresoOEGasto(String userId, String cuentaNombre, double monto, boolean esIngreso) {
        com.google.firebase.firestore.FirebaseFirestore db =
                com.example.aureum1.Backend.FirebaseHelper.INSTANCE.getDb();

        db.collection("accounts").document(userId).get().addOnSuccessListener(docSnap -> {
            if (!docSnap.exists()) return;

            java.util.List<Map<String, Object>> cuentas = (java.util.List<Map<String, Object>>) docSnap.get("cuentas");
            if (cuentas == null) return;

            for (Map<String, Object> cuenta : cuentas) {
                String nombre = (String) cuenta.get("nombre");
                double saldo = parseToDouble(cuenta.get("valorInicial"));
                if (nombre.equals(cuentaNombre)) {
                    String monedaCuenta = (String) cuenta.get("moneda");
                    if (monedaCuenta == null) monedaCuenta = "PEN";
                    double montoConvertido = convertirMoneda(monto, selectedCurrency, monedaCuenta);
                    cuenta.put("valorInicial", esIngreso ? saldo + montoConvertido : saldo - montoConvertido);
                    break;
                }
            }

            Map<String, Object> updates = new HashMap<>();
            updates.put("cuentas", cuentas);
            db.collection("accounts").document(userId).update(updates);
        });
    }

    // ==================== Procesar Transferencia ====================
    private void procesarTransferencia(String userId, String origenNombre, String destinoNombre, double monto) {
        com.google.firebase.firestore.FirebaseFirestore db =
                com.example.aureum1.Backend.FirebaseHelper.INSTANCE.getDb();

        db.collection("accounts").document(userId).get().addOnSuccessListener(docSnap -> {
            if (!docSnap.exists()) return;

            java.util.List<Map<String, Object>> cuentas = (java.util.List<Map<String, Object>>) docSnap.get("cuentas");
            if (cuentas == null) return;

            String monedaOrigen = "PEN", monedaDestino = "PEN";
            for (Map<String, Object> cuenta : cuentas) {
                String nombre = (String) cuenta.get("nombre");
                if (nombre.equals(origenNombre)) monedaOrigen = (String) cuenta.get("moneda");
                if (nombre.equals(destinoNombre)) monedaDestino = (String) cuenta.get("moneda");
            }

            for (Map<String, Object> cuenta : cuentas) {
                String nombre = (String) cuenta.get("nombre");
                double saldo = parseToDouble(cuenta.get("valorInicial"));
                // Convertir desde moneda seleccionada a moneda de origen
                double montoEnOrigen = convertirMoneda(monto, selectedCurrency, monedaOrigen);
                if (nombre.equals(origenNombre)) cuenta.put("valorInicial", saldo - montoEnOrigen);
                if (nombre.equals(destinoNombre)) cuenta.put("valorInicial", saldo + convertirMoneda(montoEnOrigen, monedaOrigen, monedaDestino));
            }

            Map<String, Object> updates = new HashMap<>();
            updates.put("cuentas", cuentas);
            db.collection("accounts").document(userId).update(updates);
        });
    }

    private double parseToDouble(Object value) {
        if (value == null) return 0.0;
        if (value instanceof Number) return ((Number) value).doubleValue();
        if (value instanceof String) {
            try { return Double.parseDouble((String) value); } catch (Exception e) { return 0.0; }
        }
        return 0.0;
    }

    private double convertirMoneda(double monto, String monedaOrigen, String monedaDestino) {
        if (monedaOrigen.equals(monedaDestino)) return monto;
        double usdToPen = 3.8;
        double eurToPen = 4.1;
        double montoEnPen;
        switch (monedaOrigen) {
            case "USD": montoEnPen = monto * usdToPen; break;
            case "EUR": montoEnPen = monto * eurToPen; break;
            default: montoEnPen = monto;
        }
        switch (monedaDestino) {
            case "USD": return montoEnPen / usdToPen;
            case "EUR": return montoEnPen / eurToPen;
            default: return montoEnPen;
        }
    }

    // ==================== Procesar Transferencia EXTERNA ====================
    private void procesarTransferenciaExterna(String userIdOrigen, String userIdDestino, String origenNombre, String destinoNombre, double monto) {
        com.google.firebase.firestore.FirebaseFirestore db =
                com.example.aureum1.Backend.FirebaseHelper.INSTANCE.getDb();

        // Actualizar saldo del usuario origen
        db.collection("accounts").document(userIdOrigen).get().addOnSuccessListener(docSnap -> {
            if (!docSnap.exists()) return;
            java.util.List<Map<String, Object>> cuentasOrigen = (java.util.List<Map<String, Object>>) docSnap.get("cuentas");
            if (cuentasOrigen == null) return;

            String monedaOrigen = "PEN";
            for (Map<String, Object> cuenta : cuentasOrigen) {
                String nombre = (String) cuenta.get("nombre");
                if (nombre.equals(origenNombre)) {
                    String m = (String) cuenta.get("moneda");
                    if (m != null) monedaOrigen = m;
                    double saldo = parseToDouble(cuenta.get("valorInicial"));
                    double montoEnOrigen = convertirMoneda(monto, selectedCurrency, monedaOrigen);
                    cuenta.put("valorInicial", saldo - montoEnOrigen);
                    break;
                }
            }
            Map<String, Object> updates = new HashMap<>();
            updates.put("cuentas", cuentasOrigen);
            db.collection("accounts").document(userIdOrigen).update(updates);
        });

        // Actualizar saldo del usuario destino y crear su registro entrante
        db.collection("accounts").document(userIdDestino).get().addOnSuccessListener(docSnap -> {
            if (!docSnap.exists()) return;
            java.util.List<Map<String, Object>> cuentasDestino = (java.util.List<Map<String, Object>>) docSnap.get("cuentas");
            if (cuentasDestino == null) return;

            String monedaCuentaDestino = "PEN";
            for (Map<String, Object> cuenta : cuentasDestino) {
                String nombre = (String) cuenta.get("nombre");
                if (nombre.equals(destinoNombre)) {
                    String m = (String) cuenta.get("moneda");
                    if (m != null) monedaCuentaDestino = m;
                    double saldo = parseToDouble(cuenta.get("valorInicial"));
                    double montoEnDestino = convertirMoneda(monto, selectedCurrency, monedaCuentaDestino);
                    cuenta.put("valorInicial", saldo + montoEnDestino);
                    break;
                }
            }
            Map<String, Object> updates = new HashMap<>();
            updates.put("cuentas", cuentasDestino);
            db.collection("accounts").document(userIdDestino).update(updates);

            // Crear registro entrante en el usuario destino
            Map<String, Object> registroDestino = new HashMap<>();
            registroDestino.put("tipo", "Transferencia");
            // Guardar el monto convertido a la moneda de la cuenta destino para coherencia visual
            double montoRegistroDestino = convertirMoneda(monto, selectedCurrency, monedaCuentaDestino);
            registroDestino.put("monto", montoRegistroDestino);
            registroDestino.put("cuentaOrigen", origenNombre);
            registroDestino.put("cuentaDestino", destinoNombre);
            registroDestino.put("nota", nota);
            registroDestino.put("beneficiario", beneficiario);
            registroDestino.put("fecha", fecha);
            registroDestino.put("hora", hora);
            registroDestino.put("formaPago", formaPago);
            registroDestino.put("estado", estado);
            // Moneda del registro debe reflejar la cuenta destino
            registroDestino.put("moneda", monedaCuentaDestino);
            registroDestino.put("categoria", destinoNombre);
            registroDestino.put("createdAt", com.google.firebase.firestore.FieldValue.serverTimestamp());
            // Marcar transferencia externa para que en la UI del receptor solo se muestre el ingreso
            registroDestino.put("esExterna", true);
            registroDestino.put("extDirection", "in");
            registroDestino.put("sourceUid", userIdOrigen);

            com.example.aureum1.Backend.FirebaseHelper.INSTANCE.addRegistro(
                    userIdDestino,
                    registroDestino,
                    (s, err) -> {
                        if (!s && err != null) {
                            android.util.Log.e("AddRecord", "Error creando registro destino: " + err);
                            runOnUiThread(() -> android.widget.Toast.makeText(this, "Error creando registro del destinatario: " + err, android.widget.Toast.LENGTH_LONG).show());
                            // Rehabilitar UI si falla
                            isSaving = false;
                            runOnUiThread(() -> btnNext.setEnabled(true));
                        } else {
                            // Éxito: notificar y cerrar
                            runOnUiThread(() -> {
                                android.widget.Toast.makeText(this, "Transferencia registrada exitosamente", android.widget.Toast.LENGTH_SHORT).show();
                                finish();
                            });
                        }
                        return null;
                    }
            );
        });
    }

    // ==================== Manejo de resultados de otras actividades ====================
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK && data != null) {
            if (requestCode == REQ_SELECCION_CUENTA) {
                String cuentaSeleccionada = data.getStringExtra("CUENTA_SELECCIONADA");
                if (cuentaSeleccionada != null) {
                    tvCuenta.setText(cuentaSeleccionada);
                    actualizarMonedaPorCuenta(cuentaSeleccionada);
                }
            } else if (requestCode == REQ_SELECCION_CATEGORIA) {
                String categoriaSeleccionada = data.getStringExtra("CATEGORIA_SELECCIONADA");
                if (categoriaSeleccionada != null) {
                    selectedCategoria = categoriaSeleccionada;
                    if (!"Transferencia".equals(currentTab)) {
                        tvCategoria.setText(categoriaSeleccionada);
                    }
                }
            } else if (requestCode == REQ_SELECCION_CUENTA_DESTINO) {
                String cuentaDestino = data.getStringExtra("CUENTA_SELECCIONADA");
                String destUid = data.getStringExtra("DEST_UID");
                if (cuentaDestino != null) {
                    selectedCuentaDestino = cuentaDestino;
                    if ("Transferencia".equals(currentTab)) {
                        tvCategoria.setText(cuentaDestino);
                    }
                }
                if (destUid != null) {
                    selectedDestUid = destUid;
                }
            } else if (requestCode == REQ_REG_INGRESO) {
                nota = data.getStringExtra("NOTA");
                pagador = data.getStringExtra("PAGADOR");
                formaPago = data.getStringExtra("FORMA_PAGO");
                estado = data.getStringExtra("ESTADO");
                fecha = data.getStringExtra("FECHA");
                hora = data.getStringExtra("HORA");
                registroCompletado = true;
            } else if (requestCode == REQ_REG_GASTO || requestCode == REQ_REG_TRANSFER) {
                nota = data.getStringExtra("NOTA");
                beneficiario = data.getStringExtra("BENEFICIARIO");
                formaPago = data.getStringExtra("FORMA_PAGO");
                estado = data.getStringExtra("ESTADO");
                fecha = data.getStringExtra("FECHA");
                hora = data.getStringExtra("HORA");
                registroCompletado = true;
            }
        }
    }

    private void actualizarMonedaPorCuenta(String cuentaSeleccionada) {
        String uid = com.example.aureum1.Backend.FirebaseHelper.INSTANCE.getAuth().getCurrentUser().getUid();
        com.example.aureum1.Backend.FirebaseHelper.INSTANCE.getDb()
                .collection("accounts").document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    java.util.List<Map<String, Object>> cuentas = (java.util.List<Map<String, Object>>) doc.get("cuentas");
                    if (cuentas == null) return;
                    for (Map<String, Object> c : cuentas) {
                        String nombre = (String) c.get("nombre");
                        if (cuentaSeleccionada.equals(nombre)) {
                            String mon = (String) c.get("moneda");
                            if (mon != null && !mon.isEmpty()) {
                                selectedCurrency = mon;
                                tvMoneda.setText(mon);
                                monedaCuentaOrigenCache = mon;
                            }
                            Object saldoObj = c.get("valorInicial");
                            double saldo = 0.0;
                            if (saldoObj instanceof Number) {
                                saldo = ((Number) saldoObj).doubleValue();
                            } else if (saldoObj instanceof String) {
                                try { saldo = Double.parseDouble((String) saldoObj); } catch (Exception ignored) {}
                            }
                            saldoCuentaOrigenCache = saldo;
                            break;
                        }
                    }
                    updateWarning();
                });
    }
}
