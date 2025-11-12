package com.example.aureum1.controller.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.aureum1.R
import java.text.NumberFormat
import com.example.aureum1.model.CategoryResolver
import com.example.aureum1.model.DateUtils
import java.util.Locale

class RegistroSectionAdapter(
    private var records: List<Map<String, Any?>> = emptyList(),
    private var accountsInfo: Map<String, Map<String, Any?>> = emptyMap()
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val rows = mutableListOf<Row>()

    private sealed class Row {
        data class Header(val label: String) : Row()
        data class Item(val data: Map<String, Any?>) : Row()
    }

    private val VIEW_TYPE_HEADER = 0
    private val VIEW_TYPE_ITEM = 1

    override fun getItemViewType(position: Int): Int = when (rows[position]) {
        is Row.Header -> VIEW_TYPE_HEADER
        is Row.Item -> VIEW_TYPE_ITEM
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_HEADER) {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_month_header, parent, false)
            HeaderVH(v)
        } else {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_registro, parent, false)
            ItemVH(v)
        }
    }

    override fun getItemCount(): Int = rows.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val row = rows[position]) {
            is Row.Header -> (holder as HeaderVH).bind(row.label)
            is Row.Item -> (holder as ItemVH).bind(row.data)
        }
    }

    fun submitList(newRecords: List<Map<String, Any?>>) {
        records = newRecords
        rebuildRows()
    }

    fun updateAccountsInfo(info: Map<String, Map<String, Any?>>) {
        accountsInfo = info
        notifyDataSetChanged()
    }

    private fun rebuildRows() {
        rows.clear()
        var currentLabel: String? = null
        for (m in records) {
            val label = monthLabel(m)
            if (label != currentLabel) {
                currentLabel = label
                rows.add(Row.Header(label))
            }
            rows.add(Row.Item(m))
        }
        notifyDataSetChanged()
    }

    private fun monthLabel(item: Map<String, Any?>): String {
        val fechaStr = (item["fecha"] as? String).orEmpty()
        val createdAt = item["createdAt"]
        return DateUtils.monthLabel(fechaStr, createdAt)
    }

    inner class HeaderVH(view: View) : RecyclerView.ViewHolder(view) {
        private val tvMes: TextView = view.findViewById(R.id.tvMes)
        private val divider: View = view.findViewById(R.id.viewDivider)
        fun bind(label: String) {
            tvMes.text = label
            // Divider styling may be static via XML; keep simple here
            divider.visibility = View.VISIBLE
        }
    }

    inner class ItemVH(view: View) : RecyclerView.ViewHolder(view) {
        private val imgCategoria: ImageView = view.findViewById(R.id.imgCategoria)
        private val txtCategoriaTitulo: TextView = view.findViewById(R.id.txtCategoriaTitulo)
        private val txtCuenta: TextView = view.findViewById(R.id.txtCuenta)
        private val txtNota: TextView = view.findViewById(R.id.txtNota)
        private val txtMoneda: TextView = view.findViewById(R.id.txtMoneda)
        private val txtMonto: TextView = view.findViewById(R.id.txtMonto)
        private val txtSaldo: TextView = view.findViewById(R.id.txtSaldo)
        private val txtFecha: TextView = view.findViewById(R.id.txtFecha)

        fun bind(item: Map<String, Any?>) {
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

            val catLower = categoria.lowercase(Locale.getDefault())
            val alreadyPrefixed = catLower.startsWith("ingreso") || catLower.startsWith("gasto") || catLower.startsWith("transferencia")
            val categoriaTitulo = when (tipo.lowercase(Locale.getDefault())) {
                "ingreso" -> if (alreadyPrefixed || categoria.isBlank()) categoria.ifBlank { "Ingreso" } else "Ingresos por $categoria"
                "gasto" -> if (alreadyPrefixed || categoria.isBlank()) categoria.ifBlank { "Gasto" } else "Gastos en $categoria"
                "transferencia" -> if (alreadyPrefixed || cuentaDestino.isBlank()) categoria.ifBlank { "Transferencia" } else "Transferencia a $cuentaDestino"
                else -> categoria.ifBlank { "Registro" }
            }
            txtCategoriaTitulo.text = categoriaTitulo

            val cuentaLine = if (tipo.equals("transferencia", true)) {
                if (cuentaOrigen.isNotBlank() && cuentaDestino.isNotBlank()) "$cuentaOrigen → $cuentaDestino" else cuentaOrigen.ifBlank { cuentaDestino }
            } else {
                cuentaOrigen
            }
            txtCuenta.text = cuentaLine

            txtNota.text = if (nota.isNotBlank()) "\"$nota\"" else ""

            // Resolve currency from account info if possible
            var moneda = monedaRegistro
            if (moneda.isNullOrBlank()) {
                moneda = if (tipo.equals("transferencia", true) && direction == "in") {
                    accountsInfo[cuentaDestino]?.get("moneda") as? String
                } else {
                    accountsInfo[cuentaOrigen]?.get("moneda") as? String
                }
            }
            val monedaFinal = moneda ?: "PEN"

            val nf = NumberFormat.getNumberInstance(Locale.getDefault())
            nf.minimumFractionDigits = 2
            nf.maximumFractionDigits = 2
            val sign = when {
                tipo.equals("ingreso", true) -> "+"
                tipo.equals("gasto", true) -> "-"
                tipo.equals("transferencia", true) && direction == "in" -> "+"
                tipo.equals("transferencia", true) && direction == "out" -> "-"
                else -> ""
            }
            txtMoneda.text = monedaFinal
            txtMonto.text = "${sign}${nf.format(monto)}"

            // Color por tipo de registro
            val colorRes = when (tipo.lowercase(Locale.getDefault())) {
                "ingreso" -> R.color.aureum_green
                "gasto" -> R.color.red
                "transferencia" -> if (direction == "out") R.color.aureum_gold_dark else R.color.aureum_gold
                else -> R.color.md_hint
            }
            val color = ContextCompat.getColor(itemView.context, colorRes)
            txtMonto.setTextColor(color)
            txtMoneda.setTextColor(color)

            val saldoAccName = if (tipo.equals("transferencia", true) && direction == "in") cuentaDestino else cuentaOrigen
            val saldoPosterior = (item["saldoPosterior"] as? Number)?.toDouble()
            if (saldoPosterior != null) {
                val monedaSaldo = (accountsInfo[saldoAccName]?.get("moneda") as? String) ?: monedaFinal
                txtSaldo.text = "(${monedaSaldo} ${nf.format(saldoPosterior)})"
                txtSaldo.visibility = View.VISIBLE
                val saldoColor = ContextCompat.getColor(itemView.context, R.color.green)
                txtSaldo.setTextColor(saldoColor)
            } else {
                txtSaldo.visibility = View.GONE
            }

            txtFecha.text = DateUtils.relativeDate(fechaStr, createdAt)

            val iconRes = CategoryResolver.iconFor(tipo, categoria, direction)
            imgCategoria.setImageResource(iconRes)
        }
        // DateUtils.relativeDate ya cubre esta lógica
    }
}