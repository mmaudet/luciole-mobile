from datetime import datetime
import sys, pathlib, pytest
sys.path.insert(0, str(pathlib.Path(__file__).parents[1]))
from datetime_fr import resolve_datetime

NOW = datetime(2026, 6, 25, 9, 0)  # jeudi 25 juin 2026, 09:00

def test_today_hhmm():
    assert resolve_datetime("14:00", NOW) == datetime(2026, 6, 25, 14, 0)

def test_demain():
    assert resolve_datetime("demain 10:30", NOW) == datetime(2026, 6, 26, 10, 30)

def test_apres_demain():
    assert resolve_datetime("après-demain 08:00", NOW) == datetime(2026, 6, 27, 8, 0)

def test_weekday_next_occurrence():
    # jeudi -> next lundi is 2026-06-29
    assert resolve_datetime("lundi 09:15", NOW) == datetime(2026, 6, 29, 9, 15)

def test_dans_minutes():
    assert resolve_datetime("dans 20 minutes", NOW) == datetime(2026, 6, 25, 9, 20)

def test_dans_heures():
    assert resolve_datetime("dans 2 heures", NOW) == datetime(2026, 6, 25, 11, 0)

def test_unknown_raises():
    with pytest.raises(ValueError):
        resolve_datetime("la semaine prochaine", NOW)
