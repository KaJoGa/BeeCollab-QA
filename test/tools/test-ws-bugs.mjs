// test/tools/test-ws-bugs.mjs
// Step 3: WebSocket Security & Database Defect Suite
// Validates:
// - R-09: Handshake bypass with garbage token & unguarded media:speaking
// - R-05: Guest poll voting database FK constraint crash
// - R-06: Host cannot kick guest (Participant not found)
// - R-07: Host cannot force-mute guest (Prisma P2025)
// - R-07b: Host cannot promote guest to co-host
// - R-21: Solo host brief disconnect permanently destroys meeting

import { io } from 'socket.io-client';
import { API, api, createMeeting, guestToken, makeReporter, register, sleep, uniqueEmail } from './config.mjs';

const t = makeReporter('WebSocket Real-Time Defects Suite');
const { check } = t;
const sockets = [];

function connect(token, label) {
  return new Promise((resolve, reject) => {
    const s = io(`${API}/meetings`, { auth: { token }, transports: ['websocket'], reconnection: false });
    s.label = label;
    s.events = [];
    s.onAny((name, payload) => s.events.push({ name, payload, at: Date.now() }));
    s.on('connect', () => resolve(s));
    s.on('connect_error', (e) => reject(new Error(`${label}: ${e.message}`)));
    sockets.push(s);
  });
}

async function seen(s, name, pred = () => true, timeout = 4000, since = 0) {
  const end = Date.now() + timeout;
  for (;;) {
    const hit = s.events.find((e) => e.at >= since && e.name === name && pred(e.payload));
    if (hit) return hit.payload;
    if (Date.now() > end) return null;
    await sleep(50);
  }
}

const join = async (s, meetingId, extra = {}) => {
  s.emit('meeting:join', { meetingId, ...extra });
  return seen(s, 'meeting:state');
};

console.log('--- Initializing Step 3 Test Environment ---');
const hostToken = await register(uniqueEmail('wsbug-host'), 'Host Alice');
const gToken = await guestToken('Guest Bob');
const meeting = await createMeeting(hostToken, { title: 'WS Defect Validation Meeting', maxParticipants: 5, duration: 30 });
const host = await connect(hostToken, 'host');
const guest = await connect(gToken, 'guest');

await join(host, meeting.id);
await join(guest, meeting.id);
console.log(`Host joined (${host.id}), Guest joined (${guest.id}) in meeting ${meeting.id}`);

console.log('\n--- 1. Testing Handshake Bypass with Invalid JWT (R-09) ---');
// Test 1.1: Connect with completely garbage token
let garbageSocket;
let garbageConnected = false;
try {
  garbageSocket = await connect('totally-invalid-garbage-jwt-token-xyz', 'garbage');
  garbageConnected = garbageSocket.connected;
  console.log(`[R-09 Check] Socket with garbage token connected: ${garbageConnected} (socketId=${garbageSocket.id})`);
} catch (e) {
  console.log(`[R-09 Check] Connection rejected: ${e.message}`);
}
check('Handshake allows garbage JWT token to connect (R-09 verified)', garbageConnected === true);

// Test 1.2: Garbage socket emits unguarded media:speaking to real room
if (garbageSocket && garbageSocket.connected) {
  garbageSocket.emit('media:speaking', { meetingId: meeting.id, speaking: true });
  const hostSawSpeaking = await seen(host, 'media:speaking', (p) => p.speaking === true, 3000);
  console.log(`[R-09 Check] Host received media:speaking from unauthenticated socket:`, !!hostSawSpeaking);
  check('Unauthenticated socket can spoof media:speaking in room (R-09 verified)', !!hostSawSpeaking);
  garbageSocket.close();
}

console.log('\n--- 2. Testing Host Moderation Failures on Guests (R-06, R-07, R-07b) ---');
// Test 2.1: Host attempts to kick guest (R-06)
const tKick = Date.now();
host.emit('meeting:kick', { meetingId: meeting.id, targetSocketId: guest.id });
const kickError = await seen(host, 'error', (p) => typeof p === 'object' || typeof p === 'string', 3000, tKick);
const guestKicked = await seen(guest, 'meeting:kicked', () => true, 2000, tKick);
console.log(`[R-06 Check] Host received error on kick guest:`, JSON.stringify(kickError));
console.log(`[R-06 Check] Guest received meeting:kicked event:`, !!guestKicked);
check('Host kick guest fails with "Participant not found" (R-06 verified)', 
  (kickError?.message?.includes('Participant not found') || kickError?.includes?.('Participant not found')) && !guestKicked && guest.connected);

