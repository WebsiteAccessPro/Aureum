package com.example.aureum1.controller.fragments

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.aureum1.R
import com.example.aureum1.controller.adapters.AccountAdapter
import com.example.aureum1.controller.activities.AjustesCuentaActivity
import com.example.aureum1.controller.activities.NuevaCuentaActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import com.example.aureum1.model.repository.AccountRepository

class AccountsFragment : Fragment(R.layout.fragment_accounts) {

    private lateinit var adapter: AccountAdapter
    private var listener: ListenerRegistration? = null

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val repo by lazy { AccountRepository() }
    private val TAG = "AccountsFragment"

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val rv = view.findViewById<RecyclerView>(R.id.rvAccounts)
        val btnSettings = view.findViewById<ImageButton?>(R.id.btnAccountsSettings)

        btnSettings?.setOnClickListener {
            startActivity(Intent(requireContext(), AjustesCuentaActivity::class.java))
        }

        rv.layoutManager = LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false)
        rv.setHasFixedSize(true)

        adapter = AccountAdapter(
            items = emptyList(),
            onAccountClick = { /* abrir detalle si quieres */ },
            onAddClick = {
                startActivity(
                    Intent(
                        requireContext(),
                        NuevaCuentaActivity::class.java
                    )
                )
            }
        )
        rv.adapter = adapter

        suscribirCuentas()
    }

    private fun suscribirCuentas() {
        val uid = auth.currentUser?.uid ?: return
        listener?.remove()
        listener = repo.subscribeAccounts(uid) { listaConAdd ->
            adapter.submitList(listaConAdd)
        }
    }

    override fun onDestroyView() {
        listener?.remove()
        listener = null
        super.onDestroyView()
    }
}