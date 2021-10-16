package com.manuel.administradorred.models

import com.google.firebase.firestore.Exclude

data class PackageService(
    @get:Exclude var id: String? = null,
    var name: String? = null,
    var description: String? = null,
    var available: Int = 0,
    var price: Int = 0,
    var speed: Int = 0,
    var limit: Int = 0,
    var validity: Int = 0,
    var imagePath: String? = null,
    var administratorId: String = "",
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as PackageService
        if (id != other.id) return false
        return true
    }

    override fun hashCode(): Int {
        return id?.hashCode() ?: 0
    }
}