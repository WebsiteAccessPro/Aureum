package com.example.aureum1.controller.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.aureum1.R

/**
 * Pantalla intermedia: ¿Deseas seleccionar un registro existente?
 * - btnSeleccionar → abre SeleccionRegistroDeudaActivity y devuelve el resultado al llamador.
 * - btnOmitir → abre el formulario según la acción inicial (Prestó o Me prestaron).
 */
class WalletSelectRecordActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_DEFAULT_ACCION = "DEFAULT_ACCION"
        private const val REQ_SELECCION_REGISTRO_FORWARD = 902
    }

    private var defaultAccion: String = "presto"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_wallet_select_record)

        defaultAccion = intent?.getStringExtra(EXTRA_DEFAULT_ACCION) ?: "presto"

        val btnSeleccionar = findViewById<Button>(R.id.btnSeleccionar)
        val btnOmitir = findViewById<TextView>(R.id.btnOmitir)

        btnSeleccionar.setOnClickListener {
            val i = Intent(this, SeleccionRegistroDeudaActivity::class.java)
            startActivityForResult(i, REQ_SELECCION_REGISTRO_FORWARD)
        }

        btnOmitir.setOnClickListener {
            if (defaultAccion == "presto") {
                val i = Intent(this, PrestoActivity::class.java)
                startActivity(i)
            } else {
                val i = Intent(this, MePrestaronActivity::class.java)
                startActivity(i)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_SELECCION_REGISTRO_FORWARD && resultCode == Activity.RESULT_OK && data != null) {
            setResult(Activity.RESULT_OK, data)
            finish()
        }
    }
}