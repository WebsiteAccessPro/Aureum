package com.example.aureum1.controller.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.aureum1.R
import com.example.aureum1.model.DateUtils
import com.google.firebase.Timestamp

class DebtClosedAdapter(
    private var items: List<Map<String, Any?>>, 
    private val onEliminar: ((String, Map<String, Any?>) -> Unit)? = null,
    private val onItemClick: ((String, Map<String, Any?>) -> Unit)? = null
) : RecyclerView.Adapter<DebtClosedAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val imgIcono: ImageView = view.findViewById(R.id.imgIcono)
        val tvTituloDeuda: TextView = view.findViewById(R.id.tvTituloDeuda)
        val tvDescripcion: TextView = view.findViewById(R.id.tvDescripcion)
        val tvFecha: TextView = view.findViewById(R.id.tvFecha)
        val ctaEliminar: View = view.findViewById(R.id.ctaEliminar)
        val tvPerdonado: TextView = view.findViewById(R.id.tvPerdonado)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_deuda_cerrada, parent, false)
        return VH(v)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        val accion = (item["accion"] as? String).orEmpty()
        val nombre = when (accion) {
            "presto" -> (item["nombrePresto"] as? String).orEmpty()
            "me_prestaron" -> (item["nombreMeprestaron"] as? String).orEmpty()
            else -> (item["nombre"] as? String).orEmpty()
        }
        val descripcion = (item["descripcion"] as? String).orEmpty()
        val fechaTs = item["fecha"] as? Timestamp

        val titulo = if (accion == "presto") {
            val base = if (nombre.isBlank()) "****" else nombre
            "$base ME DEBE"
        } else {
            val base = if (nombre.isBlank()) "****" else nombre
            "DEBO $base"
        }
        holder.tvTituloDeuda.text = titulo
        holder.tvDescripcion.text = if (descripcion.isNotBlank()) "\"$descripcion\"" else ""
        holder.tvPerdonado.text = if (accion == "presto") "Perdonado" else "Liquidado"
        val fechaLabel = DateUtils.relativeDate("", fechaTs)
        holder.tvFecha.text = fechaLabel
        holder.imgIcono.setImageResource(R.drawable.ic_user)
        holder.ctaEliminar.setOnClickListener { onEliminar?.invoke(accion, item) }
        holder.itemView.setOnClickListener { onItemClick?.invoke(accion, item) }
    }

    fun submitList(newItems: List<Map<String, Any?>>) {
        items = newItems
        notifyDataSetChanged()
    }
}