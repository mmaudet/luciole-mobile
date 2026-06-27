package fr.openllm.luciole.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.openllm.luciole.R
import fr.openllm.luciole.moniteur.MoniteurViewModel
import fr.openllm.luciole.ui.theme.Bleu
import fr.openllm.luciole.ui.theme.BleuClair
import fr.openllm.luciole.ui.theme.BleuHisto
import fr.openllm.luciole.ui.theme.Bordure
import fr.openllm.luciole.ui.theme.Encre
import fr.openllm.luciole.ui.theme.Fond
import fr.openllm.luciole.ui.theme.Or
import fr.openllm.luciole.ui.theme.OrClair
import fr.openllm.luciole.ui.theme.OrTexte
import fr.openllm.luciole.ui.theme.OrVif
import fr.openllm.luciole.ui.theme.PlexMono
import fr.openllm.luciole.ui.theme.Spectral
import fr.openllm.luciole.ui.theme.Surface
import fr.openllm.luciole.ui.theme.TexteFaible
import fr.openllm.luciole.ui.theme.TexteMuet
import fr.openllm.luciole.ui.theme.Vert
import fr.openllm.luciole.ui.theme.VertClair
import fr.openllm.luciole.ui.theme.VertSombre

@Composable
fun StatistiquesScreen(
    vm: MoniteurViewModel,
    expanded: Boolean,
    modifier: Modifier = Modifier,
) {
    val snap by vm.state.collectAsState()
    val historique by vm.historique.collectAsState()

    // Polling live tant que l'écran est composé
    LaunchedEffect(Unit) {
        while (true) {
            try {
                vm.rafraichir()
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                // réseau absent ou service indisponible → on réessaie au prochain tick
            }
            kotlinx.coroutines.delay(1000)
        }
    }

    Box(modifier.fillMaxSize().background(Fond)) {
        if (expanded) FoldStats(snap, historique)
        else PhoneStats(snap, historique)
    }
}

// ─── Phone layout ─────────────────────────────────────────────────────────────

@Composable
private fun PhoneStats(
    snap: fr.openllm.luciole.moniteur.MetricsSnapshot,
    historique: List<Int>,
) {
    Column(Modifier.fillMaxSize()) {
        // Header
        Column(
            Modifier
                .fillMaxWidth()
                .background(Surface)
                .padding(horizontal = 16.dp, vertical = 13.dp),
        ) {
            Text(
                stringResource(R.string.nav_moniteur).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = TexteFaible,
                letterSpacing = 1.4.sp,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                stringResource(R.string.stats_titre),
                style = MaterialTheme.typography.headlineSmall,
                fontFamily = Spectral,
                color = Encre,
            )
        }

        // Scrollable content
        Column(Modifier.fillMaxWidth().weight(1f).padding(15.dp)) {

            // Carte sombre — confidentialité locale
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(15.dp))
                    .background(Encre)
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    Modifier.size(38.dp).clip(RoundedCornerShape(11.dp)).background(Or.copy(alpha = .18f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.Lock, contentDescription = null, tint = Or, modifier = Modifier.size(19.dp))
                }
                Text(
                    stringResource(R.string.stats_lock),
                    color = Surface,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                )
            }

            Spacer(Modifier.height(12.dp))

            // 3 métriques
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SmallMetricCard(Modifier.weight(1f)) {
                    Text(
                        "%,d".format(snap.tokensTotal),
                        fontFamily = PlexMono,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Encre,
                    )
                    Text(stringResource(R.string.stat_tokens), fontSize = 10.sp, color = TexteMuet)
                }
                SmallMetricCard(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            snap.tokensPerSec.toInt().toString(),
                            fontFamily = PlexMono,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Encre,
                        )
                        Text(stringResource(R.string.rate_unit), fontFamily = PlexMono, fontSize = 10.sp, color = TexteMuet)
                    }
                    Text(stringResource(R.string.stat_debit_court), fontSize = 10.sp, color = TexteMuet)
                }
                SmallMetricCard(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        if (snap.requests > 0) {
                            Box(Modifier.size(7.dp).clip(CircleShape).background(Vert))
                        }
                        Text(
                            snap.requests.toString(),
                            fontFamily = PlexMono,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Encre,
                        )
                    }
                    Text(stringResource(R.string.stat_requetes_court), fontSize = 10.sp, color = TexteMuet)
                }
            }

            Spacer(Modifier.height(12.dp))

            // Histogramme — remplit l'espace restant
            HistogrammeCard(
                historique = historique,
                fonce = false,
                modifier = Modifier.weight(1f),
            )

            Spacer(Modifier.height(12.dp))

            // Puce modèle
            Row(
                Modifier
                    .align(Alignment.CenterHorizontally)
                    .clip(RoundedCornerShape(11.dp))
                    .background(BleuClair)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Box(Modifier.size(6.dp).clip(CircleShape).background(Or))
                Text(
                    stringResource(R.string.stat_modele_chip),
                    fontFamily = PlexMono,
                    fontSize = 11.sp,
                    color = Bleu,
                )
            }
        }
    }
}

