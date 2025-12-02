package com.example.aureum1.model.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.FieldValue

/**
 * Acceso a Firestore para colecciones de Deudas.
 */
class DebtRepository(private val db: FirebaseFirestore = FirebaseFirestore.getInstance()) {

    fun subscribeByAction(
        uid: String,
        accion: String, // "presto" | "me_prestaron"
        estado: String = "activo",
        onChange: (List<Map<String, Any?>>) -> Unit
    ): ListenerRegistration {
        return db.collection("accounts").document(uid)
            .collection("deudas").document(accion)
            .collection("items")
            .whereEqualTo("estado", estado)
            .addSnapshotListener { snaps, err ->
                if (err != null || snaps == null) {
                    onChange(emptyList())
                    return@addSnapshotListener
                }
                val lista = snaps.documents.mapNotNull { doc ->
                    doc.data?.toMutableMap()?.apply { put("_id", doc.id) }
                }
                val sorted = lista.sortedByDescending {
                    val ts = it["fecha"]
                    if (ts is com.google.firebase.Timestamp) ts.toDate().time else 0L
                }
                onChange(sorted)
            }
    }

    fun addDebt(uid: String, data: Map<String, Any>, onDone: (Boolean, String?) -> Unit) {
        val accion = (data["accion"] as? String).orEmpty()
        val payload = data.toMutableMap().apply { remove("accion") }
        db.collection("accounts").document(uid)
            .collection("deudas").document(accion)
            .collection("items")
            .add(payload)
            .addOnSuccessListener { onDone(true, null) }
            .addOnFailureListener { e -> onDone(false, e.message) }
    }

    fun addDebtNested(uid: String, accion: String, data: Map<String, Any>, onDone: (Boolean, String?) -> Unit) {
        db.collection("accounts").document(uid)
            .collection("deudas")
            .document(accion)
            .collection("items")
            .add(data)
            .addOnSuccessListener { onDone(true, null) }
            .addOnFailureListener { e -> onDone(false, e.message) }
    }

    fun subscribeMovementsByPerson(
        uid: String,
        accion: String,
        nombre: String,
        onChange: (List<Map<String, Any?>>) -> Unit
    ): ListenerRegistration {
        return db.collection("accounts").document(uid)
            .collection("deudas").document(accion)
            .collection("items")
            .whereEqualTo("nombre", nombre)
            .addSnapshotListener { snaps, err ->
                if (err != null || snaps == null) {
                    onChange(emptyList())
                    return@addSnapshotListener
                }
                val lista = snaps.documents.mapNotNull { it.data }
                val sorted = lista.sortedByDescending {
                    val ts = it["fecha"]
                    if (ts is com.google.firebase.Timestamp) ts.toDate().time else 0L
                }
                onChange(sorted)
            }
    }

    fun addMovementToDebt(
        uid: String,
        accion: String,
        debtId: String,
        movement: Map<String, Any>,
        delta: Double,
        onDone: (Boolean, String?) -> Unit
    ) {
        val debtRef = db.collection("accounts").document(uid)
            .collection("deudas").document(accion)
            .collection("items").document(debtId)
        db.runTransaction { tx ->
            val snap = tx.get(debtRef)
            val currentRaw = (snap.getDouble("monto") ?: 0.0) + delta
            val current = if (currentRaw < 0.0) 0.0 else currentRaw
            tx.update(debtRef, mapOf(
                "monto" to current,
                "fecha" to (movement["fecha"] ?: com.google.firebase.Timestamp.now())
            ))
            val movRef = debtRef.collection("movimientos").document()
            tx.set(movRef, movement)
            null
        }.addOnSuccessListener { onDone(true, null) }
            .addOnFailureListener { e -> onDone(false, e.message) }
    }

    fun subscribeMovements(
        uid: String,
        accion: String,
        debtId: String,
        onChange: (List<Map<String, Any?>>) -> Unit
    ): ListenerRegistration {
        return db.collection("accounts").document(uid)
            .collection("deudas").document(accion)
            .collection("items").document(debtId)
            .collection("movimientos")
            .addSnapshotListener { snaps, err ->
                if (err != null || snaps == null) {
                    onChange(emptyList())
                    return@addSnapshotListener
                }
                val lista = snaps.documents.mapNotNull { it.data }
                val sorted = lista.sortedByDescending {
                    val ts = it["fecha"]
                    if (ts is com.google.firebase.Timestamp) ts.toDate().time else 0L
                }
                onChange(sorted)
            }
    }

    fun setRecordLinked(
        uid: String,
        recordId: String,
        linked: Boolean,
        accion: String? = null,
        debtId: String? = null
    ) {
        val data = mutableMapOf<String, Any>("debtLinked" to linked)
        accion?.let { data["debtAccion"] = it }
        debtId?.let { data["debtId"] = it }
        db.collection("accounts").document(uid)
            .collection("registros").document(recordId)
            .update(data)
    }

    fun deleteDebtAndUnlink(
        uid: String,
        accion: String,
        debtId: String,
        onDone: (Boolean, String?) -> Unit
    ) {
        val debtRef = db.collection("accounts").document(uid)
            .collection("deudas").document(accion)
            .collection("items").document(debtId)
        debtRef.collection("movimientos").get()
            .addOnSuccessListener { snaps ->
                val batch = db.batch()
                snaps.documents.forEach { movDoc ->
                    val recordId = movDoc.getString("recordId")
                    if (!recordId.isNullOrBlank()) {
                        val regRef = db.collection("accounts").document(uid)
                            .collection("registros").document(recordId)
                        val unset = mapOf(
                            "debtLinked" to false,
                            "debtAccion" to FieldValue.delete(),
                            "debtId" to FieldValue.delete()
                        )
                        batch.update(regRef, unset)
                    }
                    batch.delete(movDoc.reference)
                }
                batch.delete(debtRef)
                batch.commit()
                    .addOnSuccessListener { onDone(true, null) }
                    .addOnFailureListener { e -> onDone(false, e.message) }
            }
            .addOnFailureListener { e -> onDone(false, e.message) }
    }

    fun setDebtEstado(
        uid: String,
        accion: String,
        debtId: String,
        estado: String,
        onDone: (Boolean, String?) -> Unit
    ) {
        val debtRef = db.collection("accounts").document(uid)
            .collection("deudas").document(accion)
            .collection("items").document(debtId)
        debtRef.update("estado", estado)
            .addOnSuccessListener { onDone(true, null) }
            .addOnFailureListener { e -> onDone(false, e.message) }
    }
}
