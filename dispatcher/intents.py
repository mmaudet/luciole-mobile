"""Map a validated action dict to an `am start` argv, and pull the phone number from the
user's phrase (or resolve a contact name). Pure: no side effects."""
import re, unicodedata
from datetime import datetime, timedelta
from urllib.parse import quote
from datetime_fr import resolve_datetime

_PHONE_RE = re.compile(r"\+?\d[\d \-. ]{4,}\d")

# Jeu de départ — figé/complété on-device (cf. plan Task 8). Lancement par package
# (sans connaître l'activité) via `monkey`. Réglages via android.settings.*.
_APP_PKG = {
    "youtube": "com.google.android.youtube",
    "maps": "com.google.android.apps.maps",
    "chrome": "com.android.chrome",
    "appareil_photo": "com.google.android.GoogleCamera",
}
_SETTINGS = {
    "parametres": "android.settings.SETTINGS",
    "bluetooth": "android.settings.BLUETOOTH_SETTINGS",
    "wifi": "android.settings.WIFI_SETTINGS",
}

MESSAGE_INCONNU = (
    "Ça, je ne sais pas encore le faire. Je peux : mettre une alarme ou un minuteur, "
    "créer un événement, écrire un SMS ou un e-mail, lancer un itinéraire, appeler un "
    "numéro, ouvrir une app ou un réglage, faire une recherche, ou traduire."
)

def display_text(action: dict) -> str | None:
    """Texte à afficher pour les actions display-only (pas d'`am start`). Sinon None."""
    t = action.get("type")
    if t == "inconnu":
        return MESSAGE_INCONNU
    if t == "traduction":
        return action.get("resultat") or None
    return None

def extract_phone(text: str) -> str | None:
    """Most digit-rich phone-like run in `text`, as digits (keeping a leading +), else None.
    A 1B model is unreliable at copying a 10-digit number, so for "appel" we take the number
    straight from the user's phrase instead of trusting the model's `destinataire`."""
    best = ""
    for m in _PHONE_RE.findall(text):
        if len(re.sub(r"\D", "", m)) > len(re.sub(r"\D", "", best)):
            best = m
    digits = "".join(c for c in best if c.isdigit() or c == "+")
    return digits if len(re.sub(r"\D", "", digits)) >= 6 else None

def _norm_name(s: str) -> str:
    """Lowercase, strip accents, turn punctuation into spaces, collapse whitespace."""
    s = unicodedata.normalize("NFKD", s or "")
    s = "".join(c for c in s if not unicodedata.combining(c))
    s = "".join(c if c.isalnum() else " " for c in s.lower())
    return " ".join(s.split())

def match_contact(query: str, contacts: list) -> str | None:
    """Best phone number (digits, keeping a leading +) for a spoken name among a list of
    {"name", "number"} dicts, or None if no confident match. Pure: no I/O.
    For "appelle <nom>", the 1B classifies the intent + name; we resolve the number here."""
    q = _norm_name(query)
    if not q:
        return None
    qtok = set(q.split())
    best, best_score = None, 0.0
    for c in contacts:
        name = _norm_name(c.get("name", ""))
        number = c.get("number") or ""
        if not name or not number:
            continue
        ntok = set(name.split())
        if name == q:
            score = 1.0
        elif qtok and qtok <= ntok:           # every spoken token is in the contact name
            score = 0.9
        elif qtok & ntok:                      # partial token overlap
            score = 0.7 * len(qtok & ntok) / len(qtok)
        else:
            score = 0.0
        if score > best_score:
            best, best_score = number, score
    if best is None or best_score < 0.5:       # too weak -> don't dial a wrong contact
        return None
    return "".join(ch for ch in best if ch.isdigit() or ch == "+")

_CALL_PREFIX = re.compile(
    r"^\s*(?:appelle|appeler|appelles|appelez|t[ée]l[ée]phone[rz]?|compose[rz]?|joins)"
    r"\s*(?:à|au|aux|le|la|l['’])?\s*", re.IGNORECASE)

def call_target(phrase: str) -> str:
    """The contact name to dial: the phrase minus a leading call verb (no number spoken).
    'appelle Paul Maudet' -> 'Paul Maudet'. Taken from the PHRASE, not the 1B's output."""
    return _CALL_PREFIX.sub("", phrase or "", count=1).strip(" .,!?;:'\"«»")

def _epoch_ms(dt: datetime) -> str:
    return str(int(dt.timestamp() * 1000))

def build_intent(action: dict, now: datetime) -> list[str]:
    t = action.get("type")

    if t == "alarme":
        h, m = action["heure"].split(":")
        return ["am", "start", "-a", "android.intent.action.SET_ALARM",
                "--ei", "android.intent.extra.alarm.HOUR", str(int(h)),
                "--ei", "android.intent.extra.alarm.MINUTES", str(int(m)),
                "--es", "android.intent.extra.alarm.MESSAGE", action["libelle"],
                "--ez", "android.intent.extra.alarm.SKIP_UI", "false"]

    if t == "agenda":
        start = resolve_datetime(action["quand"], now)
        end = start + timedelta(minutes=action.get("duree_min", 60))
        argv = ["am", "start", "-a", "android.intent.action.INSERT",
                "-t", "vnd.android.cursor.item/event",
                "--es", "title", action["titre"],
                "--el", "beginTime", _epoch_ms(start),
                "--el", "endTime", _epoch_ms(end)]
        if action.get("lieu"):
            argv += ["--es", "eventLocation", action["lieu"]]
        return argv

    if t == "message":
        if action["canal"] == "email":
            return ["am", "start", "-a", "android.intent.action.SENDTO", "-d", "mailto:",
                    "--es", "android.intent.extra.SUBJECT", action.get("objet", ""),
                    "--es", "android.intent.extra.TEXT", action["corps"]]
        return ["am", "start", "-a", "android.intent.action.SENDTO", "-d", "smsto:",
                "--es", "sms_body", action["corps"]]

    if t == "itineraire":
        return ["am", "start", "-a", "android.intent.action.VIEW",
                "-d", f"geo:0,0?q={quote(action['destination'])}"]

    if t == "appel":
        # DIAL (not CALL): opens the dialer pre-filled, never auto-dials.
        num = "".join(c for c in action["destinataire"] if c.isdigit() or c == "+")
        return ["am", "start", "-a", "android.intent.action.DIAL",
                "-d", f"tel:{num}"]

    if t == "minuteur":
        secs = int(action["duree_min"]) * 60
        return ["am", "start", "-a", "android.intent.action.SET_TIMER",
                "--ei", "android.intent.extra.alarm.LENGTH", str(secs),
                "--es", "android.intent.extra.alarm.MESSAGE", action.get("libelle", ""),
                "--ez", "android.intent.extra.alarm.SKIP_UI", "true"]

    if t == "note":
        return ["am", "start", "-a", "android.intent.action.SEND",
                "-t", "text/plain",
                "--es", "android.intent.extra.TEXT", action["texte"]]

    if t == "recherche":
        return ["am", "start", "-a", "android.intent.action.WEB_SEARCH",
                "--es", "query", action["requete"]]

    if t == "ouvrir":
        c = action["cible"]
        if c in _SETTINGS:
            return ["am", "start", "-a", _SETTINGS[c]]
        if c in _APP_PKG:
            return ["monkey", "-p", _APP_PKG[c], "-c", "android.intent.category.LAUNCHER", "1"]
        raise ValueError(f"cible inconnue: {c!r}")

    raise ValueError(f"type d'action inconnu: {t!r}")
