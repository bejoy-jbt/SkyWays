import { Routes, Route, Navigate } from 'react-router-dom'
import { useAuthStore } from './store/authStore'
import Layout from './components/layout/Layout'
import LoginPage from './pages/LoginPage'
import RegisterPage from './pages/RegisterPage'
import HomePage from './pages/HomePage'
import SearchPage from './pages/SearchPage'
import FlightDetailPage from './pages/FlightDetailPage'
import BookingPage from './pages/BookingPage'
import MyBookingsPage from './pages/MyBookingsPage'
import AdminDashboard from './pages/AdminDashboard'

function PrivateRoute({ children }: { children: JSX.Element }) {
  const user = useAuthStore(s => s.user)
  return user ? children : <Navigate to="/login" replace />
}

function AdminRoute({ children }: { children: JSX.Element }) {
  const { user, isAdmin } = useAuthStore()
  if (!user) return <Navigate to="/login" replace />
  if (!isAdmin()) return <Navigate to="/" replace />
  return children
}

export default function App() {
  return (
    <Routes>
      <Route path="/login"    element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />
      <Route path="/" element={<Layout />}>
        <Route index element={<HomePage />} />
        <Route path="search"  element={<PrivateRoute><SearchPage /></PrivateRoute>} />
        <Route path="flights/:id" element={<PrivateRoute><FlightDetailPage /></PrivateRoute>} />
        <Route path="book/:flightId" element={<PrivateRoute><BookingPage /></PrivateRoute>} />
        <Route path="my-bookings" element={<PrivateRoute><MyBookingsPage /></PrivateRoute>} />
        <Route path="admin/*" element={<AdminRoute><AdminDashboard /></AdminRoute>} />
      </Route>
    </Routes>
  )
}
