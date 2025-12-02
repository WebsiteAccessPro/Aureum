package com.example.aureum1.controller.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
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

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbarWalletSelect)
        toolbar.setNavigationOnClickListener { finish() }

        val btnSeleccionar = findViewById<MaterialButton>(R.id.btnSeleccionar)
        val btnOmitir = findViewById<MaterialButton>(R.id.btnOmitir)

        btnSeleccionar.setOnClickListener {
            val i = Intent(this, SeleccionRegistroDeudaActivity::class.java)
            i.putExtra(EXTRA_DEFAULT_ACCION, defaultAccion)
            i.putExtra("FILTER_SOURCE", "wallet")
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
            val cuenta = data.getStringExtra("SELECTED_RECORD_CUENTA").orEmpty()
            val moneda = data.getStringExtra("SELECTED_RECORD_MONEDA").orEmpty()
            val monto = data.getDoubleExtra("SELECTED_RECORD_MONTO", 0.0)
            val tipo  = data.getStringExtra("SELECTED_RECORD_TIPO").orEmpty()
            val fechaTxt = data.getStringExtra("SELECTED_RECORD_FECHA_TEXTO").orEmpty()

            val i = if (defaultAccion == "presto") Intent(this, PrestoActivity::class.java) else Intent(this, MePrestaronActivity::class.java)
            i.putExtra("PREFILL_CUENTA", cuenta)
            i.putExtra("PREFILL_MONEDA", moneda)
            i.putExtra("PREFILL_MONTO", monto)
            val fechaIso = data.getStringExtra("SELECTED_RECORD_FECHA_ISO").orEmpty()
            i.putExtra("PREFILL_FECHA_ISO", fechaIso)
            startActivity(i)
            finish()
        }
    }
}
