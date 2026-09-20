// Vite forwards this request to http://localhost:8080/reservations.
export async function createReservation(seatIds: number[]): Promise<number> {
  let response: Response
  try {
    response = await fetch('/reservations', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ userId: 1, screeningId: 1, seatIds }),
      signal: AbortSignal.timeout(15000),
    })
  } catch {
    throw new Error('Could not reach the reservation API. Check that Spring Boot is running on port 8080 with the dev profile. If the request timed out, its outcome may be unknown.')
  }
  const body: unknown = await response.json().catch(() => null)
  if (response.status !== 201) {
    const detail = body && typeof body === 'object' && 'detail' in body && typeof body.detail === 'string'
      ? body.detail
      : 'Could not create the reservation. Check that Spring Boot is running on port 8080 with the dev profile.'
    throw new Error(`${detail} (HTTP ${response.status})`)
  }
  if (!body || typeof body !== 'object' || !('id' in body) || typeof body.id !== 'number' || !Number.isSafeInteger(body.id) || body.id <= 0) {
    throw new Error('The API reported creation but returned no valid reservation ID. Check the backend before trying again.')
  }
  return body.id
}
