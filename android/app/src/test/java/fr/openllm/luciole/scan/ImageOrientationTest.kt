package fr.openllm.luciole.scan

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.exifinterface.media.ExifInterface
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests génériques d'orientation : EXIF, fallback CameraX, anti double-rotation,
 * et fixture réelle (carte dimo photographiée en conditions utilisateur).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
class ImageOrientationTest {

    @Test fun exif6FaitTourner90EtInverseDimensions() {
        val src = solidBitmap(200, 100, Color.RED)
        val jpeg = encodeJpeg(src)
        val file = writeJpegWithExif(jpeg, ExifInterface.ORIENTATION_ROTATE_90)
        val oriented = ImageOrientation.fromFile(file)!!
        assertTrue(oriented.fromExif)
        assertEquals(90, oriented.appliedRotationDegrees)
        assertEquals(100, oriented.bitmap.width)
        assertEquals(200, oriented.bitmap.height)
    }

    @Test fun exif3FaitTourner180SansChangerDimensions() {
        val src = solidBitmap(160, 80, Color.BLUE)
        val file = writeJpegWithExif(encodeJpeg(src), ExifInterface.ORIENTATION_ROTATE_180)
        val oriented = ImageOrientation.fromFile(file)!!
        assertEquals(180, oriented.appliedRotationDegrees)
        assertEquals(160, oriented.bitmap.width)
        assertEquals(80, oriented.bitmap.height)
    }

    @Test fun exif8FaitTourner270() {
        val src = solidBitmap(120, 60, Color.GREEN)
        val file = writeJpegWithExif(encodeJpeg(src), ExifInterface.ORIENTATION_ROTATE_270)
        val oriented = ImageOrientation.fromFile(file)!!
        assertEquals(270, oriented.appliedRotationDegrees)
        assertEquals(60, oriented.bitmap.width)
        assertEquals(120, oriented.bitmap.height)
    }

    @Test fun sansExifUtiliseRotationCameraX() {
        val src = solidBitmap(200, 100, Color.YELLOW)
        val jpeg = encodeJpeg(src) // EXIF normal / absent
        val oriented = ImageOrientation.fromJpegBytes(jpeg, cameraRotationDegrees = 90)!!
        assertFalse(oriented.fromExif)
        assertEquals(90, oriented.appliedRotationDegrees)
        assertEquals(100, oriented.bitmap.width)
        assertEquals(200, oriented.bitmap.height)
    }

    @Test fun exifPrioritaireSurCameraXPasDeDoubleRotation() {
        val src = solidBitmap(200, 100, Color.MAGENTA)
        val file = writeJpegWithExif(encodeJpeg(src), ExifInterface.ORIENTATION_ROTATE_90)
        // CameraX dirait aussi 90° : si on cumule → 180° et dimensions d'origine. On refuse ça.
        val oriented = ImageOrientation.fromFile(file, cameraRotationDegrees = 90)!!
        assertTrue(oriented.fromExif)
        assertEquals(90, oriented.appliedRotationDegrees)
        assertEquals(100, oriented.bitmap.width)
        assertEquals(200, oriented.bitmap.height)
    }

    @Test fun normalizeDegreesQuantifieAuxQuarts() {
        assertEquals(0, ImageOrientation.normalizeDegrees(10))
        assertEquals(90, ImageOrientation.normalizeDegrees(95))
        assertEquals(180, ImageOrientation.normalizeDegrees(190))
        assertEquals(270, ImageOrientation.normalizeDegrees(-90))
    }

    @Test fun carteDimoReelleOrientationExif6() {
        val bytes = javaClass.classLoader!!
            .getResourceAsStream("cards/dimo_genoux.jpg")!!
            .readBytes()
        val oriented = ImageOrientation.fromJpegBytes(bytes)!!
        assertNotNull(oriented.bitmap)
        // Photo OnePlus : EXIF orientation 6 → 90° → portrait 3008x4000
        assertTrue(oriented.fromExif, "la carte dimo doit être corrigée via EXIF")
        assertEquals(90, oriented.appliedRotationDegrees)
        assertEquals(3008, oriented.bitmap.width)
        assertEquals(4000, oriented.bitmap.height)
        assertTrue(oriented.bitmap.height > oriented.bitmap.width, "attendu portrait après EXIF")
    }

    @Test fun carteDimoNeDoitPasEtreDoubleTourneeSiCameraDitAussi90() {
        val bytes = javaClass.classLoader!!
            .getResourceAsStream("cards/dimo_genoux.jpg")!!
            .readBytes()
        val oriented = ImageOrientation.fromJpegBytes(bytes, cameraRotationDegrees = 90)!!
        assertEquals(3008, oriented.bitmap.width)
        assertEquals(4000, oriented.bitmap.height)
    }

    private fun solidBitmap(w: Int, h: Int, color: Int): Bitmap {
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        Canvas(bmp).drawColor(color)
        // marqueur coin pour debug visuel
        Canvas(bmp).drawRect(0f, 0f, 8f, 8f, Paint().apply { this.color = Color.BLACK })
        return bmp
    }

    private fun encodeJpeg(bitmap: Bitmap): ByteArray {
        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
        return out.toByteArray()
    }

    private fun writeJpegWithExif(jpeg: ByteArray, orientation: Int): File {
        val file = File.createTempFile("orient", ".jpg")
        file.writeBytes(jpeg)
        ExifInterface(file.absolutePath).apply {
            setAttribute(ExifInterface.TAG_ORIENTATION, orientation.toString())
            saveAttributes()
        }
        return file
    }
}
