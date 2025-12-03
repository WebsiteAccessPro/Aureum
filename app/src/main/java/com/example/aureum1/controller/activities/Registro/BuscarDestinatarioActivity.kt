package com.example.aureum1.controller.activities.Registro

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.aureum1.R
import com.example.aureum1.controller.adapters.CuentaAdapter
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Buscar destinatario por teléfono y seleccionar una de sus cuentas para depositar.
 */
class BuscarDestinatarioActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var tilPhone: TextInputLayout
    private lateinit var etPhone: TextInputEditText
    private lateinit var btnBuscar: Button
    private lateinit var cardUser: CardView
    private lateinit var tvNombre: TextView
    private lateinit var tvTelefono: TextView
    private lateinit var rvCuentas: RecyclerView
    private lateinit var adapter: CuentaAdapter
    private val cuentas = mutableListOf<Map<String, Any?>>()

    private val db by lazy { FirebaseFirestore.getInstance() }
    private var foundUid: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_buscar_destinatario)

        toolbar = findViewById(R.id.toolbarBuscarDestinatario)
        tilPhone = findViewById(R.id.tilPhone)
        etPhone = findViewById(R.id.etPhone)
        btnBuscar = findViewById(R.id.btnBuscarUsuario)
        cardUser = findViewById(R.id.cardUsuario)
        tvNombre = findViewById(R.id.tvNombreUsuario)
        tvTelefono = findViewById(R.id.tvTelefonoUsuario)
        rvCuentas = findViewById(R.id.recyclerCuentasUsuario)

        toolbar.setNavigationOnClickListener { finish() }
        cardUser.visibility = View.GONE
        rvCuentas.visibility = View.GONE

        rvCuentas.layoutManager = LinearLayoutManager(this)
        adapter = CuentaAdapter(items = cuentas, onOpcionesClick = { cuenta ->
            val nombreCuenta = cuenta["nombre"] as? String ?: ""
            if (nombreCuenta.isNotEmpty()) {
                val intent = android.content.Intent()
                intent.putExtra("CUENTA_SELECCIONADA", nombreCuenta)
                // Propagar el UID del usuario encontrado para transferencias externas
                intent.putExtra("DEST_UID", foundUid)
                setResult(Activity.RESULT_OK, intent)
                finish()
            }
        })
        rvCuentas.adapter = adapter

        btnBuscar.setOnClickListener { buscarPorTelefono() }
    }

    private fun buscarPorTelefono() {
        val raw = etPhone.text?.toString()?.trim().orEmpty()
        val phoneDigits = raw.filter { it.isDigit() }
        if (phoneDigits.isEmpty()) {
            tilPhone.error = "Ingresa un número válido"
            return
        }
        tilPhone.error = null

        db.collection("users")
            .whereEqualTo("phone", phoneDigits)
            .limit(1)
            .get()
            .addOnSuccessListener { snap ->
                if (!snap.isEmpty) {
                    val doc = snap.documents.first()
                    handleUserFound(doc)
                } else {
                    val asLong = phoneDigits.toLongOrNull()
                    if (asLong == null) {
                        showUserNotFound()
                        return@addOnSuccessListener
                    }
                    db.collection("users")
                        .whereEqualTo("phone", asLong)
                        .limit(1)
                        .get()
                        .addOnSuccessListener { snapNum ->
                            if (snapNum.isEmpty) {
                                showUserNotFound()
                            } else {
                                val doc = snapNum.documents.first()
                                handleUserFound(doc)
                            }
                        }
                        .addOnFailureListener { e -> showError(e)
                        }
                }
            }
            .addOnFailureListener { e -> showError(e) }
    }

    private fun handleUserFound(doc: com.google.firebase.firestore.DocumentSnapshot) {
        foundUid = doc.id
        val fullName = doc.getString("fullName").orEmpty()
        val telefono = (doc.get("phone")?.toString()).orEmpty()
        tvNombre.text = fullName
        tvTelefono.text = telefono
        cardUser.visibility = View.VISIBLE
        cargarCuentasUsuario(foundUid!!)
        rvCuentas.visibility = View.VISIBLE
    }

    private fun showUserNotFound() {
        cardUser.visibility = View.GONE
        rvCuentas.visibility = View.GONE
        foundUid = null
        com.google.android.material.snackbar.Snackbar
            .make(tilPhone, "Usuario no encontrado", com.google.android.material.snackbar.Snackbar.LENGTH_SHORT)
            .show()
    }

    private fun showError(e: Exception) {
        com.google.android.material.snackbar.Snackbar
            .make(tilPhone, "Error: ${e.message}", com.google.android.material.snackbar.Snackbar.LENGTH_SHORT)
            .show()
    }

    private fun cargarCuentasUsuario(uid: String) {
        db.collection("accounts").document(uid)
            .get()
            .addOnSuccessListener { doc ->
                val arr = doc.get("cuentas") as? List<Map<String, Any?>> ?: emptyList()
                cuentas.clear()
                cuentas.addAll(arr.map { cuenta ->
                    // Mascara número si existe
                    val numero = (cuenta["numero"] as? String).orEmpty()
                    val masked = if (numero.length >= 4) "**** ${numero.takeLast(4)}" else numero
                    val m = cuenta.toMutableMap()
                    m["tipo"] = masked // reutilizamos tvTipo para mostrar máscara
                    m
                })
                adapter.notifyDataSetChanged()
                rvCuentas.visibility = if (cuentas.isEmpty()) View.GONE else View.VISIBLE
            }
            .addOnFailureListener { e ->
                com.google.android.material.snackbar.Snackbar
                    .make(tilPhone, "Error cargando cuentas: ${e.message}", com.google.android.material.snackbar.Snackbar.LENGTH_SHORT)
                    .show()
            }
    }
}
