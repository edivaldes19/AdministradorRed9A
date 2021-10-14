package com.manuel.administradorred.requested_contract

import com.manuel.administradorred.entities.Contract

interface OnRequestedContractListener {
    fun onStartChat(contract: Contract)
    fun onStatusChange(contract: Contract)
}