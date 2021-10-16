package com.manuel.administradorred.requested_contract

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.recyclerview.widget.RecyclerView
import com.manuel.administradorred.R
import com.manuel.administradorred.databinding.ItemRequestedContractBinding
import com.manuel.administradorred.models.RequestedContract
import com.manuel.administradorred.utils.TimestampToText

class RequestedContractAdapter(
    private val requestedContractList: MutableList<RequestedContract>,
    private val listenerRequested: OnRequestedContractListener
) : RecyclerView.Adapter<RequestedContractAdapter.ViewHolder>() {
    private lateinit var context: Context
    private val aValues: Array<String> by lazy {
        context.resources.getStringArray(R.array.status_value)
    }
    private val aKeys: Array<Int> by lazy {
        context.resources.getIntArray(R.array.status_key).toTypedArray()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        context = parent.context
        val view =
            LayoutInflater.from(context).inflate(R.layout.item_requested_contract, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val contract = requestedContractList[position]
        holder.setListener(contract)
        holder.binding.tvId.text = context.getString(R.string.contract_id, contract.id)
        var names = ""
        contract.packagesServices.forEach {
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
        val time = TimestampToText.getTimeAgo(contract.timestamp)
        holder.binding.tvDate.text = time
    }

    override fun getItemCount(): Int = requestedContractList.size
    fun add(requestedContract: RequestedContract) {
        requestedContractList.add(requestedContract)
        notifyItemInserted(requestedContractList.size - 1)
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val binding = ItemRequestedContractBinding.bind(view)
        fun setListener(requestedContract: RequestedContract) {
            binding.actvStatus.setOnItemClickListener { _, _, position, _ ->
                requestedContract.status = aKeys[position]
                listenerRequested.onStatusChange(requestedContract)
            }
            binding.chpChat.setOnClickListener {
                listenerRequested.onStartChat(requestedContract)
            }
        }
    }
}