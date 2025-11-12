package com.example.aureum1.Backend

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

/**
 * Servicio Backend para operaciones específicas de cuentas.
 * Utiliza FirebaseHelper.db como instancia centralizada de Firestore.
 */
class AccountService(
    private val db: FirebaseFirestore = FirebaseHelper.db
) {
    /**
     * Agrega una nueva cuenta al array `cuentas` dentro de `accounts/{uid}`.
     */
    fun addAccount(
        uid: String,
        account: Map<String, Any?>,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        val docRef = db.collection("accounts").document(uid)
        docRef.set(
            mapOf(
                "userId" to uid,
                "cuentas" to FieldValue.arrayUnion(account)
            ),
            SetOptions.merge()
        ).addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onError(e) }
    }

    /**
     * Actualiza una cuenta específica por `id` dentro del array `cuentas`.
     */
    fun updateAccount(
        uid: String,
        accountId: String,
        newData: Map<String, Any?>,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        val docRef = db.collection("accounts").document(uid)
        db.runTransaction { tx ->
            val snap = tx.get(docRef)
            val arr = (snap.get("cuentas") as? List<Map<String, Any?>>)?.toMutableList() ?: mutableListOf()
            val idx = arr.indexOfFirst { (it["id"] as? String) == accountId }
            if (idx == -1) throw IllegalStateException("Cuenta no encontrada")
            arr[idx] = newData
            tx.update(docRef, "cuentas", arr)
        }.addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onError(e) }
    }

    /**
     * Elimina una cuenta específica por `id` del array `cuentas`.
     */
    fun deleteAccount(
        uid: String,
        accountId: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        val docRef = db.collection("accounts").document(uid)
        db.runTransaction { tx ->
            val snap = tx.get(docRef)
            val arr = (snap.get("cuentas") as? List<Map<String, Any?>>)?.toMutableList() ?: mutableListOf()
            val nueva = arr.filterNot { (it["id"] as? String) == accountId }
            tx.update(docRef, "cuentas", nueva)
        }.addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onError(e) }
    }
}