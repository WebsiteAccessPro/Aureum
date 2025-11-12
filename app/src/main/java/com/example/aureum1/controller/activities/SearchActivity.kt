package com.example.aureum1.controller.activities

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import android.widget.EditText
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.ConcatAdapter
import com.example.aureum1.R
import com.example.aureum1.controller.adapters.RegistroAdapter
import com.google.firebase.auth.FirebaseAuth
import com.example.aureum1.model.repository.AccountRepository
import com.example.aureum1.model.repository.RecordRepository
import java.text.Normalizer

class SearchActivity : AppCompatActivity() {
    private val auth by lazy { FirebaseAuth.getInstance() }
    // Repositorios MVC
    private val accountRepo by lazy { AccountRepository() }
    private val recordRepo by lazy { RecordRepository() }

    private lateinit var etBuscar: EditText
    private lateinit var rvResultados: RecyclerView
    private lateinit var adapterLatest: RegistroAdapter
    private lateinit var adapterResults: RegistroAdapter
    private lateinit var headerRecentAdapter: HeaderAdapter
    private lateinit var recentSearchAdapter: RecentSearchAdapter
    private lateinit var dividerAdapter: DividerAdapter
    private lateinit var headerLatestAdapter: HeaderAdapter
    private lateinit var headerResultsAdapter: HeaderAdapter

    private var allRegistrosProcesados: List<Map<String, Any?>> = emptyList()
    private var latestFive: List<Map<String, Any?>> = emptyList()
    private var recentQueries: MutableList<String> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        etBuscar = findViewById(R.id.etBuscar)
        rvResultados = findViewById(R.id.rvResultados)
        val btnCerrar = findViewById<ImageView>(R.id.btnCerrar)
        btnCerrar.setOnClickListener { finish() }

        adapterLatest = RegistroAdapter(emptyList())
        adapterResults = RegistroAdapter(emptyList())

        // Adapters para encabezados y secciones
        headerRecentAdapter = HeaderAdapter("Búsquedas recientes")
        recentSearchAdapter = RecentSearchAdapter(recentQueries) { q ->
            etBuscar.setText(q)
            etBuscar.setSelection(q.length)
            ejecutarBusqueda(q)
        }
        dividerAdapter = DividerAdapter()
        headerLatestAdapter = HeaderAdapter("Últimos agregados")
        headerResultsAdapter = HeaderAdapter("")

        rvResultados.layoutManager = LinearLayoutManager(this)

        val uid = auth.currentUser?.uid ?: return

        // Info de cuentas centralizada
        accountRepo.subscribeAccountsInfoByName(uid) { infoPorNombre ->
            adapterLatest.updateAccountsInfo(infoPorNombre)
            adapterResults.updateAccountsInfo(infoPorNombre)
        }
        // Cargar registros una sola vez (para búsqueda y últimos)
        recordRepo.fetchAllOnceRaw(uid, onSuccess = { list ->
            allRegistrosProcesados = list
            latestFive = list.take(5)
            adapterLatest.submitList(latestFive)
            mostrarSeccionInicial()
        }) { /* onError */ _ ->
            allRegistrosProcesados = emptyList()
            latestFive = emptyList()
            mostrarSeccionInicial()
        }

