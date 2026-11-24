import { useState, useEffect, useRef, type FormEvent } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '../api/client'
import type { DiningTable, TableInput, TableZone } from '../api/types'
import { useToast } from '../components/Toast'

const ZONES: TableZone[] = ['INDOOR', 'TERRACE', 'BAR', 'PRIVATE']

function toInput(t: DiningTable, active: boolean): TableInput {
  return { label: t.label, description: t.description, seats: t.seats, zone: t.zone, active }
}

interface FormProps {
  editing: DiningTable | null
  onDone: () => void
}

function TableForm({ editing, onDone }: FormProps) {
  const toast = useToast()
  const queryClient = useQueryClient()
  const [label, setLabel] = useState(editing?.label ?? '')
  const [description, setDescription] = useState(editing?.description ?? '')
  const [seats, setSeats] = useState(editing?.seats ?? 4)
  const [zone, setZone] = useState<TableZone>(editing?.zone ?? 'INDOOR')
  const [active, setActive] = useState(editing?.active ?? true)
  const noteRef = useRef<HTMLTextAreaElement>(null)

  useEffect(() => {
    const textarea = noteRef.current
    if (textarea) {
      textarea.style.height = 'auto'
      textarea.style.height = `${textarea.scrollHeight}px`
    }
  }, [description])

  const save = useMutation({
    mutationFn: (input: TableInput) => (editing ? api.updateTable(editing.id, input) : api.createTable(input)),
    onSuccess: async () => {
      toast(editing ? 'Table updated.' : 'Table added.')
      await queryClient.invalidateQueries({ queryKey: ['tables'] })
      onDone()
    },
    onError: (err: Error) => toast(err.message, 'error'),
  })

  function submit(event: FormEvent) {
    event.preventDefault()
    save.mutate({ label, description: description || null, seats, zone, active })
  }

  return (
    <form className="card" onSubmit={submit}>
      <h1>{editing ? `Edit table ${editing.label}` : 'Add a table'}</h1>
      <div className="row">
        <div>
          <label htmlFor="label">Name</label>
          <input id="label" required maxLength={100} placeholder="T7" value={label} onChange={(e) => setLabel(e.target.value)} />
        </div>
        <div>
          <label htmlFor="seats">Seats</label>
          <input id="seats" type="number" min={1} max={50} required value={seats} onChange={(e) => setSeats(Number(e.target.value))} />
        </div>
        <div>
          <label htmlFor="zone">Area</label>
          <select id="zone" value={zone} onChange={(e) => setZone(e.target.value as TableZone)}>
            {ZONES.map((z) => <option key={z} value={z}>{z}</option>)}
          </select>
        </div>
      </div>
      <label htmlFor="note">Note (optional)</label>
      <textarea
          ref={noteRef}
          id="note"
          maxLength={500}
          placeholder="By the window"
          value={description}
          onChange={(e) => setDescription(e.target.value)}
          rows={1}
      />
      <label htmlFor="active">
        <input id="active" type="checkbox" checked={active} onChange={(e) => setActive(e.target.checked)} />
        Available for booking
      </label>
      <p>
        <button disabled={save.isPending}>{editing ? 'Save changes' : 'Add table'}</button>{' '}
        {editing && <button type="button" className="ghost" onClick={onDone}>Stop editing</button>}
      </p>
    </form>
  )
}

export default function AdminTablesPage() {
  const toast = useToast()
  const queryClient = useQueryClient()
  const [editing, setEditing] = useState<DiningTable | null>(null)
  const [formKey, setFormKey] = useState(0)

  const tables = useQuery({ queryKey: ['tables', 'all'], queryFn: api.tables })

  const toggle = useMutation({
    mutationFn: async (t: DiningTable) => {
      if (t.active) await api.deactivateTable(t.id)
      else await api.updateTable(t.id, toInput(t, true))
    },
    onSuccess: async (_data, t) => {
      toast(t.active ? 'Table deactivated.' : 'Table reactivated.')
      await queryClient.invalidateQueries({ queryKey: ['tables'] })
    },
    onError: (err: Error) => toast(err.message, 'error'),
  })

  function finishForm() {
    setEditing(null)
    setFormKey((k) => k + 1)
  }

  return (
    <>
      <TableForm key={`${editing?.id ?? 'new'}-${formKey}`} editing={editing} onDone={finishForm} />
      <section className="card">
        {tables.isPending && <p className="muted">Loading…</p>}
        {tables.isError && <p className="error" role="alert">{tables.error.message}</p>}
        {tables.isSuccess && tables.data.length === 0 && <p className="muted">No tables yet. Add your first one above.</p>}
        {tables.isSuccess && tables.data.map((t) => (
          <div className="item" key={t.id}>
            <div>
              <strong>{t.label}</strong>
              <span className="pill">{t.zone.toLowerCase()}</span>
              {!t.active && <span className="pill CANCELLED">inactive</span>}
              <div className="muted">{t.seats} seats{t.description ? `, ${t.description}` : ''}</div>
            </div>
            <div>
              <button className="ghost small" onClick={() => { setEditing(t); window.scrollTo({ top: 0 }) }}>Edit</button>{' '}
              <button className="ghost small" disabled={toggle.isPending} onClick={() => toggle.mutate(t)}>
                {t.active ? 'Deactivate' : 'Reactivate'}
              </button>
            </div>
          </div>
        ))}
      </section>
    </>
  )
}
