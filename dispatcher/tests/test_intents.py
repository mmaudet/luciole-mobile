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

def test_unknown_type_still_raises():
    import pytest
    with pytest.raises(ValueError):
        build_intent({"type": "wat"}, NOW)

def test_display_text_inconnu():
    from intents import display_text, MESSAGE_INCONNU
    assert display_text({"type": "inconnu"}) == MESSAGE_INCONNU

def test_display_text_none_for_fireable():
    from intents import display_text
    assert display_text({"type": "alarme", "heure": "8:00", "libelle": "x"}) is None

def test_extract_phone_from_phrase():
    assert extract_phone("appelle le 07 11 22 33 44") == "0711223344"
    assert extract_phone("téléphone au 01 23 45 67 89") == "0123456789"
    assert extract_phone("compose le 04.78.12.34.56") == "0478123456"
    assert extract_phone("appelle le +33 6 12 34 56 78") == "+33612345678"
    assert extract_phone("appelle le 06 12 34 56 78 avant 9h") == "0612345678"

def test_extract_phone_none_when_no_number():
    assert extract_phone("appelle Paul") is None
    assert extract_phone("rappelle-moi à 14h") is None

def test_minuteur_converts_minutes_to_seconds():
    argv = build_intent({"type": "minuteur", "duree_min": 10, "libelle": "thé"}, NOW)
    assert "android.intent.action.SET_TIMER" in argv
    assert "android.intent.extra.alarm.LENGTH" in argv
    assert "600" in argv          # 10 min -> 600 s
    assert "thé" in argv
    assert "true" in argv         # SKIP_UI

def test_note_uses_send_text_plain():
    argv = build_intent({"type": "note", "texte": "acheter du pain"}, NOW)
    assert "android.intent.action.SEND" in argv
    assert "text/plain" in argv
    assert "acheter du pain" in argv

def test_recherche_uses_web_search():
    argv = build_intent({"type": "recherche", "requete": "tour Eiffel"}, NOW)
    assert "android.intent.action.WEB_SEARCH" in argv
    assert "query" in argv
    assert "tour Eiffel" in argv

def test_ouvrir_app_uses_monkey_launcher():
    argv = build_intent({"type": "ouvrir", "cible": "youtube"}, NOW)
    assert argv[0] == "monkey"
    assert "com.google.android.youtube" in argv

def test_ouvrir_setting_uses_settings_action():
    argv = build_intent({"type": "ouvrir", "cible": "bluetooth"}, NOW)
    assert argv[0] == "am"
    assert "android.settings.BLUETOOTH_SETTINGS" in argv
