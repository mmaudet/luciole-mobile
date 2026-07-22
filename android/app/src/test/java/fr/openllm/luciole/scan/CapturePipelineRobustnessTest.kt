package fr.openllm.luciole.scan

import android.graphics.BitmapFactory
import fr.openllm.luciole.ocr.OcrPostProcessor
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Pipeline générique : EXIF → (scan simulé) → post-OCR,
 * calé sur les difficultés utilisateur observées (orientation, bruit OCR).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
class CapturePipelineRobustnessTest {

    @Test fun photoDimoExifPuisExtractionContact() {
        val rawJpeg = javaClass.classLoader!!
            .getResourceAsStream("cards/dimo_genoux.jpg")!!
            .readBytes()

        val oriented = ImageOrientation.fromJpegBytes(rawJpeg)!!
        assertTrue(oriented.fromExif)
        assertEquals(90, oriented.appliedRotationDegrees)

        // Texte OCR représentatif de ce que Tesseract produit sur cette carte (mesuré sur appareil).
        val ocrBruite = """
            dimo
            SOFTWARE &—
            Jean Pau! GENOUX
            Directeur Général/CEO
            jpgenoux@dimosoftware.com
            Mobile : +33 (0)6 74 64 05 44 / Tél. : +33 (0)4 72 86 O1 92
            Siège Social
            561, alée des Noisetiers - 69 760 Limonest - France
            www.dimosoftware.fr
        """.trimIndent()

        val fields = OcrPostProcessor.process(ocrBruite)
        assertEquals(listOf("jpgenoux@dimosoftware.com"), fields.emails)
        assertTrue(fields.phones.any { it.endsWith("674640544") })
        assertTrue(fields.phones.any { it.contains("472860192") })
        assertTrue(fields.urls.any { it.contains("dimosoftware.fr") })
        assertTrue(oriented.bitmap.height > oriented.bitmap.width)
    }

    @Test fun photoSansExifMaisRotationCameraResteLisible() {
        // Image déjà orientée (après transpose) : EXIF normal, CameraX dit 0.
        val orientedJpeg = javaClass.classLoader!!
            .getResourceAsStream("cards/dimo_genoux_oriented.jpg")!!
            .readBytes()
        val bmp = BitmapFactory.decodeByteArray(orientedJpeg, 0, orientedJpeg.size)!!
        val result = ImageOrientation.fromJpegBytes(orientedJpeg, cameraRotationDegrees = 0)!!
        assertEquals(bmp.width, result.bitmap.width)
        assertEquals(bmp.height, result.bitmap.height)
    }

    @Test fun toutesOrientationsExifPrincipalesSontGerees() {
        val cases = listOf(
            ExifCase(androidx.exifinterface.media.ExifInterface.ORIENTATION_NORMAL, 0, false),
            ExifCase(androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_90, 90, true),
            ExifCase(androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_180, 180, true),
            ExifCase(androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_270, 270, true),
        )
        for (c in cases) {
            assertEquals(c.degrees, ImageOrientation.exifRotationDegrees(c.orientation))
        }
    }

    private data class ExifCase(val orientation: Int, val degrees: Int, val useful: Boolean)
}
