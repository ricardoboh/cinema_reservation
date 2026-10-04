import { useEffect, useState } from 'react'
import { changeReservation, createReservation, decideReservation, loadAvailability, loadReservation } from './reservations'
import type { Reservation, SeatAvailability } from './reservations'
import './App.css'
const descriptions: Record<string, string> = {
  DRAFT: 'Your draft is saved. Seats remain available until you confirm.',
  PENDING_APPROVAL: 'All selected seats are held together. A separate Approver must decide before the deadline.',
  CONFIRMED: 'Your reservation is confirmed. All selected seats remain allocated.',
  REJECTED: 'The Approver rejected this request. All seats have been released.',
  EXPIRED: 'The approval deadline passed. All seats have been released.',
  CANCELLED: 'You cancelled this reservation. All seats have been released.',
}
const date = (value: string | null) => value ? new Date(value).toLocaleString() : '—'
const message = (cause: unknown) => cause instanceof Error ? cause.message : 'Request failed.'
function App() {
  const [seats, setSeats] = useState<SeatAvailability[]>([])
  const [selected, setSelected] = useState<number[]>([])
  const [reservation, setReservation] = useState<Reservation | null>(null)
  const [busy, setBusy] = useState(false)
  const [ready, setReady] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [actor, setActor] = useState(3)
  const [now, setNow] = useState(() => Date.now())
  const active = reservation !== null && ['DRAFT', 'PENDING_APPROVAL', 'CONFIRMED'].includes(reservation.status)
  useEffect(() => {
    let mounted = true
    loadAvailability().then(data => { if (mounted) { setSeats(data); setReady(true) } })
      .catch((cause: unknown) => { if (mounted) setError(message(cause)) })
    return () => { mounted = false }
  }, [])
  useEffect(() => {
    if (reservation?.status !== 'PENDING_APPROVAL') return
    let mounted = true
    let refreshing = false
    const timer = window.setInterval(() => {
      setNow(Date.now())
      if (busy || refreshing) return
      refreshing = true
      Promise.all([loadReservation(reservation.id), loadAvailability()]).then(([current, availability]) => {
        if (mounted) {
          setReservation(current); setSeats(availability); setReady(true)
          if (current.status !== reservation.status) setNotice(`Reservation ${current.id}: ${current.status}.`)
        }
      }).catch((cause: unknown) => { if (mounted) { setError(message(cause)); setReady(false) } })
        .finally(() => { refreshing = false })
    }, 1000)
    return () => { mounted = false; window.clearInterval(timer) }
  }, [reservation?.id, reservation?.status, busy])
  async function reload(current: Reservation | null) {
    setReady(false)
    if (current) setReservation(await loadReservation(current.id))
    const data = await loadAvailability()
    setSeats(data)
    setSelected(ids => ids.filter(id => data.some(seat => seat.seatId === id && seat.availability === 'AVAILABLE')))
    setReady(true)
  }
  async function run(operation: 'create' | 'refresh' | 'confirm' | 'cancel' | 'APPROVE' | 'REJECT') {
    if (busy) return
    setBusy(true); setError(''); setNotice('')
    let current = reservation
    try {
      if (operation === 'create') { current = await createReservation(selected); setSelected([]) }
      else if (operation === 'confirm' || operation === 'cancel') { if (current) current = await changeReservation(current.id, operation) }
      else if (operation === 'APPROVE' || operation === 'REJECT') { if (current) current = await decideReservation(current.id, operation, actor) }
      if (current) setReservation(current)
      await reload(current)
      setNotice(operation === 'refresh' ? 'Availability and reservation refreshed from the backend.' : `Reservation ${current?.id}: ${current?.status}.`)
    } catch (cause) {
      setError(message(cause))
      try { await reload(current) } catch (refreshCause) { setError(`${message(cause)} Refresh failed: ${message(refreshCause)}`) }
    } finally { setBusy(false) }
  }
  const remaining = reservation?.approvalDeadline ? Math.max(0, Math.ceil((Date.parse(reservation.approvalDeadline) - now) / 1000)) : 0
  return <main className="reservation-page">
    <header className="page-header"><div><p className="eyebrow">Reserved Bytes · Baseline v0.2</p><h1>A seat for your next story.</h1><p>A runnable cinema booking demo with delayed approval.</p></div><span className="demo-tag">Development demo</span></header>
    <div className="booking-layout" aria-busy={busy}>
      <section className="reservation-card booking-card" aria-labelledby="screening-title">
        <div className="screening-info"><div><p className="eyebrow">Screening 1 · Hall A</p><h2 id="screening-title">CP1 Demo Movie</h2><p className="seat-note">Development fixture · starts one day after backend initialization</p></div><button className="secondary-button" disabled={busy} onClick={() => run('refresh')}>Check Availability</button></div>
        <div className="auditorium"><div className="cinema-screen">Cinema screen</div><p id="seat-instructions">Choose your seats</p>
          {[...new Set(seats.map(s => s.rowNumber))].map(row => <div className="seat-row" key={row} role="group" aria-label={`Row ${row}`}>
            {seats.filter(s => s.rowNumber === row).map(seat => <button key={seat.seatId} className={`seat ${seat.availability === 'UNAVAILABLE' ? 'unavailable' : ''} ${seat.approvalRequired ? 'approval-seat' : ''}`} aria-label={`Row ${row}, seat ${seat.seatNumber}: ${seat.availability}`} aria-pressed={selected.includes(seat.seatId)} disabled={busy || !ready || active || seat.availability === 'UNAVAILABLE'} onClick={() => setSelected(ids => ids.includes(seat.seatId) ? ids.filter(id => id !== seat.seatId) : [...ids, seat.seatId])}>
              <strong>{row}:{seat.seatNumber}</strong><span>{selected.includes(seat.seatId) ? 'Selected' : seat.availability === 'AVAILABLE' ? 'Available' : 'Unavailable'}</span><small>{seat.approvalRequired ? 'Approval required' : 'Direct confirm'}</small></button>)}
          </div>)}
          <div className="legend"><span>○ Available</span><span>● Selected</span><span>▧ Unavailable: held or confirmed</span></div>{!ready && <p className="seat-note">Availability is loading or stale. Refresh to retry.</p>}
        </div>
        <div className="reservation-summary"><div><h3>Your selection</h3><p>{selected.length ? selected.map(id => { const seat = seats.find(s => s.seatId === id); return `${seat?.rowNumber}:${seat?.seatNumber}` }).join(', ') : 'Select an available seat above'}</p><p className="seat-note">Customer identity: owner 1</p></div><button className="reserve-button" disabled={busy || !ready || active || !selected.length} onClick={() => run('create')}>Create Reservation</button></div>
        <div className="policy-note"><strong>Two paths, one reservation.</strong> Seats 1–2 confirm directly. Seats 3–4 require demo approval. A mixed selection holds every seat until the whole request is resolved.</div>
      </section>
      <aside className="reservation-card status-card"><p className="eyebrow">Your booking</p><h2>Reservation journey</h2><div className="journey"><span>1 · Draft</span><span>2 · Submit / hold</span><span>3 · Outcome</span></div>
        <div className="current-reservation" aria-live="polite">{reservation ? <><p>Reservation ID: <strong>{reservation.id}</strong></p><p className={`badge status-${reservation.status.toLowerCase()}`}>{reservation.status}</p><p>Seats: {reservation.seatIds.map(id => { const seat = seats.find(s => s.seatId === id); return seat ? `${seat.rowNumber}:${seat.seatNumber}` : `ID ${id}` }).join(', ')}</p><p>{descriptions[reservation.status]}</p>
          {reservation.approvalRequestedAt && <div className="deadline"><p>Requested: {date(reservation.approvalRequestedAt)}</p><p>Deadline: <strong>{date(reservation.approvalDeadline)}</strong></p>{reservation.status === 'PENDING_APPROVAL' && <p>{remaining}s remaining · state polls the backend</p>}</div>}
          {reservation.decision && <p>Decision: {reservation.decision} · Approver {reservation.decidedBy}<br />{date(reservation.decidedAt)}</p>}{reservation.expiredAt && <p>Expired: {date(reservation.expiredAt)}</p>}{reservation.cancelledAt && <p>Cancelled: {date(reservation.cancelledAt)}</p>}
          <div className="reservation-actions">{reservation.status === 'DRAFT' && <button className="reserve-button" disabled={busy} onClick={() => run('confirm')}>Confirm Reservation</button>}{active && <button className="secondary-button" disabled={busy} onClick={() => run('cancel')}>Cancel Reservation</button>}</div></> : <p>Create a draft to begin. Drafts do not block seats.</p>}</div>
        <section className="approver-panel" aria-labelledby="approver-title"><p className="eyebrow">Separate demo role</p><h3 id="approver-title">DEMO Approver</h3><p>Approver 3 has decision authority. Customer ownership alone grants no approval authority.</p>
          <label>Decision caller<select value={actor} disabled={busy} onChange={event => setActor(Number(event.target.value))}><option value={3}>Approver 3 · authorized</option><option value={1}>Owner 1 · unauthorized (403 demo)</option></select></label>
          <div className="reservation-actions"><button className="reserve-button" disabled={busy || reservation?.status !== 'PENDING_APPROVAL'} onClick={() => run('APPROVE')}>APPROVE</button><button className="secondary-button" disabled={busy || reservation?.status !== 'PENDING_APPROVAL'} onClick={() => run('REJECT')}>REJECT</button></div><p className="seat-note">For expiration, leave the request pending until its deadline. The backend releases the hold; this page refreshes automatically.</p>
        </section>
      </aside>
    </div>{busy && <p role="status" className="progress">Contacting reservation backend…</p>}{notice && <div className="message success" role="status">{notice}</div>}{error && <div className="message error" role="alert">{error}</div>}
    <p className="footnote">C02 development application · Configurable demo approval window (default 60s) · No authenticated login · Reloading clears the current booking from this page.</p>
  </main>
}
export default App
