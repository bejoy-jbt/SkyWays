import { Passenger } from '../../types'

interface Props {
  index: number
  passenger: Passenger
  onChange: (index: number, p: Passenger) => void
}

export default function PassengerForm({ index, passenger, onChange }: Props) {
  const set = (k: keyof Passenger) => (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) =>
    onChange(index, { ...passenger, [k]: e.target.value })

  return (
    <div className="border border-gray-200 rounded-xl p-5">
      <h4 className="font-semibold text-gray-700 mb-4">Passenger {index + 1}</h4>
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div>
          <label className="label">First Name *</label>
          <input className="input" value={passenger.firstName} onChange={set('firstName')} required />
        </div>
        <div>
          <label className="label">Last Name *</label>
          <input className="input" value={passenger.lastName} onChange={set('lastName')} required />
        </div>
        <div>
          <label className="label">Date of Birth *</label>
          <input className="input" type="date" value={passenger.dateOfBirth} onChange={set('dateOfBirth')} required />
        </div>
        <div>
          <label className="label">Passport Number</label>
          <input className="input" value={passenger.passportNumber} onChange={set('passportNumber')} placeholder="Optional" />
        </div>
        <div>
          <label className="label">Nationality</label>
          <input className="input" value={passenger.nationality} onChange={set('nationality')} placeholder="e.g. American" />
        </div>
        <div>
          <label className="label">Special Requests</label>
          <input className="input" value={passenger.specialRequests ?? ''} onChange={set('specialRequests')} placeholder="Vegetarian meal, wheelchair, etc." />
        </div>
      </div>
    </div>
  )
}
