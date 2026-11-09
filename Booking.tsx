import { useCallback, useEffect, useRef, useState } from 'react'
import { changeReservation, createReservation, loadAvailability, loadReservation } from '../reservations'
import type { Reservation, SeatAvailability } from '../reservations'
import { api } from '../cinema'
import type { Screening } from '../cinema'
import { ReservationCard } from '../components/ReservationCard'
import { Message } from '../components/ui'
import { errorText } from '../utils'

export function Booking({ screeningId, onCreated }: { screeningId: number; onCreated: (id: number) => void }) {
  const [screening, setScreening] = useState<Screening | null>(null)
  const [seats, setSeats] = useState<SeatAvailability[]>([])
  const [selected, setSelected] = useState<number[]>([])
  const [reservation, setReservation] = useState<Reservation | null>(null)
  const [busy, setBusy] = useState(false)
  const [ready, setReady] = useState(false)
  const [error, setError] = useState('')
  const lock = useRef(false)
  const refresh = useCallback(async () => {
    try {
      const seats = await loadAvailability(screeningId)
      setSeats(seats)
      setSelected(ids => ids.filter(id => seats.some(seat => seat.seatId === id && seat.status === 'AVAILABLE')))
      setReady(true)
    } catch (error) { setReady(false); throw error }
  }, [screeningId])
  useEffect(() => {
    let live = true
    Promise.resolve().then(() => Promise.all([api<Screening>(`/screenings/${screeningId}`), refresh()]))
      .then(([screening]) => { if (live) setScreening(screening) })
      .catch(error => { if (live) setError(errorText(error)) })
    return () => { live = false }
  }, [screeningId, refresh])
  const id = reservation?.reservationId
  const status = reservation?.status
  useEffect(() => {
    if (!id || status !== 'PENDING_APPROVAL') return
    let live = true, polling = false
    const timer = window.setInterval(async () => {
      if (polling || lock.current) return
      polling = true
      try {
        const result = await loadReservation(id)
        if (live) { setReservation(result); await refresh() }
      } catch (error) { if (live) setError(errorText(error)) }
      finally { polling = false }
    }, 1000)
    return () => { live = false; clearInterval(timer) }
  }, [id, status, refresh])
  async function run(action: 'create' | 'confirm' | 'cancel' | 'refresh') {
    if (lock.current) return
    lock.current = true; setBusy(true); setError('')
    try {
      if (action === 'create') {
        const created = await createReservation(selected, screeningId)
        setReservation(created); setSelected([]); onCreated(created.reservationId)
      } else if (reservation) {
        setReservation(action === 'refresh' ? await loadReservation(reservation.reservationId)
          : await changeReservation(reservation.reservationId, action))
      }
      await refresh()
    } catch (error) {
      setError(errorText(error))
      try { if (reservation) setReservation(await loadReservation(reservation.reservationId)); await refresh() }
      catch { setReady(false) }
    } finally { lock.current = false; setBusy(false) }
  }
  return <section><h1>Choose seats</h1>
    {screening && <p>{screening.movie.title} · {screening.hall.name} · {new Date(screening.startsAt).toLocaleString()}</p>}
    {error && <Message error>{error}</Message>}
    <p>VIP seats require approval for the whole reservation. Drafts do not hold seats.</p>
    <button disabled={busy} onClick={() => void run('refresh')}>Refresh availability</button>
    {!ready && <p role="status">Availability is not loaded.</p>}
    <div className="seat-map">{[...new Set(seats.map(seat => seat.rowNumber))].map(row => <div className="seat-row" key={row}>
      <span>Row {row}</span>{seats.filter(seat => seat.rowNumber === row).map(seat => <button
        key={seat.seatId} aria-pressed={selected.includes(seat.seatId)}
        aria-label={`Row ${row}, seat ${seat.seatNumber}: ${seat.status}, ${seat.seatType}`}
        className={`seat ${seat.seatType === 'VIP' ? 'vip' : ''} ${selected.includes(seat.seatId) ? 'selected' : ''}`}
        disabled={busy || !ready || seat.status !== 'AVAILABLE'}
        onClick={() => setSelected(ids => ids.includes(seat.seatId) ? ids.filter(id => id !== seat.seatId) : [...ids, seat.seatId])}
      >{seat.seatNumber}{seat.seatType === 'VIP' ? ' VIP' : ''}</button>)}</div>)}</div>
    <p>Selected seat IDs: {selected.join(', ') || 'None'}</p>
    <button disabled={busy || !ready || !selected.length} onClick={() => void run('create')}>Create draft</button>
    {reservation && <ReservationCard reservation={reservation} seats={seats} busy={busy}
      act={(_id, action) => { if (action === 'confirm' || action === 'cancel') void run(action) }} />}
  </section>
}
