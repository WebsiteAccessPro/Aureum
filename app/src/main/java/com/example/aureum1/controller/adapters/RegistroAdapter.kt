package com.example.aureum1.controller.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.core.content.ContextCompat
import com.example.aureum1.R
import java.text.NumberFormat
import java.util.Locale
import com.example.aureum1.model.CategoryResolver
import com.example.aureum1.model.DateUtils

class RegistroAdapter(
    private var items: List<Map<String, Any?>>, // registros
    private var accountsInfo: Map<String, Map<String, Any?>> = emptyMap() // nombre -> info
) : RecyclerView.Adapter<RegistroAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val imgCategoria: ImageView = view.findViewById(R.id.imgCategoria)
        val txtCategoriaTitulo: TextView = view.findViewById(R.id.txtCategoriaTitulo)
        val txtCuenta: TextView = view.findViewById(R.id.txtCuenta)
        val txtNota: TextView = view.findViewById(R.id.txtNota)
        val txtMoneda: TextView = view.findViewById(R.id.txtMoneda)
        val txtMonto: TextView = view.findViewById(R.id.txtMonto)
        val txtSaldo: TextView = view.findViewById(R.id.txtSaldo)
        val txtFecha: TextView = view.findViewById(R.id.txtFecha)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_registro, parent, false)
        return VH(v)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]

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
        holder.txtCategoriaTitulo.text = categoriaTitulo

        val cuentaLine = if (tipo.equals("transferencia", true)) {
            if (cuentaOrigen.isNotBlank() && cuentaDestino.isNotBlank()) "$cuentaOrigen → $cuentaDestino" else cuentaOrigen.ifBlank { cuentaDestino }
        } else {
            cuentaOrigen
        }
        holder.txtCuenta.text = cuentaLine

        holder.txtNota.text = if (nota.isNotBlank()) "\"$nota\"" else ""

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
        holder.txtMoneda.text = monedaFinal
        holder.txtMonto.text = "${sign}${nf.format(monto)}"

        // Color por tipo de registro
        val colorRes = when (tipo.lowercase(Locale.getDefault())) {
            "ingreso" -> R.color.aureum_green
            "gasto" -> R.color.red
            "transferencia" -> if (direction == "out") R.color.aureum_gold_dark else R.color.aureum_gold
            else -> R.color.md_hint
        }
        val color = ContextCompat.getColor(holder.itemView.context, colorRes)
        holder.txtMonto.setTextColor(color)
        holder.txtMoneda.setTextColor(color)

        // Mostrar saldo histórico (después de ese movimiento) en color fijo
        val saldoAccName = if (tipo.equals("transferencia", true) && direction == "in") cuentaDestino else cuentaOrigen
        val saldoPosterior = (item["saldoPosterior"] as? Number)?.toDouble()
        if (saldoPosterior != null) {
            val monedaSaldo = (accountsInfo[saldoAccName]?.get("moneda") as? String) ?: monedaFinal
            holder.txtSaldo.text = "(${monedaSaldo} ${nf.format(saldoPosterior)})"
            holder.txtSaldo.visibility = View.VISIBLE
            // Color fijo para el paréntesis, independiente del tipo
            val saldoColor = ContextCompat.getColor(holder.itemView.context, R.color.green)
            holder.txtSaldo.setTextColor(saldoColor)
        } else {
            holder.txtSaldo.visibility = View.GONE
        }

        holder.txtFecha.text = DateUtils.relativeDate(fechaStr, createdAt)

        // Icono de categoría (simple mapping por palabras clave)
        val iconRes = CategoryResolver.iconFor(tipo, categoria, direction)
        holder.imgCategoria.setImageResource(iconRes)
    }

    fun submitList(newItems: List<Map<String, Any?>>) {
        items = newItems
        notifyDataSetChanged()
    }

    fun updateAccountsInfo(info: Map<String, Map<String, Any?>>) {
        accountsInfo = info
        notifyDataSetChanged()
    }

    // DateUtils.relativeDate ya cubre esta lógica
}