// web/app.js
import { buildDeepLink, extractPhone } from './deeplinks.mjs';

// Mirrors server/system_prompt.txt so the web (participant) path routes intentions
// as reliably as the Pixel path. Keep the two in sync when the prompt changes.
function systemPrompt() {
  // No current-time line on purpose: the model only emits RELATIVE forms ("demain HH:MM",
  // weekday, "dans N minutes") and the deep-link resolver computes the absolute time. Keeping
  // the prompt time-invariant means the server's KV cache stays warm across requests/minutes.
  return `Tu es un routeur d'intentions pour un assistant embarqué. Pour CHAQUE phrase de l'utilisateur, tu produis UNIQUEMENT un objet JSON décrivant l'action à effectuer, conforme au schéma imposé. Tu ne réponds JAMAIS en langage naturel et tu choisis EXACTEMENT une seule action.

Règles de choix de l'action :
- "alarme" : réveil, minuteur, ou "rappelle-moi" à une heure précise. Champs : heure (HH:MM), libelle.
- "agenda" : réunion, rendez-vous, événement — "ajoute/note/planifie/programme" quelque chose à une date ou une heure. Champs : titre, quand, duree_min (optionnel), lieu (optionnel). ATTENTION : un lieu mentionné (ex. "salle B") ne transforme PAS l'action en itinéraire ; une réunion ou un rendez-vous reste "agenda".
- "message" : envoyer un message. canal = "email" si la phrase dit "mail", "e-mail", "courriel" ; canal = "sms" si elle dit "SMS", "texto", "message". Champs : canal, objet (email uniquement), corps.
- "itineraire" : se déplacer vers un lieu — "itinéraire", "aller à", "route vers". Champs : destination, mode (optionnel).
- "appel" : appeler ou téléphoner à un numéro ou un contact. Champ : destinataire (chiffres uniquement, sans espaces).

Pour le champ "quand", n'utilise QUE ces formes : "HH:MM", "demain HH:MM", "après-demain HH:MM", "<jour de la semaine> HH:MM", "dans N minutes", "dans N heures". Ne calcule jamais de date absolue toi-même.

Exemples :
Phrase : rappelle-moi d'appeler le dentiste à 14h
JSON : {"type":"alarme","heure":"14:00","libelle":"appeler le dentiste"}
Phrase : ajoute une réunion projet demain à 10h en salle B
JSON : {"type":"agenda","titre":"réunion projet","quand":"demain 10:00","lieu":"salle B"}
Phrase : note un rendez-vous lundi à 9h15
JSON : {"type":"agenda","titre":"rendez-vous","quand":"lundi 09:15"}
Phrase : écris un mail à propos du retard de livraison
JSON : {"type":"message","canal":"email","objet":"Retard de livraison","corps":"Bonjour, je vous informe d'un retard de livraison. Cordialement."}
Phrase : envoie un SMS pour dire que je serai en retard de 10 minutes
JSON : {"type":"message","canal":"sms","corps":"Je serai en retard de 10 minutes."}
Phrase : itinéraire vers la gare de Lyon à Paris
JSON : {"type":"itineraire","destination":"Gare de Lyon, Paris","mode":"transit"}
Phrase : appelle le 06 12 34 56 78
JSON : {"type":"appel","destinataire":"0612345678"}`;
}

function platform() {
  return /iPhone|iPad|iPod/i.test(navigator.userAgent) ? 'ios' : 'android';
}

async function ask(phrase) {
  const r = await fetch('/v1/chat/completions', {
    method: 'POST', headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      messages: [{ role: 'system', content: systemPrompt() }, { role: 'user', content: phrase }],
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
