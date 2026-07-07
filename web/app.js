// web/app.js — logique + rendu « République » (icônes ligne Iconoir). Contrat serveur + deeplinks conservés.
import { buildDeepLink, extractPhone, callTarget } from './deeplinks.mjs?v=3';

const SYSTEM = fetch('system_prompt.txt').then(r => { if (!r.ok) throw new Error('prompt ' + r.status); return r.text(); });
const platform = () => {
  const ua = navigator.userAgent;
  if (/iPhone|iPad|iPod/i.test(ua)) return 'ios';
  if (/Android/i.test(ua)) return 'android';
  return 'desktop';   // PC : les deeplinks basculent en URLs web (Maps, mailto…) ou carte « sur le Pixel »
};
const $ = id => document.getElementById(id);
const icoEl = name => { const s = document.createElement('span'); s.className = 'ico ico-' + name; return s; };

// ---------------- i18n ----------------
const I18N = {
  fr: {
    demo: 'DÉMONSTRATION', tagline: 'IA souveraine, 100 % sur votre téléphone, hors-ligne.',
    ob_titre: 'Une phrase.\nUne action.\nRien ne sort du téléphone.',
    ob_p1_titre: '100 % hors-ligne', ob_p1_desc: 'Aucune donnée ne quitte l’appareil.',
    ob_p2_titre: 'Modèle souverain', ob_p2_desc: 'Luciole-1B, IA française, tourne sur le téléphone.',
    ob_p3_titre: '11 actions natives', ob_p3_desc: 'Appel, minuteur, itinéraire… en ~3 s.',
    ob_cta: 'Commencer', ob_lang: 'Langue', stats_lock: 'Aucune donnée ne quitte le téléphone.',
    nav_chat: 'Chat', chat_titre: 'Conversation', chat_effacer: 'Effacer', envoyer: 'Envoyer',
    phone_ok: 'Téléphone', phone_ko: 'Injoignable', ph_saisie: 'Dites une phrase…',
    aide_gabarits: 'Gabarits d’actions', aide_sous_titre: 'L’entité est déjà sélectionnée, tapez par-dessus.',
    aide_inserer: 'Insérer dans le chat', aide_hint: 'Touchez l’entité pour la remplacer, puis envoyez.',
    ouvrir: 'Ouvrir', reflechit: 'Luciole réfléchit', traite_en: 'traité en', inconnu: 'Je ne sais pas faire ça.', prendre_photo: 'Prendre la photo',
    masquer_aide: 'Masquer l’aide', afficher_aide: 'Afficher l’aide',
  },
  en: {
    demo: 'DEMO', tagline: 'Sovereign AI, 100% on your phone, offline.',
    ob_titre: 'One sentence.\nOne action.\nNothing leaves the phone.',
    ob_p1_titre: '100% offline', ob_p1_desc: 'No data leaves the device.',
    ob_p2_titre: 'Sovereign model', ob_p2_desc: 'Luciole-1B, a French AI, runs on the phone.',
    ob_p3_titre: '11 native actions', ob_p3_desc: 'Call, timer, directions… in ~3 s.',
    ob_cta: 'Start', ob_lang: 'Language', stats_lock: 'No data leaves the phone.',
    nav_chat: 'Chat', chat_titre: 'Conversation', chat_effacer: 'Clear', envoyer: 'Send',
    phone_ok: 'Phone', phone_ko: 'Disconnected', ph_saisie: 'Say something…',
    aide_gabarits: 'Action templates', aide_sous_titre: 'The entity is preselected — type over it.',
    aide_inserer: 'Insert into chat', aide_hint: 'Tap the entity to replace it, then send.',
    ouvrir: 'Open', reflechit: 'Luciole is thinking', traite_en: 'done in', inconnu: 'I can’t do that.', prendre_photo: 'Take the photo',
    masquer_aide: 'Hide help', afficher_aide: 'Show help',
  },
};
let LANG = 'fr';
const t = k => (I18N[LANG] && I18N[LANG][k]) || I18N.fr[k] || k;
function applyLang() {
  document.documentElement.lang = LANG;
  for (const el of document.querySelectorAll('[data-i18n]')) el.textContent = t(el.dataset.i18n);
  for (const el of document.querySelectorAll('[data-i18n-ph]')) el.placeholder = t(el.dataset.i18nPh);
  for (const b of document.querySelectorAll('#lang-toggle button')) b.classList.toggle('on', b.dataset.lang === LANG);
  renderGabarits(); paintConn();
}

