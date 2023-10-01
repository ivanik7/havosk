package ru.ivanik.ha_vosk.homeAssistantClient.conversationProcess


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Plain(
    @SerialName("speech")
    val speech: String
)