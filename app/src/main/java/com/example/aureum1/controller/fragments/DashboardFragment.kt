package com.example.aureum1.controller.fragments

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.aureum1.R

class DashboardFragment : Fragment(R.layout.fragment_dashboard) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Demo: setear textos (reemplaza con datos reales)
        view.findViewById<TextView>(R.id.tvResumenMes).text = "Resumen de Octubre"
        view.findViewById<TextView>(R.id.tvIngresos).text = "Ingresos: S/. 2,450"
        view.findViewById<TextView>(R.id.tvGastos).text = "Gastos: S/. 1,780"
        view.findViewById<TextView>(R.id.tvAhorro).text = "Ahorro: S/. 670"
    }
}