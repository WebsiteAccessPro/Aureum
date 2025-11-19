package com.example.aureum1.controller.activities

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.aureum1.R
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class EditDeudaActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private lateinit var etNombre: TextInputEditText
    private lateinit var etDescripcion: TextInputEditText
    private lateinit var etCuenta: TextInputEditText
    private lateinit var etCantidad: TextInputEditText
    private lateinit var etFecha: TextInputEditText
    private lateinit var etFechaVenc: TextInputEditText

    private var accion: String = "presto"
    private var debtId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_deuda)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbarEditDeuda)
        toolbar.setNavigationOnClickListener { finish() }
        toolbar.setOnMenuItemClickListener { item ->
            if (item.itemId == R.id.action_check) {
                guardar()
                true
            } else false
        }

        etNombre = findViewById(R.id.etNombre)
        etDescripcion = findViewById(R.id.etDescripcion)
        etCuenta = findViewById(R.id.etCuenta)
        etCantidad = findViewById(R.id.etCantidad)
        etFecha = findViewById(R.id.etFecha)
        etFechaVenc = findViewById(R.id.etFechaVenc)

        accion = intent.getStringExtra("ACCION") ?: "presto"
        debtId = intent.getStringExtra("DEBT_ID") ?: ""

        etFecha.setOnClickListener { mostrarDatePicker { etFecha.setText(it) } }
        etFechaVenc.setOnClickListener { mostrarDatePicker { etFechaVenc.setText(it) } }

        cargar()
    }

    private fun cargar() {
        val uid = auth.currentUser?.uid ?: return
        if (debtId.isBlank()) {
            android.widget.Toast.makeText(this, "ID de deuda inválido", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        db.collection("accounts").document(uid)
            .collection("deudas").document(accion)
            .collection("items").document(debtId)
            .get()
            .addOnSuccessListener { doc ->
                try {
                    if (!doc.exists()) {
                        android.widget.Toast.makeText(this, "Deuda no encontrada", android.widget.Toast.LENGTH_SHORT).show()
                        return@addOnSuccessListener
                    }
                    val nombre = (doc.getString("nombrePresto")
                        ?: doc.getString("nombreMeprestaron")
                        ?: doc.getString("nombre")
                        ?: "")
                    val descripcion = doc.getString("descripcion") ?: ""
                    val cuenta = doc.getString("cuenta") ?: ""
                    val montoRaw = doc.get("monto")
                    val monto = when (montoRaw) {
                        is Number -> montoRaw.toDouble()
                        is String -> montoRaw.toDoubleOrNull() ?: 0.0
                        else -> 0.0
                    }
                    val fechaAny = doc.get("fecha")
                    val vencAny = doc.get("fechaVencimiento")
                    val fmt = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                    val fechaStr = when (fechaAny) {
                        is com.google.firebase.Timestamp -> fmt.format(fechaAny.toDate())
                        is String -> fechaAny
                        else -> ""
                    }
                    val vencStr = when (vencAny) {
                        is com.google.firebase.Timestamp -> fmt.format(vencAny.toDate())
                        is String -> vencAny
                        else -> ""
                    }

                    etNombre.setText(nombre)
                    etDescripcion.setText(descripcion)
                    etCuenta.setText(cuenta)
                    etCantidad.setText(monto.toString())
                    etFecha.setText(fechaStr)
                    etFechaVenc.setText(vencStr)
                } catch (e: Exception) {
                    android.widget.Toast.makeText(this, e.message ?: "Error al cargar datos", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                android.widget.Toast.makeText(this, e.message ?: "Error de conexión", android.widget.Toast.LENGTH_SHORT).show()
            }
    }

    private fun guardar() {
        val uid = auth.currentUser?.uid ?: return
        val data = mutableMapOf<String, Any>()
        val nombreEdit = etNombre.text?.toString()?.trim().orEmpty()
        if (accion == "presto") {
            data["nombrePresto"] = nombreEdit
            data["nombre"] = nombreEdit
        } else {
            data["nombreMeprestaron"] = nombreEdit
            data["nombre"] = nombreEdit
        }
        data["descripcion"] = etDescripcion.text?.toString()?.trim().orEmpty()

        val f = etFecha.text?.toString()?.trim().orEmpty()
        if (f.isNotBlank()) {
            val parsed = parseFechaToTimestamp(f)
            if (parsed != null) data["fecha"] = parsed
        }
        val fv = etFechaVenc.text?.toString()?.trim().orEmpty()
        if (fv.isNotBlank()) {
            val parsedV = parseFechaToTimestamp(fv)
            if (parsedV != null) data["fechaVencimiento"] = parsedV
        }

        db.collection("accounts").document(uid)
            .collection("deudas").document(accion)
            .collection("items").document(debtId)
            .update(data)
            .addOnSuccessListener { finish() }
            .addOnFailureListener { e ->
                android.widget.Toast.makeText(this, e.message ?: "Error al guardar", android.widget.Toast.LENGTH_SHORT).show()
            }
    }

    private fun parseFechaToTimestamp(s: String): com.google.firebase.Timestamp? {
        val formats = listOf(
            java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()),
            java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        )
        for (f in formats) {
            try {
                val d = f.parse(s)
                if (d != null) return com.google.firebase.Timestamp(d)
            } catch (_: Exception) {}
        }
        return null
    }

    private fun mostrarDatePicker(onPick: (String) -> Unit) {
        val cal = java.util.Calendar.getInstance()
        DatePickerDialog(this, { _, y, m, d ->
            val s = "%04d-%02d-%02d".format(y, m + 1, d)
            onPick(s)
        }, cal.get(java.util.Calendar.YEAR), cal.get(java.util.Calendar.MONTH), cal.get(java.util.Calendar.DAY_OF_MONTH)).show()
    }
}