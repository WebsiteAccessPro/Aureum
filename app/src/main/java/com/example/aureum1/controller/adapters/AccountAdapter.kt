package com.example.aureum1.controller.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.aureum1.R
import com.example.aureum1.model.Account
import java.util.Locale

class AccountAdapter(
    private var items: List<Account>,
    private val onAccountClick: (Account) -> Unit,
    private val onAddClick: () -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val TYPE_ACCOUNT = 1
    private val TYPE_ADD = 2

    override fun getItemViewType(position: Int) =
        if (items[position].isAddTile) TYPE_ADD else TYPE_ACCOUNT

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inf = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_ACCOUNT) {
            AccountVH(inf.inflate(R.layout.item_account, parent, false))
        } else {
            AddVH(inf.inflate(R.layout.item_account_add, parent, false))
        }
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = items[position]
        if (holder is AccountVH) holder.bind(item)
        if (holder is AddVH) holder.bind()
    }

    inner class AccountVH(view: View) : RecyclerView.ViewHolder(view) {
        private val tvNombre = view.findViewById<TextView>(R.id.tvAccountName)
        private val tvNumero = view.findViewById<TextView>(R.id.tvAccountMasked)
        private val tvValor  = view.findViewById<TextView>(R.id.tvAccountAmount)

        fun bind(acc: Account) {
            tvNombre.text = acc.nombre
            tvNumero.text = formatBlocks(acc.numero)
            tvValor.text  = String.Companion.format(Locale.getDefault(), "%s %,.2f", acc.moneda, acc.valor)
            val card = itemView as com.google.android.material.card.MaterialCardView
            val isSelected = selectedName?.equals(acc.nombre, true) == true
            card.strokeColor = itemView.resources.getColor(if (isSelected) R.color.aureum_gold else android.R.color.transparent, null)
            card.strokeWidth = if (isSelected) 3 else 1
            itemView.setOnClickListener {
                selectedName = acc.nombre
                notifyDataSetChanged()
                onAccountClick(acc)
            }
        }
    }

    inner class AddVH(view: View) : RecyclerView.ViewHolder(view) {
        fun bind() { itemView.setOnClickListener { onAddClick() } }
    }

    fun submitList(newItems: List<Account>) {
        items = newItems
        notifyDataSetChanged()
    }

    fun setSelected(name: String?) {
        selectedName = name
        notifyDataSetChanged()
    }

    private fun formatBlocks(num: String?): String {
        val clean = (num ?: "").replace("\\s+".toRegex(), "")
        return if (clean.isEmpty()) "" else clean.chunked(4).joinToString(" ")
    }
}
    private var selectedName: String? = null