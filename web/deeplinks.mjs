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

const CALL_PREFIX = /^\s*(?:appelle|appeler|appelles|appelez|t[ée]l[ée]phone[rz]?|compose[rz]?|joins)\s*(?:à|au|aux|le|la|l['’])?\s*/i;
// Name to call when no number is spoken: 'appelle Paul Maudet' -> 'Paul Maudet'.
export function callTarget(phrase) {
  return (phrase || '').replace(CALL_PREFIX, '').trim();
}

export function buildDeepLink(action, platform, now, lang) {
  const L = (fr, en) => (lang === 'en' ? en : fr);
  switch (action.type) {
    case 'alarme':
      return { kind: 'unsupported', label: L("Alarme : s'exécute sur le Pixel (réveil).", 'Alarm: runs on the Pixel (wake-up).') };

    case 'appel': {
      // Sur PC, l'appel n'a pas de sens concret -> carte « sur le Pixel ». Sur mobile : tel: (numéro) ou carnet (nom).
      if (platform === 'desktop')
        return { kind: 'unsupported', label: L(`Appel de « ${action.destinataire || ''} » : s'exécute sur le Pixel (numéroteur).`, `Call to "${action.destinataire || ''}": runs on the Pixel (dialer).`) };
      const num = (action.destinataire || '').replace(/[^\d+]/g, '');
      return /\d/.test(num)
        ? { kind: 'href', href: `tel:${num}`, label: L('Ouvrir le numéroteur', 'Open the dialer') }
        : { kind: 'text', text: L(`Appel de « ${action.destinataire} » : recherche dans le carnet d’adresses (démo sur le Pixel).`, `Call to "${action.destinataire}": contact lookup (Pixel demo).`) };
    }

    case 'message':
      if (action.canal === 'email') {
        return { kind: 'href',
          href: `mailto:?subject=${enc(action.objet || '')}&body=${enc(action.corps || '')}`,
          label: L('Rédiger l’email', 'Compose the email') };
      }
      // SMS : natif mobile. Sur PC -> carte « sur le Pixel ».
      if (platform === 'desktop')
        return { kind: 'unsupported', label: L("SMS : s'exécute sur le Pixel (ouvre Messages).", 'SMS: runs on the Pixel (opens Messages).') };
      // iOS historically uses `&body=`, Android `?body=`.
      return { kind: 'href',
        href: platform === 'ios' ? `sms:&body=${enc(action.corps || '')}` : `sms:?body=${enc(action.corps || '')}`,
        label: L('Rédiger le SMS', 'Compose the SMS') };

    case 'itineraire': {
      const dst = enc(action.destination);
      const gmode = ({ voiture: 'driving', transit: 'transit', pieton: 'walking' })[action.mode] || 'transit';
      const href = platform === 'ios' ? `https://maps.apple.com/?q=${dst}`
        : platform === 'android' ? `geo:0,0?q=${dst}`
        : `https://www.google.com/maps/dir/?api=1&destination=${dst}&travelmode=${gmode}`;
      return { kind: 'href', href, label: L('Ouvrir l’itinéraire', 'Open the directions') };
    }

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
      return { kind: 'ics', filename: 'evenement.ics', text, label: L('Ajouter au calendrier', 'Add to calendar') };
    }

    case 'inconnu':
      return { kind: 'text', text: L("Ça, je ne sais pas encore le faire. Je peux : alarme, minuteur, agenda, SMS/e-mail, itinéraire, appel, ouvrir une app/un réglage, recherche, traduction.", "I can't do that yet. I can: alarm, timer, calendar, SMS/email, directions, call, open an app/setting, search, translation.") };

    case 'minuteur':
      return { kind: 'unsupported', label: L("Minuteur : s'exécute sur le Pixel (compte à rebours).", 'Timer: runs on the Pixel (countdown).') };

    case 'note':
      return { kind: 'download', filename: 'note.txt', text: action.texte || '', label: L('Télécharger la note', 'Download the note') };

    case 'recherche':
      return { kind: 'href', href: `https://www.qwant.com/?q=${enc(action.requete || '')}`, label: L('Lancer la recherche', 'Run the search') };

    case 'ouvrir': {
      // Sur PC, « appareil photo » -> capture par la webcam du Mac (getUserMedia, contexte localhost sécurisé).
      if (action.cible === 'appareil_photo' && platform === 'desktop')
        return { kind: 'webcam', label: L('Prendre la photo', 'Take the photo') };
      const WEB = { youtube: 'https://m.youtube.com', maps: 'https://maps.google.com', wikipedia: 'https://fr.wikipedia.org' };
      const href = WEB[action.cible];
      return href
        ? { kind: 'href', href, label: L(`Ouvrir ${action.cible}`, `Open ${action.cible}`) }
        : { kind: 'unsupported', label: L(`« Ouvrir ${action.cible} » : s'exécute sur le Pixel.`, `"Open ${action.cible}": runs on the Pixel.`) };
    }

    case 'traduction':
      return { kind: 'text', text: action.resultat || '' };

    default:
      return { kind: 'unsupported', label: L(`Action inconnue : ${action.type}`, `Unknown action: ${action.type}`) };
  }
}
