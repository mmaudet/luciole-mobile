package fr.openllm.luciole.scan

import android.graphics.PointF
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
class DimoCardScanTest {

    @Test fun scanApresCorrectionExif() {
        if (!OpenCvScanEngine.ensureOpenCv()) return
        val bytes = javaClass.classLoader!!
            .getResourceAsStream("cards/dimo_genoux.jpg")!!
            .readBytes()
        val oriented = ImageOrientation.fromJpegBytes(bytes)!!
        assertTrue(oriented.fromExif)
        val result = OpenCvScanEngine().scan(oriented.bitmap)
        assertNotNull(result.bitmap)
        assertTrue(result.bitmap.width > 100)
        assertTrue(result.bitmap.height > 100)
        println("SCAN dimo ${result.bitmap.width}x${result.bitmap.height} conf=${result.confidence}")
    }

    @Test fun scanAvecCoinsManuelsResteStable() {
        if (!OpenCvScanEngine.ensureOpenCv()) return
        val bytes = javaClass.classLoader!!
            .getResourceAsStream("cards/dimo_genoux_oriented.jpg")!!
            .readBytes()
        val oriented = ImageOrientation.fromJpegBytes(bytes)!!
        val w = oriented.bitmap.width.toFloat()
        val h = oriented.bitmap.height.toFloat()
        val margin = 0.05f
        val corners = ScanCorners(
            topLeft = PointF(w * margin, h * margin),
            topRight = PointF(w * (1 - margin), h * margin),
            bottomRight = PointF(w * (1 - margin), h * (1 - margin)),
            bottomLeft = PointF(w * margin, h * (1 - margin)),
        )
        val result = OpenCvScanEngine().scan(oriented.bitmap, corners)
        assertTrue(result.confidence >= 0.9f)
    }
}
