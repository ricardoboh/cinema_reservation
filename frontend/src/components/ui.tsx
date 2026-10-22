import type { ReactNode } from 'react'
import { useState } from 'react'
import type { Movie, Screening } from '../cinema'
import { Link } from '../router'
export function Icon({ name, size = 20 }: { name: 'ticket' | 'sun' | 'moon' | 'arrow' | 'menu' | 'clock' | 'check' | 'eye'; size?: number }) {
  const paths = { ticket: 'M4 4h16v5a3 3 0 0 0 0 6v5H4v-5a3 3 0 0 0 0-6V4Zm10 0v3m0 3v4m0 3v3', sun: 'M12 8a4 4 0 1 0 0 8 4 4 0 0 0 0-8Zm0-6v2m0 16v2M2 12h2m16 0h2M5 5l1.5 1.5m11 11L19 19M5 19l1.5-1.5m11-11L19 5', moon: 'M20 15.5A9 9 0 0 1 8.5 4 9 9 0 1 0 20 15.5Z', arrow: 'M4 12h16m-6-6 6 6-6 6', menu: 'M4 6h16M4 12h16M4 18h16', clock: 'M12 8v5l3 2M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0Z', check: 'm5 12 4 4L19 6', eye: 'M2 12s4-7 10-7 10 7 10 7-4 7-10 7S2 12 2 12Zm13 0a3 3 0 1 1-6 0 3 3 0 0 1 6 0Z' }
  return <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true"><path d={paths[name]} /></svg>
}
export function Poster({ movie, hero = false }: { movie: Movie; hero?: boolean }) {
  const [failedUrl, setFailedUrl] = useState<string | null>(null)
  const hasImage = Boolean(movie.posterUrl && failedUrl !== movie.posterUrl)
  return <div className={['poster', 'poster-edition-' + movie.id % 3, hasImage ? 'has-image' : '', hero ? 'hero-art' : ''].join(' ')}>
    {hasImage ? <img src={movie.posterUrl} alt={movie.title} loading="lazy" onError={() => setFailedUrl(movie.posterUrl)} /> : <>
      <div className="poster-motif" aria-hidden="true"><span /><span /><span /></div>
      <div className="poster-type"><span>DEADLOCK CINEMA · SELECTED STORIES</span><strong>{movie.title}</strong><small>{movie.genre || 'Cinema'} · {movie.releaseYear}</small></div>
      <span className="poster-footer" aria-hidden="true">THE ART OF THE BIG SCREEN</span>
    </>}
  </div>
}
export function MovieCard({ movie, screenings = [] }: { movie: Movie; screenings?: Screening[] }) {
  const [now] = useState(() => Date.now())
  const shows = screenings.filter(s => s.movie.id === movie.id && !s.cancelled && Date.parse(s.startsAt) > now)
  return <article className="movie-card"><Link to={'/movies/' + movie.id} className="poster-link" aria-label={'View ' + movie.title}><Poster movie={movie} /></Link><div className="movie-card-content"><p className="movie-category">{movie.genre || 'Cinema'}</p><h3><Link to={'/movies/' + movie.id}>{movie.title}</Link></h3><p className="movie-metadata">{movie.durationMinutes} min · {movie.releaseYear}</p><div className="movie-card-footer"><p className="screening-caption">{shows.length ? shows.length + ' upcoming screening' + (shows.length === 1 ? '' : 's') : 'No upcoming screenings'}</p>{shows.length ? <Link className="button secondary full" to={'/movies/' + movie.id}>Choose a screening <Icon name="arrow" size={17} /></Link> : <span className="unavailable-booking">Programme coming soon</span>}</div></div></article>
}
export function Message({ children, error = false }: { children: ReactNode; error?: boolean }) { return <div className={`message ${error ? 'error' : 'success'}`} role={error ? 'alert' : 'status'}>{children}</div> }
