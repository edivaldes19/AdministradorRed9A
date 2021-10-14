package com.manuel.administradorred.requested_contract

import com.manuel.administradorred.models.Contract

interface OnRequestedContractListener {
    fun onStartChat(contract: Contract)
    fun onStatusChange(contract: Contract)
}