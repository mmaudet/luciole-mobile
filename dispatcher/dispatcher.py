"""On-device 'hands': phrase -> model API -> validate -> fire Android intent.
Run inside Termux on the Pixel. Requires: pip install jsonschema requests (Termux)."""
import json, pathlib, subprocess, sys
from datetime import datetime
import requests
from jsonschema import Draft202012Validator
from intents import build_intent

ROOT = pathlib.Path(__file__).parents[1]
SCHEMA = json.loads((ROOT / "contract/actions.schema.json").read_text())
VALIDATOR = Draft202012Validator(SCHEMA)
SYSTEM = (ROOT / "server/system_prompt.txt").read_text()
API = "http://127.0.0.1:8080/v1/chat/completions"

def ask_model(phrase: str) -> dict:
    now = datetime.now().strftime("%Y-%m-%d %H:%M (%A)")
    payload = {
        "messages": [
            {"role": "system", "content": SYSTEM.replace("{now}", now)},
            {"role": "user", "content": phrase},
        ],
        "temperature": 0.0,
        "n_predict": 256,
    }
    r = requests.post(API, json=payload, timeout=60)
    r.raise_for_status()
    content = r.json()["choices"][0]["message"]["content"]
    return json.loads(content)

def main():
    phrase = " ".join(sys.argv[1:]).strip()
    if not phrase:
        print("usage: python dispatcher.py \"<phrase>\"", file=sys.stderr); sys.exit(2)
    action = ask_model(phrase)
    VALIDATOR.validate(action)                 # defense in depth: reject off-grammar output
    argv = build_intent(action, datetime.now())
    print("ACTION:", json.dumps(action, ensure_ascii=False))
    print("INTENT:", " ".join(argv))
    subprocess.run(argv, check=True)           # fires the native intent

if __name__ == "__main__":
    main()
