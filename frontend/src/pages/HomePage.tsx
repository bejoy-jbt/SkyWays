import { Link } from 'react-router-dom'
import { Plane, Shield, Clock, CreditCard } from 'lucide-react'
import { useAuthStore } from '../store/authStore'

export default function HomePage() {
  const user = useAuthStore(s => s.user)

  return (
    <div>
      {/* Hero */}
      <div className="relative bg-gradient-to-r from-brand-700 to-brand-900 rounded-2xl overflow-hidden mb-10">
        <div className="px-8 py-16 relative z-10">
          <h1 className="text-4xl md:text-5xl font-bold text-white mb-4">
            Your Journey Starts Here
          </h1>
          <p className="text-brand-100 text-xl mb-8 max-w-lg">
            Search flights, choose your perfect seat, and book instantly.
          </p>
          {user ? (
            <Link to="/search" className="inline-flex items-center gap-2 bg-white text-brand-700 
              px-8 py-3 rounded-xl font-semibold hover:bg-brand-50 transition-colors text-lg">
              <Plane className="w-5 h-5" /> Search Flights
            </Link>
          ) : (
            <div className="flex gap-3">
              <Link to="/register" className="inline-flex items-center gap-2 bg-white text-brand-700 
                px-6 py-3 rounded-xl font-semibold hover:bg-brand-50 transition-colors">
                Get Started
              </Link>
              <Link to="/login" className="inline-flex items-center gap-2 border border-white text-white 
                px-6 py-3 rounded-xl font-semibold hover:bg-white/10 transition-colors">
                Sign In
              </Link>
            </div>
          )}
        </div>
        <div className="absolute right-0 top-0 w-96 h-full opacity-10">
          <Plane className="w-full h-full" />
        </div>
      </div>

      {/* Features */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-10">
        {[
          { icon: Plane,      title: 'Search Flights',    desc: 'Find flights between cities with real-time seat availability' },
          { icon: Shield,     title: 'Secure Payments',   desc: 'Stripe-powered checkout with fraud protection and encryption' },
          { icon: Clock,      title: 'Instant Booking',   desc: 'Seat locked immediately — no double bookings, ever' },
          { icon: CreditCard, title: 'Easy Cancellation', desc: 'Flexible cancellation policy with email notifications' },
        ].map(f => (
          <div key={f.title} className="card flex items-start gap-4">
            <div className="w-10 h-10 bg-brand-100 rounded-lg flex items-center justify-center flex-shrink-0">
              <f.icon className="w-5 h-5 text-brand-600" />
            </div>
            <div>
              <h3 className="font-semibold text-gray-900">{f.title}</h3>
              <p className="text-gray-500 text-sm mt-1">{f.desc}</p>
            </div>
          </div>
        ))}
      </div>
    </div>
  )
}
