package fr.openllm.luciole.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.openllm.luciole.R
import fr.openllm.luciole.moniteur.MoniteurViewModel

@Composable
fun MoniteurScreen(vm: MoniteurViewModel) {
    val s by vm.state.collectAsState()
    val histo by vm.historique.collectAsState()
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.moniteur_titre), fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Text(
            stringResource(R.string.moniteur_modele),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Stat(stringResource(R.string.stat_tokens), s.tokensTotal.toString(), Modifier.fillMaxWidth())
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Stat(stringResource(R.string.stat_debit), "%.0f".format(s.tokensPerSec), Modifier.weight(1f))
            Stat(stringResource(R.string.stat_requetes), s.requests.toString(), Modifier.weight(1f))
        }
        Text(stringResource(R.string.aucune_donnee))
        Text(stringResource(R.string.histo_titre), fontSize = 12.sp)
        Histogramme(histo, Modifier.fillMaxWidth().height(160.dp))
    }
}

@Composable
private fun Histogramme(histo: List<Int>, modifier: Modifier = Modifier) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val onSurface = MaterialTheme.colorScheme.onSurface
    val axisColor = onSurface.copy(alpha = 0.3f)
    val labelColor = onSurface.copy(alpha = 0.7f)
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = TextStyle(fontSize = 9.sp, color = labelColor)

    Canvas(modifier = modifier) {
        val leftMargin = 40.dp.toPx()
        val bottomMargin = 20.dp.toPx()
        val chartWidth = size.width - leftMargin
        val chartHeight = size.height - bottomMargin

        val maxV = histo.maxOrNull()?.coerceAtLeast(1) ?: 1

        // Y axis
        drawLine(
            color = axisColor,
            start = Offset(leftMargin, 0f),
            end = Offset(leftMargin, chartHeight),
            strokeWidth = 1.dp.toPx()
        )

        // X axis
        drawLine(
            color = axisColor,
            start = Offset(leftMargin, chartHeight),
            end = Offset(size.width, chartHeight),
            strokeWidth = 1.dp.toPx()
        )

        // Y labels: maxV at top, maxV/2 at middle, 0 at bottom
        val maxLabel  = textMeasurer.measure("$maxV",      labelStyle)
        val midLabel  = textMeasurer.measure("${maxV / 2}", labelStyle)
        val zeroLabel = textMeasurer.measure("0",           labelStyle)

        val gap = 2.dp.toPx()

        drawText(maxLabel,  topLeft = Offset(leftMargin - maxLabel.size.width - gap, 0f))
        drawText(zeroLabel, topLeft = Offset(leftMargin - zeroLabel.size.width - gap,
            chartHeight - zeroLabel.size.height))

        val midY = chartHeight / 2f
        drawText(midLabel, topLeft = Offset(leftMargin - midLabel.size.width - gap,
            midY - midLabel.size.height / 2f))
        // Faint mid gridline
        drawLine(
            color = axisColor.copy(alpha = axisColor.alpha * 0.5f),
            start = Offset(leftMargin, midY),
            end = Offset(size.width, midY),
            strokeWidth = 0.5.dp.toPx()
        )

        // X labels: −60 s at left edge, 0 s at right edge
        val xLeftLabel  = textMeasurer.measure("−60 s", labelStyle)
        val xRightLabel = textMeasurer.measure("0 s",   labelStyle)
        drawText(xLeftLabel,  topLeft = Offset(leftMargin, chartHeight + gap))
        drawText(xRightLabel, topLeft = Offset(size.width - xRightLabel.size.width, chartHeight + gap))

        // Bars
        if (histo.isNotEmpty()) {
            val barWidth = chartWidth / histo.size
            val barGap = 2.dp.toPx()
            histo.forEachIndexed { i, value ->
                val barH = (value.toFloat() / maxV) * chartHeight
                if (barH > 0f) {
                    drawRect(
                        color = primaryColor,
                        topLeft = Offset(leftMargin + i * barWidth + barGap / 2f, chartHeight - barH),
                        size = Size((barWidth - barGap).coerceAtLeast(1f), barH)
                    )
                }
            }
        }
    }
}

@Composable
private fun Stat(label: String, valeur: String, modifier: Modifier = Modifier) {
    Card(modifier) {
        Column(Modifier.padding(14.dp)) {
            Text(label, fontSize = 12.sp)
            Text(valeur, fontWeight = FontWeight.Bold, fontSize = 26.sp)
        }
    }
}