        etBuscar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val qRaw = (s?.toString() ?: "").trim()
                if (qRaw.isEmpty()) {
                    mostrarSeccionInicial()
                } else {
                    ejecutarBusqueda(qRaw)
                }
            }
        })

        // Accionar búsqueda explícita al pulsar el teclado en acción Search
        etBuscar.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val term = etBuscar.text?.toString()?.trim().orEmpty()
                if (term.isNotEmpty()) {
                    agregarBusquedaReciente(term)
                    ejecutarBusqueda(term)
                }
                true
            } else false
        }
    }

    private fun mostrarSeccionInicial() {
        // Construir ConcatAdapter: (si hay historial) Búsquedas recientes + Divider + Últimos agregados
        val adapters = mutableListOf<RecyclerView.Adapter<out RecyclerView.ViewHolder>>()
        if (recentQueries.isNotEmpty()) {
            headerRecentAdapter = HeaderAdapter("Búsquedas recientes")
            recentSearchAdapter = RecentSearchAdapter(recentQueries) { q ->
                etBuscar.setText(q)
                etBuscar.setSelection(q.length)
                ejecutarBusqueda(q)
            }
            adapters.add(headerRecentAdapter)
            adapters.add(recentSearchAdapter)
            adapters.add(dividerAdapter)
        }
        adapters.add(headerLatestAdapter)
        adapters.add(adapterLatest)
        rvResultados.adapter = ConcatAdapter(adapters)
    }

    private fun ejecutarBusqueda(qRaw: String) {
        val query = normalize(qRaw)
        val filtrados = allRegistrosProcesados.filter { reg ->
            val tipo = (reg["tipo"] as? String).orEmpty()
            val categoria = (reg["categoria"] as? String).orEmpty()
            val tipoLower = tipo.lowercase()

            val prefijoIngresoPlural = "Ingresos por "
            val prefijoIngresoSingular = "Ingreso por "
            val prefijoGastoPlural = "Gastos por "
            val prefijoGastoSingular = "Gasto por "

            val displayCategoria = when (tipoLower) {
                "ingreso" -> "$prefijoIngresoPlural$categoria"
                "gasto" -> "$prefijoGastoPlural$categoria"
                else -> categoria
            }
            val displayCategoriaAlt = when (tipoLower) {
                "ingreso" -> "$prefijoIngresoSingular$categoria"
                "gasto" -> "$prefijoGastoSingular$categoria"
                else -> categoria
            }

            fun contains(field: String): Boolean {
                val v = normalize((reg[field] as? String).orEmpty())
                return v.contains(query)
            }

            contains("categoria") ||
            contains("tipo") ||
            contains("cuentaOrigen") ||
            contains("cuentaDestino") ||
            contains("nota") ||
            contains("pagador") ||
            contains("beneficiario") ||
            contains("formaPago") ||
            contains("estado") ||
            contains("moneda") ||
            normalize(displayCategoria).contains(query) ||
            normalize(displayCategoriaAlt).contains(query) ||
            normalize("$tipo por $categoria").contains(query)
        }

        adapterResults.submitList(filtrados)
        headerResultsAdapter = HeaderAdapter("Resultados para: $qRaw")

        val adapters = mutableListOf<RecyclerView.Adapter<out RecyclerView.ViewHolder>>()
        if (recentQueries.isNotEmpty()) {
            adapters.add(headerRecentAdapter)
            adapters.add(recentSearchAdapter)
        }
        adapters.add(headerResultsAdapter)
        adapters.add(adapterResults)
        adapters.add(dividerAdapter)
        adapters.add(headerLatestAdapter)
        adapters.add(adapterLatest)
        rvResultados.adapter = ConcatAdapter(adapters)
    }

    private fun agregarBusquedaReciente(term: String) {
        // Persistir historial en SharedPreferences, manteniendo orden y evitando duplicados
        val prefs = getSharedPreferences("aureum_prefs", MODE_PRIVATE)
        val key = "search_history_list"
        val stored = prefs.getString(key, "") ?: ""
        val current = if (stored.isBlank()) mutableListOf<String>() else stored.split("|").toMutableList()
        current.removeAll { normalize(it) == normalize(term) }
        current.add(0, term)
        // Limitar a 10 términos
        while (current.size > 10) current.removeAt(current.size - 1)
        prefs.edit().putString(key, current.joinToString("|")) .apply()
        recentQueries = current
        recentSearchAdapter.update(recentQueries)
    }

    override fun onResume() {
        super.onResume()
        // Cargar historial al abrir la actividad
        val prefs = getSharedPreferences("aureum_prefs", MODE_PRIVATE)
        val stored = prefs.getString("search_history_list", "") ?: ""
        recentQueries = if (stored.isBlank()) mutableListOf() else stored.split("|").toMutableList()
        recentSearchAdapter.update(recentQueries)
        mostrarSeccionInicial()
    }

    // Adaptador simple de encabezado
    inner class HeaderAdapter(private val title: String) : RecyclerView.Adapter<HeaderVH>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HeaderVH {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_header, parent, false)
            return HeaderVH(v)
        }
        override fun getItemCount(): Int = 1
        override fun onBindViewHolder(holder: HeaderVH, position: Int) { holder.text.text = title }
    }
    inner class HeaderVH(view: View) : RecyclerView.ViewHolder(view) { val text: TextView = view.findViewById(R.id.tvHeader) }

    // Separador visual
    inner class DividerAdapter : RecyclerView.Adapter<DividerVH>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DividerVH {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_divider, parent, false)
            return DividerVH(v)
        }
        override fun getItemCount(): Int = 1
        override fun onBindViewHolder(holder: DividerVH, position: Int) {}
    }
    inner class DividerVH(view: View) : RecyclerView.ViewHolder(view)

    // Historial de búsquedas recientes
    inner class RecentSearchAdapter(private var items: List<String>, val onClick: (String) -> Unit) : RecyclerView.Adapter<RecentVH>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecentVH {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_recent_search, parent, false)
            return RecentVH(v)
        }
        override fun getItemCount(): Int = items.size
        override fun onBindViewHolder(holder: RecentVH, position: Int) {
            val term = items[position]
            holder.text.text = term
            holder.itemView.setOnClickListener { onClick(term) }
        }
        fun update(newItems: List<String>) { items = newItems; notifyDataSetChanged() }
    }
    inner class RecentVH(view: View) : RecyclerView.ViewHolder(view) { val text: TextView = view.findViewById(R.id.tvRecentSearch) }

    private fun normalize(input: String): String {
        val nfd = Normalizer.normalize(input, Normalizer.Form.NFD)
        return nfd.replace("\\p{M}+".toRegex(), "").lowercase()
    }
}