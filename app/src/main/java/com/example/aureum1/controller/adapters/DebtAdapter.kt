package com.example.aureum1.controller.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.aureum1.R
import com.example.aureum1.model.DateUtils
import com.google.firebase.Timestamp
import java.text.NumberFormat
import java.util.Locale

/**
 * Adapter para mostrar deudas reutilizando `item_registro.xml`.
 * Campos mapeados:
 *  - txtCategoriaTitulo: nombre/persona del registro
 *  - txtCuenta: cuenta
 *  - txtNota: descripción
 *  - txtMoneda/txtMonto: monto con signo por acción
 *  - txtFecha: fecha
 */
class DebtAdapter(
    private var items: List<Map<String, Any?>>, 
    private var accountsInfo: Map<String, Map<String, Any?>> = emptyMap(),
    private val accionFija: String,
    private val onAddRegistroClick: ((String, Map<String, Any?>) -> Unit)? = null,
    private val onItemClick: ((String, Map<String, Any?>) -> Unit)? = null
) : RecyclerView.Adapter<DebtAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val imgIcono: ImageView = view.findViewById(R.id.imgIcono)
        val tvTituloDeuda: TextView = view.findViewById(R.id.tvTituloDeuda)
        val tvCuenta: TextView = view.findViewById(R.id.tvCuenta)
        val tvDescripcion: TextView = view.findViewById(R.id.tvDescripcion)
        val tvMonto: TextView = view.findViewById(R.id.tvMonto)
        val tvFecha: TextView = view.findViewById(R.id.tvFecha)
        val ctaAdd: View = view.findViewById(R.id.ctaAddRegistro)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_deuda, parent, false)
        return VH(v)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        val accion = (item["accion"] as? String).orEmpty().ifBlank { accionFija }
        val nombre = when (accion) {
            "presto" -> (item["nombrePresto"] as? String).orEmpty()
            "me_prestaron" -> (item["nombreMeprestaron"] as? String).orEmpty()
            else -> (item["nombre"] as? String).orEmpty()
        }
        val cuenta = (item["cuenta"] as? String).orEmpty()
        val descripcion = (item["descripcion"] as? String).orEmpty()
        val monto = (item["monto"] as? Number)?.toDouble() ?: 0.0
        val monedaRegistro = (item["moneda"] as? String)
        val fechaTs = item["fecha"] as? Timestamp

        val titulo = if (accion == "presto") {
            val base = if (nombre.isBlank()) "****" else nombre
            "$base ME DEBE"
        } else {
            val base = if (nombre.isBlank()) "****" else nombre
            "DEBO $base"
        }
        holder.tvTituloDeuda.text = titulo
        holder.tvCuenta.text = cuenta
        holder.tvDescripcion.text = if (descripcion.isNotBlank()) "\"$descripcion\"" else ""

        val monedaFinal = monedaRegistro ?: (accountsInfo[cuenta]?.get("moneda") as? String) ?: "PEN"
        val nf = NumberFormat.getNumberInstance(Locale.getDefault()).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
        val base = "PEN"
        val montoBase = convertirMoneda(monto, monedaFinal, base)
        val sign = if (accion == "presto") "+" else "-"
        holder.tvMonto.text = "$base ${sign}${nf.format(montoBase)}"

        val colorRes = if (accion == "presto") R.color.aureum_green else R.color.aureum_red
        val color = ContextCompat.getColor(holder.itemView.context, colorRes)
        holder.tvMonto.setTextColor(color)

        val fechaLabel = DateUtils.relativeDate("", fechaTs)
        holder.tvFecha.text = fechaLabel
        holder.imgIcono.setImageResource(R.drawable.ic_user)
        holder.ctaAdd.setOnClickListener { onAddRegistroClick?.invoke(accionFija, item) }
        holder.itemView.setOnClickListener { onItemClick?.invoke(accionFija, item) }
    }

    private fun convertirMoneda(monto: Double, origen: String, destino: String): Double {
        if (origen.equals(destino, true)) return monto
        val usdToPen = 3.8
        val eurToPen = 4.1
        val montoEnPen = when (origen.uppercase()) {
            "USD" -> monto * usdToPen
            "EUR" -> monto * eurToPen
            else -> monto
        }
        return when (destino.uppercase()) {
            "USD" -> montoEnPen / usdToPen
            "EUR" -> montoEnPen / eurToPen
            else -> montoEnPen
        }
    }

    fun submitList(newItems: List<Map<String, Any?>>) {
        items = newItems
        notifyDataSetChanged()
    }

    fun updateAccountsInfo(info: Map<String, Map<String, Any?>>) {
        accountsInfo = info
        notifyDataSetChanged()
    }
}