// ---------------- action -> icône (Iconoir) + libellé ----------------
const ACT = {
  appel: ['phone', 'Appel'], alarme: ['bell', 'Alarme'], minuteur: ['timer', 'Minuteur'], agenda: ['calendar', 'Agenda'],
  message: ['mail', 'Message'], itineraire: ['map', 'Itinéraire'], recherche: ['search', 'Recherche'],
  ouvrir: ['app', 'Ouvrir'], note: ['notes', 'Note'], traduction: ['language', 'Traduction'], inconnu: ['question', '—'],
};
function sub(a) {
  switch (a.type) {
    case 'appel': return a.destinataire || '';
    case 'itineraire': return a.destination + (a.mode ? ` · ${a.mode}` : '');
    case 'minuteur': return `${a.duree_min} min` + (a.libelle ? ` · ${a.libelle}` : '');
    case 'alarme': return `${a.heure || ''}${a.libelle ? ' · ' + a.libelle : ''}`;
    case 'agenda': return `${a.titre || ''}${a.quand ? ' · ' + a.quand : ''}`;
    case 'message': return `${a.canal || ''}${a.corps ? ' · ' + a.corps : ''}`;
    case 'note': return a.texte || '';
    case 'recherche': return a.requete || '';
    case 'ouvrir': return a.cible || '';
    case 'traduction': return `${a.cible} : ${a.resultat || a.texte || ''}`;
    default: return '';
  }
}

async function ask(phrase) {
  const system = await SYSTEM;
  const r = await fetch('/v1/chat/completions', {
    method: 'POST', headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ messages: [{ role: 'system', content: system }, { role: 'user', content: phrase }], temperature: 0.0, max_tokens: 256 }),
  });
  if (!r.ok) throw new Error('API ' + r.status);
  return JSON.parse((await r.json()).choices[0].message.content);
}

// ---------------- rendu chat ----------------
const messages = () => $('messages');
const scroll = () => { const m = messages(); m.scrollTop = m.scrollHeight; };
function addUser(text) { const d = document.createElement('div'); d.className = 'msg-user'; d.textContent = text; messages().appendChild(d); scroll(); }

function actionCard(action, secs) {
  const wrap = document.createElement('div'); wrap.className = 'msg-bot';
  const card = document.createElement('div'); card.className = 'action-card';
  const [icon, label] = ACT[action.type] || ACT.inconnu;
  const top = document.createElement('div'); top.className = 'top';
  const ic = document.createElement('div'); ic.className = 'ic'; ic.appendChild(icoEl(icon));
  const meta = document.createElement('div');
  const lab = document.createElement('div'); lab.className = 'label'; lab.textContent = action.type === 'inconnu' ? t('inconnu') : label;
  const s = document.createElement('div'); s.className = 'sub'; s.textContent = sub(action);
  meta.append(lab, s); top.append(ic, meta); card.appendChild(top);

  const pre = document.createElement('pre'); pre.textContent = JSON.stringify(action, null, 2); card.appendChild(pre);

  const foot = document.createElement('div'); foot.className = 'foot';
  const link = buildDeepLink(action, platform(), new Date());
  const openBtn = () => { const a = document.createElement('a'); a.className = 'btn-open'; a.append(document.createTextNode(t('ouvrir') + ' '), icoEl('arrow')); return a; };
  if (link.kind === 'href') { const a = openBtn(); a.href = link.href; foot.appendChild(a); }
  else if (link.kind === 'ics') { const a = openBtn(); a.href = URL.createObjectURL(new Blob([link.text], { type: 'text/calendar' })); a.download = link.filename; foot.appendChild(a); }
  else if (link.kind === 'download') { const a = openBtn(); a.href = URL.createObjectURL(new Blob([link.text], { type: 'text/plain' })); a.download = link.filename; foot.appendChild(a); }
  else if (link.kind === 'text') { const p = document.createElement('div'); p.className = 'sub'; p.textContent = link.text; foot.appendChild(p); }
  else if (link.kind === 'webcam') { foot.appendChild(boutonWebcam(card)); }
  else { const p = document.createElement('div'); p.className = 'proc'; p.textContent = link.label || ''; foot.appendChild(p); }

  const proc = document.createElement('span'); proc.className = 'proc';
  if (secs != null) proc.textContent = `${t('traite_en')} ${secs} s`;
  foot.appendChild(proc); card.appendChild(foot); wrap.appendChild(card);
  return wrap;
}

