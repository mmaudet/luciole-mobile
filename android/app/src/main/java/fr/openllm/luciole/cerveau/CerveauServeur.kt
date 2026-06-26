package fr.openllm.luciole.cerveau

import fr.openllm.luciole.model.Action
import fr.openllm.luciole.model.ActionJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class CerveauServeur(
    private val baseUrl: String,
    private val client: OkHttpClient = OkHttpClient()
) : Cerveau {

    private val jsonMedia = "application/json".toMediaType()
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun suggest(phrase: String): Action = withContext(Dispatchers.IO) {
        val systemEscaped = SystemPrompt.FR
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
        val phraseEscaped = phrase
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
        val body = """{"temperature":0,"stream":false,"messages":[{"role":"system","content":"$systemEscaped"},{"role":"user","content":"$phraseEscaped"}]}"""
        val req = Request.Builder()
            .url("$baseUrl/v1/chat/completions")
            .post(body.toRequestBody(jsonMedia))
            .build()
        try {
            client.newCall(req).execute().use { resp ->
                val txt = resp.body?.string().orEmpty()
                val content = json.parseToJsonElement(txt)
                    .jsonObject["choices"]?.jsonArray?.get(0)
                    ?.jsonObject?.get("message")?.jsonObject?.get("content")
                    ?.jsonPrimitive?.content.orEmpty()
                ActionJson.parse(content)
            }
        } catch (e: CerveauIndisponible) {
            throw e
        } catch (e: Exception) {
            throw CerveauIndisponible(e)
        }
    }
}
