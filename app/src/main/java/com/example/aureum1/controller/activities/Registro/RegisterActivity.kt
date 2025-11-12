package com.example.aureum1.controller.activities.Registro

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.example.aureum1.R
import com.example.aureum1.Backend.FirebaseHelper
import com.example.aureum1.controller.activities.LoginActivity
import com.example.aureum1.controller.activities.MainActivity
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: com.google.firebase.firestore.FirebaseFirestore

    private lateinit var viewPager: ViewPager2
    private lateinit var btnGoBack: MaterialButton
    private lateinit var stepIndicator: LinearLayout

    private lateinit var step1Circle: View
    private lateinit var step2Circle: View
    private lateinit var step3Circle: View
    private lateinit var step1Number: TextView
    private lateinit var step2Number: TextView
    private lateinit var step3Number: TextView
    private lateinit var connector1: View
    private lateinit var connector2: View

    private lateinit var pagerAdapter: RegisterPagerAdapter

    private var email: String = ""
    private var password: String = ""
    private var fullName: String = ""
    private var phone: String = ""
    private var gender: String = ""
    private var birthDate: String = ""
    private var age: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = FirebaseHelper.auth
        db = FirebaseHelper.db

        initializeViews()
        setupViewPager()
        setupButtonListeners()
        updateStepIndicator(0)

        onBackPressedDispatcher.addCallback(this) {
            val current = viewPager.currentItem
            if (current > 0) {
                goToPreviousStep()
            } else {
                finish()
            }
        }
    }

    private fun initializeViews() {
        viewPager = findViewById(R.id.viewPager)
        btnGoBack = findViewById(R.id.btnGoBack)
        stepIndicator = findViewById(R.id.stepIndicator)

        step1Circle = findViewById(R.id.step1Circle)
        step2Circle = findViewById(R.id.step2Circle)
        step3Circle = findViewById(R.id.step3Circle)
        step1Number = findViewById(R.id.step1Number)
        step2Number = findViewById(R.id.step2Number)
        step3Number = findViewById(R.id.step3Number)
        connector1 = findViewById(R.id.connector1)
        connector2 = findViewById(R.id.connector2)
    }

    private fun setupViewPager() {
        pagerAdapter = RegisterPagerAdapter(this)
        viewPager.adapter = pagerAdapter
        viewPager.isUserInputEnabled = false

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateStepIndicator(position)
            }
        })

        // Configurar callbacks de los fragments
        setupFragmentCallbacks()
    }

    private fun setupFragmentCallbacks() {
        // Configurar callbacks cuando los fragmentos estén disponibles
        viewPager.post {
            val fragmentManager = supportFragmentManager
            
            // Fragmento Paso 1
            val step1Fragment = fragmentManager.findFragmentByTag("f0") as? RegisterStep1Fragment
            step1Fragment?.let {
                it.onNextClicked = { handleNextClick() }
                it.onSocialLoginSuccess = {
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }
            }
            
            // Fragmento Paso 2
            val step2Fragment = fragmentManager.findFragmentByTag("f1") as? RegisterStep2Fragment
            step2Fragment?.let {
                it.onNextClicked = { handleNextClick() }
                it.onValidationComplete = { name, phone, gender, birthDate, age ->
                    this.fullName = name
                    this.phone = phone
                    this.gender = gender
                    this.birthDate = birthDate
                    this.age = age
                }
            }
        }
    }

    private fun setupButtonListeners() {
        btnGoBack.setOnClickListener {
            val current = viewPager.currentItem
            if (current > 0) {
                goToPreviousStep()
            } else {
                finish()
            }
        }
    }

    fun handleNextClick() {
        val currentPosition = viewPager.currentItem
        val fragmentManager = supportFragmentManager

        when (currentPosition) {
            0 -> {
                val fragment = fragmentManager.findFragmentByTag("f0") as? RegisterStep1Fragment
                fragment?.let {
                    // Validar los datos del paso 1
                    if (it.validateAndProceed()) {
                        val data = it.getData()
                        if (data != null) {
                            // Guardar los datos del paso 1
                            email = data.first
                            password = data.second
                            // Ir al siguiente paso
                            goToNextStep()
                        }
                    }
                }
            }

            1 -> {
                val fragment = fragmentManager.findFragmentByTag("f1") as? RegisterStep2Fragment
                fragment?.let {
                    // Validar los datos del paso 2
                    if (it.validateAndProceed()) {
                        val personalData = it.getData()
                        val birthData = it.getBirthData()

                        // Guardar los datos del paso 2
                        fullName = personalData.first
                        phone = personalData.second
                        gender = personalData.third
                        birthDate = birthData.first
                        age = birthData.second
                        
                        // Ir al siguiente paso y crear cuenta
                        goToNextStep()
                        createAccount()
                    }
                }
            }

            2 -> {
                // Ir a la actividad principal
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
        }
    }

    private fun goToNextStep() {
        val current = viewPager.currentItem
        if (current < 2) viewPager.setCurrentItem(current + 1, true)
    }

    private fun goToPreviousStep() {
        val current = viewPager.currentItem
        if (current > 0) viewPager.setCurrentItem(current - 1, true)
    }

    // Header back button is always visible; no bottom buttons to update.
    private fun updateButtonStates(position: Int) { /* no-op */ }

    private fun updateStepIndicator(currentStep: Int) {
        step1Circle.setBackgroundResource(R.drawable.circle_step_inactive)
        step2Circle.setBackgroundResource(R.drawable.circle_step_inactive)
        step3Circle.setBackgroundResource(R.drawable.circle_step_inactive)

        step1Number.setTextColor(resources.getColor(android.R.color.darker_gray))
        step2Number.setTextColor(resources.getColor(android.R.color.darker_gray))
        step3Number.setTextColor(resources.getColor(android.R.color.darker_gray))

        connector1.setBackgroundColor(resources.getColor(android.R.color.darker_gray))
        connector2.setBackgroundColor(resources.getColor(android.R.color.darker_gray))

        when (currentStep) {
            0 -> {
                step1Circle.setBackgroundResource(R.drawable.circle_step_active)
                step1Number.setTextColor(resources.getColor(android.R.color.white))
            }
            1 -> {
                step1Circle.setBackgroundResource(R.drawable.circle_step_active)
                step2Circle.setBackgroundResource(R.drawable.circle_step_active)
                step1Number.setTextColor(resources.getColor(android.R.color.white))
                step2Number.setTextColor(resources.getColor(android.R.color.white))
                connector1.setBackgroundColor(resources.getColor(R.color.aureum_gold))
            }
            2 -> {
                step1Circle.setBackgroundResource(R.drawable.circle_step_active)
                step2Circle.setBackgroundResource(R.drawable.circle_step_active)
                step3Circle.setBackgroundResource(R.drawable.circle_step_active)
                step1Number.setTextColor(resources.getColor(android.R.color.white))
                step2Number.setTextColor(resources.getColor(android.R.color.white))
                step3Number.setTextColor(resources.getColor(android.R.color.white))
                connector1.setBackgroundColor(resources.getColor(R.color.aureum_gold))
                connector2.setBackgroundColor(resources.getColor(R.color.aureum_gold))
            }
        }
    }

    private fun createAccount() {
        val fragmentManager = supportFragmentManager
        val step3Fragment = fragmentManager.findFragmentByTag("f2") as? RegisterStep3Fragment
        step3Fragment?.showLoading()

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    step3Fragment?.hideLoading()

                    val msg = when (val ex = task.exception) {
                        is FirebaseAuthUserCollisionException -> "Ese correo ya está registrado."
                        else -> "Error: ${ex?.message ?: "intenta de nuevo"}"
                    }

                    Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
                    viewPager.setCurrentItem(0, true)
                    return@addOnCompleteListener
                }

                val uid = auth.currentUser?.uid ?: run {
                    step3Fragment?.hideLoading()
                    Toast.makeText(this, "No se pudo obtener el UID", Toast.LENGTH_LONG).show()
                    viewPager.setCurrentItem(0, true)
                    return@addOnCompleteListener
                }

                val userDoc = mapOf(
                    "email" to email,
                    "fullName" to fullName,
                    "gender" to gender,
                    "phone" to phone,
                    "birthDate" to birthDate,
                    "age" to age,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )

                FirebaseHelper.createUser(uid, userDoc) { ok, err ->
                    if (ok) {
                        autoLogin()
                    } else {
                        step3Fragment?.hideLoading()
                        Toast.makeText(this, "Error guardando perfil: ${err ?: "intenta de nuevo"}", Toast.LENGTH_LONG).show()
                        viewPager.setCurrentItem(1, true)
                    }
                }
            }
    }

    private fun autoLogin() {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                val fragmentManager = supportFragmentManager
                val step3Fragment = fragmentManager.findFragmentByTag("f2") as? RegisterStep3Fragment
                step3Fragment?.hideLoading()

                if (task.isSuccessful) {
                    viewPager.postDelayed({ goHome() }, 1500)
                } else {
                    Toast.makeText(this, "Error al iniciar sesión automáticamente", Toast.LENGTH_LONG).show()
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                }
            }
    }

    private fun goHome() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}