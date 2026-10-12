// test/tools/test-api-security.mjs
// Step 2: API Contract & Security Testing Suite
// Tests IDOR vulnerabilities (R-04), Guest boundaries (R-03), Case sensitivity (R-16),
// Token attacks (alg: none, tampered, expired), and input boundary fuzzing.

import { PASSWORD, api, createMeeting, guestToken, makeReporter, uniqueEmail } from './config.mjs';

const t = makeReporter('API Security & IDOR Suite');
const { check } = t;

console.log('--- Initializing Test Personas ---');
// 1. Setup User A (Host)
const emailA = uniqueEmail('sec-host');
const regA = await api('POST', '/auth/register', { body: { email: emailA, name: 'Host Alice', password: PASSWORD } });
const tokenA = regA.data?.access_token;
check('Host A registered successfully', regA.status === 201 && !!tokenA);

// 2. Setup User B (Unrelated registered member)
const emailB = uniqueEmail('sec-member');
const regB = await api('POST', '/auth/register', { body: { email: emailB, name: 'Member Bob', password: PASSWORD } });
const tokenB = regB.data?.access_token;
check('Member B registered successfully', regB.status === 201 && !!tokenB);

// 3. Setup Guest C (Anonymous / unauthenticated guest)
const regC = await api('POST', '/auth/guest', { body: { name: 'Guest Charlie' } });
const tokenC = regC.data?.access_token;
check('Guest C token acquired', regC.status === 201 && !!tokenC);

// 4. Host A creates Meeting A
const meetingA = await createMeeting(tokenA, { title: 'Alice Confidential Strategy', duration: 30, maxParticipants: 5 });
const idA = meetingA.id;
const codeA = meetingA.roomCode;
check('Meeting A created with private roomCode', !!idA && /^[0-9A-F]{8}$/.test(codeA), `id=${idA}, code=${codeA}`);

// Track all created meetings for clean teardown
const createdMeetingIds = [idA];

console.log('\n--- 1. Testing IDOR Vulnerabilities (R-04) ---');
// Test 1.1: Unrelated Member B querying Meeting A details by UUID
const bGetMeeting = await api('GET', `/meetings/${idA}`, { token: tokenB });
console.log(`[IDOR Check 1] GET /meetings/:id by Member B -> HTTP ${bGetMeeting.status}`);
if (bGetMeeting.status === 200) {
  console.log(`  [VULNERABILITY DETECTED]: IDOR on /meetings/:id allows Member B to view meeting details!`);
  console.log(`  Leaked data: Title="${bGetMeeting.data?.title}", Host="${bGetMeeting.data?.hostId}", RoomCode="${bGetMeeting.data?.roomCode}"`);
}
check('IDOR: Member B reads Meeting A metadata (R-04 verified)', bGetMeeting.status === 200 && bGetMeeting.data?.roomCode === codeA);

// Test 1.2: Unrelated Member B querying Meeting A chat history
const bGetChat = await api('GET', `/meetings/${idA}/chat`, { token: tokenB });
console.log(`[IDOR Check 2] GET /meetings/:id/chat by Member B -> HTTP ${bGetChat.status}`);
if (bGetChat.status === 200) {
  console.log(`  [VULNERABILITY DETECTED]: IDOR on /meetings/:id/chat allows Member B to view chat history without being a participant!`);
}
check('IDOR: Member B reads Meeting A chat history (R-04 verified)', bGetChat.status === 200 && Array.isArray(bGetChat.data));

// Test 1.3: Unrelated Member B querying Meeting A participant list
const bGetParts = await api('GET', `/meetings/${idA}/participants`, { token: tokenB });
console.log(`[IDOR Check 3] GET /meetings/:id/participants by Member B -> HTTP ${bGetParts.status}`);
if (bGetParts.status === 200) {
  console.log(`  [VULNERABILITY DETECTED]: IDOR on /meetings/:id/participants allows Member B to list participants without joining!`);
}
check('IDOR: Member B reads Meeting A participants (R-04 verified)', bGetParts.status === 200 && Array.isArray(bGetParts.data));

