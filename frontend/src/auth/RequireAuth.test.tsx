import { render, screen } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter, Route, Routes } from 'react-router'
import { beforeEach, describe, expect, it } from 'vitest'
import type { Role } from '../api/types'
import { AuthProvider } from './AuthContext'
import RequireAuth from './RequireAuth'

// A token with a readable payload. The signature is fake, because the browser never verifies it.
function fakeToken(role: Role) {
  const payload = btoa(JSON.stringify({ email: 'a@test.com', role, exp: Math.floor(Date.now() / 1000) + 3600 }))
  return `header.${payload}.signature`
}

function renderAt(path: string) {
  return render(
    <QueryClientProvider client={new QueryClient()}>
      <MemoryRouter initialEntries={[path]}>
        <AuthProvider>
          <Routes>
            <Route path="/login" element={<p>login page</p>} />
            <Route element={<RequireAuth />}>
              <Route path="/" element={<p>home page</p>} />
              <Route element={<RequireAuth role="ADMIN" />}>
                <Route path="/admin" element={<p>admin page</p>} />
              </Route>
            </Route>
          </Routes>
        </AuthProvider>
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('RequireAuth', () => {
  beforeEach(() => localStorage.clear())

  it('sends visitors without a token to the login page', () => {
    renderAt('/')
    expect(screen.getByText('login page')).toBeInTheDocument()
  })

  it('lets a logged-in user see normal pages', () => {
    localStorage.setItem('token', fakeToken('USER'))
    renderAt('/')
    expect(screen.getByText('home page')).toBeInTheDocument()
  })

  it('keeps normal users out of admin pages', () => {
    localStorage.setItem('token', fakeToken('USER'))
    renderAt('/admin')
    expect(screen.getByText('home page')).toBeInTheDocument()
    expect(screen.queryByText('admin page')).not.toBeInTheDocument()
  })

  it('lets admins into admin pages', () => {
    localStorage.setItem('token', fakeToken('ADMIN'))
    renderAt('/admin')
    expect(screen.getByText('admin page')).toBeInTheDocument()
  })
})
