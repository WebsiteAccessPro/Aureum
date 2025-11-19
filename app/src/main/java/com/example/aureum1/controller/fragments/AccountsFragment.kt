package com.example.aureum1.controller.fragments

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.aureum1.R
import com.example.aureum1.controller.adapters.AccountAdapter
import com.example.aureum1.controller.activities.AjustesCuentaActivity
import com.example.aureum1.controller.activities.NuevaCuentaActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import com.example.aureum1.model.repository.AccountRepository
import com.example.aureum1.model.repository.DebtRepository
import com.example.aureum1.model.repository.RecordRepository
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.formatter.ValueFormatter
import androidx.core.content.ContextCompat

class AccountsFragment : Fragment(R.layout.fragment_accounts) {

    private lateinit var adapter: AccountAdapter
    private var listener: ListenerRegistration? = null

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val repo by lazy { AccountRepository() }
    private val debtRepo by lazy { DebtRepository() }
    private val recordRepo by lazy { RecordRepository() }

    private var selectedAccount: com.example.aureum1.model.Account? = null
    private var lActivasPresto: ListenerRegistration? = null
    private var lActivasMP: ListenerRegistration? = null
    private var lCerradasPresto: ListenerRegistration? = null
    private var lCerradasMP: ListenerRegistration? = null


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val rv = view.findViewById<RecyclerView>(R.id.rvAccounts)
        val btnSettings = view.findViewById<ImageButton?>(R.id.btnAccountsSettings)

        btnSettings?.setOnClickListener {
            startActivity(Intent(requireContext(), AjustesCuentaActivity::class.java))
        }

