import { useEffect, useRef, useState } from 'react'
import type { FormEvent } from 'react'
import { changeReservation, decideReservation, loadReservation } from '../reservations'
import type { Reservation } from '../reservations'
import { ReservationCard } from '../components/ReservationCard'
import { Message } from '../components/ui'
import { errorText } from '../utils'

export function ReservationPage({ approver, initialId }: { approver: boolean; initialId?: number }) {
  const [id, setId] = useState(initialId?.toString() || '')
  const [reservation, setReservation] = useState<Reservation | null>(null)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const lock = useRef(false)
  async function lookup(event: FormEvent) {
    event.preventDefault()
    if (lock.current) return
    lock.current = true; setBusy(true); setError('')
    try {
      const value = Number(id)
      if (!Number.isSafeInteger(value) || value <= 0) throw new Error('Enter a positive reservation ID.')
      setReservation(await loadReservation(value))
    } catch (error) { setError(errorText(error)); setReservation(null) }
    finally { lock.current = false; setBusy(false) }
  }
  const observedId = reservation?.reservationId
  const status = reservation?.status
  useEffect(() => {
    if (!observedId || status !== 'PENDING_APPROVAL') return
    let live = true, polling = false
    const timer = window.setInterval(async () => {
      if (polling || lock.current) return
      polling = true
      try { const result = await loadReservation(observedId); if (live) setReservation(result) }
      catch (error) { if (live) setError(errorText(error)) }
      finally { polling = false }
    }, 1000)
    return () => { live = false; clearInterval(timer) }
  }, [observedId, status])
  async function act(reservationId: number, action: 'confirm' | 'cancel' | 'APPROVE' | 'REJECT') {
    if (lock.current) return
    lock.current = true; setBusy(true); setError('')
    try {
      setReservation(action === 'confirm' || action === 'cancel'
        ? await changeReservation(reservationId, action) : await decideReservation(reservationId, action))
    } catch (error) {
      setError(errorText(error))
      try { setReservation(await loadReservation(reservationId)) } catch { /* Keep the last observation visible. */ }
    } finally { lock.current = false; setBusy(false) }
  }
  return <section><h1>{approver ? 'Approver' : 'Reservation status'}</h1>
    <p>{approver ? 'C08 demo: decisions use approver 3; details are read as owner 1.' : 'C08 demo customer: owner 1.'}</p>
    <form onSubmit={event => void lookup(event)}><label>Reservation ID<input type="number" min="1" required value={id} onChange={event => setId(event.target.value)} /></label>
      <button disabled={busy}>Load reservation</button></form>
    {error && <Message error>{error}</Message>}
    {reservation && <ReservationCard reservation={reservation} busy={busy} approver={approver} act={(id, action) => void act(id, action)} />}
  </section>
}
