import { io } from 'socket.io-client';
import { execSync } from 'node:child_process';
import { readFileSync } from 'node:fs';
import { API, createMeeting, register, uniqueEmail, sleep, api } from './config.mjs';

const conn = (t) => new Promise((res) => {
  const s = io(`${API}/meetings`, { auth: { token: t }, transports: ['websocket'], reconnection: false });
  s.on('connect', () => res(s));
});
const sfx = Date.now();
const qa1 = await register(uniqueEmail('sqlqa1'), 'SQL QA One');
const qa2 = await register(uniqueEmail('sqlqa2'), 'SQL QA Two');
const real = await register(`sqlreal-${sfx}@example.org`, 'SQL Real');
const mQa = await createMeeting(qa1, { title: 'SQL QA meeting' });
const mReal = await createMeeting(real, { title: 'SQL real meeting' });
const socks = [];
for (const [tok, mid, msg] of [[qa1, mQa.id, 'qa chat'], [real, mQa.id, 'real in qa'], [qa2, mReal.id, 'qa in real'], [real, mReal.id, 'real chat']]) {
  const s = await conn(tok);
  s.emit('meeting:join', { meetingId: mid });
  await sleep(500);
  s.emit('chat:message', { meetingId: mid, message: msg });
  await sleep(300);
  socks.push(s);
}
socks[0].emit('poll:create', { meetingId: mQa.id, question: 'q?', options: ['a', 'b'] });
await sleep(500);
socks[3].emit('poll:create', { meetingId: mReal.id, question: 'real poll?', options: ['a', 'b'] });
await sleep(500);
socks[0].emit('agenda:create', { meetingId: mQa.id, items: [{ title: 'x', duration: 60 }] });
await sleep(400);
socks[2].emit('reaction:send', { meetingId: mReal.id, type: 'like', anonymous: false });
await sleep(400);

const psql = (sql) => execSync('docker exec -i beecollab-db psql -U beecollab -d beecollab -tA', { input: sql }).toString().trim();
const counts = () => psql(
  `select 'users', count(*) from "User" union all select 'meetings', count(*) from "Meeting" union all select 'participants', count(*) from "Participant" union all select 'chat', count(*) from "ChatMessage" union all select 'polls', count(*) from "Poll" union all select 'agendas', count(*) from "Agenda" union all select 'reactions', count(*) from "Reaction"`,
).split(/\r?\n/).join('  ');
const qaUsers = () => psql(`select count(*) from "User" where email like '%@qa.beecollab.test'`);

console.log('before sql :', counts(), '| qa users:', qaUsers());
psql(readFileSync(new URL('./cleanup.sql', import.meta.url), 'utf8'));
console.log('after sql  :', counts(), '| qa users:', qaUsers());
console.log('real meeting still there (expect 200):', (await api('GET', `/meetings/${mReal.id}`, { token: real })).status);
const chat = (await api('GET', `/meetings/${mReal.id}/chat`, { token: real })).data.map((m) => m.message);
console.log('real meeting chat (expect only "real chat"):', JSON.stringify(chat));
console.log('real poll in real meeting (expect 1):', psql(`select count(*) from "Poll" where "meetingId"='${mReal.id}'`));
console.log('reaction in real meeting (expect 1):', psql(`select count(*) from "Reaction" where "meetingId"='${mReal.id}'`));
console.log('real user row (expect 1):', psql(`select count(*) from "User" where email='sqlreal-${sfx}@example.org'`));
socks.forEach((s) => s.close());
await sleep(500);
