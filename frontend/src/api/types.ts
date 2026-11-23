export type Role = 'USER' | 'ADMIN'
export type TableZone = 'INDOOR' | 'TERRACE' | 'BAR' | 'PRIVATE'
export type BookingStatus = 'PENDING' | 'CONFIRMED' | 'CANCELLED'

export interface AuthResponse {
  accessToken: string
  tokenType: string
  expiresInSeconds: number
}

export interface DiningTable {
  id: number
  label: string
  description: string | null
  seats: number
  zone: TableZone
  active: boolean
}

export interface TableInput {
  label: string
  description: string | null
  seats: number
  zone: TableZone
  active?: boolean
}

export interface Booking {
  id: number
  userId: number
  userEmail: string
  tableId: number
  tableLabel: string
  startTime: string
  endTime: string
  partySize: number
  status: BookingStatus
}

export interface BookingInput {
  tableId: number
  startTime: string
  endTime: string
  partySize: number
}

export interface Page<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface BookingFilters {
  status?: BookingStatus
  tableId?: number
  email?: string
  from?: string
  to?: string
  page?: number
  size?: number
  sort?: 'startTime' | 'createdAt'
  dir?: 'asc' | 'desc'
}
