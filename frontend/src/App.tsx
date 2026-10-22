import { useEffect, useState } from 'react'
import { Link } from './router'
import { usePath } from './hooks/usePath'
import { Icon } from './components/ui'
import { Home, Catalog, Details, HowItWorks } from './pages/Movies'
import { Booking } from './pages/Booking'
import { ReservationPage } from './pages/ReservationPage'
import { Account } from './pages/Account'
import { Admin } from './pages/Admin'
import './App.css'
function App() {
  const path = usePath()
  const [theme, setTheme] = useState(() => { try { return localStorage.getItem('deadlock-theme') === 'light' ? 'light' : 'dark' } catch { return 'dark' } })
  const [menu, setMenu] = useState(false)
  useEffect(() => { document.documentElement.dataset.theme = theme; try { localStorage.setItem('deadlock-theme', theme) } catch { /* Theme still works for this visit. */ } }, [theme])
  useEffect(() => { document.title = `${path.startsWith('/book') ? 'Choose your seats' : path === '/approver' ? 'Demo approver' : 'Your next great story'} · Deadlock Cinema` }, [path])
  const nav = [{ to: '/', label: 'Home' }, { to: '/movies', label: 'Movies' }, { to: '/how-it-works', label: 'How it works' }, { to: '/reservations', label: 'My reservations' }]
  return <div className="app-shell"><a className="skip-link" href="#main">Skip to content</a><header className="header"><div className="nav-container"><Link to="/" className="brand"><span className="brand-icon"><Icon name="ticket" size={24} /></span><span>deadlock<span className="brand-sub">CINEMA</span></span></Link><nav className={menu ? 'navigation open' : 'navigation'} aria-label="Main navigation">{nav.map(n => <Link key={n.to} to={n.to} className={path === n.to ? 'active' : ''} aria-current={path === n.to ? 'page' : undefined} onClick={() => setMenu(false)}>{n.label}</Link>)}</nav><div className="nav-actions"><button className="icon-button theme-toggle" onClick={() => setTheme(theme === 'dark' ? 'light' : 'dark')} aria-label={`Switch to ${theme === 'dark' ? 'light' : 'dark'} mode`}><Icon name={theme === 'dark' ? 'sun' : 'moon'} /></button><details className="account-menu" key={path}><summary>Account <span aria-hidden="true">&#8964;</span></summary><div><Link to="/reservations">My reservations</Link><Link to="/admin">Demo administration</Link><Link to="/approver">Demo approver dashboard</Link><Link to="/login">Login preview</Link><Link to="/register">Register preview</Link></div></details><button className="icon-button mobile-menu" aria-label="Toggle navigation" aria-expanded={menu} onClick={() => setMenu(!menu)}><Icon name="menu" /></button></div></div></header><main id="main" key={path}>{path === '/' ? <Home /> : path === '/how-it-works' ? <HowItWorks /> : path === '/movies' ? <Catalog /> : path.startsWith('/movies/') ? <Details id={path.split('/')[2]} /> : path.startsWith('/admin') ? <Admin path={path} /> : /^\/book\/\d+$/.test(path) ? <Booking screeningId={Number(path.split('/')[2])} /> : path === '/reservations' || path === '/approver' ? <ReservationPage approver={path === '/approver'} /> : path === '/login' || path === '/register' ? <Account register={path === '/register'} /> : <div className="container page empty"><h1>This scene is missing.</h1><p>That page or screening is not available.</p><Link className="button" to="/movies">Explore movies</Link></div>}</main><footer className="footer container"><div><Link to="/" className="brand"><Icon name="ticket" /> deadlock <span className="footer-cinema">CINEMA</span></Link><p>Big stories. Shared moments.</p></div><div className="footer-links"><Link to="/movies">Explore movies</Link><Link to="/approver">Demo approver</Link><span>Built by team Deadlock · University project</span></div></footer></div>
}
export default App
