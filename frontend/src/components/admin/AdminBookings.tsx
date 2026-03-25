import { useEffect, useState } from 'react'
import { bookingApi } from '../../services/api'
import { Booking } from '../../types'
import { format, parseISO } from 'date-fns'
import { ChevronDown, ChevronRight, Users, Plane } from 'lucide-react'

const STATUS_CLS: Record<string, string> = {
  CONFIRMED:          'badge-confirmed',
  PENDING:            'badge-pending',
  SEAT_LOCKED:        'badge-pending',
  PAYMENT_PROCESSING: 'badge-pending',
  PAYMENT_FAILED:     'badge-failed',
  CANCELLED:          'badge-cancelled',
}

export default function AdminBookings() {
  const [bookings, setBookings] = useState<Booking[]>([])
  const [loading, setLoading]   = useState(true)
  const [expanded, setExpanded] = useState<string | null>(null)
  const [search, setSearch]     = useState('')
  const [statusF, setStatusF]   = useState('ALL')
  const [viewMode, setViewMode] = useState<'list' | 'byFlight'>('list')

  useEffect(() => {
    bookingApi.getAll()
      .then(r => setBookings(r.data))
      .finally(() => setLoading(false))
  }, [])

  const toggle = (id: string) => setExpanded(e => e === id ? null : id)

  const filtered = bookings.filter(b => {
    const q = search.toLowerCase()
    const matchText = !q ||
      b.userEmail?.toLowerCase().includes(q) ||
      b.flightNumber?.toLowerCase().includes(q) ||
      b.id?.toLowerCase().includes(q) ||
      b.seatNumber?.toLowerCase().includes(q)
    return matchText && (statusF === 'ALL' || b.status === statusF)
  })

  // Group by flight for "by flight" view
  const byFlight = filtered.reduce((acc, b) => {
    const k = b.flightNumber || 'Unknown'
    if (!acc[k]) acc[k] = []
    acc[k].push(b)
    return acc
  }, {} as Record<string, Booking[]>)

  if (loading) return <div className="text-center py-8 text-gray-400">Loading...</div>

  return (
    <div>
      {/* Summary row */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-3 mb-6">
        {[
          { label: 'Total',      val: bookings.length,                                          cls: 'text-gray-900' },
          { label: 'Confirmed',  val: bookings.filter(b => b.status==='CONFIRMED').length,      cls: 'text-green-600' },
          { label: 'Processing', val: bookings.filter(b => ['PENDING','SEAT_LOCKED','PAYMENT_PROCESSING'].includes(b.status)).length, cls: 'text-yellow-600' },
          { label: 'Failed',     val: bookings.filter(b => b.status==='PAYMENT_FAILED').length, cls: 'text-red-600' },
        ].map(s => (
          <div key={s.label} className="card !p-4">
            <p className="text-xs text-gray-500 uppercase tracking-wide">{s.label}</p>
            <p className={`text-2xl font-bold mt-1 ${s.cls}`}>{s.val}</p>
          </div>
        ))}
      </div>

      {/* Toolbar */}
      <div className="flex flex-wrap gap-3 mb-4 items-center">
        <input className="input max-w-xs" placeholder="Search email, flight, seat, ref..."
          value={search} onChange={e => setSearch(e.target.value)} />
        <select className="input w-44" value={statusF} onChange={e => setStatusF(e.target.value)}>
          <option value="ALL">All statuses</option>
          <option value="CONFIRMED">Confirmed</option>
          <option value="PAYMENT_PROCESSING">Processing</option>
          <option value="PAYMENT_FAILED">Failed</option>
          <option value="CANCELLED">Cancelled</option>
        </select>
        <div className="flex rounded-lg border border-gray-200 overflow-hidden">
          <button onClick={() => setViewMode('list')}
            className={`px-3 py-1.5 text-sm ${viewMode==='list' ? 'bg-brand-600 text-white' : 'text-gray-500 hover:bg-gray-50'}`}>
            List
          </button>
          <button onClick={() => setViewMode('byFlight')}
            className={`px-3 py-1.5 text-sm flex items-center gap-1 ${viewMode==='byFlight' ? 'bg-brand-600 text-white' : 'text-gray-500 hover:bg-gray-50'}`}>
            <Plane className="w-3 h-3" /> By Flight
          </button>
        </div>
        <span className="text-sm text-gray-400">{filtered.length} results</span>
      </div>

      {/* ── BY FLIGHT VIEW ── */}
      {viewMode === 'byFlight' && (
        <div className="space-y-4">
          {Object.entries(byFlight).map(([flightNum, bkgs]) => (
            <div key={flightNum} className="border border-gray-200 rounded-xl overflow-hidden">
              <div className="bg-brand-50 border-b border-brand-100 px-4 py-3 flex items-center justify-between cursor-pointer"
                onClick={() => toggle(flightNum)}>
                <div className="flex items-center gap-3">
                  <Plane className="w-4 h-4 text-brand-600" />
                  <span className="font-semibold text-brand-800">{flightNum}</span>
                  <span className="text-sm text-brand-600">{bkgs.length} booking{bkgs.length!==1?'s':''}</span>
                  <span className="text-sm text-green-600">
                    {bkgs.filter(b=>b.status==='CONFIRMED').length} confirmed
                  </span>
                </div>
                {expanded === flightNum
                  ? <ChevronDown className="w-4 h-4 text-brand-600"/>
                  : <ChevronRight className="w-4 h-4 text-brand-600"/>}
              </div>

              {expanded === flightNum && (
                <div className="divide-y">
                  {bkgs.map(b => (
                    <div key={b.id} className="px-4 py-3 bg-white">
                      <div className="flex items-center justify-between flex-wrap gap-2">
                        <div className="flex items-center gap-4 text-sm">
                          <span className="font-mono text-xs text-gray-400">
                            {b.id?.substring(0,8).toUpperCase()}
                          </span>
                          <span className="font-medium">{b.userEmail}</span>
                          <span className="bg-gray-100 text-gray-600 px-2 py-0.5 rounded text-xs font-medium">
                            Seat {b.seatNumber}
                          </span>
                          <span className="flex items-center gap-1 text-gray-500 text-xs">
                            <Users className="w-3 h-3"/>
                            {b.passengers?.length || 1} pax
                          </span>
                        </div>
                        <div className="flex items-center gap-3">
                          <span className="font-semibold text-brand-700">${b.totalAmount}</span>
                          <span className={STATUS_CLS[b.status]||'badge-pending'}>{b.status}</span>
                        </div>
                      </div>
                      {/* Passengers */}
                      {b.passengers && b.passengers.length > 0 && (
                        <div className="mt-2 grid grid-cols-1 sm:grid-cols-2 gap-2">
                          {b.passengers.map((p, i) => (
                            <div key={i} className="bg-gray-50 rounded-lg px-3 py-2 text-xs">
                              <p className="font-semibold text-gray-800">{p.firstName} {p.lastName}</p>
                              <p className="text-gray-500">
                                {p.dateOfBirth && `DOB: ${p.dateOfBirth}`}
                                {p.nationality && ` · ${p.nationality}`}
                                {p.passportNumber && ` · ${p.passportNumber}`}
                              </p>
                              {p.specialRequests && (
                                <p className="text-amber-600 mt-0.5">⚠ {p.specialRequests}</p>
                              )}
                            </div>
                          ))}
                        </div>
                      )}
                    </div>
                  ))}
                </div>
              )}
            </div>
          ))}
          {Object.keys(byFlight).length === 0 && (
            <div className="text-center py-12 text-gray-400">No bookings found</div>
          )}
        </div>
      )}

      {/* ── LIST VIEW ── */}
      {viewMode === 'list' && (
        <div className="space-y-2">
          {filtered.map(b => (
            <div key={b.id} className="border border-gray-200 rounded-xl overflow-hidden">
              <div className="flex items-center gap-3 px-4 py-3 bg-white hover:bg-gray-50 cursor-pointer"
                onClick={() => toggle(b.id)}>
                <div className="text-gray-400">
                  {expanded === b.id
                    ? <ChevronDown className="w-4 h-4"/>
                    : <ChevronRight className="w-4 h-4"/>}
                </div>
                <div className="flex-1 grid grid-cols-2 md:grid-cols-6 gap-2 text-sm">
                  <div>
                    <p className="text-xs text-gray-400">Ref</p>
                    <p className="font-mono font-semibold">{b.id?.substring(0,8).toUpperCase()}</p>
                  </div>
                  <div>
                    <p className="text-xs text-gray-400">User</p>
                    <p className="truncate max-w-[150px]">{b.userEmail}</p>
                  </div>
                  <div>
                    <p className="text-xs text-gray-400">Flight · Seat</p>
                    <p className="font-medium">{b.flightNumber} · {b.seatNumber}</p>
                  </div>
                  <div>
                    <p className="text-xs text-gray-400">Passengers</p>
                    <p className="flex items-center gap-1">
                      <Users className="w-3 h-3 text-gray-400"/>{b.passengers?.length||1}
                    </p>
                  </div>
                  <div>
                    <p className="text-xs text-gray-400">Amount</p>
                    <p className="font-semibold text-brand-700">${b.totalAmount}</p>
                  </div>
                  <div>
                    <span className={STATUS_CLS[b.status]||'badge-pending'}>{b.status}</span>
                  </div>
                </div>
              </div>
              {expanded === b.id && (
                <div className="border-t bg-gray-50 px-4 py-4">
                  <p className="text-xs font-semibold text-gray-500 uppercase tracking-wide mb-2">
                    Passenger Details
                  </p>
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-2">
                    {b.passengers?.map((p, i) => (
                      <div key={i} className="bg-white border border-gray-200 rounded-lg p-3 text-sm">
                        <p className="font-semibold">{p.firstName} {p.lastName}</p>
                        <p className="text-xs text-gray-500 mt-0.5">
                          {p.dateOfBirth && `DOB: ${p.dateOfBirth}`}
                          {p.nationality && ` · ${p.nationality}`}
                          {p.passportNumber && ` · Passport: ${p.passportNumber}`}
                        </p>
                        {p.specialRequests && (
                          <p className="text-xs text-amber-600 mt-1">⚠ {p.specialRequests}</p>
                        )}
                      </div>
                    ))}
                  </div>
                  <p className="text-xs text-gray-400 mt-2">
                    Booked: {b.createdAt ? format(parseISO(b.createdAt),'MMM d, yyyy HH:mm') : '—'}
                    {b.confirmedAt && ` · Confirmed: ${format(parseISO(b.confirmedAt),'MMM d, yyyy HH:mm')}`}
                  </p>
                </div>
              )}
            </div>
          ))}
          {filtered.length === 0 && (
            <div className="text-center py-12 text-gray-400">No bookings found</div>
          )}
        </div>
      )}
    </div>
  )
}