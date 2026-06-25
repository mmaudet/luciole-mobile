from datetime import datetime
import sys, pathlib
sys.path.insert(0, str(pathlib.Path(__file__).parents[1]))
from intents import build_intent

NOW = datetime(2026, 6, 25, 9, 0)

def test_alarme():
    argv = build_intent({"type": "alarme", "heure": "14:05", "libelle": "dentiste"}, NOW)
    assert argv[:4] == ["am", "start", "-a", "android.intent.action.SET_ALARM"]
    assert "android.intent.extra.alarm.HOUR" in argv
    assert "14" in argv and "5" in argv
    assert "dentiste" in argv

def test_appel_uses_dial_not_call():
    argv = build_intent({"type": "appel", "destinataire": "0612345678"}, NOW)
    assert "android.intent.action.DIAL" in argv
    assert "android.intent.action.CALL" not in argv
    assert "tel:0612345678" in argv

def test_message_email():
    argv = build_intent({"type": "message", "canal": "email", "objet": "O", "corps": "C"}, NOW)
    assert "android.intent.action.SENDTO" in argv
    assert "mailto:" in argv
    assert "O" in argv and "C" in argv

def test_message_sms():
    argv = build_intent({"type": "message", "canal": "sms", "corps": "C"}, NOW)
    assert "smsto:" in argv
    assert "C" in argv

def test_itineraire():
    argv = build_intent({"type": "itineraire", "destination": "Gare de Lyon"}, NOW)
    assert "android.intent.action.VIEW" in argv
    assert any(a.startswith("geo:0,0?q=") for a in argv)

def test_agenda_resolves_time():
    argv = build_intent({"type": "agenda", "titre": "R", "quand": "demain 10:00"}, NOW)
    assert "android.intent.action.INSERT" in argv
    # 2026-06-26 10:00 local -> beginTime present as epoch ms string
    assert "beginTime" in argv

def test_unknown_type():
    import pytest
    with pytest.raises(ValueError):
        build_intent({"type": "inconnu"}, NOW)
