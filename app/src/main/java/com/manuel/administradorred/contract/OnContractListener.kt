package com.manuel.administradorred.contract

import com.manuel.administradorred.entities.Contract

interface OnContractListener {
    fun onStartChat(contract: Contract)
    fun onStatusChange(contract: Contract)
}