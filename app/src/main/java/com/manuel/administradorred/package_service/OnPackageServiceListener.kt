package com.manuel.administradorred.package_service

import com.manuel.administradorred.models.PackageService

interface OnPackageServiceListener {
    fun onClick(packageService: PackageService)
    fun onLongClick(packageService: PackageService)
}