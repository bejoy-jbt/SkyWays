import { useEffect, useState } from 'react'
import { bookingApi } from '../services/api'
import { Booking } from '../types'
import { format, parseISO } from 'date-fns'
import { Plane, Calendar, Users, CheckCircle, Clock, XCircle } from 'lucide-react'

const statusConfig: Record<string, { label: string; icon: any; cls: string }> = {
  CONFIRMED:          { label: 'Confirmed',   icon: CheckCircle, cls: 'badge-confirmed' },
  PENDING:            { label: 'Pending',     icon: Clock,       cls: 'badge-pending' },
  SEAT_LOCKED:        { label: 'Processing',  icon: Clock,       cls: 'badge-pending' },
  PAYMENT_PROCESSING: { label: 'Processing',  icon: Clock,       cls: 'badge-pending' },
  PAYMENT_FAILED:     { label: 'Failed',      icon: XCircle,     cls: 'badge-failed' },
  CANCELLED:          { label: 'Cancelled',   icon: XCircle,     cls: 'badge-cancelled' },
}

export default function MyBookingsPage() {
  const [bookings, setBookings] = useState<Booking[]>([])
  const [loading, setLoading]  = useState(true)

  useEffect(() => {
    bookingApi.getMy()
      .then(r => setBookings(r.data))
      .finally(() => setLoading(false))
  }, [])

  if (loading) return <div className="text-center py-16 text-gray-400">Loading your bookings...</div>

  return (
    <div>
      <h2 className="text-2xl font-bold text-gray-900 mb-6 flex items-center gap-2">
        <Plane className="w-6 h-6 text-brand-600" /> My Bookings
      </h2>

      {bookings.length === 0 ? (
        <div className="card text-center py-16">
          <Plane className="w-12 h-12 text-gray-300 mx-auto mb-3" />
          <p className="text-gray-500">No bookings yet. Search for flights to get started.</p>
        </div>
      ) : (
        <div className="space-y-4">
          {bookings.map(b => {
            const cfg = statusConfig[b.status] ?? statusConfig['PENDING']
            const Icon = cfg.icon
            return (
              <div key={b.id} className="card">
                <div className="flex flex-wrap items-start justify-between gap-4">
                  <div className="flex items-start gap-4">
                    <div className="w-10 h-10 bg-brand-100 rounded-lg flex items-center justify-center">
                      <Plane className="w-5 h-5 text-brand-600" />
                    </div>
                    <div>
                      <div className="flex items-center gap-2 mb-1">
                        <span className="font-bold text-gray-900">{b.flightNumber}</span>
                        <span className={cfg.cls}>
                          <Icon className="w-3 h-3 mr-1" />{cfg.label}
                        </span>
                      </div>
                      <p className="text-sm text-gray-500">Seat {b.seatNumber}</p>
                      <div className="flex items-center gap-3 mt-2 text-xs text-gray-400">
                        <span className="flex items-center gap-1">
                          <Calendar className="w-3 h-3" />
                          {format(parseISO(b.createdAt), 'MMM d, yyyy HH:mm')}
                        </span>
                        <span className="flex items-center gap-1">
                          <Users className="w-3 h-3" />
                          {b.passengers.length} passenger{b.passengers.length !== 1 ? 's' : ''}
                        </span>
                      </div>
                    </div>
                  </div>
                  <div className="text-right">
                    <p className="text-xl font-bold text-brand-700">${b.totalAmount}</p>
                    <p className="text-xs text-gray-400 font-mono mt-1">
                      Ref: {b.id.substring(0,8).toUpperCase()}
                    </p>
                    {b.status === 'PAYMENT_FAILED' && b.paymentIntentId && (
                      <p className="text-xs text-red-500 mt-1">{b.paymentIntentId}</p>
                    )}
                  </div>
                </div>

                {/* Passenger list */}
                {b.passengers.length > 0 && (
                  <div className="mt-4 pt-4 border-t">
                    <p className="text-xs font-medium text-gray-500 mb-2">PASSENGERS</p>
                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
                      {b.passengers.map((p, i) => (
                        <div key={i} className="text-sm bg-gray-50 rounded-lg px-3 py-2">
                          <span className="font-medium">{p.firstName} {p.lastName}</span>
                          {p.nationality && <span className="text-gray-400 ml-2">· {p.nationality}</span>}
                        </div>
                      ))}
                    </div>
                  </div>
                )}
              </div>
            )
          })}
        </div>
      )}
    </div>
  )
}
