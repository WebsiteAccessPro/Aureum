package com.example.aureum1.controller.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.aureum1.R
import com.example.aureum1.controller.adapters.CuentaAdapter
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.example.aureum1.model.repository.AccountRepository

class AjustesCuentaActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var rv: RecyclerView
    private lateinit var fab: ExtendedFloatingActionButton
    private lateinit var adapter: CuentaAdapter
    private val cuentas = mutableListOf<Map<String, Any?>>()
    private var listener: com.google.firebase.firestore.ListenerRegistration? = null

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val accountRepo by lazy { AccountRepository() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ajustes_cuenta)

        // Referencias UI
        toolbar = findViewById(R.id.toolbarAjustesCuenta)
        rv = findViewById(R.id.recyclerCuentas)
        fab = findViewById(R.id.fabNuevaCuenta)

        // Cerrar con el botón de retroceso en el toolbar
        toolbar.setNavigationOnClickListener {
            finish()
            overridePendingTransition(R.anim.enter_from_left, R.anim.exit_to_right)
        }

        rv.layoutManager = LinearLayoutManager(this)
        rv.setHasFixedSize(true)

        // Adaptador: al tocar las opciones, abrir edición de cuenta
        adapter = CuentaAdapter(
            items = cuentas,
            onOpcionesClick = { cuenta ->
                val accId = cuenta["id"] as? String
                if (accId.isNullOrBlank()) {
                    Toast.makeText(this, "ID de cuenta no encontrado", Toast.LENGTH_SHORT).show()
                    return@CuentaAdapter
                }

                val intent = Intent(this, EditCuentaActivity::class.java).apply {
                    putExtra("ACC_ID", accId)
                }
                startActivity(intent)
                overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left)
            }
        )
        rv.adapter = adapter

        suscribirCuentas()

        // ➕ Crear nueva cuenta
        fab.setOnClickListener {
            startActivity(Intent(this, NuevaCuentaActivity::class.java))
            overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left)
        }
    }

    // Función para cargar todas las cuentas del usuario (sin acceso directo a Firestore)
    private fun suscribirCuentas() {
        val uid = auth.currentUser?.uid ?: return
        listener?.remove()
        listener = accountRepo.subscribeAccountsRaw(uid) { arr ->
            cuentas.clear()
            cuentas.addAll(arr)
            adapter.notifyDataSetChanged()
        }
    }

    override fun onDestroy() {
        listener?.remove(); listener = null
        super.onDestroy()
    }
}
