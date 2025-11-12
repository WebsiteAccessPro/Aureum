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
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.example.aureum1.Backend.AccountService
import com.google.firebase.firestore.FirebaseFirestore
import java.util.UUID

class NuevaCuentaActivity : AppCompatActivity() {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }
    private val accountService by lazy { AccountService(db) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nueva_cuenta)

        val tipoView = findViewById<MaterialAutoCompleteTextView>(R.id.etTipoCuenta)
        val monedaView = findViewById<MaterialAutoCompleteTextView>(R.id.spMoneda)
        val etNombre = findViewById<TextInputEditText>(R.id.etNombreCuenta)
        val etNumero = findViewById<TextInputEditText>(R.id.etNumeroCuenta)
        val etValorInicial = findViewById<TextInputEditText>(R.id.etValorInicial)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbarNuevaCuenta)
        toolbar.setNavigationOnClickListener { finish() }
        toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.action_guardar -> {
                    guardarCuenta(
                        nombre = etNombre.text.toString().trim(),
                        tipo = tipoView.text.toString().trim(),
                        numero = etNumero.text.toString().trim(),
                        moneda = monedaView.text.toString().trim(),
                        valorInicial = etValorInicial.text.toString().trim()
                    )
                    true
                }
                else -> false
            }
        }

        val tipos = resources.getStringArray(R.array.tipos_cuenta)
        val monedas = resources.getStringArray(R.array.monedas_array)
        tipoView.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, tipos))
        monedaView.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, monedas))
        tipoView.setOnClickListener { tipoView.showDropDown() }
        monedaView.setOnClickListener { monedaView.showDropDown() }
        tipoView.setOnFocusChangeListener { v, hasFocus -> if (hasFocus) (v as MaterialAutoCompleteTextView).showDropDown() }
        monedaView.setOnFocusChangeListener { v, hasFocus -> if (hasFocus) (v as MaterialAutoCompleteTextView).showDropDown() }

        etValorInicial.showSoftInputOnFocus = false
        etValorInicial.setOnClickListener { showCalculator { etValorInicial.setText(it) } }
        etValorInicial.setOnFocusChangeListener { _, hasFocus -> if (hasFocus) showCalculator { etValorInicial.setText(it) } }
    }

    private fun guardarCuenta(nombre: String, tipo: String, numero: String, moneda: String, valorInicial: String) {
        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(this, "No hay usuario autenticado", Toast.LENGTH_SHORT).show()
            return
        }

        if (nombre.isEmpty() || tipo.isEmpty() || moneda.isEmpty()) {
            Toast.makeText(this, "Completa todos los campos obligatorios", Toast.LENGTH_SHORT).show()
            return
        }

        // 🔹 Creamos la estructura de la cuenta
        val nuevaCuenta = hashMapOf(
            "id" to UUID.randomUUID().toString(),
            "nombre" to nombre.ifEmpty { "Sin nombre" },
            "tipo" to tipo.ifEmpty { "Sin tipo" },
            "numero" to numero.ifEmpty { "" },
            "moneda" to moneda.ifEmpty { "USD" },
            "valorInicial" to valorInicial.ifEmpty { "0" }
        )

        // 🔹 Usar servicio backend para mutación (MVC: controlador delega al backend)
        accountService.addAccount(
            uid = user.uid,
            account = nuevaCuenta,
            onSuccess = {
                Toast.makeText(this, "Cuenta agregada correctamente", Toast.LENGTH_SHORT).show()
                finish()
            },
            onError = { e ->
                Toast.makeText(this, "Error al guardar cuenta: ${e.message}", Toast.LENGTH_LONG).show()
            }
        )
    }

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
        ids.forEach { (id, ch) -> view.findViewById<Button>(id).setOnClickListener { append(ch) } }

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
}