// ─── Fold layout ──────────────────────────────────────────────────────────────

@Composable
private fun FoldStats(
    snap: fr.openllm.luciole.moniteur.MetricsSnapshot,
    historique: List<Int>,
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Fond)
            .padding(horizontal = 30.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        // Header
        Column {
            Text(
                stringResource(R.string.nav_moniteur).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = TexteFaible,
                letterSpacing = 1.2.sp,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.stats_titre),
                fontFamily = Spectral,
                fontWeight = FontWeight.SemiBold,
                fontSize = 27.sp,
                color = Encre,
            )
        }

        // 4 métriques en ligne
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            // tokens
            BigMetricCard(Modifier.weight(1f)) {
                Text(
                    "%,d".format(snap.tokensTotal),
                    fontFamily = PlexMono,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Encre,
                )
                Text(stringResource(R.string.stat_tokens), fontSize = 10.sp, color = TexteMuet)
            }
            // débit
            BigMetricCard(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        snap.tokensPerSec.toInt().toString(),
                        fontFamily = PlexMono,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Encre,
                    )
                    Text(stringResource(R.string.rate_unit), fontFamily = PlexMono, fontSize = 11.sp, color = TexteMuet)
                }
                Text(stringResource(R.string.stat_debit_court), fontSize = 10.sp, color = TexteMuet)
            }
            // requêtes
            BigMetricCard(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (snap.requests > 0) {
                        Box(Modifier.size(7.dp).clip(CircleShape).background(Vert))
                    }
                    Text(
                        snap.requests.toString(),
                        fontFamily = PlexMono,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Encre,
                    )
                }
                Text(stringResource(R.string.stat_requetes_court), fontSize = 10.sp, color = TexteMuet)
            }
            // modèle
            BigMetricCard(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.stat_modele_valeur),
                    fontFamily = PlexMono,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Medium,
                    color = Encre,
                )
                Text(stringResource(R.string.stat_modele_label), fontSize = 10.sp, color = TexteMuet)
            }
        }

        // Ligne inférieure : histogramme (gauche) + cards infos (droite)
        Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(18.dp)) {

            // ── Histogramme foncé ──────────────────────────────────────────
            Column(
                Modifier
                    .weight(1.5f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Encre)
                    .padding(22.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        stringResource(R.string.hist_caption),
                        color = Color(0xFFD4E0F5),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                    )
                    Text(
                        stringResource(R.string.rate_unit),
                        fontFamily = PlexMono,
                        fontSize = 11.sp,
                        color = Color(0xFF9FB0C8),
                    )
                }
                Spacer(Modifier.height(16.dp))
                BarresRow(
                    historique = historique,
                    couleurs = listOf(OrVif, Or),
                    modifier = Modifier.fillMaxWidth().weight(1f),
                )
            }

            // ── Colonne droite ─────────────────────────────────────────────
            Column(
                Modifier.weight(1f).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                // Carte confidentialité avec barre verte gauche
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Surface)
                        .border(1.dp, Bordure, RoundedCornerShape(16.dp)),
                ) {
                    // Barre accent gauche
                    Box(
                        Modifier
                            .width(4.dp)
                            .matchParentSize()
                            .background(Vert),
                    )
                    Row(
                        Modifier.fillMaxWidth().padding(start = 22.dp, top = 18.dp, end = 18.dp, bottom = 18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(
                            Modifier.size(38.dp).clip(RoundedCornerShape(11.dp)).background(VertClair),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Outlined.Lock, contentDescription = null, tint = Vert, modifier = Modifier.size(19.dp))
                        }
                        Text(
                            stringResource(R.string.stats_lock),
                            color = VertSombre,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                        )
                    }
                }

                // Carte warm-up
                Column(
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Surface)
                        .border(1.dp, Bordure, RoundedCornerShape(16.dp))
                        .padding(20.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(Icons.Outlined.Bolt, contentDescription = null, tint = Or, modifier = Modifier.size(18.dp))
                        Text(
                            stringResource(R.string.warm_titre),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Encre,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.warm_corps),
                        style = MaterialTheme.typography.bodySmall,
                        color = TexteMuet,
                        lineHeight = 18.sp,
                    )
                    Spacer(Modifier.weight(1f))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        // Premier démarrage
                        Column(
                            Modifier
                                .clip(RoundedCornerShape(11.dp))
                                .background(OrClair)
                                .padding(horizontal = 11.dp, vertical = 12.dp),
                        ) {
                            Text(
                                stringResource(R.string.warm_first_val),
                                fontFamily = PlexMono,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = OrTexte,
                            )
                            Text(
                                stringResource(R.string.warm_first_label),
                                fontSize = 10.5.sp,
                                color = OrTexte,
                            )
                        }
                        // Modèle en cache
                        Column(
                            Modifier
                                .clip(RoundedCornerShape(11.dp))
                                .background(VertClair)
                                .padding(horizontal = 11.dp, vertical = 12.dp),
                        ) {
                            Text(
                                stringResource(R.string.warm_cached_val),
                                fontFamily = PlexMono,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = VertSombre,
                            )
                            Text(
                                stringResource(R.string.warm_cached_label),
                                fontSize = 10.5.sp,
                                color = VertSombre,
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─── Composables partagés ─────────────────────────────────────────────────────

@Composable
private fun SmallMetricCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .clip(RoundedCornerShape(13.dp))
            .background(Surface)
            .border(1.dp, Bordure, RoundedCornerShape(13.dp))
            .padding(horizontal = 10.dp, vertical = 9.dp),
        content = content,
    )
}

