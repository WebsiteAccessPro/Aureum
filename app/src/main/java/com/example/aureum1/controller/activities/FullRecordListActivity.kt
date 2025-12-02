package com.example.aureum1.controller.activities

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.aureum1.R
import com.example.aureum1.controller.adapters.RegistroSectionAdapter
import com.example.aureum1.model.DateUtils
import com.example.aureum1.model.CategoryResolver
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth
import com.example.aureum1.model.repository.AccountRepository
import com.example.aureum1.model.repository.RecordRepository

class FullRecordListActivity : AppCompatActivity() {
    private val auth by lazy { FirebaseAuth.getInstance() }
    // Repositorios MVC: encapsulan Firestore y reglas de negocio
    private val accountRepo by lazy { AccountRepository() }
    private val recordRepo by lazy { RecordRepository() }
    private val db by lazy { FirebaseFirestore.getInstance() }

    private lateinit var rvListaCompleta: RecyclerView
    private lateinit var adapter: RegistroSectionAdapter
    private var accountsInfoByName: Map<String, Map<String, Any?>> = emptyMap()
    private var lastRawRecords: List<Map<String, Any?>> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_full_records)

        findViewById<ImageView>(R.id.btnCerrar).setOnClickListener { finish() }

        rvListaCompleta = findViewById(R.id.rvListaCompleta)
        adapter = RegistroSectionAdapter(emptyList())
        rvListaCompleta.layoutManager = LinearLayoutManager(this)
        rvListaCompleta.adapter = adapter
        adapter.setOnItemClick { item -> showRecordDetail(item) }

        cargarRegistrosCompletos()
    }

    private fun cargarRegistrosCompletos() {
        val uid = auth.currentUser?.uid ?: return

        // Escuchar info de cuentas centralizada en repositorio
        accountRepo.subscribeAccountsInfoByName(uid) { infoPorNombre ->
            adapter.updateAccountsInfo(infoPorNombre)
            accountsInfoByName = infoPorNombre
            submitProcessed()
        }

        // Suscribir registros procesados (con duplicación de transferencias) desde el repositorio
        recordRepo.subscribeAllRaw(uid) { list ->
            lastRawRecords = list
            submitProcessed()
        }
    }

    private fun showRecordDetail(item: Map<String, Any?>) {
        val dialog = BottomSheetDialog(this)
        val v = layoutInflater.inflate(R.layout.bottomsheet_record_detail, null)

        val tipo = (item["tipo"] as? String).orEmpty()
        val categoria = (item["categoria"] as? String).orEmpty()
        val cuentaOrigen = (item["cuentaOrigen"] as? String).orEmpty()
        val cuentaDestino = (item["cuentaDestino"] as? String).orEmpty()
        val nota = (item["nota"] as? String).orEmpty()
        val monto = (item["monto"] as? Number)?.toDouble() ?: 0.0
        val monedaRegistro = (item["moneda"] as? String)
        val fechaStr = (item["fecha"] as? String).orEmpty()
        val createdAt = item["createdAt"]
        val direction = (item["direction"] as? String).orEmpty()

        val catLower = categoria.lowercase(java.util.Locale.getDefault())
        val alreadyPrefixed = catLower.startsWith("ingreso") || catLower.startsWith("gasto") || catLower.startsWith("transferencia")
        val titulo = when (tipo.lowercase(java.util.Locale.getDefault())) {
            "ingreso" -> if (alreadyPrefixed || categoria.isBlank()) categoria.ifBlank { "Ingreso" } else "Ingresos por $categoria"
            "gasto" -> if (alreadyPrefixed || categoria.isBlank()) categoria.ifBlank { "Gasto" } else "Gastos en $categoria"
            "transferencia" -> if (alreadyPrefixed || cuentaDestino.isBlank()) categoria.ifBlank { "Transferencia" } else "Transferencia a $cuentaDestino"
            else -> categoria.ifBlank { "Registro" }
        }

        val cuentasLinea = if (tipo.equals("transferencia", true)) {
            if (cuentaOrigen.isNotBlank() && cuentaDestino.isNotBlank()) "$cuentaOrigen → $cuentaDestino" else cuentaOrigen.ifBlank { cuentaDestino }
        } else {
            cuentaOrigen
        }

        var moneda = monedaRegistro
        if (moneda.isNullOrBlank()) {
            moneda = if (tipo.equals("transferencia", true) && direction == "in") {
                accountsInfoByName[cuentaDestino]?.get("moneda") as? String
            } else {
                accountsInfoByName[cuentaOrigen]?.get("moneda") as? String
            }
        }
        val monedaFinal = moneda ?: "PEN"
        val nf = java.text.NumberFormat.getNumberInstance(java.util.Locale.getDefault()).apply { minimumFractionDigits = 2; maximumFractionDigits = 2 }
        val sign = when {
            tipo.equals("ingreso", true) -> "+"
            tipo.equals("gasto", true) -> "-"
            tipo.equals("transferencia", true) && direction == "in" -> "+"
            tipo.equals("transferencia", true) && direction == "out" -> "-"
            else -> ""
        }

        val tvTitulo = v.findViewById<TextView>(R.id.tvTituloRegistro)
        val tvTipo = v.findViewById<TextView>(R.id.tvTipo)
        val tvCuentas = v.findViewById<TextView>(R.id.tvCuentas)
        val tvMonto = v.findViewById<TextView>(R.id.tvMonto)
        val tvFecha = v.findViewById<TextView>(R.id.tvFecha)
        val tvNota = v.findViewById<TextView>(R.id.tvNota)
        val tvSaldo = v.findViewById<TextView>(R.id.tvSaldoPosterior)
        val btnCerrar = v.findViewById<TextView>(R.id.btnCerrarDetalle)
        val btnEliminar = v.findViewById<TextView>(R.id.btnEliminarRegistro)
        val imgCategoria = v.findViewById<ImageView>(R.id.imgCategoria)

        tvTitulo.text = titulo
        tvTipo.text = tipo.ifBlank { "Registro" }
        tvCuentas.text = cuentasLinea
        tvMonto.text = "$monedaFinal $sign${nf.format(monto)}"
        tvFecha.text = DateUtils.relativeDate(fechaStr, createdAt)
        tvNota.text = if (nota.isNotBlank()) "\"$nota\"" else ""

        val saldoPosterior = (item["saldoPosterior"] as? Number)?.toDouble()
        if (saldoPosterior != null) {
            tvSaldo.visibility = View.VISIBLE
            tvSaldo.text = "Saldo posterior: ${monedaFinal} ${nf.format(saldoPosterior)}"
        } else {
            tvSaldo.visibility = View.GONE
        }

        btnCerrar.setOnClickListener { dialog.dismiss() }
        val iconRes = CategoryResolver.iconFor(tipo, categoria, direction)
        imgCategoria.setImageResource(iconRes)
        val colorRes = when (tipo.lowercase(java.util.Locale.getDefault())) {
            "ingreso" -> R.color.aureum_green
            "gasto" -> R.color.red
            "transferencia" -> if (direction == "out") R.color.aureum_gold_dark else R.color.aureum_gold
            else -> R.color.md_hint
        }
        tvMonto.setTextColor(getColor(colorRes))

        btnEliminar.setOnClickListener {
            val idDoc = (item["idDoc"] as? String)
            val uid = auth.currentUser?.uid
            if (idDoc.isNullOrBlank() || uid.isNullOrBlank()) {
                android.widget.Toast.makeText(this, "No se pudo identificar el registro", android.widget.Toast.LENGTH_SHORT).show()
            } else {
                db.collection("accounts").document(uid)
                    .collection("registros").document(idDoc)
                    .delete()
                    .addOnSuccessListener {
                        android.widget.Toast.makeText(this, "Registro eliminado", android.widget.Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                    }
                    .addOnFailureListener { e ->
                        android.widget.Toast.makeText(this, e.message ?: "Error al eliminar", android.widget.Toast.LENGTH_SHORT).show()
                    }
            }
        }

        dialog.setContentView(v)
        dialog.show()
    }

    private fun submitProcessed() {
        val rollingPorCuenta = accountsInfoByName.mapValues {
            val v = it.value["valorInicial"]
            when (v) {
                is Number -> v.toDouble()
                is String -> v.toDoubleOrNull() ?: 0.0
                else -> 0.0
            }
        }.toMutableMap()

        fun convertirMoneda(monto: Double, monedaOrigen: String, monedaDestino: String): Double {
            if (monedaOrigen.equals(monedaDestino, true)) return monto
            val usdToPen = 3.8
            val eurToPen = 4.1
            val montoEnPen = when (monedaOrigen.uppercase()) {
                "USD" -> monto * usdToPen
                "EUR" -> monto * eurToPen
                else -> monto
            }
            return when (monedaDestino.uppercase()) {
                "USD" -> montoEnPen / usdToPen
                "EUR" -> montoEnPen / eurToPen
                else -> montoEnPen
            }
        }

        val conSaldo = mutableListOf<Map<String, Any?>>()
        for (item in lastRawRecords) {
            val tipo = (item["tipo"] as? String).orEmpty().lowercase()
            val direction = (item["direction"] as? String)
            val origen = (item["cuentaOrigen"] as? String).orEmpty()
            val destino = (item["cuentaDestino"] as? String).orEmpty()
            val monto = (item["monto"] as? Number)?.toDouble() ?: 0.0

            val afectada = if (tipo == "transferencia" && direction == "in") destino else origen
            val saldoActual = rollingPorCuenta[afectada] ?: 0.0

            val monedaOrigen = (accountsInfoByName[origen]?.get("moneda") as? String).orEmpty().ifEmpty { "PEN" }
            val monedaDestino = (accountsInfoByName[destino]?.get("moneda") as? String).orEmpty().ifEmpty { "PEN" }

            val efecto = when {
                tipo == "ingreso" -> monto
                tipo == "gasto" -> -monto
                tipo == "transferencia" && direction == "out" -> -monto
                tipo == "transferencia" && direction == "in" -> convertirMoneda(monto, monedaOrigen, monedaDestino)
                else -> 0.0
            }

            val nuevo = item.toMutableMap().apply { put("saldoPosterior", saldoActual) }
            conSaldo.add(nuevo)

            rollingPorCuenta[afectada] = saldoActual - efecto
        }

        adapter.submitList(conSaldo)
    }
}