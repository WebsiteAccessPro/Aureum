package com.example.aureum1.controller.activities

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.aureum1.R
import com.example.aureum1.controller.adapters.RegistrosDeudaAdapter
import com.example.aureum1.model.repository.AccountRepository
import com.example.aureum1.model.repository.DebtRepository
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import java.text.NumberFormat
import java.util.Locale

class RegistrosDeudaActivity : AppCompatActivity() {

    private val repo = DebtRepository()
    private val accountRepo = AccountRepository()
    private val auth = FirebaseAuth.getInstance()

    private lateinit var adapter: RegistrosDeudaAdapter
    private var listener: ListenerRegistration? = null

    private lateinit var tvTotal: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registros_deuda)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbarRegistrosDeuda)
        toolbar.setNavigationOnClickListener { finish() }

        tvTotal = findViewById(R.id.tvTotal)

        val rv = findViewById<RecyclerView>(R.id.rvMovimientosDeuda)
        rv.layoutManager = LinearLayoutManager(this)
        adapter = RegistrosDeudaAdapter(emptyList())
        rv.adapter = adapter

        val uid = auth.currentUser?.uid ?: return
        val accion = intent.getStringExtra("ACCION") ?: "presto"
        val debtId = intent.getStringExtra("DEBT_ID") ?: ""

        val source = intent.getStringExtra("SOURCE") ?: "activo"
        if (source == "cerrado") {
            toolbar.inflateMenu(R.menu.menu_debt_closed)
        } else {
            toolbar.inflateMenu(R.menu.menu_edit_account)
            val isPresto = accion == "presto"
            toolbar.menu.findItem(R.id.action_forgive)?.isVisible = isPresto
            toolbar.menu.findItem(R.id.action_close)?.isVisible = !isPresto
            toolbar.menu.findItem(R.id.action_save)?.isVisible = false
            toolbar.menu.findItem(R.id.action_save_overflow)?.isVisible = false
            toolbar.menu.findItem(R.id.action_delete_overflow)?.isVisible = false
        }
        toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_delete -> {
                    repo.deleteDebtAndUnlink(uid, accion, debtId) { ok, err ->
                        if (!ok) {
                            android.widget.Toast.makeText(this, err ?: "Error al eliminar deuda", android.widget.Toast.LENGTH_SHORT).show()
                        } else {
                            finish()
                        }
                    }
                    true
                }
                R.id.action_forgive -> {
                    repo.setDebtEstado(uid, accion, debtId, "cerrado") { ok, err ->
                        if (!ok) {
                            android.widget.Toast.makeText(this, err ?: "Error al perdonar deuda", android.widget.Toast.LENGTH_SHORT).show()
                        } else {
                            finish()
                        }
                    }
                    true
                }
                R.id.action_close -> {
                    repo.setDebtEstado(uid, accion, debtId, "cerrado") { ok, err ->
                        if (!ok) {
                            android.widget.Toast.makeText(this, err ?: "Error al cerrar deuda", android.widget.Toast.LENGTH_SHORT).show()
                        } else {
                            finish()
                        }
                    }
                    true
                }
                R.id.action_move_active -> {
                    repo.setDebtEstado(uid, accion, debtId, "activo") { ok, err ->
                        if (!ok) {
                            android.widget.Toast.makeText(this, err ?: "Error al mover a activas", android.widget.Toast.LENGTH_SHORT).show()
                        } else {
                            finish()
                        }
                    }
                    true
                }
                R.id.action_edit_debt -> {
                    val i = android.content.Intent(this, com.example.aureum1.controller.activities.EditDeudaActivity::class.java)
                    i.putExtra("ACCION", accion)
                    i.putExtra("DEBT_ID", debtId)
                    startActivity(i)
                    true
                }
                else -> false
            }
        }

        accountRepo.subscribeAccountsInfoByName(uid) { info ->
            adapter.updateAccountsInfo(info)
        }

        listener?.remove()
        listener = repo.subscribeMovements(uid, accion, debtId) { lista ->
            adapter.submitList(lista)
            val total = lista.sumOf { m ->
                val monto = (m["monto"] as? Number)?.toDouble() ?: 0.0
                val signo = (m["movimiento_signo"] as? String).orEmpty()
                if (signo.startsWith("-")) -monto else monto
            }
            val moneda = lista.firstOrNull()?.get("moneda") as? String ?: "PEN"
            val labelMoneda = if (moneda.equals("PEN", true)) "PEN" else moneda
            val nf = NumberFormat.getNumberInstance(Locale.getDefault()).apply {
                minimumFractionDigits = 2
                maximumFractionDigits = 2
            }
            tvTotal.text = "$labelMoneda ${nf.format(total)}"
        }
    }

    override fun onDestroy() {
        listener?.remove()
        super.onDestroy()
    }
}
