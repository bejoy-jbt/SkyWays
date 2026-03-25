import { useEffect, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { flightApi } from '../services/api'
import { Flight, Seat } from '../types'
import SeatMap from '../components/flight/SeatMap'
import toast from 'react-hot-toast'
import { Plane, Calendar, Clock, DollarSign } from 'lucide-react'
import { format, parseISO } from 'date-fns'

export default function FlightDetailPage() {
  const { id }       = useParams<{ id: string }>()
  const nav          = useNavigate()
  const [flight, setFlight]           = useState<Flight | null>(null)
  const [seats, setSeats]             = useState<Seat[]>([])
  const [selectedSeat, setSelectedSeat] = useState<string | null>(null)
  const [loading, setLoading]         = useState(true)

  useEffect(() => {
    const load = async () => {
      try {
        const [fRes, sRes] = await Promise.all([
          flightApi.getById(Number(id)),
          flightApi.getSeats(Number(id)),
        ])
        setFlight(fRes.data)
        setSeats(sRes.data)
      } catch {
        toast.error('Failed to load flight')
        nav('/search')
      } finally {
        setLoading(false)
      }
    }
    load()
  }, [id])

  if (loading) return <div className="text-center py-16 text-gray-400">Loading...</div>
  if (!flight)  return null

  const dep = parseISO(flight.departureTime)
  const arr = parseISO(flight.arrivalTime)
  const mins = Math.round((arr.getTime() - dep.getTime()) / 60000)

  return (
    <div className="max-w-5xl mx-auto">
      {/* Flight summary card */}
      <div className="card mb-6">
        <div className="flex flex-wrap items-center justify-between gap-4">
          <div>
            <h2 className="text-xl font-bold flex items-center gap-2">
              <Plane className="w-5 h-5 text-brand-600" />{flight.flightNumber}
            </h2>
            <p className="text-gray-500 text-sm mt-1">{flight.aircraftType}</p>
          </div>
          <div className="flex items-center gap-6 text-center">
            <div>
              <p className="text-2xl font-bold">{format(dep,'HH:mm')}</p>
              <p className="text-gray-500 text-sm">{flight.origin}</p>
            </div>
            <div className="text-gray-400 text-sm">
              <Clock className="w-4 h-4 mx-auto mb-1" />
              {Math.floor(mins/60)}h {mins%60}m
            </div>
            <div>
              <p className="text-2xl font-bold">{format(arr,'HH:mm')}</p>
              <p className="text-gray-500 text-sm">{flight.destination}</p>
            </div>
          </div>
          <div className="flex items-center gap-2">
            <Calendar className="w-4 h-4 text-gray-400" />
            <span className="text-gray-600">{format(dep,'EEE, MMM d yyyy')}</span>
          </div>
          <div className="text-right">
            <p className="text-3xl font-bold text-brand-700">${flight.price}</p>
            <p className="text-xs text-gray-400">per person</p>
          </div>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Seat map */}
        <div className="lg:col-span-2 card">
          <h3 className="font-semibold text-gray-900 mb-4">Select Your Seat</h3>
          <SeatMap
            seats={seats}
            totalRows={flight.totalRows}
            seatsPerRow={flight.seatsPerRow}
            onSeatSelect={setSelectedSeat}
            selectedSeat={selectedSeat}
          />
        </div>

        {/* Booking summary */}
        <div className="card h-fit sticky top-24">
          <h3 className="font-semibold text-gray-900 mb-4">Booking Summary</h3>
          {selectedSeat ? (
            <>
              <div className="bg-brand-50 rounded-lg p-3 mb-4">
                <p className="text-sm text-gray-500">Selected Seat</p>
                <p className="text-2xl font-bold text-brand-700">{selectedSeat}</p>
                <p className="text-xs text-gray-400">
                  {parseInt(selectedSeat) <= 3 ? '👑 Business Class' : '💺 Economy Class'}
                </p>
              </div>
              <div className="space-y-2 mb-4 text-sm">
                <div className="flex justify-between">
                  <span className="text-gray-500">Flight</span>
                  <span>{flight.flightNumber}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-gray-500">Route</span>
                  <span>{flight.origin} → {flight.destination}</span>
                </div>
                <div className="flex justify-between font-semibold border-t pt-2">
                  <span>Total</span>
                  <span className="text-brand-700">${flight.price}</span>
                </div>
              </div>
              <button
                onClick={() => nav(`/book/${flight.id}/${selectedSeat}`)}
                className="btn-primary w-full flex items-center justify-center gap-2">
                <DollarSign className="w-4 h-4" /> Continue to Booking
              </button>
            </>
          ) : (
            <div className="text-center py-8 text-gray-400">
              <Plane className="w-10 h-10 mx-auto mb-2 opacity-30" />
              <p className="text-sm">Click a green seat to select it</p>
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
