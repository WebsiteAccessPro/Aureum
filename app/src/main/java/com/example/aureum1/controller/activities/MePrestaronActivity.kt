package com.example.aureum1.controller.activities

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.aureum1.R
import com.example.aureum1.model.repository.AccountRepository
import com.example.aureum1.model.repository.DebtRepository
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.Timestamp
import android.app.DatePickerDialog
import android.view.LayoutInflater
import android.view.View

class MePrestaronActivity : AppCompatActivity() {

    private val debtRepo = DebtRepository()
    private val accountRepo = AccountRepository()
    private val auth = FirebaseAuth.getInstance()

    private lateinit var etCantidad: TextInputEditText
    private lateinit var etNombre: TextInputEditText
    private lateinit var etDescripcion: TextInputEditText
    private lateinit var spCuenta: MaterialAutoCompleteTextView
    private lateinit var spFecha: MaterialAutoCompleteTextView
    private lateinit var spFechaVenc: MaterialAutoCompleteTextView

    private lateinit var tilNombre: TextInputLayout
    private lateinit var tilCuenta: TextInputLayout
    private lateinit var tilCantidad: TextInputLayout
    private lateinit var tilFecha: TextInputLayout
    private lateinit var tilVenc: TextInputLayout

    private var cuentas: List<String> = emptyList()
    private var monedaPorCuenta: Map<String, String> = emptyMap()
    private var prefillCuenta: String? = null
    private var prefillMonto: Double? = null
    private var prefillNota: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_meprestaron)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbarPresto)
        toolbar.setNavigationOnClickListener { finish() }
        toolbar.inflateMenu(R.menu.menu_toolbar_check)
        toolbar.setOnMenuItemClickListener { item ->
            if (item.itemId == R.id.action_check) {
                guardar()
                true
            } else false
        }

        etCantidad = findViewById(R.id.etCantidad)
        etNombre = findViewById(R.id.etNombre)
        etDescripcion = findViewById(R.id.etDescripcion)
        spCuenta = findViewById(R.id.spCuenta)
        spFecha = findViewById(R.id.spFecha)
        spFechaVenc = findViewById(R.id.spFechaVencimiento)

        tilNombre = findViewById(R.id.tilNombreMP)
        tilCuenta = findViewById(R.id.tilCuentaMP)
        tilCantidad = findViewById(R.id.tilCantidadMP)
        tilFecha = findViewById(R.id.tilFechaMP)
        tilVenc = findViewById(R.id.tilVencMP)

        configurarCuenta()
        configurarCantidad()
        configurarFechas()

        prefillCuenta = intent.getStringExtra("PREFILL_CUENTA")
        prefillMonto = intent.getDoubleExtra("PREFILL_MONTO", 0.0).takeIf { it > 0 }
        prefillMonto?.let { etCantidad.setText("%s".format(if (it % 1.0 == 0.0) it.toInt() else it)) }
        val prefillFechaIso = intent.getStringExtra("PREFILL_FECHA_ISO").orEmpty()
        if (prefillFechaIso.isNotBlank()) {
            spFecha.setText(prefillFechaIso, false)
            try {
                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                val base = sdf.parse(prefillFechaIso)
                if (base != null) {
                    val cal = java.util.Calendar.getInstance().apply { time = base }
                    cal.add(java.util.Calendar.DAY_OF_YEAR, 30)
                    val vencTxt = "%04d-%02d-%02d".format(
                        cal.get(java.util.Calendar.YEAR),
                        cal.get(java.util.Calendar.MONTH) + 1,
                        cal.get(java.util.Calendar.DAY_OF_MONTH)
                    )
                    spFechaVenc.setText(vencTxt, false)
                }
            } catch (_: Exception) { }
        } else {
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            val hoy = sdf.format(java.util.Date())
            spFecha.setText(hoy, false)
            val cal = java.util.Calendar.getInstance().apply { add(java.util.Calendar.DAY_OF_YEAR, 30) }
            val vencTxt = "%04d-%02d-%02d".format(
                cal.get(java.util.Calendar.YEAR),
                cal.get(java.util.Calendar.MONTH) + 1,
                cal.get(java.util.Calendar.DAY_OF_MONTH)
            )
            spFechaVenc.setText(vencTxt, false)
        }
    }

    private fun configurarCuenta() {
        val uid = auth.currentUser?.uid ?: return
        accountRepo.subscribeAccounts(uid) { list ->
            val cuentasReales = list.filter { !it.isAddTile }
            cuentas = cuentasReales.map { it.nombre }
            monedaPorCuenta = cuentasReales.associate { it.nombre to it.moneda }

            spCuenta.setSimpleItems(cuentas.toTypedArray())
            if (cuentas.isNotEmpty()) spCuenta.setText(cuentas.first(), false)
            prefillCuenta?.let { if (cuentas.contains(it)) spCuenta.setText(it, false) }
        }
    }

    private fun configurarCantidad() {
        etCantidad.setShowSoftInputOnFocus(false)
        etCantidad.isFocusable = false
        etCantidad.isFocusableInTouchMode = false
        etCantidad.isClickable = true
        etCantidad.setOnClickListener {
            mostrarCalculadora { monto ->
                etCantidad.setText("%s".format(if (monto % 1.0 == 0.0) monto.toInt() else monto))
            }
        }
    }

    private fun configurarFechas() {
        val cal = java.util.Calendar.getInstance()
        spFecha.setOnClickListener { mostrarDatePicker(false, cal) }
        spFechaVenc.setOnClickListener { mostrarDatePicker(true, cal) }
        tilFecha.setEndIconOnClickListener { mostrarDatePicker(false, cal) }
        tilVenc.setEndIconOnClickListener { mostrarDatePicker(true, cal) }
    }

    private fun mostrarDatePicker(venc: Boolean, base: java.util.Calendar) {
        val y = base.get(java.util.Calendar.YEAR)
        val m = base.get(java.util.Calendar.MONTH)
        val d = base.get(java.util.Calendar.DAY_OF_MONTH)
        DatePickerDialog(this, { _, yy, mm, dd ->
            val sel = java.util.Calendar.getInstance().apply { set(yy, mm, dd) }
            val txt = "%04d-%02d-%02d".format(yy, mm + 1, dd)
            if (venc) {
                spFechaVenc.setText(txt, false)
            } else {
                spFecha.setText(txt, false)
                val vencCal = sel.clone() as java.util.Calendar
                vencCal.add(java.util.Calendar.DAY_OF_YEAR, 30)
                val vencTxt = "%04d-%02d-%02d".format(
                    vencCal.get(java.util.Calendar.YEAR),
                    vencCal.get(java.util.Calendar.MONTH) + 1,
                    vencCal.get(java.util.Calendar.DAY_OF_MONTH)
                )
                spFechaVenc.setText(vencTxt, false)
            }
        }, y, m, d).show()
    }

    private fun mostrarCalculadora(onMonto: (Double) -> Unit) {
        val dialog = BottomSheetDialog(this)
        val v = LayoutInflater.from(this).inflate(R.layout.bottomsheet_calculator, null)
        dialog.setContentView(v)
        val tv = v.findViewById<TextView>(R.id.tvDisplay)
        fun append(s: String) { tv.text = (tv.text.toString() + s).trimStart('0') }
        v.findViewById<View>(R.id.btn0).setOnClickListener { append("0") }
        v.findViewById<View>(R.id.btn00).setOnClickListener { append("00") }
        v.findViewById<View>(R.id.btn1).setOnClickListener { append("1") }
        v.findViewById<View>(R.id.btn2).setOnClickListener { append("2") }
        v.findViewById<View>(R.id.btn3).setOnClickListener { append("3") }
        v.findViewById<View>(R.id.btn4).setOnClickListener { append("4") }
        v.findViewById<View>(R.id.btn5).setOnClickListener { append("5") }
        v.findViewById<View>(R.id.btn6).setOnClickListener { append("6") }
        v.findViewById<View>(R.id.btn7).setOnClickListener { append("7") }
        v.findViewById<View>(R.id.btn8).setOnClickListener { append("8") }
        v.findViewById<View>(R.id.btn9).setOnClickListener { append("9") }
        v.findViewById<View>(R.id.btnDot).setOnClickListener { append(".") }
        v.findViewById<View>(R.id.btnBack).setOnClickListener {
            val s = tv.text.toString()
            tv.text = if (s.isNotEmpty()) s.dropLast(1) else "0"
        }
        v.findViewById<View>(R.id.btnClear).setOnClickListener { tv.text = "0" }
        v.findViewById<View>(R.id.btnOk).setOnClickListener {
            val monto = tv.text.toString().toDoubleOrNull() ?: 0.0
            dialog.dismiss()
            onMonto(monto)
        }
        v.findViewById<View>(R.id.btnCloseCalc).setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun guardar() {
        val uid = auth.currentUser?.uid ?: return
        val cuenta = spCuenta.text.toString().trim()
        val monto = etCantidad.text?.toString()?.toDoubleOrNull() ?: 0.0
        val fechaSel = spFecha.text.toString().trim()
        val fechaVSel = spFechaVenc.text.toString().trim()
        val nombre = etNombre.text?.toString()?.trim() ?: ""
        val descripcion = etDescripcion.text?.toString()?.trim() ?: ""
        val moneda = monedaPorCuenta[cuenta] ?: "PEN"

        // limpiar errores visuales
        tilNombre.error = null; tilNombre.isErrorEnabled = false
        tilCuenta.error = null; tilCuenta.isErrorEnabled = false
        tilCantidad.error = null; tilCantidad.isErrorEnabled = false
        tilFecha.error = null; tilFecha.isErrorEnabled = false
        tilVenc.error = null; tilVenc.isErrorEnabled = false

        // validaciones sin espacios extra
        if (nombre.isBlank()) {
            tilNombre.isErrorEnabled = true
            tilNombre.error = "Ingrese un nombre"
            return
        }
        if (cuenta.isBlank()) {
            tilCuenta.isErrorEnabled = true
            tilCuenta.error = "Seleccione una cuenta"
            return
        }
        if (monto <= 0.0) {
            tilCantidad.isErrorEnabled = true
            tilCantidad.error = "Monto mayor a 0"
            return
        }
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        val fechaTs = try { Timestamp(sdf.parse(fechaSel) ?: java.util.Date()) } catch (_: Exception) {
            tilFecha.isErrorEnabled = true
            tilFecha.error = "Formato inválido (yyyy-MM-dd)"
            return
        }
        val fechaVTs = try { Timestamp(sdf.parse(fechaVSel) ?: java.util.Date()) } catch (_: Exception) {
            tilVenc.isErrorEnabled = true
            tilVenc.error = "Formato inválido (yyyy-MM-dd)"
            return
        }

        val accion = "me_prestaron"

        debtRepo.addDebtNested(uid, accion, mapOf(
            "cuenta" to cuenta,
            "moneda" to moneda,
            "monto" to monto,
            "fecha" to fechaTs,
            "fechaVencimiento" to fechaVTs,
            "nombreMeprestaron" to nombre,
            "descripcion" to descripcion,
            "estado" to "activo",
            "from" to "manual"
        )) { success, error ->
            if (!success) {
                Toast.makeText(this, error ?: "Error al guardar la deuda", Toast.LENGTH_SHORT).show()
            }
            val i = android.content.Intent(this, MainActivity::class.java)
            i.putExtra("START_TAB", "DEBTS")
            i.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP or android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP)
            startActivity(i)
            finish()
        }
    }
}
