package com.example.aureum1.model.mappers

import com.example.aureum1.model.Record
import com.google.firebase.Timestamp

object RecordMapper {
    fun fromMap(map: Map<String, Any?>): Record {
        val monto = when (val v = map["monto"]) {
            is Number -> v.toDouble()
            is String -> v.toDoubleOrNull() ?: 0.0
            else -> 0.0
        }
        return Record(
            id = (map["id"] as? String).orEmpty(),
            tipo = (map["tipo"] as? String).orEmpty(),
            categoria = (map["categoria"] as? String).orEmpty(),
            descripcion = (map["descripcion"] as? String).orEmpty(),
            monto = monto,
            moneda = (map["moneda"] as? String).orEmpty().ifEmpty { "PEN" },
            fechaStr = (map["fecha"] as? String).orEmpty(),
            createdAt = map["createdAt"] as? Timestamp,
            origenCuentaId = map["origenCuentaId"] as? String,
            destinoCuentaId = map["destinoCuentaId"] as? String,
            direction = map["direction"] as? String
        )
    }

    fun toMap(record: Record): Map<String, Any?> {
        return mapOf(
            "id" to record.id,
            "tipo" to record.tipo,
            "categoria" to record.categoria,
            "descripcion" to record.descripcion,
            "monto" to record.monto,
            "moneda" to record.moneda,
            "fecha" to record.fechaStr,
            "createdAt" to record.createdAt,
            "origenCuentaId" to record.origenCuentaId,
            "destinoCuentaId" to record.destinoCuentaId,
            "direction" to record.direction
        )
    }
}