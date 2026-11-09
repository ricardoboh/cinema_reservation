export const DEMO_USER_ID = 1
export type SeatAvailability = {
  seatId: number; rowNumber: number; seatNumber: number
  seatType: 'STANDARD' | 'VIP'; status: 'AVAILABLE' | 'UNAVAILABLE'
}
export type Reservation = {
  reservationId: number; userId: number; screeningId: number; seats: number[]
  movieTitle: string; hallName: string; startsAt: string
  status: 'DRAFT' | 'PENDING_APPROVAL' | 'CONFIRMED' | 'CANCELLED' | 'REJECTED' | 'EXPIRED'
  createdAt: string; cancelledAt: string | null; approvalRequestedAt: string | null
  approvalDeadline: string | null; approvalPolicy: string | null
  approvalRequiredSeatIds: number[]; authorizedApproverId: number | null
  decision: 'APPROVE' | 'REJECT' | null; approverId: number | null
  decisionAt: string | null; expiredAt: string | null
}
async function request(path: string, expected: number, options: RequestInit = {}): Promise<unknown> {
  let response: Response
  try { response = await fetch(path, { ...options, headers: { Accept: 'application/json', ...options.headers } }) }
  catch { throw new Error('Could not reach the backend. Refresh reservation status before retrying.') }
  const body: unknown = await response.json().catch(() => null)
  if (response.status !== expected) {
    const detail = body && typeof body === 'object' && 'detail' in body && typeof body.detail === 'string'
      ? body.detail : `Request failed (HTTP ${response.status}).`
    throw new Error(detail)
  }
  return body
}
const positiveId = (value: unknown): value is number => typeof value === 'number' && Number.isSafeInteger(value) && value > 0
function reservation(body: unknown): Reservation {
  if (!body || typeof body !== 'object' || !('reservationId' in body) || !positiveId(body.reservationId)
    || !('status' in body) || !['DRAFT', 'PENDING_APPROVAL', 'CONFIRMED', 'CANCELLED', 'REJECTED', 'EXPIRED'].includes(String(body.status))
    || !('seats' in body) || !Array.isArray(body.seats) || !body.seats.every(positiveId)) {
    throw new Error('Invalid reservation response.')
  }
  return body as Reservation
}
export async function loadAvailability(screeningId: number): Promise<SeatAvailability[]> {
  const body = await request(`/screenings/${screeningId}/availability`, 200)
  if (!Array.isArray(body) || !body.every(seat => seat && typeof seat === 'object'
    && positiveId(seat.seatId) && positiveId(seat.rowNumber) && positiveId(seat.seatNumber)
    && ['STANDARD', 'VIP'].includes(seat.seatType) && ['AVAILABLE', 'UNAVAILABLE'].includes(seat.status))) {
    throw new Error('Invalid availability response.')
  }
  return body as SeatAvailability[]
}
export async function loadReservation(id: number): Promise<Reservation> {
  const result = reservation(await request(`/reservations/${id}`, 200, { headers: { 'X-User-Id': String(DEMO_USER_ID) } }))
  if (result.reservationId !== id) throw new Error('Invalid reservation response.')
  return result
}
export async function createReservation(seatIds: number[], screeningId: number): Promise<Reservation> {
  return reservation(await request('/reservations', 201, {
    method: 'POST', headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ userId: DEMO_USER_ID, screeningId, seatIds }),
  }))
}
export async function changeReservation(id: number, operation: 'confirm' | 'cancel'): Promise<Reservation> {
  return reservation(await request(`/reservations/${id}/${operation}`, 200, {
    method: 'POST', headers: { 'X-User-Id': String(DEMO_USER_ID) },
  }))
}
export async function decideReservation(id: number, decision: 'APPROVE' | 'REJECT'): Promise<Reservation> {
  return reservation(await request(`/reservations/${id}/approval`, 200, {
    method: 'POST', headers: { 'X-User-Id': '3', 'Content-Type': 'application/json' },
    body: JSON.stringify({ decision }),
  }))
}
