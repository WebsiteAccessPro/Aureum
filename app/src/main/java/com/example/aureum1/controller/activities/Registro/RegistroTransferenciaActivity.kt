package com.example.aureum1.controller.activities.Registro

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.ArrayAdapter
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import com.example.aureum1.R
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class RegistroTransferenciaActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var etNota: TextInputEditText
    private lateinit var etBeneficiario: TextInputEditText
    private lateinit var etFecha: MaterialAutoCompleteTextView
    private lateinit var etHora: MaterialAutoCompleteTextView
    private lateinit var spFormaPago: MaterialAutoCompleteTextView
    private lateinit var spEstado: MaterialAutoCompleteTextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registro_transferencia)

        // Toolbar y título con monto
        toolbar = findViewById(R.id.toolbarRegistroTransferencia)
        val monto = intent.getStringExtra("MONTO") ?: "0"
        val moneda = intent.getStringExtra("MONEDA") ?: "PEN"
        toolbar.title = "$moneda $monto"
        toolbar.setNavigationOnClickListener { finish() }
        toolbar.setOnMenuItemClickListener { item ->
            if (item.itemId == R.id.action_guardar) {
                val data = Intent().apply {
                    putExtra("NOTA", etNota.text?.toString()?.trim().orEmpty())
                    putExtra("BENEFICIARIO", etBeneficiario.text?.toString()?.trim().orEmpty())
                    putExtra("FORMA_PAGO", spFormaPago.text?.toString()?.trim().orEmpty())
                    putExtra("ESTADO", spEstado.text?.toString()?.trim().orEmpty())
                    putExtra("FECHA", etFecha.text?.toString()?.trim().orEmpty())
                    putExtra("HORA", etHora.text?.toString()?.trim().orEmpty())
                }
                setResult(RESULT_OK, data)
                finish()
                true
            } else false
        }

        // IDs del XML
        etNota = findViewById(R.id.etNota)
        etBeneficiario = findViewById(R.id.etBeneficiario)
        etFecha = findViewById(R.id.etFecha)
        etHora = findViewById(R.id.etHora)
        spFormaPago = findViewById(R.id.spFormaPago)
        spEstado = findViewById(R.id.spEstado)

        // Adapters de los dropdowns
        val formasPago = listOf(
            "Dinero en efectivo",
            "Tarjeta de débito",
            "Tarjeta de crédito",
            "Transferencia bancaria",
            "Cupón",
            "Pago por móvil",
            "Pago por web"
        )
        val estados = listOf("Conciliado", "Procesado", "Pendiente")

        spFormaPago.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, formasPago))
        spEstado.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, estados))

        // Valores por defecto
        // Evitar que se filtre la lista al asignar texto por defecto
        spFormaPago.setText(formasPago.first(), false)
        spEstado.setText(estados.first(), false)

        // Date & Time pickers
        configurarPickers()
    }

    private fun configurarPickers() {
        val cal = Calendar.getInstance()
        // Formatos en español (ej: 11 oct 2025 – 7:28 p. m.)
        val formatoFecha = SimpleDateFormat("dd MMM yyyy", Locale("es", "ES"))
        val formatoHora = SimpleDateFormat("h:mm a", Locale("es", "ES"))

        // Prefill inicial con fecha y hora actuales
        etFecha.setText(formatoFecha.format(cal.time))
        etHora.setText(formatoHora.format(cal.time))

        etFecha.setOnClickListener {
            val y = cal.get(Calendar.YEAR)
            val m = cal.get(Calendar.MONTH)
            val d = cal.get(Calendar.DAY_OF_MONTH)

            DatePickerDialog(this, { _, year, month, day ->
                cal.set(year, month, day)
                etFecha.setText(formatoFecha.format(cal.time))
            }, y, m, d).show()
        }

        etHora.setOnClickListener {
            val h = cal.get(Calendar.HOUR_OF_DAY)
            val min = cal.get(Calendar.MINUTE)

            TimePickerDialog(this, { _, hourOfDay, minute ->
                cal.set(Calendar.HOUR_OF_DAY, hourOfDay)
                cal.set(Calendar.MINUTE, minute)
                etHora.setText(formatoHora.format(cal.time))
            }, h, min, false).show()
        }
    }
}