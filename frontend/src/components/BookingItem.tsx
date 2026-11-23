import type { Booking } from '../api/types'
import { formatDateTime, formatTime } from '../lib/format'

interface Props {
  booking: Booking
  showGuest?: boolean
  cancellable?: boolean
  onCancel?: (id: number) => void
}

export default function BookingItem({ booking: b, showGuest = false, cancellable = false, onCancel }: Props) {
  return (
    <div className="item">
      <div>
        <strong>{b.tableLabel}</strong>
        <span className={`pill ${b.status}`}>{b.status.toLowerCase()}</span>
        <div className="muted">
          {formatDateTime(b.startTime)} to {formatTime(b.endTime)}, {b.partySize} guests
          {showGuest ? `, ${b.userEmail}` : ''}
        </div>
      </div>
      {cancellable && onCancel && (
        <button className="ghost small" onClick={() => onCancel(b.id)}>Cancel booking</button>
      )}
    </div>
  )
}
