package com.manuel.administradorred.models

import com.google.firebase.firestore.Exclude

data class PackageServiceContract(
    @get:Exclude var id: String = "",
    var name: String = "",
    var amount: Int = 0
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as PackageServiceContract
        if (id != other.id) return false
        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}