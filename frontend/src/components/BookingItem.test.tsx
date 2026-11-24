import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import type { Booking } from '../api/types'
import BookingItem from './BookingItem'

const booking: Booking = {
  id: 7,
  userId: 1,
  userEmail: 'guest@test.com',
  tableId: 2,
  tableLabel: 'T2',
  startTime: '2030-01-15T18:00:00Z',
  endTime: '2030-01-15T20:00:00Z',
  partySize: 4,
  status: 'CONFIRMED',
}

describe('BookingItem', () => {
  it('shows the table, the status and the party size', () => {
    render(<BookingItem booking={booking} />)
    expect(screen.getByText('T2')).toBeInTheDocument()
    expect(screen.getByText('confirmed')).toBeInTheDocument()
    expect(screen.getByText(/4 guests/)).toBeInTheDocument()
  })

  it('shows the guest email only when asked to', () => {
    const { rerender } = render(<BookingItem booking={booking} />)
    expect(screen.queryByText(/guest@test.com/)).not.toBeInTheDocument()
    rerender(<BookingItem booking={booking} showGuest />)
    expect(screen.getByText(/guest@test.com/)).toBeInTheDocument()
  })

  it('has no cancel button unless the booking is cancellable', () => {
    render(<BookingItem booking={booking} onCancel={() => {}} />)
    expect(screen.queryByRole('button', { name: 'Cancel booking' })).not.toBeInTheDocument()
  })

  it('calls onCancel with the booking id', async () => {
    const onCancel = vi.fn()
    render(<BookingItem booking={booking} cancellable onCancel={onCancel} />)
    await userEvent.click(screen.getByRole('button', { name: 'Cancel booking' }))
    expect(onCancel).toHaveBeenCalledWith(7)
  })
})
