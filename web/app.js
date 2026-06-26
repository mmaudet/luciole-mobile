// web/app.js
import { buildDeepLink, extractPhone, callTarget } from './deeplinks.mjs';

// Source unique du prompt : web/system_prompt.txt (servie par llama-server --path web).
// Envoyée VERBATIM — time-invariant (pas de {now}) pour garder le préfixe KV-cache valide.
const SYSTEM = fetch('system_prompt.txt').then(r => {
  if (!r.ok) throw new Error('prompt ' + r.status);
  return r.text();
});

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
  } else if (link.kind === 'download') {
    const a = document.createElement('a');
    a.className = 'action';
    a.href = URL.createObjectURL(new Blob([link.text], { type: 'text/plain' }));
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

async function run(phrase) {
  const out = document.getElementById('out');
  // Indicateur d'attente animé + chrono : la génération tourne SUR LE TÉLÉPHONE (~2 s, plus à froid).
  out.replaceChildren();
  const bar = document.createElement('div'); bar.className = 'progress';
  bar.appendChild(document.createElement('span'));
  const label = document.createElement('p'); label.className = 'muted';
  out.append(bar, label);
  const t0 = performance.now();
  const timer = setInterval(() => {
    label.textContent = `🔦 Luciole réfléchit… ${((performance.now() - t0) / 1000).toFixed(1)} s`;
  }, 100);
  try {
    const action = await ask(phrase);
    if (action.type === 'appel') {           // from the PHRASE: a number, else a name (Pixel-only on web)
      const n = extractPhone(phrase);
      action.destinataire = n || callTarget(phrase);
    }
    clearInterval(timer);
    render(action);
  } catch (e) { clearInterval(timer); out.textContent = 'Erreur : ' + e.message; }
}

document.getElementById('go').addEventListener('click', () => run(document.getElementById('phrase').value));

// Exemples cliquables : remplir la zone de texte puis lancer. Couvre les 11 actions
// avec des formulations fiables (cf. RUNBOOK §6 sur les limites de routage du 1B).
const EXAMPLES = [
  'ajoute une réunion demain à 14h',
  "envoie un SMS pour dire que j'arrive",
  "écris un mail à propos d'un retard de livraison",
  'itinéraire vers la gare de Lyon à Paris',
  'appelle le 06 12 34 56 78',
  "note d'acheter du pain et du lait",
  'traduis bonjour en anglais',
  "cherche la capitale de l'Australie",
  'mets une alarme à 7h30',
  'minuteur de 10 minutes',
  'raconte-moi une blague',
];
const exBox = document.getElementById('examples');
for (const phrase of EXAMPLES) {
  const b = document.createElement('button');
  b.className = 'chip'; b.type = 'button'; b.textContent = phrase;
  b.addEventListener('click', () => { document.getElementById('phrase').value = phrase; run(phrase); });
  exBox.appendChild(b);
}
