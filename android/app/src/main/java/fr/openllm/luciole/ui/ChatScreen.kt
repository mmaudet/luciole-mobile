package fr.openllm.luciole.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.openllm.luciole.R
import fr.openllm.luciole.mains.AffichageType
import fr.openllm.luciole.mains.IntentSpec
import fr.openllm.luciole.mains.Sortie
import fr.openllm.luciole.ui.theme.BleuClair
import fr.openllm.luciole.ui.theme.Bordure
import fr.openllm.luciole.ui.theme.Encre
import fr.openllm.luciole.ui.theme.Fond
import fr.openllm.luciole.ui.theme.FondChamp
import fr.openllm.luciole.ui.theme.PlexMono
import fr.openllm.luciole.ui.theme.Spectral
import fr.openllm.luciole.ui.theme.Surface
import fr.openllm.luciole.ui.theme.TexteFaible
import fr.openllm.luciole.ui.theme.TexteMuet
import fr.openllm.luciole.ui.theme.Vert

@Composable
fun ChatPane(
    expanded: Boolean,
    messages: List<Message>,
    enCours: Boolean,
    saisie: TextFieldValue,
    onSaisieChange: (TextFieldValue) -> Unit,
    focusRequester: FocusRequester,
    onEnvoyer: (String) -> Unit,
    onRelancer: (IntentSpec) -> Unit,
    onEffacer: () -> Unit,
    onOuvrirAide: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val keyboard = LocalSoftwareKeyboardController.current
    val focus = LocalFocusManager.current
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    Column(modifier.fillMaxSize().background(Fond)) {
        // En-tête
        Row(
            Modifier.fillMaxWidth().background(Surface).border(0.dp, Color.Transparent)
                .padding(horizontal = if (expanded) 30.dp else 18.dp, vertical = if (expanded) 18.dp else 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (expanded) {
                Text(stringResource(R.string.chat_titre), style = MaterialTheme.typography.titleLarge, color = Encre)
            } else {
                LucioleLogo(24.dp)
                Spacer(Modifier.size(10.dp))
                Text("Luciole", fontFamily = Spectral, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = Encre)
            }
            Spacer(Modifier.weight(1f))
            if (expanded) {
                Row(
                    Modifier.clip(RoundedCornerShape(8.dp)).clickable { onEffacer() }.padding(6.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(Icons.Outlined.DeleteOutline, null, tint = TexteFaible, modifier = Modifier.size(16.dp))
                    Text(stringResource(R.string.chat_effacer), color = TexteFaible, fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp)
                }
            } else {
                OfflinePill()
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(Bordure))

        // Fil de conversation
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = if (expanded) 34.dp else 16.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            items(messages) { m -> MessageItem(m, expanded, onRelancer) }
        }
        if (enCours) LinearProgressIndicator(Modifier.fillMaxWidth())

        // Barre de saisie
        Row(
            Modifier.fillMaxWidth().background(Surface).padding(horizontal = 13.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            if (onOuvrirAide != null) {
                Box(
                    Modifier.size(42.dp).clip(CircleShape).background(Surface).border(1.dp, Bordure, CircleShape)
                        .clickable { onOuvrirAide() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.AutoMirrored.Outlined.HelpOutline, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                }
            }
            TextField(
                value = saisie,
                onValueChange = onSaisieChange,
                modifier = Modifier.weight(1f).focusRequester(focusRequester),
                placeholder = { Text(stringResource(R.string.placeholder_saisie), color = TexteFaible) },
                shape = RoundedCornerShape(24.dp),
                maxLines = 4,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = FondChamp,
                    unfocusedContainerColor = FondChamp,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                ),
            )
            Box(
                Modifier.size(46.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary)
                    .clickable(enabled = !enCours) {
                        onEnvoyer(saisie.text)
                        onSaisieChange(TextFieldValue(""))
                        keyboard?.hide()
                        focus.clearFocus()
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.AutoMirrored.Outlined.Send, null, tint = Surface, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun MessageItem(m: Message, expanded: Boolean, onRelancer: (IntentSpec) -> Unit) {
    val maxBubble = if (expanded) 540.dp else 320.dp
    if (m.role == Role.USER) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Box(
                Modifier.widthIn(max = maxBubble)
                    .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomEnd = 6.dp, bottomStart = 18.dp))
                    .background(MaterialTheme.colorScheme.primary).padding(horizontal = 15.dp, vertical = 12.dp),
            ) {
                Text(m.texte, color = Surface, fontSize = 14.5.sp)
            }
        }
        return
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
        Box(Modifier.widthIn(max = maxBubble)) { LucioleCarte(m, onRelancer) }
    }
}

@Composable
private fun LucioleCarte(m: Message, onRelancer: (IntentSpec) -> Unit) {
    val forme = RoundedCornerShape(topStart = 6.dp, topEnd = 18.dp, bottomEnd = 18.dp, bottomStart = 18.dp)
    val base = Modifier.fillMaxWidth().clip(forme).background(Surface).border(1.dp, Bordure, forme).padding(15.dp)

    when (val s = m.sortie) {
        is Sortie.Lancer -> {
            val label = stringResource(libelleRes(s.libelle)).substringAfter(' ', stringResource(libelleRes(s.libelle)))
            Column(base) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    IconeChip(actionKey = s.libelle)
                    Text(label, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = Encre, modifier = Modifier.weight(1f))
                    Box(
                        Modifier.clip(RoundedCornerShape(11.dp)).background(MaterialTheme.colorScheme.primary)
                            .clickable { onRelancer(s.spec) }.padding(horizontal = 18.dp, vertical = 11.dp),
                    ) {
                        Text(stringResource(R.string.bouton_ouvrir), color = Surface, fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp)
                    }
                }
                if (m.dureeMs != null) ProcNote(m.dureeMs)
            }
        }
        is Sortie.Afficher -> {
            val texte = when (s.type) {
                AffichageType.NOTE -> stringResource(R.string.note_prefixe, s.texte)
                AffichageType.TRADUCTION -> s.texte
                AffichageType.INCONNU -> stringResource(R.string.inconnu)
            }
            Row(base, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                IconeChip(actionKey = if (s.type == AffichageType.NOTE) "note" else "traduction")
                Text(texte, fontSize = 15.sp, color = Encre, modifier = Modifier.weight(1f))
            }
        }
        is Sortie.ContactIntrouvable -> InfoCarte(forme, stringResource(R.string.contact_introuvable, s.nom))
        null -> InfoCarte(forme, if (m.texte == "serveur_indisponible") stringResource(R.string.serveur_injoignable) else m.texte)
    }
}

@Composable
private fun InfoCarte(forme: RoundedCornerShape, texte: String) {
    Row(
        Modifier.fillMaxWidth().clip(forme).background(Surface).border(1.dp, Bordure, forme).padding(15.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(Icons.Outlined.ErrorOutline, null, tint = TexteMuet, modifier = Modifier.size(22.dp))
        Text(texte, fontSize = 15.sp, color = Encre, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun IconeChip(actionKey: String) {
    Box(
        Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(BleuClair),
        contentAlignment = Alignment.Center,
    ) {
        Icon(actionIcon(actionKey), null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun ProcNote(dureeMs: Long) {
    Row(
        Modifier.padding(top = 11.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(Vert))
        Text(
            stringResource(R.string.duree_traitement, "%.1f".format(dureeMs / 1000.0)),
            color = TexteFaible, fontFamily = PlexMono, fontSize = 11.sp,
        )
    }
}

private fun libelleRes(libelle: String): Int = when (libelle) {
    "act_appel" -> R.string.act_appel
    "act_alarme" -> R.string.act_alarme
    "act_minuteur" -> R.string.act_minuteur
    "act_agenda" -> R.string.act_agenda
    "act_message" -> R.string.act_message
    "act_itineraire" -> R.string.act_itineraire
    "act_recherche" -> R.string.act_recherche
    "act_ouvrir" -> R.string.act_ouvrir
    else -> R.string.act_ouvrir
}
