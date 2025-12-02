package com.example.aureum1.controller.activities.Registro

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.aureum1.R
import com.example.aureum1.controller.adapters.CuentaAdapter
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Selección de cuenta destino para Transferencias.
 * Lista "Mis cuentas" y ofrece el botón "Enviar a otro usuario" que abre BuscarDestinatarioActivity.
 * No modifica el XML original usado por otras secciones.
 */
class SeleccionCuentaExternaActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var rv: RecyclerView
    private lateinit var adapter: CuentaAdapter
    private val cuentas = mutableListOf<Map<String, Any?>>()
    private lateinit var fabOtroUsuario: ExtendedFloatingActionButton

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_seleccion_cuenta_externa)

        toolbar = findViewById(R.id.toolbarSeleccionCuentaExterna)
        rv = findViewById(R.id.recyclerSeleccionCuentaExterna)
        fabOtroUsuario = findViewById(R.id.fabEnviarOtroUsuario)

        toolbar.setNavigationOnClickListener { finish() }

        rv.layoutManager = LinearLayoutManager(this)
        rv.setHasFixedSize(true)

        adapter = CuentaAdapter(
            items = cuentas,
            onOpcionesClick = { cuenta ->
                val nombreCuenta = cuenta["nombre"] as? String ?: ""
                if (nombreCuenta.isNotEmpty()) {
                    val resultIntent = Intent()
                    resultIntent.putExtra("CUENTA_SELECCIONADA", nombreCuenta)
                    setResult(Activity.RESULT_OK, resultIntent)
                    finish()
                }
            }
        )
        rv.adapter = adapter

        fabOtroUsuario.setOnClickListener {
            val i = Intent(this, BuscarDestinatarioActivity::class.java)
            startActivityForResult(i, REQ_BUSCAR_DESTINATARIO)
        }

        cargarMisCuentas()
    }

    private fun cargarMisCuentas() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("accounts").document(uid)
            .get()
            .addOnSuccessListener { snap ->
                val arr = snap.get("cuentas") as? List<Map<String, Any?>> ?: emptyList()
                cuentas.clear()
                cuentas.addAll(arr)
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener {
                // opcional: mostrar error
            }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && data != null) {
            if (requestCode == REQ_BUSCAR_DESTINATARIO) {
                val cuentaDestino = data.getStringExtra("CUENTA_SELECCIONADA")
                val destUid = data.getStringExtra("DEST_UID")
                if (!cuentaDestino.isNullOrEmpty()) {
                    val resultIntent = Intent()
                    resultIntent.putExtra("CUENTA_SELECCIONADA", cuentaDestino)
                    if (!destUid.isNullOrEmpty()) {
                        resultIntent.putExtra("DEST_UID", destUid)
                    }
                    setResult(Activity.RESULT_OK, resultIntent)
                    finish()
                }
            }
        }
    }

    companion object {
        private const val REQ_BUSCAR_DESTINATARIO = 910
    }
}
