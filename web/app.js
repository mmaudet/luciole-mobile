// web/app.js
import { buildDeepLink, extractPhone } from './deeplinks.mjs';

// Source unique du prompt : web/system_prompt.txt (servie par llama-server --path web).
// Envoyée VERBATIM — time-invariant (pas de {now}) pour garder le préfixe KV-cache valide.
const SYSTEM = fetch('system_prompt.txt').then(r => r.text());

function platform() {
  return /iPhone|iPad|iPod/i.test(navigator.userAgent) ? 'ios' : 'android';
}

async function ask(phrase) {
  const system = await SYSTEM;
  const r = await fetch('/v1/chat/completions', {
    method: 'POST', headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      messages: [{ role: 'system', content: system }, { role: 'user', content: phrase }],
      temperature: 0.0, max_tokens: 256,
    }),
  });
  if (!r.ok) throw new Error('API ' + r.status);
  return JSON.parse((await r.json()).choices[0].message.content);
}

// Build the result via DOM nodes (textContent / properties) — never innerHTML —
// so model-returned strings can't inject markup.
function render(action) {
  const out = document.getElementById('out');
  out.replaceChildren();

  const pre = document.createElement('pre');
  pre.textContent = JSON.stringify(action, null, 2);
  out.appendChild(pre);

  const link = buildDeepLink(action, platform(), new Date());
  if (link.kind === 'href') {
    const a = document.createElement('a');
    a.className = 'action'; a.href = link.href; a.textContent = link.label;
    out.appendChild(a);
  } else if (link.kind === 'ics') {
    const a = document.createElement('a');
    a.className = 'action';
    a.href = URL.createObjectURL(new Blob([link.text], { type: 'text/calendar' }));
    a.download = link.filename; a.textContent = link.label;
    out.appendChild(a);
  } else if (link.kind === 'text') {
    const p = document.createElement('p');
    p.textContent = link.text;
    out.appendChild(p);
  } else {
    const p = document.createElement('p');
    p.className = 'muted'; p.textContent = link.label;
    out.appendChild(p);
  }
}

document.getElementById('go').addEventListener('click', async () => {
  const out = document.getElementById('out');
  out.textContent = '…';
  try {
    const phrase = document.getElementById('phrase').value;
    const action = await ask(phrase);
    if (action.type === 'appel') {           // trust the phrase's digits, not the 1B's copy
      const n = extractPhone(phrase);
      if (n) action.destinataire = n;
    }
    render(action);
  } catch (e) { out.textContent = 'Erreur : ' + e.message; }
});
