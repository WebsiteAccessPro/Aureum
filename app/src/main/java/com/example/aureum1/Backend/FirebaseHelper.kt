package com.example.aureum1.Backend

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

/**
 * Helper global para Firebase.
 * Centraliza conexión a Auth y Firestore, y operaciones básicas de usuarios y registros.
 */
object FirebaseHelper {
    val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    /**
     * Crea o actualiza (merge) un documento de usuario con datos arbitrarios.
     * Útil para flujos de registro que tienen perfil completo.
     */
    fun createUser(uid: String, data: Map<String, Any?>, callback: (Boolean, String?) -> Unit) {
        db.collection("users").document(uid)
            .set(data, SetOptions.merge())
            .addOnSuccessListener { callback(true, null) }
            .addOnFailureListener { e -> callback(false, e.message) }
    }

    /**
     * Agrega un nuevo registro financiero a la cuenta del usuario.
     * Puede ser ingreso, gasto o transferencia.
     */
    fun addRegistro(
        userId: String,
        data: Map<String, Any>,
        callback: (Boolean, String?) -> Unit
    ) {
        db.collection("accounts")
            .document(userId)
            .collection("registros")
            .add(data)
            .addOnSuccessListener { callback(true, null) }
            .addOnFailureListener { e -> callback(false, e.message) }
    }

    /**
     * Obtiene los datos de un usuario específico desde Firestore.
     */
    fun getUser(uid: String, callback: (Map<String, Any>?) -> Unit) {
        db.collection("users").document(uid)
            .get()
            .addOnSuccessListener { snapshot -> callback(snapshot.data) }
            .addOnFailureListener { _ -> callback(null) }
    }

    /**
     * Actualiza (merge) campos de un usuario.
     */
    fun updateUser(uid: String, patch: Map<String, Any?>, callback: (Boolean, String?) -> Unit) {
        db.collection("users").document(uid)
            .set(patch, SetOptions.merge())
            .addOnSuccessListener { callback(true, null) }
            .addOnFailureListener { e -> callback(false, e.message) }
    }
}
