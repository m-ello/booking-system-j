import { useState, type FormEvent } from 'react'
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '../api/client'
import type { BookingFilters, BookingStatus } from '../api/types'
import BookingItem from '../components/BookingItem'
import { useToast } from '../components/Toast'

interface Draft {
  status: '' | BookingStatus
  tableId: string
  email: string
  from: string
  to: string
  dir: 'asc' | 'desc'
}

const EMPTY: Draft = { status: '', tableId: '', email: '', from: '', to: '', dir: 'desc' }

function toFilters(d: Draft, page: number): BookingFilters {
  const filters: BookingFilters = { page, size: 10, sort: 'startTime', dir: d.dir }
  if (d.status) filters.status = d.status
  if (d.tableId) filters.tableId = Number(d.tableId)
  if (d.email.trim()) filters.email = d.email.trim()
  if (d.from) filters.from = new Date(`${d.from}T00:00`).toISOString()
  if (d.to) {
    const end = new Date(`${d.to}T00:00`)
    end.setDate(end.getDate() + 1) // "to" is inclusive, the API's end is exclusive
    filters.to = end.toISOString()
  }
  return filters
}

export default function AdminBookingsPage() {
  const toast = useToast()
  const queryClient = useQueryClient()
  const [draft, setDraft] = useState<Draft>(EMPTY)
  const [applied, setApplied] = useState<Draft>(EMPTY)
  const [page, setPage] = useState(0)

  const tables = useQuery({ queryKey: ['tables', 'all'], queryFn: api.tables })
  const result = useQuery({
    queryKey: ['bookings', 'admin', applied, page],
    queryFn: () => api.searchBookings(toFilters(applied, page)),
    placeholderData: keepPreviousData,
  })

  const cancel = useMutation({
    mutationFn: api.cancelBooking,
    onSuccess: async () => {
      toast('Booking cancelled.')
      await queryClient.invalidateQueries({ queryKey: ['bookings'] })
      await queryClient.invalidateQueries({ queryKey: ['tables', 'available'] })
    },
    onError: (err: Error) => toast(err.message, 'error'),
  })

  const set = <K extends keyof Draft>(key: K, value: Draft[K]) => setDraft((d) => ({ ...d, [key]: value }))

  function apply(event: FormEvent) {
    event.preventDefault()
    setApplied(draft)
    setPage(0)
  }

  function clear() {
    setDraft(EMPTY)
    setApplied(EMPTY)
    setPage(0)
  }

  function confirmCancel(id: number) {
    if (window.confirm('Cancel this booking?')) cancel.mutate(id)
  }

  return (
    <>
      <form className="card" onSubmit={apply}>
        <h1>All bookings</h1>
        <div className="row">
          <div>
            <label htmlFor="status">Status</label>
            <select id="status" value={draft.status} onChange={(e) => set('status', e.target.value as Draft['status'])}>
              <option value="">Any</option>
              <option value="CONFIRMED">CONFIRMED</option>
              <option value="CANCELLED">CANCELLED</option>
              <option value="PENDING">PENDING</option>
            </select>
          </div>
          <div>
            <label htmlFor="table">Table</label>
            <select id="table" value={draft.tableId} onChange={(e) => set('tableId', e.target.value)}>
              <option value="">Any</option>
              {tables.data?.map((t) => <option key={t.id} value={t.id}>{t.label}</option>)}
            </select>
          </div>
          <div>
            <label htmlFor="email">Guest email contains</label>
            <input id="email" value={draft.email} onChange={(e) => set('email', e.target.value)} />
          </div>
        </div>
        <div className="row">
          <div>
            <label htmlFor="from">From date</label>
            <input id="from" type="date" value={draft.from} onChange={(e) => set('from', e.target.value)} />
          </div>
          <div>
            <label htmlFor="to">To date</label>
            <input id="to" type="date" value={draft.to} onChange={(e) => set('to', e.target.value)} />
          </div>
          <div>
            <label htmlFor="dir">Order</label>
            <select id="dir" value={draft.dir} onChange={(e) => set('dir', e.target.value as Draft['dir'])}>
              <option value="desc">Latest first</option>
              <option value="asc">Earliest first</option>
            </select>
          </div>
        </div>
        <p>
          <button>Apply filters</button>{' '}
          <button type="button" className="ghost" onClick={clear}>Clear</button>
        </p>
      </form>

      <section className="card">
        {result.isPending && <p className="muted">Loading…</p>}
        {result.isError && <p className="error" role="alert">{result.error.message}</p>}
        {result.isSuccess && (
          <>
            {result.data.content.length === 0 && <p className="muted">No bookings match these filters.</p>}
            {result.data.content.map((b) => (
              <BookingItem key={b.id} booking={b} showGuest cancellable={b.status === 'CONFIRMED'} onCancel={confirmCancel} />
            ))}
            <div className="pager">
              <button className="ghost small" disabled={page === 0} onClick={() => setPage(page - 1)}>Previous</button>
              <span className="muted">
                Page {result.data.page + 1} of {Math.max(result.data.totalPages, 1)}, {result.data.totalElements} bookings
              </span>
              <button className="ghost small" disabled={page + 1 >= result.data.totalPages} onClick={() => setPage(page + 1)}>Next</button>
            </div>
          </>
        )}
      </section>
    </>
  )
}
