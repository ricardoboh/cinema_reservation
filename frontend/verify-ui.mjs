import fs from 'node:fs'
import assert from 'node:assert/strict'
const base = 'http://127.0.0.1:5173'
async function page() {
  const target = await (await fetch('http://127.0.0.1:9222/json/new?about:blank', { method: 'PUT' })).json()
  const ws = new WebSocket(target.webSocketDebuggerUrl)
  await new Promise((resolve, reject) => { ws.onopen = resolve; ws.onerror = reject })
  let next = 0
  const pending = new Map(), network = [], errors = []
  ws.onmessage = ({ data }) => {
    const msg = JSON.parse(data)
    if (msg.id) { const task = pending.get(msg.id); pending.delete(msg.id); if (msg.error) task.reject(msg.error); else task.resolve(msg.result) }
    if (msg.method === 'Network.responseReceived' && /\/reservations|\/availability/.test(msg.params.response.url)) network.push({ url: msg.params.response.url, status: msg.params.response.status })
    if (msg.method === 'Runtime.exceptionThrown') errors.push(msg.params.exceptionDetails.text)
  }
  const send = (method, params = {}) => new Promise((resolve, reject) => { const id = ++next; pending.set(id, { resolve, reject }); ws.send(JSON.stringify({ id, method, params })) })
  const evaluate = async expression => { const r = await send('Runtime.evaluate', { expression, returnByValue: true, awaitPromise: true }); if (r.exceptionDetails) throw Error(JSON.stringify(r.exceptionDetails)); return r.result.value }
  const wait = async (expression, seconds = 15) => {
    for (let i = 0; i < seconds * 10; i++) { if (await evaluate(expression)) return; await new Promise(r => setTimeout(r, 100)) }
    throw Error('Timeout: ' + expression + '\n' + await evaluate('document.body.innerText'))
  }
  const click = async label => { await evaluate(`(()=>{const b=[...document.querySelectorAll('button')].find(b=>b.textContent.trim()===${JSON.stringify(label)});if(!b||b.disabled)throw Error('Button missing/disabled: '+${JSON.stringify(label)});b.click()})()`) }
  const select = async n => { await evaluate(`(()=>{const b=document.querySelector('button[aria-label="Row 1, seat ${n}: AVAILABLE"]');if(!b||b.disabled)throw Error('Seat missing/disabled');b.click()})()`) }
  const status = async (expected, seconds) => wait(`document.querySelector('.current-reservation .badge')?.textContent===${JSON.stringify(expected)} && !document.querySelector('.progress')`, seconds)
  const id = () => evaluate(`Number(document.querySelector('.current-reservation strong').textContent)`)
  const availability = async (n, expected) => wait(`document.querySelector('button[aria-label="Row 1, seat ${n}: ${expected}"]') !== null`)
  const capture = async name => {
    await send('Page.bringToFront')
    fs.writeFileSync(`backend/target/${name}.png`, Buffer.from((await send('Page.captureScreenshot', { captureBeyondViewport: true })).data, 'base64'))
  }
  await send('Network.enable'); await send('Runtime.enable'); await send('Page.enable')
  await send('Emulation.setDeviceMetricsOverride', { width: 1440, height: 1100, deviceScaleFactor: 1, mobile: false })
  await send('Page.navigate', { url: base })
  await wait(`document.querySelector('button[aria-label="Row 1, seat 3: AVAILABLE"]') && !document.querySelector('.progress')`)
  return { send, evaluate, wait, click, select, status, id, availability, capture, network, errors, ws }
}
const report = { flows: {}, network: [], browserErrors: [] }
async function main() {
  const a = await page()
  await a.click('Check Availability'); await a.wait(`document.body.innerText.includes('Availability and reservation refreshed') && !document.querySelector('.progress')`)
  await a.select(1); await a.click('Create Reservation'); await a.status('DRAFT'); const direct = await a.id()
  await a.availability(1, 'AVAILABLE'); await a.click('Confirm Reservation'); await a.status('CONFIRMED'); await a.availability(1, 'UNAVAILABLE')
  await a.click('Cancel Reservation'); await a.status('CANCELLED'); await a.availability(1, 'AVAILABLE')
  report.flows.A = { id: direct, result: 'DRAFT non-blocking -> CONFIRMED blocking -> CANCELLED released' }
  console.log('UI A passed')
  await a.select(1); await a.select(3); await a.click('Create Reservation'); await a.status('DRAFT'); const approved = await a.id()
  await a.click('Confirm Reservation'); await a.status('PENDING_APPROVAL'); await a.availability(1, 'UNAVAILABLE'); await a.availability(3, 'UNAVAILABLE')
  assert.equal(await a.evaluate(`document.querySelector('.deadline').innerText.includes('Deadline:')`), true)
  await a.evaluate(`(()=>{const s=document.querySelector('select');s.value='1';s.dispatchEvent(new Event('change',{bubbles:true}))})()`)
  await a.click('APPROVE'); await a.wait(`document.querySelector('[role="alert"]')?.textContent.includes('HTTP 403') && !document.querySelector('.progress')`)
  await a.status('PENDING_APPROVAL'); await a.availability(3, 'UNAVAILABLE')
  await a.click('REJECT'); await a.wait(`document.querySelector('[role="alert"]')?.textContent.includes('HTTP 403') && !document.querySelector('.progress')`)
  await a.evaluate(`(()=>{const s=document.querySelector('select');s.value='3';s.dispatchEvent(new Event('change',{bubbles:true}))})()`)
  await a.capture('c02-v02-ui-pending')
  await a.click('APPROVE'); await a.status('CONFIRMED'); await a.availability(1, 'UNAVAILABLE'); await a.availability(3, 'UNAVAILABLE')
  assert.equal(await a.evaluate(`document.querySelector('.current-reservation').innerText.includes('Approver 3')`), true)
  await a.capture('c02-v02-ui-confirmed'); await a.click('Cancel Reservation'); await a.status('CANCELLED')
  report.flows.B = { id: approved, result: 'Mixed seats held atomically; owner APPROVE/REJECT 403; Approver APPROVE confirmed without release' }
  console.log('UI B and 403 passed')
  await a.select(3); await a.click('Create Reservation'); await a.status('DRAFT'); const rejected = await a.id()
  await a.click('Confirm Reservation'); await a.status('PENDING_APPROVAL'); await a.click('REJECT'); await a.status('REJECTED'); await a.availability(3, 'AVAILABLE')
  report.flows.C = { id: rejected, result: 'REJECTED and available' }; console.log('UI C passed')
  await a.select(3); await a.click('Create Reservation'); await a.status('DRAFT'); const cancelled = await a.id()
  await a.click('Confirm Reservation'); await a.status('PENDING_APPROVAL'); await a.click('Cancel Reservation'); await a.status('CANCELLED'); await a.availability(3, 'AVAILABLE')
  report.flows.D = { id: cancelled, result: 'Pending CANCELLED and available' }; console.log('UI D passed')
  await a.select(3); await a.click('Create Reservation'); await a.status('DRAFT'); const winner = await a.id()
  const b = await page(); await b.select(3); await b.click('Create Reservation'); await b.status('DRAFT'); const loser = await b.id()
  await a.click('Confirm Reservation'); await a.status('PENDING_APPROVAL'); await b.click('Confirm Reservation')
  await b.wait(`document.querySelector('[role="alert"]')?.textContent.includes('HTTP 409') && !document.querySelector('.progress')`)
  await b.status('DRAFT'); await b.availability(3, 'UNAVAILABLE'); await b.capture('c02-v02-ui-conflict')
  report.flows.F = { winner, loser, result: 'Live pending conflict 409 visibly shown; loser remains DRAFT' }; console.log('UI F passed; waiting for actual deadline')
  await b.click('Cancel Reservation'); await b.status('CANCELLED')
  await a.status('EXPIRED', 90); await a.availability(3, 'AVAILABLE')
  assert.equal(await a.evaluate(`document.querySelector('.current-reservation').innerText.includes('Expired:')`), true)
  const late = await a.evaluate(`fetch('/reservations/${winner}/approval',{method:'POST',headers:{'X-User-Id':'3','Content-Type':'application/json'},body:JSON.stringify({decision:'APPROVE'})}).then(r=>r.status)`)
  assert.equal(late, 409)
  report.flows.E = { id: winner, result: 'Backend-polled EXPIRED; resource available; late approval 409' }; console.log('UI E passed')
  await a.capture('c02-v02-ui-expired')
  await a.send('Emulation.setDeviceMetricsOverride', { width: 390, height: 844, deviceScaleFactor: 1, mobile: true })
  assert.equal(await a.evaluate(`document.documentElement.scrollWidth <= window.innerWidth`), true)
  await a.capture('c02-v02-ui-mobile')
  assert.deepEqual(a.errors, []); assert.deepEqual(b.errors, [])
  report.network = [...a.network, ...b.network]; report.browserErrors = [...a.errors, ...b.errors]
  report.responsive = '390px no horizontal overflow'; report.result = 'A-F PASS'
  fs.writeFileSync('backend/target/c02-v02-ui-verification.json', JSON.stringify(report, null, 2))
  a.ws.close(); b.ws.close()
  console.log(JSON.stringify({ result: report.result, flows: report.flows, responsive: report.responsive, browserErrors: report.browserErrors }, null, 2))
}
main().catch(cause => { console.error(cause); process.exit(1) })
