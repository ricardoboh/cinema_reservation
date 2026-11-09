import type { ReactNode } from 'react'

export function Message({ children, error = false }: { children: ReactNode; error?: boolean }) {
  return <p className={`message ${error ? 'error' : ''}`} role={error ? 'alert' : 'status'}>{children}</p>
}
