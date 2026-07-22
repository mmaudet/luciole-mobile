package fr.openllm.luciole.cerveau

import fr.openllm.luciole.contact.ContactCard
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CerveauServeurContactTest {
    private val server = MockWebServer()

    @AfterTest fun stop() = server.shutdown()

    @Test fun extractContactParseJson() = runTest {
        server.enqueue(MockResponse().setBody(
            """{"choices":[{"message":{"content":"{\"full_name\":\"Jean Dupont\",\"phones\":[\"0612345678\"]}"}}]}"""))
        val c = CerveauServeur(server.url("/").toString().trimEnd('/'), OkHttpClient())
        val card = c.extractContact("Jean Dupont\n0612345678")
        assertEquals("Jean Dupont", card?.fullName)
        assertEquals(listOf("0612345678"), card?.phones)
    }

    @Test fun extractContactErreurReseauRetourneNull() = runTest {
        val c = CerveauServeur("http://127.0.0.1:1", OkHttpClient())
        assertNull(c.extractContact("texte"))
    }
}
