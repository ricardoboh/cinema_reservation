import { useEffect, useState } from 'react'
export type Movie = { id: number; title: string }
export type Hall = { id: number; name: string }
export type Screening = { id: number; movie: Movie; hall: Hall; startsAt: string }
export type Seat = { id: number; rowNumber: number; seatNumber: number; seatType: 'STANDARD' | 'VIP' }

export async function api<T>(path: string, method = 'GET', body?: unknown): Promise<T> {
  const response = await fetch(path, {
    method, headers: { Accept: 'application/json', 'Content-Type': 'application/json' },
    body: body === undefined ? undefined : JSON.stringify(body),
  })
  const data = await response.json().catch(() => null)
  if (!response.ok) throw new Error(data?.detail || `Request failed (HTTP ${response.status}).`)
  return data as T
}
export function useProgramme() {
  const [movies, setMovies] = useState<Movie[]>([])
  const [screenings, setScreenings] = useState<Screening[]>([])
  const [error, setError] = useState('')
  useEffect(() => {
    let live = true
    Promise.all([api<Movie[]>('/movies'), api<Screening[]>('/screenings?upcoming=true')])
      .then(([movies, screenings]) => { if (live) { setMovies(movies); setScreenings(screenings) } })
      .catch(error => { if (live) setError(String(error)) })
    return () => { live = false }
  }, [])
  return { movies, screenings, error }
}
