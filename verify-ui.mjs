// Start current dev backend, Vite and a local Chromium/Edge CDP listener on 9222.
// Run from repository root: node frontend/verify-ui.mjs
import fs from 'node:fs'
import assert from 'node:assert/strict'

const origin = 'http://127.0.0.1:5173'
const target = await (await fetch('http://127.0.0.1:9222/json/new?about:blank', { method: 'PUT' })).json()
const ws = new WebSocket(target.webSocketDebuggerUrl)
await new Promise((resolve, reject) => { ws.onopen = resolve; ws.onerror = reject })
let sequence = 0
const pending = new Map(), errors = [], checks = []
ws.onmessage = ({ data }) => {
  const message = JSON.parse(data)
  if (message.id) {
    const task = pending.get(message.id); pending.delete(message.id)
    if (message.error) task.reject(Error(JSON.stringify(message.error))); else task.resolve(message.result)
  }
  if (message.method === 'Runtime.exceptionThrown') errors.push(message.params.exceptionDetails)
}
const send = (method, params = {}) => new Promise((resolve, reject) => {
  const id = ++sequence; pending.set(id, { resolve, reject }); ws.send(JSON.stringify({ id, method, params }))
})
async function evaluate(expression) {
  const result = await send('Runtime.evaluate', { expression, returnByValue: true, awaitPromise: true })
  if (result.exceptionDetails) throw Error(JSON.stringify(result.exceptionDetails))
  return result.result.value
}
async function wait(expression, seconds = 15) {
  for (let i = 0; i < seconds * 10; i++) {
    if (await evaluate(expression)) return
    await new Promise(resolve => setTimeout(resolve, 100))
  }
  throw Error(`Timeout: ${expression}\n${await evaluate('document.body.innerText')}`)
}
async function go(path) { await send('Page.navigate', { url: origin + path }); await wait('document.querySelector("footer") !== null') }
async function click(text) {
  await evaluate(`(()=>{const button=[...document.querySelectorAll('button')].find(b=>b.textContent.trim()===${JSON.stringify(text)});if(!button||button.disabled)throw Error('Missing/disabled button '+${JSON.stringify(text)});button.click()})()`)
}
async function fill(selector, value) {
  await evaluate(`(()=>{const input=document.querySelector(${JSON.stringify(selector)});Object.getOwnPropertyDescriptor(HTMLInputElement.prototype,'value').set.call(input,${JSON.stringify(String(value))});input.dispatchEvent(new Event('input',{bubbles:true}))})()`)
}
async function api(path, method = 'GET', body, actor = 1) {
  return evaluate(`fetch(${JSON.stringify(path)},{method:${JSON.stringify(method)},headers:{Accept:'application/json','Content-Type':'application/json','X-User-Id':${JSON.stringify(String(actor))}},${body ? `body:JSON.stringify(${JSON.stringify(body)}),` : ''}}).then(async response=>({status:response.status,body:await response.json()}))`)
}
const status = (value, seconds) => wait(`document.querySelector('.ticket-card .badge')?.textContent===${JSON.stringify(value)} && ![...document.querySelectorAll('.ticket-card button')].some(b=>b.disabled)`, seconds)
const reservationId = () => evaluate(`Number(document.querySelector('.ticket-card h2').textContent.replace(/[^0-9]/g,''))`)
let bookingPath
async function select(type) {
  await wait(`document.querySelector('button.seat[aria-label$="AVAILABLE, ${type}"]:enabled') !== null`)
  await evaluate(`document.querySelector('button.seat[aria-label$="AVAILABLE, ${type}"]:enabled').click()`)
}
async function createPending() {
  await go(bookingPath); await select('STANDARD'); await select('VIP')
  await click('Create draft'); await status('Draft'); const id = await reservationId()
  await click('Confirm reservation'); await status('Awaiting approval')
  return id
}
await send('Page.enable'); await send('Runtime.enable')
await send('Emulation.setDeviceMetricsOverride', { width: 1280, height: 900, deviceScaleFactor: 1, mobile: false })
try {
  await go('/movies'); await wait(`document.querySelector('a[href^="/book/"]') !== null`)
  bookingPath = await evaluate(`document.querySelector('a[href^="/book/"]').getAttribute('href')`)
  assert.equal(await evaluate(`document.querySelector('input[type="search"],a[href="/login"],a[href="/register"],.theme-toggle')===null`), true)
  checks.push('Only scoped navigation and catalogue; no search/theme/account previews')
  await go(bookingPath); await select('STANDARD'); await click('Create draft'); await status('Draft')
  const direct = await reservationId()
  assert.equal((await api(`/reservations/${direct}`)).body.status, 'DRAFT')
  await click('Confirm reservation'); await status('Confirmed'); await click('Cancel reservation'); await status('Cancelled')
  checks.push('Real UI Create full DRAFT -> Confirm -> Cancel')
  const approved = await createPending()
  await evaluate(`document.querySelector('nav[aria-label="Main navigation"] a[href="/approver"]').click()`)
  await wait('document.querySelector("h1")?.textContent === "Approver"')
  assert.equal(await evaluate('document.querySelector("input[type=number]").value'), String(approved))
  await click('Load reservation'); await status('Awaiting approval'); await click('Approve'); await status('Confirmed')
  assert.equal((await api(`/reservations/${approved}`)).body.decision, 'APPROVE')
  await go('/reservations'); await fill('input[type=number]', approved); await click('Load reservation'); await status('Confirmed')
  await click('Cancel reservation'); await status('Cancelled')
  checks.push('Independent approver UI and owner observation/cancellation; current decision metadata')
  const rejected = await createPending()
  await go('/approver'); await fill('input[type=number]', rejected); await click('Load reservation'); await status('Awaiting approval')
  await click('Reject'); await status('Rejected')
  checks.push('UI rejection and terminal controls')
  const expiring = await createPending()
  const due = (await api(`/reservations/${expiring}`)).body.approvalDeadline
  const seconds = Math.max(15, Math.ceil((Date.parse(due) - Date.now()) / 1000) + 10)
  await status('Expired', seconds)
  assert.equal(await evaluate(`document.querySelector('.ticket-card').innerText.includes('Expired at:')`), true)
  assert.equal((await api(`/reservations/${expiring}`)).body.expiredAt, due)
  checks.push('Real deadline observed via backend polling; effective expiration time visible')
  await go('/admin/movies'); await wait('document.querySelector("form input") !== null')
  await fill('form input', 'Browser verified movie'); await click('Save movie')
  await wait(`document.body.innerText.includes('Browser verified movie') && !document.querySelector('button:disabled')`)
  checks.push('Minimal movie management form and live API')
  await send('Emulation.setDeviceMetricsOverride', { width: 390, height: 844, deviceScaleFactor: 1, mobile: true })
  for (const path of ['/movies', bookingPath, '/reservations', '/approver', '/admin/movies', '/admin/halls', '/admin/screenings']) {
    await go(path)
    await new Promise(resolve => setTimeout(resolve, 150))
    assert.equal(await evaluate('document.documentElement.scrollWidth <= innerWidth'), true, `Overflow on ${path}`)
  }
  checks.push('390px viewport: scoped routes have no document-wide horizontal overflow')
  assert.deepEqual(errors, [])
  const report = { result: 'PASS', timestamp: new Date().toISOString(), checks, browserErrors: errors }
  fs.mkdirSync('backend/target', { recursive: true })
  fs.writeFileSync('backend/target/current-ui-verification.json', JSON.stringify(report, null, 2))
  console.log(JSON.stringify(report, null, 2))
} finally {
  ws.close()
  await fetch(`http://127.0.0.1:9222/json/close/${target.id}`)
}
