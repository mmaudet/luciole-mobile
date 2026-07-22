package fr.openllm.luciole.ui

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.openllm.luciole.cerveau.Cerveau
import fr.openllm.luciole.contact.ContactCard
import fr.openllm.luciole.contact.ContactDraftMerge
import fr.openllm.luciole.ocr.OcrEngine
import fr.openllm.luciole.ocr.OcrException
import fr.openllm.luciole.ocr.OcrResult
import fr.openllm.luciole.scan.ScanEngine
import fr.openllm.luciole.scan.ScanException
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface ScanUiState {
    data object Idle : ScanUiState
    data object Capturing : ScanUiState
    data object Scanning : ScanUiState
    data object OcrRunning : ScanUiState
    data object Structuring : ScanUiState
    data class DraftReady(val draft: ContactDraftUi) : ScanUiState
    data class Error(val message: String) : ScanUiState
}

data class ContactDraftUi(
    val card: ContactCard,
    val rawOcrText: String,
    val preview: Bitmap? = null,
)

class ScanCarteViewModel(
    private val scanEngine: ScanEngine,
    private val ocrEngine: OcrEngine,
    private val cerveau: Cerveau,
) : ViewModel() {

    private val _state = MutableStateFlow<ScanUiState>(ScanUiState.Idle)
    val state: StateFlow<ScanUiState> = _state.asStateFlow()

    fun onCaptureReady() {
        _state.value = ScanUiState.Capturing
    }

    fun processCapture(bitmap: Bitmap) {
        viewModelScope.launch {
            try {
                _state.value = ScanUiState.Scanning
                val scanned = withContext(Dispatchers.Default) { scanEngine.scan(bitmap) }
                _state.value = ScanUiState.OcrRunning
                val ocr = withContext(Dispatchers.Default) { ocrEngine.recognize(scanned.bitmap) }
                _state.value = ScanUiState.Structuring
                val llm = withContext(Dispatchers.IO) {
                    runCatching { cerveau.extractContact(ocr.rawText) }.getOrNull()
                }
                val merged = ContactDraftMerge.merge(llm, ocr)
                _state.value = ScanUiState.DraftReady(
                    ContactDraftUi(
                        card = merged,
                        rawOcrText = ocr.rawText,
                        preview = scanned.bitmap,
                    )
                )
            } catch (e: ScanException) {
                Log.e(TAG, "scan", e)
                _state.value = ScanUiState.Error(e.message ?: "scan_erreur")
            } catch (e: OcrException) {
                Log.e(TAG, "ocr", e)
                _state.value = ScanUiState.Error(e.message ?: "ocr_erreur")
            } catch (t: Throwable) {
                Log.e(TAG, "pipeline", t)
                _state.value = ScanUiState.Error(t.message ?: "scan_erreur")
            }
        }
    }

    companion object {
        private const val TAG = "LucioleScan"
    }

    fun updateDraft(card: ContactCard) {
        val current = _state.value
        if (current is ScanUiState.DraftReady) {
            _state.value = current.copy(draft = current.draft.copy(card = card))
        }
    }

    fun reset() {
        _state.value = ScanUiState.Idle
    }

    fun onCaptureDecodeFailed() {
        _state.value = ScanUiState.Error("Capture impossible — réessayez")
    }
}
