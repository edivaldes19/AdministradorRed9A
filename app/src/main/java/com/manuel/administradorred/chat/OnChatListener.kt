package com.manuel.administradorred.chat

import com.manuel.administradorred.entities.Message

interface OnChatListener {
    fun deleteMessage(message: Message)
}