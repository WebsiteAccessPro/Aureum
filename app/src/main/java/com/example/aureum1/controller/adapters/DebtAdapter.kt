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
    private var items: List<Map<String, Any?>>, // deudas crudas desde Firestore
    private var accountsInfo: Map<String, Map<String, Any?>> = emptyMap()
) : RecyclerView.Adapter<DebtAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val imgCategoria: ImageView = view.findViewById(R.id.imgCategoria)
        val txtCategoriaTitulo: TextView = view.findViewById(R.id.txtCategoriaTitulo)
        val txtCuenta: TextView = view.findViewById(R.id.txtCuenta)
        val txtNota: TextView = view.findViewById(R.id.txtNota)
        val txtMoneda: TextView = view.findViewById(R.id.txtMoneda)
        val txtMonto: TextView = view.findViewById(R.id.txtMonto)
        val txtFecha: TextView = view.findViewById(R.id.txtFecha)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_registro, parent, false)
        return VH(v)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        val nombre = (item["nombre"] as? String).orEmpty()
        val cuenta = (item["cuenta"] as? String).orEmpty()
        val descripcion = (item["descripcion"] as? String).orEmpty()
        val monto = (item["monto"] as? Number)?.toDouble() ?: 0.0
        val monedaRegistro = (item["moneda"] as? String)
        val fecha = (item["fecha"] as? String).orEmpty()
        val accion = (item["accion"] as? String).orEmpty()

        holder.txtCategoriaTitulo.text = nombre.ifBlank { "Deuda" }
        holder.txtCuenta.text = cuenta
        holder.txtNota.text = if (descripcion.isNotBlank()) "\"$descripcion\"" else ""

        val monedaFinal = monedaRegistro ?: (accountsInfo[cuenta]?.get("moneda") as? String) ?: "PEN"
        val nf = NumberFormat.getNumberInstance(Locale.getDefault()).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
        val sign = if (accion == "presto") "+" else "-"
        holder.txtMoneda.text = monedaFinal
        holder.txtMonto.text = "${sign}${nf.format(monto)}"

        val colorRes = if (accion == "presto") R.color.aureum_green else R.color.red
        val color = ContextCompat.getColor(holder.itemView.context, colorRes)
        holder.txtMonto.setTextColor(color)
        holder.txtMoneda.setTextColor(color)

        holder.txtFecha.text = fecha
        holder.imgCategoria.setImageResource(R.drawable.ic_user)
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