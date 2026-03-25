import { useEffect, useState } from 'react'
import { authApi } from '../../services/api'
import toast from 'react-hot-toast'
import { format, parseISO } from 'date-fns'
import { UserCheck, UserX } from 'lucide-react'

export default function AdminUsers() {
  const [users, setUsers] = useState<any[]>([])

  const load = () => authApi.getUsers().then(r => setUsers(r.data))
  useEffect(() => { load() }, [])

  const toggle = async (id: number, enabled: boolean) => {
    await authApi.toggleUser(id, enabled)
    toast.success(enabled ? 'User enabled' : 'User disabled')
    load()
  }

  return (
    <div>
      <h3 className="font-semibold text-gray-900 mb-4">All Users ({users.length})</h3>
      <div className="overflow-x-auto">
        <table className="w-full text-sm">
          <thead>
            <tr className="border-b bg-gray-50">
              {['ID','Name','Email','Phone','Role','Status','Joined','Actions'].map(h=>(
                <th key={h} className="text-left py-3 px-3 text-xs font-semibold text-gray-500 uppercase">{h}</th>
              ))}
            </tr>
          </thead>
          <tbody>
            {users.map(u => (
              <tr key={u.id} className="border-b hover:bg-gray-50">
                <td className="py-3 px-3 text-gray-400">{u.id}</td>
                <td className="py-3 px-3 font-medium">{u.firstName} {u.lastName}</td>
                <td className="py-3 px-3 text-gray-600">{u.email}</td>
                <td className="py-3 px-3 text-gray-500">{u.phone ?? '—'}</td>
                <td className="py-3 px-3">
                  <span className={`text-xs px-2 py-0.5 rounded-full font-medium
                    ${u.role==='ADMIN'?'bg-purple-100 text-purple-700':'bg-gray-100 text-gray-600'}`}>
                    {u.role}
                  </span>
                </td>
                <td className="py-3 px-3">
                  <span className={`text-xs px-2 py-0.5 rounded-full font-medium
                    ${u.enabled?'bg-green-100 text-green-700':'bg-red-100 text-red-700'}`}>
                    {u.enabled?'Active':'Disabled'}
                  </span>
                </td>
                <td className="py-3 px-3 text-gray-400">{u.createdAt?format(parseISO(u.createdAt),'MMM d yyyy'):'—'}</td>
                <td className="py-3 px-3">
                  {u.role !== 'ADMIN' && (
                    <button onClick={() => toggle(u.id, !u.enabled)}
                      className={`text-xs flex items-center gap-1 px-2 py-1 rounded ${u.enabled?'text-red-500 hover:bg-red-50':'text-green-600 hover:bg-green-50'}`}>
                      {u.enabled ? <><UserX className="w-3 h-3"/>Disable</> : <><UserCheck className="w-3 h-3"/>Enable</>}
                    </button>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  )
}
