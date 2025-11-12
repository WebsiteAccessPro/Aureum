package com.example.aureum1.controller.activities

import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.aureum1.R
import com.example.aureum1.controller.adapters.RegistroSectionAdapter
import com.google.firebase.auth.FirebaseAuth
import com.example.aureum1.model.repository.AccountRepository
import com.example.aureum1.model.repository.RecordRepository

class FullRecordListActivity : AppCompatActivity() {
    private val auth by lazy { FirebaseAuth.getInstance() }
    // Repositorios MVC: encapsulan Firestore y reglas de negocio
    private val accountRepo by lazy { AccountRepository() }
    private val recordRepo by lazy { RecordRepository() }

    private lateinit var rvListaCompleta: RecyclerView
    private lateinit var adapter: RegistroSectionAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_full_records)

        findViewById<ImageView>(R.id.btnCerrar).setOnClickListener { finish() }

        rvListaCompleta = findViewById(R.id.rvListaCompleta)
        adapter = RegistroSectionAdapter(emptyList())
        rvListaCompleta.layoutManager = LinearLayoutManager(this)
        rvListaCompleta.adapter = adapter

        cargarRegistrosCompletos()
    }

    private fun cargarRegistrosCompletos() {
        val uid = auth.currentUser?.uid ?: return

        // Escuchar info de cuentas centralizada en repositorio
        accountRepo.subscribeAccountsInfoByName(uid) { infoPorNombre ->
            adapter.updateAccountsInfo(infoPorNombre)
        }

        // Suscribir registros procesados (con duplicación de transferencias) desde el repositorio
        recordRepo.subscribeAllRaw(uid) { list ->
            adapter.submitList(list)
        }
    }
}