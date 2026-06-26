// web/tests/deeplinks.test.mjs
import { test } from 'node:test';
import assert from 'node:assert/strict';
import { buildDeepLink, extractPhone, callTarget } from '../deeplinks.mjs';

const NOW = new Date(2026, 5, 25, 9, 0, 0);

test('alarme is unsupported on web', () => {
  assert.equal(buildDeepLink({type:'alarme',heure:'14:00',libelle:'x'}, 'android', NOW).kind, 'unsupported');
});
test('email mailto both platforms', () => {
  for (const p of ['ios','android']) {
    const r = buildDeepLink({type:'message',canal:'email',objet:'O',corps:'C'}, p, NOW);
    assert.equal(r.kind, 'href');
    assert.match(r.href, /^mailto:\?/);
    assert.match(r.href, /subject=O/);
    assert.match(r.href, /body=C/);
  }
});
test('sms body separator differs by platform', () => {
  assert.match(buildDeepLink({type:'message',canal:'sms',corps:'C'}, 'ios', NOW).href, /^sms:&body=/);
  assert.match(buildDeepLink({type:'message',canal:'sms',corps:'C'}, 'android', NOW).href, /^sms:\?body=/);
});
test('tel both platforms', () => {
  assert.equal(buildDeepLink({type:'appel',destinataire:'0612'}, 'ios', NOW).href, 'tel:0612');
});
test('appel by contact name is Pixel-only text (no tel:)', () => {
  const r = buildDeepLink({type:'appel',destinataire:'Michel-Marie Maudet'}, 'android', NOW);
  assert.equal(r.kind, 'text');
  assert.match(r.text, /Pixel/);
});
test('callTarget strips the call verb', () => {
  assert.equal(callTarget('appelle Paul Maudet'), 'Paul Maudet');
  assert.equal(callTarget('téléphone à Marie'), 'Marie');
});
test('itineraire differs by platform', () => {
  assert.match(buildDeepLink({type:'itineraire',destination:'Gare'}, 'android', NOW).href, /^geo:0,0\?q=Gare/);
  assert.match(buildDeepLink({type:'itineraire',destination:'Gare'}, 'ios', NOW).href, /maps\.apple\.com/);
});
test('agenda produces ics with DTSTART', () => {
  const r = buildDeepLink({type:'agenda',titre:'R',quand:'demain 10:00'}, 'ios', NOW);
  assert.equal(r.kind, 'ics');
  assert.match(r.text, /BEGIN:VCALENDAR/);
  assert.match(r.text, /DTSTART:20260626T100000/);
  assert.match(r.text, /SUMMARY:R/);
});
test('extractPhone pulls the number from the phrase', () => {
  assert.equal(extractPhone('appelle le 07 11 22 33 44'), '0711223344');
  assert.equal(extractPhone('téléphone au 01 23 45 67 89'), '0123456789');
  assert.equal(extractPhone('compose le 04.78.12.34.56'), '0478123456');
  assert.equal(extractPhone('appelle le +33 6 12 34 56 78'), '+33612345678');
  assert.equal(extractPhone('appelle le 06 12 34 56 78 avant 9h'), '0612345678');
});
test('extractPhone returns null when no real number', () => {
  assert.equal(extractPhone('appelle Paul'), null);
  assert.equal(extractPhone('rappelle-moi à 14h'), null);
});
test('inconnu renders a fixed text message', () => {
  const r = buildDeepLink({type:'inconnu'}, 'android', NOW);
  assert.equal(r.kind, 'text');
  assert.match(r.text, /je ne sais pas/i);
});
test('minuteur is unsupported on web (Pixel only)', () => {
  assert.equal(buildDeepLink({type:'minuteur',duree_min:10}, 'android', NOW).kind, 'unsupported');
});
test('note produces a downloadable .txt', () => {
  const r = buildDeepLink({type:'note',texte:'acheter du pain'}, 'android', NOW);
  assert.equal(r.kind, 'download');
  assert.equal(r.filename, 'note.txt');
  assert.match(r.text, /acheter du pain/);
});
test('recherche opens Qwant with the query', () => {
  const r = buildDeepLink({type:'recherche',requete:'tour Eiffel'}, 'android', NOW);
  assert.equal(r.kind, 'href');
  assert.match(r.href, /^https:\/\/www\.qwant\.com\/\?q=/);
  assert.match(r.href, /tour%20Eiffel|tour\+Eiffel/);
});
test('ouvrir youtube -> https href, bluetooth -> unsupported', () => {
  assert.match(buildDeepLink({type:'ouvrir',cible:'youtube'}, 'android', NOW).href, /youtube\.com/);
  assert.equal(buildDeepLink({type:'ouvrir',cible:'bluetooth'}, 'android', NOW).kind, 'unsupported');
});
test('traduction shows the resultat as text', () => {
  const r = buildDeepLink({type:'traduction',texte:'bonjour',cible:'anglais',resultat:'hello'}, 'android', NOW);
  assert.equal(r.kind, 'text');
  assert.equal(r.text, 'hello');
});
