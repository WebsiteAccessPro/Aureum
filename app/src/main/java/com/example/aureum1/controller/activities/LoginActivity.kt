package com.example.aureum1.controller.activities

import android.app.AlertDialog
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.Log
import android.util.Patterns
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import android.provider.Settings
import android.os.Build
import com.example.aureum1.R
import com.example.aureum1.databinding.ActivityLoginBinding
import com.example.aureum1.controller.activities.Registro.RegisterActivity
import com.facebook.AccessToken
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.example.aureum1.Backend.FirebaseHelper
import com.google.android.gms.common.api.ApiException
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }
    private lateinit var prefs: SharedPreferences
    private var linkAfterRegister: Boolean = false
    private val ACCOUNT_PREFS = "account_prefs"


    // --- Facebook ---
    private lateinit var callbackManager: CallbackManager

    private val googleLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)
            val emailFromGoogle = account.email?.trim()?.lowercase()
            val current = auth.currentUser

            Log.d("AUTH", "emailFromGoogle=$emailFromGoogle current=${current?.uid} providers=${current?.providerData?.map { it.providerId }}")

            // === Caso: ya hay sesión y el correo coincide ===
            if (current != null && !emailFromGoogle.isNullOrBlank()
                && current.email?.trim()?.lowercase() == emailFromGoogle) {

                val providers = current.providerData.map { it.providerId }
                val isGoogleUser = providers.contains("google.com")
                val hasPasswordLinked = providers.contains("password")

                // 1) Si el usuario actual es Google y todavía NO tiene password vinculado,
                // pero ese email sí tiene método password en Firebase → pedir pass y LINK email->usuario actual
                if (isGoogleUser && !hasPasswordLinked) {
                    auth.fetchSignInMethodsForEmail(emailFromGoogle)
                        .addOnSuccessListener { r ->
                            val methods = r.signInMethods ?: emptyList()
                            if (methods.contains("password")) {
                                Log.d("AUTH", "Rama: usuario actual Google; vincular EMAIL/PASSWORD al usuario actual")
                                promptPasswordToLink(emailFromGoogle) { password ->
                                    val emailCredential = EmailAuthProvider.getCredential(emailFromGoogle, password)
                                    current.linkWithCredential(emailCredential).addOnCompleteListener { linkTask ->
                                        if (linkTask.isSuccessful) {
                                            Log.d("AUTH", "✅ linkWithCredential exitoso (Email -> Google actual)")
                                            if (binding.chkRemember.isChecked) savePrefs(emailFromGoogle, password) else clearPrefs()
                                            ensureUserProfile()
                                            goHome()
                                        } else {
                                            showError(linkTask.exception)
                                        }
                                    }
                                }
                            } else {
                                // No hay password que vincular; seguimos y probamos vínculo de Google si aplica
                                Log.d("AUTH", "Rama: usuario Google sin método password en servidor; nada que vincular con email")
                                ensureUserProfile(); goHome()
                            }
                        }
                        .addOnFailureListener { e -> showError(e) }

                    return@registerForActivityResult
                }

                // 2) Si el usuario actual es Email (o no tiene Google), vinculamos Google -> cuenta actual
                Log.d("AUTH", "Rama: vincular Google a cuenta actual")
                current.linkWithCredential(credential).addOnCompleteListener { linkTask ->
                    if (linkTask.isSuccessful) {
                        Log.d("AUTH", "✅ linkWithCredential exitoso (Google -> Email)")
                        if (binding.chkRemember.isChecked) {
                            savePrefs(current.email ?: "", binding.etPassword.text?.toString().orEmpty())
                        } else clearPrefs()
                        ensureUserProfile()
                        goHome()
                    } else {
                        val ex = linkTask.exception
                        if (ex is FirebaseAuthUserCollisionException) {
                            Log.d("AUTH", "⚠️ Collision: ya vinculado, inicia con Google")
                            auth.signInWithCredential(credential).addOnCompleteListener { t2 ->
                                if (t2.isSuccessful) {
                                    Log.d("AUTH", "✅ SignIn con Google después de colisión")
                                    ensureUserProfile()
                                    goHome()
                                } else showError(t2.exception)
                            }
                        } else showError(ex)
                    }
                }
            } else {
                // === Caso: no hay sesión previa o el email no coincide ===
                Log.d("AUTH", "Rama: no hay sesión previa (o correo no coincide), revisando métodos del correo...")

                if (emailFromGoogle.isNullOrBlank()) {
                    // No vino email desde Google (raro)
                    Log.d("AUTH", "Rama: Google sin email; iniciar con Google directo")
                    auth.signInWithCredential(credential).addOnCompleteListener { t ->
                        if (t.isSuccessful) {
                            clearPrefs()
                            ensureUserProfile()
                            goHome()
                        } else showError(t.exception)
                    }
                } else {
                    auth.fetchSignInMethodsForEmail(emailFromGoogle)
                        .addOnSuccessListener { resultMethods ->
                            val methods = resultMethods.signInMethods ?: emptyList()

                            when {
                                // A) Ya existe con password → pedir pass, loguear y luego LINK Google
                                methods.contains("password") -> {
                                    Log.d("AUTH", "Rama: existe password para este email → pedir pass, login y luego link Google")
                                    promptPasswordToLink(emailFromGoogle) { password ->
                                        auth.signInWithEmailAndPassword(emailFromGoogle, password)
                                            .addOnCompleteListener { emailLogin ->
                                                if (emailLogin.isSuccessful) {
                                                    Log.d("AUTH", "Rama: login con email OK → link Google")
                                                    auth.currentUser?.linkWithCredential(credential)
                                                        ?.addOnCompleteListener { linkTask ->
                                                            if (linkTask.isSuccessful) {
                                                                Log.d("AUTH", "✅ linkWithCredential exitoso (Google -> Email tras login)")
                                                                if (binding.chkRemember.isChecked) savePrefs(emailFromGoogle, password) else clearPrefs()
                                                                ensureUserProfile()
                                                                goHome()
                                                            } else showError(linkTask.exception)
                                                        }
                                                } else showError(emailLogin.exception)
                                            }
                                    }
                                }

                                // B) Ya está Google → iniciar normal con Google
                                methods.contains("google.com") -> {
                                    Log.d("AUTH", "Rama: ya existe Google → signInWithCredential(Google)")
                                    auth.signInWithCredential(credential).addOnCompleteListener { t ->
                                        if (t.isSuccessful) {
                                            clearPrefs()
                                            ensureUserProfile()
                                            goHome()
                                        } else showError(t.exception)
                                    }
                                }

                                // C) No existe nada → crear con Google
                                else -> {
                                    Log.d("AUTH", "Rama: no hay métodos → crear con Google (signInWithCredential)")
                                    auth.signInWithCredential(credential).addOnCompleteListener { t ->
                                        if (t.isSuccessful) {
                                            clearPrefs()
                                            ensureUserProfile()
                                            goHome()
                                        } else showError(t.exception)
                                    }
                                }
                            }
                        }
                        .addOnFailureListener { e -> showError(e) }
                }
            }
        } catch (e: Exception) {
            showError(e)
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = getSharedPreferences("login_prefs", MODE_PRIVATE)

        // Inicializar CallbackManager
        callbackManager = CallbackManager.Factory.create()

        // --- Botón personalizado Facebook ---
        binding.btnFacebook.setOnClickListener {
            LoginManager.Companion.getInstance().logInWithReadPermissions(
                this,
                listOf("email", "public_profile")
            )
        }

        // Disparar flujo social si venimos desde Register
        when (intent.getStringExtra("ACTION_SOCIAL")) {
            "GOOGLE" -> binding.btnGoogle.performClick()
            "FACEBOOK" -> binding.btnFacebook.performClick()
        }

        // Registrar el callback
        LoginManager.Companion.getInstance().registerCallback(callbackManager,
            object : FacebookCallback<LoginResult> {
                override fun onSuccess(result: LoginResult) {
                    handleFacebookAccessToken(result.accessToken)
                }

                override fun onCancel() {
                    Toast.makeText(this@LoginActivity, "Login cancelado", Toast.LENGTH_SHORT).show()
                }

                override fun onError(error: FacebookException) {
                    showError(error)
                }
            })

        linkAfterRegister = intent?.getBooleanExtra("LINK_AFTER_REGISTER", false) == true

        // (opcional) si enviaste el email desde RegisterActivity
        intent?.getStringExtra("EMAIL_REGISTERED")?.let { email ->
            if (binding.etEmail.text.isNullOrBlank()) binding.etEmail.setText(email)
        }

        if (linkAfterRegister) {
            Toast.makeText(
                this,
                "Cuenta creada. Toca Google y elige el MISMO correo para vincular.",
                Toast.LENGTH_LONG
            ).show()
        }

        // --- Cargar datos recordados ---
        loadPrefs()

        // --- Validaciones en tiempo real ---
        setupValidation()

        // --- Botón ayuda ---
        setupHelpButton()

        // --- Biometría ---
        setupBiometricLogin()

        // --- LOGIN CORREO/CONTRASEÑA ---
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text?.toString()?.trim()?.lowercase().orEmpty()
            val pass = binding.etPassword.text?.toString()?.trim().orEmpty()

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.etEmail.error = "Ingresa un correo válido"
                return@setOnClickListener
            }
            if (pass.isEmpty()) {
                binding.etPassword.error = "Ingresa tu contraseña"
                return@setOnClickListener
            }

            auth.signInWithEmailAndPassword(email, pass)
                .addOnSuccessListener {
                    if (binding.chkRemember.isChecked) {
                        savePrefs(email, pass)
                    } else {
                        clearPrefs()
                    }
                    ensureUserProfile()
                    goHome()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, e.localizedMessage, Toast.LENGTH_LONG).show()
                }
        }

        binding.btnGoogle.setOnClickListener {
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()
            val client = GoogleSignIn.getClient(this, gso)

            // Forzar el chooser de cuentas
            client.signOut().addOnCompleteListener {
                googleLauncher.launch(client.signInIntent)
            }
        }

        // --- REGISTRO ---
        binding.tvCreateAccount.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        // --- OLVIDÉ CONTRASEÑA ---
        auth.setLanguageCode("es")
        binding.tvForgot.setOnClickListener {
            val email = binding.etEmail.text?.toString()?.trim().orEmpty()
            if (email.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                sendResetEmail(email)
            } else {
                showEmailDialog()
            }
        }

        // ------------------------------
        // ✨ ANIMACIONES DE ENTRADA
        // ------------------------------
        val fadeSlide = AnimationUtils.loadAnimation(this, R.anim.fade_in_slide_up)
        val fadeShort = AnimationUtils.loadAnimation(this, R.anim.fade_in_short)
        val bounce = AnimationUtils.loadAnimation(this, R.anim.bounce)

        // Logo principal
        binding.imgLogo.startAnimation(fadeSlide)

        // Card del login (inputs + botones)
        binding.loginCard.startAnimation(fadeSlide)

        // Aparecer con secuencia: botones sociales y recordarme
        fadeShort.startOffset = 500
        binding.btnGoogle.startAnimation(fadeShort)
        binding.btnFacebook.startAnimation(fadeShort)
        binding.chkRemember.startAnimation(fadeShort)
        binding.tvForgot.startAnimation(fadeShort)

        // Biometría aparece al final con rebote
        bounce.startOffset = 700
        binding.biometricSection.startAnimation(bounce)
    }


    override fun onStart() {
        super.onStart()
        val user = auth.currentUser
        if (user != null) {
            if (linkAfterRegister) {
                // Venimos de registro y queremos que el usuario toque Google para vincular
                return
            }
            ensureUserProfile()
            goHome()
        }
    }


    private fun handleFacebookAccessToken(token: AccessToken) {
        Log.d("FacebookLogin", "handleFacebookAccessToken:$token")

        val credential = FacebookAuthProvider.getCredential(token.token)
        FirebaseAuth.getInstance().signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = FirebaseAuth.getInstance().currentUser
                    Toast.makeText(this, "Bienvenido ${user?.displayName}", Toast.LENGTH_SHORT).show()

                    // ✅ Guardar/actualizar en Firestore igual que con Google
                    ensureUserProfile()

                    // Ir a Home
                    goHome()
                } else {
                    Toast.makeText(
                        this,
                        "Error en autenticación: ${task.exception?.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }



    // Recibir el resultado del login
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        callbackManager.onActivityResult(requestCode, resultCode, data)
    }

    private fun setupValidation() {
        val emailLayout = binding.etEmail.parent.parent as TextInputLayout
        val passLayout = binding.etPassword.parent.parent as TextInputLayout

        // watcher email
        binding.etEmail.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val email = s.toString().trim()
                when {
                    email.isEmpty() -> {
                        emailLayout.error = "Este campo es obligatorio"
                        emailLayout.helperText = null
                        emailLayout.boxStrokeWidth = 4
                        emailLayout.setBoxStrokeColor(Color.RED)
                    }
                    !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                        emailLayout.error = "Formato inválido (ej: usuario@mail.com)"
                        emailLayout.helperText = null
                        emailLayout.boxStrokeWidth = 4
                        emailLayout.setBoxStrokeColor(Color.parseColor("#FFA500"))
                    }
                    else -> {
                        emailLayout.error = null
                        emailLayout.helperText = "Correo válido ✅"
                        emailLayout.boxStrokeWidth = 5
                        emailLayout.setBoxStrokeColor(Color.parseColor("#4CAF50"))
                    }
                }
                toggleLoginButton()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // watcher password
        binding.etPassword.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val pass = s.toString()
                when {
                    pass.isEmpty() -> {
                        passLayout.error = "Contraseña inválida"
                        passLayout.helperText = null
                        passLayout.boxStrokeWidth = 4
                        passLayout.setBoxStrokeColor(Color.RED)
                    }
                    pass.length < 4 -> {
                        passLayout.error = "Muy débil ❌"
                        passLayout.helperText = null
                        passLayout.boxStrokeWidth = 5
                        passLayout.setBoxStrokeColor(Color.RED)
                    }
                    pass.length < 6 -> {
                        passLayout.error = null
                        passLayout.helperText = "Aceptable ⚠️"
                        passLayout.boxStrokeWidth = 5
                        passLayout.setBoxStrokeColor(Color.parseColor("#FFA500"))
                    }
                    pass.length < 10 -> {
                        passLayout.error = null
                        passLayout.helperText = "Segura ✅"
                        passLayout.boxStrokeWidth = 5
                        passLayout.setBoxStrokeColor(Color.parseColor("#8BC34A"))
                    }
                    else -> {
                        passLayout.error = null
                        passLayout.helperText = "Súper segura 🔒"
                        passLayout.boxStrokeWidth = 6
                        passLayout.setBoxStrokeColor(Color.parseColor("#2E7D32"))
                    }
                }
                toggleLoginButton()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun toggleLoginButton() {
        val emailValid = Patterns.EMAIL_ADDRESS.matcher(binding.etEmail.text?.toString().orEmpty()).matches()
        val passValid = !binding.etPassword.text.isNullOrEmpty()
        binding.btnLogin.isEnabled = emailValid && passValid
        binding.btnLogin.setBackgroundColor(
            if (binding.btnLogin.isEnabled) Color.parseColor("#4A90E2")
            else Color.GRAY
        )
    }

    private fun launchGoogle() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id)) // ✅ IMPORTANTE
            .requestEmail()
            .build()
        val client = GoogleSignIn.getClient(this, gso)
        googleLauncher.launch(client.signInIntent)
    }

    private fun goHome() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun showError(e: Exception?) {
        Toast.makeText(this, e?.localizedMessage ?: "Error al iniciar sesión", Toast.LENGTH_SHORT).show()
    }

    private fun showEmailDialog() {
        val input = TextInputEditText(this).apply {
            hint = getString(R.string.enter_your_email)
            inputType = InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.forgot_password))
            .setView(input)
            .setPositiveButton(getString(R.string.send)) { d, _ ->
                val email = input.text?.toString()?.trim().orEmpty()
                if (Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    sendResetEmail(email)
                } else {
                    Toast.makeText(this, "Correo inválido.", Toast.LENGTH_SHORT).show()
                }
                d.dismiss()
            }
            .setNegativeButton(getString(R.string.cancel)) { d, _ -> d.dismiss() }
            .show()
    }

    private fun sendResetEmail(email: String) {
        binding.tvForgot.isEnabled = false
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                binding.tvForgot.isEnabled = true
                if (task.isSuccessful) {
                    Toast.makeText(this, getString(R.string.reset_email_sent), Toast.LENGTH_LONG).show()
                } else {
                    val msg = task.exception?.localizedMessage ?: "No se pudo enviar el correo."
                    Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
                }
            }
    }
    private fun promptPasswordToLink(email: String, onOk: (String) -> Unit) {
        val input = TextInputEditText(this).apply {
            hint = getString(R.string.password)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.link_google_title))
            .setMessage(getString(R.string.link_google_message, email))
            .setView(input)
            .setPositiveButton(getString(R.string.link)) { _, _ ->
                val pass = input.text?.toString()?.trim().orEmpty()
                if (pass.isNotEmpty()) onOk(pass)
                else Toast.makeText(this, R.string.password_required, Toast.LENGTH_LONG).show()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun ensureUserProfile() {
        val user = auth.currentUser ?: return
        val uid = user.uid
        saveAccountId(uid)

        FirebaseHelper.getUser(uid) { data ->
            if (data != null) {
                val storedEmail = data["email"] as? String
                val storedDisplay = data["displayName"] as? String
                val needPatch = (storedEmail.isNullOrBlank() && !user.email.isNullOrBlank()) ||
                        (storedDisplay.isNullOrBlank() && !user.displayName.isNullOrBlank())
                if (needPatch) {
                    val providers = user.providerData.map { it.providerId }
                    val deviceId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
                    val deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}"
                    val patch = mutableMapOf<String, Any?>(
                        "updatedAt" to FieldValue.serverTimestamp(),
                        "providerIds" to providers,
                        "deviceId" to deviceId,
                        "deviceModel" to deviceModel
                    )
                    user.email?.let { patch["email"] = it }
                    user.displayName?.let { patch["displayName"] = it }
                    FirebaseHelper.updateUser(uid, patch) { _, _ -> }
                }
            } else {
                val providers = user.providerData.map { it.providerId }
                val deviceId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
                val deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}"
                val userDoc = hashMapOf(
                    "email" to (user.email ?: ""),
                    "fullName" to (user.displayName ?: ""),
                    "gender" to null,
                    "phone" to null,
                    "birthDate" to null,
                    "age" to null,
                    "providerIds" to providers,
                    "deviceId" to deviceId,
                    "deviceModel" to deviceModel,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                FirebaseHelper.createUser(uid, userDoc) { _, _ -> }
            }
        }
    }

    // --- SharedPreferences ---
    private fun savePrefs(email: String, pass: String) {
        prefs.edit().apply {
            putString("email", email)
            putString("pass", pass)
            putBoolean("remember", binding.chkRemember.isChecked)
            apply()
        }
    }

    private fun loadPrefs() {
        val remember = prefs.getBoolean("remember", false)
        if (remember) {
            binding.etEmail.setText(prefs.getString("email", ""))
            binding.etPassword.setText(prefs.getString("pass", ""))
            binding.chkRemember.isChecked = true
        }
    }

    private fun setupHelpButton() {
        binding.btnHelp.setOnClickListener {
            val message = """
            <b>Grupo 4</b><br><br>
            <b>Integrantes:</b><br>
            • <font color="#5B4E2D">AGUILAR CONTRERAS, Adrián Antonio</font><br>
            • <font color="#5B4E2D">DOMINGUEZ TERREROS, Jerson Manuel</font><br>
            • <font color="#5B4E2D">GARCIA ALAYO, Jimena Alexandra</font><br>
            • <font color="#5B4E2D">SANCHEZ PRIETO, Bruce Harbert</font><br>
            • <font color="#5B4E2D">TERRONES HUARACA, Christian Jose</font><br><br>
            <i>Trujillo - 2025</i>
        """.trimIndent()

            AlertDialog.Builder(this)
                .setTitle("Información del Grupo")
                .setMessage(HtmlCompat.fromHtml(message, HtmlCompat.FROM_HTML_MODE_LEGACY))
                .setPositiveButton("Cerrar") { dialog, _ -> dialog.dismiss() }
                .show()
        }
    }

    private fun saveAccountId(uid: String) {
        getSharedPreferences(ACCOUNT_PREFS, MODE_PRIVATE).edit().apply {
            putString("account_uid", uid)
            apply()
        }
    }
    private fun clearPrefs() {
        prefs.edit().clear().apply()
    }

    // --- Login con Huella Digital ---
    private fun setupBiometricLogin() {
        val executor = ContextCompat.getMainExecutor(this)

        val biometricPrompt = BiometricPrompt(
            this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Toast.makeText(
                        applicationContext,
                        "Error de autenticación: $errString", Toast.LENGTH_SHORT
                    ).show()
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    Toast.makeText(
                        applicationContext,
                        "Huella reconocida", Toast.LENGTH_SHORT
                    ).show()
                    val current = auth.currentUser
                    if (current != null) {
                        ensureUserProfile()
                        goHome()
                        return
                    }

                    val remembered = prefs.getBoolean("remember", false)
                    val email = prefs.getString("email", "")?.trim()?.lowercase().orEmpty()
                    val pass = prefs.getString("pass", "")?.trim().orEmpty()

                    if (remembered && email.isNotBlank() && pass.isNotBlank()) {
                        auth.signInWithEmailAndPassword(email, pass)
                            .addOnSuccessListener {
                                ensureUserProfile()
                                goHome()
                            }
                            .addOnFailureListener { e -> showError(e) }
                    } else {
                        Toast.makeText(
                            this@LoginActivity,
                            "Para usar la huella, habilita 'Recordar' con email y contraseña, o activa Anonymous en Firebase.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Toast.makeText(
                        applicationContext,
                        "Huella no reconocida", Toast.LENGTH_SHORT
                    ).show()
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Acceso Biométrico")
            .setSubtitle("Usa tu huella digital para iniciar sesión")
            .setNegativeButtonText("Cancelar")
            .build()

        binding.tvBiometric.setOnClickListener {
            val biometricManager = BiometricManager.from(this)
            when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
                BiometricManager.BIOMETRIC_SUCCESS -> biometricPrompt.authenticate(promptInfo)
                BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE ->
                    Toast.makeText(this, "No hay hardware biométrico en este dispositivo", Toast.LENGTH_LONG).show()
                BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE ->
                    Toast.makeText(this, "Hardware biométrico no disponible", Toast.LENGTH_LONG).show()
                BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED ->
                    Toast.makeText(this, "No hay huellas registradas", Toast.LENGTH_LONG).show()
            }
        }
    }
}