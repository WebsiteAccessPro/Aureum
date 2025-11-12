package com.example.aureum1.controller.fragments

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.example.aureum1.R
import com.example.aureum1.controller.activities.AddRecordActivity
import com.example.aureum1.controller.activities.Registro.SeleccionCuentaActivity
import com.example.aureum1.controller.activities.SearchActivity
import com.example.aureum1.controller.adapters.RegistroSectionAdapter
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

class RecordFragment : Fragment(R.layout.fragment_record) {

    companion object {
        private const val REQ_SELECCION_CUENTA = 500
    }

    private lateinit var tvCuentaSeleccionada: TextView
    private lateinit var layoutCuentas: LinearLayout
    private lateinit var behavior: BottomSheetBehavior<View>
    private lateinit var recycler: RecyclerView
    private lateinit var adapterReg: RegistroSectionAdapter
    private lateinit var verMasAdapter: VerMasAdapter
    private lateinit var concatAdapter: ConcatAdapter
    private var registrosListener: ListenerRegistration? = null
    private var accountsListener: ListenerRegistration? = null
    private var accountsInfoByName: Map<String, Map<String, Any?>> = emptyMap()
    private lateinit var tvResumenPeriodo: TextView
    private lateinit var tvResumenMonto: TextView
    private lateinit var tvSemanaActual: TextView
    private lateinit var tvSaldoTotalAcumulado: TextView
    private lateinit var tvSaldoSemanaActual: TextView
    private var currentStart: java.util.Date? = null
    private var currentEnd: java.util.Date? = null

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 🔹 Referencias UI
        val bottomSheet = view.findViewById<View>(R.id.layoutFiltros)
        layoutCuentas = view.findViewById(R.id.layoutCuentas)
        tvCuentaSeleccionada = view.findViewById(R.id.tvCuentaSeleccionada)
        tvResumenPeriodo = view.findViewById(R.id.tvResumenPeriodo)
        tvResumenMonto = view.findViewById(R.id.tvResumenMonto)
        tvSemanaActual = view.findViewById(R.id.tvSemanaActual)
        tvSaldoTotalAcumulado = view.findViewById(R.id.tvSaldoTotalAcumulado)
        tvSaldoSemanaActual = view.findViewById(R.id.tvSaldoSemanaActual)

        // Mostrar semana actual al iniciar (dentro del ciclo de vida del fragment)
        tvSemanaActual.text = "SEMANA ${java.util.Calendar.getInstance().get(java.util.Calendar.WEEK_OF_YEAR)}"

        val viewPager = view.findViewById<ViewPager2>(R.id.viewPagerFiltros)
        val handle = view.findViewById<View>(R.id.handleFiltros)
        val dot1 = view.findViewById<ImageView>(R.id.dot1)
        val dot2 = view.findViewById<ImageView>(R.id.dot2)
        val dot3 = view.findViewById<ImageView>(R.id.dot3)

