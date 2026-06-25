import json, pathlib, pytest
from jsonschema import Draft202012Validator

SCHEMA = json.loads((pathlib.Path(__file__).parents[1] / "actions.schema.json").read_text())
V = Draft202012Validator(SCHEMA)

VALID = [
    {"type": "alarme", "heure": "14:00", "libelle": "appeler le dentiste"},
    {"type": "agenda", "titre": "Réunion", "quand": "demain 10:00", "duree_min": 60, "lieu": "salle B"},
    {"type": "message", "canal": "email", "objet": "Retard", "corps": "Je serai en retard."},
    {"type": "message", "canal": "sms", "corps": "Je serai en retard."},
    {"type": "itineraire", "destination": "Gare de Lyon, Paris", "mode": "transit"},
    {"type": "appel", "destinataire": ""},
]
INVALID = [
    {"type": "alarme", "heure": "26:00", "libelle": "x"},      # bad hour
    {"type": "alarme", "libelle": "x"},                          # missing heure
    {"type": "message", "canal": "fax", "corps": "x"},          # bad canal
    {"type": "agenda", "titre": "x"},                            # missing quand
    {"type": "inconnu", "x": 1},                                 # not in repertoire
    {"type": "appel", "destinataire": "x", "extra": 1},          # additionalProperties
]

@pytest.mark.parametrize("obj", VALID)
def test_valid(obj):
    V.validate(obj)

@pytest.mark.parametrize("obj", INVALID)
def test_invalid(obj):
    with pytest.raises(Exception):
        V.validate(obj)
