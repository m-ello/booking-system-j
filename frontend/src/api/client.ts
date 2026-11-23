import type {
  AuthResponse, Booking, BookingFilters, BookingInput, DiningTable, Page, TableInput,
} from './types'

const TOKEN_KEY = 'token'

export const tokenStore = {
  get: () => localStorage.getItem(TOKEN_KEY),
  set: (token: string) => localStorage.setItem(TOKEN_KEY, token),
  clear: () => localStorage.removeItem(TOKEN_KEY),
}

export class ApiError extends Error {
  status: number
  constructor(status: number, message: string) {
    super(message)
    this.status = status
  }
}

interface ProblemBody {
  detail?: string
  errors?: Record<string, string>
}

let onUnauthorized: () => void = () => {}
export function setUnauthorizedHandler(handler: () => void) {
  onUnauthorized = handler
}

async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const token = tokenStore.get()
  const res = await fetch('/api' + path, {
    ...init,
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
  })
  if (res.status === 401 && token) {
    onUnauthorized()
    throw new ApiError(401, 'Your session expired. Log in again.')
  }
  if (res.status === 403) throw new ApiError(403, 'Your account cannot do that.')

  const body: ProblemBody | null = res.status === 204 ? null : await res.json().catch(() => null)
  if (!res.ok) {
    const fields = body?.errors ? Object.values(body.errors).join(', ') : null
    throw new ApiError(res.status, fields || body?.detail || `Request failed (${res.status})`)
  }
  return body as T
}

const json = (value: unknown) => JSON.stringify(value)

export const api = {
  register: (email: string, password: string) =>
    request<AuthResponse>('/auth/register', { method: 'POST', body: json({ email, password }) }),
  login: (email: string, password: string) =>
    request<AuthResponse>('/auth/login', { method: 'POST', body: json({ email, password }) }),

  tables: () => request<DiningTable[]>('/tables'),
  availableTables: (start: string, end: string, partySize: number) =>
    request<DiningTable[]>('/tables/available?' + new URLSearchParams({ start, end, partySize: String(partySize) })),
  createTable: (input: TableInput) => request<DiningTable>('/tables', { method: 'POST', body: json(input) }),
  updateTable: (id: number, input: TableInput) =>
    request<DiningTable>(`/tables/${id}`, { method: 'PUT', body: json(input) }),
  deactivateTable: (id: number) => request<void>(`/tables/${id}`, { method: 'DELETE' }),

  createBooking: (input: BookingInput) => request<Booking>('/bookings', { method: 'POST', body: json(input) }),
  myBookings: () => request<Booking[]>('/bookings/my'),
  cancelBooking: (id: number) => request<void>(`/bookings/${id}`, { method: 'DELETE' }),
  searchBookings: (filters: BookingFilters) => {
    const params = new URLSearchParams()
    Object.entries(filters).forEach(([key, value]) => {
      if (value !== undefined && value !== '') params.set(key, String(value))
    })
    return request<Page<Booking>>('/bookings?' + params)
  },
}
