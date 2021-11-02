package com.manuel.administradorred.requested_contract

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ArrayAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.firebase.firestore.FirebaseFirestore
import com.manuel.administradorred.R
import com.manuel.administradorred.databinding.ItemRequestedContractBinding
import com.manuel.administradorred.models.RequestedContract
import com.manuel.administradorred.utils.Constants
import com.manuel.administradorred.utils.TimestampToText

class RequestedContractAdapter(
    private var requestedContractList: MutableList<RequestedContract>,
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
        holder.binding.root.animation = AnimationUtils.loadAnimation(context, R.anim.slide)
        val contract = requestedContractList[position]
        holder.setListener(contract)
        holder.binding.tvId.text = context.getString(R.string.contract_id, contract.id)
        var names = ""
        contract.packagesServices.forEach { entry ->
            names += "${entry.value.name}(${entry.value.available}), "
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
        val time = TimestampToText.getTimeAgo(contract.requested)
        holder.binding.tvDate.text = time
        val db = FirebaseFirestore.getInstance()
        db.collection(Constants.COLL_USERS).document(contract.userId).get()
            .addOnSuccessListener { snapshot ->
                holder.binding.tvUserName.text =
                    snapshot.getString(Constants.PROP_USERNAME).toString()
                Glide.with(context)
                    .load(snapshot.getString(Constants.PROP_PROFILE_PICTURE).toString())
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(R.drawable.ic_cloud_download).error(R.drawable.ic_error_outline)
                    .into(holder.binding.imgProfilePicture)
            }
    }

    override fun getItemCount(): Int = requestedContractList.size
    fun add(requestedContract: RequestedContract) {
        if (!requestedContractList.contains(requestedContract)) {
            requestedContractList.add(requestedContract)
            notifyItemInserted(requestedContractList.size - 1)
        } else {
            update(requestedContract)
        }
    }

    fun update(requestedContract: RequestedContract) {
        val index = requestedContractList.indexOf(requestedContract)
        if (index != -1) {
            requestedContractList[index] = requestedContract
            notifyItemChanged(index)
        }
    }

    fun delete(requestedContract: RequestedContract) {
        val index = requestedContractList.indexOf(requestedContract)
        if (index != -1) {
            requestedContractList.removeAt(index)
            notifyItemRemoved(index)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateList(list: MutableList<RequestedContract>) {
        requestedContractList = list
        notifyDataSetChanged()
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