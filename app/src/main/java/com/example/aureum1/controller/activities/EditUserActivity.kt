package com.example.aureum1.controller.activities

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.aureum1.R
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class EditUserActivity : AppCompatActivity() {

    // UI
    private lateinit var toolbar: MaterialToolbar
    private lateinit var etFullName: TextInputEditText
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPhone: TextInputEditText
    private lateinit var etGender: MaterialAutoCompleteTextView
    private lateinit var etBirthDate: TextInputEditText
    private lateinit var etAge: TextInputEditText

    // Firebase
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            setContentView(R.layout.activity_edit_user)

            // Verificar que el usuario esté autenticado antes de continuar
            if (auth.currentUser == null) {
                Toast.makeText(this, "Usuario no autenticado", Toast.LENGTH_SHORT).show()
                finish()
                return
            }

            // Bind con manejo de excepciones
            try {
                toolbar     = findViewById(R.id.toolbarEditarPerfil)
                etFullName  = findViewById(R.id.etFullName)
                etEmail     = findViewById(R.id.etEmail)
                etPhone     = findViewById(R.id.etPhone)
                etGender    = findViewById(R.id.etGender)
                etBirthDate = findViewById(R.id.etBirthDate)
                etAge       = findViewById(R.id.etAge)
            } catch (e: Exception) {
                Toast.makeText(this, "Error al inicializar interfaz: ${e.message}", Toast.LENGTH_LONG).show()
                finish()
                return
            }

            // Toolbar
            toolbar.setNavigationOnClickListener { finish() }
            toolbar.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_save -> {
                        try {
                            guardarPerfil()
                        } catch (e: Exception) {
                            Toast.makeText(this, "Error al guardar: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                        true
                    }
                    else -> false
                }
            }

            // Ocultar el ítem de eliminar y otros mas
            toolbar.menu.findItem(R.id.action_delete)?.isVisible = false
            toolbar.menu.findItem(R.id.action_forgive)?.isVisible = false
            toolbar.menu.findItem(R.id.action_close)?.isVisible = false
            toolbar.menu.findItem(R.id.action_edit_debt)?.isVisible = false

            // Géneros
            try {
                val generos = listOf("Masculino", "Femenino", "No Definido")
                val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, generos)
                etGender.setAdapter(adapter)
            } catch (e: Exception) {
                Toast.makeText(this, "Error al configurar géneros", Toast.LENGTH_SHORT).show()
            }

            // Date picker
            etBirthDate.setOnClickListener {
                try {
                    mostrarDatePicker()
                } catch (e: Exception) {
                    Toast.makeText(this, "Error al abrir calendario", Toast.LENGTH_SHORT).show()
                }
            }
            // Cargar datos actuales
            cargarPerfil()

        } catch (e: Exception) {
            Toast.makeText(this, "Error al iniciar actividad: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun cargarPerfil() {
        val user = auth.currentUser ?: run {
            Toast.makeText(this, "Usuario no autenticado", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        val uid = user.uid

        try {
            // Establecer email del usuario
            etEmail.setText(user.email ?: "")

            db.collection("users").document(uid).get()
                .addOnSuccessListener { snap ->
                    try {
                        if (!snap.exists()) {
                            Toast.makeText(this, "Perfil no encontrado", Toast.LENGTH_SHORT).show()
                            return@addOnSuccessListener
                        }

                        // Nombre
                        val nombre = snap.getString("displayName")
                            ?: snap.getString("fullName")
                            ?: user.displayName
                            ?: ""
                        etFullName.setText(nombre)

                        // Teléfono: puede ser String o Number
                        val phoneValue = snap.get("phone")
                        val phoneText = when (phoneValue) {
                            is String -> phoneValue
                            is Number -> phoneValue.toLong().toString()
                            else -> ""
                        }
                        etPhone.setText(phoneText)
                        etPhone.isEnabled = false

                        // Género
                        val gender = snap.getString("gender") ?: ""
                        etGender.setText(gender, false)

                        // Fecha de nacimiento
                        val birthRaw = snap.get("birthDate")?.let {
                            when (it) {
                                is String -> it
                                is com.google.firebase.Timestamp -> dateFormat.format(it.toDate())
                                else -> ""
                            }
                        } ?: ""
                        etBirthDate.setText(birthRaw)

                        // Edad
                        if (birthRaw.isNotEmpty()) {
                            val edad = calcularEdad(birthRaw)
                            etAge.setText(edad.toString())
                        } else {
                            val ageVal = snap.get("age")?.toString() ?: ""
                            etAge.setText(ageVal)
                        }

                    } catch (e: Exception) {
                        Toast.makeText(
                            this,
                            "Error al procesar datos del perfil: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error al cargar perfil: ${e.message}", Toast.LENGTH_SHORT).show()
                }

        } catch (e: Exception) {
            Toast.makeText(this, "Error al cargar datos: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun mostrarDatePicker() {
        try {
            val cal = Calendar.getInstance()
            val actual = etBirthDate.text?.toString()?.takeIf { it.isNotBlank() }
            if (actual != null) {
                try {
                    val parsedDate = dateFormat.parse(actual)
                    if (parsedDate != null) {
                        cal.time = parsedDate
                    }
                } catch (e: Exception) {
                    android.util.Log.w("EditUserActivity", "Error al parsear fecha actual: ${e.message}")
                }
            }

            // Limitar la fecha máxima a hoy
            val year = cal.get(Calendar.YEAR)
            val month = cal.get(Calendar.MONTH)
            val day = cal.get(Calendar.DAY_OF_MONTH)

            val datePickerDialog = DatePickerDialog(this, { _, y, m, d ->
                try {
                    val str = String.format(Locale.getDefault(), "%02d/%02d/%04d", d, m + 1, y)
                    etBirthDate.setText(str)
                    val edad = calcularEdad(str)
                    etAge.setText(edad.toString())
                } catch (e: Exception) {
                    android.util.Log.e("EditUserActivity", "Error al procesar fecha seleccionada: ${e.message}")
                    Toast.makeText(this, "Error al procesar fecha", Toast.LENGTH_SHORT).show()
                }
            }, year, month, day)

            // Establecer fecha máxima como hoy
            datePickerDialog.datePicker.maxDate = System.currentTimeMillis()
            datePickerDialog.show()

        } catch (e: Exception) {
            android.util.Log.e("EditUserActivity", "Error al mostrar date picker: ${e.message}")
            Toast.makeText(this, "Error al abrir calendario", Toast.LENGTH_SHORT).show()
        }
    }

    private fun calcularEdad(birthStr: String): Int {
        return try {
            if (birthStr.isBlank()) return 0
            val birth = dateFormat.parse(birthStr) ?: return 0
            val calBirth = Calendar.getInstance().apply { time = birth }
            val calNow = Calendar.getInstance()

            // Verificar que la fecha de nacimiento no sea futura
            if (calBirth.after(calNow)) return 0

            var age = calNow.get(Calendar.YEAR) - calBirth.get(Calendar.YEAR)
            if (calNow.get(Calendar.DAY_OF_YEAR) < calBirth.get(Calendar.DAY_OF_YEAR)) age--
            if (age < 0) 0 else age
        } catch (e: Exception) {
            android.util.Log.e("EditUserActivity", "Error al calcular edad: ${e.message}")
            0
        }
    }

    private fun guardarPerfil() {
        val user = auth.currentUser ?: run {
            Toast.makeText(this, "Usuario no autenticado", Toast.LENGTH_SHORT).show()
            finish();
            return
        }
        val uid = user.uid

        try {
            val nombre = etFullName.text?.toString()?.trim() ?: ""
            val phone  = etPhone.text?.toString()?.trim()?.takeIf { it.isNotBlank() }
            val gender = etGender.text?.toString()?.trim()?.takeIf { it.isNotBlank() }
            val birth  = etBirthDate.text?.toString()?.trim()?.takeIf { it.isNotBlank() }
            val age    = birth?.let { calcularEdad(it) }

            val patch = mutableMapOf<String, Any?>()
            if (nombre.isNotBlank()) {
                patch["displayName"] = nombre
                patch["fullName"] = nombre
            }
            patch["phone"] = phone
            patch["gender"] = gender
            patch["birthDate"] = birth
            if (age != null) patch["age"] = age
            patch["updatedAt"] = FieldValue.serverTimestamp()

            // Validar datos antes de guardar
            if (nombre.isBlank()) {
                Toast.makeText(this, "Por favor ingrese un nombre", Toast.LENGTH_SHORT).show()
                return
            }

            // Usar set con merge para permitir valores nulos sin crashes
            db.collection("users").document(uid).set(patch, SetOptions.merge())
                .addOnSuccessListener {
                    Toast.makeText(this, "Perfil actualizado", Toast.LENGTH_SHORT).show()
                    setResult(RESULT_OK)
                    finish()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error al guardar: ${e.message}", Toast.LENGTH_LONG).show()
                }
        } catch (e: Exception) {
            Toast.makeText(this, "Error al preparar datos para guardar: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}