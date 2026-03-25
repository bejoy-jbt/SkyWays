import { useState } from 'react'
import { Routes, Route, useNavigate, useLocation } from 'react-router-dom'
import AdminFlights from '../components/admin/AdminFlights'
import AdminBookings from '../components/admin/AdminBookings'
import AdminUsers from '../components/admin/AdminUsers'
import AdminPayments from '../components/admin/AdminPayments'
import { Plane, BookOpen, Users, CreditCard } from 'lucide-react'

const tabs = [
  { path: '/admin',          label: 'Flights',  icon: Plane },
  { path: '/admin/bookings', label: 'Bookings', icon: BookOpen },
  { path: '/admin/users',    label: 'Users',    icon: Users },
  { path: '/admin/payments', label: 'Payments', icon: CreditCard },
]

export default function AdminDashboard() {
  const nav      = useNavigate()
  const location = useLocation()

  return (
    <div>
      <h2 className="text-2xl font-bold text-gray-900 mb-6">Admin Dashboard</h2>
      <div className="flex gap-1 bg-gray-100 rounded-xl p-1 mb-6 w-fit">
        {tabs.map(t => {
          const active = location.pathname === t.path ||
            (t.path !== '/admin' && location.pathname.startsWith(t.path))
          return (
            <button key={t.path} onClick={() => nav(t.path)}
              className={`flex items-center gap-2 px-4 py-2 rounded-lg text-sm font-medium transition-colors
                ${active ? 'bg-white text-brand-700 shadow-sm' : 'text-gray-500 hover:text-gray-700'}`}>
              <t.icon className="w-4 h-4" />{t.label}
            </button>
          )
        })}
      </div>

      <Routes>
        <Route index         element={<AdminFlights />} />
        <Route path="bookings" element={<AdminBookings />} />
        <Route path="users"    element={<AdminUsers />} />
        <Route path="payments" element={<AdminPayments />} />
      </Routes>
    </div>
  )
}
