package com.example.aureum1.controller.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.aureum1.R

/**
 * Adapter para seleccionar un registro y vincularlo a una deuda.
 * Usa el layout `item_deuda.xml` para mantener la estética pedida.
 */
class SeleccionRegistroAdapter(
    private var items: List<RecordItem>,
    private val onClick: (RecordItem) -> Unit
) : RecyclerView.Adapter<SeleccionRegistroAdapter.VH>() {

    data class RecordItem(
        val id: String,
        val titulo: String,
        val cuenta: String,
        val descripcion: String,
        val monto: Double,
        val moneda: String,
        val fechaTexto: String,
        val tipo: String
    )

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val imgIcono: ImageView = view.findViewById(R.id.imgIcono)
        val tvTitulo: TextView = view.findViewById(R.id.tvTituloDeuda)
        val tvCuenta: TextView = view.findViewById(R.id.tvCuenta)
        val tvDescripcion: TextView = view.findViewById(R.id.tvDescripcion)
        val tvMonto: TextView = view.findViewById(R.id.tvMonto)
        val tvFecha: TextView = view.findViewById(R.id.tvFecha)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_deuda, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.tvTitulo.text = item.titulo
        holder.tvCuenta.text = item.cuenta
        holder.tvDescripcion.text = item.descripcion
        val sign = if (item.tipo.equals("Ingreso", true)) "+" else "-"
        holder.tvMonto.text = "${item.moneda} $sign${String.format("%.2f", item.monto)}"
        holder.tvMonto.setTextColor(
            holder.itemView.context.getColor(
                if (sign == "+") R.color.aureum_green else R.color.aureum_red
            )
        )
        holder.tvFecha.text = item.fechaTexto
        holder.itemView.setOnClickListener { onClick(item) }
    }

    override fun getItemCount(): Int = items.size

    fun submit(itemsNew: List<RecordItem>) {
        items = itemsNew
        notifyDataSetChanged()
    }
}