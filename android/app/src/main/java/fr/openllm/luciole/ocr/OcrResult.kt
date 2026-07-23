package fr.openllm.luciole.ocr

data class OcrResult(
    val rawText: String,
    val lines: List<String> = emptyList(),
    val phones: List<String> = emptyList(),
    val emails: List<String> = emptyList(),
    val urls: List<String> = emptyList(),
    val confidence: Float? = null,
)
