import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter } from 'react-router'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { AuthProvider } from '../auth/AuthContext'
import LoginPage from './LoginPage'

function renderPage() {
  return render(
    <QueryClientProvider client={new QueryClient()}>
      <MemoryRouter>
        <AuthProvider>
          <LoginPage />
        </AuthProvider>
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('LoginPage', () => {
  beforeEach(() => localStorage.clear())
  afterEach(() => vi.restoreAllMocks())

  it('shows the server message when the login fails', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(
      new Response(JSON.stringify({ detail: 'Invalid email or password' }), {
        status: 401,
        headers: { 'Content-Type': 'application/json' },
      }),
    )
    renderPage()
    await userEvent.type(screen.getByLabelText('Email'), 'a@test.com')
    await userEvent.type(screen.getByLabelText(/password/i), 'wrong-password{enter}')
    expect(await screen.findByRole('alert')).toHaveTextContent('Invalid email or password')
  })

  it('switches to the registration form', async () => {
    renderPage()
    await userEvent.click(screen.getByRole('button', { name: 'Register' }))
    expect(screen.getByRole('button', { name: 'Create account' })).toBeInTheDocument()
  })
})
