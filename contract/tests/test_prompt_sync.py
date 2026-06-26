import json, pathlib

ROOT = pathlib.Path(__file__).resolve().parents[2]
SCHEMA = json.loads((ROOT / "contract/actions.schema.json").read_text())
PROMPT = (ROOT / "web/system_prompt.txt").read_text()
TYPES = [b["properties"]["type"]["const"] for b in SCHEMA["oneOf"]]

def test_prompt_documents_every_type():
    missing = [t for t in TYPES if t not in PROMPT]
    assert not missing, f"types absents du prompt système: {missing}"

def test_prompt_is_time_invariant():
    # un {now} casserait le préfixe KV-cache (cf. server/run-server.sh)
    assert "{now}" not in PROMPT
