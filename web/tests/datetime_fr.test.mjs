// web/tests/datetime_fr.test.mjs
import { test } from 'node:test';
import assert from 'node:assert/strict';
import { resolveDatetimeFr } from '../datetime_fr.mjs';

const NOW = new Date(2026, 5, 25, 9, 0, 0); // months are 0-based: 5 = June

test('today hh:mm', () => {
  assert.equal(resolveDatetimeFr('14:00', NOW).getTime(), new Date(2026, 5, 25, 14, 0).getTime());
});
test('demain', () => {
  assert.equal(resolveDatetimeFr('demain 10:30', NOW).getTime(), new Date(2026, 5, 26, 10, 30).getTime());
});
test('weekday next occurrence', () => {
  assert.equal(resolveDatetimeFr('lundi 09:15', NOW).getTime(), new Date(2026, 5, 29, 9, 15).getTime());
});
test('dans minutes', () => {
  assert.equal(resolveDatetimeFr('dans 20 minutes', NOW).getTime(), new Date(2026, 5, 25, 9, 20).getTime());
});
test('unknown throws', () => {
  assert.throws(() => resolveDatetimeFr('la semaine prochaine', NOW));
});
