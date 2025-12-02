package com.example.aureum1.controller.activities

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.aureum1.R
import com.example.aureum1.controller.adapters.DebtAdapter
import com.example.aureum1.controller.adapters.DebtClosedAdapter
import com.example.aureum1.model.repository.AccountRepository
import com.example.aureum1.model.repository.DebtRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration

//Buscar para la seccion deudas activo/cerrado
class DebtSearchActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_SOURCE = "SOURCE" // "activo" | "cerrado"
    }

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val accountRepo by lazy { AccountRepository() }
    private val repo by lazy { DebtRepository() }

    private lateinit var etBuscar: EditText
    private lateinit var rvResultados: RecyclerView
    private lateinit var btnCerrar: ImageView

    private var adapterActivas: DebtAdapter? = null
    private var adapterCerradas: DebtClosedAdapter? = null

    private var listenerPresto: ListenerRegistration? = null
    private var listenerMP: ListenerRegistration? = null
    private var allItems: List<Map<String, Any?>> = emptyList()
    private var source: String = "activo"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        etBuscar = findViewById(R.id.etBuscar)
        rvResultados = findViewById(R.id.rvResultados)
        btnCerrar = findViewById(R.id.btnCerrar)
        rvResultados.layoutManager = LinearLayoutManager(this)

        btnCerrar.setOnClickListener { finish() }

        source = intent.getStringExtra(EXTRA_SOURCE) ?: "activo"
        val uid = auth.currentUser?.uid ?: return

        if (source == "cerrado") {
            adapterCerradas = DebtClosedAdapter(emptyList(), onEliminar = null, onItemClick = null)
            rvResultados.adapter = adapterCerradas
            listenerPresto?.remove(); listenerMP?.remove()
            listenerPresto = repo.subscribeByAction(uid, "presto", estado = "cerrado") { lista ->
                val withAccion = lista.map { it.toMutableMap().apply { put("accion", "presto") } }
                mergeClosed(withAccion, null)
            }
            listenerMP = repo.subscribeByAction(uid, "me_prestaron", estado = "cerrado") { lista ->
                val withAccion = lista.map { it.toMutableMap().apply { put("accion", "me_prestaron") } }
                mergeClosed(null, withAccion)
            }
        } else {
            adapterActivas = DebtAdapter(
                emptyList(),
                emptyMap(),
                "presto",
                onAddRegistroClick = null,
                onItemClick = null,
                showAddCta = false
            )
            rvResultados.adapter = adapterActivas
            accountRepo.subscribeAccountsInfoByName(uid) { info ->
                adapterActivas?.updateAccountsInfo(info)
            }
            listenerPresto?.remove(); listenerMP?.remove()
            listenerPresto = repo.subscribeByAction(uid, "presto", estado = "activo") { lista ->
                val withAccion = lista.map { it.toMutableMap().apply { put("accion", "presto") } }
                mergeActive(withAccion, null)
            }
            listenerMP = repo.subscribeByAction(uid, "me_prestaron", estado = "activo") { lista ->
                val withAccion = lista.map { it.toMutableMap().apply { put("accion", "me_prestaron") } }
                mergeActive(null, withAccion)
            }
        }

        etBuscar.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val q = s?.toString()?.trim()?.lowercase().orEmpty()
                applyFilter(q)
            }
        })
    }

    private fun mergeActive(presto: List<Map<String, Any?>>?, mp: List<Map<String, Any?>>?) {
        val p = presto ?: allItems.filter { (it["accion"] as? String) == "presto" }
        val m = mp ?: allItems.filter { (it["accion"] as? String) == "me_prestaron" }
        allItems = (p + m).sortedByDescending {
            val ts = it["fecha"]
            if (ts is com.google.firebase.Timestamp) ts.toDate().time else 0L
        }
        adapterActivas?.submitList(allItems)
    }

    private fun mergeClosed(presto: List<Map<String, Any?>>?, mp: List<Map<String, Any?>>?) {
        val p = presto ?: allItems.filter { (it["accion"] as? String) == "presto" }
        val m = mp ?: allItems.filter { (it["accion"] as? String) == "me_prestaron" }
        allItems = (p + m).sortedByDescending {
            val ts = it["fecha"]
            if (ts is com.google.firebase.Timestamp) ts.toDate().time else 0L
        }
        adapterCerradas?.submitList(allItems)
    }

    private fun applyFilter(q: String) {
        val filtrados = if (q.isEmpty()) allItems else allItems.filter { m ->
            val accion = (m["accion"] as? String).orEmpty()
            val nombre = when (accion) {
                "presto" -> (m["nombrePresto"] as? String).orEmpty()
                "me_prestaron" -> (m["nombreMeprestaron"] as? String).orEmpty()
                else -> (m["nombre"] as? String).orEmpty()
            }
            nombre.lowercase().contains(q)
        }
        if (source == "cerrado") {
            adapterCerradas?.submitList(filtrados)
        } else {
            adapterActivas?.submitList(filtrados)
        }
    }

    override fun onDestroy() {
        listenerPresto?.remove(); listenerPresto = null
        listenerMP?.remove(); listenerMP = null
        super.onDestroy()
    }
}
