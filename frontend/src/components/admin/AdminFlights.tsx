import { useEffect, useState } from 'react'
import { flightApi } from '../../services/api'
import { Flight } from '../../types'
import toast from 'react-hot-toast'
import { Plus, Pencil, Trash2 } from 'lucide-react'

export default function AdminFlights() {
  const [flights, setFlights] = useState<Flight[]>([])
  const [loading, setLoading] = useState(true)
  const [showForm, setShowForm] = useState(false)
  const [editing, setEditing]   = useState<Flight | null>(null)

  const load = () => flightApi.getAll().then(r => setFlights(r.data)).finally(() => setLoading(false))
  useEffect(() => { load() }, [])

  const handleDelete = async (id: number) => {
    if (!confirm('Delete this flight?')) return
    await flightApi.delete(id)
    toast.success('Flight deleted')
    load()
  }

  return (
    <div>
      <div className="flex items-center justify-between mb-4">
        <h3 className="font-semibold text-gray-900">All Flights</h3>
        <button onClick={() => { setEditing(null); setShowForm(true) }}
          className="btn-primary flex items-center gap-2">
          <Plus className="w-4 h-4" /> Add Flight
        </button>
      </div>

      {showForm && (
        <FlightForm flight={editing} onClose={() => setShowForm(false)} onSave={() => { setShowForm(false); load() }} />
      )}

      <div className="overflow-x-auto">
        <table className="w-full text-sm">
          <thead>
            <tr className="border-b bg-gray-50">
              {['#','Flight','Route','Date','Dep','Arr','Price','Seats','Status',''].map(h => (
                <th key={h} className="text-left py-3 px-3 text-xs font-semibold text-gray-500 uppercase">{h}</th>
              ))}
            </tr>
          </thead>
          <tbody>
            {flights.map(f => (
              <tr key={f.id} className="border-b hover:bg-gray-50">
                <td className="py-3 px-3 text-gray-400">{f.id}</td>
                <td className="py-3 px-3 font-medium">{f.flightNumber}</td>
                <td className="py-3 px-3">{f.origin} → {f.destination}</td>
                <td className="py-3 px-3">{f.departureDate}</td>
                <td className="py-3 px-3">{f.departureTime?.substring(11,16)}</td>
                <td className="py-3 px-3">{f.arrivalTime?.substring(11,16)}</td>
                <td className="py-3 px-3 font-medium text-brand-700">${f.price}</td>
                <td className="py-3 px-3 text-green-600">{f.availableSeats}</td>
                <td className="py-3 px-3">
                  <span className={`text-xs px-2 py-0.5 rounded-full font-medium
                    ${f.status==='SCHEDULED'?'bg-blue-100 text-blue-700':
                      f.status==='CANCELLED'?'bg-red-100 text-red-700':'bg-green-100 text-green-700'}`}>
                    {f.status}
                  </span>
                </td>
                <td className="py-3 px-3">
                  <div className="flex gap-2">
                    <button onClick={() => { setEditing(f); setShowForm(true) }}
                      className="text-gray-400 hover:text-brand-600"><Pencil className="w-4 h-4" /></button>
                    <button onClick={() => handleDelete(f.id)}
                      className="text-gray-400 hover:text-red-500"><Trash2 className="w-4 h-4" /></button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  )
}

function FlightForm({ flight, onClose, onSave }: { flight: Flight|null; onClose: ()=>void; onSave: ()=>void }) {
  const initial: Partial<Flight> = flight
    ? {
        ...flight,
        // UI uses time-only inputs; backend stores ISO datetime strings
        departureTime: flight.departureTime ? flight.departureTime.substring(11, 16) : '',
        arrivalTime:   flight.arrivalTime ? flight.arrivalTime.substring(11, 16) : '',
      }
    : {
        flightNumber:'', origin:'', destination:'', departureDate:'',
        departureTime:'', arrivalTime:'', price:0, aircraftType:'Boeing 737', totalRows:28, seatsPerRow:6, status:'SCHEDULED'
      }

  const [form, setForm] = useState<Partial<Flight>>(initial)
  const set = (k: string) => (e: React.ChangeEvent<HTMLInputElement|HTMLSelectElement>) =>
    setForm(f => ({...f, [k]: e.target.value}))

  const toLocalDateTime = (date: string | undefined, timeOrDateTime: string | undefined) => {
    if (!timeOrDateTime) return ''

    // If the control still provides a full datetime-local value (YYYY-MM-DDTHH:mm[...]),
    // normalize it to include seconds.
    if (timeOrDateTime.includes('T')) {
      const v = timeOrDateTime.trim()
      if (/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}$/.test(v)) return `${v}:00`
      if (/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}$/.test(v)) return v
      return v
    }

    if (!date) return ''
    const t = timeOrDateTime.trim()
    // HH:mm -> YYYY-MM-DDTHH:mm:00
    return `${date}T${t}:00`
  }

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault()
    const payload: any = {
      ...form,
      price: Number(form.price ?? 0),
      totalRows: Number(form.totalRows ?? 0),
      seatsPerRow: Number(form.seatsPerRow ?? 0),
      departureTime: toLocalDateTime(form.departureDate as any, form.departureTime as any),
      arrivalTime: toLocalDateTime(form.departureDate as any, form.arrivalTime as any),
    }
    if (flight?.id) {
      await flightApi.update(flight.id, payload)
      toast.success('Flight updated')
    } else {
      await flightApi.create(payload)
      toast.success('Flight created')
    }
    onSave()
  }

  return (
    <div className="card mb-4 border-brand-200">
      <h4 className="font-semibold mb-4">{flight ? 'Edit Flight' : 'New Flight'}</h4>
      <form onSubmit={handleSave} className="grid grid-cols-2 md:grid-cols-4 gap-3">
        <div><label className="label">Flight No</label><input className="input" value={form.flightNumber??''} onChange={set('flightNumber')} required /></div>
        <div><label className="label">Origin</label><input className="input" value={form.origin??''} onChange={set('origin')} required /></div>
        <div><label className="label">Destination</label><input className="input" value={form.destination??''} onChange={set('destination')} required /></div>
        <div><label className="label">Date</label><input className="input" type="date" value={form.departureDate??''} onChange={set('departureDate')} required /></div>
        <div><label className="label">Departure</label><input className="input" type="time" value={form.departureTime??''} onChange={set('departureTime')} required /></div>
        <div><label className="label">Arrival</label><input className="input" type="time" value={form.arrivalTime??''} onChange={set('arrivalTime')} required /></div>
        <div><label className="label">Price</label><input className="input" type="number" value={form.price??''} onChange={set('price')} required /></div>
        <div><label className="label">Aircraft</label><input className="input" value={form.aircraftType??''} onChange={set('aircraftType')} /></div>
        <div><label className="label">Rows</label><input className="input" type="number" value={form.totalRows??28} onChange={set('totalRows')} /></div>
        <div><label className="label">Seats/Row</label><input className="input" type="number" value={form.seatsPerRow??6} onChange={set('seatsPerRow')} /></div>
        {flight && <div><label className="label">Status</label>
          <select className="input" value={form.status??'SCHEDULED'} onChange={set('status')}>
            {['SCHEDULED','BOARDING','DEPARTED','ARRIVED','CANCELLED'].map(s=>(<option key={s}>{s}</option>))}
          </select></div>}
        <div className="col-span-2 flex gap-2 items-end">
          <button type="submit" className="btn-primary">Save</button>
          <button type="button" onClick={onClose} className="btn-secondary">Cancel</button>
        </div>
      </form>
    </div>
  )
}
