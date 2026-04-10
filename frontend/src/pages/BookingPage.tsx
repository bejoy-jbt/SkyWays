import { useEffect, useState } from 'react'
import { useParams, useNavigate, useSearchParams } from 'react-router-dom'
import { flightApi, bookingApi } from '../services/api'
import api from '../services/api'
import { Flight, Passenger } from '../types'
import PassengerForm from '../components/booking/PassengerForm'
import toast from 'react-hot-toast'
import { Plane, Users, CreditCard, CheckCircle, Lock, ShieldCheck } from 'lucide-react'
import { format, parseISO } from 'date-fns'

const empty = (): Passenger => ({
  firstName: '', lastName: '', dateOfBirth: '',
  passportNumber: '', nationality: '', specialRequests: ''
})

type CardState = {
  holderName: string
  number: string
  expiry: string
  cvc: string
}

type CardError = Partial<CardState>

export default function BookingPage() {
  const { flightId } = useParams<{ flightId: string }>()
  const [searchParams] = useSearchParams()
  const nav = useNavigate()
  const seatNumbers = (searchParams.get('seats') || '')
    .split(',')
    .map(s => s.trim())
    .filter(Boolean)

  const [flight, setFlight]         = useState<Flight | null>(null)
  const [step, setStep]             = useState<'passengers' | 'payment' | 'done'>('passengers')
  const [passengers, setPassengers] = useState<Passenger[]>(
    Array.from({ length: Math.max(1, seatNumbers.length) }, () => empty())
  )
  const [booking, setBooking]       = useState<any>(null)
  const [loading, setLoading]       = useState(false)

  const [card, setCard] = useState<CardState>({
    holderName: '', number: '', expiry: '', cvc: ''
  })
  const [cardErrors, setCardErrors] = useState<CardError>({})
  const [cardType, setCardType]     = useState('')
  const [validating, setValidating] = useState(false)

  useEffect(() => {
    flightApi.getById(Number(flightId)).then(r => setFlight(r.data))
  }, [flightId])

  useEffect(() => {
    if (seatNumbers.length === 0) {
      toast.error('Please select seat(s) first')
      nav(`/flights/${flightId}`)
    }
  }, [seatNumbers.length, nav, flightId])

  const updatePassenger = (i: number, p: Passenger) => {
    const arr = [...passengers]; arr[i] = p; setPassengers(arr)
  }

  // ── Format card number with spaces every 4 digits
  const formatCardNumber = (raw: string) => {
    const digits = raw.replace(/\D/g, '').substring(0, 16)
    return digits.replace(/(.{4})/g, '$1 ').trim()
  }

  // ── Format expiry MM/YY
  const formatExpiry = (raw: string) => {
    const digits = raw.replace(/\D/g, '').substring(0, 4)
    if (digits.length >= 3) return digits.substring(0, 2) + '/' + digits.substring(2)
    return digits
  }

  // ── Detect card type from number prefix
  const detectType = (num: string) => {
    const n = num.replace(/\s/g, '')
    if (/^4/.test(n))           return 'Visa'
    if (/^5[1-5]/.test(n))      return 'Mastercard'
    if (/^2[2-7]/.test(n))      return 'Mastercard'
    if (/^3[47]/.test(n))       return 'Amex'
    if (/^6(?:011|5)/.test(n))  return 'Discover'
    return ''
  }

  const handleCardChange = (field: keyof CardState, value: string) => {
    let formatted = value
    if (field === 'number') { formatted = formatCardNumber(value); setCardType(detectType(formatted)) }
    if (field === 'expiry') formatted = formatExpiry(value)
    if (field === 'cvc')    formatted = value.replace(/\D/g, '').substring(0, 4)
    setCard(c => ({ ...c, [field]: formatted }))
    setCardErrors(e => ({ ...e, [field]: '' }))
  }

  // ── Validate card via backend then submit booking
  const handlePayment = async (e: React.FormEvent) => {
    e.preventDefault()
    setValidating(true)

    try {
      // Step 1: validate card details
      const validRes = await api.post('/payments/validate-card', {
        holderName: card.holderName,
        number:     card.number.replace(/\s/g, ''),
        expiry:     card.expiry,
        cvc:        card.cvc,
      })

      if (!validRes.data.valid) {
        toast.error(validRes.data.message)
        setValidating(false)
        return
      }

      toast.success(`${validRes.data.cardType} card verified!`)
      setCardType(validRes.data.cardType)

    } catch {
      toast.error('Card validation failed. Please check your details.')
      setValidating(false)
      return
    }

    setValidating(false)
    setLoading(true)

    // Step 2: create booking (triggers saga → payment processed automatically)
    try {
      const { data } = await bookingApi.create({
        flightId:   Number(flightId),
        seatNumber: seatNumbers[0],
        seatNumbers,
        passengers,
      })
      setBooking(data)
      setStep('done')
      toast.success('Booking confirmed!')
    } catch (err: any) {
      toast.error(err.response?.data?.message || 'Booking failed. Please try again.')
    } finally {
      setLoading(false)
    }
  }

  if (!flight) return <div className="text-center py-16 text-gray-400">Loading...</div>

  const total = (flight.price * passengers.length).toFixed(2)

  return (
    <div className="max-w-3xl mx-auto">
      {/* Progress */}
      <div className="flex items-center mb-8">
        {(['passengers', 'payment', 'done'] as const).map((s, i) => {
          const idx = ['passengers','payment','done'].indexOf(step)
          return (
            <div key={s} className="flex items-center flex-1">
              <div className={`w-8 h-8 rounded-full flex items-center justify-center text-sm font-semibold
                ${step === s ? 'bg-brand-600 text-white' :
                  idx > i    ? 'bg-green-500 text-white' : 'bg-gray-200 text-gray-500'}`}>
                {idx > i ? '✓' : i + 1}
              </div>
              <span className={`ml-2 text-sm font-medium capitalize
                ${step === s ? 'text-brand-600' : 'text-gray-400'}`}>
                {s === 'done' ? 'Confirmed' : s}
              </span>
              {i < 2 && <div className="flex-1 h-px bg-gray-200 mx-3" />}
            </div>
          )
        })}
      </div>

      {/* Flight strip */}
      <div className="bg-brand-50 border border-brand-200 rounded-xl p-4 mb-6 flex items-center justify-between">
        <div className="flex items-center gap-3">
          <Plane className="w-5 h-5 text-brand-600" />
          <div>
            <p className="font-semibold">{flight.flightNumber}: {flight.origin} → {flight.destination}</p>
            <p className="text-sm text-gray-500">
              {format(parseISO(flight.departureTime), 'EEE, MMM d · HH:mm')} · Seats {seatNumbers.join(', ')}
            </p>
          </div>
        </div>
        <p className="text-xl font-bold text-brand-700">${total}</p>
      </div>

      {/* Step 1 — Passengers */}
      {step === 'passengers' && (
        <form onSubmit={e => { e.preventDefault(); setStep('payment') }}>
          <div className="card mb-4">
            <h3 className="font-semibold text-gray-900 mb-1 flex items-center gap-2">
              <Users className="w-5 h-5 text-brand-600" /> Passenger Details
            </h3>
            <p className="text-sm text-gray-500 mb-5">Enter details for all passengers.</p>
            <div className="space-y-4">
              {passengers.map((p, i) => (
                <PassengerForm key={i} index={i} passenger={p} onChange={updatePassenger} />
              ))}
            </div>
            <div className="flex gap-3 mt-4">
              <p className="text-xs text-gray-500">
                Passenger count is fixed to selected seats ({seatNumbers.length}).
              </p>
            </div>
          </div>
          <button type="submit" className="btn-primary w-full py-3">Continue to Payment →</button>
        </form>
      )}

      {/* Step 2 — Payment */}
      {step === 'payment' && (
        <form onSubmit={handlePayment}>
          <div className="card mb-4">
            <h3 className="font-semibold text-gray-900 mb-4 flex items-center gap-2">
              <CreditCard className="w-5 h-5 text-brand-600" /> Payment Details
            </h3>

            <div className="space-y-4">
              {/* Cardholder name */}
              <div>
                <label className="label">Cardholder Name</label>
                <input className="input" placeholder="John Smith"
                  value={card.holderName}
                  onChange={e => handleCardChange('holderName', e.target.value)}
                  required />
              </div>

              {/* Card number */}
              <div>
                <label className="label">Card Number</label>
                <div className="relative">
                  <input className="input pr-20" placeholder="1234 5678 9012 3456"
                    value={card.number}
                    onChange={e => handleCardChange('number', e.target.value)}
                    maxLength={19} required />
                  {cardType && (
                    <span className="absolute right-3 top-1/2 -translate-y-1/2
                      text-xs font-semibold text-brand-600 bg-brand-50 px-2 py-0.5 rounded">
                      {cardType}
                    </span>
                  )}
                </div>
              </div>

              {/* Expiry + CVC */}
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="label">Expiry Date</label>
                  <input className="input" placeholder="MM/YY"
                    value={card.expiry}
                    onChange={e => handleCardChange('expiry', e.target.value)}
                    maxLength={5} required />
                </div>
                <div>
                  <label className="label">CVC</label>
                  <div className="relative">
                    <input className="input pr-8" placeholder="123"
                      value={card.cvc}
                      onChange={e => handleCardChange('cvc', e.target.value)}
                      maxLength={4} required />
                    <Lock className="w-3.5 h-3.5 text-gray-400 absolute right-3 top-1/2 -translate-y-1/2" />
                  </div>
                </div>
              </div>
            </div>

            {/* Security notice */}
            <div className="flex items-center gap-2 mt-4 text-xs text-gray-400">
              <ShieldCheck className="w-4 h-4 text-green-500" />
              Card details are validated securely. No card data is stored.
            </div>

            {/* Order summary */}
            <div className="border-t mt-5 pt-4 space-y-1">
              <div className="flex justify-between text-sm text-gray-500">
                <span>{flight.flightNumber} · Seats {seatNumbers.join(', ')}</span>
                <span>${flight.price} × {passengers.length}</span>
              </div>
              <div className="flex justify-between font-semibold text-lg">
                <span>Total</span>
                <span className="text-brand-700">${total}</span>
              </div>
            </div>
          </div>

          <div className="flex gap-3">
            <button type="button" onClick={() => setStep('passengers')} className="btn-secondary flex-1">
              ← Back
            </button>
            <button type="submit" disabled={loading || validating} className="btn-primary flex-1 py-3">
              {validating ? 'Validating card...' : loading ? 'Processing...' : `Pay $${total}`}
            </button>
          </div>
        </form>
      )}

      {/* Step 3 — Done */}
      {step === 'done' && booking && (
        <div className="card text-center py-10">
          <CheckCircle className="w-16 h-16 text-green-500 mx-auto mb-4" />
          <h3 className="text-2xl font-bold text-gray-900 mb-2">Booking Confirmed!</h3>
          <p className="text-gray-500 mb-2">
            Payment processed. A confirmation email will be sent shortly.
          </p>
          <p className="font-mono text-sm bg-gray-100 inline-block px-3 py-1 rounded mb-6">
            Ref: {booking.id?.substring(0, 8).toUpperCase()}
          </p>
          <div className="flex gap-3 justify-center">
            <button onClick={() => nav('/my-bookings')} className="btn-primary">
              View My Bookings
            </button>
            <button onClick={() => nav('/search')} className="btn-secondary">
              Search More Flights
            </button>
          </div>
        </div>
      )}
    </div>
  )
}