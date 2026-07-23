package fr.openllm.luciole.ocr

import android.graphics.Bitmap
import android.graphics.Matrix

/**
 * Essaie plusieurs orientations et retient celle qui maximise
 * les indices de carte de visite (email, téléphone, URL, volume de texte).
 */
object OcrOrientation {
    private val angles = intArrayOf(0, 90, 180, 270)

    fun recognizeBest(bitmap: Bitmap, recognize: (Bitmap) -> OcrResult): OcrResult {
        var best: OcrResult? = null
        var bestScore = Int.MIN_VALUE
        for (angle in angles) {
            val candidate = if (angle == 0) bitmap else rotate(bitmap, angle)
            val result = runCatching { recognize(candidate) }.getOrNull() ?: continue
            val score = score(result)
            if (score > bestScore) {
                bestScore = score
                best = result
            }
            if (candidate !== bitmap) candidate.recycle()
            // Assez bon : email + téléphone trouvés
            if (result.emails.isNotEmpty() && result.phones.isNotEmpty()) break
        }
        return best ?: throw OcrException("OCR vide — reprenez la photo")
    }

    fun score(result: OcrResult): Int {
        var s = result.rawText.length
        s += result.emails.size * 200
        s += result.phones.size * 150
        s += result.urls.size * 100
        if (result.rawText.contains('@')) s += 50
        return s
    }

    fun rotate(src: Bitmap, degrees: Int): Bitmap {
        val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
        return Bitmap.createBitmap(src, 0, 0, src.width, src.height, matrix, true)
    }
}
