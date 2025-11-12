package com.example.aureum1.model

data class Account(
    val id: String = "",
    val nombre: String = "",
    val numero: String = "",
    val tipo: String = "",
    val valorInicial: Double = 0.0,
    val moneda: String = "PEN",
    val isAddTile: Boolean = false
) {
    // Compatibilidad con código antiguo que usa 'valor'
    val valor: Double get() = valorInicial
}