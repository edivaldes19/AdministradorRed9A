package com.manuel.administradorred.package_service

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.manuel.administradorred.R
import com.manuel.administradorred.databinding.ItemPackageServiceBinding
import com.manuel.administradorred.models.PackageService

class PackageServiceAdapter(
    private var packageServiceList: MutableList<PackageService>,
    private val listener: OnPackageServiceListener
) : RecyclerView.Adapter<PackageServiceAdapter.ViewHolder>() {
    private lateinit var context: Context
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        context = parent.context
        val view =
            LayoutInflater.from(context).inflate(R.layout.item_package_service, parent, false)
        return ViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.binding.root.animation =
            AnimationUtils.loadAnimation(context, R.anim.fade_transition)
        val packageService = packageServiceList[position]
        holder.setListener(packageService)
        holder.binding.tvName.text = packageService.name
        holder.binding.tvPrice.text = "$${packageService.price} MXN"
        holder.binding.tvSpeed.text = "${packageService.speed} Mbps"
        Glide.with(context).load(packageService.imagePath).diskCacheStrategy(DiskCacheStrategy.ALL)
            .placeholder(R.drawable.ic_cloud_download).error(R.drawable.ic_broken_image)
            .into(holder.binding.imgPackage)
    }

    override fun getItemCount() = packageServiceList.size
    fun add(packageService: PackageService) {
        if (!packageServiceList.contains(packageService)) {
            packageServiceList.add(packageService)
            notifyItemInserted(packageServiceList.size - 1)
        } else {
            update(packageService)
        }
    }

    fun update(packageService: PackageService) {
        val index = packageServiceList.indexOf(packageService)
        if (index != -1) {
            packageServiceList[index] = packageService
            notifyItemChanged(index)
        }
    }

    fun delete(packageService: PackageService) {
        val index = packageServiceList.indexOf(packageService)
        if (index != -1) {
            packageServiceList.removeAt(index)
            notifyItemRemoved(index)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateList(list: MutableList<PackageService>) {
        packageServiceList = list
        notifyDataSetChanged()
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val binding = ItemPackageServiceBinding.bind(view)
        fun setListener(packageService: PackageService) {
            binding.root.setOnClickListener {
                listener.onClick(packageService)
            }
            binding.root.setOnLongClickListener {
                listener.onLongClick(packageService)
                true
            }
        }
    }
}