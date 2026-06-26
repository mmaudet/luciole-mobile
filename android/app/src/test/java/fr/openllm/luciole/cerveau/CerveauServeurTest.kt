package fr.openllm.luciole.cerveau

import fr.openllm.luciole.model.Action
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CerveauServeurTest {
    private val server = MockWebServer()

    @AfterTest fun stop() = server.shutdown()

    @Test fun renvoieLActionParsee() = runTest {
        server.enqueue(MockResponse().setBody(
            """{"choices":[{"message":{"content":"{\"type\":\"appel\",\"destinataire\":\"0612345678\"}"}}]}"""))
        val c = CerveauServeur(server.url("/").toString().trimEnd('/'), OkHttpClient())
        assertEquals(Action.Appel("0612345678"), c.suggest("appelle le 06 12 34 56 78"))
    }

    @Test fun erreurReseauLeveCerveauIndisponible() = runTest {
        val c = CerveauServeur("http://127.0.0.1:1", OkHttpClient())
        assertFailsWith<CerveauIndisponible> { c.suggest("bonjour") }
    }
}
