import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { api, setUnauthorizedHandler, tokenStore } from '../api/client'
import type { AuthResponse, Role } from '../api/types'

interface Session {
  email: string
  role: Role
}

interface AuthState {
  session: Session | null
  login: (email: string, password: string) => Promise<void>
  register: (email: string, password: string) => Promise<void>
  logout: () => void
}

const AuthContext = createContext<AuthState | null>(null)

// Reads the identity from the JWT payload. This is only for display and menu choices:
// the server checks the token signature and role on every request.
function readSession(): Session | null {
  const token = tokenStore.get()
  if (!token) return null
  try {
    const payload = JSON.parse(atob(token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')))
    if (payload.exp * 1000 <= Date.now()) return null
    return { email: payload.email, role: payload.role }
  } catch {
    return null
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const queryClient = useQueryClient()
  const [session, setSession] = useState<Session | null>(readSession)

  const logout = useCallback(() => {
    tokenStore.clear()
    setSession(null)
    queryClient.clear()
  }, [queryClient])

  useEffect(() => setUnauthorizedHandler(logout), [logout])

  const finish = useCallback((response: AuthResponse) => {
    tokenStore.set(response.accessToken)
    setSession(readSession())
  }, [])

  const value = useMemo<AuthState>(
    () => ({
      session,
      login: async (email, password) => finish(await api.login(email, password)),
      register: async (email, password) => finish(await api.register(email, password)),
      logout,
    }),
    [session, finish, logout],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

// eslint-disable-next-line react-refresh/only-export-components
export function useAuth(): AuthState {
  const context = useContext(AuthContext)
  if (!context) throw new Error('useAuth must be used inside AuthProvider')
  return context
}
