// web/deeplinks.mjs
import { resolveDatetimeFr } from './datetime_fr.mjs';

const enc = encodeURIComponent;

function pad(n) { return String(n).padStart(2, '0'); }
function icsStamp(d) {
  return `${d.getFullYear()}${pad(d.getMonth()+1)}${pad(d.getDate())}T${pad(d.getHours())}${pad(d.getMinutes())}00`;
}

// Most digit-rich phone-like run in `text`, as digits (keeping a leading +), else null.
// The 1B is unreliable at copying a 10-digit number, so for "appel" we take it from the phrase.
export function extractPhone(text) {
  let best = '';
  for (const m of text.match(/\+?\d[\d . -]{4,}\d/g) || []) {
    if (m.replace(/\D/g, '').length > best.replace(/\D/g, '').length) best = m;
  }
  const digits = (best.match(/[\d+]/g) || []).join('');
  return digits.replace(/\D/g, '').length >= 6 ? digits : null;
}

export function buildDeepLink(action, platform, now) {
  switch (action.type) {
    case 'alarme':
      return { kind: 'unsupported', label: "Alarme : démo sur le Pixel uniquement (non disponible côté web)" };

    case 'appel':
      // strip spaces/separators so the tel: URI is well-formed
      return { kind: 'href', href: `tel:${(action.destinataire || '').replace(/\s/g, '')}`, label: 'Ouvrir le numéroteur' };

    case 'message':
      if (action.canal === 'email') {
        return { kind: 'href',
          href: `mailto:?subject=${enc(action.objet || '')}&body=${enc(action.corps || '')}`,
          label: 'Rédiger l’email' };
      }
      // SMS body param: iOS historically uses `&body=`, Android `?body=`.
      return { kind: 'href',
        href: platform === 'ios' ? `sms:&body=${enc(action.corps || '')}` : `sms:?body=${enc(action.corps || '')}`,
        label: 'Rédiger le SMS' };

    case 'itineraire':
      return { kind: 'href',
        href: platform === 'ios'
          ? `https://maps.apple.com/?q=${enc(action.destination)}`
          : `geo:0,0?q=${enc(action.destination)}`,
        label: 'Ouvrir l’itinéraire' };

    case 'agenda': {
      const start = resolveDatetimeFr(action.quand, now);
      const end = new Date(start.getTime() + (action.duree_min || 60) * 60000);
      const text = [
        'BEGIN:VCALENDAR', 'VERSION:2.0', 'PRODID:-//luciole-mobile//FR',
        'BEGIN:VEVENT',
        `UID:${icsStamp(start)}-${enc(action.titre)}@luciole-mobile`,
        `DTSTAMP:${icsStamp(now)}`,
        `DTSTART:${icsStamp(start)}`, `DTEND:${icsStamp(end)}`,
        `SUMMARY:${action.titre}`,
        action.lieu ? `LOCATION:${action.lieu}` : '',
        'END:VEVENT', 'END:VCALENDAR',
      ].filter(Boolean).join('\r\n') + '\r\n';   // RFC 5545: every line (incl. last) ends CRLF
      return { kind: 'ics', filename: 'evenement.ics', text, label: 'Ajouter au calendrier' };
    }

    case 'inconnu':
      return { kind: 'text', text: "Ça, je ne sais pas encore le faire. Je peux : alarme, minuteur, agenda, SMS/e-mail, itinéraire, appel, ouvrir une app/un réglage, recherche, traduction." };

    case 'minuteur':
      return { kind: 'unsupported', label: 'Minuteur : démo sur le Pixel uniquement' };

    case 'note':
      return { kind: 'download', filename: 'note.txt', text: action.texte || '', label: 'Télécharger la note' };

    case 'recherche':
      return { kind: 'href', href: `https://www.qwant.com/?q=${enc(action.requete || '')}`, label: 'Lancer la recherche' };

    default:
      return { kind: 'unsupported', label: `Action inconnue: ${action.type}` };
  }
}
