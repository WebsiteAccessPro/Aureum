package com.example.aureum1.controller.activities

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.aureum1.R
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import android.text.Editable
import android.text.TextWatcher
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.aureum1.Backend.AccountService

class EditCuentaActivity : AppCompatActivity() {

    // Views
    private lateinit var toolbar: MaterialToolbar
    private lateinit var etNombre: TextInputEditText
    private lateinit var etNumero: TextInputEditText
    private lateinit var etTipo: MaterialAutoCompleteTextView
    private lateinit var etValorInicial: TextInputEditText
    private lateinit var spMoneda: MaterialAutoCompleteTextView
    private lateinit var tilNombre: TextInputLayout
    private lateinit var tilNumero: TextInputLayout
    private lateinit var tilTipo: TextInputLayout
    private lateinit var tilValorInicial: TextInputLayout
    private lateinit var tilMoneda: TextInputLayout

    // Firebase
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }
    private val accountService by lazy { AccountService(db) }

    private var accId: String? = null

    // Dropdown data
    private val monedas = listOf("PEN", "USD", "EUR")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_cuenta)

        // Bind views
        toolbar        = findViewById(R.id.toolbarEditarCuenta)
        etNombre       = findViewById(R.id.etNombreCuenta)
        etNumero       = findViewById(R.id.etNumeroCuenta)
        etTipo         = findViewById(R.id.etTipoCuenta)
        etValorInicial = findViewById(R.id.etValorInicial)
        spMoneda       = findViewById(R.id.spMoneda)
        tilNombre      = findViewById(R.id.tilNombre)
        tilNumero      = findViewById(R.id.tilNumero)
        tilTipo        = findViewById(R.id.tilTipo)
        tilValorInicial= findViewById(R.id.tilValorInicial)
        tilMoneda      = findViewById(R.id.tilMoneda)

        // Dropdowns
        etTipo.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, resources.getStringArray(R.array.tipos_cuenta)))
        spMoneda.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, monedas))

        // Calculadora para valor inicial
        etValorInicial.showSoftInputOnFocus = false
        etValorInicial.setOnClickListener { showCalculator { etValorInicial.setText(it) } }
        etValorInicial.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) showCalculator { etValorInicial.setText(it) }
        }

        // Toolbar
        toolbar.setNavigationOnClickListener { finish() }
        toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.action_save,
                R.id.action_save_overflow   -> { confirmarGuardar(); true }

                R.id.action_delete,
                R.id.action_delete_overflow -> { confirmarEliminar(); true }

                else -> false
            }
        }

        // Forzar overflow y ocultar botones no usados
        toolbar.menu.findItem(R.id.action_forgive)?.isVisible = false
        toolbar.menu.findItem(R.id.action_close)?.isVisible = false
        toolbar.menu.findItem(R.id.action_edit_debt)?.isVisible = false
        toolbar.menu.findItem(R.id.action_save)?.isVisible = false
        toolbar.menu.findItem(R.id.action_delete)?.isVisible = false
        toolbar.menu.findItem(R.id.action_save_overflow)?.isVisible = true
        toolbar.menu.findItem(R.id.action_delete_overflow)?.isVisible = true

        // Recuperar ID de cuenta
        accId = intent.getStringExtra("ACC_ID")
        if (accId.isNullOrEmpty()) {
            Toast.makeText(this, "ID de cuenta no recibido", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Load data
        cargarCuenta()

        // ---------------------------
        // ELIMINAR ERRORES AUTOMÁTICO
        // ---------------------------
        etNombre.addTextChangedListener(simpleWatcher { tilNombre.error = null })
        etNumero.addTextChangedListener(simpleWatcher { tilNumero.error = null })
        etValorInicial.addTextChangedListener(simpleWatcher { tilValorInicial.error = null })
        etTipo.setOnItemClickListener { _, _, _, _ -> tilTipo.error = null }
        spMoneda.setOnItemClickListener { _, _, _, _ -> tilMoneda.error = null }
    }

    // Confirmaciones
    private fun confirmarGuardar() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Confirmar cambios")
            .setMessage("¿Quieres guardar los cambios de esta cuenta?")
            .setPositiveButton("Guardar") { d, _ -> d.dismiss(); guardarCambios() }
            .setNegativeButton("Cancelar") { d, _ -> d.dismiss() }
            .show()
    }

    private fun confirmarEliminar() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Eliminar cuenta")
            .setMessage("¿Estás seguro de eliminar esta cuenta? Esta acción no se puede deshacer.")
            .setPositiveButton("Eliminar") { d, _ -> d.dismiss(); eliminarCuenta() }
            .setNegativeButton("Cancelar") { d, _ -> d.dismiss() }
            .show()
    }

    // Carga la cuenta desde Firestore
    private fun cargarCuenta() {
        val uid = auth.currentUser?.uid ?: run {
            toast("Usuario no autenticado")
            finish()
            return
        }

        db.collection("accounts").document(uid)
            .get()
            .addOnSuccessListener { snap ->
                val arr = snap.get("cuentas") as? List<Map<String, Any?>> ?: emptyList()
                val item = arr.firstOrNull { (it["id"] as? String) == accId }

                if (item == null) {
                    toast("Cuenta no encontrada")
                    finish()
                    return@addOnSuccessListener
                }

                val nombre = (item["nombre"] as? String).orEmpty()
                val numero = (item["numero"] as? String).orEmpty()
                val tipo   = (item["tipo"] as? String).orEmpty()
                val moneda = (item["moneda"] as? String).orEmpty().ifEmpty { "PEN" }
                val valor  = (item["valorInicial"] as? Number)?.toDouble() ?: 0.0

                etNombre.setText(nombre)
                etNumero.setText(numero)
                etTipo.setText(tipo, false)
                spMoneda.setText(moneda, false)
                etValorInicial.setText(if (valor == 0.0) "" else valor.toString())
            }
            .addOnFailureListener {
                toast("Error al cargar: ${it.message}")
                finish()
            }
    }

    // Guardar cambios
    private fun guardarCambios() {
        val uid = auth.currentUser?.uid ?: return
        val id  = accId ?: return

        val nombre = etNombre.text?.toString()?.trim().orEmpty()
        val numero = etNumero.text?.toString()?.trim().orEmpty()
        val tipo   = etTipo.text?.toString()?.trim().orEmpty()
        val moneda = spMoneda.text?.toString()?.trim().orEmpty()
        val valorInicial = etValorInicial.text?.toString()?.trim()?.toDoubleOrNull() ?: 0.0

        if (!validarFormulario(nombre, numero, tipo, moneda, valorInicial)) return

        val nuevo = hashMapOf(
            "id"           to id,
            "nombre"       to nombre,
            "numero"       to numero,
            "tipo"         to tipo,
            "moneda"       to moneda,
            "valorInicial" to valorInicial
        )

        accountService.updateAccount(
            uid = uid,
            accountId = id,
            newData = nuevo,
            onSuccess = {
                toast("Cambios guardados")
                finish()
            },
            onError = { e ->
                toast("Error al guardar: ${e.message}")
            }
        )
    }

    private fun validarFormulario(
        nombre: String, numero: String, tipo: String, moneda: String, valorInicial: Double
    ): Boolean {

        var ok = true

        if (nombre.isBlank()) { tilNombre.error = "Ingresa el nombre de la cuenta"; ok = false }

        val onlyDigits = numero.all { it.isDigit() }
        if (numero.isBlank()) { tilNumero.error = "Ingresa el número"; ok = false }
        else if (!onlyDigits) { tilNumero.error = "Solo números"; ok = false }
        else if (numero.length != 20) { tilNumero.error = "Debe tener 20 dígitos"; ok = false }

        if (tipo.isBlank()) { tilTipo.error = "Selecciona el tipo"; ok = false }
        if (moneda.isBlank()) { tilMoneda.error = "Selecciona la moneda"; ok = false }
        if (valorInicial <= 0) { tilValorInicial.error = "Debe ser mayor a 0"; ok = false }
        
        return ok
    }

    private fun simpleWatcher(onChange: () -> Unit) = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { onChange() }
        override fun afterTextChanged(s: Editable?) {}
    }

    // Eliminar cuenta
    private fun eliminarCuenta() {
        val uid = auth.currentUser?.uid ?: return
        val id  = accId ?: return

        accountService.deleteAccount(
            uid = uid,
            accountId = id,
            onSuccess = {
                toast("Cuenta eliminada")
                finish()
            },
            onError = { e ->
                toast("Error al eliminar: ${e.message}")
            }
        )
    }

    // BottomSheet Calculator
    private fun showCalculator(onResult: (String) -> Unit) {
        val dialog = BottomSheetDialog(this)
        val view = LayoutInflater.from(this).inflate(R.layout.bottomsheet_calculator, null, false)
        dialog.setContentView(view)

        val tv = view.findViewById<TextView>(R.id.tvDisplay)

        fun append(ch: String) {
            val cur = tv.text.toString()
            when (ch) {
                "." -> if (!cur.contains(".")) tv.text = cur + "."
                "C" -> tv.text = "0"
                "←" -> tv.text = if (cur.length > 1) cur.dropLast(1) else "0"
                else -> tv.text = if (cur == "0") ch else cur + ch
            }
        }

        val ids = listOf(
            R.id.btn0 to "0", R.id.btn1 to "1", R.id.btn2 to "2", R.id.btn3 to "3",
            R.id.btn4 to "4", R.id.btn5 to "5", R.id.btn6 to "6",
            R.id.btn7 to "7", R.id.btn8 to "8", R.id.btn9 to "9",
            R.id.btn00 to "00", R.id.btnDot to ".", R.id.btnClear to "C", R.id.btnBack to "←"
        )

        ids.forEach { (id, ch) ->
            view.findViewById<Button>(id).setOnClickListener { append(ch) }
        }

        view.findViewById<Button>(R.id.btnOk).setOnClickListener {
            val text = tv.text.toString().trim().removeSuffix(".")
            onResult(text)
            dialog.dismiss()
        }

        dialog.show()

        val bottomSheet = dialog.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
        bottomSheet?.let {
            val behavior = BottomSheetBehavior.from(it)
            behavior.peekHeight = (420 * resources.displayMetrics.density).toInt()
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
        }

        view.findViewById<ImageView?>(R.id.btnCloseCalc)?.setOnClickListener {
            dialog.dismiss()
        }
    }

    private fun toast(msg: String) =
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
