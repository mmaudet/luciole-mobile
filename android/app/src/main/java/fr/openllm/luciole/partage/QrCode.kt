package fr.openllm.luciole.partage

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/** Génère un QR code (ImageBitmap) à partir d'un contenu texte. 100 % local (ZXing). */
fun qrBitmap(content: String, size: Int = 640): ImageBitmap {
    val hints = mapOf(
        EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
        EncodeHintType.MARGIN to 1,
    )
    val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size, hints)
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    for (x in 0 until size) {
        for (y in 0 until size) {
            bmp.setPixel(x, y, if (matrix[x, y]) Color.BLACK else Color.WHITE)
        }
    }
    return bmp.asImageBitmap()
}

private fun echappe(s: String): String =
    s.replace("\\", "\\\\").replace(";", "\\;").replace(",", "\\,").replace("\"", "\\\"").replace(":", "\\:")

/** Contenu QR standard pour rejoindre un réseau WiFi WPA (scan = connexion, sans saisie). */
fun wifiQr(ssid: String, motDePasse: String): String =
    "WIFI:T:WPA;S:${echappe(ssid)};P:${echappe(motDePasse)};;"
