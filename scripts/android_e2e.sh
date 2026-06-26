#!/usr/bin/env bash
# =============================================================================
# android_e2e.sh — Harnais e2e Android (DIFFÉRÉ : requiert un appareil/émulateur)
# =============================================================================
#
# PRÉREQUIS (à exécuter sur le Pixel ou un émulateur branché) :
#   1. llama-server en cours dans Termux :
#        llama-server -m ~/.models/Luciole-1B-SFT-Q4_K_M.gguf \
#            --host 127.0.0.1 --port 8080 \
#            --grammar-file ~/luciole-mobile/contract/actions.gbnf \
#            -c 2048 -ngl 99 --metrics
#   2. Port forwardé si le script tourne sur le Mac :
#        adb forward tcp:8080 tcp:8080
#   3. Python 3 + jsonschema disponibles dans l'env courant :
#        pip install jsonschema
#
# UTILISATION :
#   bash scripts/android_e2e.sh [BASE_URL]
#   BASE_URL par défaut : http://127.0.0.1:8080
#
# SORTIE :
#   Rejoue les ~21 cas FR de scripts/e2e_smoke.py + un mini-jeu EN (10 cas).
#   Imprime [OK] / [XX] pour chaque phrase.
#   Affiche le taux de routage correct FR et EN séparément + global.
#   Sert de base de comparaison pour la S2 (parity baseline).
#   Code de sortie : 0 si tous les cas passent, 1 sinon.
#
# NOTE : ce script NE SERA PAS exécuté dans l'environnement CI/Mac (pas
# d'émulateur disponible). Il est inclus en tant que livrable e2e différé.
# =============================================================================

set -euo pipefail

BASE_URL="${1:-http://127.0.0.1:8080}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

echo "=== Luciole Android E2E — serveur : $BASE_URL ==="
echo

# Vérifier que le serveur est joignable
if ! curl -sf --max-time 5 "$BASE_URL/health" > /dev/null 2>&1 \
   && ! curl -sf --max-time 5 "$BASE_URL/v1/models" > /dev/null 2>&1; then
    echo "ERREUR : serveur injoignable à $BASE_URL"
    echo "Lancez llama-server dans Termux et, si besoin : adb forward tcp:8080 tcp:8080"
    exit 1
fi

# ---------------------------------------------------------------------------
# Script Python inline — rejoue les cas FR de e2e_smoke.py + mini-jeu EN
# ---------------------------------------------------------------------------
python3 - "$BASE_URL" "$ROOT" <<'PYEOF'
import json, sys, pathlib, urllib.request
from datetime import datetime

BASE_URL = sys.argv[1]
ROOT = pathlib.Path(sys.argv[2])
sys.path.insert(0, str(ROOT / "dispatcher"))

try:
    from intents import build_intent
except ImportError:
    # Sur appareil sans le dispatcher Python, on saute la validation d'intent
    def build_intent(action, now):
        return ["(intent-non-verifie)"]

try:
    from jsonschema import Draft202012Validator
    SCHEMA = json.loads((ROOT / "contract/actions.schema.json").read_text())
    VALIDATOR = Draft202012Validator(SCHEMA)
    def validate(action): VALIDATOR.validate(action)
except Exception:
    def validate(action): pass  # jsonschema absent ou schema introuvable

SYSTEM = (ROOT / "web/system_prompt.txt").read_text()
API = f"{BASE_URL}/v1/chat/completions"

