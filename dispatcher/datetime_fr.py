"""Resolve a small, closed set of French relative time expressions.
The set is intentionally bounded. The system prompt instructs the model to emit only these
forms for `quand`; the GBNF grammar itself allows any string, so this resolver is the
enforcement point and raises ValueError on anything outside the set."""
import re
from datetime import datetime, timedelta

_WEEKDAYS = {
    "lundi": 0, "mardi": 1, "mercredi": 2, "jeudi": 3,
    "vendredi": 4, "samedi": 5, "dimanche": 6,
}

def _hhmm(s: str) -> tuple[int, int]:
    h, m = s.split(":")
    return int(h), int(m)

def resolve_datetime(expr: str, now: datetime) -> datetime:
    e = expr.strip().lower()

    m = re.fullmatch(r"dans (\d+) (minute|heure)s?", e)
    if m:
        n, unit = int(m.group(1)), m.group(2)
        return now + (timedelta(minutes=n) if unit == "minute" else timedelta(hours=n))

    m = re.fullmatch(r"(?:(aujourd'hui|demain|après-demain) )?(\d{1,2}:\d{2})", e)
    if m:
        day, hhmm = m.group(1), m.group(2)
        h, mi = _hhmm(hhmm)
        base = now.replace(hour=h, minute=mi, second=0, microsecond=0)
        if day == "demain":
            base += timedelta(days=1)
        elif day == "après-demain":
            base += timedelta(days=2)
        return base

    m = re.fullmatch(r"(lundi|mardi|mercredi|jeudi|vendredi|samedi|dimanche) (\d{1,2}:\d{2})", e)
    if m:
        target = _WEEKDAYS[m.group(1)]
        h, mi = _hhmm(m.group(2))
        delta = (target - now.weekday()) % 7
        delta = 7 if delta == 0 else delta  # always a future occurrence
        return (now + timedelta(days=delta)).replace(hour=h, minute=mi, second=0, microsecond=0)

    raise ValueError(f"expression temporelle non supportée: {expr!r}")