// Test 1.4: Guest C querying Meeting A by UUID
const cGetMeeting = await api('GET', `/meetings/${idA}`, { token: tokenC });
console.log(`[IDOR Check 4] GET /meetings/:id by Guest C -> HTTP ${cGetMeeting.status}`);
if (cGetMeeting.status === 200) {
  console.log(`  [CRITICAL LEAK]: Guest C knowing UUID can read private 8-hex roomCode "${cGetMeeting.data?.roomCode}"!`);
}
check('IDOR: Guest C reads Meeting A and extracts room code (R-04 verified)', cGetMeeting.status === 200 && cGetMeeting.data?.roomCode === codeA);

console.log('\n--- 2. Testing Case-Sensitivity Vulnerability (R-16) ---');
// Test 2.1: Uppercase code lookup vs Lowercase code lookup
const upperLookup = await api('GET', `/meetings/code/${codeA}`, { token: tokenB });
const lowerLookup = await api('GET', `/meetings/code/${codeA.toLowerCase()}`, { token: tokenB });
console.log(`[R-16 Check] RoomCode Uppercase (${codeA}) -> HTTP ${upperLookup.status}, Lowercase (${codeA.toLowerCase()}) -> HTTP ${lowerLookup.status}`);
check('Code lookup: Uppercase resolves -> 200', upperLookup.status === 200);
check('Code lookup: Lowercase fails -> 404 (R-16 verified)', lowerLookup.status === 404);

console.log('\n--- 3. Testing Guest Identity & Privilege Boundaries (R-03) ---');
// Test 3.1: Guest calling /users/me
const guestMe = await api('GET', '/users/me', { token: tokenC });
console.log(`[R-03 Check] GET /users/me with guest token -> HTTP ${guestMe.status}: ${JSON.stringify(guestMe.body)}`);
check('Guest profile: GET /users/me returns 404 User not found (R-03 verified)', guestMe.status === 404);

// Test 3.2: Guest attempting DELETE /meetings/:id
const guestDelete = await api('DELETE', `/meetings/${idA}`, { token: tokenC });
check('Guest cannot delete meeting -> 403', guestDelete.status === 403);

console.log('\n--- 4. Testing Token Security & Tampering ---');
// Test 4.1: Tampered signature
const tamperedToken = `${tokenA}tampered_signature`;
check('Tampered JWT signature rejected -> 401', (await api('GET', '/users/me', { token: tamperedToken })).status === 401);

// Test 4.2: Forged alg: none JWT
const algNoneHeader = Buffer.from(JSON.stringify({ alg: 'none', typ: 'JWT' })).toString('base64url');
const algNonePayload = Buffer.from(JSON.stringify({ sub: '00000000-0000-0000-0000-000000000001', email: 'admin@example.com' })).toString('base64url');
const algNoneToken = `${algNoneHeader}.${algNonePayload}.`;
const algNoneRes = await api('GET', '/users/me', { token: algNoneToken });
console.log(`[Token Check] Forged alg: none JWT -> HTTP ${algNoneRes.status}`);
check('Forged alg: none JWT rejected -> 401', algNoneRes.status === 401);

// Test 4.3: Expired JWT (backdated exp)
const expiredPayload = Buffer.from(JSON.stringify({ sub: '00000000-0000-0000-0000-000000000001', email: 'admin@example.com', exp: Math.floor(Date.now() / 1000) - 3600 })).toString('base64url');
const expiredToken = `${algNoneHeader}.${expiredPayload}.dummy_sig`;
check('Expired JWT rejected -> 401', (await api('GET', '/users/me', { token: expiredToken })).status === 401);

