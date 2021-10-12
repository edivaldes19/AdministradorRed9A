package com.manuel.administradorred.contract

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.recyclerview.widget.RecyclerView
import com.manuel.administradorred.R
import com.manuel.administradorred.databinding.ItemContractBinding
import com.manuel.administradorred.entities.Contract

class ContractAdapter(
    private val contractList: MutableList<Contract>,
    private val listener: OnContractListener
) :
    RecyclerView.Adapter<ContractAdapter.ViewHolder>() {
    private lateinit var context: Context
    private val aValues: Array<String> by lazy {
        context.resources.getStringArray(R.array.status_value)
    }
    private val aKeys: Array<Int> by lazy {
        context.resources.getIntArray(R.array.status_key).toTypedArray()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        context = parent.context
        val view = LayoutInflater.from(context).inflate(R.layout.item_contract, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val contract = contractList[position]
        holder.setListener(contract)
        holder.binding.tvId.text = context.getString(R.string.contract_id, contract.id)
        var names = ""
        contract.packages.forEach {
            names += "${it.value.name}, "
        }
        holder.binding.tvProductNames.text = names.dropLast(2)
        holder.binding.tvTotalPrice.text =
            context.getString(R.string.total_price, contract.totalPrice)
        val index = aKeys.indexOf(contract.status)
        val statusAdapter =
            ArrayAdapter(context, android.R.layout.simple_dropdown_item_1line, aValues)
        holder.binding.actvStatus.setAdapter(statusAdapter)
        if (index != -1) {
            holder.binding.actvStatus.setText(aValues[index], false)
        } else {
            holder.binding.actvStatus.setText(context.getText(R.string.unknown), false)
        }
    }

    override fun getItemCount(): Int = contractList.size
    fun add(contract: Contract) {
        contractList.add(contract)
        notifyItemInserted(contractList.size - 1)
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val binding = ItemContractBinding.bind(view)
        fun setListener(contract: Contract) {
            binding.actvStatus.setOnItemClickListener { _, _, position, _ ->
                contract.status = aKeys[position]
                listener.onStatusChange(contract)
            }
            binding.chpChat.setOnClickListener {
                listener.onStartChat(contract)
            }
        }
    }
}