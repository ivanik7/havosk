package ru.ivanik.ha_vosk.lib.homeAssistantClient.conversationProcess


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Response(
    @SerialName("conversation_id")
    val conversationId: String?,
    @SerialName("response")
    val response: ResponseX
)