        rv.layoutManager = LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false)
        rv.setHasFixedSize(true)

        val btnDetalle = view.findViewById<android.widget.TextView>(R.id.btnDetalleCuenta)
        val chartDeudas = view.findViewById<PieChart>(R.id.chartDeudas)
        val chartAnual = view.findViewById<BarChart>(R.id.chartGastoAnual)
        val chartMes = view.findViewById<LineChart>(R.id.chartIngresosGastos)

        adapter = AccountAdapter(
            items = emptyList(),
            onAccountClick = { acc -> selectedAccount = acc },
            onAddClick = {
                startActivity(
                    Intent(
                        requireContext(),
                        NuevaCuentaActivity::class.java
                    )
                )
            }
        )
        rv.adapter = adapter

        btnDetalle?.setOnClickListener {
            val acc = selectedAccount
            if (acc == null) {
                android.widget.Toast.makeText(requireContext(), "Selecciona una cuenta", android.widget.Toast.LENGTH_SHORT).show()
            } else {
                mostrarDetalleCuenta(acc)
            }
        }

        suscribirCuentas()
        configurarCharts(chartDeudas, chartAnual, chartMes)
        suscribirEstadisticas(chartDeudas, chartAnual, chartMes)
    }

    private fun suscribirCuentas() {
        val uid = auth.currentUser?.uid ?: return
        listener?.remove()
        listener = repo.subscribeAccounts(uid) { listaConAdd ->
            adapter.submitList(listaConAdd)
        }
    }

    override fun onDestroyView() {
        listener?.remove()
        listener = null
        lActivasPresto?.remove(); lActivasPresto = null
        lActivasMP?.remove(); lActivasMP = null
        lCerradasPresto?.remove(); lCerradasPresto = null
        lCerradasMP?.remove(); lCerradasMP = null
        super.onDestroyView()
    }

    private fun mostrarDetalleCuenta(acc: com.example.aureum1.model.Account) {
        val dialog = com.google.android.material.bottomsheet.BottomSheetDialog(requireContext())
        val v = layoutInflater.inflate(R.layout.bottomsheet_account_detail, null)
        dialog.setContentView(v)
        val icon = v.findViewById<android.widget.ImageView>(R.id.imgIconoCuenta)
        val tvNombre = v.findViewById<android.widget.TextView>(R.id.tvNombreCuenta)
        val tvNumero = v.findViewById<android.widget.TextView>(R.id.tvNumeroCuenta)
        val tvTipo = v.findViewById<android.widget.TextView>(R.id.tvTipoCuenta)
        val tvMoneda = v.findViewById<android.widget.TextView>(R.id.tvMonedaCuenta)
        val tvSaldo = v.findViewById<android.widget.TextView>(R.id.tvSaldoCuenta)

        icon.setImageResource(R.drawable.ic_bank)
        tvNombre.text = acc.nombre
        tvNumero.text = acc.numero
        tvTipo.text = acc.tipo.ifBlank { "Cuenta" }
        tvMoneda.text = acc.moneda
        tvSaldo.text = String.format(java.util.Locale.getDefault(), "%s %,.2f", acc.moneda, acc.valor)

        v.findViewById<View>(R.id.btnCerrarDetalle)?.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun configurarCharts(chartDeudas: PieChart?, chartAnual: BarChart?, chartMes: LineChart?) {
        chartDeudas?.description?.isEnabled = false
        chartDeudas?.isDrawHoleEnabled = true
        chartDeudas?.holeRadius = 70f
        chartDeudas?.setTransparentCircleRadius(75f)
        chartDeudas?.setDrawEntryLabels(false)
        chartDeudas?.setNoDataText("Sin datos")
        chartDeudas?.setNoDataTextColor(android.graphics.Color.parseColor("#999999"))
        chartDeudas?.legend?.verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
        chartDeudas?.legend?.horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
        chartDeudas?.legend?.orientation = Legend.LegendOrientation.HORIZONTAL
        chartDeudas?.legend?.isEnabled = true
        chartDeudas?.legend?.textColor = android.graphics.Color.parseColor("#3A3A3A")
        chartDeudas?.legend?.xEntrySpace = 12f

        chartAnual?.description?.isEnabled = false
        chartAnual?.axisRight?.isEnabled = false
        chartAnual?.axisLeft?.axisMinimum = 0f
        chartAnual?.axisLeft?.textColor = android.graphics.Color.parseColor("#3A3A3A")
        chartAnual?.xAxis?.setDrawGridLines(false)
        chartAnual?.xAxis?.granularity = 1f
        chartAnual?.xAxis?.textColor = android.graphics.Color.parseColor("#3A3A3A")
        chartAnual?.legend?.isEnabled = false
        chartAnual?.setNoDataText("Sin datos")
        chartAnual?.setNoDataTextColor(android.graphics.Color.parseColor("#999999"))

        chartMes?.description?.isEnabled = false
        chartMes?.axisRight?.isEnabled = false
        chartMes?.axisLeft?.axisMinimum = 0f
        chartMes?.axisLeft?.textColor = android.graphics.Color.parseColor("#3A3A3A")
        chartMes?.xAxis?.granularity = 1f
        chartMes?.xAxis?.textColor = android.graphics.Color.parseColor("#3A3A3A")
        chartMes?.legend?.verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
        chartMes?.legend?.horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
        chartMes?.legend?.orientation = Legend.LegendOrientation.HORIZONTAL
        chartMes?.legend?.isEnabled = true
        chartMes?.legend?.textColor = android.graphics.Color.parseColor("#3A3A3A")
        chartMes?.setExtraBottomOffset(12f)
    }

    private fun suscribirEstadisticas(chartDeudas: PieChart?, chartAnual: BarChart?, chartMes: LineChart?) {
        val uid = auth.currentUser?.uid ?: return
        var activasPresto = 0
        var activasMP = 0
        var cerradasPresto = 0
        var cerradasMP = 0
        fun renderDeudasChart() {
            val act = activasPresto + activasMP
            val cer = cerradasPresto + cerradasMP
            val entries = mutableListOf<PieEntry>()
            if (act > 0) entries.add(PieEntry(act.toFloat(), "Activas"))
            if (cer > 0) entries.add(PieEntry(cer.toFloat(), "Cerradas"))
            val ds = PieDataSet(entries, "")
            ds.colors = listOf(android.graphics.Color.parseColor("#C9AA66"), android.graphics.Color.parseColor("#00BFA6"))
            ds.sliceSpace = 2f
            ds.valueTextColor = android.graphics.Color.TRANSPARENT
            chartDeudas?.data = PieData(ds)
            chartDeudas?.animateY(600)
            chartDeudas?.invalidate()
        }
        lActivasPresto = debtRepo.subscribeByAction(uid, "presto", estado = "activo") { lista -> activasPresto = lista.size; renderDeudasChart() }
        lActivasMP = debtRepo.subscribeByAction(uid, "me_prestaron", estado = "activo") { lista -> activasMP = lista.size; renderDeudasChart() }
        lCerradasPresto = debtRepo.subscribeByAction(uid, "presto", estado = "cerrado") { lista -> cerradasPresto = lista.size; renderDeudasChart() }
        lCerradasMP = debtRepo.subscribeByAction(uid, "me_prestaron", estado = "cerrado") { lista -> cerradasMP = lista.size; renderDeudasChart() }

        recordRepo.subscribeAllRaw(uid) { lista ->
            val now = java.util.Calendar.getInstance()
            val curYear = now.get(java.util.Calendar.YEAR)
            val curMonth = now.get(java.util.Calendar.MONTH)
            val mensual = DoubleArray(12) { 0.0 }
            val ingresosDia = DoubleArray(31) { 0.0 }
            val gastosDia = DoubleArray(31) { 0.0 }
            lista.forEach { m ->
                val tipo = (m["tipo"] as? String).orEmpty()
                val ts = m["createdAt"] as? com.google.firebase.Timestamp
                val fecha = ts?.toDate() ?: java.util.Date()
                val cal = java.util.Calendar.getInstance().apply { time = fecha }
                val year = cal.get(java.util.Calendar.YEAR)
                val month = cal.get(java.util.Calendar.MONTH)
                val day = cal.get(java.util.Calendar.DAY_OF_MONTH)
                val monto = (m["monto"] as? Number)?.toDouble() ?: 0.0
                if (tipo.equals("Gasto", true) && year == curYear) mensual[month] += monto
                if (month == curMonth && year == curYear) {
                    if (tipo.equals("Ingreso", true)) ingresosDia[day - 1] += monto
                    if (tipo.equals("Gasto", true)) gastosDia[day - 1] += monto
                }
            }
            val meses = arrayOf("Ene","Feb","Mar","Abr","May","Jun","Jul","Ago","Sep","Oct","Nov","Dic")
            val barEntries = mensual.mapIndexed { i, v -> BarEntry(i.toFloat(), v.toFloat()) }
            val barSet = BarDataSet(barEntries, "Gasto")
            barSet.color = android.graphics.Color.parseColor("#D86D6D")
            val barData = BarData(barSet)
            barData.barWidth = 0.6f
            barData.setValueTextColor(android.graphics.Color.TRANSPARENT)
            chartAnual?.xAxis?.valueFormatter = object: ValueFormatter(){ override fun getAxisLabel(value: Float, axis: com.github.mikephil.charting.components.AxisBase?): String { val i=value.toInt(); return if(i in 0..11) meses[i] else "" } }
            chartAnual?.data = barData
            chartAnual?.animateY(600)
            chartAnual?.invalidate()

            val ingresosEntries = ingresosDia.mapIndexed { i, v -> Entry(i.toFloat()+1f, v.toFloat()) }
            val gastosEntries = gastosDia.mapIndexed { i, v -> Entry(i.toFloat()+1f, v.toFloat()) }
            val lIng = LineDataSet(ingresosEntries, "Ingresos")
            lIng.color = android.graphics.Color.parseColor("#3FA77A")
            lIng.setDrawCircles(false)
            lIng.mode = LineDataSet.Mode.CUBIC_BEZIER
            lIng.setDrawFilled(true)
            lIng.fillColor = android.graphics.Color.parseColor("#CFEFE4")
            val lGas = LineDataSet(gastosEntries, "Gastos")
            lGas.color = android.graphics.Color.parseColor("#D86D6D")
            lGas.setDrawCircles(false)
            lGas.mode = LineDataSet.Mode.CUBIC_BEZIER
            lGas.setDrawFilled(true)
            lGas.fillColor = android.graphics.Color.parseColor("#F7D3D3")
            val lineData = LineData(lIng, lGas)
            lineData.setValueTextColor(android.graphics.Color.TRANSPARENT)
            chartMes?.data = lineData
            chartMes?.animateX(600)
            chartMes?.invalidate()
        }
    }
}