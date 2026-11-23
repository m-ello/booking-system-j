import { Navigate, Outlet, useLocation } from 'react-router'
import { useAuth } from './AuthContext'
import type { Role } from '../api/types'

// Wrap routes with this to require a login, and optionally a specific role.
export default function RequireAuth({ role }: { role?: Role }) {
  const { session } = useAuth()
  const location = useLocation()
  if (!session) return <Navigate to="/login" replace state={{ from: location.pathname }} />
  if (role && session.role !== role) return <Navigate to="/" replace />
  return <Outlet />
}
