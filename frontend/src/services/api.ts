import axios from 'axios'
import { useAuthStore } from '../store/authStore'

const api = axios.create({ baseURL: '/api' })

// Attach JWT to every request
api.interceptors.request.use((config) => {
  const token = useAuthStore.getState().user?.token
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

// Auto logout on 401
api.interceptors.response.use(
  (res) => res,
  (err) => {
    if (err.response?.status === 401) {
      useAuthStore.getState().logout()
      window.location.href = '/login'
    }
    return Promise.reject(err)
  }
)

// ── Auth ──────────────────────────────────────────────────────
export const authApi = {
  register: (data: any)  => api.post('/auth/register', data),
  login:    (data: any)  => api.post('/auth/login', data),
  getUsers: ()           => api.get('/auth/admin/users'),
  toggleUser: (id: number, enabled: boolean) =>
    api.patch(`/auth/admin/users/${id}/toggle?enabled=${enabled}`),
}

// ── Flights ───────────────────────────────────────────────────
export const flightApi = {
  search:   (data: any)  => api.post('/flights/search', data),
  getAll:   ()           => api.get('/flights'),
  getById:  (id: number) => api.get(`/flights/${id}`),
  getSeats: (id: number) => api.get(`/flights/${id}/seats`),
  // Admin
  create:   (data: any)  => api.post('/admin/flights', data),
  update:   (id: number, data: any) => api.put(`/admin/flights/${id}`, data),
  delete:   (id: number) => api.delete(`/admin/flights/${id}`),
}

// ── Bookings ──────────────────────────────────────────────────
export const bookingApi = {
  create:   (data: any)  => api.post('/bookings', data),
  getMy:    ()           => api.get('/bookings/my'),
  getById:  (id: string) => api.get(`/bookings/${id}`),
  getAll:   ()           => api.get('/bookings/admin/all'),
}

// ── Payments ──────────────────────────────────────────────────
export const paymentApi = {
  getMy:    ()           => api.get('/payments/my'),
  getAll:   ()           => api.get('/payments/admin/all'),
  createIntent: (data: any) => api.post('/payments/create-intent', data),
}

export default api