// ---------------- webcam (démo PC : la page capture la webcam du Mac via getUserMedia ; OK car localhost) ----------------
function boutonWebcam(card) {
  const b = document.createElement('button'); b.className = 'btn-open';
  b.append(document.createTextNode(t('prendre_photo') + ' '), icoEl('flash'));
  b.addEventListener('click', () => capturerPhoto(card, b));
  return b;
}
async function capturerPhoto(card, btn) {
  btn.disabled = true;
  let stream;
  try {
    stream = await navigator.mediaDevices.getUserMedia({ video: { width: { ideal: 1280 }, height: { ideal: 720 } }, audio: false });
    const video = document.createElement('video'); video.srcObject = stream; video.muted = true; video.playsInline = true;
    await video.play();
    await new Promise(res => (video.readyState >= 2 ? res() : (video.onloadeddata = res)));
    const w = video.videoWidth || 1280, h = video.videoHeight || 720;
    const canvas = document.createElement('canvas'); canvas.width = w; canvas.height = h;
    canvas.getContext('2d').drawImage(video, 0, 0, w, h);
    const img = document.createElement('img'); img.className = 'photo'; img.alt = 'photo webcam'; img.src = canvas.toDataURL('image/jpeg', 0.9);
    card.insertBefore(img, card.querySelector('.foot'));
    btn.remove(); scroll();
  } catch (e) {
    const p = document.createElement('div'); p.className = 'sub'; p.textContent = 'Webcam indisponible : ' + (e.message || e.name || e);
    card.insertBefore(p, card.querySelector('.foot'));
    btn.disabled = false;
  } finally {
    if (stream) stream.getTracks().forEach(tr => tr.stop());
  }
}

async function run(phrase) {
  if (!phrase.trim()) return;
  addUser(phrase);
  const wrap = document.createElement('div'); wrap.className = 'msg-bot';
  const card = document.createElement('div'); card.className = 'action-card';
  const bar = document.createElement('div'); bar.className = 'progress'; bar.appendChild(document.createElement('span'));
  const lbl = document.createElement('div'); lbl.className = 'proc'; lbl.style.marginTop = '10px';
  card.append(bar, lbl); wrap.appendChild(card); messages().appendChild(wrap); scroll();
  const t0 = performance.now();
  const timer = setInterval(() => { lbl.textContent = `${t('reflechit')}… ${((performance.now() - t0) / 1000).toFixed(1)} s`; }, 100);
  try {
    const action = await ask(phrase);
    if (action.type === 'appel') { const n = extractPhone(phrase); action.destinataire = n || callTarget(phrase); }
    clearInterval(timer);
    wrap.replaceWith(actionCard(action, ((performance.now() - t0) / 1000).toFixed(1))); scroll();
  } catch (e) { clearInterval(timer); bar.remove(); lbl.className = 'sub'; lbl.textContent = 'Erreur : ' + e.message; }
}

