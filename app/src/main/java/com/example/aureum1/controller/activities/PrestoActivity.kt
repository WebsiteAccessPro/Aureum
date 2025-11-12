package com.example.aureum1.controller.activities

import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
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

class PrestoActivity : AppCompatActivity() {

    private val debtRepo = DebtRepository()
    private val accountRepo = AccountRepository()
    private val auth = FirebaseAuth.getInstance()

    private lateinit var tvCantidad: TextView
    private lateinit var spAccion: MaterialAutoCompleteTextView
    private lateinit var spCuenta: MaterialAutoCompleteTextView

    private var cuentas: List<String> = emptyList()
    private var monedaPorCuenta: Map<String, String> = emptyMap()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_presto)

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
        val acciones = listOf("Aumento de deuda", "Devolución de deuda")
        spAccion.setSimpleItems(acciones.toTypedArray())
        // Por default en Prestó, sugerimos "Devolución de deuda"
        spAccion.setText("Devolución de deuda", false)

        val uid = auth.currentUser?.uid ?: return
        accountRepo.subscribeAccounts(uid) { list ->
            val cuentasReales = list.filter { !it.isAddTile }
            cuentas = cuentasReales.map { it.nombre }
            monedaPorCuenta = cuentasReales.associate { it.nombre to it.moneda }

            spCuenta.setSimpleItems(cuentas.toTypedArray())
            if (cuentas.isNotEmpty()) spCuenta.setText(cuentas.first(), false)

            val label = findViewById<TextView>(R.id.tvLabelCantidad)
            val mon = monedaPorCuenta[spCuenta.text.toString()] ?: "PEN"
            label.text = "Cantidad en $mon"

            spCuenta.setOnItemClickListener { _, _, _, _ ->
                val m = monedaPorCuenta[spCuenta.text.toString()] ?: "PEN"
                label.text = "Cantidad en $m"
            }
        }
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
        findViewById<ImageButton>(R.id.btnBackspace).setOnClickListener {
            val cur = tvCantidad.text.toString()
            tvCantidad.text = if (cur.length <= 1) "0" else cur.dropLast(1)
        }
    }

    private fun guardar() {
        val uid = auth.currentUser?.uid ?: return
        val accionElegida = spAccion.text.toString().trim()
        val cuenta = spCuenta.text.toString().trim()
        val monto = tvCantidad.text.toString().toDoubleOrNull() ?: 0.0
        val moneda = monedaPorCuenta[cuenta] ?: "PEN"

        if (cuenta.isBlank() || accionElegida.isBlank() || monto <= 0.0) {
            Toast.makeText(this, "Complete todos los campos correctamente", Toast.LENGTH_SHORT).show()
            return
        }

        val accion = if (accionElegida == "Aumento de deuda") "me_prestaron" else "presto"

        debtRepo.addDebt(uid, mapOf(
            "accion" to accion,
            "cuenta" to cuenta,
            "moneda" to moneda,
            "monto" to monto,
            "fecha" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
            "estado" to "activo",
            "from" to "manual"
        )) { success, error ->
            if (!success) {
                Toast.makeText(this, error ?: "Error al guardar la deuda", Toast.LENGTH_SHORT).show()
            }
        }

        finish()
    }
}