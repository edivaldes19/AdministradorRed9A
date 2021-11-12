package com.manuel.administradorred.requested_contract

import com.manuel.administradorred.models.RequestedContract

interface OnRequestedContractSelected {
    fun getContractSelected(): RequestedContract
}