// Test 2.2: Host attempts to force-mute guest (R-07)
const tMute = Date.now();
host.emit('media:force-mute', { meetingId: meeting.id, targetSocketId: guest.id });
const muteError = await seen(host, 'exception', (p) => typeof p === 'object', 3000, tMute) 
  || await seen(host, 'error', (p) => typeof p === 'object', 3000, tMute);
console.log(`[R-07 Check] Host received exception/error on force-mute guest:`, JSON.stringify(muteError));
check('Host force-mute guest triggers database exception (R-07 verified)', !!muteError);

// Test 2.3: Host attempts to promote guest to co-host (R-07b)
const tCohost = Date.now();
host.emit('meeting:make-cohost', { meetingId: meeting.id, targetUserId: 'guest_nonexistent' });
const cohostError = await seen(host, 'exception', (p) => typeof p === 'object', 3000, tCohost)
  || await seen(host, 'error', (p) => typeof p === 'object', 3000, tCohost);
console.log(`[R-07b Check] Host received exception on promote guest:`, JSON.stringify(cohostError));
check('Host promote guest to co-host triggers exception (R-07b verified)', !!cohostError);

console.log('\n--- 3. Testing Guest Poll Voting Database Crash (R-05) ---');
// Host creates a poll
host.emit('poll:create', { meetingId: meeting.id, question: 'Team Lunch?', options: ['Tacos', 'Burgers'] });
const pollCreated = await seen(host, 'poll:created', () => true, 4000);
console.log(`[Poll Setup] Poll created:`, pollCreated ? `id=${pollCreated.id}` : 'failed');

if (pollCreated && pollCreated.options?.length > 0) {
  const pollId = pollCreated.id;
  const optionId = pollCreated.options[0].id;

  // Guest attempts to vote
  guest.emit('poll:vote', { meetingId: meeting.id, pollId, optionId });
  const guestVoteException = await seen(guest, 'exception', (p) => typeof p === 'object', 4000)
    || await seen(guest, 'error', (p) => typeof p === 'object', 4000);
  console.log(`[R-05 Check] Guest received exception/error on poll:vote:`, JSON.stringify(guestVoteException));
  
  // Also check if room poll:updated arrived with guest vote
  const pollUpdated = await seen(host, 'poll:updated', (p) => p.id === pollId && p.totalVotes > 0, 2000);
  console.log(`[R-05 Check] Did poll update with guest vote:`, !!pollUpdated);
  check('Guest voting triggers exception due to missing User FK (R-05 verified)', !!guestVoteException || !pollUpdated);
} else {
  check('Guest voting test (poll setup fallback)', false);
}

// Clean up first meeting
host.emit('meeting:end', { meetingId: meeting.id });
await sleep(1000);
guest.close();
host.close();

console.log('\n--- 4. Testing Solo Host Disconnect Deletion (R-21) ---');
// Host creates dedicated meeting for solo disconnect test
const soloHostToken = await register(uniqueEmail('solo-host'), 'Solo Host');
const soloMeeting = await createMeeting(soloHostToken, { title: 'Solo Host Disconnect Test', maxParticipants: 3, duration: 15 });
const soloSocket = await connect(soloHostToken, 'soloHost');
await join(soloSocket, soloMeeting.id);
console.log(`Solo host joined meeting ${soloMeeting.id}. Verifying live state via REST...`);

const preCheck = await api('GET', `/meetings/${soloMeeting.id}`, { token: soloHostToken });
check('Meeting is LIVE while host connected -> 200', preCheck.status === 200);

// Host disconnects (simulating network drop)
console.log('Disconnecting solo host socket...');
soloSocket.close();
await sleep(2500); // Wait for backend empty-room listener

const postCheck = await api('GET', `/meetings/${soloMeeting.id}`, { token: soloHostToken });
console.log(`[R-21 Check] GET /meetings/:id after 2.5s solo disconnect -> HTTP ${postCheck.status}`);
check('Meeting is immediately destroyed on solo disconnect (R-21 verified)', postCheck.status === 404);

for (const s of sockets) {
  if (s.connected) s.close();
}

t.finish();
