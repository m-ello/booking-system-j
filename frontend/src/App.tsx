import { Navigate, Route, Routes } from 'react-router'
import RequireAuth from './auth/RequireAuth'
import Layout from './components/Layout'
import AdminBookingsPage from './pages/AdminBookingsPage'
import AdminTablesPage from './pages/AdminTablesPage'
import FindTablePage from './pages/FindTablePage'
import LoginPage from './pages/LoginPage'
import MyBookingsPage from './pages/MyBookingsPage'

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route element={<RequireAuth />}>
        <Route element={<Layout />}>
          <Route index element={<FindTablePage />} />
          <Route path="bookings" element={<MyBookingsPage />} />
          <Route element={<RequireAuth role="ADMIN" />}>
            <Route path="admin/bookings" element={<AdminBookingsPage />} />
            <Route path="admin/tables" element={<AdminTablesPage />} />
          </Route>
        </Route>
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
