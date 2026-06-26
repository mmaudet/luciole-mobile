package fr.openllm.luciole

import androidx.test.core.app.ApplicationProvider
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class StringsParityTest {
    @Test fun memeChaineExisteDansLesDeuxLangues() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val id = R.string.envoyer
        RuntimeEnvironment.setQualifiers("fr"); val fr = ctx.getString(id)
        RuntimeEnvironment.setQualifiers("en"); val en = ctx.getString(id)
        assertEquals("Envoyer", fr); assertEquals("Send", en)
    }
}