@Composable
private fun BigMetricCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .clip(RoundedCornerShape(15.dp))
            .background(Surface)
            .border(1.dp, Bordure, RoundedCornerShape(15.dp))
            .padding(horizontal = 17.dp, vertical = 18.dp),
        content = content,
    )
}

/** Histogramme téléphone — fond Surface. */
@Composable
private fun HistogrammeCard(
    historique: List<Int>,
    fonce: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (fonce) Encre else Surface)
            .border(if (fonce) 0.dp else 1.dp, Bordure, RoundedCornerShape(16.dp))
            .padding(14.dp),
    ) {
        Text(
            stringResource(R.string.hist_caption),
            style = MaterialTheme.typography.bodySmall,
            color = if (fonce) Color(0xFFD4E0F5) else TexteMuet,
        )
        Spacer(Modifier.height(8.dp))
        BarresRow(
            historique = historique,
            couleurs = if (fonce) listOf(OrVif, Or) else listOf(BleuHisto, Bleu),
            modifier = Modifier.fillMaxWidth().weight(1f).heightIn(min = 96.dp),
        )
    }
}

/** Barres de l'histogramme. Les couleurs dégradent du haut vers le bas. */
@Composable
private fun BarresRow(
    historique: List<Int>,
    couleurs: List<Color>,
    modifier: Modifier = Modifier,
) {
    val maxVal = (historique.maxOrNull() ?: 1).coerceAtLeast(1)
    Row(
        modifier,
        horizontalArrangement = Arrangement.spacedBy(2.5.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        historique.forEach { v ->
            val fraction = (v.toFloat() / maxVal).coerceIn(0.02f, 1f)
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight(fraction)
                    .clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                    .background(Brush.verticalGradient(couleurs)),
            )
        }
    }
}
