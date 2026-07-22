package fr.openllm.luciole.scan

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream

/**
 * Normalise l'orientation d'une photo avant scan/OCR.
 *
 * Cas couverts (difficultés utilisateur réelles) :
 * - EXIF Orientation 1/3/6/8 (0°, 180°, 90°, 270°)
 * - miroirs EXIF (FLIP_*)
 * - rotation CameraX [ImageInfo.rotationDegrees] si EXIF absent/normal
 * - pas de double rotation EXIF + CameraX
 * - JPEG décodé via [BitmapFactory.decodeByteArray] (ignore EXIF tout seul)
 */
object ImageOrientation {

    data class OrientedBitmap(
        val bitmap: Bitmap,
        /** Degrés appliqués (0/90/180/270), hors miroirs. */
        val appliedRotationDegrees: Int,
        /** true si la correction vient de l'EXIF, false si fallback CameraX. */
        val fromExif: Boolean,
        val exifOrientation: Int,
    )

    fun fromJpegBytes(
        jpegBytes: ByteArray,
        cameraRotationDegrees: Int = 0,
        opts: BitmapFactory.Options? = null,
    ): OrientedBitmap? {
        val bitmap = BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size, opts) ?: return null
        val exif = runCatching {
            ExifInterface(ByteArrayInputStream(jpegBytes))
        }.getOrNull()
        return orient(bitmap, exif, cameraRotationDegrees)
    }

    fun fromFile(file: File, cameraRotationDegrees: Int = 0): OrientedBitmap? {
        val bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: return null
        val exif = runCatching { ExifInterface(file) }.getOrNull()
        return orient(bitmap, exif, cameraRotationDegrees)
    }

    fun fromStream(stream: InputStream, cameraRotationDegrees: Int = 0): OrientedBitmap? {
        val bytes = stream.readBytes()
        return fromJpegBytes(bytes, cameraRotationDegrees)
    }

    /**
     * Applique EXIF si présent et non-NORMAL, sinon la rotation caméra.
     * Les deux ne sont jamais cumulés.
     */
    fun orient(
        bitmap: Bitmap,
        exif: ExifInterface?,
        cameraRotationDegrees: Int = 0,
    ): OrientedBitmap {
        val orientation = exif?.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_UNDEFINED,
        ) ?: ExifInterface.ORIENTATION_UNDEFINED

        val exifUseful = orientation != ExifInterface.ORIENTATION_UNDEFINED
            && orientation != ExifInterface.ORIENTATION_NORMAL

        return if (exifUseful) {
            val transformed = applyExifTransform(bitmap, orientation)
            OrientedBitmap(
                bitmap = transformed,
                appliedRotationDegrees = exifRotationDegrees(orientation),
                fromExif = true,
                exifOrientation = orientation,
            )
        } else {
            val deg = normalizeDegrees(cameraRotationDegrees)
            val transformed = if (deg == 0) bitmap else rotate(bitmap, deg)
            OrientedBitmap(
                bitmap = transformed,
                appliedRotationDegrees = deg,
                fromExif = false,
                exifOrientation = orientation,
            )
        }
    }

    fun exifRotationDegrees(orientation: Int): Int = when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90,
        ExifInterface.ORIENTATION_TRANSPOSE,
        -> 90
        ExifInterface.ORIENTATION_ROTATE_180,
        ExifInterface.ORIENTATION_FLIP_VERTICAL,
        -> 180
        ExifInterface.ORIENTATION_ROTATE_270,
        ExifInterface.ORIENTATION_TRANSVERSE,
        -> 270
        else -> 0
    }

    fun normalizeDegrees(degrees: Int): Int {
        var d = degrees % 360
        if (d < 0) d += 360
        return when (d) {
            in 45 until 135 -> 90
            in 135 until 225 -> 180
            in 225 until 315 -> 270
            else -> 0
        }
    }

    internal fun applyExifTransform(src: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.setScale(-1f, 1f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.setRotate(180f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> {
                matrix.setRotate(180f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.setRotate(90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.setRotate(90f)
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.setRotate(270f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.setRotate(270f)
            else -> return src
        }
        return Bitmap.createBitmap(src, 0, 0, src.width, src.height, matrix, true).also {
            if (it !== src && !src.isRecycled) {
                // ne recycle pas src : l'appelant peut encore le tenir
            }
        }
    }

    private fun rotate(src: Bitmap, degrees: Int): Bitmap {
        val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
        return Bitmap.createBitmap(src, 0, 0, src.width, src.height, matrix, true)
    }
}
