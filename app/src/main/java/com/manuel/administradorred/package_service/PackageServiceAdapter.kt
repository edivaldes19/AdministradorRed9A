package com.manuel.administradorred.package_service

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.manuel.administradorred.R
import com.manuel.administradorred.databinding.ItemPackageBinding
import com.manuel.administradorred.entities.PackageService

class PackageServiceAdapter(
    private val productList: MutableList<PackageService>,
    private val listener: OnPackageServiceListener
) : RecyclerView.Adapter<PackageServiceAdapter.ViewHolder>() {
    private lateinit var context: Context
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        context = parent.context
        val view = LayoutInflater.from(context).inflate(R.layout.item_package_service, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val product = productList[position]
        holder.setListener(product)
        holder.binding.tvName.text = product.name
        holder.binding.tvPrice.text = product.price.toString()
        holder.binding.tvSpeed.text = product.speed.toString()
        Glide.with(context).load(product.imagePath).diskCacheStrategy(DiskCacheStrategy.ALL)
            .placeholder(R.drawable.ic_cloud_download).error(R.drawable.ic_error_outline)
            .centerCrop().into(holder.binding.imgPackage)
    }

    override fun getItemCount(): Int = productList.size
    fun add(product: PackageService) {
        if (!productList.contains(product)) {
            productList.add(product)
            notifyItemInserted(productList.size - 1)
        } else {
            update(product)
        }
    }

    fun update(product: PackageService) {
        val index = productList.indexOf(product)
        if (index != -1) {
            productList[index] = product
            notifyItemChanged(index)
        }
    }

    fun delete(product: PackageService) {
        val index = productList.indexOf(product)
        if (index != -1) {
            productList.removeAt(index)
            notifyItemRemoved(index)
        }
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val binding = ItemPackageBinding.bind(view)
        fun setListener(product: PackageService) {
            binding.root.setOnClickListener {
                listener.onClick(product)
            }
            binding.root.setOnLongClickListener {
                listener.onLongClick(product)
                true
            }
        }
    }
}