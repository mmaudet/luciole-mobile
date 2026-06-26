#!/usr/bin/env python3
"""E2E smoke test for the Luciole action router.

Requires a running llama-server with the model + grammar (see server/run-server.sh),
reachable at http://127.0.0.1:8080. On a dev laptop you can run the same stack natively:

    llama-server -m .models/Luciole-1B-SFT-Q4_K_M.gguf --host 127.0.0.1 --port 8080 \
        --grammar-file contract/actions.gbnf -c 2048 -ngl 99
    python scripts/e2e_smoke.py

For each phrase it checks: JSON parseable, schema-valid, correct intention, correct
canal (message cases), and that the intent builds (which proves `quand` is resolvable).
Exits non-zero unless all cases pass. Mirrors the exact request shape dispatcher.py sends.
"""
import json, sys, pathlib, urllib.request
from datetime import datetime

ROOT = pathlib.Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "dispatcher"))
from intents import build_intent
from jsonschema import Draft202012Validator

SCHEMA = json.loads((ROOT / "contract/actions.schema.json").read_text())
VALIDATOR = Draft202012Validator(SCHEMA)
SYSTEM = (ROOT / "web/system_prompt.txt").read_text()
API = "http://127.0.0.1:8080/v1/chat/completions"

# (phrase, expected type, expected canal or None)
CASES = [
    ("rappelle-moi d'appeler le dentiste à 14h",                 "alarme",     None),
    ("mets une alarme à 7h30",                                   "alarme",     None),
    ("ajoute une réunion projet demain à 10h en salle B",        "agenda",     None),
    ("note un rendez-vous lundi à 9h15",                         "agenda",     None),
    ("envoie un SMS pour dire que je serai en retard de 10 min", "message",    "sms"),
    ("écris un mail à propos du retard de livraison",            "message",    "email"),
    ("itinéraire vers la gare de Lyon à Paris",                  "itineraire", None),
    ("appelle le 06 12 34 56 78",                                "appel",      None),
    ("préviens par texto que j'arrive dans 5 minutes",           "message",    "sms"),
]

def ask(phrase):
    payload = {"messages": [{"role": "system", "content": SYSTEM},
                            {"role": "user", "content": phrase}],
               "temperature": 0.0, "max_tokens": 256}
    req = urllib.request.Request(API, data=json.dumps(payload).encode(),
                                 headers={"Content-Type": "application/json"})
    with urllib.request.urlopen(req, timeout=180) as r:
        return json.loads(r.read())["choices"][0]["message"]["content"]

def main():
    now = datetime.now()
    npass = 0
    for phrase, exp_type, exp_canal in CASES:
        try:
            action = json.loads(ask(phrase))
            VALIDATOR.validate(action)
            argv = build_intent(action, now)
            type_ok = action.get("type") == exp_type
            canal_ok = exp_canal is None or action.get("canal") == exp_canal
            ok = type_ok and canal_ok
            npass += ok
            why = "" if ok else (f"  <-- type={action.get('type')}/{exp_type}"
                                 + (f" canal={action.get('canal')}/{exp_canal}" if exp_canal else ""))
            print(f"[{'OK ' if ok else 'XX'}] {phrase}{why}")
            print(f"      {json.dumps(action, ensure_ascii=False)}")
            print(f"      intent: {' '.join(argv[:8])}{' ...' if len(argv) > 8 else ''}")
        except Exception as e:
            print(f"[XX] {phrase}\n      ERREUR: {type(e).__name__}: {e}")
    print(f"\n=== {npass}/{len(CASES)} cas OK (intention + canal ; JSON+schéma valides exigés) ===")
    sys.exit(0 if npass == len(CASES) else 1)

if __name__ == "__main__":
    main()
