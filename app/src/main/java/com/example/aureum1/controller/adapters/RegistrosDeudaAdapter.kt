package com.example.aureum1.controller.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.aureum1.R
import com.example.aureum1.model.DateUtils
import com.google.firebase.Timestamp
import java.text.NumberFormat
import java.util.Locale

class RegistrosDeudaAdapter(
    private var items: List<Map<String, Any?>>,
    private var accountsInfo: Map<String, Map<String, Any?>> = emptyMap()
) : RecyclerView.Adapter<RegistrosDeudaAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val imgIcono: ImageView = view.findViewById(R.id.imgCategoria)
        val tvTitulo: TextView = view.findViewById(R.id.txtCategoriaTitulo)
        val tvCuenta: TextView = view.findViewById(R.id.txtCuenta)
        val tvNota: TextView = view.findViewById(R.id.txtNota)
        val tvMoneda: TextView = view.findViewById(R.id.txtMoneda)
        val tvMonto: TextView = view.findViewById(R.id.txtMonto)
        val tvFecha: TextView = view.findViewById(R.id.txtFecha)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_registro, parent, false)
        v.findViewById<View>(R.id.txtSaldo)?.visibility = View.GONE
        return VH(v)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        val tipo = (item["movimiento_tipo"] as? String).orEmpty()
        val cuenta = (item["cuenta"] as? String).orEmpty()
        val direccion = (item["movimiento_direccion"] as? String).orEmpty()
        val monto = (item["monto"] as? Number)?.toDouble() ?: 0.0
        val signo = (item["movimiento_signo"] as? String).orEmpty().ifBlank { "+" }
        val fechaTs = item["fecha"] as? Timestamp
        val monedaRegistro = (item["moneda"] as? String)

        holder.tvTitulo.text = tipo
        holder.tvCuenta.text = cuenta
        holder.tvNota.text = "\"$direccion\""

        val monedaFinal = monedaRegistro ?: (accountsInfo[cuenta]?.get("moneda") as? String) ?: "PEN"
        holder.tvMoneda.text = if (monedaFinal.equals("PEN", true)) "PEN" else monedaFinal

        val nf = NumberFormat.getNumberInstance(Locale.getDefault()).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
        holder.tvMonto.text = "$signo ${nf.format(monto)}"

        val colorRes = if (signo.startsWith("+")) R.color.aureum_green else R.color.aureum_red
        val color = ContextCompat.getColor(holder.itemView.context, colorRes)
        holder.tvMonto.setTextColor(color)

        val fechaLabel = DateUtils.relativeDate("", fechaTs)
        holder.tvFecha.text = fechaLabel

        holder.imgIcono.setImageResource(R.drawable.registro)
    }

    fun submitList(newItems: List<Map<String, Any?>>) {
        items = newItems
        notifyDataSetChanged()
    }

    fun updateAccountsInfo(info: Map<String, Map<String, Any?>>) {
        accountsInfo = info
        notifyDataSetChanged()
    }
}