// ---------------- Aide : gabarits (les 10 de l'app) ----------------
// Ordre pensé pour la démo PC : d'abord les actions qui ouvrent un résultat à l'écran.
const GABARITS = [
  { key: 'itineraire', texte: 'itinéraire vers la gare de Lyon', ent: 'la gare de Lyon' },
  { key: 'recherche', texte: 'cherche la capitale du Pérou', ent: 'la capitale du Pérou' },
  { key: 'traduction', texte: 'traduis bonjour en anglais', ent: 'bonjour' },
  { key: 'agenda', texte: 'ajoute une réunion demain à 10h', ent: 'une réunion' },
  { key: 'message', texte: 'écris un mail à propos de la réunion de lundi', ent: 'la réunion de lundi' },
  { key: 'ouvrir', texte: 'ouvre Wikipédia', ent: 'Wikipédia' },
  { key: 'note', texte: 'note : acheter du pain', ent: 'acheter du pain' },
  { key: 'appel', texte: 'appelle Marie Curie', ent: 'Marie Curie' },
  { key: 'minuteur', texte: 'minuteur de 5 minutes', ent: '5' },
  { key: 'alarme', texte: 'réveille-moi à 7h30', ent: '7h30' },
];
const GLABEL = {
  fr: { appel: 'Appel', minuteur: 'Minuteur', alarme: 'Alarme', agenda: 'Agenda', message: 'Message', itineraire: 'Itinéraire', recherche: 'Recherche', traduction: 'Traduction', note: 'Note', ouvrir: 'Ouvrir' },
  en: { appel: 'Call', minuteur: 'Timer', alarme: 'Alarm', agenda: 'Calendar', message: 'Message', itineraire: 'Directions', recherche: 'Search', traduction: 'Translate', note: 'Note', ouvrir: 'Open' },
};
let selected = GABARITS.find(g => g.key === 'itineraire');
function renderGabarits() {
  const box = $('gabarits'); if (!box) return; box.replaceChildren();
  for (const g of GABARITS) {
    const b = document.createElement('button'); b.type = 'button'; b.className = 'gab' + (g === selected ? ' on' : '');
    b.append(icoEl((ACT[g.key] || ['question'])[0]), document.createTextNode((GLABEL[LANG] || GLABEL.fr)[g.key]));
    b.addEventListener('click', () => { selected = g; renderGabarits(); });
    box.appendChild(b);
  }
  renderFeatured();
}
function renderFeatured() {
  const f = $('featured'); if (!f) return; f.replaceChildren();
  const g = selected;
  const ic = document.createElement('div'); ic.className = 'f-ic'; ic.appendChild(icoEl((ACT[g.key] || ['question'])[0]));
  const lab = document.createElement('div'); lab.className = 'f-label'; lab.textContent = ((GLABEL[LANG] || GLABEL.fr)[g.key] || '').toUpperCase();
  const idx = g.texte.indexOf(g.ent);
  const pre = idx >= 0 ? g.texte.slice(0, idx) : g.texte;
  const suf = idx >= 0 ? g.texte.slice(idx + g.ent.length) : '';
  const p = document.createElement('div'); p.className = 'f-phrase';
  const ent = document.createElement('span'); ent.className = 'ent'; ent.textContent = g.ent;
  p.append(document.createTextNode(pre), ent, document.createTextNode(suf));
  const h = document.createElement('div'); h.className = 'f-hint'; h.textContent = t('aide_hint');
  f.append(ic, lab, p, h);
}

// ---------------- connectivité : lien vers le TÉLÉPHONE (ping /health) ----------------
let connOk = true;
async function pingConn() {
  try { const r = await fetch('/health', { cache: 'no-store' }); connOk = r.ok; } catch (e) { connOk = false; }
  paintConn();
}
function paintConn() {
  const c = $('conn'); if (c) c.classList.toggle('ko', !connOk);
  const l = $('conn-lbl'); if (l) l.textContent = connOk ? t('phone_ok') : t('phone_ko');
}
setInterval(pingConn, 5000);

// ---------------- wiring ----------------
$('commencer').addEventListener('click', () => { $('onboarding').classList.add('hidden'); $('app').classList.remove('hidden'); $('phrase').focus(); });
for (const b of document.querySelectorAll('#lang-toggle button')) b.addEventListener('click', () => { LANG = b.dataset.lang; applyLang(); paintAide(); });
$('go').addEventListener('click', () => { const v = $('phrase').value; $('phrase').value = ''; run(v); });
$('phrase').addEventListener('keydown', e => { if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); $('go').click(); } });
$('effacer').addEventListener('click', () => messages().replaceChildren());
$('inserer').addEventListener('click', () => {
  const g = selected, ta = $('phrase');
  ta.value = g.texte; ta.focus();
  const i = g.texte.indexOf(g.ent);
  if (i >= 0) ta.setSelectionRange(i, i + g.ent.length);
});

// ---------------- volet Aide : rétractable + responsive (tiroir sous 900 px) ----------------
let aideOuvert = window.innerWidth > 900;
function paintAide() {
  $('app').classList.toggle('aide-ferme', !aideOuvert);
  const b = $('toggle-aide');
  if (b) { b.classList.toggle('on', aideOuvert); b.textContent = t(aideOuvert ? 'masquer_aide' : 'afficher_aide'); }
}
$('toggle-aide').addEventListener('click', () => { aideOuvert = !aideOuvert; paintAide(); });

applyLang();
pingConn();
paintAide();
