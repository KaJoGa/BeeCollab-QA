import { io } from 'socket.io-client';
import { API, createMeeting, guestToken, makeReporter, register, timedFetch, uniqueEmail } from './config.mjs';

const t = makeReporter('WebSocket Real-Time Smoke Suite');
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

async function seen(s, name, pred = () => true, timeout = 5000) {
  const end = Date.now() + timeout;
  while (Date.now() <= end) {
    const hit = s.events.find((e) => e.name === name && pred(e.payload));
    if (hit) return hit.payload;
    await new Promise((r) => setTimeout(r, 50));
  }
  return null;
}

try {
  // 0. Pre-warming probe: ensure cloud instance is awake before opening socket connections
  console.log('Probing backend health before running WebSocket smoke suite...');
  try {
    await timedFetch(`${API}/health`, {}, 60_000);
  } catch (err) {
    console.warn('Backend warmup probe warning:', err.message);
  }

  // 1. Connection without token is rejected
  const anon = io(`${API}/meetings`, { transports: ['websocket'], reconnection: false, timeout: 15_000 });
  const rejectionDetected = await new Promise((resolve) => {
    let handled = false;
    const done = (ok) => {
      if (handled) return;
      handled = true;
      clearTimeout(timer);
      resolve(ok);
    };
    const timer = setTimeout(() => done(!anon.connected), 15_000);

    anon.on('connect_error', () => done(true));
    anon.on('error', () => done(true));
    anon.on('disconnect', () => done(true));
    anon.on('connect', () => {
      setTimeout(() => done(!anon.connected), 2000);
    });
  });
  check('Unauthenticated handshake is rejected', rejectionDetected && !anon.connected);
  anon.close();

  // 2. Host and guest join meeting
  const hostToken = await register(uniqueEmail('ws-host'), 'WS Host');
  const guestTok = await guestToken('WS Guest');
  const meeting = await createMeeting(hostToken, { title: 'WS Smoke Meeting' });
  const host = await connect(hostToken, 'host');
  const guest = await connect(guestTok, 'guest');

  host.emit('meeting:join', { meetingId: meeting.id });
  const hostState = await seen(host, 'meeting:state');
  check('Host joins and receives initial room state', hostState?.participants?.some((p) => p.socketId === host.id));

  guest.emit('meeting:join', { meetingId: meeting.id });
  const guestJoined = await seen(host, 'participant:joined', (p) => p.socketId === guest.id);
  check('Guest joins and notifies existing participants', !!guestJoined);

  // 3. Real-time chat message broadcast
  guest.emit('chat:message', { meetingId: meeting.id, message: 'ping from guest' });
  const chatMsg = await seen(host, 'chat:message', (m) => m.message === 'ping from guest');
  check('Real-time chat is broadcast across sockets', !!chatMsg);

  // 4. Media toggle event
  guest.emit('media:toggle', { meetingId: meeting.id, type: 'audio', enabled: true });
  const mediaUpdated = await seen(host, 'media:updated', (m) => m.socketId === guest.id && m.type === 'audio');
  check('Media state toggle is synchronized', !!mediaUpdated);

  // 5. Hand raise queue
  guest.emit('hand:toggle', { meetingId: meeting.id, raised: true });
  const queueUpdated = await seen(host, 'queue:updated', (q) => q.length >= 1);
  check('Hand raise queue updates in real-time', !!queueUpdated);

  // 6. WebRTC SDP signaling relay
  guest.emit('webrtc:offer', { to: host.id, from: guest.id, sdp: { type: 'offer', sdp: 'v=0' } });
  const sdpRelay = await seen(host, 'webrtc:offer', (m) => m.from === guest.id);
  check('WebRTC SDP offer is relayed between peers', !!sdpRelay);

  // 7. Host ends meeting and cleans up room
  host.emit('meeting:end', { meetingId: meeting.id });
  const ended = await seen(guest, 'meeting:ended', (m) => m.meetingId === meeting.id);
  check('Host ends meeting and terminates session for all participants', !!ended);
} catch (e) {
  check(`Smoke suite executed without fatal errors (${e.message})`, false);
} finally {
  sockets.forEach((s) => s.close());
}
t.finish();
