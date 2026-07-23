package fr.openllm.luciole.ocr

import android.content.Context
import android.graphics.Bitmap
import com.googlecode.tesseract.android.TessBaseAPI
import java.io.File
import java.io.FileOutputStream

class TesseractOcrEngine(
    private val context: Context,
    private val languages: String = "fra+eng",
) : OcrEngine {

    override fun recognize(bitmap: Bitmap): OcrResult {
        val dataPath = ensureTessData()
        return OcrOrientation.recognizeBest(bitmap) { candidate ->
            recognizeOnce(candidate, dataPath)
        }
    }

    private fun recognizeOnce(bitmap: Bitmap, dataPath: String): OcrResult {
        val api = TessBaseAPI()
        try {
            if (!api.init(dataPath, languages)) {
                throw OcrException("Impossible d'initialiser Tesseract ($languages)")
            }
            api.pageSegMode = TessBaseAPI.PageSegMode.PSM_AUTO
            api.setImage(bitmap)
            val raw = api.utF8Text.orEmpty().trim()
            if (raw.isBlank()) throw OcrException("OCR vide")
            return OcrPostProcessor.process(raw)
        } finally {
            api.recycle()
        }
    }

    private fun ensureTessData(): String {
        val tessRoot = File(context.filesDir, "tesseract").apply { mkdirs() }
        val tessData = File(tessRoot, "tessdata").apply { mkdirs() }
        val langs = languages.split('+')
        for (lang in langs) {
            val target = File(tessData, "$lang.traineddata")
            if (!target.exists()) {
                copyAsset("tesseract/tessdata/$lang.traineddata", target)
            }
            if (!target.exists()) {
                throw OcrException(
                    "Modèle OCR manquant : $lang.traineddata — placez-le dans assets/tesseract/tessdata/"
                )
            }
        }
        return tessRoot.absolutePath
    }

    private fun copyAsset(assetPath: String, target: File) {
        runCatching {
            context.assets.open(assetPath).use { input ->
                FileOutputStream(target).use { output -> input.copyTo(output) }
            }
        }
    }
}
