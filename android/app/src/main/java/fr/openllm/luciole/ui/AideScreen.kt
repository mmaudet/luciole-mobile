package fr.openllm.luciole.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.openllm.luciole.R
import fr.openllm.luciole.ui.theme.Bleu
import fr.openllm.luciole.ui.theme.BleuBordure
import fr.openllm.luciole.ui.theme.BleuClair
import fr.openllm.luciole.ui.theme.Bordure
import fr.openllm.luciole.ui.theme.Encre
import fr.openllm.luciole.ui.theme.Fond
import fr.openllm.luciole.ui.theme.Surface
import fr.openllm.luciole.ui.theme.TexteMuet

private data class Gabarit(val labelRes: Int, val texte: String, val placeholder: String, val iconKey: String)

private val GABARITS = listOf(
    Gabarit(R.string.act_appel,      "appelle Marie Curie",                    "Marie Curie",           "appel"),
    Gabarit(R.string.act_minuteur,   "minuteur de 5 minutes",                  "5",                     "minuteur"),
    Gabarit(R.string.act_alarme,     "réveille-moi à 7h30",                    "7h30",                  "alarme"),
    Gabarit(R.string.act_agenda,     "ajoute une réunion demain à 10h",        "une réunion",           "agenda"),
    Gabarit(R.string.act_message,    "envoie un SMS pour dire à bientôt",      "à bientôt",             "message"),
    Gabarit(R.string.act_itineraire, "itinéraire vers la gare de Lyon",        "la gare de Lyon",       "itineraire"),
    Gabarit(R.string.act_recherche,  "cherche la capitale du Pérou",           "la capitale du Pérou",  "recherche"),
    Gabarit(R.string.act_traduction, "traduis bonjour en anglais",             "bonjour",               "traduction"),
    Gabarit(R.string.act_note,       "note : acheter du pain",                 "acheter du pain",       "note"),
    Gabarit(R.string.act_ouvrir,     "ouvre YouTube",                          "YouTube",               "ouvrir"),
)

@Composable
fun AideScreen(
    expanded: Boolean,
    onInserer: (texte: String, placeholder: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val cols = if (expanded) 2 else 3
    val chunks = GABARITS.chunked(cols)

    Column(modifier.background(Fond).verticalScroll(rememberScrollState())) {

        // ── Header ────────────────────────────────────────────────────────────
        Column(
            Modifier
                .fillMaxWidth()
                .background(Surface)
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Text(
                stringResource(R.string.aide_gabarits),
                style = MaterialTheme.typography.titleLarge,
                color = Encre,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.aide_sous_titre),
                style = MaterialTheme.typography.bodySmall,
                color = TexteMuet,
            )
        }

        // ── Grid ──────────────────────────────────────────────────────────────
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 15.dp)
                .padding(top = 15.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            chunks.forEach { row ->
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                ) {
                    row.forEach { g ->
                        GabaritChip(
                            g = g,
                            expanded = expanded,
                            onClick = { onInserer(g.texte, g.placeholder) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    // pad incomplete last row
                    repeat(cols - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // ── Featured card ─────────────────────────────────────────────────────
        FeaturedCard(
            onInserer = onInserer,
            modifier = Modifier
                .padding(horizontal = 15.dp)
                .padding(bottom = 20.dp),
        )
    }
}

@Composable
private fun GabaritChip(
    g: Gabarit,
    expanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = stringResource(g.labelRes).substringAfter(' ')
    Box(
        modifier
            .clip(RoundedCornerShape(13.dp))
            .background(Surface)
            .border(1.dp, Bordure, RoundedCornerShape(13.dp))
            .clickable(onClick = onClick)
            .padding(11.dp),
    ) {
        if (expanded) {
            // Fold — horizontal
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(actionIcon(g.iconKey), contentDescription = null, tint = Bleu, modifier = Modifier.size(21.dp))
                Text(label, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = Encre)
            }
        } else {
            // Phone — vertical, centré
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(actionIcon(g.iconKey), contentDescription = null, tint = Bleu, modifier = Modifier.size(21.dp))
                Text(
                    label,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Encre,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun FeaturedCard(
    onInserer: (String, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Surface)
            .border(1.5.dp, Bleu, RoundedCornerShape(16.dp))
            .padding(15.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Label catégorie
        Text(
            stringResource(R.string.act_itineraire).substringAfter(' ').uppercase(),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = Bleu,
            letterSpacing = 1.1.sp,
        )

        // Ligne avec entité surlignée
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                stringResource(R.string.aide_feat_pre),
                style = MaterialTheme.typography.bodyMedium,
                color = Encre,
            )
            Box(
                Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(BleuClair)
                    .border(1.dp, BleuBordure, RoundedCornerShape(6.dp))
                    .padding(horizontal = 7.dp, vertical = 2.dp),
            ) {
                Text(
                    stringResource(R.string.aide_feat_ent),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Bleu,
                )
            }
        }

        // Indice
        Text(
            stringResource(R.string.aide_feat_hint),
            style = MaterialTheme.typography.bodySmall,
            color = TexteMuet,
        )

        // Bouton insérer
        Button(
            onClick = { onInserer("itinéraire vers la gare de Lyon", "la gare de Lyon") },
            modifier = Modifier.fillMaxWidth().height(43.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Bleu),
        ) {
            Text(stringResource(R.string.aide_inserer), fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
        }
    }
}
