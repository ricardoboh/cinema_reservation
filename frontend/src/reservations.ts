export const DEMO_USER_ID = 1
export const DEMO_SCREENING_ID = 1

export type ReservationStatus = 'DRAFT' | 'CONFIRMED' | 'CANCELLED'
export type SeatAvailability = {
  seatId: number
  rowNumber: number
  seatNumber: number
  availability: 'AVAILABLE' | 'UNAVAILABLE'
}
export type Reservation = { id: number; status: ReservationStatus; seatIds: number[] }

async function request(path: string, expectedStatus: number, options: RequestInit = {}): Promise<unknown> {
  let response: Response
  try {
    response = await fetch(path, { ...options, signal: AbortSignal.timeout(15000) })
  } catch {
    throw new Error('Could not reach the reservation API. Check that the dev backend is running on port 8080. If a request timed out, its outcome may be unknown.')
  }
  const body: unknown = await response.json().catch(() => null)
  if (response.status !== expectedStatus) {
    const detail = body && typeof body === 'object' && 'detail' in body && typeof body.detail === 'string'
      ? body.detail : 'The API could not complete the request.'
    throw new Error(`${detail} (HTTP ${response.status})`)
  }
  return body
}

function positiveId(value: unknown): value is number {
  return typeof value === 'number' && Number.isSafeInteger(value) && value > 0
}

export async function loadAvailability(): Promise<SeatAvailability[]> {
  const body = await request(`/screenings/${DEMO_SCREENING_ID}/availability`, 200)
  if (!Array.isArray(body) || !body.every((seat: unknown) => seat && typeof seat === 'object'
    && 'seatId' in seat && positiveId(seat.seatId)
    && 'rowNumber' in seat && positiveId(seat.rowNumber)
    && 'seatNumber' in seat && positiveId(seat.seatNumber)
    && 'availability' in seat && ['AVAILABLE', 'UNAVAILABLE'].includes(String(seat.availability)))) {
    throw new Error('The API returned invalid seat availability.')
  }
  return body as SeatAvailability[]
}

export async function createReservation(seatIds: number[]): Promise<Reservation> {
  const body = await request('/reservations', 201, {
    method: 'POST', headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ userId: DEMO_USER_ID, screeningId: DEMO_SCREENING_ID, seatIds }),
  })
  if (!body || typeof body !== 'object' || !('id' in body) || !positiveId(body.id)) {
    throw new Error('The API reported creation but returned no valid reservation ID. Check the backend before trying again.')
  }
  // Baseline Create returns only an ID; a successful 201 guarantees DRAFT.
  return { id: body.id, status: 'DRAFT', seatIds: [...seatIds] }
}

export async function changeReservation(id: number, operation: 'confirm' | 'cancel'): Promise<Reservation> {
  const body = await request(`/reservations/${id}/${operation}`, 200, {
    method: 'POST', headers: { 'X-User-Id': String(DEMO_USER_ID) },
  })
  if (!body || typeof body !== 'object' || !('id' in body) || body.id !== id
    || !('status' in body) || !['DRAFT', 'CONFIRMED', 'CANCELLED'].includes(String(body.status))
    || !('seatIds' in body) || !Array.isArray(body.seatIds) || !body.seatIds.every(positiveId)) {
    throw new Error('The API reported success but returned an invalid reservation. Its state may have changed; check the backend before retrying.')
  }
  return { id, status: body.status as ReservationStatus, seatIds: body.seatIds }
}
