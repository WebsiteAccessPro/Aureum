package com.example.aureum1.model

import com.example.aureum1.R

/**
 * Resolver simple de iconos por categoría y tipo.
 * Centraliza el mapeo para que la vista no tenga lógica duplicada.
 */
object CategoryResolver {
    fun iconFor(tipo: String, categoria: String, direction: String? = null): Int {
        val tipoLower = tipo.lowercase()
        val cat = categoria
        // Transferencia tiene icono fijo
        if (tipoLower == "transferencia") return R.drawable.transfer

        return when {
            // INGRESOS
            cat.contains("sueldo", true) -> R.drawable.ic_money
            cat.contains("alquiler", true) -> R.drawable.ic_home
            cat.contains("venta", true) -> R.drawable.ic_shopping_bag
            cat.contains("comision", true) -> R.drawable.ic_chart
            cat.contains("interes", true) -> R.drawable.ic_bank
            cat.contains("reembolso", true) -> R.drawable.ic_refresh
            cat.contains("premio", true) -> R.drawable.ic_gift
            cat.contains("inversion", true) -> R.drawable.ic_trending_up
            cat.contains("bonificacion", true) -> R.drawable.ic_star

            // GASTOS
            cat.contains("aliment", true) || cat.contains("comida", true) || cat.contains("rest", true) -> R.drawable.ic_food
            cat.contains("transporte", true) || cat.contains("movilidad", true) -> R.drawable.ic_car
            cat.contains("educacion", true) -> R.drawable.ic_book
            cat.contains("salud", true) -> R.drawable.ic_health
            cat.contains("ropa", true) -> R.drawable.ic_shirt
            cat.contains("servicio", true) -> R.drawable.ic_light
            cat.contains("entreten", true) || cat.contains("ocio", true) -> R.drawable.ic_movie
            cat.contains("viaje", true) -> R.drawable.ic_plane
            cat.contains("mascota", true) -> R.drawable.ic_paw
            cat.contains("impuesto", true) -> R.drawable.ic_receipt
            cat.contains("donacion", true) -> R.drawable.ic_heart_hand

            else -> R.drawable.ic_category
        }
    }
}