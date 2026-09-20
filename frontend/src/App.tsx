import { useState } from 'react'
import { createReservation } from './reservations'
import './App.css'

const seats = [1, 2, 3, 4].map((id) => ({ id, label: `A${id}` }))

function App() {
  const [selectedIds, setSelectedIds] = useState<number[]>([])
  const [pending, setPending] = useState(false)
  const [reservationId, setReservationId] = useState<number | null>(null)
  const [error, setError] = useState('')
  const selectedSeats = seats.filter((seat) => selectedIds.includes(seat.id))

  function toggleSeat(id: number) {
    setSelectedIds((current) => current.includes(id)
      ? current.filter((selected) => selected !== id)
      : [...current, id])
    setReservationId(null)
    setError('')
  }

  async function reserve() {
    if (pending || selectedIds.length === 0) return
    setPending(true)
    setError('')
    setReservationId(null)
    try {
      const id = await createReservation(selectedSeats.map((seat) => seat.id))
      setReservationId(id)
      setSelectedIds([])
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Could not create the reservation.')
    } finally {
      setPending(false)
    }
  }

  return (
    <main className="reservation-page">
      <header>
        <p className="eyebrow">Reserved Bytes / CP1 demo</p>
        <h1>Cinema reservation</h1>
        <p>Choose your seats for the big screen.</p>
      </header>
      <section className="reservation-card" aria-labelledby="screening-title">
        <div className="screening-info">
          <div>
            <p className="eyebrow">Development screening / 1</p>
            <h2 id="screening-title">CP1 Demo Movie</h2>
            <p>Hall A</p>
          </div>
          <span className="badge">Draft reservation</span>
        </div>
        <div className="auditorium">
          <div className="cinema-screen">Screen</div>
          <p id="seat-instructions">Select a seat. Select it again to remove it.</p>
          <div className="seat-row" role="group" aria-label="Row A seats" aria-describedby="seat-instructions">
            {seats.map((seat) => (
              <button key={seat.id} type="button" className="seat"
                aria-label={`Seat ${seat.label}`} aria-pressed={selectedIds.includes(seat.id)}
                disabled={pending} onClick={() => toggleSeat(seat.id)}>
                {seat.label}
                <span aria-hidden="true">{selectedIds.includes(seat.id) ? '✓' : '○'}</span>
              </button>
            ))}
          </div>
          <p className="seat-note">Four development seats / Row A</p>
        </div>
        <div className="reservation-summary">
          <div>
            <h3>Selected seats</h3>
            <p aria-live="polite">{selectedSeats.length ? selectedSeats.map((seat) => seat.label).join(', ') : 'No seats selected'}</p>
          </div>
          <button className="reserve-button" type="button" disabled={pending || selectedIds.length === 0} onClick={reserve}>
            {pending ? 'Reserving…' : 'Reserve'}
          </button>
        </div>
        {reservationId !== null && (
          <div className="message success" role="status">
            <strong>Draft reservation created successfully.</strong>
            <p>Reservation ID: {reservationId}</p>
          </div>
        )}
        {error && <div className="message error" role="alert">{error}</div>}
      </section>
      <p className="footnote">This demo creates a draft reservation. Seats are not held or confirmed.</p>
    </main>
  )
}

export default App
