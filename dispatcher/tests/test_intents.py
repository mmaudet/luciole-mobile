from datetime import datetime
import sys, pathlib
sys.path.insert(0, str(pathlib.Path(__file__).parents[1]))
from intents import build_intent, extract_phone

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

def test_extract_phone_from_phrase():
    assert extract_phone("appelle le 07 11 22 33 44") == "0711223344"
    assert extract_phone("téléphone au 01 23 45 67 89") == "0123456789"
    assert extract_phone("compose le 04.78.12.34.56") == "0478123456"
    assert extract_phone("appelle le +33 6 12 34 56 78") == "+33612345678"
    assert extract_phone("appelle le 06 12 34 56 78 avant 9h") == "0612345678"

def test_extract_phone_none_when_no_number():
    assert extract_phone("appelle Paul") is None
    assert extract_phone("rappelle-moi à 14h") is None
