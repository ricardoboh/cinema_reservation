import { useEffect, useState } from 'react'
import { loadAvailability } from '../reservations'
import type { Reservation, SeatAvailability } from '../reservations'

import { Icon } from './ui'
const statusLabels: Record<Reservation['status'], string> = { DRAFT: 'Draft', PENDING_APPROVAL: 'Awaiting approval', CONFIRMED: 'Confirmed', CANCELLED: 'Cancelled', REJECTED: 'Rejected', EXPIRED: 'Expired' }
const descriptions: Record<Reservation['status'], string> = { DRAFT: 'Your draft is saved. Confirm to secure your seats or send an approval request. Drafts do not hold seats.', PENDING_APPROVAL: 'Your seats are held together while the demo approver reviews your request. Unanswered requests expire automatically.', CONFIRMED: 'You’re all set. Your seats are reserved. Enjoy the show!', CANCELLED: 'Reservation cancelled. Your seats have been released.', REJECTED: 'This request was declined. Your seats have been released.', EXPIRED: 'The approval window has closed. Your seats have been released.' }
export function ReservationCard({ reservation: r, busy, act, approver = false, seats = [] }: { reservation: Reservation; busy: boolean; act: (id: number, action: 'confirm' | 'cancel' | 'APPROVE' | 'REJECT') => void; approver?: boolean; seats?: SeatAvailability[] }) {
  const [physical,setPhysical]=useState<SeatAvailability[]>([])
  useEffect(()=>{let live=true;loadAvailability(r.screeningId).then(data=>{if(live)setPhysical(data)}).catch(()=>{});return()=>{live=false}},[r.screeningId])
  const layout=seats.length?seats:physical
  return <article className="ticket-card">
    <div className="ticket-top">
      <span className="ticket-icon"><Icon name="ticket" /></span>
      <span className={`badge status-${r.status.toLowerCase()}`}>{statusLabels[r.status]}</span>
    </div>
    <p className="eyebrow">RESERVATION #{r.id}</p>
    <h3>{r.movieTitle}</h3>
    <p className="muted">{r.hallName} / Screening {r.screeningId} / {new Date(r.startsAt).toLocaleString()}</p>
    <div className="ticket-seats">{r.seatIds.map(id => {
      const seat = layout.find(s => s.seatId === id)
      return <span key={id}>{seat ? `Row ${seat.rowNumber} · Seat ${seat.seatNumber}` : `Seat ID ${id}`}</span>
    })}</div>
    <p className="ticket-description">{descriptions[r.status]}</p>
    {r.approvalRequestedAt && <p className="muted">Approval requested: <time dateTime={r.approvalRequestedAt}>{new Date(r.approvalRequestedAt).toLocaleString()}</time></p>}
    {r.approvalDeadline && <p className="deadline">Approval deadline: <time dateTime={r.approvalDeadline}>{new Date(r.approvalDeadline).toLocaleString()}</time></p>}
    {r.decision && <p className="muted">{r.decision === 'APPROVE' ? 'Approved' : 'Rejected'} by demo approver {r.decidedBy}</p>}
    {r.decidedAt && <p className="muted">Decision recorded: <time dateTime={r.decidedAt}>{new Date(r.decidedAt).toLocaleString()}</time></p>}
    {r.cancelledAt && <p className="muted">Cancelled at: <time dateTime={r.cancelledAt}>{new Date(r.cancelledAt).toLocaleString()}</time></p>}
    {r.expiredAt && <p className="muted">Expired at: <time dateTime={r.expiredAt}>{new Date(r.expiredAt).toLocaleString()}</time></p>}
    <div className="actions">{approver ? r.status === 'PENDING_APPROVAL' && <>
      <button className="button" disabled={busy} onClick={() => act(r.id, 'APPROVE')}>Approve request</button>
      <button className="button secondary" disabled={busy} onClick={() => act(r.id, 'REJECT')}>Reject</button>
    </> : <>
      {r.status === 'DRAFT' && <button className="button" disabled={busy} onClick={() => act(r.id, 'confirm')}>Confirm reservation</button>}
      {['DRAFT', 'PENDING_APPROVAL', 'CONFIRMED'].includes(r.status) && <button className="button secondary" disabled={busy} onClick={() => act(r.id, 'cancel')}>Cancel reservation</button>}
    </>}</div>
  </article>
}
