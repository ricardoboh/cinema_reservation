import { useState } from 'react'
import { Link } from './router'
import { usePath } from './hooks/usePath'
import { Catalog, Details } from './pages/Movies'
import { Booking } from './pages/Booking'
import { ReservationPage } from './pages/ReservationPage'
import { Admin } from './pages/Admin'
import './App.css'

function App() {
  const path = usePath()
  const [lastReservationId, setLastReservationId] = useState<number>()
  const adminRoutes = ['/admin', '/admin/movies', '/admin/halls', '/admin/screenings']
  return <div className="app-shell"><header><Link to="/movies" className="brand">Cinema Reservation</Link>
    <nav aria-label="Main navigation"><Link to="/movies">Programme</Link><Link to="/reservations">Reservation status</Link>
      <Link to="/approver">Approver</Link><Link to="/admin">Cinema management</Link></nav></header>
    <main key={path}>{path === '/' || path === '/movies' ? <Catalog />
      : /^\/movies\/\d+$/.test(path) ? <Details id={path.split('/')[2]} />
      : /^\/book\/\d+$/.test(path) ? <Booking screeningId={Number(path.split('/')[2])} onCreated={setLastReservationId} />
      : path === '/reservations' || path === '/approver' ? <ReservationPage approver={path === '/approver'} initialId={lastReservationId} />
      : adminRoutes.includes(path) ? <Admin path={path} />
      : <section><h1>Page not found</h1><Link to="/movies">Cinema programme</Link></section>}</main>
    <footer>Team Deadlock · C08 demonstration</footer>
  </div>
}
export default App
