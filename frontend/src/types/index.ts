export interface User {
  email: string
  firstName: string
  lastName: string
  role: 'USER' | 'ADMIN'
  token: string
}

export interface Flight {
  id: number
  flightNumber: string
  origin: string
  destination: string
  departureDate: string
  departureTime: string
  arrivalTime: string
  price: number
  aircraftType: string
  totalRows: number
  seatsPerRow: number
  availableSeats: number
  status: string
}

export interface Seat {
  id: number
  seatNumber: string
  status: 'AVAILABLE' | 'LOCKED' | 'BOOKED'
  row: number
  column: string
}

export interface Passenger {
  firstName: string
  lastName: string
  dateOfBirth: string
  passportNumber: string
  nationality: string
  specialRequests?: string
}

export interface Booking {
  id: string
  userEmail: string
  flightId: number
  flightNumber: string
  seatNumber: string
  seatNumbers?: string[]
  totalAmount: number
  status: 'PENDING' | 'SEAT_LOCKED' | 'PAYMENT_PROCESSING' | 'CONFIRMED' | 'PAYMENT_FAILED' | 'CANCELLED'
  paymentIntentId?: string
  passengers: Passenger[]
  createdAt: string
  confirmedAt?: string
}

export interface Payment {
  id: number
  bookingId: string
  userEmail: string
  amount: number
  stripePaymentIntentId?: string
  stripeClientSecret?: string
  status: 'PENDING' | 'SUCCEEDED' | 'PARTIALLY_REFUNDED' | 'FAILED' | 'REFUNDED'
  failureReason?: string
  createdAt: string
  processedAt?: string
}
