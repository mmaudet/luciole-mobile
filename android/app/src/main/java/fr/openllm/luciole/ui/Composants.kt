package fr.openllm.luciole.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import fr.openllm.luciole.R
import fr.openllm.luciole.ui.theme.Or
import fr.openllm.luciole.ui.theme.OrPale
import fr.openllm.luciole.ui.theme.Vert
import fr.openllm.luciole.ui.theme.VertClair

/** Le halo « luciole » : un dégradé radial or, qui respire doucement. */
@Composable
fun LucioleLogo(size: Dp, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "luciole")
    val pulse by transition.animateFloat(
        initialValue = 0.82f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse",
    )
    Box(
        modifier
            .size(size)
            .graphicsLayer { alpha = pulse }
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colorStops = arrayOf(0f to OrPale, 0.47f to Or, 0.74f to Color.Transparent),
                )
            )
    )
}

/** Puce verte « Hors-ligne » avec icône wifi barré. */
@Composable
fun OfflinePill(modifier: Modifier = Modifier) {
    Row(
        modifier
            .clip(RoundedCornerShape(20.dp))
            .background(VertClair)
            .padding(horizontal = 11.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(Icons.Outlined.WifiOff, contentDescription = null, tint = Vert, modifier = Modifier.size(13.dp))
        Text(
            stringResource(R.string.offline),
            color = Vert,
            fontWeight = FontWeight.Bold,
            style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
        )
    }
}

/** Icône Material correspondant à un type d'action (clé = libellé interne « act_xxx » ou nom court). */
fun actionIcon(key: String): ImageVector = when {
    key.contains("appel") -> Icons.Outlined.Call
    key.contains("alarme") -> Icons.Outlined.Alarm
    key.contains("minuteur") -> Icons.Outlined.Timer
    key.contains("agenda") -> Icons.Outlined.CalendarMonth
    key.contains("message") -> Icons.Outlined.MailOutline
    key.contains("itineraire") -> Icons.Outlined.Place
    key.contains("recherche") -> Icons.Outlined.Search
    key.contains("ouvrir") -> Icons.Outlined.GridView
    key.contains("traduction") || key.contains("traduire") -> Icons.Outlined.Translate
    key.contains("note") -> Icons.Outlined.EditNote
    else -> Icons.Outlined.AutoAwesome
}
