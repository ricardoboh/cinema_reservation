import { useCallback, useEffect, useRef, useState } from 'react'
import { changeReservation, decideReservation, loadReservation } from '../reservations'
import type { Reservation } from '../reservations'
const key = 'deadlock-reservation-ids-v1'
export function knownIds(): number[] { try { const data: unknown = JSON.parse(localStorage.getItem(key) || '[]'); return Array.isArray(data) ? [...new Set(data.filter((id): id is number => Number.isSafeInteger(id) && id > 0))] : [] } catch { return [] } }
export function remember(id: number) { try { localStorage.setItem(key, JSON.stringify([...new Set([id, ...knownIds()])])) } catch { /* The displayed ID can still be used manually. */ } window.dispatchEvent(new Event('reservation-history')) }
export function useReservations() {
  const [items, setItems] = useState<Reservation[]>([])
  const [loadError, setLoadError] = useState('')
  const [actionError, setActionError] = useState('')
  const [busy, setBusy] = useState(false)
  const [loading, setLoading] = useState(true)
  const actionLock = useRef(false)
  const refreshLock = useRef(false)
  const refresh = useCallback(async () => {
    if (refreshLock.current) return
    refreshLock.current = true
    const results = await Promise.allSettled(knownIds().map(loadReservation))
    setItems(results.flatMap(r => r.status === 'fulfilled' ? [r.value] : []))
    setLoadError(results.some(r => r.status === 'rejected') ? 'Some saved reservations could not be loaded. The demo database may have restarted, or the backend may be offline. You can retry or look up an ID.' : '')
    setLoading(false)
    refreshLock.current = false
  }, [])
  useEffect(() => { void Promise.resolve().then(refresh); const timer = window.setInterval(() => void refresh(), 4000); window.addEventListener('reservation-history', refresh); window.addEventListener('storage', refresh); return () => { clearInterval(timer); window.removeEventListener('reservation-history', refresh); window.removeEventListener('storage', refresh) } }, [refresh])
  async function act(id: number, action: 'confirm' | 'cancel' | 'APPROVE' | 'REJECT') { if (actionLock.current) return; actionLock.current = true; setBusy(true); setActionError(''); try { const current = action === 'confirm' || action === 'cancel' ? await changeReservation(id, action) : await decideReservation(id, action); setItems(previous => previous.map(item => item.id === id ? current : item)); await refresh() } catch (e) { await refresh(); setActionError(e instanceof Error ? e.message : 'Please try again.') } finally { actionLock.current = false; setBusy(false) } }
  return { items, error: actionError || loadError, busy, loading, refresh, act }
}
