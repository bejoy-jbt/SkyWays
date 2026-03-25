import { useEffect, useState } from 'react'
import { paymentApi } from '../../services/api'
import { Payment } from '../../types'
import { format, parseISO } from 'date-fns'

export default function AdminPayments() {
  const [payments, setPayments] = useState<Payment[]>([])
  const [total, setTotal] = useState(0)

  useEffect(() => {
    paymentApi.getAll().then(r => {
      setPayments(r.data)
      setTotal(r.data.filter((p:Payment)=>p.status==='SUCCEEDED').reduce((s:number,p:Payment)=>s+p.amount,0))
    })
  }, [])

  return (
    <div>
      <div className="grid grid-cols-3 gap-4 mb-6">
        {[
          { label:'Total Revenue', value: `$${total.toFixed(2)}`, cls:'text-green-600' },
          { label:'Transactions',  value: payments.length, cls:'text-brand-700' },
          { label:'Failed',        value: payments.filter(p=>p.status==='FAILED').length, cls:'text-red-500' },
        ].map(s=>(
          <div key={s.label} className="card">
            <p className="text-sm text-gray-500">{s.label}</p>
            <p className={`text-2xl font-bold mt-1 ${s.cls}`}>{s.value}</p>
          </div>
        ))}
      </div>
      <div className="overflow-x-auto">
        <table className="w-full text-sm">
          <thead>
            <tr className="border-b bg-gray-50">
              {['Booking Ref','User','Amount','Stripe ID','Status','Date'].map(h=>(
                <th key={h} className="text-left py-3 px-3 text-xs font-semibold text-gray-500 uppercase">{h}</th>
              ))}
            </tr>
          </thead>
          <tbody>
            {payments.map(p => (
              <tr key={p.id} className="border-b hover:bg-gray-50">
                <td className="py-3 px-3 font-mono text-xs">{p.bookingId.substring(0,8).toUpperCase()}</td>
                <td className="py-3 px-3 text-gray-600">{p.userEmail}</td>
                <td className="py-3 px-3 font-medium text-brand-700">${p.amount}</td>
                <td className="py-3 px-3 font-mono text-xs text-gray-400">{p.stripePaymentIntentId?.substring(0,20) ?? '—'}...</td>
                <td className="py-3 px-3">
                  <span className={`text-xs px-2 py-0.5 rounded-full font-medium
                    ${p.status==='SUCCEEDED'?'bg-green-100 text-green-700':
                      p.status==='FAILED'?'bg-red-100 text-red-700':'bg-yellow-100 text-yellow-700'}`}>
                    {p.status}
                  </span>
                </td>
                <td className="py-3 px-3 text-gray-400">{p.createdAt?format(parseISO(p.createdAt),'MMM d HH:mm'):'—'}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  )
}
