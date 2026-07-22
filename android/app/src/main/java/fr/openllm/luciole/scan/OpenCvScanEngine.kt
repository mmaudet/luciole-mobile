package fr.openllm.luciole.scan

import android.graphics.Bitmap
import android.graphics.PointF
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.hypot
import kotlin.math.max

class OpenCvScanEngine : ScanEngine {

    override fun scan(source: Bitmap, manualCorners: ScanCorners?): ScanResult {
        if (!ensureOpenCv()) throw ScanException("OpenCV non initialisé")
        val src = Mat()
        Utils.bitmapToMat(source, src)
        try {
            val working = resizeIfNeeded(src)
            val corners: MatOfPoint2f
            val detected: Boolean
            if (manualCorners != null) {
                corners = manualCorners.toMatPoints()
                detected = true
            } else {
                val found = detectDocumentCorners(working)
                if (found != null) {
                    corners = found
                    detected = true
                } else {
                    corners = fullFrameCorners(working)
                    detected = false
                }
            }
            // Pas de deskew via findNonZero : alloue des millions de points et tue l'app (OOM).
            val warped = perspectiveCorrect(working, corners)
            val outBitmap = Bitmap.createBitmap(warped.cols(), warped.rows(), Bitmap.Config.ARGB_8888)
            Utils.matToBitmap(warped, outBitmap)
            warped.release()
            if (working !== src) working.release()
            return ScanResult(
                bitmap = outBitmap,
                angleDegrees = estimateSkewAngle(corners),
                confidence = when {
                    manualCorners != null -> 1f
                    detected -> 0.8f
                    else -> 0.4f
                },
                corners = corners.toScanCorners(),
            )
        } finally {
            src.release()
        }
    }

    private fun resizeIfNeeded(src: Mat): Mat {
        val maxDim = max(src.rows(), src.cols())
        if (maxDim <= MAX_DIM) return src.clone()
        val scale = MAX_DIM.toDouble() / maxDim
        val resized = Mat()
        Imgproc.resize(src, resized, Size(src.cols() * scale, src.rows() * scale))
        return resized
    }

    /** Retourne les coins détectés, ou null si aucune stratégie n'a convergé. */
    private fun detectDocumentCorners(src: Mat): MatOfPoint2f? {
        val gray = toGray(src)
        val imageArea = src.rows() * src.cols()
        try {
            detectViaAdaptiveThreshold(gray, imageArea)?.let { return it }
            detectViaCanny(gray, imageArea)?.let { return it }
            detectViaMinAreaRect(gray, imageArea)?.let { return it }
            return null
        } finally {
            gray.release()
        }
    }

