import { useProgramme } from '../cinema'
import { Message } from '../components/ui'
import { Link } from '../router'

export function Catalog() {
  const { movies, screenings, error } = useProgramme()
  return <section><h1>Cinema programme</h1>{error && <Message error>{error}</Message>}
    {movies.map(movie => <article className="panel" key={movie.id}>
      <h2><Link to={`/movies/${movie.id}`}>{movie.title}</Link></h2>
      {screenings.filter(screening => screening.movie.id === movie.id).map(screening => <p key={screening.id}>
        {new Date(screening.startsAt).toLocaleString()} · {screening.hall.name} · <Link to={`/book/${screening.id}`}>Choose seats</Link>
      </p>)}
    </article>)}
    {!movies.length && !error && <p>No movies are available.</p>}
  </section>
}
export function Details({ id }: { id: string }) {
  const { movies, screenings, error } = useProgramme()
  const movie = movies.find(movie => movie.id === Number(id))
  return <section><Link to="/movies">Back to programme</Link>
    {error && <Message error>{error}</Message>}
    {movie ? <><h1>{movie.title}</h1>{screenings.filter(screening => screening.movie.id === movie.id).map(screening =>
      <p className="panel" key={screening.id}>{new Date(screening.startsAt).toLocaleString()} · {screening.hall.name} · <Link to={`/book/${screening.id}`}>Choose seats</Link></p>)}</>
      : <p>Movie unavailable or loading.</p>}
  </section>
}
