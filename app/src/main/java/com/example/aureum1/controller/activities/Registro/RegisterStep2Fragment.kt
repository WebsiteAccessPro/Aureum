package com.example.aureum1.controller.activities.Registro

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.aureum1.R
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.button.MaterialButton
import java.text.SimpleDateFormat
import java.util.*

class RegisterStep2Fragment : Fragment() {
    
    private lateinit var tilName: TextInputLayout
    private lateinit var tilPhone: TextInputLayout
    private lateinit var tilGender: TextInputLayout
    private lateinit var tilBirthDate: TextInputLayout
    private lateinit var etName: TextInputEditText
    private lateinit var etPhone: TextInputEditText
    private lateinit var actvGender: AutoCompleteTextView
    private lateinit var etBirthDate: TextInputEditText
    private lateinit var tvAge: TextView
    private lateinit var btnNext: MaterialButton
    
    private val calendar = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    
    var onValidationComplete: ((name: String, phone: String, gender: String, birthDate: String, age: Int) -> Unit)? = null
    var onNextClicked: (() -> Unit)? = null
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_register_step2, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        tilName = view.findViewById(R.id.tilName)
        tilPhone = view.findViewById(R.id.tilPhone)
        tilGender = view.findViewById(R.id.tilGender)
        tilBirthDate = view.findViewById(R.id.tilBirthDate)
        etName = view.findViewById(R.id.etName)
        etPhone = view.findViewById(R.id.etPhone)
        actvGender = view.findViewById(R.id.actvGender)
        etBirthDate = view.findViewById(R.id.etBirthDate)
        tvAge = view.findViewById(R.id.tvAge)
        btnNext = view.findViewById(R.id.btnNext)
        
        setupGenderDropdown()
        setupDatePicker()
        
        btnNext.setOnClickListener {
            if (validateAndProceed()) {
                // Llamar directamente al Activity para avanzar
                (activity as? RegisterActivity)?.handleNextClick()
            }
        }
    }
    
    private fun setupGenderDropdown() {
        val genders = listOf("Masculino", "Femenino", "Prefiero no decir")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, genders)
        actvGender.setAdapter(adapter)
    }
    
    private fun setupDatePicker() {
        etBirthDate.setOnClickListener {
            showDatePicker()
        }
        
        etBirthDate.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                showDatePicker()
            }
        }
    }
    
    private fun showDatePicker() {
        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                val selectedDate = dateFormat.format(calendar.time)
                etBirthDate.setText(selectedDate)
                calculateAge()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        
        // Limitar fecha máxima a 18 años atrás
        val maxDate = Calendar.getInstance()
        maxDate.add(Calendar.YEAR, -18)
        datePickerDialog.datePicker.maxDate = maxDate.timeInMillis
        
        datePickerDialog.show()
    }
    
    private fun calculateAge() {
        val today = Calendar.getInstance()
        val birthDate = Calendar.getInstance()
        birthDate.time = calendar.time
        
        var age = today.get(Calendar.YEAR) - birthDate.get(Calendar.YEAR)
        
        if (today.get(Calendar.DAY_OF_YEAR) < birthDate.get(Calendar.DAY_OF_YEAR)) {
            age--
        }
        
        tvAge.text = "Edad: $age años"
        tvAge.visibility = View.VISIBLE
    }
    
    fun validateAndProceed(): Boolean {
        val name = etName.text?.toString()?.trim().orEmpty()
        val phone = etPhone.text?.toString()?.trim().orEmpty()
        val gender = actvGender.text?.toString()?.trim().orEmpty()
        val birthDate = etBirthDate.text?.toString()?.trim().orEmpty()
        
        var isValid = true
        
        if (name.isEmpty()) {
            tilName.error = "Ingresa tu nombre"
            isValid = false
        } else {
            tilName.error = null
        }
        
        if (phone.isEmpty()) {
            tilPhone.error = "Ingresa tu teléfono"
            isValid = false
        } else if (phone.length < 9) {
            tilPhone.error = "Número inválido"
            isValid = false
        } else {
            tilPhone.error = null
        }
        
        if (gender.isEmpty()) {
            tilGender.error = "Selecciona tu género"
            isValid = false
        } else {
            tilGender.error = null
        }
        
        if (birthDate.isEmpty()) {
            tilBirthDate.error = "Selecciona tu fecha de nacimiento"
            isValid = false
        } else {
            tilBirthDate.error = null
        }
        
        if (isValid) {
            val age = calculateAgeFromBirthDate(birthDate)
            onValidationComplete?.invoke(name, phone, gender, birthDate, age)
        }
        
        return isValid
    }
    
    private fun calculateAgeFromBirthDate(birthDateStr: String): Int {
        return try {
            val birthDate = dateFormat.parse(birthDateStr)
            val today = Calendar.getInstance()
            val birthCalendar = Calendar.getInstance()
            birthCalendar.time = birthDate
            
            var age = today.get(Calendar.YEAR) - birthCalendar.get(Calendar.YEAR)
            
            if (today.get(Calendar.DAY_OF_YEAR) < birthCalendar.get(Calendar.DAY_OF_YEAR)) {
                age--
            }
            
            age
        } catch (e: Exception) {
            0
        }
    }
    
    fun getData(): Triple<String, String, String> {
        return Triple(
            etName.text?.toString()?.trim().orEmpty(),
            etPhone.text?.toString()?.trim().orEmpty(),
            actvGender.text?.toString()?.trim().orEmpty()
        )
    }
    
    fun getBirthData(): Pair<String, Int> {
        val birthDate = etBirthDate.text?.toString()?.trim().orEmpty()
        val age = calculateAgeFromBirthDate(birthDate)
        return Pair(birthDate, age)
    }
}