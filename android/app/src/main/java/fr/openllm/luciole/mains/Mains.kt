package fr.openllm.luciole.mains

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.AlarmClock
import android.provider.CalendarContract
import fr.openllm.luciole.R
import fr.openllm.luciole.model.*

data class IntentSpec(val action: String, val data: String? = null, val extras: Map<String, Any> = emptyMap())
enum class AffichageType { NOTE, TRADUCTION, INCONNU }
sealed interface Sortie {
    data class Lancer(val spec: IntentSpec, val libelle: String) : Sortie
    data class Afficher(val type: AffichageType, val texte: String) : Sortie
    data class ContactIntrouvable(val nom: String) : Sortie
}

object Mains {
    fun traiter(
        action: Action,
        phrase: String,
        now: java.time.LocalDateTime = java.time.LocalDateTime.now(),
        resoudreContact: (String) -> String?,
    ): Sortie = when (action) {
        is Action.Appel -> {
            val target = Extraction.callTarget(phrase)
            val num = Extraction.extractPhone(phrase) ?: resoudreContact(target)
            if (num == null) Sortie.ContactIntrouvable(target)
            else Sortie.Lancer(IntentSpec(Intent.ACTION_DIAL, "tel:$num"), "act_appel")
        }
        is Action.Alarme -> {
            val parts = action.heure.split(":")
            val h = parts.getOrNull(0)?.toIntOrNull() ?: 0
            val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
            Sortie.Lancer(IntentSpec(AlarmClock.ACTION_SET_ALARM, extras = mapOf(
                AlarmClock.EXTRA_HOUR to h,
                AlarmClock.EXTRA_MINUTES to m,
                AlarmClock.EXTRA_MESSAGE to action.libelle)), "act_alarme")
        }
        is Action.Minuteur -> Sortie.Lancer(IntentSpec(AlarmClock.ACTION_SET_TIMER, extras = mapOf(
            AlarmClock.EXTRA_LENGTH to action.dureeMin * 60,
            AlarmClock.EXTRA_MESSAGE to (action.libelle ?: ""))), "act_minuteur")
        is Action.Agenda -> Sortie.Lancer(IntentSpec(Intent.ACTION_INSERT,
            "content://com.android.calendar/events",
            extras = buildMap {
                put(CalendarContract.Events.TITLE, action.titre)
                action.lieu?.let { put(CalendarContract.Events.EVENT_LOCATION, it) }
                val epochMs = DatetimeFr.resolveEpochMs(action.quand, now)
                if (epochMs != null) {
                    put(CalendarContract.EXTRA_EVENT_BEGIN_TIME, epochMs)
                    put(CalendarContract.EXTRA_EVENT_END_TIME, epochMs + 3_600_000L)
                }
            }), "act_agenda")
        is Action.Message -> when (action.canal) {
            Canal.EMAIL -> Sortie.Lancer(IntentSpec(Intent.ACTION_SENDTO, "mailto:",
                extras = buildMap {
                    action.objet?.let { put(Intent.EXTRA_SUBJECT, it) }
                    put(Intent.EXTRA_TEXT, action.corps)
                }), "act_message")
            Canal.SMS -> Sortie.Lancer(IntentSpec(Intent.ACTION_SENDTO, "smsto:",
                extras = mapOf("sms_body" to action.corps)), "act_message")
        }
        is Action.Itineraire -> Sortie.Lancer(IntentSpec(Intent.ACTION_VIEW,
            "geo:0,0?q=" + Uri.encode(action.destination)), "act_itineraire")
        is Action.Recherche -> Sortie.Lancer(IntentSpec(Intent.ACTION_VIEW,
            "https://www.qwant.com/?q=" + Uri.encode(action.requete)), "act_recherche")
        is Action.Ouvrir -> Sortie.Lancer(ouvrir(action.cible), "act_ouvrir")
        is Action.Note -> Sortie.Afficher(AffichageType.NOTE, action.texte)
        is Action.Traduction -> Sortie.Afficher(AffichageType.TRADUCTION, action.resultat)
        Action.Inconnu -> Sortie.Afficher(AffichageType.INCONNU, "")
    }

    private fun ouvrir(cible: Cible): IntentSpec = when (cible) {
        Cible.YOUTUBE -> IntentSpec(Intent.ACTION_VIEW, "https://www.youtube.com")
        Cible.MAPS -> IntentSpec(Intent.ACTION_VIEW, "geo:0,0")
        Cible.CHROME -> IntentSpec(Intent.ACTION_VIEW, "https://www.google.com")
        Cible.APPAREIL_PHOTO -> IntentSpec("android.media.action.IMAGE_CAPTURE")
        Cible.PARAMETRES -> IntentSpec("android.settings.SETTINGS")
        Cible.BLUETOOTH -> IntentSpec("android.settings.BLUETOOTH_SETTINGS")
        Cible.WIFI -> IntentSpec("android.settings.WIFI_SETTINGS")
    }

    fun Context.lancer(spec: IntentSpec) {
        val intent = Intent(spec.action).apply {
            spec.data?.let { data = Uri.parse(it) }
            spec.extras.forEach { (k, v) ->
                when (v) {
                    is Int    -> putExtra(k, v)
                    is Long   -> putExtra(k, v)
                    is String -> putExtra(k, v)
                    else -> android.util.Log.w("Luciole", "extra ignoré: $k=$v")
                }
            }
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            startActivity(intent)
        } catch (e: android.content.ActivityNotFoundException) {
            android.widget.Toast.makeText(this, R.string.aucune_app, android.widget.Toast.LENGTH_SHORT).show()
        } catch (e: SecurityException) {
            android.widget.Toast.makeText(this, R.string.aucune_app, android.widget.Toast.LENGTH_SHORT).show()
        }
    }
}
