package ru.ivanik.ha_vosk.lib.homeAssistantClient.conversationProcess


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Data(
    @SerialName("failed")
    val failed: List<Action>,
    @SerialName("success")
    val success: List<Action>,
    @SerialName("targets")
    val targets: List<Action>
)