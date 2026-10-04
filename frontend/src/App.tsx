import { useEffect, useState } from 'react'
import { changeReservation, createReservation, DEMO_SCREENING_ID, DEMO_USER_ID, loadAvailability } from './reservations'
import type { Reservation, SeatAvailability } from './reservations'
import './App.css'

function seatLabel(seat: SeatAvailability) {
  return `Row ${seat.rowNumber}, seat ${seat.seatNumber}`
}
function errorMessage(cause: unknown) {
  return cause instanceof Error ? cause.message : 'Could not complete the request.'
}

function App() {
  const [seats, setSeats] = useState<SeatAvailability[]>([])
  const [availabilityReady, setAvailabilityReady] = useState(false)
  const [selectedIds, setSelectedIds] = useState<number[]>([])
  const [pending, setPending] = useState('Loading availability')
  const [reservation, setReservation] = useState<Reservation | null>(null)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  const activeReservation = reservation !== null && reservation.status !== 'CANCELLED'
  const selectedSeats = seats.filter((seat) => selectedIds.includes(seat.seatId))
  const rows = [...new Set(seats.map((seat) => seat.rowNumber))].sort((a, b) => a - b)

  useEffect(() => {
    let active = true
    loadAvailability().then((loaded) => {
      if (active) { setSeats(loaded); setAvailabilityReady(true) }
    }).catch((cause: unknown) => {
      if (active) setError(errorMessage(cause))
    }).finally(() => { if (active) setPending('') })
    return () => { active = false }
  }, [])

  async function reloadAvailability() {
    setAvailabilityReady(false)
    const loaded = await loadAvailability()
    setSeats(loaded)
    setSelectedIds((current) => current.filter((id) => loaded.some((seat) => seat.seatId === id && seat.availability === 'AVAILABLE')))
    setAvailabilityReady(true)
  }

  async function refresh() {
    if (pending) return
    setPending('Refreshing availability')
    setError('')
    setSuccess('')
    try { await reloadAvailability(); setSuccess('Availability refreshed from the backend.') }
    catch (cause) { setError(errorMessage(cause)) }
    finally { setPending('') }
  }

  function toggleSeat(id: number) {
    setSelectedIds((current) => current.includes(id) ? current.filter((selected) => selected !== id) : [...current, id])
    setError('')
    setSuccess('')
  }

  async function reserve() {
    if (pending || !availabilityReady || activeReservation || selectedIds.length === 0) return
    setPending('Creating reservation')
    setError('')
    setSuccess('')
    try {
      const created = await createReservation(selectedIds)
      setReservation(created)
      setSelectedIds([])
      setSuccess(`Reservation ${created.id} created (HTTP 201). DRAFT does not block seats.`)
      try { await reloadAvailability() }
      catch (cause) { setError(`Reservation created, but availability could not be refreshed: ${errorMessage(cause)}`) }
    } catch (cause) { setError(errorMessage(cause)) }
    finally { setPending('') }
  }

  async function transition(operation: 'confirm' | 'cancel') {
    if (pending || !reservation) return
    setPending(operation === 'confirm' ? 'Confirming reservation' : 'Cancelling reservation')
    setError('')
    setSuccess('')
    try {
      const updated = await changeReservation(reservation.id, operation)
      setReservation(updated)
      setSuccess(`Reservation ${updated.id} is ${updated.status} (HTTP 200).`)
      try { await reloadAvailability() }
      catch (cause) { setError(`Reservation updated, but availability could not be refreshed: ${errorMessage(cause)}`) }
    } catch (cause) {
      const message = errorMessage(cause)
      try { await reloadAvailability(); setError(message) }
      catch (refreshCause) { setError(`${message} Availability refresh also failed: ${errorMessage(refreshCause)}`) }
    } finally { setPending('') }
  }

  return (
    <main className="reservation-page">
      <header>
        <p className="eyebrow">Reserved Bytes / C02 demo</p>
        <h1>Cinema reservation</h1>
        <p>Check seats, create a draft, then confirm or cancel.</p>
      </header>
      <section className="reservation-card" aria-labelledby="screening-title" aria-busy={Boolean(pending)}>
        <div className="screening-info">
          <div>
            <p className="eyebrow">Development screening / {DEMO_SCREENING_ID}</p>
            <h2 id="screening-title">CP1 Demo Movie</h2>
            <p>Hall A · Demo user ID: {DEMO_USER_ID}</p>
            <p className="seat-note">Fixture information. Starts one day after the dev backend starts.</p>
          </div>
          <button type="button" className="secondary-button" disabled={Boolean(pending)} onClick={refresh}>Check Availability</button>
        </div>
        <div className="auditorium">
          <div className="cinema-screen">Screen</div>
          <p id="seat-instructions">Select available seats. Unavailable seats cannot be selected.</p>
          {rows.map((row) => (
            <div className="seat-row" key={row} role="group" aria-label={`Row ${row}`} aria-describedby="seat-instructions">
              {seats.filter((seat) => seat.rowNumber === row).map((seat) => (
                <button key={seat.seatId} type="button"
                  className={`seat ${seat.availability === 'UNAVAILABLE' ? 'unavailable' : ''}`}
                  aria-label={`${seatLabel(seat)}: ${seat.availability}`} aria-pressed={selectedIds.includes(seat.seatId)}
                  disabled={Boolean(pending) || !availabilityReady || activeReservation || seat.availability === 'UNAVAILABLE'}
                  onClick={() => toggleSeat(seat.seatId)}>
                  {seat.rowNumber}:{seat.seatNumber}
                  <span>{selectedIds.includes(seat.seatId) ? 'Selected' : seat.availability === 'AVAILABLE' ? 'Available' : 'Unavailable'}</span>
                </button>
              ))}
            </div>
          ))}
          {!pending && availabilityReady && seats.length === 0 && <p>No seats returned for this screening.</p>}
          {!pending && !availabilityReady && <p>Availability is unavailable or stale. Use Check Availability to retry.</p>}
          <p className="seat-note">AVAILABLE = selectable · UNAVAILABLE = confirmed reservation</p>
        </div>
        <div className="reservation-summary">
          <div>
            <h3>Selected seats</h3>
            <p aria-live="polite">{selectedSeats.length ? selectedSeats.map(seatLabel).join('; ') : 'No seats selected'}</p>
          </div>
          <button className="reserve-button" type="button"
            disabled={Boolean(pending) || !availabilityReady || activeReservation || selectedIds.length === 0} onClick={reserve}>
            Create Reservation
          </button>
        </div>
        <div className="current-reservation" aria-live="polite">
          <h3>Current reservation</h3>
          {reservation ? <>
            <p>Reservation ID: <strong>{reservation.id}</strong> · Status: <strong className="badge">{reservation.status}</strong></p>
            <p>Reserved seats: {reservation.seatIds.map((id) => {
              const seat = seats.find((item) => item.seatId === id)
              return seat ? seatLabel(seat) : `Seat ID ${id}`
            }).join('; ')}</p>
            <p className="seat-note">{reservation.status === 'DRAFT' ? 'This draft does not block seats. Confirm checks availability.'
              : reservation.status === 'CONFIRMED' ? 'These seats are now allocated to this reservation.'
                : 'The reservation is cancelled. You can select seats and create another draft.'}</p>
            <div className="reservation-actions">
              {reservation.status === 'DRAFT' && <button className="reserve-button" disabled={Boolean(pending)} onClick={() => transition('confirm')}>Confirm Reservation</button>}
              {activeReservation && <button className="secondary-button" disabled={Boolean(pending)} onClick={() => transition('cancel')}>Cancel Reservation</button>}
            </div>
          </> : <p>No reservation created yet.</p>}
        </div>
        {pending && <p className="progress" role="status">{pending}…</p>}
        {success && <div className="message success" role="status">{success}</div>}
        {error && <div className="message error" role="alert">{error}</div>}
      </section>
      <p className="footnote">Development demo · Only CONFIRMED reservations block seats. Reloading the page clears the current reservation from this UI.</p>
    </main>
  )
}

export default App
