export const formatDateTime = (iso: string) =>
  new Date(iso).toLocaleString([], { dateStyle: 'medium', timeStyle: 'short' })

export const formatTime = (iso: string) =>
  new Date(iso).toLocaleTimeString([], { timeStyle: 'short' })

// Value for the "min" attribute of <input type="datetime-local">, in the user's local time.
export function nowForDateTimeInput(): string {
  const local = new Date(Date.now() - new Date().getTimezoneOffset() * 60000)
  return local.toISOString().slice(0, 16)
}
