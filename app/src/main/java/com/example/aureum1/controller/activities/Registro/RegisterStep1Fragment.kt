package com.example.aureum1.controller.activities.Registro

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.example.aureum1.R
import com.example.aureum1.Backend.FirebaseHelper
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

class RegisterStep1Fragment : Fragment() {
    
    private lateinit var tilEmail: TextInputLayout
    private lateinit var tilPass: TextInputLayout
    private lateinit var tilConfirm: TextInputLayout
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPass: TextInputEditText
    private lateinit var etConfirm: TextInputEditText
    private lateinit var btnGoogle: MaterialButton
    private lateinit var btnFacebook: MaterialButton
    private lateinit var btnNext: MaterialButton
    
    private lateinit var auth: FirebaseAuth
    
    var onValidationComplete: ((email: String, password: String) -> Unit)? = null
    var onSocialLoginSuccess: (() -> Unit)? = null
    var onNextClicked: (() -> Unit)? = null
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_register_step1, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        auth = FirebaseHelper.auth
        
        tilEmail = view.findViewById(R.id.tilEmail)
        tilPass = view.findViewById(R.id.tilPass)
        tilConfirm = view.findViewById(R.id.tilConfirm)
        etEmail = view.findViewById(R.id.etEmail)
        etPass = view.findViewById(R.id.etPass)
        etConfirm = view.findViewById(R.id.etConfirm)
        btnGoogle = view.findViewById(R.id.btnGoogle)
        btnFacebook = view.findViewById(R.id.btnFacebook)
        btnNext = view.findViewById(R.id.btnNext)
        
        btnGoogle.setOnClickListener { signInWithGoogle() }
        btnFacebook.setOnClickListener { signInWithFacebook() }
        btnNext.setOnClickListener { 
            if (validateAndProceed()) {
                onNextClicked?.invoke() 
            }
        }

        etEmail.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) { tilEmail.error = null }
        })
        etPass.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) { tilPass.error = null }
        })
        etConfirm.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) { tilConfirm.error = null }
        })
    }
    
    private val googleLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)
            auth.signInWithCredential(credential).addOnCompleteListener { t ->
                if (t.isSuccessful) {
                    onSocialLoginSuccess?.invoke()
                } else {
                    Toast.makeText(requireContext(), t.exception?.localizedMessage ?: "Error con Google", Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), e.localizedMessage ?: "No se pudo abrir Google", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun signInWithGoogle() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        val client = GoogleSignIn.getClient(requireContext(), gso)
        client.signOut().addOnCompleteListener {
            googleLauncher.launch(client.signInIntent)
        }
    }
    
    private fun signInWithFacebook() {
        // Delegar al LoginActivity para Facebook
        val intent = Intent(requireContext(), com.example.aureum1.controller.activities.LoginActivity::class.java)
        intent.putExtra("ACTION_SOCIAL", "FACEBOOK")
        intent.putExtra("FROM_REGISTER", true)
        startActivity(intent)
        requireActivity().finish()
    }
    
    fun validateAndProceed(): Boolean {
        val email = etEmail.text?.toString()?.trim().orEmpty()
        val pass = etPass.text?.toString()?.trim().orEmpty()
        val confirm = etConfirm.text?.toString()?.trim().orEmpty()
        
        var isValid = true
        
        if (!isValidEmailStrict(email)) {
            tilEmail.error = "Correo inválido"
            isValid = false
        } else {
            tilEmail.error = null
        }
        
        if (!isStrongPassword(pass) || pass.contains(" ") || pass.equals(email, true)) {
            tilPass.error = "Debe tener 8+, may/min, número y símbolo"
            isValid = false
        } else {
            tilPass.error = null
        }
        
        if (confirm != pass) {
            tilConfirm.error = "No coincide"
            isValid = false
        } else {
            tilConfirm.error = null
        }
        
        if (isValid) {
            onValidationComplete?.invoke(email, pass)
        }
        
        return isValid
    }

    private fun isValidEmailStrict(email: String): Boolean {
        val r = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$", RegexOption.IGNORE_CASE)
        if (!r.matches(email)) return false
        val domain = email.substringAfter("@")
        if (domain.contains("..")) return false
        val parts = domain.split('.')
        if (parts.any { it.isEmpty() || it.startsWith('-') || it.endsWith('-') }) return false
        return true
    }

    private fun isStrongPassword(p: String): Boolean {
        if (p.length < 8) return false
        val hasUpper = p.any { it.isUpperCase() }
        val hasLower = p.any { it.isLowerCase() }
        val hasDigit = p.any { it.isDigit() }
        val hasSymbol = p.any { !it.isLetterOrDigit() }
        return hasUpper && hasLower && hasDigit && hasSymbol
    }
    
    fun getData(): Pair<String, String> {
        return Pair(
            etEmail.text?.toString()?.trim().orEmpty(),
            etPass.text?.toString()?.trim().orEmpty()
        )
    }
}