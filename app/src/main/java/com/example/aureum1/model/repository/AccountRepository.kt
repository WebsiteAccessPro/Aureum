package com.example.aureum1.model.repository

import com.example.aureum1.model.Account
import com.example.aureum1.model.mappers.AccountMapper
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

/**
 * Repositorio simple para cuentas. Maneja suscripción al documento y mapea a [Account].
 */
class AccountRepository(private val db: FirebaseFirestore = FirebaseFirestore.getInstance()) {

    fun subscribeAccounts(uid: String, onChange: (List<Account>) -> Unit): ListenerRegistration {
        val docRef = db.collection("accounts").document(uid)
        return docRef.addSnapshotListener { snap, e ->
            if (e != null || snap == null || !snap.exists()) {
                // Si hay error o no existe, devolvemos solo el tile de agregar
                onChange(listOf(Account(isAddTile = true)))
                return@addSnapshotListener
            }
            val arr = snap.get("cuentas") as? List<Map<String, Any?>> ?: emptyList()
            val lista = arr.map { AccountMapper.fromMap(it) }
            val listaConAdd = lista.toMutableList().apply { add(Account(isAddTile = true)) }
            onChange(listaConAdd)
        }
    }

    /**
     * Suscripción de apoyo para adapters que aún usan Map: devuelve info de cuentas
     * asociada por nombre (clave = nombre de cuenta), útil para resolver moneda y otros datos.
     */
    fun subscribeAccountsInfoByName(uid: String, onChange: (Map<String, Map<String, Any?>>) -> Unit): ListenerRegistration {
        val docRef = db.collection("accounts").document(uid)
        return docRef.addSnapshotListener { snap, _ ->
            val arr = snap?.get("cuentas") as? List<Map<String, Any?>> ?: emptyList()
            val infoPorNombre = arr.associateBy { (it["nombre"] as? String).orEmpty() }
            onChange(infoPorNombre)
        }
    }

    /**
     * Variantes crudas (Map) de cuentas para Activities que aún no usan modelos.
     */
    fun fetchAccountsRaw(uid: String, onSuccess: (List<Map<String, Any?>>) -> Unit, onError: (Exception) -> Unit = {}): ListenerRegistration? {
        val docRef = db.collection("accounts").document(uid)
        docRef.get()
            .addOnSuccessListener { snap ->
                val arr = snap.get("cuentas") as? List<Map<String, Any?>> ?: emptyList()
                onSuccess(arr)
            }
            .addOnFailureListener { e -> onError(e) }
        return null
    }

    fun subscribeAccountsRaw(uid: String, onChange: (List<Map<String, Any?>>) -> Unit): ListenerRegistration {
        val docRef = db.collection("accounts").document(uid)
        return docRef.addSnapshotListener { snap, _ ->
            val arr = snap?.get("cuentas") as? List<Map<String, Any?>> ?: emptyList()
            onChange(arr)
        }
    }
}