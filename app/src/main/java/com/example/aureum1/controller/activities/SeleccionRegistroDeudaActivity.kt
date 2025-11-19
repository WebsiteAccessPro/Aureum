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
    private var defaultAccion: String = "presto"
    private var filterSource: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.seleccionar_registro)
        defaultAccion = intent.getStringExtra("DEFAULT_ACCION")
            ?: intent.getStringExtra(com.example.aureum1.controller.activities.WalletSelectRecordActivity.EXTRA_DEFAULT_ACCION)
            ?: "presto"
        filterSource = intent.getStringExtra("FILTER_SOURCE").orEmpty()

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
            data.putExtra("SELECTED_RECORD_FECHA_ISO", item.fechaIso)
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
                val list = snaps.documents.mapNotNull { doc ->
                    val m = doc.data ?: emptyMap<String, Any?>()
                    val linked = (m["debtLinked"] as? Boolean) == true
                    if (linked) return@mapNotNull null
                    val tipo = (m["tipo"] as? String).orEmpty()
                    val cuenta = (m["cuentaOrigen"] as? String)
                        ?: (m["cuentaDestino"] as? String).orEmpty()
                    val nota = (m["nota"] as? String).orEmpty()
                    val monto = (m["monto"] as? Number)?.toDouble() ?: 0.0
                    val moneda = (m["moneda"] as? String).orEmpty()
                    val fechaTexto = (m["fechaTexto"] as? String) ?: ""
                    val fechaIso = run {
                        val f = m["fecha"]
                        when (f) {
                            is com.google.firebase.Timestamp -> {
                                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                                sdf.format(f.toDate())
                            }
                            is String -> {
                                val s = f.trim()
                                val isoRegex = Regex("\\d{4}-\\d{2}-\\d{2}")
                                if (isoRegex.matches(s)) s else parseFechaTextoToIso(s)
                            }
                            else -> parseFechaTextoToIso(fechaTexto)
                        }
                    }
                    val shouldInclude = if (filterSource.equals("wallet", true)) {
                        if (defaultAccion == "presto") tipo.equals("Gasto", true) else tipo.equals("Ingreso", true)
                    } else true
                    if (!shouldInclude) return@mapNotNull null
                    SeleccionRegistroAdapter.RecordItem(
                        id = doc.id,
                        titulo = tipo,
                        cuenta = cuenta,
                        descripcion = nota,
                        monto = monto,
                        moneda = moneda,
                        fechaTexto = fechaTexto,
                        tipo = tipo,
                        fechaIso = fechaIso
                    )
                }
                adapter.submit(list)
            }
    }

    private fun parseFechaTextoToIso(texto: String): String {
        val s = texto.replace(".", "").trim()
        val locales = listOf(java.util.Locale("es", "ES"), java.util.Locale.getDefault())
        val patterns = listOf("dd MMM yyyy", "yyyy-MM-dd")
        for (loc in locales) {
            for (p in patterns) {
                try {
                    val sdf = java.text.SimpleDateFormat(p, loc)
                    val d = sdf.parse(s)
                    if (d != null) {
                        val out = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                        return out.format(d)
                    }
                } catch (_: Exception) { }
            }
        }
        val out = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        return out.format(java.util.Date())
    }
}