# ------------------------------------------------------------------
# Cas FR — source : scripts/e2e_smoke.py (21 cas identiques)
# ------------------------------------------------------------------
CASES_FR = [
    ("rappelle-moi d'appeler le dentiste à 14h",                 "alarme",     None),
    ("mets une alarme à 7h30",                                   "alarme",     None),
    ("ajoute une réunion projet demain à 10h en salle B",        "agenda",     None),
    ("note un rendez-vous lundi à 9h15",                         "agenda",     None),
    ("envoie un SMS pour dire que je serai en retard de 10 min", "message",    "sms"),
    ("écris un mail à propos du retard de livraison",            "message",    "email"),
    ("itinéraire vers la gare de Lyon à Paris",                  "itineraire", None),
    ("appelle le 06 12 34 56 78",                                "appel",      None),
    ("préviens par texto que j'arrive dans 5 minutes",           "message",    "sms"),
    ("minuteur de 10 minutes pour le thé",                       "minuteur",   None),
    ("mets un compte à rebours de 5 minutes",                    "minuteur",   None),
    ("note d'acheter du pain",                                   "note",       None),
    ("prends une note : réserver le restaurant",                 "note",       None),
    ("c'est quoi la capitale de l'Australie",                    "recherche",  None),
    ("cherche les horaires du musée du Louvre",                  "recherche",  None),
    ("ouvre YouTube",                                            "ouvrir",     None),
    ("ouvre les réglages Bluetooth",                             "ouvrir",     None),
    ("traduis bonjour le monde en anglais",                      "traduction", None),
    ("traduis le chien dort en anglais",                         "traduction", None),
    ("raconte-moi une blague",                                   "inconnu",    None),
    ("raconte-moi une histoire",                                 "inconnu",    None),
]

# ------------------------------------------------------------------
# Mini-jeu EN — go/no-go anglais + base de comparaison S2
# ------------------------------------------------------------------
CASES_EN = [
    ("set a 5-minute timer",                                     "minuteur",   None),
    ("set an alarm for 7:30",                                    "alarme",     None),
    ("call 06 12 34 56 78",                                      "appel",      None),
    ("navigate to the train station",                            "itineraire", None),
    ("send a text saying I'll be 10 minutes late",               "message",    "sms"),
    ("send an email about the delivery delay",                   "message",    "email"),
    ("add a meeting tomorrow at 10am in room B",                 "agenda",     None),
    ("search for Louvre museum opening hours",                   "recherche",  None),
    ("open YouTube",                                             "ouvrir",     None),
    ("tell me a joke",                                           "inconnu",    None),
]

def ask(phrase):
    payload = {
        "messages": [
            {"role": "system", "content": SYSTEM},
            {"role": "user",   "content": phrase}
        ],
        "temperature": 0.0,
        "max_tokens": 256,
    }
    req = urllib.request.Request(
        API,
        data=json.dumps(payload).encode(),
        headers={"Content-Type": "application/json"},
    )
    with urllib.request.urlopen(req, timeout=180) as r:
        return json.loads(r.read())["choices"][0]["message"]["content"]

def run_cases(cases, label):
    now = datetime.now()
    npass = 0
    print(f"--- {label} ({len(cases)} cas) ---")
    for phrase, exp_type, exp_canal in cases:
        try:
            action = json.loads(ask(phrase))
            validate(action)
            if action.get("type") not in {"inconnu", "traduction"}:
                argv = build_intent(action, now)
            else:
                argv = ["(display-only)"]
            type_ok = action.get("type") == exp_type
            canal_ok = exp_canal is None or action.get("canal") == exp_canal
            ok = type_ok and canal_ok
            npass += ok
            why = "" if ok else (
                f"  <-- type={action.get('type')}/{exp_type}"
                + (f" canal={action.get('canal')}/{exp_canal}" if exp_canal else "")
            )
            print(f"  [{'OK' if ok else 'XX'}] {phrase}{why}")
        except Exception as e:
            print(f"  [XX] {phrase}\n       ERREUR: {type(e).__name__}: {e}")
    print(f"  Résultat {label}: {npass}/{len(cases)}\n")
    return npass, len(cases)

ok_fr, tot_fr = run_cases(CASES_FR, "FR (parity e2e_smoke.py)")
ok_en, tot_en = run_cases(CASES_EN, "EN (go/no-go + baseline S2)")

total_ok  = ok_fr + ok_en
total_tot = tot_fr + tot_en
rate_fr   = 100 * ok_fr / tot_fr if tot_fr else 0
rate_en   = 100 * ok_en / tot_en if tot_en else 0
rate_all  = 100 * total_ok / total_tot if total_tot else 0

print("=" * 60)
print(f"FR  : {ok_fr:2d}/{tot_fr} ({rate_fr:.0f}%)")
print(f"EN  : {ok_en:2d}/{tot_en} ({rate_en:.0f}%)  ← go/no-go EN")
print(f"ALL : {total_ok:2d}/{total_tot} ({rate_all:.0f}%)")
print("=" * 60)

sys.exit(0 if total_ok == total_tot else 1)
PYEOF
