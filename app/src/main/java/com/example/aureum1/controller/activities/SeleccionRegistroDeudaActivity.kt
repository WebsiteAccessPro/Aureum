package com.example.aureum1.controller.activities

import android.app.Activity
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.aureum1.R
import com.example.aureum1.controller.adapters.SeleccionRegistroAdapter
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class SeleccionRegistroDeudaActivity : AppCompatActivity() {

    private lateinit var adapter: SeleccionRegistroAdapter
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.seleccionar_registro)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbarSeleccionarRegistro)
        toolbar.setNavigationOnClickListener { finish() }

        val rv = findViewById<RecyclerView>(R.id.rvSeleccionarRegistro)
        rv.layoutManager = LinearLayoutManager(this)
        adapter = SeleccionRegistroAdapter(emptyList()) { item ->
            // devolver resultado al fragmento que invocó
            val data = intent
            data.putExtra("SELECTED_RECORD_ID", item.id)
            data.putExtra("SELECTED_RECORD_TIPO", item.tipo)
            data.putExtra("SELECTED_RECORD_CUENTA", item.cuenta)
            data.putExtra("SELECTED_RECORD_MONTO", item.monto)
            data.putExtra("SELECTED_RECORD_MONEDA", item.moneda)
            data.putExtra("SELECTED_RECORD_FECHA_TEXTO", item.fechaTexto)
            setResult(Activity.RESULT_OK, data)
            finish()
        }
        rv.adapter = adapter

        cargarRegistros()
    }

    private fun cargarRegistros() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("accounts").document(uid)
            .collection("registros")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snaps ->
                val list = snaps.documents.map { doc ->
                    val m = doc.data ?: emptyMap<String, Any?>()
                    val tipo = (m["tipo"] as? String).orEmpty()
                    val cuenta = (m["cuentaOrigen"] as? String)
                        ?: (m["cuentaDestino"] as? String).orEmpty()
                    val nota = (m["nota"] as? String).orEmpty()
                    val monto = (m["monto"] as? Number)?.toDouble() ?: 0.0
                    val moneda = (m["moneda"] as? String).orEmpty()
                    val fechaTexto = (m["fechaTexto"] as? String) ?: "" // usado en varios layouts
                    SeleccionRegistroAdapter.RecordItem(
                        id = doc.id,
                        titulo = tipo,
                        cuenta = cuenta,
                        descripcion = nota,
                        monto = monto,
                        moneda = moneda,
                        fechaTexto = fechaTexto,
                        tipo = tipo
                    )
                }
                adapter.submit(list)
            }
    }
}