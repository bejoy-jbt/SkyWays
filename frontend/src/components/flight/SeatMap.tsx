import { useRef } from 'react'
import { Seat } from '../../types'

interface Props {
  seats: Seat[]
  totalRows: number
  seatsPerRow: number
  onSeatToggle: (seat: string) => void
  selectedSeats: string[]
}

export default function SeatMap({ seats, totalRows, seatsPerRow, onSeatToggle, selectedSeats }: Props) {
  const containerRef = useRef<HTMLDivElement>(null)

  const cols = ['A','B','C','D','E','F'].slice(0, seatsPerRow)

  const getSeatStatus = (seatNumber: string): Seat['status'] => {
    return seats.find(s => s.seatNumber === seatNumber)?.status ?? 'AVAILABLE'
  }

  const seatClass = (status: Seat['status'], seatNum: string) => {
    const base = 'w-9 h-9 rounded-t-xl border-2 text-xs font-medium transition-all cursor-pointer flex items-center justify-center'
    if (selectedSeats.includes(seatNum))
      return `${base} bg-brand-600 border-brand-700 text-white scale-110 shadow-md`
    if (status === 'AVAILABLE')
      return `${base} bg-green-100 border-green-400 text-green-700 hover:bg-green-300 hover:scale-105`
    if (status === 'LOCKED')
      return `${base} bg-yellow-100 border-yellow-400 text-yellow-700 cursor-not-allowed`
    return `${base} bg-red-100 border-red-300 text-red-400 cursor-not-allowed opacity-60`
  }

  return (
    <div className="overflow-auto">
      {/* Cabin legend */}
      <div className="flex items-center gap-4 mb-4 text-xs">
        {[
          { color: 'bg-green-200 border-green-400', label: 'Available' },
          { color: 'bg-brand-600 border-brand-700', label: 'Selected' },
          { color: 'bg-yellow-100 border-yellow-400', label: 'Locked' },
          { color: 'bg-red-100 border-red-300', label: 'Booked' },
        ].map(l => (
          <div key={l.label} className="flex items-center gap-1.5">
            <div className={`w-4 h-4 rounded border-2 ${l.color}`} />
            <span className="text-gray-500">{l.label}</span>
          </div>
        ))}
      </div>

      {/* Aircraft nose */}
      <div className="flex justify-center mb-2">
        <div className="w-24 h-8 bg-gray-200 rounded-t-full flex items-center justify-center text-xs text-gray-500 font-medium">
          ✈ FRONT
        </div>
      </div>

      {/* Seat grid */}
      <div className="inline-block bg-gray-50 rounded-xl border border-gray-200 p-4">
        {/* Column headers */}
        <div className="flex items-center gap-1 mb-2 pl-10">
          {cols.map((c, i) => (
            <div key={c}>
              <div className="w-9 text-center text-xs font-semibold text-gray-400">{c}</div>
              {i === Math.floor(seatsPerRow/2) - 1 && <div className="w-6" />}
            </div>
          ))}
        </div>

        {/* Rows */}
        {Array.from({ length: totalRows }, (_, ri) => {
          const rowNum = ri + 1
          return (
            <div key={rowNum} className="flex items-center gap-1 mb-1">
              <span className="w-8 text-right text-xs text-gray-400 pr-2">{rowNum}</span>
              {cols.map((col, ci) => {
                const seatNum = `${rowNum}${col}`
                const status  = getSeatStatus(seatNum)
                const disabled = status !== 'AVAILABLE'
                return (
                  <div key={col} className="flex items-center">
                    <button
                      className={seatClass(status, seatNum)}
                      disabled={disabled}
                      onClick={() => !disabled && onSeatToggle(seatNum)}
                      title={`${seatNum} - ${status}`}
                    >
                      {seatNum}
                    </button>
                    {/* Aisle gap in the middle */}
                    {ci === Math.floor(seatsPerRow/2) - 1 && (
                      <div className="w-6 text-center text-xs text-gray-300">│</div>
                    )}
                  </div>
                )
              })}
            </div>
          )
        })}
      </div>

      <div className="mt-3 text-xs text-gray-500">All seats are Economy Class.</div>
    </div>
  )
}
