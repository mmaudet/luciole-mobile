package fr.openllm.luciole.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.openllm.luciole.R
import fr.openllm.luciole.ui.theme.BleuClair
import fr.openllm.luciole.ui.theme.Bordure
import fr.openllm.luciole.ui.theme.Encre
import fr.openllm.luciole.ui.theme.Fond
import fr.openllm.luciole.ui.theme.Or
import fr.openllm.luciole.ui.theme.OrClair
import fr.openllm.luciole.ui.theme.PlexMono
import fr.openllm.luciole.ui.theme.Spectral
import fr.openllm.luciole.ui.theme.Surface
import fr.openllm.luciole.ui.theme.TexteFaible
import fr.openllm.luciole.ui.theme.TexteMuet

private data class Atout(val icone: ImageVector, val titreRes: Int, val descRes: Int, val orange: Boolean = false)

private val ATOUTS = listOf(
    Atout(Icons.Outlined.Lock, R.string.ob_p1_titre, R.string.ob_p1_desc),
    Atout(Icons.Outlined.Shield, R.string.ob_p2_titre, R.string.ob_p2_desc),
    Atout(Icons.Outlined.Bolt, R.string.ob_p3_titre, R.string.ob_p3_desc, orange = true),
)

@Composable
fun OnboardingScreen(
    expanded: Boolean,
    langue: String,
    onSetLangue: (String) -> Unit,
    onCommencer: () -> Unit,
) {
    Box(Modifier.fillMaxSize().background(Fond)) {
        if (expanded) FoldOnboarding(langue, onSetLangue, onCommencer)
        else PhoneOnboarding(langue, onSetLangue, onCommencer)
    }
}

@Composable
private fun EnTete(grand: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        LucioleLogo(if (grand) 42.dp else 34.dp)
        Spacer(Modifier.width(12.dp))
        Text("Luciole", fontFamily = Spectral, fontWeight = FontWeight.SemiBold, fontSize = if (grand) 30.sp else 23.sp, color = Encre)
        Spacer(Modifier.width(10.dp))
        Text(
            stringResource(R.string.demo).uppercase(),
            color = Or, fontWeight = FontWeight.Bold, fontSize = 10.sp, letterSpacing = 1.4.sp,
        )
    }
}

@Composable
private fun LangSwitch(langue: String, onSetLangue: (String) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(R.string.ob_lang), color = TexteMuet, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.width(10.dp))
        Row(
            Modifier.clip(RoundedCornerShape(10.dp)).background(Surface).border(1.dp, Bordure, RoundedCornerShape(10.dp)).padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            for (code in listOf("fr", "en")) {
                val on = langue == code
                Box(
                    Modifier.clip(RoundedCornerShape(7.dp)).background(if (on) Encre else Color.Transparent)
                        .clickable { onSetLangue(code) }.padding(horizontal = 13.dp, vertical = 6.dp),
                ) {
                    Text(code.uppercase(), color = if (on) Surface else TexteMuet, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun CommencerButton(onCommencer: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onCommencer,
        modifier = modifier.height(54.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
    ) {
        Text(stringResource(R.string.ob_cta), fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        Spacer(Modifier.width(9.dp))
        Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, Modifier.size(20.dp))
    }
}

@Composable
private fun AtoutRow(a: Atout) {
    Row(horizontalArrangement = Arrangement.spacedBy(13.dp), verticalAlignment = Alignment.Top) {
        Box(
            Modifier.size(38.dp).clip(RoundedCornerShape(11.dp)).background(if (a.orange) OrClair else BleuClair),
            contentAlignment = Alignment.Center,
        ) {
            Icon(a.icone, null, tint = if (a.orange) Or else MaterialTheme.colorScheme.primary, modifier = Modifier.size(19.dp))
        }
        Column {
            Text(stringResource(a.titreRes), fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Encre)
            Text(stringResource(a.descRes), color = TexteMuet, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun PhoneOnboarding(langue: String, onSetLangue: (String) -> Unit, onCommencer: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 26.dp, vertical = 18.dp)) {
        EnTete(grand = false)
        Box(Modifier.fillMaxWidth().height(1.dp).padding(top = 0.dp).background(Bordure).padding(top = 16.dp))
        Spacer(Modifier.height(22.dp))
        Text(stringResource(R.string.ob_titre), fontFamily = Spectral, fontWeight = FontWeight.SemiBold, fontSize = 30.sp, lineHeight = 35.sp, color = Encre)
        Spacer(Modifier.height(14.dp))
        Text(stringResource(R.string.tagline), color = TexteMuet, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(26.dp))
        Column(verticalArrangement = Arrangement.spacedBy(15.dp)) { ATOUTS.forEach { AtoutRow(it) } }
        Spacer(Modifier.weight(1f))
        LangSwitch(langue, onSetLangue)
        Spacer(Modifier.height(14.dp))
        CommencerButton(onCommencer, Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.ob_foot), color = TexteFaible, fontFamily = PlexMono, fontSize = 10.5.sp, modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

@Composable
private fun FoldOnboarding(langue: String, onSetLangue: (String) -> Unit, onCommencer: () -> Unit) {
    Row(
        Modifier.fillMaxSize().padding(horizontal = 64.dp, vertical = 40.dp),
        horizontalArrangement = Arrangement.spacedBy(64.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1.05f)) {
            EnTete(grand = true)
            Spacer(Modifier.height(34.dp))
            Text(stringResource(R.string.ob_titre), fontFamily = Spectral, fontWeight = FontWeight.SemiBold, fontSize = 46.sp, lineHeight = 52.sp, color = Encre)
            Spacer(Modifier.height(20.dp))
            Text(stringResource(R.string.tagline), color = TexteMuet, fontSize = 17.sp, modifier = Modifier.widthIn(max = 420.dp))
            Spacer(Modifier.height(36.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                CommencerButton(onCommencer)
                LangSwitch(langue, onSetLangue)
            }
            Spacer(Modifier.height(26.dp))
            Text(stringResource(R.string.ob_foot), color = TexteFaible, fontFamily = PlexMono, fontSize = 12.sp)
        }
        Column(Modifier.weight(0.95f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Encre).padding(20.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Box(Modifier.size(48.dp).clip(RoundedCornerShape(13.dp)).background(Or.copy(alpha = .18f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Lock, null, tint = Or, modifier = Modifier.size(24.dp))
                }
                Text(stringResource(R.string.stats_lock), color = Surface, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            }
            ATOUTS.forEach { a ->
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Surface).border(1.dp, Bordure, RoundedCornerShape(16.dp)).padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Box(Modifier.size(48.dp).clip(RoundedCornerShape(13.dp)).background(if (a.orange) OrClair else BleuClair), contentAlignment = Alignment.Center) {
                        Icon(a.icone, null, tint = if (a.orange) Or else MaterialTheme.colorScheme.primary, modifier = Modifier.size(23.dp))
                    }
                    Column {
                        Text(stringResource(a.titreRes), fontWeight = FontWeight.SemiBold, fontSize = 17.sp, color = Encre)
                        Text(stringResource(a.descRes), color = TexteMuet, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}
