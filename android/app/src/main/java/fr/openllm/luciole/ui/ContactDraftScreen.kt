package fr.openllm.luciole.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import fr.openllm.luciole.R
import fr.openllm.luciole.ui.theme.Bordure
import fr.openllm.luciole.ui.theme.Encre
import fr.openllm.luciole.ui.theme.Fond
import fr.openllm.luciole.ui.theme.Surface
import fr.openllm.luciole.ui.theme.TexteFaible

@Composable
fun ContactDraftScreen(
    draft: ContactDraftUi,
    onCardChange: (fr.openllm.luciole.contact.ContactCard) -> Unit,
    onCreateContact: () -> Unit,
    onExportVcf: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.fillMaxSize().background(Fond).padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(stringResource(R.string.scan_brouillon_titre), style = MaterialTheme.typography.titleLarge, color = Encre)
        Text(stringResource(R.string.scan_brouillon_sous_titre), color = TexteFaible)
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            ContactDraftFields(card = draft.card, onChange = onCardChange)
            Text(stringResource(R.string.scan_ocr_brut), color = Encre)
            Box(
                Modifier.fillMaxWidth().background(Surface, RoundedCornerShape(12.dp))
                    .border(1.dp, Bordure, RoundedCornerShape(12.dp)).padding(12.dp),
            ) {
                Text(draft.rawOcrText, fontFamily = FontFamily.Monospace, color = TexteFaible)
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = onCreateContact, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.scan_creer_contact))
            }
            OutlinedButton(onClick = onExportVcf, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.scan_exporter_vcf))
            }
        }
        OutlinedButton(onClick = onRetry, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.scan_reprendre))
        }
    }
}

@Composable
fun ScanProgress(messageRes: Int) {
    Column(
        Modifier.fillMaxSize().background(Fond),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator()
        Text(stringResource(messageRes), modifier = Modifier.padding(top = 16.dp), color = TexteFaible)
    }
}
