import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '../api/client'
import BookingItem from '../components/BookingItem'
import { useToast } from '../components/Toast'

export default function MyBookingsPage() {
  const toast = useToast()
  const queryClient = useQueryClient()
  const [now] = useState(() => Date.now())

  const bookings = useQuery({ queryKey: ['bookings', 'mine'], queryFn: api.myBookings })

  const cancel = useMutation({
    mutationFn: api.cancelBooking,
    onSuccess: async () => {
      toast('Booking cancelled.')
      await queryClient.invalidateQueries({ queryKey: ['bookings'] })
      await queryClient.invalidateQueries({ queryKey: ['tables', 'available'] })
    },
    onError: (err: Error) => toast(err.message, 'error'),
  })

  function confirmCancel(id: number) {
    if (window.confirm('Cancel this booking?')) cancel.mutate(id)
  }

  return (
    <section className="card">
      <h1>My bookings</h1>
      {bookings.isPending && <p className="muted">Loading…</p>}
      {bookings.isError && <p className="error" role="alert">{bookings.error.message}</p>}
      {bookings.isSuccess && bookings.data.length === 0 && (
        <p className="muted">No bookings yet. Find a table to make your first one.</p>
      )}
      {bookings.isSuccess && bookings.data.map((b) => (
        <BookingItem
          key={b.id}
          booking={b}
          cancellable={b.status === 'CONFIRMED' && Date.parse(b.startTime) > now}
          onCancel={confirmCancel}
        />
      ))}
    </section>
  )
}
