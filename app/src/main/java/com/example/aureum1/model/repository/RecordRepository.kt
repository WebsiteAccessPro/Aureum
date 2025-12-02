package com.example.aureum1.model.repository

import com.example.aureum1.model.Record
import com.example.aureum1.model.mappers.RecordMapper
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

/**
 * RecordRepository centraliza el acceso a Firestore para registros.
 * Provee métodos con datos crudos (Map) para compatibilidad con adapters existentes
 * y métodos tipados (Record) para migraciones a datos seguros.
 */
class RecordRepository(private val db: FirebaseFirestore = FirebaseFirestore.getInstance()) {

    /**
     * Suscribe a todos los registros del usuario ordenados por fecha de creación (desc).
     * Devuelve la lista procesada en formato Map, incluyendo duplicación de Transferencias con `direction`.
     */
    fun subscribeAllRaw(uid: String, onChange: (List<Map<String, Any?>>) -> Unit): ListenerRegistration {
        return db.collection("accounts").document(uid)
            .collection("registros")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snaps, err ->
                if (err != null || snaps == null) {
                    onChange(emptyList())
                    return@addSnapshotListener
                }
                val list = mutableListOf<Map<String, Any?>>()
                for (doc in snaps.documents) {
                    val data = doc.data ?: continue
                    val base = data.toMutableMap().apply { put("idDoc", doc.id) }
                    val t = (base["tipo"] as? String).orEmpty()
                    if (t.equals("Transferencia", true)) {
                        val extDirection = (base["extDirection"] as? String).orEmpty()
                        if (extDirection.isNotEmpty()) {
                            val single = base.toMutableMap().apply { put("direction", extDirection) }
                            list.add(single)
                        } else {
                            val inn = base.toMutableMap().apply { put("direction", "in") }
                            val out = base.toMutableMap().apply { put("direction", "out") }
                            list.add(inn)
                            list.add(out)
                        }
                    } else {
                        list.add(base)
                    }
                }
                onChange(list)
            }
    }

    /**
     * Obtiene todos los registros (una sola vez) en formato Map crudo, con duplicación de transferencias.
     */
    fun fetchAllOnceRaw(uid: String, onSuccess: (List<Map<String, Any?>>) -> Unit, onError: (Exception) -> Unit = {}) {
        db.collection("accounts").document(uid)
            .collection("registros")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snaps ->
                val list = mutableListOf<Map<String, Any?>>()
                for (doc in snaps.documents) {
                    val data = doc.data ?: continue
                    val base = data.toMutableMap().apply { put("idDoc", doc.id) }
                    val t = (base["tipo"] as? String).orEmpty()
                    if (t.equals("Transferencia", true)) {
                        val inn = base.toMutableMap().apply { put("direction", "in") }
                        val out = base.toMutableMap().apply { put("direction", "out") }
                        list.add(inn)
                        list.add(out)
                    } else {
                        list.add(base)
                    }
                }
                onSuccess(list)
            }
            .addOnFailureListener { e -> onError(e) }
    }

    /**
     * Suscribe a todos los registros tipados [Record].
     */
    fun subscribeAll(uid: String, onChange: (List<Record>) -> Unit): ListenerRegistration {
        return subscribeAllRaw(uid) { raw ->
            onChange(raw.map { RecordMapper.fromMap(it) })
        }
    }
}