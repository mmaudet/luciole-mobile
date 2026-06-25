// web/app.js
import { buildDeepLink } from './deeplinks.mjs';

const SYSTEM = "Tu es un routeur d'intentions. Pour chaque phrase, tu produis UNIQUEMENT un objet JSON d'action selon le schéma imposé. Tu ne réponds jamais en langage naturel.";

function platform() {
  return /iPhone|iPad|iPod/i.test(navigator.userAgent) ? 'ios' : 'android';
}

async function ask(phrase) {
  const r = await fetch('/v1/chat/completions', {
    method: 'POST', headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      messages: [{ role: 'system', content: SYSTEM }, { role: 'user', content: phrase }],
      temperature: 0.0, n_predict: 256,
    }),
  });
  if (!r.ok) throw new Error('API ' + r.status);
  return JSON.parse((await r.json()).choices[0].message.content);
}

function render(action) {
  const out = document.getElementById('out');
  const link = buildDeepLink(action, platform(), new Date());
  out.innerHTML = `<pre>${JSON.stringify(action, null, 2)}</pre>`;
  if (link.kind === 'href') {
    out.innerHTML += `<a class="action" href="${link.href}">${link.label}</a>`;
  } else if (link.kind === 'ics') {
    const url = URL.createObjectURL(new Blob([link.text], { type: 'text/calendar' }));
    out.innerHTML += `<a class="action" href="${url}" download="${link.filename}">${link.label}</a>`;
  } else {
    out.innerHTML += `<p class="muted">${link.label}</p>`;
  }
}

document.getElementById('go').addEventListener('click', async () => {
  const out = document.getElementById('out');
  out.textContent = '…';
  try { render(await ask(document.getElementById('phrase').value)); }
  catch (e) { out.textContent = 'Erreur : ' + e.message; }
});
