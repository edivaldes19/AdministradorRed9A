package com.manuel.administradorred.chat

import com.manuel.administradorred.models.Message

interface OnChatListener {
    fun deleteMessage(message: Message)
}