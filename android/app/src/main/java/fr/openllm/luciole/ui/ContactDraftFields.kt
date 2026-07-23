package fr.openllm.luciole.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import fr.openllm.luciole.R
import fr.openllm.luciole.contact.ContactCard

@Composable
fun ContactDraftFields(
    card: ContactCard,
    onChange: (ContactCard) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Pas de verticalScroll ici : le parent ContactDraftScreen scrolle déjà.
    Column(
        modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        DraftField(stringResource(R.string.scan_champ_nom), card.fullName.orEmpty()) {
            onChange(card.copy(fullName = it.ifBlank { null }))
        }
        DraftField(stringResource(R.string.scan_champ_prenom), card.firstName.orEmpty()) {
            onChange(card.copy(firstName = it.ifBlank { null }))
        }
        DraftField(stringResource(R.string.scan_champ_nom_famille), card.lastName.orEmpty()) {
            onChange(card.copy(lastName = it.ifBlank { null }))
        }
        DraftField(stringResource(R.string.scan_champ_societe), card.company.orEmpty()) {
            onChange(card.copy(company = it.ifBlank { null }))
        }
        DraftField(stringResource(R.string.scan_champ_poste), card.jobTitle.orEmpty()) {
            onChange(card.copy(jobTitle = it.ifBlank { null }))
        }
        DraftField(stringResource(R.string.scan_champ_tel), card.phones.joinToString(", ")) {
            onChange(card.copy(phones = it.split(',', ';').map { p -> p.trim() }.filter { p -> p.isNotEmpty() }))
        }
        DraftField(stringResource(R.string.scan_champ_email), card.emails.joinToString(", ")) {
            onChange(card.copy(emails = it.split(',', ';').map { e -> e.trim() }.filter { e -> e.isNotEmpty() }))
        }
        DraftField(stringResource(R.string.scan_champ_site), card.website.orEmpty()) {
            onChange(card.copy(website = it.ifBlank { null }))
        }
        DraftField(stringResource(R.string.scan_champ_adresse), card.address.orEmpty()) {
            onChange(card.copy(address = it.ifBlank { null }))
        }
        DraftField(stringResource(R.string.scan_champ_note), card.note.orEmpty(), minLines = 2) {
            onChange(card.copy(note = it.ifBlank { null }))
        }
    }
}

@Composable
private fun DraftField(
    label: String,
    value: String,
    minLines: Int = 1,
    onChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        minLines = minLines,
        modifier = Modifier.fillMaxWidth(),
    )
}
