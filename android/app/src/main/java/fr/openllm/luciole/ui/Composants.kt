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
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
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

/** Vrai état de connectivité réseau, observé en direct via un callback réseau. */
@Composable
fun rememberEstConnecte(): Boolean {
    val context = LocalContext.current
    val cm = remember(context) { context.getSystemService(ConnectivityManager::class.java) }
    var connecte by remember { mutableStateOf(estConnecteMaintenant(cm)) }
    DisposableEffect(cm) {
        if (cm == null) return@DisposableEffect onDispose { }
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { connecte = estConnecteMaintenant(cm) }
            override fun onLost(network: Network) { connecte = estConnecteMaintenant(cm) }
            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                connecte = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            }
        }
        cm.registerDefaultNetworkCallback(callback)
        onDispose { cm.unregisterNetworkCallback(callback) }
    }
    return connecte
}

private fun estConnecteMaintenant(cm: ConnectivityManager?): Boolean {
    val net = cm?.activeNetwork ?: return false
    val caps = cm.getNetworkCapabilities(net) ?: return false
    return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
        caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}

/** Puce d'état réseau réelle : « En ligne » (wifi) ou « Hors-ligne » (wifi barré). */
@Composable
fun ConnectivitePill(modifier: Modifier = Modifier) {
    val connecte = rememberEstConnecte()
    Row(
        modifier
            .clip(RoundedCornerShape(20.dp))
            .background(VertClair)
            .padding(horizontal = 11.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            if (connecte) Icons.Outlined.Wifi else Icons.Outlined.WifiOff,
            contentDescription = null, tint = Vert, modifier = Modifier.size(13.dp),
        )
        Text(
            stringResource(if (connecte) R.string.en_ligne else R.string.offline),
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
