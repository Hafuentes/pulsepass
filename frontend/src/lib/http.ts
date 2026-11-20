import { z } from 'zod'
import { ErrorResponseSchema } from './schemas'

const API_BASE = (import.meta.env.VITE_API_BASE_URL || '/api').replace(/\/$/, '')
let tokenProvider: () => string | null = () => null
let unauthorizedHandler: (() => void) | null = null
export const configureHttp = (getToken: () => string | null, onUnauthorized: () => void) => { tokenProvider = getToken; unauthorizedHandler = onUnauthorized }

export class ApiError extends Error {
  status: number
  details: Record<string, string>
  constructor(status: number, message: string, details: Record<string, string> = {}) { super(message); this.name = 'ApiError'; this.status = status; this.details = details }
}

export function mensajeError(error: unknown): string {
  if (error instanceof ApiError) {
    if (error.status === 0) return 'No fue posible conectar con PulsePass. Comprueba que el backend esté iniciado.'
    if (error.status === 400) return error.message || 'Revisa los datos ingresados.'
    if (error.status === 401) return 'Tu sesión expiró o las credenciales no son válidas.'
    if (error.status === 403) return 'No tienes permisos para realizar esta acción.'
    if (error.status === 404) return 'No encontramos la información solicitada.'
    if (error.status === 409) return error.message || 'La operación no se puede completar por una regla del sistema.'
    if (error.status >= 500) return 'El servidor tuvo un problema. Inténtalo de nuevo más tarde.'
    return error.message || 'Ocurrió un error inesperado.'
  }
  if (error instanceof Error && error.message.toLowerCase().includes('fetch')) return 'No fue posible conectar con PulsePass. Comprueba que el backend esté iniciado.'
  // Errores propios de la UI (p. ej. «Credenciales inválidas»): se muestran tal cual; TypeError/otros → mensaje genérico.
  if (error instanceof Error && error.name === 'Error' && error.message) return error.message
  return 'Ocurrió un error inesperado. Inténtalo de nuevo.'
}

export async function request<T>(path: string, schema: z.ZodType<T>, options: RequestInit = {}): Promise<T> {
  const headers = new Headers(options.headers)
  if (options.body && !headers.has('Content-Type')) headers.set('Content-Type', 'application/json')
  const token = tokenProvider()
  if (token) headers.set('Authorization', `Bearer ${token}`)
  let response: Response
  try { response = await fetch(`${API_BASE}${path}`, { ...options, headers }) }
  catch { throw new ApiError(0, 'No fue posible conectar con el servidor.') }
  if (response.status === 204) return undefined as T
  const raw: unknown = await response.json().catch(() => null)
  if (!response.ok) {
    const parsed = ErrorResponseSchema.safeParse(raw)
    const body = parsed.success ? parsed.data : null
    if (response.status === 401 && token) unauthorizedHandler?.()
    throw new ApiError(response.status, body?.message || (response.status === 401 ? 'Credenciales inválidas.' : 'No se pudo completar la solicitud.'), body?.details ?? {})
  }
  const parsed = schema.safeParse(raw)
  if (!parsed.success) throw new ApiError(500, 'La respuesta del servidor no tiene el formato esperado.')
  return parsed.data
}
export const jsonBody = (value: unknown) => JSON.stringify(value)
