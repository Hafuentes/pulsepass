/* eslint-disable react-refresh/only-export-components */
import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { api } from '../lib/api'
import { ApiError, configureHttp } from '../lib/http'
import { z } from 'zod'
import { AuthUserSchema } from '../lib/schemas'
import type { AuthUser } from '../lib/schemas'

type Session = { estado: 'cargando' } | { estado: 'anonimo'; motivo?: 'expirada' } | { estado: 'autenticado'; token: string; expiraEn: number; usuario: AuthUser }
type AuthContextValue = { sesion: Session; iniciarSesion: (identifier: string, password: string) => Promise<void>; cerrarSesion: () => void; tieneRol: (...roles: string[]) => boolean }
const AuthContext = createContext<AuthContextValue | null>(null)
const STORAGE_KEY = 'pulsepass.session'
const StoredSessionSchema = z.object({ token: z.string().min(1), expiraEn: z.number().finite().positive() })
type StoredSession = z.infer<typeof StoredSessionSchema>

export function AuthProvider({ children }: { children: ReactNode }) {
  const queryClient = useQueryClient()
  const [sesion, setSesion] = useState<Session>({ estado: 'cargando' })
  const cerrarSesion = useCallback(() => { sessionStorage.removeItem(STORAGE_KEY); queryClient.clear(); setSesion({ estado: 'anonimo' }) }, [queryClient])
  const expirarSesion = useCallback(() => { sessionStorage.removeItem(STORAGE_KEY); queryClient.clear(); setSesion({ estado: 'anonimo', motivo: 'expirada' }) }, [queryClient])
  const leerToken = useCallback(() => sesion.estado === 'autenticado' ? sesion.token : null, [sesion])
  useEffect(() => { configureHttp(leerToken, expirarSesion) }, [leerToken, expirarSesion])
  useEffect(() => {
    let stored: StoredSession | null = null
    try { const raw = sessionStorage.getItem(STORAGE_KEY); if (raw) { const parsed: unknown = JSON.parse(raw); const result = StoredSessionSchema.safeParse(parsed); stored = result.success ? result.data : null; if (!result.success) sessionStorage.removeItem(STORAGE_KEY) } } catch { sessionStorage.removeItem(STORAGE_KEY) }
    if (!stored) { cerrarSesion(); return }
    if (stored.expiraEn <= Date.now()) { expirarSesion(); return }
    let alive = true
    configureHttp(() => stored?.token ?? null, expirarSesion)
    api.me().then((usuario) => { if (alive && stored) setSesion({ estado: 'autenticado', token: stored.token, expiraEn: stored.expiraEn, usuario }) }).catch((error: unknown) => { if (alive) { if (error instanceof ApiError && error.status === 401) expirarSesion(); else cerrarSesion() } })
    return () => { alive = false }
  }, [cerrarSesion, expirarSesion])
  useEffect(() => {
    if (sesion.estado !== 'autenticado') return
    const remaining = sesion.expiraEn - Date.now()
    if (remaining <= 0) { expirarSesion(); return }
    const timer = window.setTimeout(expirarSesion, remaining)
    return () => window.clearTimeout(timer)
  }, [sesion, expirarSesion])
  const iniciarSesion = useCallback(async (identifier: string, password: string) => {
    const auth = await api.login(identifier, password)
    const expiraEn = Date.now() + auth.expiresIn * 1000
    sessionStorage.setItem(STORAGE_KEY, JSON.stringify({ token: auth.accessToken, expiraEn }))
    configureHttp(() => auth.accessToken, expirarSesion)
    try {
      const usuario = AuthUserSchema.parse(await api.me())
      setSesion({ estado: 'autenticado', token: auth.accessToken, expiraEn, usuario })
    } catch (error) {
      sessionStorage.removeItem(STORAGE_KEY)
      queryClient.clear()
      setSesion({ estado: 'anonimo' })
      throw error
    }
  }, [expirarSesion, queryClient])
  const tieneRol = useCallback((...roles: string[]) => sesion.estado === 'autenticado' && roles.some((role) => sesion.usuario.roles.includes(role as 'USER' | 'STAFF' | 'ADMIN')), [sesion])
  const value = useMemo(() => ({ sesion, iniciarSesion, cerrarSesion, tieneRol }), [sesion, iniciarSesion, cerrarSesion, tieneRol])
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
export function useAuth() { const context = useContext(AuthContext); if (!context) throw new Error('useAuth debe usarse dentro de AuthProvider'); return context }
