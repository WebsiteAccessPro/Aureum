package com.example.aureum1.controller.fragments

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.aureum1.R
import com.example.aureum1.controller.activities.LoginActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.google.firebase.auth.EmailAuthProvider
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.GoogleAuthProvider
import java.text.SimpleDateFormat
import java.util.Locale

class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }
    private var listener: ListenerRegistration? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvName: TextView = view.findViewById(R.id.tvUserName)
        val tvEmailTop: TextView = view.findViewById(R.id.tvUserEmail)
        val tvPhone: TextView = view.findViewById(R.id.tvUserPhone)
        val tvGender: TextView = view.findViewById(R.id.tvUserGender)
        val tvBirth: TextView = view.findViewById(R.id.tvUserBirthDate)
        val tvEmailDetail: TextView = view.findViewById(R.id.tvUserEmailDetail)
        val btnEdit: MaterialButton = view.findViewById(R.id.btnEditProfile)
        val tvDeleteAccount: TextView = view.findViewById(R.id.tvDeleteAccount)

        // Configurar click listener para eliminar cuenta
        tvDeleteAccount.setOnClickListener {
            showDeleteAccountDialog()
        }

        // Abrir actividad de edición
        btnEdit.setOnClickListener {
            try {
                val ctx = requireContext()
                val intent = Intent(ctx, com.example.aureum1.controller.activities.EditUserActivity::class.java)

                if (intent.resolveActivity(ctx.packageManager) != null) {
                    startActivity(intent)
                } else {
                    android.widget.Toast.makeText(ctx, "No se pudo abrir la actividad de edición", android.widget.Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                android.util.Log.e("ProfileFragment", "Error al abrir edición de perfil: ${e.message}")
                android.widget.Toast.makeText(requireContext(), "Error al abrir edición: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
            }
        }

        val user = auth.currentUser ?: run {
            tvName.text = "Invitado"
            tvEmailTop.text = ""
            tvEmailDetail.text = ""
            tvPhone.text = "—"
            tvGender.text = "—"
            tvBirth.text = "—"
            return
        }
        val uid = user.uid
        tvEmailTop.text = user.email ?: ""
        tvEmailDetail.text = user.email ?: ""

        listener?.remove()
        listener = db.collection("users").document(uid)
            .addSnapshotListener { snap, error ->
                if (error != null || snap == null || !snap.exists()) return@addSnapshotListener

                val name = snap.get("displayName") as? String
                    ?: snap.get("fullName") as? String
                val rawName = name?.takeIf { it.isNotBlank() } ?: (user.displayName ?: "Usuario")
                tvName.text = formatDisplayName(rawName)

                val phoneValue = snap.get("phone")
                val phoneText = when (phoneValue) {
                    is String -> phoneValue
                    is Number -> phoneValue.toLong().toString()
                    else -> null
                }
                tvPhone.text = phoneText?.takeIf { it.isNotBlank() } ?: "—"

                val gender = snap.get("gender") as? String
                tvGender.text = gender?.takeIf { it.isNotBlank() } ?: "—"

                val birthRaw = snap.get("birthDate")?.let {
                    when (it) {
                        is String -> it
                        is com.google.firebase.Timestamp -> SimpleDateFormat(
                            "dd/MM/yyyy",
                            Locale.getDefault()
                        ).format(it.toDate())
                        else -> null
                    }
                }
                tvBirth.text = birthRaw?.takeIf { it.isNotBlank() } ?: "—"
            }
    }

    private fun formatDisplayName(name: String): String {
        val n = name.trim().replace("\\s+".toRegex(), " ")
        if (n.length <= 24) return n
        val lastSpace = n.lastIndexOf(' ')
        return if (lastSpace in 2 until (n.length - 1)) {
            n.substring(0, lastSpace) + "\n" + n.substring(lastSpace + 1)
        } else {
            n
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        listener?.remove(); listener = null
    }

    private fun showDeleteAccountDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Eliminar cuenta")
            .setMessage("¿Estás seguro que deseas eliminar tu cuenta permanentemente? Esta acción no se puede deshacer.")
            .setPositiveButton("Confirmar") { _, _ ->
                deleteAccount()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showReauthenticationDialog() {
        val user = auth.currentUser ?: return
        val email = user.email ?: return

        val inputLayout = com.google.android.material.textfield.TextInputLayout(requireContext())
        inputLayout.hint = "Contraseña"
        inputLayout.boxBackgroundMode = com.google.android.material.textfield.TextInputLayout.BOX_BACKGROUND_OUTLINE

        val input = com.google.android.material.textfield.TextInputEditText(requireContext())
        input.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        inputLayout.addView(input)

        val container = android.widget.LinearLayout(requireContext())
        container.orientation = android.widget.LinearLayout.VERTICAL
        container.setPadding(32, 16, 32, 16)
        container.addView(inputLayout)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Confirmar identidad")
            .setMessage("Por seguridad, ingresa tu contraseña para eliminar tu cuenta:")
            .setView(container)
            .setPositiveButton("Confirmar") { _, _ ->
                val password = input.text.toString()
                if (password.isNotBlank()) {
                    reauthenticateAndDelete(email, password)
                } else {
                    android.widget.Toast.makeText(requireContext(), "Por favor ingresa tu contraseña", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun reauthenticateAndDelete(email: String, password: String) {
        val user = auth.currentUser ?: return

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val credential = EmailAuthProvider.getCredential(email, password)
                user.reauthenticate(credential).await()
                deleteAccountAfterReauth()
            } catch (e: Exception) {
                android.util.Log.e("ProfileFragment", "Error en reautenticación: ${e.message}", e)
                android.widget.Toast.makeText(requireContext(), "Contraseña incorrecta. Intenta nuevamente.", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun deleteAccountAfterReauth() {
        val user = auth.currentUser ?: return
        val uid = user.uid

        CoroutineScope(Dispatchers.Main).launch {
            try {
                android.widget.Toast.makeText(requireContext(), "Eliminando cuenta...", android.widget.Toast.LENGTH_SHORT).show()
                db.collection("users").document(uid).delete().await()
                user.delete().await()
                android.widget.Toast.makeText(requireContext(), "Cuenta eliminada exitosamente", android.widget.Toast.LENGTH_LONG).show()
                val intent = Intent(requireContext(), LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                requireActivity().finish()
            } catch (e: Exception) {
                android.util.Log.e("ProfileFragment", "Error al eliminar cuenta después de reautenticación: ${e.message}", e)
                android.widget.Toast.makeText(requireContext(), "Error al eliminar cuenta: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun deleteAccount() {
        val user = auth.currentUser ?: run {
            android.widget.Toast.makeText(requireContext(), "No hay usuario autenticado", android.widget.Toast.LENGTH_SHORT).show()
            return
        }

        CoroutineScope(Dispatchers.Main).launch {
            try {
                // Si el usuario usa Google, pedir reautenticación con su cuenta Google
                if (user.providerData.any { it.providerId == "google.com" }) {
                    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestIdToken(getString(R.string.default_web_client_id))
                        .requestEmail()
                        .build()

                    val googleSignInClient = GoogleSignIn.getClient(requireContext(), gso)
                    val account = GoogleSignIn.getLastSignedInAccount(requireContext())
                        ?: run {
                            android.widget.Toast.makeText(requireContext(), "Por favor, vuelve a iniciar sesión con Google antes de eliminar la cuenta.", android.widget.Toast.LENGTH_LONG).show()
                            return@launch
                        }

                    val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                    user.reauthenticate(credential).await()
                }

                // Eliminar Firestore y Auth
                val uid = user.uid
                android.widget.Toast.makeText(requireContext(), "Eliminando cuenta...", android.widget.Toast.LENGTH_SHORT).show()
                db.collection("users").document(uid).delete().await()
                user.delete().await()

                android.widget.Toast.makeText(requireContext(), "Cuenta eliminada exitosamente", android.widget.Toast.LENGTH_LONG).show()
                val intent = Intent(requireContext(), LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                requireActivity().finish()

            } catch (e: Exception) {
                if (e.message?.contains("requires recent authentication") == true) {
                    android.widget.Toast.makeText(requireContext(), "Se requiere verificación adicional", android.widget.Toast.LENGTH_SHORT).show()
                    showReauthenticationDialog()
                } else {
                    android.util.Log.e("ProfileFragment", "Error al eliminar cuenta: ${e.message}", e)
                    android.widget.Toast.makeText(requireContext(), "Error al eliminar cuenta: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
