package com.example.aureum1.controller.activities.Registro

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.aureum1.R
import com.example.aureum1.controller.adapters.CuentaAdapter
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.auth.FirebaseAuth
import com.example.aureum1.model.repository.AccountRepository

class SeleccionCuentaActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var rv: RecyclerView
    private lateinit var adapter: CuentaAdapter
    private val cuentas = mutableListOf<Map<String, Any?>>()

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val accountRepo by lazy { AccountRepository() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_seleccion_cuenta)

        toolbar = findViewById(R.id.toolbarSeleccionCuenta)
        rv = findViewById(R.id.recyclerSeleccionCuenta)

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
                    setResult(RESULT_OK, resultIntent)
                    finish()
                } else {
                    Toast.makeText(this, "Cuenta sin nombre", Toast.LENGTH_SHORT).show()
                }
            }
        )
        rv.adapter = adapter

        cargarCuentas()
    }

    private fun cargarCuentas() {
        val uid = auth.currentUser?.uid ?: return
        accountRepo.fetchAccountsRaw(
            uid = uid,
            onSuccess = { arr ->
                cuentas.clear()
                cuentas.addAll(arr)
                adapter.notifyDataSetChanged()
            },
            onError = { e ->
                Toast.makeText(this, "Error cargando cuentas: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        )
    }
}