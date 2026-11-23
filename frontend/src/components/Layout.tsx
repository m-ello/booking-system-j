import { NavLink, Outlet } from 'react-router'
import { useAuth } from '../auth/AuthContext'

export default function Layout() {
  const { session, logout } = useAuth()
  const isAdmin = session?.role === 'ADMIN'
  return (
    <>
      <header>
        <span className="brand">Table booking</span>
        <div className="who">
          <span className="muted">{session?.email}</span>
          <span className="pill">{session?.role}</span>
          <button className="ghost small" onClick={logout}>Log out</button>
        </div>
      </header>
      <main>
        <nav className="nav">
          <NavLink to="/" end>Find a table</NavLink>
          <NavLink to="/bookings">My bookings</NavLink>
          {isAdmin && <NavLink to="/admin/bookings">All bookings</NavLink>}
          {isAdmin && <NavLink to="/admin/tables">Manage tables</NavLink>}
        </nav>
        <Outlet />
      </main>
    </>
  )
}
