// web/tests/deeplinks.test.mjs
import { test } from 'node:test';
import assert from 'node:assert/strict';
import { buildDeepLink } from '../deeplinks.mjs';

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
