const trim = (u) => u.replace(/\/+$/, '');
export const API = trim(process.env.API_URL ?? 'https://beecollab-rwbj.onrender.com');
export const uniqueEmail = (prefix) => `${prefix}-${Date.now()}-${Math.floor(Math.random() * 1e4)}@qa.beecollab.test`;

export async function timedFetch(url, init = {}, ms = 60_000) {
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), ms);
  try {
    return await fetch(url, { ...init, signal: controller.signal });
  } finally {
    clearTimeout(timer);
  }
}

export async function api(method, path, { body, token } = {}) {
  const res = await timedFetch(API + path, {
    method,
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  let json = {};
  try { json = await res.json(); } catch {}
  return { status: res.status, data: json?.data };
}

export async function register(email, name, password = 'QaPassw0rd!') {
  const r = await api('POST', '/auth/register', { body: { email, name, password } });
  if (r.status === 201) return r.data.access_token;
  const l = await api('POST', '/auth/login', { body: { email, password } });
  return l.data.access_token;
}

export async function guestToken(name = 'Guest QA') {
  const r = await api('POST', '/auth/guest', { body: { name } });
  return r.data.access_token;
}

export async function createMeeting(token, overrides = {}) {
  const r = await api('POST', '/meetings', {
    token,
    body: { title: 'QA Meeting', duration: 30, maxParticipants: 10, ...overrides },
  });
  return r.data;
}

export function makeReporter(title) {
  const results = [];
  console.log(`\n== ${title} == (api=${API})`);
  return {
    check(name, ok, detail = '') {
      results.push({ name, ok: Boolean(ok) });
      console.log(`${ok ? 'PASS' : 'FAIL'}  ${name}${!ok && detail ? ` -> ${detail}` : ''}`);
    },
    finish() {
      const failed = results.filter((r) => !r.ok).length;
      console.log(`\n${results.length - failed}/${results.length} passed${failed ? `, ${failed} FAILED` : ''}`);
      process.exitCode = failed ? 1 : 0;
    },
  };
}