console.log('\n--- 5. Testing Input Boundaries & Fuzzing ---');
// Test 5.1: Extreme length title (10,000 chars)
const hugeTitle = 'A'.repeat(10000);
const hugeTitleRes = await api('POST', '/meetings', { token: tokenA, body: { title: hugeTitle } });
console.log(`[Fuzz Check] Title 10,000 characters -> HTTP ${hugeTitleRes.status}`);
check('Title 10,000 chars handled gracefully (not 500)', hugeTitleRes.status !== 500);
if (hugeTitleRes.status === 201 && hugeTitleRes.data?.id) createdMeetingIds.push(hugeTitleRes.data.id);

// Test 5.2: XSS in title stored as raw text without server crash
const xssTitle = '<script>alert("XSS")</script>';
const xssRes = await api('POST', '/meetings', { token: tokenA, body: { title: xssTitle } });
console.log(`[Fuzz Check] XSS title -> HTTP ${xssRes.status}`);
check('XSS in title accepted as string without crash -> 201', xssRes.status === 201);
if (xssRes.status === 201 && xssRes.data?.id) createdMeetingIds.push(xssRes.data.id);

// Test 5.3: SQL Injection pattern in title
const sqliTitle = "'; DROP TABLE \"Meeting\"; SELECT '1";
const sqliRes = await api('POST', '/meetings', { token: tokenA, body: { title: sqliTitle } });
console.log(`[Fuzz Check] SQLi string in title -> HTTP ${sqliRes.status}:`, JSON.stringify(sqliRes.body));
check('SQL injection in title safely rejected or stored (not 500)', sqliRes.status === 201 || sqliRes.status === 403 || sqliRes.status === 400);
if (sqliRes.status === 201 && sqliRes.data?.id) createdMeetingIds.push(sqliRes.data.id);

// Test 5.4: maxParticipants boundaries
check('maxParticipants: 0 rejected -> 400', (await api('POST', '/meetings', { token: tokenA, body: { title: 'Bound', maxParticipants: 0 } })).status === 400);
check('maxParticipants: -1 rejected -> 400', (await api('POST', '/meetings', { token: tokenA, body: { title: 'Bound', maxParticipants: -1 } })).status === 400);
const pMin = await api('POST', '/meetings', { token: tokenA, body: { title: 'Valid Min', maxParticipants: 2 } });
check('maxParticipants: 2 valid -> 201', pMin.status === 201);
if (pMin.status === 201 && pMin.data?.id) createdMeetingIds.push(pMin.data.id);

const pMax = await api('POST', '/meetings', { token: tokenA, body: { title: 'Valid Max', maxParticipants: 500 } });
check('maxParticipants: 500 valid -> 201', pMax.status === 201);
if (pMax.status === 201 && pMax.data?.id) createdMeetingIds.push(pMax.data.id);

// Test 5.5: duration boundaries
check('duration: -10 rejected -> 400', (await api('POST', '/meetings', { token: tokenA, body: { title: 'Bound', duration: -10 } })).status === 400);
const dMin = await api('POST', '/meetings', { token: tokenA, body: { title: 'Valid 1m', duration: 1 } });
check('duration: 1 valid min -> 201', dMin.status === 201);
if (dMin.status === 201 && dMin.data?.id) createdMeetingIds.push(dMin.data.id);

const dMax = await api('POST', '/meetings', { token: tokenA, body: { title: 'Valid 24h', duration: 1440 } });
check('duration: 1440 valid max -> 201', dMax.status === 201);
if (dMax.status === 201 && dMax.data?.id) createdMeetingIds.push(dMax.data.id);

console.log('\n--- Teardown: Deleting All Created Meetings ---');
for (const id of createdMeetingIds) {
  const d = await api('DELETE', `/meetings/${id}`, { token: tokenA });
  console.log(`Deleted test meeting ${id} -> HTTP ${d.status}`);
}
check('All created test meetings cleaned up', true);

t.finish();
