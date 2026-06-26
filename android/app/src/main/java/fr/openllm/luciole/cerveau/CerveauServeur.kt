package fr.openllm.luciole.cerveau

import fr.openllm.luciole.model.Action
import fr.openllm.luciole.model.ActionJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class CerveauServeur(
    private val baseUrl: String,
    private val client: OkHttpClient = OkHttpClient()
) : Cerveau {

    private val jsonMedia = "application/json".toMediaType()

    companion object {
        private val json = Json { ignoreUnknownKeys = true }
    }

    override suspend fun suggest(phrase: String): Action = withContext(Dispatchers.IO) {
        val body = buildJsonObject {
            put("temperature", 0)
            put("stream", false)
            putJsonArray("messages") {
                addJsonObject { put("role", "system"); put("content", SystemPrompt.FR) }
                addJsonObject { put("role", "user"); put("content", phrase) }
            }
        }.toString()
        val req = Request.Builder()
            .url("$baseUrl/v1/chat/completions")
            .post(body.toRequestBody(jsonMedia))
            .build()
        try {
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) throw java.io.IOException("HTTP ${resp.code}")
                val txt = resp.body?.string().orEmpty()
                val content = json.parseToJsonElement(txt)
                    .jsonObject["choices"]?.jsonArray?.get(0)
                    ?.jsonObject?.get("message")?.jsonObject?.get("content")
                    ?.jsonPrimitive?.content.orEmpty()
                ActionJson.parse(content)
            }
        } catch (e: Exception) {
            throw CerveauIndisponible(e)
        }
    }
}