        recycler = view.findViewById(R.id.recyclerRegistros)
        recycler.layoutManager = LinearLayoutManager(requireContext())
        adapterReg = RegistroSectionAdapter(emptyList())
        verMasAdapter = VerMasAdapter(false) {
            val intent = Intent(requireContext(), com.example.aureum1.controller.activities.FullRecordListActivity::class.java)
            startActivity(intent)
            requireActivity().overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left)
        }
        concatAdapter = ConcatAdapter(adapterReg, verMasAdapter)
        recycler.adapter = concatAdapter
        // Desactivar nested scrolling para que el BottomSheet no reaccione a gestos fuera del área de filtros
        recycler.isNestedScrollingEnabled = false

        // ⚡ Botón flotante para agregar nuevo registro
        val fabNuevoRegistro = view.findViewById<FloatingActionButton>(R.id.fabNuevoRegistro)
        fabNuevoRegistro.setOnClickListener {
            val intent = Intent(requireContext(), AddRecordActivity::class.java)
            startActivity(intent)
        }

        // 🔍 Abrir búsqueda al tocar la lupa superior
        view.findViewById<ImageView>(R.id.btnBuscar)?.setOnClickListener {
            startActivity(Intent(requireContext(), SearchActivity::class.java))
        }

        // 🔸 Layouts del ViewPager
        val layouts = listOf(
            R.layout.item_filtro_rango,
            R.layout.item_filtro_selector,
            R.layout.item_filtro_rango_fecha
        )

        viewPager.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                val v = LayoutInflater.from(parent.context).inflate(layouts[viewType], parent, false)
                return object : RecyclerView.ViewHolder(v) {}
            }
            override fun getItemCount() = layouts.size
            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                when (position) {
                    0 -> { // RANGO PRESET: 7 días, 30 días, 12 semanas, 6 meses
                        val v = holder.itemView
                        v.findViewById<View>(R.id.btn7Dias)?.setOnClickListener {
                            val end = java.util.Date()
                            val cal = java.util.Calendar.getInstance().apply { time = end; add(java.util.Calendar.DAY_OF_YEAR, -7) }
                            val start = cal.time
                            applyFilter("Últimos 7 Días", start, end)
                        }
                        v.findViewById<View>(R.id.btn30Dias)?.setOnClickListener {
                            val end = java.util.Date()
                            val cal = java.util.Calendar.getInstance().apply { time = end; add(java.util.Calendar.DAY_OF_YEAR, -30) }
                            val start = cal.time
                            applyFilter("Últimos 30 Días", start, end)
                        }
                        v.findViewById<View>(R.id.btn12Semanas)?.setOnClickListener {
                            val end = java.util.Date()
                            val cal = java.util.Calendar.getInstance().apply { time = end; add(java.util.Calendar.DAY_OF_YEAR, -84) }
                            val start = cal.time
                            applyFilter("Últimas 12 Semanas", start, end)
                        }
                        v.findViewById<View>(R.id.btn6Meses)?.setOnClickListener {
                            val end = java.util.Date()
                            val cal = java.util.Calendar.getInstance().apply { time = end; add(java.util.Calendar.MONTH, -6) }
                            val start = cal.time
                            applyFilter("Últimos 6 Meses", start, end)
                        }
                    }
                    1 -> { // SELECTOR: Spinner de periodo y botón Calendario
                        val v = holder.itemView
                        // Nuevo: manejar Spinner (select) y botón Calendario
                        val spinner = v.findViewById<Spinner>(R.id.spinnerFiltroTiempo)
                        spinner?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                            override fun onItemSelected(parent: AdapterView<*>, viewSel: View?, position: Int, id: Long) {
                                when (position) {
                                    0 -> { // Hoy
                                        val now = java.util.Calendar.getInstance()
                                        now.set(java.util.Calendar.HOUR_OF_DAY, 0)
                                        now.set(java.util.Calendar.MINUTE, 0)
                                        now.set(java.util.Calendar.SECOND, 0)
                                        now.set(java.util.Calendar.MILLISECOND, 0)
                                        applyFilter("Hoy", now.time, java.util.Date())
                                    }
                                    1 -> { // Esta semana
                                        val now = java.util.Calendar.getInstance()
                                        now.firstDayOfWeek = java.util.Calendar.MONDAY
                                        now.set(java.util.Calendar.DAY_OF_WEEK, java.util.Calendar.MONDAY)
                                        now.set(java.util.Calendar.HOUR_OF_DAY, 0)
                                        now.set(java.util.Calendar.MINUTE, 0)
                                        now.set(java.util.Calendar.SECOND, 0)
                                        now.set(java.util.Calendar.MILLISECOND, 0)
                                        applyFilter("Esta semana", now.time, java.util.Date())
                                    }
                                    2 -> { // Este mes
                                        val start = java.util.Calendar.getInstance().apply {
                                            set(java.util.Calendar.DAY_OF_MONTH, 1)
                                            set(java.util.Calendar.HOUR_OF_DAY, 0)
                                            set(java.util.Calendar.MINUTE, 0)
                                            set(java.util.Calendar.SECOND, 0)
                                            set(java.util.Calendar.MILLISECOND, 0)
                                        }
                                        applyFilter("Este mes", start.time, java.util.Date())
                                    }
                                    3 -> { // Este año
                                        val start = java.util.Calendar.getInstance().apply {
                                            set(java.util.Calendar.MONTH, java.util.Calendar.JANUARY)
                                            set(java.util.Calendar.DAY_OF_MONTH, 1)
                                            set(java.util.Calendar.HOUR_OF_DAY, 0)
                                            set(java.util.Calendar.MINUTE, 0)
                                            set(java.util.Calendar.SECOND, 0)
                                            set(java.util.Calendar.MILLISECOND, 0)
                                        }
                                        applyFilter("Este año", start.time, java.util.Date())
                                    }
                                }
                            }
                            override fun onNothingSelected(parent: AdapterView<*>) {}
                        }
                        v.findViewById<View>(R.id.btnIrCalendario)?.setOnClickListener { viewPager.currentItem = 2 }
                    }
                    2 -> { // RANGO DE FECHAS PERSONALIZADO
                        val v = holder.itemView
                        val btnInicio = v.findViewById<android.widget.Button>(R.id.btnFechaInicio)
                        val btnFin = v.findViewById<android.widget.Button>(R.id.btnFechaFin)
                        // Inicializar textos con el filtro actual o últimos 30 días
                        val initStart = currentStart ?: run {
                            val cal = java.util.Calendar.getInstance()
                            cal.add(java.util.Calendar.DAY_OF_YEAR, -30)
                            cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
                            cal.set(java.util.Calendar.MINUTE, 0)
                            cal.set(java.util.Calendar.SECOND, 0)
                            cal.set(java.util.Calendar.MILLISECOND, 0)
                            cal.time
                        }
                        val initEnd = currentEnd ?: java.util.Date()
                        btnInicio.text = java.text.SimpleDateFormat("dd/MM/yyyy").format(initStart)
                        btnFin.text = java.text.SimpleDateFormat("dd/MM/yyyy").format(initEnd)
                        currentStart = initStart
                        currentEnd = initEnd
                        fun showDatePicker(isStart: Boolean) {
                            val cal = java.util.Calendar.getInstance()
                            val dialog = android.app.DatePickerDialog(requireContext(), { _, y, m, d ->
                                val picked = java.util.Calendar.getInstance().apply { set(y, m, d, 0, 0, 0); set(java.util.Calendar.MILLISECOND, 0) }
                                if (isStart) {
                                    btnInicio.text = "%02d/%02d/%04d".format(d, m + 1, y)
                                    currentStart = picked.time
                                } else {
                                    btnFin.text = "%02d/%02d/%04d".format(d, m + 1, y)
                                    // fin a final del día seleccionado
                                    picked.set(java.util.Calendar.HOUR_OF_DAY, 23)
                                    picked.set(java.util.Calendar.MINUTE, 59)
                                    picked.set(java.util.Calendar.SECOND, 59)
                                    picked.set(java.util.Calendar.MILLISECOND, 999)
                                    currentEnd = picked.time
                                }
                                val label = if (currentStart != null && currentEnd != null) "Rango personalizado" else tvResumenPeriodo.text.toString()
                                if (currentStart != null && currentEnd != null) applyFilter(label, currentStart, currentEnd)
                            }, cal.get(java.util.Calendar.YEAR), cal.get(java.util.Calendar.MONTH), cal.get(java.util.Calendar.DAY_OF_MONTH))
                            dialog.show()
                        }
                        btnInicio?.setOnClickListener { showDatePicker(true) }
                        btnFin?.setOnClickListener { showDatePicker(false) }
                    }
                }
            }
            override fun getItemViewType(position: Int) = position
        }

        // Indicadores del ViewPager
        val dots = listOf(dot1, dot2, dot3)
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                dots.forEachIndexed { i, d ->
                    d.setImageResource(if (i == position) R.drawable.dot_active else R.drawable.dot_inactive)
                }
            }
        })

        // Configurar el BottomSheet
        behavior = BottomSheetBehavior.from(bottomSheet).apply {
            // Altura mínima visible en estado colapsado (dp -> px)
            val peekPx = (100 * resources.displayMetrics.density).toInt()
            peekHeight = peekPx
            // No permitir que el BottomSheet se oculte debajo de la barra inferior
            isHideable = false
            isDraggable = false
            state = BottomSheetBehavior.STATE_COLLAPSED
        }

        // Asegurar que los últimos elementos del RecyclerView no queden tapados por el BottomSheet
        // Se suma el peekHeight al padding inferior actual
        recycler.post {
            val peekPx = behavior.peekHeight
            recycler.setPadding(
                recycler.paddingLeft,
                recycler.paddingTop,
                recycler.paddingRight,
                recycler.paddingBottom + peekPx
            )
        }

        // Solo permitir el arrastre cuando el toque se origina sobre el BottomSheet
        bottomSheet.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    // Toque dentro del panel de filtros: habilitar arrastre
                    behavior.isDraggable = true
                    false
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    // Finaliza el gesto: deshabilitar arrastre para evitar activación fuera del área
                    behavior.isDraggable = false
                    false
                }
                else -> false
            }
        }

        // Handle de la barra de filtros: tap para expandir/colapsar
        handle.setOnClickListener {
            behavior.state = if (behavior.state == BottomSheetBehavior.STATE_EXPANDED) {
                BottomSheetBehavior.STATE_COLLAPSED
            } else {
                BottomSheetBehavior.STATE_EXPANDED
            }
        }

        behavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                when (newState) {
                    BottomSheetBehavior.STATE_EXPANDED -> {
                        layoutCuentas.visibility = View.VISIBLE
                        layoutCuentas.post {
                            layoutCuentas.alpha = 1f
                            layoutCuentas.requestLayout()
                        }
                        // Ajustar el padding inferior de la lista al alto actual del panel expandido
                        recycler.post {
                            val expanded = bottomSheet.height
                            recycler.setPadding(
                                recycler.paddingLeft,
                                recycler.paddingTop,
                                recycler.paddingRight,
                                expanded
                            )
                        }
                    }
                    BottomSheetBehavior.STATE_COLLAPSED -> {
                        layoutCuentas.visibility = View.GONE
                        // Restaurar padding inferior al peekHeight cuando se colapsa
                        recycler.post {
                            val peekPx = behavior.peekHeight
                            recycler.setPadding(
                                recycler.paddingLeft,
                                recycler.paddingTop,
                                recycler.paddingRight,
                                peekPx
                            )
                        }
                    }
                    else -> {}
                }
            }
            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                layoutCuentas.alpha = slideOffset
            }
        })

        // Botón Reset de filtros a la derecha del campo de cuenta
        view.findViewById<View>(R.id.btnResetFiltros)?.setOnClickListener {
            tvCuentaSeleccionada.text = "Todas"
            val end = java.util.Date()
            val cal = java.util.Calendar.getInstance().apply { time = end; add(java.util.Calendar.DAY_OF_YEAR, -30) }
            val start = cal.time
            applyFilter("Últimos 30 Días", start, end)
        }

        // Abrir selector de cuentas al tocar la caja de texto
        tvCuentaSeleccionada.setOnClickListener {
            val intent = Intent(requireContext(), SeleccionCuentaActivity::class.java)
            startActivityForResult(intent, REQ_SELECCION_CUENTA)
            requireActivity().overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left)
        }
        // Cargar registros al iniciar
        cargarRegistros()
    }

    private fun applyFilter(label: String, start: java.util.Date?, end: java.util.Date?) {
        tvResumenPeriodo.text = label
        currentStart = start
        currentEnd = end
        val sel = tvCuentaSeleccionada.text?.toString()?.trim()
        val cuentaFilter = if (sel.isNullOrBlank() || sel.equals("Todas", true)) null else sel
        cargarRegistros(cuentaFilter, start, end)
    }

    // Recibir la cuenta seleccionada desde AjustesCuentaActivity
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQ_SELECCION_CUENTA && resultCode == Activity.RESULT_OK && data != null) {
            val cuentaSeleccionada = data.getStringExtra("CUENTA_SELECCIONADA")

            if (!cuentaSeleccionada.isNullOrEmpty()) {
                // 🔹 Actualizar el TextView con el nombre de la cuenta
                tvCuentaSeleccionada.text = cuentaSeleccionada

                // 🧭 Dejar el BottomSheet expandido al volver
                behavior.state = BottomSheetBehavior.STATE_EXPANDED

                // 🔁 Filtrar registros por la cuenta seleccionada
                cargarRegistros(cuentaSeleccionada)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        registrosListener?.remove()
        accountsListener?.remove()
    }

    private fun cargarRegistros(filtrarCuenta: String? = null, start: java.util.Date? = null, end: java.util.Date? = null) {
        val uid = auth.currentUser?.uid ?: return

        // Escuchar cuentas en tiempo real para actualizar saldo/moneda
        accountsListener?.remove()
        accountsListener = db.collection("accounts").document(uid)
            .addSnapshotListener { doc, _ ->
                if (doc == null || !doc.exists()) return@addSnapshotListener
                val cuentas = (doc.get("cuentas") as? List<Map<String, Any?>>) ?: emptyList()
                val infoPorNombre = cuentas.associateBy { (it["nombre"] as? String).orEmpty() }
                adapterReg.updateAccountsInfo(infoPorNombre)
                accountsInfoByName = infoPorNombre

                // Actualizar total acumulado en moneda local
                val preferred = obtenerMonedaPreferida()
                var totalAcumulado = 0.0
                for (c in cuentas) {
                    val saldo = when (val v = c["valorInicial"]) {
                        is Number -> v.toDouble()
                        is String -> v.toDoubleOrNull() ?: 0.0
                        else -> 0.0
                    }
                    val monedaCuenta = (c["moneda"] as? String).orEmpty().ifEmpty { "PEN" }
                    totalAcumulado += convertirMonedaSimple(saldo, monedaCuenta, preferred)
                }
                tvSaldoTotalAcumulado.text = "Saldo ${preferred} ${formatear(totalAcumulado)}"
            }

        registrosListener?.remove()
        var query = db.collection("accounts").document(uid)
            .collection("registros")
            .orderBy("createdAt", Query.Direction.DESCENDING)

        if (start != null) {
            query = query.whereGreaterThanOrEqualTo("createdAt", com.google.firebase.Timestamp(start))
        }
        if (end != null) {
            query = query.whereLessThanOrEqualTo("createdAt", com.google.firebase.Timestamp(end))
        }

        registrosListener = query.addSnapshotListener { snaps, err ->
            if (err != null || snaps == null) return@addSnapshotListener
            val listRaw = snaps.documents.map { it.data ?: emptyMap<String, Any?>() }

                val list = mutableListOf<Map<String, Any?>>()
                for (m in listRaw) {
                    val t = (m["tipo"] as? String).orEmpty()
                    if (t.equals("Transferencia", true)) {
                        val extDirection = (m["extDirection"] as? String).orEmpty()
                        if (extDirection.isNotEmpty()) {
                            // En transferencias externas ya registramos la dirección correspondiente (out para emisor, in para receptor)
                            val single = m.toMutableMap().apply { put("direction", extDirection) }
                            list.add(single)
                        } else {
                            // Para transferencias internas duplicamos en ambos sentidos
                            val inn = m.toMutableMap().apply { put("direction", "in") }
                            val out = m.toMutableMap().apply { put("direction", "out") }
                            list.add(inn)
                            list.add(out)
                        }
                    } else {
                        list.add(m)
                    }
                }

                val filtrados = if (filtrarCuenta.isNullOrBlank()) list else list.filter {
                    val origen = (it["cuentaOrigen"] as? String).orEmpty()
                    val destino = (it["cuentaDestino"] as? String).orEmpty()
                    origen.equals(filtrarCuenta, true) || destino.equals(filtrarCuenta, true)
                }
                // Calcular saldo histórico (después de cada movimiento) por cuenta
                val rollingPorCuenta = accountsInfoByName.mapValues {
                    val v = it.value["valorInicial"]
                    when (v) {
                        is Number -> v.toDouble()
                        is String -> v.toDoubleOrNull() ?: 0.0
                        else -> 0.0
                    }
                }.toMutableMap()

                fun convertirMoneda(monto: Double, monedaOrigen: String, monedaDestino: String): Double {
                    if (monedaOrigen.equals(monedaDestino, true)) return monto
                    val usdToPen = 3.8
                    val eurToPen = 4.1
                    val montoEnPen = when (monedaOrigen.uppercase()) {
                        "USD" -> monto * usdToPen
                        "EUR" -> monto * eurToPen
                        else -> monto
                    }
                    return when (monedaDestino.uppercase()) {
                        "USD" -> montoEnPen / usdToPen
                        "EUR" -> montoEnPen / eurToPen
                        else -> montoEnPen
                    }
                }

                val conSaldo = mutableListOf<Map<String, Any?>>()
                for (item in filtrados) {
                    val tipo = (item["tipo"] as? String).orEmpty().lowercase()
                    val direction = (item["direction"] as? String)
                    val origen = (item["cuentaOrigen"] as? String).orEmpty()
                    val destino = (item["cuentaDestino"] as? String).orEmpty()
                    val monto = (item["monto"] as? Number)?.toDouble() ?: 0.0

                    val afectada = if (tipo == "transferencia" && direction == "in") destino else origen
                    val saldoActual = rollingPorCuenta[afectada] ?: 0.0

                    val monedaOrigen = (accountsInfoByName[origen]?.get("moneda") as? String).orEmpty().ifEmpty { "PEN" }
                    val monedaDestino = (accountsInfoByName[destino]?.get("moneda") as? String).orEmpty().ifEmpty { "PEN" }

                    val efecto = when {
                        tipo == "ingreso" -> monto
                        tipo == "gasto" -> -monto
                        tipo == "transferencia" && direction == "out" -> -monto
                        tipo == "transferencia" && direction == "in" -> convertirMoneda(monto, monedaOrigen, monedaDestino)
                        else -> 0.0
                    }

                    val nuevo = item.toMutableMap().apply { put("saldoPosterior", saldoActual) }
                    conSaldo.add(nuevo)

                    // Retroceder el saldo para el siguiente ítem más antiguo
                    rollingPorCuenta[afectada] = saldoActual - efecto
                }

                // Actualizar resumen del periodo: neto ingresos - gastos (transferencias no cuentan)
                var neto = 0.0
                for (item in filtrados) {
                    val tipo = (item["tipo"] as? String).orEmpty().lowercase()
                    val monto = (item["monto"] as? Number)?.toDouble() ?: 0.0
                    when (tipo) {
                        "ingreso" -> neto += monto
                        "gasto" -> neto -= monto
                    }
                }
                val nf = java.text.NumberFormat.getNumberInstance(java.util.Locale.getDefault()).apply {
                    minimumFractionDigits = 2
                    maximumFractionDigits = 2
                }
                tvResumenMonto.text = "PEN ${nf.format(neto)}"

                // Cargar saldo de la semana actual (independiente del filtro)
                cargarResumenSemana(uid)

                // Limitar a 10 registros y añadir botón "Ver más" como ítem 11
                val limitados = if (conSaldo.size > 10) conSaldo.take(10) else conSaldo
                adapterReg.submitList(limitados)
                verMasAdapter.showButton = conSaldo.size > 10
                verMasAdapter.notifyDataSetChanged()

                // Actualizar totales independientes del filtro
                cargarTotalAcumulado(uid)
            }
    }

    // Adaptador para el botón inline "Ver más"
    private inner class VerMasAdapter(
        var showButton: Boolean,
        private val onClick: () -> Unit
    ) : RecyclerView.Adapter<VerMasAdapter.VH>() {
        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val btn: com.google.android.material.button.MaterialButton = v.findViewById(R.id.btnVerMasInline)
        }

        override fun getItemCount(): Int = if (showButton) 1 else 0

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_ver_mas, parent, false)
            return VH(v)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            holder.btn.setOnClickListener { onClick() }
        }
    }

    private fun cargarResumenSemana(uid: String) {
        val cal = java.util.Calendar.getInstance()
        cal.firstDayOfWeek = java.util.Calendar.MONDAY
        cal.set(java.util.Calendar.DAY_OF_WEEK, java.util.Calendar.MONDAY)
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        val start = cal.time
        cal.add(java.util.Calendar.DAY_OF_YEAR, 6)
        cal.set(java.util.Calendar.HOUR_OF_DAY, 23)
        cal.set(java.util.Calendar.MINUTE, 59)
        cal.set(java.util.Calendar.SECOND, 59)
        cal.set(java.util.Calendar.MILLISECOND, 999)
        val end = cal.time

        val preferred = obtenerMonedaPreferida()
        db.collection("accounts").document(uid)
            .collection("registros")
            .whereGreaterThanOrEqualTo("createdAt", com.google.firebase.Timestamp(start))
            .whereLessThanOrEqualTo("createdAt", com.google.firebase.Timestamp(end))
            .get()
            .addOnSuccessListener { snaps ->
                var netoSemana = 0.0
                for (doc in snaps.documents) {
                    val data = doc.data ?: continue
                    val tipo = (data["tipo"] as? String).orEmpty().lowercase()
                    val monto = (data["monto"] as? Number)?.toDouble() ?: 0.0
                    val monedaRegistro = (data["moneda"] as? String).orEmpty().ifEmpty { "PEN" }
                    val montoLocal = convertirMonedaSimple(monto, monedaRegistro, preferred)
                    when (tipo) {
                        "ingreso" -> netoSemana += montoLocal
                        "gasto" -> netoSemana -= montoLocal
                    }
                }
                tvSaldoSemanaActual.text = "${preferred} ${formatear(netoSemana)}"
                // Actualizar número de semana mostrado
                tvSemanaActual.text = "SEMANA ${java.util.Calendar.getInstance().get(java.util.Calendar.WEEK_OF_YEAR)}"
            }
    }

    private fun cargarTotalAcumulado(uid: String) {
        // Suma del saldo actual de todas las cuentas (valorInicial) convertido a la moneda preferida
        val preferred = obtenerMonedaPreferida()
        db.collection("accounts").document(uid)
            .get()
            .addOnSuccessListener { doc ->
                if (doc == null || !doc.exists()) return@addOnSuccessListener
                val cuentas = (doc.get("cuentas") as? List<Map<String, Any?>>) ?: emptyList()
                var totalAcumulado = 0.0
                for (c in cuentas) {
                    val saldo = when (val v = c["valorInicial"]) {
                        is Number -> v.toDouble()
                        is String -> v.toDoubleOrNull() ?: 0.0
                        else -> 0.0
                    }
                    val monedaCuenta = (c["moneda"] as? String).orEmpty().ifEmpty { "PEN" }
                    totalAcumulado += convertirMonedaSimple(saldo, monedaCuenta, preferred)
                }
                tvSaldoTotalAcumulado.text = "Saldo ${preferred} ${formatear(totalAcumulado)}"
            }
    }

    private fun obtenerMonedaPreferida(): String {
        return try {
            val ctx = activity ?: return java.util.Currency.getInstance(java.util.Locale.getDefault()).currencyCode ?: "PEN"
            val prefs = ctx.getSharedPreferences("aureum_prefs", android.content.Context.MODE_PRIVATE)
            prefs.getString("preferred_currency", null)
                ?: java.util.Currency.getInstance(java.util.Locale.getDefault()).currencyCode
                ?: "PEN"
        } catch (e: Exception) {
            "PEN"
        }
    }

    private fun convertirMonedaSimple(monto: Double, origen: String, destino: String): Double {
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

    private fun formatear(valor: Double): String {
        val nf = java.text.NumberFormat.getNumberInstance(java.util.Locale.getDefault()).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
        return nf.format(valor)
    }
}
