// web/datetime_fr.mjs
const WEEKDAYS = { lundi:1, mardi:2, mercredi:3, jeudi:4, vendredi:5, samedi:6, dimanche:0 };

export function resolveDatetimeFr(expr, now) {
  const e = expr.trim().toLowerCase();
  let m;

  if ((m = e.match(/^dans (\d+) (minute|heure)s?$/))) {
    const n = +m[1];
    const d = new Date(now);
    if (m[2] === 'minute') d.setMinutes(d.getMinutes() + n);
    else d.setHours(d.getHours() + n);
    return d;
  }

  if ((m = e.match(/^(?:(aujourd'hui|demain|après-demain) )?(\d{1,2}):(\d{2})$/))) {
    const d = new Date(now);
    d.setSeconds(0, 0);
    d.setHours(+m[2], +m[3]);
    if (m[1] === 'demain') d.setDate(d.getDate() + 1);
    else if (m[1] === 'après-demain') d.setDate(d.getDate() + 2);
    return d;
  }

  if ((m = e.match(/^(lundi|mardi|mercredi|jeudi|vendredi|samedi|dimanche) (\d{1,2}):(\d{2})$/))) {
    const target = WEEKDAYS[m[1]];
    let delta = (target - now.getDay() + 7) % 7;
    if (delta === 0) delta = 7;
    const d = new Date(now);
    d.setDate(d.getDate() + delta);
    d.setHours(+m[2], +m[3], 0, 0);
    return d;
  }

  throw new Error(`expression temporelle non supportée: ${expr}`);
}