    private fun detectViaAdaptiveThreshold(gray: Mat, imageArea: Int): MatOfPoint2f? {
        val blurred = Mat()
        val thresh = Mat()
        val kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(3.0, 3.0))
        try {
            Imgproc.GaussianBlur(gray, blurred, Size(5.0, 5.0), 0.0)
            Imgproc.adaptiveThreshold(
                blurred, thresh, 255.0,
                Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 11, 2.0,
            )
            Imgproc.morphologyEx(thresh, thresh, Imgproc.MORPH_CLOSE, kernel)
            return findQuadContour(thresh, imageArea, minAreaRatio = 0.03, epsilons = listOf(0.02, 0.035, 0.05))
        } finally {
            blurred.release()
            thresh.release()
            kernel.release()
        }
    }

    private fun detectViaCanny(gray: Mat, imageArea: Int): MatOfPoint2f? {
        val edges = Mat()
        val kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(3.0, 3.0))
        try {
            Imgproc.GaussianBlur(gray, edges, Size(5.0, 5.0), 0.0)
            Imgproc.Canny(edges, edges, 40.0, 120.0)
            Imgproc.dilate(edges, edges, kernel)
            return findQuadContour(edges, imageArea, minAreaRatio = 0.05, epsilons = listOf(0.02, 0.04, 0.06))
        } finally {
            edges.release()
            kernel.release()
        }
    }

    /** Dernier recours : boîte englobante orientée du plus grand contour. */
    private fun detectViaMinAreaRect(gray: Mat, imageArea: Int): MatOfPoint2f? {
        val thresh = Mat()
        try {
            Imgproc.threshold(gray, thresh, 0.0, 255.0, Imgproc.THRESH_BINARY + Imgproc.THRESH_OTSU)
            val contours = mutableListOf<MatOfPoint>()
            Imgproc.findContours(thresh, contours, Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)
            val largest = contours.maxByOrNull { Imgproc.contourArea(it) } ?: return null
            val area = Imgproc.contourArea(largest)
            if (area < imageArea * 0.12) return null
            val rect = Imgproc.minAreaRect(MatOfPoint2f(*largest.toArray().map { Point(it.x, it.y) }.toTypedArray()))
            val box = MatOfPoint2f()
            Imgproc.boxPoints(rect, box)
            return orderCorners(box)
        } finally {
            thresh.release()
        }
    }

    private fun findQuadContour(
        binary: Mat,
        imageArea: Int,
        minAreaRatio: Double,
        epsilons: List<Double>,
    ): MatOfPoint2f? {
        val contours = mutableListOf<MatOfPoint>()
        Imgproc.findContours(binary, contours, Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)
        contours.sortByDescending { Imgproc.contourArea(it) }
        val minArea = imageArea * minAreaRatio
        for (contour in contours.take(25)) {
            val area = Imgproc.contourArea(contour)
            if (area < minArea) continue
            val curve = MatOfPoint2f(*contour.toArray())
            val peri = Imgproc.arcLength(curve, true)
            for (eps in epsilons) {
                val approx = MatOfPoint2f()
                Imgproc.approxPolyDP(curve, approx, eps * peri, true)
                if (approx.total() == 4L && isConvexQuad(approx)) {
                    return orderCorners(approx)
                }
            }
        }
        return null
    }

    private fun isConvexQuad(approx: MatOfPoint2f): Boolean {
        val pts = approx.toArray()
        if (pts.size != 4) return false
        var sign = 0
        for (i in pts.indices) {
            val a = pts[i]
            val b = pts[(i + 1) % 4]
            val c = pts[(i + 2) % 4]
            val cross = (b.x - a.x) * (c.y - b.y) - (b.y - a.y) * (c.x - b.x)
            if (kotlin.math.abs(cross) < 1.0) continue
            val s = if (cross > 0) 1 else -1
            if (sign == 0) sign = s else if (sign != s) return false
        }
        return true
    }

    private fun fullFrameCorners(src: Mat, marginRatio: Double = 0.02): MatOfPoint2f {
        val mx = src.cols() * marginRatio
        val my = src.rows() * marginRatio
        val w = src.cols() - 1.0
        val h = src.rows() - 1.0
        return MatOfPoint2f(
            Point(mx, my),
            Point(w - mx, my),
            Point(w - mx, h - my),
            Point(mx, h - my),
        )
    }

    private fun toGray(src: Mat): Mat {
        val gray = Mat()
        when (src.channels()) {
            4 -> Imgproc.cvtColor(src, gray, Imgproc.COLOR_RGBA2GRAY)
            3 -> Imgproc.cvtColor(src, gray, Imgproc.COLOR_BGR2GRAY)
            else -> src.copyTo(gray)
        }
        return gray
    }

    private fun perspectiveCorrect(src: Mat, corners: MatOfPoint2f): Mat {
        val pts = corners.toArray()
        val widthA = distance(pts[2], pts[3])
        val widthB = distance(pts[1], pts[0])
        val maxWidth = max(widthA, widthB).toInt().coerceAtLeast(1)
        val heightA = distance(pts[1], pts[2])
        val heightB = distance(pts[0], pts[3])
        val maxHeight = max(heightA, heightB).toInt().coerceAtLeast(1)
        val srcPts = MatOfPoint2f(pts[0], pts[1], pts[2], pts[3])
        val dstPts = MatOfPoint2f(
            Point(0.0, 0.0),
            Point(maxWidth - 1.0, 0.0),
            Point(maxWidth - 1.0, maxHeight - 1.0),
            Point(0.0, maxHeight - 1.0),
        )
        val matrix = Imgproc.getPerspectiveTransform(srcPts, dstPts)
        val out = Mat()
        Imgproc.warpPerspective(src, out, matrix, Size(maxWidth.toDouble(), maxHeight.toDouble()))
        matrix.release()
        return out
    }

    private fun orderCorners(approx: MatOfPoint2f): MatOfPoint2f {
        val pts = approx.toArray().sortedWith(compareBy({ it.y }, { it.x })).toMutableList()
        val top = pts.take(2).sortedBy { it.x }
        val bottom = pts.takeLast(2).sortedBy { it.x }
        return MatOfPoint2f(top[0], top[1], bottom[1], bottom[0])
    }

    private fun estimateSkewAngle(corners: MatOfPoint2f): Float {
        val pts = corners.toArray()
        val dx = pts[1].x - pts[0].x
        val dy = pts[1].y - pts[0].y
        return Math.toDegrees(kotlin.math.atan2(dy, dx)).toFloat()
    }

    private fun distance(a: Point, b: Point): Double = hypot(a.x - b.x, a.y - b.y)

    private fun ScanCorners.toMatPoints(): MatOfPoint2f =
        MatOfPoint2f(
            Point(topLeft.x.toDouble(), topLeft.y.toDouble()),
            Point(topRight.x.toDouble(), topRight.y.toDouble()),
            Point(bottomRight.x.toDouble(), bottomRight.y.toDouble()),
            Point(bottomLeft.x.toDouble(), bottomLeft.y.toDouble()),
        )

    private fun MatOfPoint2f.toScanCorners(): ScanCorners {
        val p = toArray()
        return ScanCorners(
            topLeft = PointF(p[0].x.toFloat(), p[0].y.toFloat()),
            topRight = PointF(p[1].x.toFloat(), p[1].y.toFloat()),
            bottomRight = PointF(p[2].x.toFloat(), p[2].y.toFloat()),
            bottomLeft = PointF(p[3].x.toFloat(), p[3].y.toFloat()),
        )
    }

    companion object {
        private const val MAX_DIM = 1500
        private val initialized = AtomicBoolean(false)

        fun ensureOpenCv(): Boolean {
            if (initialized.get()) return true
            synchronized(OpenCvScanEngine::class.java) {
                if (OpenCVLoader.initLocal()) {
                    initialized.set(true)
                    return true
                }
            }
            return false
        }
    }
}
