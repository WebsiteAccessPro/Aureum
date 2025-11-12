package com.example.aureum1.controller.activities.Registro

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.aureum1.R
import com.google.android.material.progressindicator.CircularProgressIndicator

class RegisterStep3Fragment : Fragment() {
    
    private lateinit var progressIndicator: CircularProgressIndicator
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_register_step3, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        progressIndicator = view.findViewById(R.id.progressIndicator)
    }
    
    fun showLoading() {
        progressIndicator.visibility = View.VISIBLE
    }
    
    fun hideLoading() {
        progressIndicator.visibility = View.GONE
    }
}