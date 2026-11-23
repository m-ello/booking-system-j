import { useState, type FormEvent } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useNavigate } from 'react-router'
import { api } from '../api/client'
import { useToast } from '../components/Toast'
import { formatDateTime, nowForDateTimeInput } from '../lib/format'

interface Search {
  start: string
  end: string
  partySize: number
}

export default function FindTablePage() {
  const toast = useToast()
  const navigate = useNavigate()
  const queryClient = useQueryClient()

  const [start, setStart] = useState('')
  const [minutes, setMinutes] = useState(120)
  const [partySize, setPartySize] = useState(2)
  const [search, setSearch] = useState<Search | null>(null)

  const available = useQuery({
    queryKey: ['tables', 'available', search],
    queryFn: () => api.availableTables(search!.start, search!.end, search!.partySize),
    enabled: search !== null,
  })

  const book = useMutation({
    mutationFn: api.createBooking,
    onSuccess: async () => {
      toast('Booked.')
      await queryClient.invalidateQueries({ queryKey: ['bookings'] })
      await queryClient.invalidateQueries({ queryKey: ['tables', 'available'] })
      navigate('/bookings')
    },
    onError: (err: Error) => toast(err.message, 'error'),
  })

  function submit(event: FormEvent) {
    event.preventDefault()
    const startDate = new Date(start)
    setSearch({
      start: startDate.toISOString(),
      end: new Date(startDate.getTime() + minutes * 60000).toISOString(),
      partySize,
    })
  }

  return (
    <>
      <form className="card" onSubmit={submit}>
        <h1>Find a table</h1>
        <div className="row">
          <div>
            <label htmlFor="start">Date and time</label>
            <input id="start" type="datetime-local" required min={nowForDateTimeInput()}
              value={start} onChange={(e) => setStart(e.target.value)} />
          </div>
          <div>
            <label htmlFor="minutes">Stay for</label>
            <select id="minutes" value={minutes} onChange={(e) => setMinutes(Number(e.target.value))}>
              <option value={60}>1 hour</option>
              <option value={90}>1.5 hours</option>
              <option value={120}>2 hours</option>
              <option value={180}>3 hours</option>
            </select>
          </div>
          <div>
            <label htmlFor="party">Guests</label>
            <input id="party" type="number" min={1} max={50} required
              value={partySize} onChange={(e) => setPartySize(Number(e.target.value))} />
          </div>
        </div>
        <p><button>Show free tables</button></p>
      </form>

      {search && (
        <section className="card">
          {available.isPending && <p className="muted">Searching…</p>}
          {available.isError && <p className="error" role="alert">{available.error.message}</p>}
          {available.isSuccess && (
            <>
              <strong>
                {available.data.length
                  ? `Free tables for ${formatDateTime(search.start)}`
                  : `No free tables at ${formatDateTime(search.start)}`}
              </strong>
              {available.data.length === 0 && <p className="muted">Try a different time or a smaller group.</p>}
              {available.data.map((t) => (
                <div className="item" key={t.id}>
                  <div>
                    <strong>{t.label}</strong>
                    <span className="pill">{t.zone.toLowerCase()}</span>
                    <div>
                      <span className="seats" aria-label={`${t.seats} seats`}>{'●'.repeat(Math.min(t.seats, 12))}</span>{' '}
                      <span className="muted">{t.seats} seats{t.description ? `, ${t.description}` : ''}</span>
                    </div>
                  </div>
                  <button className="small" disabled={book.isPending}
                    onClick={() => book.mutate({ tableId: t.id, startTime: search.start, endTime: search.end, partySize: search.partySize })}>
                    Book
                  </button>
                </div>
              ))}
            </>
          )}
        </section>
      )}
    </>
  )
}
