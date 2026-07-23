package fr.openllm.luciole.scan

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
class OpenCvScanEngineTest {

    @Test fun redresseDocumentRectangulaire() {
        if (!OpenCvScanEngine.ensureOpenCv()) return
        val bmp = cardBitmap(margin = 40)
        val engine = OpenCvScanEngine()
        val corners = ScanCorners(
            topLeft = PointF(40f, 40f),
            topRight = PointF(559f, 40f),
            bottomRight = PointF(559f, 339f),
            bottomLeft = PointF(40f, 339f),
        )
        val result = engine.scan(bmp, corners)
        assertNotNull(result.bitmap)
        assertTrue(result.bitmap.width > 0)
    }

    @Test fun autoDetectCarteBlancheSurFondNoir() {
        if (!OpenCvScanEngine.ensureOpenCv()) return
        val result = OpenCvScanEngine().scan(cardBitmap(margin = 40))
        assertNotNull(result.bitmap)
        assertTrue(result.bitmap.width > 100)
        assertTrue(result.confidence >= 0.4f)
    }

    @Test fun fallbackCadreCompletSiPasDeContour() {
        if (!OpenCvScanEngine.ensureOpenCv()) return
        // Image uniforme : aucun contour, le fallback cadre complet doit quand même produire une sortie.
        val bmp = Bitmap.createBitmap(200, 120, Bitmap.Config.ARGB_8888)
        Canvas(bmp).drawColor(Color.GRAY)
        val result = OpenCvScanEngine().scan(bmp)
        assertNotNull(result.bitmap)
        assertTrue(result.confidence <= 0.5f)
    }

    private fun cardBitmap(margin: Int): Bitmap {
        val bmp = Bitmap.createBitmap(600, 380, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(Color.BLACK)
        canvas.drawRect(
            margin.toFloat(), margin.toFloat(),
            (600 - margin).toFloat(), (380 - margin).toFloat(),
            Paint().apply { color = Color.WHITE },
        )
        val paint = Paint().apply { color = Color.BLACK; textSize = 28f }
        canvas.drawText("Jean Dupont", (margin + 20).toFloat(), (margin + 50).toFloat(), paint)
        canvas.drawText("0612345678", (margin + 20).toFloat(), (margin + 100).toFloat(), paint)
        return bmp
    }
}
