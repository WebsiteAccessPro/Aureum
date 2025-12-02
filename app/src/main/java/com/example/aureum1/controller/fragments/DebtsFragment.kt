package com.example.aureum1.controller.fragments

import android.app.DatePickerDialog
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.aureum1.R
import com.example.aureum1.controller.activities.Registro.SeleccionCuentaActivity
import com.example.aureum1.controller.adapters.DebtAdapter
import com.example.aureum1.model.repository.AccountRepository
import com.example.aureum1.model.repository.DebtRepository
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ListenerRegistration

class DebtsFragment : Fragment(R.layout.fragment_debts) {

    companion object {
        private const val REQ_SELECCION_CUENTA = 801
        private const val REQ_SELECCION_REGISTRO = 802
    }

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val repo by lazy { DebtRepository() }
    private val accountRepo by lazy { AccountRepository() }

    private var listenerPresto: ListenerRegistration? = null
    private var listenerMePrestaron: ListenerRegistration? = null
    private var accountsInfoByName: Map<String, Map<String, Any?>> = emptyMap()

    private lateinit var rvPresto: RecyclerView
    private lateinit var rvMePrestaron: RecyclerView
    private lateinit var adapterPresto: DebtAdapter
    private lateinit var adapterMePrestaron: DebtAdapter

