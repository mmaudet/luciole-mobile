package fr.openllm.luciole.contact

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

object VcfShare {
    fun fileName(card: ContactCard): String {
        val base = card.displayName().ifBlank { "contact" }
            .lowercase()
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')
            .ifBlank { "contact" }
        return "$base.vcf"
    }

    fun writeTempFile(context: Context, card: ContactCard): File {
        val dir = File(context.cacheDir, "vcf").apply { mkdirs() }
        val file = File(dir, fileName(card))
        file.writeText(VCardSerializer.toVCard3(card))
        return file
    }

    fun shareIntent(context: Context, card: ContactCard): Intent {
        val file = writeTempFile(context, card)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        return Intent(Intent.ACTION_SEND).apply {
            type = "text/vcard"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
