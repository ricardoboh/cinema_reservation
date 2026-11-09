import type { Reservation, SeatAvailability } from '../reservations'

const labels: Record<Reservation['status'], string> = {
  DRAFT: 'Draft', PENDING_APPROVAL: 'Awaiting approval', CONFIRMED: 'Confirmed',
  CANCELLED: 'Cancelled', REJECTED: 'Rejected', EXPIRED: 'Expired',
}
type Action = 'confirm' | 'cancel' | 'APPROVE' | 'REJECT'
export function ReservationCard({ reservation: r, busy, act, approver = false, seats = [] }: {
  reservation: Reservation; busy: boolean; act: (id: number, action: Action) => void
  approver?: boolean; seats?: SeatAvailability[]
}) {
  const times: [string, string | null][] = [
    ['Created', r.createdAt], ['Approval requested', r.approvalRequestedAt],
    ['Approval deadline', r.approvalDeadline], ['Decision recorded', r.decisionAt],
    ['Cancelled at', r.cancelledAt], ['Expired at', r.expiredAt],
  ]
  return <article className="ticket-card" aria-label={`Reservation ${r.reservationId}`}>
    <h2>Reservation #{r.reservationId}</h2>
    <p className={`badge status-${r.status.toLowerCase()}`}>{labels[r.status]}</p>
    <p>{r.movieTitle} · {r.hallName} · {new Date(r.startsAt).toLocaleString()}</p>
    <p>Seats: {r.seats.map(id => {
      const seat = seats.find(seat => seat.seatId === id)
      return seat ? `${seat.rowNumber}:${seat.seatNumber}` : `ID ${id}`
    }).join(', ')}</p>
    {r.status === 'DRAFT' && <p>Drafts do not hold seats. Confirm to request allocation.</p>}
    {r.status === 'PENDING_APPROVAL' && <p>All selected seats are held until a decision or the approval deadline.</p>}
    {r.decision && <p>{r.decision === 'APPROVE' ? 'Approved' : 'Rejected'} by approver {r.approverId}</p>}
    {times.filter(([, value]) => value !== null).map(([label, value]) => <p key={label}>
      {label}: <time dateTime={value!}>{new Date(value!).toLocaleString()}</time>
    </p>)}
    <div className="actions">{approver ? r.status === 'PENDING_APPROVAL' && <>
      <button disabled={busy} onClick={() => act(r.reservationId, 'APPROVE')}>Approve</button>
      <button disabled={busy} onClick={() => act(r.reservationId, 'REJECT')}>Reject</button>
    </> : <>
      {r.status === 'DRAFT' && <button disabled={busy} onClick={() => act(r.reservationId, 'confirm')}>Confirm reservation</button>}
      {['DRAFT', 'PENDING_APPROVAL', 'CONFIRMED'].includes(r.status)
        && <button disabled={busy} onClick={() => act(r.reservationId, 'cancel')}>Cancel reservation</button>}
    </>}</div>
  </article>
}
