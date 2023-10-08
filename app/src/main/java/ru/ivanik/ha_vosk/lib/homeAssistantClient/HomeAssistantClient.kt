package ru.ivanik.ha_vosk.lib.homeAssistantClient

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import ru.ivanik.ha_vosk.lib.homeAssistantClient.conversationProcess.Request as conversationProcessReq
import ru.ivanik.ha_vosk.lib.homeAssistantClient.conversationProcess.Response as conversationProcessRes

class HomeAssistantClient(val baseUrl: String, val token: String) {
    val client = HttpClient() {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
            })

        }
    }

    suspend fun conversationProcess(req: conversationProcessReq): conversationProcessRes {
        val res: conversationProcessRes = client.post("$baseUrl/api/conversation/process") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(req)
        }.body()

        return res;
    }
}