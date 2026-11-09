import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { api } from '../cinema'
import type { Movie, Hall, Screening, Seat } from '../cinema'
import { Link } from '../router'
import { Message } from '../components/ui'
import { errorText } from '../utils'

export function Admin({ path }: { path: string }) {
  const [movies, setMovies] = useState<Movie[]>([])
  const [halls, setHalls] = useState<Hall[]>([])
  const [screenings, setScreenings] = useState<Screening[]>([])
  const [seats, setSeats] = useState<Seat[]>([])
  const [movie, setMovie] = useState<Movie>({ id: 0, title: '' })
  const [hall, setHall] = useState<Hall>({ id: 0, name: '' })
  const [layout, setLayout] = useState({ rows: 1, seatsPerRow: 4 })
  const [screening, setScreening] = useState({ movieId: '', hallId: '', startsAt: '' })
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const section = path.split('/')[2] || 'movies'
  async function reload() {
    const [movies, halls, screenings] = await Promise.all([
      api<Movie[]>('/movies'), api<Hall[]>('/halls'), api<Screening[]>('/screenings'),
    ])
    setMovies(movies); setHalls(halls); setScreenings(screenings)
  }
  useEffect(() => { void Promise.resolve().then(reload).catch(error => setError(errorText(error))) }, [])
  async function run(action: () => Promise<unknown>) {
    if (busy) return
    setBusy(true); setError('')
    try { await action(); await reload() } catch (error) { setError(errorText(error)) }
    finally { setBusy(false) }
  }
  function submit(event: FormEvent, action: () => Promise<unknown>) { event.preventDefault(); void run(action) }
  async function selectHall(hall: Hall) {
    setHall(hall); setSeats([])
    try { setSeats(await api<Seat[]>(`/halls/${hall.id}/seats`)) } catch (error) { setError(errorText(error)) }
  }
  return <section><h1>Cinema management</h1><p>C08 local demo identity convention; no production authentication.</p>
    <nav aria-label="Cinema management"><Link to="/admin/movies">Movies</Link><Link to="/admin/halls">Halls and seats</Link><Link to="/admin/screenings">Screenings</Link></nav>
    {error && <Message error>{error}</Message>}
    {section === 'movies' && <><h2>Movie catalogue</h2>
      {movies.map(item => <p key={item.id}>{item.title} <button disabled={busy} onClick={() => setMovie(item)}>Edit</button>
        <button disabled={busy} onClick={() => void run(async () => { await api(`/movies/${item.id}`, 'DELETE'); if (movie.id === item.id) setMovie({ id: 0, title: '' }) })}>Delete</button></p>)}
      <form onSubmit={event => submit(event, async () => {
        await api(`/movies${movie.id ? `/${movie.id}` : ''}`, movie.id ? 'PUT' : 'POST', { title: movie.title })
        setMovie({ id: 0, title: '' })
      })}><label>Movie title<input required value={movie.title} onChange={event => setMovie({ ...movie, title: event.target.value })} /></label>
        <button disabled={busy}>Save movie</button><button type="button" onClick={() => setMovie({ id: 0, title: '' })}>New movie</button></form>
    </>}
    {section === 'halls' && <><h2>Cinema halls</h2>
      <div className="actions">{halls.map(item => <button key={item.id} disabled={busy} onClick={() => void selectHall(item)}>{item.name}</button>)}</div>
      <form onSubmit={event => submit(event, async () => {
        const saved = await api<Hall>(`/halls${hall.id ? `/${hall.id}` : ''}`, hall.id ? 'PUT' : 'POST', { name: hall.name })
        await selectHall(saved)
      })}><label>Hall name<input required value={hall.name} onChange={event => setHall({ ...hall, name: event.target.value })} /></label>
        <button disabled={busy}>Save hall</button><button type="button" onClick={() => { setHall({ id: 0, name: '' }); setSeats([]) }}>New hall</button></form>
      {hall.id > 0 && <><form onSubmit={event => submit(event, async () => {
        setSeats(await api<Seat[]>(`/halls/${hall.id}/layout`, 'PUT', layout))
      })}><h3>{hall.name}: seat layout</h3><label>Rows<input type="number" min="1" required value={layout.rows} onChange={event => setLayout({ ...layout, rows: Number(event.target.value) })} /></label>
        <label>Seats per row<input type="number" min="1" required value={layout.seatsPerRow} onChange={event => setLayout({ ...layout, seatsPerRow: Number(event.target.value) })} /></label>
        <button disabled={busy}>Save layout</button></form><p>Publishing a screening fixes its layout. Configure VIP seats before creating the screening.</p>
        <div className="seat-map">{seats.map(seat => <label className="seat-setting" key={seat.id}>Row {seat.rowNumber}, seat {seat.seatNumber}
          <select disabled={busy} value={seat.seatType} onChange={event => void run(async () => {
            await api(`/halls/${hall.id}/seats/${seat.id}`, 'PUT', { seatType: event.target.value })
            setSeats(await api<Seat[]>(`/halls/${hall.id}/seats`))
          })}><option>STANDARD</option><option>VIP</option></select></label>)}</div>
      </>}
    </>}
    {section === 'screenings' && <><h2>Screenings</h2>
      {screenings.map(item => <p className="panel" key={item.id}>{item.movie.title} · {item.hall.name} · <time dateTime={item.startsAt}>{new Date(item.startsAt).toLocaleString()}</time></p>)}
      <p>A published screening has an immutable UTC start time.</p>
      <form onSubmit={event => submit(event, async () => {
        await api('/screenings', 'POST', { movieId: Number(screening.movieId), hallId: Number(screening.hallId), startsAt: new Date(screening.startsAt).toISOString() })
        setScreening({ movieId: '', hallId: '', startsAt: '' })
      })}><label>Movie<select required value={screening.movieId} onChange={event => setScreening({ ...screening, movieId: event.target.value })}>
        <option value="">Choose movie</option>{movies.map(movie => <option key={movie.id} value={movie.id}>{movie.title}</option>)}</select></label>
        <label>Hall<select required value={screening.hallId} onChange={event => setScreening({ ...screening, hallId: event.target.value })}>
          <option value="">Choose hall</option>{halls.map(hall => <option key={hall.id} value={hall.id}>{hall.name}</option>)}</select></label>
        <label>Start (local input, stored in UTC)<input type="datetime-local" required value={screening.startsAt} onChange={event => setScreening({ ...screening, startsAt: event.target.value })} /></label>
        <button disabled={busy}>Create screening</button></form>
    </>}
  </section>
}
