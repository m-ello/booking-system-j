import { useState, type FormEvent } from 'react'
import { Navigate, useLocation } from 'react-router'
import { useAuth } from '../auth/AuthContext'

type Mode = 'login' | 'register'

export default function LoginPage() {
  const { session, login, register } = useAuth()
  const location = useLocation()
  const from = (location.state as { from?: string } | null)?.from ?? '/'

  const [mode, setMode] = useState<Mode>('login')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  if (session) return <Navigate to={from} replace />

  async function submit(event: FormEvent) {
    event.preventDefault()
    setBusy(true)
    setError(null)
    try {
      await (mode === 'login' ? login : register)(email, password)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Something went wrong')
    } finally {
      setBusy(false)
    }
  }

  return (
    <main>
      <section className="card">
        <h1>Welcome</h1>
        <p className="muted">Log in to book a table, or create an account.</p>
        <div className="tabs">
          <button className={mode === 'login' ? 'on' : ''} onClick={() => setMode('login')}>Log in</button>
          <button className={mode === 'register' ? 'on' : ''} onClick={() => setMode('register')}>Register</button>
        </div>
        <form onSubmit={submit}>
          <label htmlFor="email">Email</label>
          <input id="email" type="email" required autoComplete="email" value={email} onChange={(e) => setEmail(e.target.value)} />
          <label htmlFor="password">Password (at least 8 characters)</label>
          <input
            id="password" type="password" required minLength={8}
            autoComplete={mode === 'login' ? 'current-password' : 'new-password'}
            value={password} onChange={(e) => setPassword(e.target.value)}
          />
          {error && <p className="error" role="alert">{error}</p>}
          <p><button disabled={busy}>{mode === 'login' ? 'Log in' : 'Create account'}</button></p>
        </form>
      </section>
    </main>
  )
}
