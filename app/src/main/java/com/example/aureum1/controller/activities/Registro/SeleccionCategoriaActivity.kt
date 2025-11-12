package com.example.aureum1.controller.activities.Registro

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.aureum1.R
import com.example.aureum1.controller.adapters.CategoriaAdapter
import com.google.android.material.appbar.MaterialToolbar

class SeleccionCategoriaActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var rv: RecyclerView
    private lateinit var adapter: CategoriaAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_seleccion_categoria)

        toolbar = findViewById(R.id.toolbarSeleccionCategoria)
        rv = findViewById(R.id.recyclerSeleccionCategoria)

        toolbar.setNavigationOnClickListener { finish() }

        rv.layoutManager = LinearLayoutManager(this)
        rv.setHasFixedSize(true)

        // 🔹 Recibir tipo de categoría
        val tipo = intent.getStringExtra("TIPO_CATEGORIA") ?: "Ingreso"

        val categorias = if (tipo == "Ingreso") getCategoriasIngreso() else getCategoriasGasto()

        adapter = CategoriaAdapter(categorias) { categoria ->
            val resultIntent = Intent()
            resultIntent.putExtra("CATEGORIA_SELECCIONADA", categoria)
            setResult(RESULT_OK, resultIntent)
            finish()
        }

        rv.adapter = adapter
    }

    private fun getCategoriasIngreso(): List<String> {
        return listOf(
            "Sueldo o salario",
            "Ingresos por alquiler",
            "Venta de productos",
            "Comisiones",
            "Intereses bancarios",
            "Reembolsos",
            "Premios o sorteos",
            "Inversiones",
            "Bonificaciones",
            "Otros ingresos"
        )
    }

    private fun getCategoriasGasto(): List<String> {
        return listOf(
            "Alimentación",
            "Transporte",
            "Educación",
            "Salud",
            "Ropa y calzado",
            "Servicios (agua, luz, internet)",
            "Entretenimiento y ocio",
            "Viajes",
            "Mascotas",
            "Impuestos",
            "Donaciones",
            "Otros gastos"
        )
    }
}