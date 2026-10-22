import { useEffect, useState } from 'react'
export function usePath() {
  const [path, setPath] = useState(location.pathname)
  useEffect(() => { const update = () => setPath(location.pathname); window.addEventListener('popstate', update); return () => window.removeEventListener('popstate', update) }, [])
  return path
}
