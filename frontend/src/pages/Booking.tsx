import { useCallback, useEffect, useRef, useState } from 'react'
import { changeReservation, createReservation, loadAvailability, loadReservation } from '../reservations'
import type { Reservation, SeatAvailability } from '../reservations'
import { remember } from '../hooks/useReservations'
import { ReservationCard } from '../components/ReservationCard'
import { Icon, Message, Poster } from '../components/ui'
import { errorText } from '../utils'
import { movies } from '../data/catalog'
import { Link } from '../router'
export function Booking() {
  const [seats, setSeats] = useState<SeatAvailability[]>([])
  const [selected, setSelected] = useState<number[]>([])
  const [reservation, setReservation] = useState<Reservation | null>(null)
  const [busy, setBusy] = useState(false)
  const [ready, setReady] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [now, setNow] = useState(() => Date.now())
  const lock = useRef(false)
  const refresh = useCallback(async () => {
    try { const data = await loadAvailability(); setSeats(data); setSelected(ids => ids.filter(id => data.some(s => s.seatId === id && s.availability === 'AVAILABLE'))); setReady(true) }
    catch (e) { setReady(false); throw e }
  }, [])
  useEffect(() => { let live = true; void Promise.resolve().then(refresh).catch(e => { if (live) setError(errorText(e)) }); return () => { live = false } }, [refresh])
  const reservationId = reservation?.id
  const reservationStatus = reservation?.status
  useEffect(() => {
    if (!reservationId || reservationStatus !== 'PENDING_APPROVAL') return
    let live = true; let polling = false
    const timer = window.setInterval(async () => {
      setNow(Date.now())
      if (polling || lock.current) return
      polling = true
      try {
        const current = await loadReservation(reservationId)
        if (live) {
          setReservation(current)
          if (current.status !== reservationStatus) setNotice(current.status === 'CONFIRMED' ? 'Your reservation is confirmed. Enjoy the show!' : current.status === 'EXPIRED' ? 'The approval window has expired. Your seats have been released.' : current.status === 'REJECTED' ? 'Your request was declined. Your seats have been released.' : 'Your reservation status has been updated.')
          await refresh()
        }
      } catch (e) { if (live) setError(errorText(e)) }
      finally { polling = false }
    }, 2000)
    return () => { live = false; clearInterval(timer) }
  }, [reservationId, reservationStatus, refresh])
  async function run(action: 'create' | 'confirm' | 'cancel' | 'refresh') {
    if (lock.current) return
    lock.current = true; setBusy(true); setError(''); setNotice('')
    let current = reservation
    try {
      if (action === 'create') { current = await createReservation(selected, remember); setSelected([]) }
      else if ((action === 'confirm' || action === 'cancel') && current) current = await changeReservation(current.id, action)
      else if (current) current = await loadReservation(current.id)
      setReservation(current); await refresh()
      setNotice(action === 'refresh' ? 'Seat availability and booking status updated.' : action === 'create' ? 'Reservation created successfully. Confirm your draft to continue.' : action === 'cancel' ? 'Reservation cancelled.' : current?.status === 'PENDING_APPROVAL' ? 'Your reservation is awaiting approval.' : 'Your reservation is confirmed. Enjoy the show!')
    } catch (e) { setError(errorText(e)); try { if (current) setReservation(await loadReservation(current.id)); await refresh() } catch { setReady(false) } }
    finally { lock.current = false; setBusy(false) }
  }
  const active = reservation && ['DRAFT', 'PENDING_APPROVAL', 'CONFIRMED'].includes(reservation.status)
  const requiresApproval = selected.some(id => seats.find(s => s.seatId === id)?.approvalRequired)
  return <section className="container page booking-page"><Link to="/movies/demo" className="back-link">← Back to movie</Link><div className="section-heading"><div><p className="eyebrow">MAKE IT YOUR MOVIE NIGHT</p><h1>Find your perfect seat<span className="accent">.</span></h1></div><span className="demo-pill">Demo experience · Customer 1</span></div><div className="booking-steps"><span className="complete">01 <b>Choose seats</b></span><span className={reservation ? 'complete' : ''}>02 <b>Confirm reservation</b></span><span className={reservation?.status === 'CONFIRMED' ? 'complete' : ''}>03 <b>Enjoy the show</b></span></div>{error && <Message error>{error}</Message>}{notice && <Message>{notice}</Message>}<div className="booking-layout"><div className="panel auditorium-panel"><div className="panel-heading"><div><h2>Hall A</h2><p className="muted">CP1 Demo Movie · Screening 1</p></div><button className="button small secondary" disabled={busy} onClick={() => run('refresh')}>Refresh seats</button></div><div className="auditorium" aria-busy={busy}><div className="cinema-screen"><span>THE BIG SCREEN</span></div><p className="seat-prompt">A great view starts right here.</p>{!ready && <p role="status" className="seat-loading">{error ? 'Availability unavailable. Use Refresh seats to retry.' : 'Finding your seats…'}</p>}<div className="seating-scroll">{[...new Set(seats.map(s => s.rowNumber))].sort((a, b) => a - b).map(row => <div className="seat-row" key={row} role="group" aria-label={`Row ${row}`}><span className="row-label">{row}</span>{seats.filter(s => s.rowNumber === row).sort((a, b) => a.seatNumber - b.seatNumber).map(s => <button key={s.seatId} className={`seat ${s.approvalRequired ? 'approval-seat' : ''} ${s.availability === 'UNAVAILABLE' ? 'unavailable' : ''} ${selected.includes(s.seatId) ? 'selected' : ''}`} aria-pressed={selected.includes(s.seatId)} aria-label={`Row ${row}, seat ${s.seatNumber}: ${s.availability}${s.approvalRequired ? ', approval required' : ''}`} disabled={busy || !ready || !!active || s.availability === 'UNAVAILABLE'} onClick={() => setSelected(ids => ids.includes(s.seatId) ? ids.filter(id => id !== s.seatId) : [...ids, s.seatId])}><span>{s.seatNumber}</span>{s.approvalRequired && <small aria-hidden="true">•</small>}</button>)}<span className="row-label">{row}</span></div>)}</div>{ready && !seats.length && <p>No seats are configured for this screening.</p>}<div className="legend"><span><i />Available</span><span><i className="selected" />Selected</span><span><i className="unavailable" />Occupied / held</span><span><i className="approval" />Approval required</span></div></div><div className="booking-note"><Icon name="ticket" /><p><strong>A small hall. The full experience.</strong> This development screening has four seats. Seats marked in amber require approval after confirmation. All availability comes from the backend.</p></div></div><aside className="booking-sidebar"><div className="panel summary"><div className="summary-movie"><Poster movie={movies[0]} /><div><p className="eyebrow">YOUR MOVIE NIGHT</p><h3>CP1 Demo Movie</h3><p className="muted">Hall A · Screening 1</p></div></div><div className="summary-line"><span>Selected seats</span><strong>{selected.length ? selected.map(id => { const s = seats.find(s => s.seatId === id); return `${s?.rowNumber}:${s?.seatNumber}` }).join(', ') : 'Not selected'}</strong></div><div className="summary-line"><span>Guests</span><strong>{selected.length || '—'}</strong></div><p className="muted summary-help">{requiresApproval ? 'Your selection includes approval-required seats. Confirm your draft to request approval.' : 'Choose your seats, create a draft, then confirm your reservation.'}</p><button className="button full" disabled={busy || !ready || !!active || !selected.length} onClick={() => run('create')}>{busy ? 'Updating booking…' : 'Create reservation'}<Icon name="arrow" size={17} /></button>{active && <p className="muted summary-help">Finish or cancel your current booking before choosing more seats.</p>}<p className="no-payment">No payment needed · University demo</p></div>{reservation && <ReservationCard reservation={reservation} seats={seats} busy={busy} act={(_id, action) => { if (action === 'confirm' || action === 'cancel') void run(action) }} />}{reservation?.status === 'PENDING_APPROVAL' && <div className="panel approval-help"><p>Approval window: <strong>{Math.max(0, Math.ceil((Date.parse(reservation.approvalDeadline || '') - now) / 1000))}s remaining</strong></p><p className="muted">Status refreshes automatically. A zero countdown does not confirm expiration until the server responds.</p><Link to="/approver" className="text-link">Open demo approver <Icon name="arrow" size={16} /></Link></div>}</aside></div><p className="catalog-note">Demo identity uses fixture customer 1, with no authentication. The screening starts one day after backend initialization; its exact time is not available through the current API. <Link to="/reservations">View saved reservation IDs →</Link></p></section>
}
