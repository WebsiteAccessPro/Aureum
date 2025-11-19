package com.example.aureum1.controller.fragments

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.aureum1.R
import com.example.aureum1.controller.adapters.DebtClosedAdapter
import com.example.aureum1.model.repository.DebtRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration

class DebtsClosedFragment : Fragment(R.layout.fragment_debts_tabcerrado) {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val repo by lazy { DebtRepository() }

    private lateinit var rvCerradas: RecyclerView
    private lateinit var adapter: DebtClosedAdapter

    private var listenerPresto: ListenerRegistration? = null
    private var listenerMePrestaron: ListenerRegistration? = null
    private var listaPresto: List<Map<String, Any?>> = emptyList()
    private var listaMePrestaron: List<Map<String, Any?>> = emptyList()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<TextView>(R.id.tabActivo)?.setOnClickListener {
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, DebtsFragment(), "DEBTS")
                .commit()
        }
        view.findViewById<TextView>(R.id.tabCerrado)?.setOnClickListener { }

        rvCerradas = view.findViewById(R.id.rvDeudasCerradas)
        val tvEmpty = view.findViewById<TextView>(R.id.tvEmptyClosed)
        rvCerradas.layoutManager = LinearLayoutManager(requireContext())
        rvCerradas.setHasFixedSize(false)
        rvCerradas.isNestedScrollingEnabled = false

        adapter = DebtClosedAdapter(emptyList(), onEliminar = { acc, item ->
            val uid = auth.currentUser?.uid ?: return@DebtClosedAdapter
            val id = (item["_id"] as? String).orEmpty()
            repo.deleteDebtAndUnlink(uid, acc, id) { ok, err ->
                if (!ok) {
                    android.widget.Toast.makeText(requireContext(), err ?: "Error al eliminar deuda", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }, onItemClick = { acc, item ->
            val i = android.content.Intent(requireContext(), com.example.aureum1.controller.activities.RegistrosDeudaActivity::class.java)
            i.putExtra("ACCION", acc)
            i.putExtra("DEBT_ID", (item["_id"] as? String).orEmpty())
            i.putExtra("SOURCE", "cerrado")
            startActivity(i)
        })
        rvCerradas.adapter = adapter
        view.findViewById<android.widget.ImageView>(R.id.btnBuscarDeuda)?.setOnClickListener {
            val i = android.content.Intent(requireContext(), com.example.aureum1.controller.activities.DebtSearchActivity::class.java)
            i.putExtra(com.example.aureum1.controller.activities.DebtSearchActivity.EXTRA_SOURCE, "cerrado")
            startActivity(i)
        }

        val uid = auth.currentUser?.uid ?: return
        listenerPresto?.remove()
        listenerMePrestaron?.remove()
        listenerPresto = repo.subscribeByAction(uid, "presto", estado = "cerrado") { lista ->
            listaPresto = lista.map { it.toMutableMap().apply { put("accion", "presto") } }
            mergeAndRender()
        }
        listenerMePrestaron = repo.subscribeByAction(uid, "me_prestaron", estado = "cerrado") { lista ->
            listaMePrestaron = lista.map { it.toMutableMap().apply { put("accion", "me_prestaron") } }
            mergeAndRender()
        }
    }

    override fun onDestroyView() {
        listenerPresto?.remove(); listenerPresto = null
        listenerMePrestaron?.remove(); listenerMePrestaron = null
        super.onDestroyView()
    }

    private fun mergeAndRender() {
        val combined = (listaPresto + listaMePrestaron).sortedByDescending {
            val ts = it["fecha"]
            if (ts is com.google.firebase.Timestamp) ts.toDate().time else 0L
        }
        adapter.submitList(combined)
        view?.findViewById<TextView>(R.id.tvEmptyClosed)?.visibility = if (combined.isEmpty()) View.VISIBLE else View.GONE
    }
}