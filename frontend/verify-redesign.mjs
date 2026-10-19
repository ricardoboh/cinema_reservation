// Run with the dev backend, Vite, and a local Chromium CDP listener on 9222.
// Uses Node 22 built-ins; creates real reservations and cancels confirmed ones.
import fs from 'node:fs'
import assert from 'node:assert/strict'
const origin = 'http://127.0.0.1:5173'
const target = await (await fetch('http://127.0.0.1:9222/json/new?about:blank', { method: 'PUT' })).json()
const ws = new WebSocket(target.webSocketDebuggerUrl)
await new Promise((resolve, reject) => { ws.onopen = resolve; ws.onerror = reject })
let sequence = 0
const pending = new Map(), errors = [], report = []
ws.onmessage = ({ data }) => { const event = JSON.parse(data); if (event.id) { const p = pending.get(event.id); pending.delete(event.id); if (event.error) p.reject(Error(p.method + ': ' + JSON.stringify(event.error))); else p.resolve(event.result) } if (event.method === 'Runtime.exceptionThrown') errors.push(event.params.exceptionDetails) }
const send = (method, params = {}) => new Promise((resolve, reject) => { const id = ++sequence; pending.set(id, { resolve, reject, method: method + ' ' + (params.expression || '') }); ws.send(JSON.stringify({ id, method, params })) })
async function evaluate(expression) { const result = await send('Runtime.evaluate', { expression, awaitPromise: true, returnByValue: true }); if (result.exceptionDetails) throw Error(JSON.stringify(result.exceptionDetails)); return result.result.value }
async function wait(expression, seconds = 20) { for (let i = 0; i < seconds * 5; i++) { if (await evaluate(`Boolean(${expression})`)) return; await new Promise(r => setTimeout(r, 200)) } throw Error(`Timeout: ${expression}\n${await evaluate('document.body.innerText')}`) }
async function go(path) { await send('Page.navigate', { url: origin + path }); await wait('document.querySelector("footer") !== null') }
async function click(text) { await evaluate(`(()=>{const b=[...document.querySelectorAll('button')].find(b=>b.textContent.trim().startsWith(${JSON.stringify(text)})||b.getAttribute('aria-label')===${JSON.stringify(text)});if(!b||b.disabled)throw Error('Missing or disabled: '+${JSON.stringify(text)});b.click()})()`); await new Promise(r => setTimeout(r, 200)) }
const state = s => wait(`document.querySelector('.ticket-card .badge')?.textContent===${JSON.stringify(s)} && ![...document.querySelectorAll('button')].some(b=>b.textContent.includes('Updating booking'))`)
const id = () => evaluate(`Number(document.querySelector('.ticket-card>.eyebrow').textContent.replace(/[^0-9]/g,''))`)
async function select(n) { await wait(`document.querySelector('.seat[aria-label^="Row 1, seat ${n}: AVAILABLE"]:enabled') !== null`); await evaluate(`document.querySelector('.seat[aria-label^="Row 1, seat ${n}: AVAILABLE"]').click()`) }
async function screenshot(name) { const result = await send('Page.captureScreenshot', { captureBeyondViewport: true }); fs.writeFileSync(`../backend/target/redesign-${name}.png`, Buffer.from(result.data, 'base64')) }
async function api(path, method = 'GET', body, actor = 1) { return evaluate(`fetch(${JSON.stringify(path)}, {method:${JSON.stringify(method)},headers:{Accept:'application/json','Content-Type':'application/json','X-User-Id':${JSON.stringify(String(actor))}},${body ? `body:JSON.stringify(${JSON.stringify(body)}),` : ''}}).then(async r=>({status:r.status,body:await r.json()}))`) }
async function draft(seats) { await go('/book/1'); for (const n of seats) await select(n); await click('Create reservation'); await state('Draft'); return id() }
async function confirmPending(seats = [3]) { const value = await draft(seats); await click('Confirm reservation'); await state('Awaiting approval'); return value }
await send('Page.enable'); await send('Runtime.enable'); await send('Emulation.setDeviceMetricsOverride', { width: 1440, height: 1000, deviceScaleFactor: 1, mobile: false })
try {
  await go('/'); await evaluate('localStorage.clear()'); await go('/');
  assert.equal(await evaluate('document.documentElement.dataset.theme'), 'dark'); await screenshot('home-dark')
  await click(''); // first header button is the theme toggle
  await wait('document.documentElement.dataset.theme === "light"'); await go('/'); assert.equal(await evaluate('document.documentElement.dataset.theme'), 'light'); await screenshot('home-light')
  await evaluate(`document.querySelector('.theme-toggle').click()`); await wait('document.documentElement.dataset.theme === "dark"')
  await go('/movies'); await click('Bookable'); assert.equal(await evaluate('document.querySelectorAll(".movie-card").length'), 1)
  await go('/movies/beyond'); assert.equal(await evaluate('document.body.innerText.includes("no available screenings")'), true)
  const direct = await draft([1]); await click('Confirm reservation'); await state('Confirmed'); assert.equal((await api('/screenings/1/availability')).body[0].availability, 'UNAVAILABLE'); await screenshot('booking-confirmed'); await click('Cancel reservation'); await state('Cancelled'); report.push({ flow: 'Direct confirmation and cancellation', id: direct, result: 'PASS' })
  const approved = await confirmPending([1, 3]); await screenshot('booking-pending'); assert.equal((await api(`/reservations/${approved}/approval`, 'POST', { decision: 'APPROVE' }, 1)).status, 403)
  await go('/approver'); await wait('document.querySelector(".status-pending_approval")'); await click('Approve request'); await wait('!document.querySelector(".status-pending_approval")'); assert.equal((await api(`/reservations/${approved}`)).body.status, 'CONFIRMED'); await screenshot('approver');
  await go('/reservations'); await wait('document.querySelector(".status-confirmed")'); await click('Cancel reservation'); await wait('!document.querySelector(".status-confirmed")'); report.push({ flow: 'Mixed approval, owner denied, separate approver, history cancellation', id: approved, result: 'PASS' })
  const rejected = await confirmPending(); await go('/approver'); await wait('document.querySelector(".status-pending_approval")'); await click('Reject'); await wait('!document.querySelector(".status-pending_approval")'); assert.equal((await api(`/reservations/${rejected}`)).body.status, 'REJECTED'); report.push({ flow: 'Separate approver rejection', id: rejected, result: 'PASS' })
  const cancelled = await confirmPending(); await click('Cancel reservation'); await state('Cancelled'); report.push({ flow: 'Cancel pending approval', id: cancelled, result: 'PASS' })
  // Two drafts can exist, but only one can allocate a shared seat.
  const expiring = await draft([3]); const loser = (await api('/reservations', 'POST', { userId: 1, screeningId: 1, seatIds: [3] })).body.id; await click('Confirm reservation'); await state('Awaiting approval'); assert.equal((await api(`/reservations/${loser}/confirm`, 'POST')).status, 409); await api(`/reservations/${loser}/cancel`, 'POST');
  console.log('Direct, approval, rejection, cancellation, conflict passed. Waiting for real backend expiration…')
  await wait('document.querySelector(".status-expired")', 85); assert.equal((await api('/screenings/1/availability')).body[2].availability, 'AVAILABLE'); assert.equal((await api(`/reservations/${expiring}/approval`, 'POST', { decision: 'APPROVE' }, 3)).status, 409); report.push({ flow: 'Conflict and backend-polled expiration, late approval denied', id: expiring, result: 'PASS' })
  for (const path of ['/', '/movies', '/movies/demo', '/book/1', '/reservations', '/approver', '/login', '/register', '/missing']) {
    await send('Emulation.setDeviceMetricsOverride', { width: 390, height: 844, deviceScaleFactor: 1, mobile: true }); await go(path); assert.equal(await evaluate('document.documentElement.scrollWidth <= innerWidth'), true, `Overflow on ${path}`)
    if (path === '/') { await click('Toggle navigation'); assert.equal(await evaluate('document.querySelector("nav").classList.contains("open")'), true); await click('Toggle navigation'); await screenshot('mobile') }
  }
  await go('/register'); await evaluate(`(()=>{const set=(selector,value)=>{const el=document.querySelector(selector);Object.getOwnPropertyDescriptor(HTMLInputElement.prototype,'value').set.call(el,value);el.dispatchEvent(new Event('input',{bubbles:true}));};set('[name="name"]','Demo Guest');set('[name="email"]','guest@example.com');set('[name="password"]','not-saved-123');set('[name="confirmPassword"]','not-saved-123')})()`); await click('Preview registration'); await wait('document.body.innerText.includes("no account was created or signed in")'); assert.equal(await evaluate('JSON.stringify(localStorage).includes("not-saved-123")'), false)
  await go('/book/1'); await wait('document.querySelectorAll(".seat").length === 4'); await send('Network.enable'); await send('Network.setBlockedURLs', { urls: ['*screenings/1/availability*'] }); await click('Refresh seats'); await wait('document.querySelector("[role=alert]")'); assert.equal(await evaluate('[...document.querySelectorAll(".seat")].every(s=>s.disabled)'), true); await send('Network.setBlockedURLs', { urls: [] }); await click('Refresh seats'); await wait('!document.querySelector("[role=alert]") && document.querySelector(".seat:enabled")');
  report.push({ flow: 'All routes, mobile overflow, navigation, theme persistence, account preview, API failure and recovery', result: 'PASS' }); assert.deepEqual(errors, []); fs.writeFileSync('../backend/target/redesign-verification.json', JSON.stringify({ report, browserErrors: errors }, null, 2)); console.log(JSON.stringify(report, null, 2))
} finally { ws.close(); await fetch(`http://127.0.0.1:9222/json/close/${target.id}`) }
