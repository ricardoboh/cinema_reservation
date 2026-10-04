export const DEMO_USER_ID = 1
export const DEMO_SCREENING_ID = 1
export type SeatAvailability = { seatId: number; rowNumber: number; seatNumber: number; availability: 'AVAILABLE' | 'UNAVAILABLE'; approvalRequired: boolean }
export type Reservation = {
  id: number; status: 'DRAFT' | 'PENDING_APPROVAL' | 'CONFIRMED' | 'CANCELLED' | 'REJECTED' | 'EXPIRED'; seatIds: number[]
  approvalRequestedAt: string | null; approvalDeadline: string | null; approvalPolicy: string | null
  decision: string | null; decidedBy: number | null; decidedAt: string | null; expiredAt: string | null; cancelledAt: string | null
}
async function request(path: string, expected: number, options: RequestInit = {}): Promise<unknown> {
  let response: Response
  try { response = await fetch(path, { ...options, signal: AbortSignal.timeout(15000) }) }
  catch { throw new Error('Could not reach the API. Check the backend. A timed-out mutation may have succeeded; refresh before retrying.') }
  const body: unknown = await response.json().catch(() => null)
  if (response.status !== expected) {
    const detail = body && typeof body === 'object' && 'detail' in body && typeof body.detail === 'string' ? body.detail : 'Request failed.'
    throw new Error(`${detail} (HTTP ${response.status})`)
  }
  return body
}
const positiveId = (value: unknown): value is number => typeof value === 'number' && Number.isSafeInteger(value) && value > 0
export async function loadAvailability(): Promise<SeatAvailability[]> {
  const body = await request(`/screenings/${DEMO_SCREENING_ID}/availability`, 200)
  if (!Array.isArray(body) || !body.every((seat: unknown) => seat && typeof seat === 'object'
    && 'seatId' in seat && positiveId(seat.seatId) && 'rowNumber' in seat && positiveId(seat.rowNumber)
    && 'seatNumber' in seat && positiveId(seat.seatNumber) && 'approvalRequired' in seat && typeof seat.approvalRequired === 'boolean'
    && 'availability' in seat && ['AVAILABLE', 'UNAVAILABLE'].includes(String(seat.availability)))) throw new Error('Invalid availability response.')
  return body as SeatAvailability[]
}
export async function loadReservation(id: number): Promise<Reservation> {
  const body = await request(`/reservations/${id}`, 200, { headers: { 'X-User-Id': String(DEMO_USER_ID) } })
  if (!body || typeof body !== 'object' || !('id' in body) || body.id !== id
    || !('status' in body) || !['DRAFT','PENDING_APPROVAL','CONFIRMED','CANCELLED','REJECTED','EXPIRED'].includes(String(body.status))
    || !('seatIds' in body) || !Array.isArray(body.seatIds) || !body.seatIds.every(positiveId)) throw new Error('Invalid reservation response; refresh before retrying.')
  return body as Reservation
}
export async function createReservation(seatIds: number[]): Promise<Reservation> {
  const body = await request('/reservations', 201, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ userId: DEMO_USER_ID, screeningId: DEMO_SCREENING_ID, seatIds }) })
  if (!body || typeof body !== 'object' || !('id' in body) || !positiveId(body.id)) throw new Error('Created but no valid ID returned; check backend before retrying.')
  return loadReservation(body.id)
}
export async function changeReservation(id: number, operation: 'confirm' | 'cancel'): Promise<Reservation> {
  await request(`/reservations/${id}/${operation}`, 200, { method: 'POST', headers: { 'X-User-Id': String(DEMO_USER_ID) } })
  return loadReservation(id)
}
export async function decideReservation(id: number, decision: 'APPROVE' | 'REJECT', actor = 3): Promise<Reservation> {
  await request(`/reservations/${id}/approval`, 200, { method: 'POST', headers: { 'X-User-Id': String(actor), 'Content-Type': 'application/json' }, body: JSON.stringify({ decision }) })
  return loadReservation(id)
}
