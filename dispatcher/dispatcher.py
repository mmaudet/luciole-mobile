"""On-device 'hands': phrase -> model API -> (optional validate) -> fire Android intent.
Run inside Termux on the Pixel. STDLIB-ONLY by default (no pip deps): the HTTP call
uses urllib, and the server-level GBNF grammar already guarantees schema-valid JSON.

Optional env switches:
  DISPATCH_VALIDATE=1  -> also validate against the JSON Schema (needs `pip install jsonschema`)
  DISPATCH_DRY_RUN=1   -> print the intent but do NOT fire it (for benchmarks/tests)
"""
import json, os, pathlib, subprocess, sys, urllib.request
from datetime import datetime
from intents import build_intent, extract_phone, display_text, match_contact, call_target

ROOT = pathlib.Path(__file__).parents[1]
SYSTEM = (ROOT / "web/system_prompt.txt").read_text()
API = "http://127.0.0.1:8080/v1/chat/completions"

VALIDATOR = None
if os.environ.get("DISPATCH_VALIDATE"):
    from jsonschema import Draft202012Validator  # optional defense-in-depth
    VALIDATOR = Draft202012Validator(
        json.loads((ROOT / "contract/actions.schema.json").read_text()))

def ask_model(phrase: str) -> dict:
    payload = {
        "messages": [
            {"role": "system", "content": SYSTEM},
            {"role": "user", "content": phrase},
        ],
        "temperature": 0.0,
        "max_tokens": 256,
    }
    req = urllib.request.Request(API, data=json.dumps(payload).encode(),
                                 headers={"Content-Type": "application/json"})
    with urllib.request.urlopen(req, timeout=60) as r:
        content = json.loads(r.read())["choices"][0]["message"]["content"]
    return json.loads(content)

def resolve_contact(name: str):
    """Look up a contact's number by name via Termux:API (termux-contact-list). None on miss
    (Termux:API absent / Contacts permission denied / no confident match)."""
    name = (name or "").strip()
    if not name:
        return None
    try:
        out = subprocess.run(["termux-contact-list"], capture_output=True, text=True, timeout=20)
        contacts = json.loads(out.stdout or "[]")
    except Exception:
        return None
    return match_contact(name, contacts)

def main():
    phrase = " ".join(sys.argv[1:]).strip()
    if not phrase:
        print("usage: python dispatcher.py \"<phrase>\"", file=sys.stderr); sys.exit(2)
    action = ask_model(phrase)
    if VALIDATOR is not None:
        VALIDATOR.validate(action)             # defense in depth: reject off-grammar output
    if action.get("type") == "appel":
        # The number/name come from the PHRASE, not the 1B (which over-copies examples):
        # a spoken number wins, else resolve the spoken NAME via the address book.
        num = extract_phone(phrase)
        if not num:
            target = call_target(phrase)
            num = resolve_contact(target)
            if not num:
                print("ACTION:", json.dumps(action, ensure_ascii=False))
                print("TEXTE:", f"Contact introuvable dans le carnet : « {target} ».")
                return
        action["destinataire"] = num
    msg = display_text(action)
    if msg is not None:
        print("ACTION:", json.dumps(action, ensure_ascii=False))
        print("TEXTE:", msg)
        return
    argv = build_intent(action, datetime.now())
    print("ACTION:", json.dumps(action, ensure_ascii=False))
    print("INTENT:", " ".join(argv))
    if os.environ.get("DISPATCH_DRY_RUN"):
        return
    subprocess.run(argv, check=True)           # fires the native intent

if __name__ == "__main__":
    main()
