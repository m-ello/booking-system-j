import { createContext, useCallback, useContext, useRef, useState, type ReactNode } from 'react'

type Kind = 'info' | 'error'
interface ToastItem {
  id: number
  text: string
  kind: Kind
}
type Notify = (text: string, kind?: Kind) => void

const ToastContext = createContext<Notify>(() => {})

export function ToastProvider({ children }: { children: ReactNode }) {
  const [toasts, setToasts] = useState<ToastItem[]>([])
  const nextId = useRef(0)

  const notify = useCallback<Notify>((text, kind = 'info') => {
    const id = nextId.current++
    setToasts((current) => [...current, { id, text, kind }])
    setTimeout(() => setToasts((current) => current.filter((t) => t.id !== id)), 4000)
  }, [])

  return (
    <ToastContext.Provider value={notify}>
      {children}
      <div className="toasts" aria-live="polite">
        {toasts.map((t) => (
          <div key={t.id} className={`toast ${t.kind}`}>{t.text}</div>
        ))}
      </div>
    </ToastContext.Provider>
  )
}

// eslint-disable-next-line react-refresh/only-export-components
export function useToast(): Notify {
  return useContext(ToastContext)
}
