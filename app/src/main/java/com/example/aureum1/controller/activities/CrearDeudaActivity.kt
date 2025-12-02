package com.example.aureum1.controller.activities

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.aureum1.R
import com.example.aureum1.model.repository.AccountRepository
import com.example.aureum1.model.repository.DebtRepository
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class CrearDeudaActivity : AppCompatActivity() {

    private val debtRepo = DebtRepository()
    private val accountRepo = AccountRepository()
    private val auth = FirebaseAuth.getInstance()

    private lateinit var tvCantidad: TextView
    private lateinit var spAccion: MaterialAutoCompleteTextView
    private lateinit var spCuenta: MaterialAutoCompleteTextView

    private var cuentas: List<String> = emptyList()
    private var monedaPorCuenta: Map<String, String> = emptyMap()
    private var montoDeudaActual: Double = 0.0
    private var monedaDeuda: String = "PEN"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_crear_registro_deuda)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbarCrearDeuda)
        toolbar.setNavigationOnClickListener { finish() }
        toolbar.inflateMenu(R.menu.menu_toolbar_check)
        toolbar.setOnMenuItemClickListener { item ->
            if (item.itemId == R.id.action_check) {
                guardar()
                true
            } else false
        }

        tvCantidad = findViewById(R.id.tvCantidad)
        spAccion = findViewById(R.id.spAccionDeuda)
        spCuenta = findViewById(R.id.spCuenta)

        configurarSpinners()
        configurarKeypad()
    }

    private fun configurarSpinners() {
        val acciones = listOf("Aumentar deuda", "Reembolsar deuda")
        spAccion.setSimpleItems(acciones.toTypedArray())
        spAccion.setOnClickListener { spAccion.showDropDown() }
        spAccion.setOnFocusChangeListener { _, hasFocus -> if (hasFocus) spAccion.showDropDown() }

        when (intent.getStringExtra("DEFAULT_ACCION")) {
            "presto" -> spAccion.setText("Reembolsar deuda", false)
            "me_prestaron" -> spAccion.setText("Aumentar deuda", false)
        }

        val uid = auth.currentUser?.uid ?: return
        accountRepo.subscribeAccounts(uid) { list ->
            // filtramos solo cuentas reales (omitimos tile de "Agregar")
            val cuentasReales = list.filter { !it.isAddTile }

            cuentas = cuentasReales.map { it.nombre }
            monedaPorCuenta = cuentasReales.associate { it.nombre to it.moneda }

            spCuenta.setSimpleItems(cuentas.toTypedArray())
            if (cuentas.isNotEmpty()) spCuenta.setText(cuentas.first(), false)
            spCuenta.setOnClickListener { spCuenta.showDropDown() }
            spCuenta.setOnFocusChangeListener { _, hasFocus -> if (hasFocus) spCuenta.showDropDown() }

            val label = findViewById<TextView>(R.id.tvLabelCantidad)
            val mInit = monedaPorCuenta[spCuenta.text.toString()] ?: "PEN"
            val mInitLabel = if (mInit.equals("PEN", true)) "SOL" else mInit
            label.text = "Cantidad en $mInitLabel"
            spCuenta.setOnItemClickListener { _, _, _, _ ->
                val m = monedaPorCuenta[spCuenta.text.toString()] ?: "PEN"
                val mLabel = if (m.equals("PEN", true)) "SOL" else m
                label.text = "Cantidad en $mLabel"
            }
        }

        val accionDefault = intent.getStringExtra("DEFAULT_ACCION") ?: "presto"
        val debtId = intent.getStringExtra("DEBT_ID")
        if (!debtId.isNullOrBlank()) {
            val db = FirebaseFirestore.getInstance()
            db.collection("accounts").document(uid)
                .collection("deudas").document(accionDefault)
                .collection("items").document(debtId)
                .get()
                .addOnSuccessListener { doc ->
                    val m = doc.data
                    if (m != null) {
                        montoDeudaActual = (m["monto"] as? Number)?.toDouble() ?: 0.0
                        monedaDeuda = (m["moneda"] as? String).orEmpty().ifEmpty { "PEN" }
                        actualizarPlaceholder()
                    }
                }
        } else {
            actualizarPlaceholder()
        }

        spAccion.setOnItemClickListener { _, _, _, _ -> actualizarPlaceholder() }
    }

    private fun configurarKeypad() {
        val ids = listOf(
            R.id.btn1, R.id.btn2, R.id.btn3,
            R.id.btn4, R.id.btn5, R.id.btn6,
            R.id.btn7, R.id.btn8, R.id.btn9,
            R.id.btn0, R.id.btnDot
        )
        for (id in ids) {
            findViewById<Button>(id).setOnClickListener { v ->
                val t = (v as Button).text.toString()
                val cur = tvCantidad.text.toString()
                tvCantidad.text = if (cur == "0" && t != ".") t else cur + t
            }
        }
        findViewById<Button>(R.id.btnBack).setOnClickListener {
            val cur = tvCantidad.text.toString()
            tvCantidad.text = if (cur.length <= 1) "0" else cur.dropLast(1)
        }
    }

    private fun guardar() {
        val uid = auth.currentUser?.uid ?: return
        val operacionElegida = spAccion.text.toString().trim()
        val cuenta = spCuenta.text.toString().trim()
        val monto = tvCantidad.text.toString().toDoubleOrNull() ?: 0.0
        val moneda = monedaPorCuenta[cuenta] ?: "PEN"

        if (cuenta.isBlank() || operacionElegida.isBlank() || monto <= 0.0) {
            Toast.makeText(this, "Complete todos los campos correctamente", Toast.LENGTH_SHORT).show()
            return
        }

        val accion = intent.getStringExtra("DEFAULT_ACCION") ?: "presto"
        val debtId = intent.getStringExtra("DEBT_ID")

        val nombrePersona = intent.getStringExtra("DEFAULT_NOMBRE") ?: "Persona"
        val esAumento = operacionElegida.equals("Aumentar deuda", true)

        val tipoMovimiento = when {
            accion == "presto" && !esAumento -> "Préstamos, alquileres"
            accion == "presto" && esAumento -> "Préstamos, interés"
            accion == "me_prestaron" && !esAumento -> "Préstamos, interés"
            else -> "Préstamos, alquileres"
        }

        val signo = when {
            accion == "presto" && !esAumento -> "+"
            accion == "presto" && esAumento -> "-"
            accion == "me_prestaron" && !esAumento -> "-"
            else -> "+"
        }

        val direccion = when {
            accion == "presto" && !esAumento -> "Yo → $nombrePersona"
            accion == "presto" && esAumento -> "$nombrePersona → Yo"
            accion == "me_prestaron" && !esAumento -> "Yo → $nombrePersona"
            else -> "$nombrePersona → Yo"
        }

        if (!debtId.isNullOrBlank()) {
            val montoConv = convertirMoneda(monto, moneda, monedaDeuda)
            val delta = when (accion) {
                "presto" -> if (esAumento) montoConv else -montoConv
                else -> if (esAumento) montoConv else -montoConv
            }
            val movement = mapOf(
                "movimiento_operacion" to (if (esAumento) "aumento" else "reembolso"),
                "movimiento_tipo" to tipoMovimiento,
                "movimiento_signo" to signo,
                "movimiento_direccion" to direccion,
                "cuenta" to cuenta,
                "moneda" to moneda,
                "monto" to monto,
                "fecha" to FieldValue.serverTimestamp()
            )
            debtRepo.addMovementToDebt(uid, accion, debtId, movement, delta) { success, error ->
                if (!success) {
                    Toast.makeText(this, error ?: "Error al guardar el movimiento", Toast.LENGTH_SHORT).show()
                } else {
                    ajustarSaldoCuenta(uid, cuenta, moneda, monto, accion, esAumento)
                }
            }
        } else {
            debtRepo.addDebt(uid, mapOf(
                "accion" to accion,
                "cuenta" to cuenta,
                "moneda" to moneda,
                "monto" to monto,
                "fecha" to FieldValue.serverTimestamp(),
                "estado" to "activo",
                "from" to "manual",
                "movimiento_operacion" to (if (esAumento) "aumento" else "reembolso"),
                "movimiento_tipo" to tipoMovimiento,
                "movimiento_signo" to signo,
                "movimiento_direccion" to direccion,
                "nombre" to nombrePersona
            )) { success, error ->
                if (!success) {
                    Toast.makeText(this, error ?: "Error al guardar la deuda", Toast.LENGTH_SHORT).show()
                } else {
                    ajustarSaldoCuenta(uid, cuenta, moneda, monto, accion, esAumento)
                }
            }
        }

        finish()
    }

    private fun actualizarPlaceholder() {
        val tv = findViewById<TextView>(R.id.tvPlaceholderAccion)
        val op = spAccion.text.toString().trim()
        if (op.equals("Reembolsar deuda", true) && montoDeudaActual > 0.0) {
            val labelMon = if (monedaDeuda.equals("PEN", true)) "SOL" else monedaDeuda
            val nf = java.text.NumberFormat.getNumberInstance(java.util.Locale.getDefault()).apply {
                minimumFractionDigits = 2
                maximumFractionDigits = 2
            }
            tv.text = "$labelMon${nf.format(montoDeudaActual)} para pagar la deuda"
        } else {
            tv.text = ""
        }
    }

    private fun ajustarSaldoCuenta(
        uid: String,
        cuentaNombre: String,
        selectedCurrency: String,
        monto: Double,
        accion: String,
        esAumento: Boolean
    ) {
        val db = FirebaseFirestore.getInstance()
        db.collection("accounts").document(uid).get().addOnSuccessListener { snap ->
            val cuentas = (snap.get("cuentas") as? List<Map<String, Any?>>)?.map { it.toMutableMap() } ?: return@addOnSuccessListener
            val deltaSign = when {
                accion == "presto" && esAumento -> -1
                accion == "presto" && !esAumento -> 1
                accion == "me_prestaron" && esAumento -> 1
                else -> -1
            }
            cuentas.forEach { c ->
                val nombre = (c["nombre"] as? String).orEmpty()
                if (nombre == cuentaNombre) {
                    val monCuenta = (c["moneda"] as? String).orEmpty().ifEmpty { "PEN" }
                    val saldoRaw = c["valorInicial"]
                    val saldo = when (saldoRaw) {
                        is Number -> saldoRaw.toDouble()
                        is String -> saldoRaw.toDoubleOrNull() ?: 0.0
                        else -> 0.0
                    }
                    val montoConv = convertirMoneda(monto, selectedCurrency, monCuenta)
                    c["valorInicial"] = saldo + deltaSign * montoConv
                }
            }
            db.collection("accounts").document(uid).update("cuentas", cuentas)
        }
    }

    private fun convertirMoneda(monto: Double, origen: String, destino: String): Double {
        if (origen.equals(destino, true)) return monto
        val usdToPen = 3.8
        val eurToPen = 4.1
        val montoEnPen = when (origen.uppercase()) {
            "USD" -> monto * usdToPen
            "EUR" -> monto * eurToPen
            else -> monto
        }
        return when (destino.uppercase()) {
            "USD" -> montoEnPen / usdToPen
            "EUR" -> montoEnPen / eurToPen
            else -> montoEnPen
        }
    }
}
