package com.example.aureum1.model.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

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
            .collection("deudas")
            .whereEqualTo("accion", accion)
            .whereEqualTo("estado", estado)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snaps, err ->
                if (err != null || snaps == null) {
                    onChange(emptyList())
                    return@addSnapshotListener
                }
                onChange(snaps.documents.mapNotNull { it.data })
            }
    }

    fun addDebt(uid: String, data: Map<String, Any>, onDone: (Boolean, String?) -> Unit) {
        db.collection("accounts").document(uid)
            .collection("deudas")
            .add(data)
            .addOnSuccessListener { onDone(true, null) }
            .addOnFailureListener { e -> onDone(false, e.message) }
    }
}