    // Estados temporales para el formulario
    private var tmpCuentaSeleccionada: String? = null
    private var tmpMonto: Double = 0.0
    private var tmpFecha: java.util.Calendar = java.util.Calendar.getInstance()
    private var tmpFechaVenc: java.util.Calendar? = null
    private var pendingDebtId: String? = null
    private var pendingAccion: String? = null
    private var pendingNombre: String? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tabActivo = view.findViewById<TextView>(R.id.tabActivo)
        val tabCerrado = view.findViewById<TextView>(R.id.tabCerrado)
        tabActivo?.isSelected = true
        tabCerrado?.isSelected = false
        tabActivo?.setOnClickListener { }
        tabCerrado?.setOnClickListener {
            tabActivo?.isSelected = false
            tabCerrado?.isSelected = true
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, DebtsClosedFragment(), "DEBTS_CLOSED")
                .commit()
        }

        view.findViewById<android.widget.ImageView>(R.id.btnBuscarDeuda)?.setOnClickListener {
            val i = Intent(requireContext(), com.example.aureum1.controller.activities.DebtSearchActivity::class.java)
            i.putExtra(com.example.aureum1.controller.activities.DebtSearchActivity.EXTRA_SOURCE, "activo")
            startActivity(i)
        }

        rvPresto = view.findViewById(R.id.rvDeudasPresto)
        rvMePrestaron = view.findViewById(R.id.rvDeudasMePrestaron)
        val tvEmptyPresto = view.findViewById<TextView>(R.id.tvEmptyPresto)
        val tvEmptyMePrestaron = view.findViewById<TextView>(R.id.tvEmptyMePrestaron)
        val cardEmptyPresto = view.findViewById<CardView>(R.id.cardEmptyPresto)
        val cardEmptyMePrestaron = view.findViewById<CardView>(R.id.cardEmptyMePrestaron)
        rvPresto.layoutManager = LinearLayoutManager(requireContext())
        rvMePrestaron.layoutManager = LinearLayoutManager(requireContext())
        rvPresto.setHasFixedSize(false)
        rvMePrestaron.setHasFixedSize(false)
        rvPresto.isNestedScrollingEnabled = false
        rvMePrestaron.isNestedScrollingEnabled = false

        adapterPresto = DebtAdapter(emptyList(), emptyMap(), "presto", onAddRegistroClick = { acc, item ->
            val nombre = (item["nombrePresto"] as? String)
                ?: (item["nombre"] as? String)
                ?: ""
            val debtId = (item["_id"] as? String).orEmpty()
            mostrarModalOpciones(acc, nombre, debtId)
        }, onItemClick = { acc, item ->
            val nombre = (item["nombrePresto"] as? String)
                ?: (item["nombre"] as? String)
                ?: ""
            val i = Intent(requireContext(), com.example.aureum1.controller.activities.RegistrosDeudaActivity::class.java)
            i.putExtra("ACCION", acc)
            i.putExtra("DEBT_ID", (item["_id"] as? String).orEmpty())
            i.putExtra("NOMBRE", nombre)
            i.putExtra("SOURCE", "activo")
            startActivity(i)
        })
        adapterMePrestaron = DebtAdapter(emptyList(), emptyMap(), "me_prestaron", onAddRegistroClick = { acc, item ->
            val nombre = (item["nombreMeprestaron"] as? String)
                ?: (item["nombre"] as? String)
                ?: ""
            val debtId = (item["_id"] as? String).orEmpty()
            mostrarModalOpciones(acc, nombre, debtId)
        }, onItemClick = { acc, item ->
            val nombre = (item["nombreMeprestaron"] as? String)
                ?: (item["nombre"] as? String)
                ?: ""
            val i = Intent(requireContext(), com.example.aureum1.controller.activities.RegistrosDeudaActivity::class.java)
            i.putExtra("ACCION", acc)
            i.putExtra("DEBT_ID", (item["_id"] as? String).orEmpty())
            i.putExtra("NOMBRE", nombre)
            i.putExtra("SOURCE", "activo")
            startActivity(i)
        })
        rvPresto.adapter = adapterPresto
        rvMePrestaron.adapter = adapterMePrestaron



        // Botones secundarios (ocultos/mostrados por el FAB): abren flujo wallet_select_record
        view.findViewById<MaterialButton>(R.id.btnPresto)?.setOnClickListener {
            val i = Intent(requireContext(), com.example.aureum1.controller.activities.WalletSelectRecordActivity::class.java)
            i.putExtra(com.example.aureum1.controller.activities.WalletSelectRecordActivity.EXTRA_DEFAULT_ACCION, "presto")
            startActivityForResult(i, REQ_SELECCION_REGISTRO)
        }
        view.findViewById<MaterialButton>(R.id.btnMePrestaron)?.setOnClickListener {
            val i = Intent(requireContext(), com.example.aureum1.controller.activities.WalletSelectRecordActivity::class.java)
            i.putExtra(com.example.aureum1.controller.activities.WalletSelectRecordActivity.EXTRA_DEFAULT_ACCION, "me_prestaron")
            startActivityForResult(i, REQ_SELECCION_REGISTRO)
        }

        // FAB: solo despliega/oculta los dos botones secundarios
        val layoutBotones = view.findViewById<LinearLayout>(R.id.layoutBotonesSecundarios)
        view.findViewById<FloatingActionButton>(R.id.fabAgregarDeuda)?.setOnClickListener {
            layoutBotones?.let {
                it.visibility = if (it.visibility == View.VISIBLE) View.GONE else View.VISIBLE
            }
        }

        // Suscripciones a cuentas (para moneda) y deudas
        val uid = auth.currentUser?.uid ?: return
        accountRepo.subscribeAccountsInfoByName(uid) { info ->
            accountsInfoByName = info
            adapterPresto.updateAccountsInfo(info)
            adapterMePrestaron.updateAccountsInfo(info)
        }

        listenerPresto?.remove()
        listenerMePrestaron?.remove()
        listenerPresto = repo.subscribeByAction(uid, "presto") { lista ->
            adapterPresto.submitList(lista)
            val vis = if (lista.isEmpty()) View.VISIBLE else View.GONE
            tvEmptyPresto?.visibility = vis
            cardEmptyPresto?.visibility = vis
        }
        listenerMePrestaron = repo.subscribeByAction(uid, "me_prestaron") { lista ->
            adapterMePrestaron.submitList(lista)
            val vis = if (lista.isEmpty()) View.VISIBLE else View.GONE
            tvEmptyMePrestaron?.visibility = vis
            cardEmptyMePrestaron?.visibility = vis
        }
    }

    private fun mostrarModalOpciones(defaultAccion: String, defaultNombre: String?, debtId: String) {
        val sheet = BottomSheetDialog(requireContext())
        val v = layoutInflater.inflate(R.layout.bottomsheet_debt_add_options, null)
        sheet.setContentView(v)
        pendingDebtId = debtId
        pendingAccion = defaultAccion
        pendingNombre = defaultNombre
        v.findViewById<View>(R.id.opSeleccionarRegistro).setOnClickListener {
            sheet.dismiss()
            val i = Intent(requireContext(), com.example.aureum1.controller.activities.SeleccionRegistroDeudaActivity::class.java)
            i.putExtra("DEFAULT_ACCION", defaultAccion)
            i.putExtra("FILTER_SOURCE", "debt_card")
            startActivityForResult(i, REQ_SELECCION_REGISTRO)
        }
        v.findViewById<View>(R.id.opCrearNuevoRegistro).setOnClickListener {
            sheet.dismiss()
            val i = Intent(requireContext(), com.example.aureum1.controller.activities.CrearDeudaActivity::class.java)
            i.putExtra("DEFAULT_ACCION", defaultAccion)
            i.putExtra("DEBT_ID", debtId)
            if (!defaultNombre.isNullOrBlank()) {
                i.putExtra("DEFAULT_NOMBRE", defaultNombre)
            }
            startActivity(i)
        }
        v.findViewById<View>(R.id.btnCancelarModal).setOnClickListener { sheet.dismiss() }
        sheet.show()
    }

    private fun createFormView(accion: String): View {
        val ctx = requireContext()
        val v = LayoutInflater.from(ctx).inflate(R.layout.activity_edit_user, null) // Reutilizamos estilo de formulario simple
        // Tomamos solo el contenedor principal y añadimos nuestros campos
        val container = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
            setBackgroundColor(resources.getColor(R.color.aureum_cream, null))
        }

        val tvAccion = TextView(ctx).apply {
            text = if (accion == "presto") "Acción: Prestó" else "Acción: Me prestaron"
            setTextColor(resources.getColor(R.color.aureum_text, null))
            textSize = 16f
        }
        container.addView(tvAccion)

        val etNombre = EditText(ctx).apply { hint = "Nombre" }
        val etDescripcion = EditText(ctx).apply { hint = "Descripción" }
        container.addView(etNombre)
        container.addView(etDescripcion)

        val btnCuenta = MaterialButton(ctx).apply {
            text = "Cuenta"
            setOnClickListener {
                startActivityForResult(Intent(ctx, SeleccionCuentaActivity::class.java), REQ_SELECCION_CUENTA)
            }
        }
        container.addView(btnCuenta)

        val btnMonto = MaterialButton(ctx).apply {
            text = "Cantidad"
            setOnClickListener { mostrarCalculadora { monto -> tmpMonto = monto } }
        }
        container.addView(btnMonto)

        val btnFecha = MaterialButton(ctx).apply {
            text = "Fecha"
            setOnClickListener { mostrarDatePicker(false) }
        }
        val btnFechaVenc = MaterialButton(ctx).apply {
            text = "Fecha de vencimiento"
            setOnClickListener { mostrarDatePicker(true) }
        }
        container.addView(btnFecha)
        container.addView(btnFechaVenc)

        val btnGuardar = MaterialButton(ctx).apply {
            text = "Guardar"
            setOnClickListener {
                val nombre = etNombre.text.toString().trim()
                val desc = etDescripcion.text.toString().trim()
                val cuenta = tmpCuentaSeleccionada ?: ""
                val uid = auth.currentUser?.uid ?: return@setOnClickListener
                if (nombre.isBlank() || cuenta.isBlank() || tmpMonto <= 0.0) {
                    return@setOnClickListener
                }
                val fechaStr = "%04d-%02d-%02d".format(
                    tmpFecha.get(java.util.Calendar.YEAR),
                    tmpFecha.get(java.util.Calendar.MONTH) + 1,
                    tmpFecha.get(java.util.Calendar.DAY_OF_MONTH)
                )
                val vencStr = tmpFechaVenc?.let { f ->
                    "%04d-%02d-%02d".format(
                        f.get(java.util.Calendar.YEAR),
                        f.get(java.util.Calendar.MONTH) + 1,
                        f.get(java.util.Calendar.DAY_OF_MONTH)
                    )
                }
                val data = mapOf(
                    "accion" to accion,
                    "nombre" to nombre,
                    "descripcion" to desc,
                    "cuenta" to cuenta,
                    "moneda" to (accountsInfoByName[cuenta]?.get("moneda") ?: "PEN"),
                    "monto" to tmpMonto,
                    "fecha" to fechaStr,
                    "fechaVencimiento" to vencStr, // Puede ser null
                    "estado" to "activo",
                    "createdAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
            }
        }
        container.addView(btnGuardar)

        return container
    }

    private fun mostrarDatePicker(venc: Boolean) {
        val cal = if (venc) (tmpFechaVenc ?: java.util.Calendar.getInstance()) else tmpFecha
        DatePickerDialog(requireContext(), { _, y, m, d ->
            if (venc) {
                tmpFechaVenc = java.util.Calendar.getInstance().apply { set(y, m, d) }
            } else {
                tmpFecha = java.util.Calendar.getInstance().apply { set(y, m, d) }
            }
        }, cal.get(java.util.Calendar.YEAR), cal.get(java.util.Calendar.MONTH), cal.get(java.util.Calendar.DAY_OF_MONTH)).show()
    }

    private fun mostrarCalculadora(onMonto: (Double) -> Unit) {
        val dialog = BottomSheetDialog(requireContext())
        val v = layoutInflater.inflate(R.layout.bottomsheet_calculator, null)
        dialog.setContentView(v)

        val tv = v.findViewById<TextView>(R.id.tvDisplay)
        fun append(s: String) { tv.text = (tv.text.toString() + s).trimStart('0') }
        v.findViewById<View>(R.id.btn0).setOnClickListener { append("0") }
        v.findViewById<View>(R.id.btn00).setOnClickListener { append("00") }
        v.findViewById<View>(R.id.btn1).setOnClickListener { append("1") }
        v.findViewById<View>(R.id.btn2).setOnClickListener { append("2") }
        v.findViewById<View>(R.id.btn3).setOnClickListener { append("3") }
        v.findViewById<View>(R.id.btn4).setOnClickListener { append("4") }
        v.findViewById<View>(R.id.btn5).setOnClickListener { append("5") }
        v.findViewById<View>(R.id.btn6).setOnClickListener { append("6") }
        v.findViewById<View>(R.id.btn7).setOnClickListener { append("7") }
        v.findViewById<View>(R.id.btn8).setOnClickListener { append("8") }
        v.findViewById<View>(R.id.btn9).setOnClickListener { append("9") }
        v.findViewById<View>(R.id.btnDot).setOnClickListener { append(".") }
        v.findViewById<View>(R.id.btnBack).setOnClickListener {
            val s = tv.text.toString()
            tv.text = if (s.isNotEmpty()) s.dropLast(1) else "0"
        }
        v.findViewById<View>(R.id.btnClear).setOnClickListener { tv.text = "0" }
        v.findViewById<View>(R.id.btnOk).setOnClickListener {
            val monto = tv.text.toString().toDoubleOrNull() ?: 0.0
            dialog.dismiss()
            onMonto(monto)
        }
        v.findViewById<View>(R.id.btnCloseCalc).setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_SELECCION_CUENTA && data != null) {
            val nombreCuenta = data.getStringExtra("CUENTA_SELECCIONADA")
            if (!nombreCuenta.isNullOrBlank()) tmpCuentaSeleccionada = nombreCuenta
        } else if (requestCode == REQ_SELECCION_REGISTRO && data != null) {
            val uid = auth.currentUser?.uid ?: return
            val recordId = data.getStringExtra("SELECTED_RECORD_ID").orEmpty()
            val tipo = data.getStringExtra("SELECTED_RECORD_TIPO").orEmpty()
            val cuenta = data.getStringExtra("SELECTED_RECORD_CUENTA").orEmpty()
            val monto = data.getDoubleExtra("SELECTED_RECORD_MONTO", 0.0)
            val monedaSel = data.getStringExtra("SELECTED_RECORD_MONEDA").orEmpty()
            val mon = if (monedaSel.isNotBlank()) monedaSel else (accountsInfoByName[cuenta]?.get("moneda") ?: "PEN") as String
            val accion = pendingAccion ?: return
            val debtId = pendingDebtId ?: return
            val nombre = pendingNombre ?: ""
            val esIngreso = tipo.equals("Ingreso", true)
            val esAumento = (accion == "presto" && !esIngreso) || (accion == "me_prestaron" && esIngreso)
            val operacion = if (esAumento) "aumento" else "reembolso"
            val signo = if (esAumento) "-" else "+"
            val tipoMov = if (esAumento) "Préstamos, interés" else "Préstamos, alquileres"
            val direccion = when {
                accion == "presto" && !esAumento -> "Yo → $nombre"
                accion == "presto" && esAumento -> "$nombre → Yo"
                accion == "me_prestaron" && !esAumento -> "Yo → $nombre"
                else -> "$nombre → Yo"
            }
            val db = FirebaseFirestore.getInstance()
            db.collection("accounts").document(uid)
                .collection("deudas").document(accion)
                .collection("items").document(debtId)
                .get()
                .addOnSuccessListener { debtDoc ->
                    val monedaCard = (debtDoc.getString("moneda") ?: "PEN")
                    val montoConv = convertirMoneda(monto, mon, monedaCard)
                    val delta = when (accion) {
                        "presto" -> if (esAumento) montoConv else -montoConv
                        else -> if (esAumento) montoConv else -montoConv
                    }
                    val movement = mapOf(
                "recordId" to recordId,
                "movimiento_operacion" to operacion,
                "movimiento_tipo" to tipoMov,
                "movimiento_signo" to signo,
                "movimiento_direccion" to direccion,
                "cuenta" to cuenta,
                "moneda" to mon,
                "monto" to monto,
                "fecha" to com.google.firebase.Timestamp.now()
                    )
                    repo.addMovementToDebt(uid, accion, debtId, movement, delta) { success, error ->
                        if (!success) {
                            android.widget.Toast.makeText(requireContext(), error ?: "Error al aplicar registro", android.widget.Toast.LENGTH_SHORT).show()
                        } else {
                            repo.setRecordLinked(uid, recordId, true, accion, debtId)
                        }
                    }
                }
        }
    }

    override fun onDestroyView() {
        listenerPresto?.remove(); listenerPresto = null
        listenerMePrestaron?.remove(); listenerMePrestaron = null
        super.onDestroyView()
    }

    private fun convertirMoneda(monto: Double, origen: String, destino: String): Double {
        if (origen.equals(destino, true)) return monto
        val usdToPen = 3.8
        val eurToPen = 4.1
        val montoEnPen = when (origen.uppercase()) {
            "USD" -> monto * usdToPen
            "EUR" -> monto * eurToPen
            else -> monto
        }
        return when (destino.uppercase()) {
            "USD" -> montoEnPen / usdToPen
            "EUR" -> montoEnPen / eurToPen
            else -> montoEnPen
        }
    }
}
