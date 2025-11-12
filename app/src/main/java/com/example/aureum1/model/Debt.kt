package com.example.aureum1.model

/**
 * Modelo simple de Deuda para Firestore.
 * Se guarda en `accounts/{uid}/deudas`.
 */
data class Debt(
    val accion: String,              // "presto" | "me_prestaron"
    val nombre: String,              // destinatario o emisor
    val descripcion: String?,
    val cuenta: String,              // nombre de cuenta seleccionada
    val moneda: String?,             // opcional (se resuelve desde cuenta al listar)
    val monto: Double,               // siempre positivo; signo se deduce por accion
    val fecha: String,               // ISO simple (yyyy-MM-dd)
    val fechaVencimiento: String?,   // opcional
    val estado: String = "activo",  // "activo" | "cerrado"
    val createdAt: com.google.firebase.Timestamp? = null
)