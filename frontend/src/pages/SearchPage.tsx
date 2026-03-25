import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { flightApi } from '../services/api'
import { Flight } from '../types'
import toast from 'react-hot-toast'
import { Search, Plane, ArrowRight, Clock } from 'lucide-react'
import { format, parseISO } from 'date-fns'

export default function SearchPage() {
  const [form, setForm]       = useState({ origin: '', destination: '', departureDate: '', passengers: 1 })
  const [flights, setFlights] = useState<Flight[]>([])
  const [loading, setLoading] = useState(false)
  const [searched, setSearched] = useState(false)
  const nav = useNavigate()

  const set = (k: string) => (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) =>
    setForm(f => ({ ...f, [k]: e.target.value }))

  const handleSearch = async (e: React.FormEvent) => {
    e.preventDefault()
    setLoading(true)
    setSearched(true)
    try {
      const { data } = await flightApi.search(form)
      setFlights(data)
      if (data.length === 0) toast('No flights found for this route and date.', { icon: '✈️' })
    } catch {
      toast.error('Search failed. Please try again.')
    } finally {
      setLoading(false)
    }
  }

  const duration = (dep: string, arr: string) => {
    const d = new Date(dep), a = new Date(arr)
    const mins = Math.round((a.getTime() - d.getTime()) / 60000)
    return `${Math.floor(mins/60)}h ${mins % 60}m`
  }

  return (
    <div>
      <div className="card mb-6">
        <h2 className="text-xl font-semibold text-gray-900 mb-5 flex items-center gap-2">
          <Search className="w-5 h-5 text-brand-600" /> Find Your Flight
        </h2>
        <form onSubmit={handleSearch} className="grid grid-cols-1 md:grid-cols-5 gap-4">
          <div>
            <label className="label">From</label>
            <input className="input" placeholder="New York" value={form.origin} onChange={set('origin')} required />
          </div>
          <div>
            <label className="label">To</label>
            <input className="input" placeholder="Los Angeles" value={form.destination} onChange={set('destination')} required />
          </div>
          <div>
            <label className="label">Date</label>
            <input className="input" type="date" value={form.departureDate} onChange={set('departureDate')}
              min={new Date().toISOString().split('T')[0]} required />
          </div>
          <div>
            <label className="label">Passengers</label>
            <select className="input" value={form.passengers} onChange={set('passengers')}>
              {[1,2,3,4,5,6].map(n => <option key={n} value={n}>{n} {n===1?'Passenger':'Passengers'}</option>)}
            </select>
          </div>
          <div className="flex items-end">
            <button type="submit" disabled={loading} className="btn-primary w-full">
              {loading ? 'Searching...' : 'Search'}
            </button>
          </div>
        </form>
      </div>

      {searched && (
        <div className="space-y-4">
          <p className="text-gray-500 text-sm">{flights.length} flight{flights.length !== 1 ? 's' : ''} found</p>
          {flights.map(f => (
            <div key={f.id} className="card hover:shadow-md transition-shadow">
              <div className="flex items-center justify-between flex-wrap gap-4">
                <div className="flex items-center gap-8">
                  <div className="text-center">
                    <p className="text-2xl font-bold">{format(parseISO(f.departureTime), 'HH:mm')}</p>
                    <p className="text-gray-500 text-sm">{f.origin}</p>
                  </div>
                  <div className="flex flex-col items-center">
                    <p className="text-xs text-gray-400 flex items-center gap-1">
                      <Clock className="w-3 h-3" />{duration(f.departureTime, f.arrivalTime)}
                    </p>
                    <div className="flex items-center gap-1 my-1">
                      <div className="w-2 h-2 rounded-full bg-brand-600" />
                      <div className="w-16 h-px bg-brand-300" />
                      <Plane className="w-4 h-4 text-brand-600" />
                      <div className="w-16 h-px bg-brand-300" />
                      <div className="w-2 h-2 rounded-full bg-brand-600" />
                    </div>
                    <p className="text-xs text-gray-400">Direct</p>
                  </div>
                  <div className="text-center">
                    <p className="text-2xl font-bold">{format(parseISO(f.arrivalTime), 'HH:mm')}</p>
                    <p className="text-gray-500 text-sm">{f.destination}</p>
                  </div>
                </div>
                <div className="flex items-center gap-6">
                  <div className="text-center">
                    <p className="text-xs text-gray-400">{f.aircraftType}</p>
                    <p className="text-sm text-green-600 font-medium">{f.availableSeats} seats left</p>
                  </div>
                  <div className="text-right">
                    <p className="text-3xl font-bold text-brand-700">${f.price}</p>
                    <p className="text-xs text-gray-400">per person</p>
                  </div>
                  <button onClick={() => nav(`/flights/${f.id}`)}
                    className="btn-primary flex items-center gap-2">
                    Select <ArrowRight className="w-4 h-4" />
                  </button>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
