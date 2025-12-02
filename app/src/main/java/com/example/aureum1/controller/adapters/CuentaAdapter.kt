package com.example.aureum1.controller.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.aureum1.R

class CuentaAdapter(
    private val items: List<Map<String, Any?>>, 
    private val onOpcionesClick: (Map<String, Any?>) -> Unit
) : RecyclerView.Adapter<CuentaAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tvNombre: TextView = view.findViewById(R.id.tvNombreCuenta)
        val tvTipo: TextView = view.findViewById(R.id.tvTipoCuenta)
        val btnOpciones: ImageButton = view.findViewById(R.id.btnOpciones)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_cuenta_ajustes, parent, false)
        return VH(v)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.tvNombre.text = (item["nombre"] as? String).orEmpty()
        holder.tvTipo.text   = (item["tipo"] as? String).orEmpty()
        holder.btnOpciones.setOnClickListener { onOpcionesClick(item) }
        holder.itemView.setOnClickListener { onOpcionesClick(item) }
    }
}