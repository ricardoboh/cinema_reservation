import { useEffect, useState } from 'react'
import { Link } from './router'
import { usePath } from './hooks/usePath'
import { Icon } from './components/ui'
import { Home, Catalog, Details } from './pages/Movies'
import { Booking } from './pages/Booking'
import { ReservationPage } from './pages/ReservationPage'
import { Account } from './pages/Account'
import './App.css'
function App() {
  const path = usePath()
  const [theme, setTheme] = useState(() => { try { return localStorage.getItem('deadlock-theme') === 'light' ? 'light' : 'dark' } catch { return 'dark' } })
  const [menu, setMenu] = useState(false)
  useEffect(() => { document.documentElement.dataset.theme = theme; try { localStorage.setItem('deadlock-theme', theme) } catch { /* Theme still works for this visit. */ } }, [theme])
  useEffect(() => { document.title = `${path.startsWith('/book') ? 'Choose your seats' : path === '/approver' ? 'Demo approver' : 'Your next great story'} · Deadlock Cinema` }, [path])
  const nav = [{ to: '/', label: 'Discover' }, { to: '/movies', label: 'Movies' }, { to: '/reservations', label: 'My reservations' }]
  return <><a className="skip-link" href="#main">Skip to content</a><header className="header"><div className="nav-container"><Link to="/" className="brand"><span className="brand-icon"><Icon name="ticket" size={24} /></span><span>deadlock<span className="brand-sub">CINEMA</span></span></Link><nav className={menu ? 'navigation open' : 'navigation'} aria-label="Main navigation">{nav.map(n => <Link key={n.to} to={n.to} className={path === n.to ? 'active' : ''} aria-current={path === n.to ? 'page' : undefined} onClick={() => setMenu(false)}>{n.label}</Link>)}</nav><div className="nav-actions"><button className="icon-button theme-toggle" onClick={() => setTheme(theme === 'dark' ? 'light' : 'dark')} aria-label={`Switch to ${theme === 'dark' ? 'light' : 'dark'} mode`}><Icon name={theme === 'dark' ? 'sun' : 'moon'} /></button><Link to="/login" className="sign-in">Sign in <Icon name="arrow" size={16} /></Link><button className="icon-button mobile-menu" aria-label="Toggle navigation" aria-expanded={menu} onClick={() => setMenu(!menu)}><Icon name="menu" /></button></div></div></header><main id="main" key={path}>{path === '/' ? <Home /> : path === '/movies' ? <Catalog /> : path.startsWith('/movies/') ? <Details id={path.split('/')[2]} /> : path === '/book/1' ? <Booking /> : path === '/reservations' || path === '/approver' ? <ReservationPage approver={path === '/approver'} /> : path === '/login' || path === '/register' ? <Account register={path === '/register'} /> : <div className="container page empty"><h1>This scene is missing.</h1><p>That page or screening is not available.</p><Link className="button" to="/movies">Explore movies</Link></div>}</main><footer className="footer container"><div><Link to="/" className="brand"><Icon name="ticket" /> deadlock <span className="footer-cinema">CINEMA</span></Link><p>Big stories. Shared moments.</p></div><div className="footer-links"><Link to="/movies">Explore movies</Link><Link to="/approver">Demo approver</Link><span>Built by team Deadlock · University project</span></div></footer></>
}
export default App
