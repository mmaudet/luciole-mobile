package fr.openllm.luciole.ocr

import android.graphics.BitmapFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import fr.openllm.luciole.scan.ImageOrientation
import fr.openllm.luciole.scan.OpenCvScanEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Test instrumenté générique : JPEG brut + EXIF → orientation → scan → OCR.
 * Couvre le chemin réel utilisateur (photo téléphone avec EXIF Orientation=6).
 */
@RunWith(AndroidJUnit4::class)
class DimoCardOcrInstrumentedTest {

    @Test
    fun scanEtOcrAvecCorrectionExif() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        OpenCvScanEngine.ensureOpenCv()
        val assets = InstrumentationRegistry.getInstrumentation().context.assets

        // JPEG brut (avec EXIF 6), pas la version déjà orientée.
        val jpeg = assets.open("cards/dimo_genoux.jpg").use { it.readBytes() }
        val oriented = ImageOrientation.fromJpegBytes(jpeg)!!
        assertTrue("EXIF doit corriger la photo dimo", oriented.fromExif)
        assertEquals(90, oriented.appliedRotationDegrees)

        val scanned = OpenCvScanEngine().scan(oriented.bitmap)
        val ocr = TesseractOcrEngine(context).recognize(scanned.bitmap)

        println("=== OCR RAW ===\n${ocr.rawText}")
        println("=== phones=${ocr.phones}")
        println("=== emails=${ocr.emails}")
        println("=== urls=${ocr.urls}")

        assertTrue("texte OCR trop court: ${ocr.rawText.length}", ocr.rawText.length > 20)
        assertTrue(
            "email attendu absent: ${ocr.emails}",
            ocr.emails.any { it.contains("dimosoftware", ignoreCase = true) }
                || ocr.rawText.contains("dimosoftware", ignoreCase = true),
        )
    }

    @Test
    fun fallbackCameraXSiExifNormal() {
        val assets = InstrumentationRegistry.getInstrumentation().context.assets
        // Version déjà orientée : EXIF normal → pas de rotation EXIF
        val jpeg = assets.open("cards/dimo_genoux_oriented.jpg").use { it.readBytes() }
        val withCam = ImageOrientation.fromJpegBytes(jpeg, cameraRotationDegrees = 0)!!
        assertEquals(0, withCam.appliedRotationDegrees)
        val bmp = BitmapFactory.decodeByteArray(jpeg, 0, jpeg.size)!!
        assertEquals(bmp.width, withCam.bitmap.width)
    }
}
