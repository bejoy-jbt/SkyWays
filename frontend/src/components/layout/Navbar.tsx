import { Link, useNavigate } from 'react-router-dom'
import { useAuthStore } from '../../store/authStore'
import { Plane, LogOut, User, BookOpen, Settings } from 'lucide-react'

export default function Navbar() {
  const { user, logout, isAdmin } = useAuthStore()
  const nav = useNavigate()

  const handleLogout = () => {
    logout()
    nav('/login')
  }

  return (
    <nav className="bg-brand-700 text-white shadow-lg">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between h-16">
          <Link to="/" className="flex items-center gap-2 font-bold text-xl hover:text-brand-100">
            <Plane className="w-6 h-6" />
            SkyWays
          </Link>

          {user ? (
            <div className="flex items-center gap-4">
              <Link to="/search" className="hover:text-brand-100 text-sm font-medium">Search Flights</Link>
              <Link to="/my-bookings" className="flex items-center gap-1 hover:text-brand-100 text-sm font-medium">
                <BookOpen className="w-4 h-4" /> My Bookings
              </Link>
              {isAdmin() && (
                <Link to="/admin" className="flex items-center gap-1 hover:text-brand-100 text-sm font-medium">
                  <Settings className="w-4 h-4" /> Admin
                </Link>
              )}
              <div className="flex items-center gap-2 border-l border-brand-500 pl-4">
                <User className="w-4 h-4" />
                <span className="text-sm">{user.firstName}</span>
                <button onClick={handleLogout} className="hover:text-brand-100 ml-1">
                  <LogOut className="w-4 h-4" />
                </button>
              </div>
            </div>
          ) : (
            <div className="flex items-center gap-3">
              <Link to="/login"    className="hover:text-brand-100 text-sm font-medium">Sign In</Link>
              <Link to="/register" className="bg-white text-brand-700 px-4 py-1.5 rounded-lg text-sm font-medium hover:bg-brand-50">
                Sign Up
              </Link>
            </div>
          )}
        </div>
      </div>
    </nav>
  )
}
