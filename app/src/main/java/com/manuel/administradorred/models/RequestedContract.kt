package com.manuel.administradorred.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.ServerTimestamp

data class RequestedContract(
    @get:Exclude var id: String = "",
    var userId: String = "",
    var packagesServices: Map<String, PackageServiceContract> = hashMapOf(),
    var totalPrice: Int = 0,
    var status: Int = 0,
    var timestamp: Long = 0,
    @ServerTimestamp var date: Timestamp? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as RequestedContract
        if (id != other.id) return false
        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}