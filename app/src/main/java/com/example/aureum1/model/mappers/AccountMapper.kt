package com.example.aureum1.model.mappers

import com.example.aureum1.model.Account

object AccountMapper {
    fun fromMap(m: Map<String, Any?>): Account {
        val vIni = when (val v = m["valorInicial"]) {
            is Number -> v.toDouble()
            is String -> v.toDoubleOrNull() ?: 0.0
            else -> 0.0
        }
        return Account(
            id = (m["id"] as? String).orEmpty(),
            nombre = (m["nombre"] as? String).orEmpty(),
            numero = (m["numero"] as? String).orEmpty(),
            tipo = (m["tipo"] as? String).orEmpty(),
            valorInicial = vIni,
            moneda = (m["moneda"] as? String).orEmpty().ifEmpty { "PEN" },
            isAddTile = false
        )
    }
}