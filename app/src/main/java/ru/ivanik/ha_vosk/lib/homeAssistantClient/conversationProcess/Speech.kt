package ru.ivanik.ha_vosk.lib.homeAssistantClient.conversationProcess


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Speech(
    @SerialName("plain")
    val plain: Plain
)