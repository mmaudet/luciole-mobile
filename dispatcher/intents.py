"""Map a validated action dict to an `am start` argv. Pure: no side effects."""
from datetime import datetime, timedelta
from urllib.parse import quote
from datetime_fr import resolve_datetime

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

    raise ValueError(f"type d'action inconnu: {t!r}")
