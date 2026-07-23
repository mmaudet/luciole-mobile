package fr.openllm.luciole.scan

import android.graphics.Bitmap
import android.graphics.PointF

data class ScanCorners(
    val topLeft: PointF,
    val topRight: PointF,
    val bottomRight: PointF,
    val bottomLeft: PointF,
)

data class ScanResult(
    val bitmap: Bitmap,
    val angleDegrees: Float = 0f,
    val confidence: Float = 1f,
    val corners: ScanCorners? = null,
)

interface ScanEngine {
    fun scan(source: Bitmap, manualCorners: ScanCorners? = null): ScanResult
}

class ScanException(message: String, cause: Throwable? = null) : Exception(message, cause)
