package fr.openllm.luciole.ocr

import android.graphics.Bitmap

interface OcrEngine {
    fun recognize(bitmap: Bitmap): OcrResult
}

class OcrException(message: String, cause: Throwable? = null) : Exception(message, cause)
