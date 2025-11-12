package com.example.aureum1.controller.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.aureum1.R

class CategoriaAdapter(
    private val categorias: List<String>,
    private val onClick: (String) -> Unit
) : RecyclerView.Adapter<CategoriaAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvCategoria: TextView = view.findViewById(R.id.tvItemCategoria)
        val imgCategoria: ImageView = view.findViewById(R.id.imgCategoria)

        init {
            view.setOnClickListener {
                onClick(categorias[adapterPosition])
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_categoria, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val categoria = categorias[position]
        holder.tvCategoria.text = categoria

        // 🔹 Íconos personalizados por tipo de categoría
        val iconRes = when {
            // INGRESOS
            categoria.contains("sueldo", true) -> R.drawable.ic_money
            categoria.contains("alquiler", true) -> R.drawable.ic_home
            categoria.contains("venta", true) -> R.drawable.ic_shopping_bag
            categoria.contains("comision", true) -> R.drawable.ic_chart
            categoria.contains("interes", true) -> R.drawable.ic_bank
            categoria.contains("reembolso", true) -> R.drawable.ic_refresh
            categoria.contains("premio", true) -> R.drawable.ic_gift
            categoria.contains("inversion", true) -> R.drawable.ic_trending_up
            categoria.contains("bonificacion", true) -> R.drawable.ic_star

            // GASTOS
            categoria.contains("aliment", true) -> R.drawable.ic_food
            categoria.contains("transporte", true) -> R.drawable.ic_car
            categoria.contains("educacion", true) -> R.drawable.ic_book
            categoria.contains("salud", true) -> R.drawable.ic_health
            categoria.contains("ropa", true) -> R.drawable.ic_shirt
            categoria.contains("servicio", true) -> R.drawable.ic_light
            categoria.contains("entreten", true) || categoria.contains("ocio", true) -> R.drawable.ic_movie
            categoria.contains("viaje", true) -> R.drawable.ic_plane
            categoria.contains("mascota", true) -> R.drawable.ic_paw
            categoria.contains("impuesto", true) -> R.drawable.ic_receipt
            categoria.contains("donacion", true) -> R.drawable.ic_heart_hand
            else -> R.drawable.ic_category
        }

        holder.imgCategoria.setImageResource(iconRes)
    }

    override fun getItemCount(): Int = categorias.size
}
