import { useEffect, useState } from 'react'
import { bookingApi, flightApi } from '../services/api'
import { Booking } from '../types'
import { format, parseISO } from 'date-fns'
import { Plane, Calendar, Users, CheckCircle, Clock, XCircle } from 'lucide-react'
import toast from 'react-hot-toast'

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
  const [deletingId, setDeletingId] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [departureByFlightId, setDepartureByFlightId] = useState<Record<number, string>>({})

  useEffect(() => {
    bookingApi.getMy()
      .then(r => setBookings(r.data))
      .finally(() => setLoading(false))
  }, [])

  useEffect(() => {
    const flightIds = Array.from(new Set(bookings.map(b => b.flightId))).filter(Boolean)
    const missing = flightIds.filter(id => !departureByFlightId[id])
    if (missing.length === 0) return

    Promise.all(
      missing.map(id =>
        flightApi.getById(id)
          .then(r => ({ id, departureTime: r.data?.departureTime as string | undefined }))
          .catch(() => ({ id, departureTime: undefined }))
      )
    ).then(results => {
      setDepartureByFlightId(prev => {
        const next = { ...prev }
        results.forEach(({ id, departureTime }) => {
          if (departureTime) next[id] = departureTime
        })
        return next
      })
    })
  }, [bookings])

  const isWithin2HoursOfDeparture = (flightId: number) => {
    const dep = departureByFlightId[flightId]
    if (!dep) return false
    const depMs = new Date(dep).getTime()
    if (Number.isNaN(depMs)) return false
    return depMs - Date.now() <= 2 * 60 * 60 * 1000
  }

  const handleDeleteBooking = async (id: string) => {
    if (!window.confirm('Delete this booking? This action cannot be undone.')) return

    setError(null)
    setDeletingId(id)
    try {
      await bookingApi.delete(id)
      setBookings((prev) => prev.filter((b) => b.id !== id))
      toast.success('Booking deleted successfully')
    } catch (err: any) {
      const message = err?.response?.data?.message || 'Failed to delete booking'
      setError(message)
      toast.error(message)
    } finally {
      setDeletingId(null)
    }
  }

  const handleDeletePassengerTicket = async (bookingId: string, passengerIndex: number) => {
    if (!window.confirm('Cancel ticket for this passenger and process split refund?')) return

    setError(null)
    setDeletingId(`${bookingId}:${passengerIndex}`)
    try {
      await bookingApi.deletePassengerTicket(bookingId, passengerIndex)
      setBookings((prev) => prev.map((b) => {
        if (b.id !== bookingId) return b
        const seats = b.seatNumbers && b.seatNumbers.length > 0 ? [...b.seatNumbers] : [b.seatNumber]
        seats.splice(passengerIndex, 1)
        return {
          ...b,
          passengers: b.passengers.filter((_, i) => i !== passengerIndex),
          seatNumbers: seats,
          seatNumber: seats[0] || b.seatNumber,
        }
      }))
      toast.success('Passenger ticket cancelled. Split refund initiated.')
    } catch (err: any) {
      const message = err?.response?.data?.message || 'Failed to cancel passenger ticket'
      setError(message)
      toast.error(message)
    } finally {
      setDeletingId(null)
    }
  }

  if (loading) return <div className="text-center py-16 text-gray-400">Loading your bookings...</div>

  return (
    <div>
      <h2 className="text-2xl font-bold text-gray-900 mb-6 flex items-center gap-2">
        <Plane className="w-6 h-6 text-brand-600" /> My Bookings
      </h2>

      {error && (
        <div className="mb-4 rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">
          {error}
        </div>
      )}

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
            const blocked = isWithin2HoursOfDeparture(b.flightId)
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
                      <p className="text-sm text-gray-500">
                        Seats {(b.seatNumbers && b.seatNumbers.length > 0 ? b.seatNumbers : [b.seatNumber]).join(', ')}
                      </p>
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
                    <button
                      type="button"
                      onClick={() => handleDeleteBooking(b.id)}
                      disabled={deletingId === b.id || blocked}
                      className="mt-3 inline-flex items-center rounded-md border border-red-300 px-3 py-1.5 text-xs font-medium text-red-600 hover:bg-red-50 disabled:cursor-not-allowed disabled:opacity-50"
                      title={blocked ? 'Not allowed within 2 hours of departure' : undefined}
                    >
                      {deletingId === b.id ? 'Deleting...' : 'Delete Booking'}
                    </button>
                    {blocked && (
                      <p className="mt-1 text-[11px] text-gray-400">
                        Deletion/cancellation is disabled within 2 hours of departure.
                      </p>
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
                          <div className="flex items-center justify-between gap-2">
                            <div>
                              <span className="font-medium">{p.firstName} {p.lastName}</span>
                              {p.nationality && <span className="text-gray-400 ml-2">· {p.nationality}</span>}
                              <p className="text-xs text-gray-500 mt-1">
                                Seat {(b.seatNumbers && b.seatNumbers.length > i ? b.seatNumbers[i] : b.seatNumber)}
                              </p>
                            </div>
                            {b.passengers.length > 1 && (
                              <button
                                type="button"
                                onClick={() => handleDeletePassengerTicket(b.id, i)}
                                disabled={blocked || deletingId === `${b.id}:${i}` || !!deletingId}
                                className="rounded-md border border-amber-300 px-2 py-1 text-xs font-medium text-amber-700 hover:bg-amber-50 disabled:cursor-not-allowed disabled:opacity-50"
                                title={blocked ? 'Not allowed within 2 hours of departure' : undefined}
                              >
                                {deletingId === `${b.id}:${i}` ? 'Cancelling...' : 'Cancel Ticket'}
                              </button>
                            )}
                          </div>
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
