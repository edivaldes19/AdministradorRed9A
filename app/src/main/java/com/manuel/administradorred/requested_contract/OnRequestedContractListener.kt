package com.manuel.administradorred.requested_contract

import com.manuel.administradorred.models.RequestedContract

interface OnRequestedContractListener {
    fun onStartChat(requestedContract: RequestedContract)
    fun onStatusChange(requestedContract: RequestedContract)
}