import { useState } from 'react'
import { useProgramme } from '../cinema'
import { MovieCard, Poster, Message } from '../components/ui'
import { Link } from '../router'
export function Home(){return <><section className="hero container"><div className="hero-copy"><p className="eyebrow">THE DEADLOCK CINEMA PROGRAMME</p><h1>Make room<br/>for a <em>great story.</em></h1><p className="hero-description">The lights dim. The everyday falls away.</p><Link className="button" to="/movies">Explore movies →</Link></div></section><Catalog/></>}
export function Catalog(){const {movies,screenings,error}=useProgramme();const [query,setQuery]=useState('');return <section className="container page"><p className="eyebrow">ON THE PROGRAMME</p><h1>Stories worth seeing.</h1>{error&&<Message error>{error}</Message>}<label className="search catalog-search">Search movies<input type="search" placeholder="Search by title or genre" value={query} onChange={e=>setQuery(e.target.value)}/></label><div className="movie-grid catalog-grid">{movies.filter(m=>(m.title+' '+m.genre).toLowerCase().includes(query.toLowerCase())).map(movie=><MovieCard key={movie.id} movie={movie} screenings={screenings}/>)}</div>{!movies.length&&!error&&<p className="empty-state">No movies on the programme yet.</p>}{movies.length>0&&!movies.some(m=>(m.title+' '+m.genre).toLowerCase().includes(query.toLowerCase()))&&<p className="empty-state">No movies match your search. Try another title or genre.</p>}</section>}
export function Details({id}:{id:string}){const {movies,screenings,error}=useProgramme();const movie=movies.find(m=>m.id===Number(id));return <section className="container page"><Link to="/movies" className="back-link">← Movies</Link>{error&&<Message error>{error}</Message>}{movie?<div className="detail-layout"><Poster movie={movie}/><div><h1>{movie.title}</h1><p>{movie.genre} · {movie.durationMinutes} min · {movie.releaseYear}</p><p>{movie.description}</p><h2>Choose your screening</h2>{screenings.filter(s=>s.movie.id===movie.id).map(s=><div className="panel detail-screening" key={s.id}><p>{new Date(s.startsAt).toLocaleString('en-GB', {day:'numeric',month:'short',year:'numeric',hour:'2-digit',minute:'2-digit'})} · {s.hall.name}</p><Link className="button" to={'/book/'+s.id}>Choose seats</Link></div>)}{!screenings.some(s=>s.movie.id===movie.id)&&<p>No available screenings.</p>}</div></div>:<p>Loading movie, or movie unavailable.</p>}</section>}
export function HowItWorks() {
  const steps = [
    ['01', 'Choose a movie', 'Browse movies and select an available screening.'],
    ['02', 'Select your seats', 'Choose available seats from the actual cinema hall layout.'],
    ['03', 'Confirm your reservation', 'Confirm standard bookings or request approval for VIP seats.'],
  ]
  return <section className="container page how-it-works">
    <p className="eyebrow">YOUR VISIT, STEP BY STEP</p>
    <h1>Choose. Reserve. Enjoy.</h1>
    <p className="lead">A great story starts with a seat. Here is how to reserve yours.</p>
    <div className="guide-steps">{steps.map(([number, title, description]) => <article key={number}>
      <span className="eyebrow">{number}</span><h2>{title}</h2><p>{description}</p>
    </article>)}</div>
    <aside className="guide-note" aria-label="Reservation essentials">
      <h2>Before the lights dim</h2>
      <p>Draft reservations do not hold seats. VIP reservations require approval, and pending approvals expire automatically.</p>
      <p>Manage your bookings under <Link className="text-link" to="/reservations">My reservations</Link>.</p>
    </aside>
    <Link className="button" to="/movies">Browse movies</Link>
  </section>
}
