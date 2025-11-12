package com.example.aureum1.model

import com.google.firebase.Timestamp

data class Record(
    val id: String = "",
    val tipo: String = "",
    val categoria: String = "",
    val descripcion: String = "",
    val monto: Double = 0.0,
    val moneda: String = "PEN",
    val fechaStr: String = "",
    val createdAt: Timestamp? = null,
    // Transferencias
    val origenCuentaId: String? = null,
    val destinoCuentaId: String? = null,
    val direction: String? = null // "in" | "out" para transferencias duplicadas
)