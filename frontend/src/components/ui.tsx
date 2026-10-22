import type { ReactNode } from 'react'
import type { Movie } from '../data/catalog'
import { Link } from '../router'
export function Icon({ name, size = 20 }: { name: 'ticket' | 'sun' | 'moon' | 'arrow' | 'menu' | 'clock' | 'check' | 'eye'; size?: number }) {
  const paths = { ticket: 'M4 4h16v5a3 3 0 0 0 0 6v5H4v-5a3 3 0 0 0 0-6V4Zm10 0v3m0 3v4m0 3v3', sun: 'M12 8a4 4 0 1 0 0 8 4 4 0 0 0 0-8Zm0-6v2m0 16v2M2 12h2m16 0h2M5 5l1.5 1.5m11 11L19 19M5 19l1.5-1.5m11-11L19 5', moon: 'M20 15.5A9 9 0 0 1 8.5 4 9 9 0 1 0 20 15.5Z', arrow: 'M4 12h16m-6-6 6 6-6 6', menu: 'M4 6h16M4 12h16M4 18h16', clock: 'M12 8v5l3 2M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0Z', check: 'm5 12 4 4L19 6', eye: 'M2 12s4-7 10-7 10 7 10 7-4 7-10 7S2 12 2 12Zm13 0a3 3 0 1 1-6 0 3 3 0 0 1 6 0Z' }
  return <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true"><path d={paths[name]} /></svg>
}
export function Poster({ movie, hero = false }: { movie: Movie; hero?: boolean }) {
  return <div className={`poster art-${movie.art} ${hero ? 'hero-art' : ''}`} role="img" aria-label={`Original illustrated poster for ${movie.title}`}><div className="art-grain" /><div className="art-orb" /><div className="art-landscape" /><div className="poster-type"><span>A DEADLOCK CINEMA SELECTION</span><strong>{movie.live ? <>THE NEXT<br />GREAT STORY</> : movie.title}</strong><small>{movie.tagline}</small></div><span className="poster-credit">A LITTLE ESCAPE. A BIG SCREEN.</span></div>
}
export function MovieCard({ movie }: { movie: Movie }) {
  return <article className="movie-card"><Link to={`/movies/${movie.id}`} className="poster-link"><Poster movie={movie} /><span className={`poster-tag ${movie.live ? 'live' : ''}`}>{movie.live ? 'Booking available' : 'Editorial preview'}</span><span className="poster-open"><Icon name="arrow" /></span></Link><p className="movie-category">{movie.category}</p><h3><Link to={`/movies/${movie.id}`}>{movie.title}</Link></h3><div className="movie-bottom"><span><Icon name="clock" size={14} />{movie.duration}</span><Link to={movie.live ? '/book/1' : `/movies/${movie.id}`} className="text-link">{movie.live ? 'Book seats' : 'Explore'} <Icon name="arrow" size={16} /></Link></div></article>
}
export function Message({ children, error = false }: { children: ReactNode; error?: boolean }) { return <div className={`message ${error ? 'error' : 'success'}`} role={error ? 'alert' : 'status'}>{children}</div> }
