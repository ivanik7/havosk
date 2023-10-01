package ru.ivanik.ha_vosk.homeAssistantClient.conversationProcess

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Request (
    @SerialName("text")
    val text: String,
    @SerialName("language")
    val language: String,
){
}
