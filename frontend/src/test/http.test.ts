import { afterEach, describe, expect, it, vi } from 'vitest'
import { z } from 'zod'
import { ApiError, configureHttp, mensajeError, request } from '../lib/http'

const SuccessSchema = z.object({ ok: z.boolean() })

afterEach(() => {
  configureHttp(() => null, () => undefined)
  vi.unstubAllGlobals()
  vi.restoreAllMocks()
})

describe('cliente HTTP y mensajes de error', () => {
  it('traduce conflictos del backend sin ocultar la regla devuelta', () => {
    expect(mensajeError(new ApiError(409, 'El evento no tiene capacidad disponible.'))).toBe('El evento no tiene capacidad disponible.')
  })

  it('presenta un mensaje de permisos para 403', () => {
    expect(mensajeError(new ApiError(403, 'Forbidden'))).toContain('No tienes permisos')
  })

  it('presenta un mensaje genérico para errores de servidor', () => {
    expect(mensajeError(new ApiError(500, 'Internal Server Error'))).toContain('servidor tuvo un problema')
  })

  it('envía el JWT únicamente en Authorization y valida la respuesta', async () => {
    const fetchMock = vi.fn().mockResolvedValue({ ok: true, status: 200, json: async () => ({ ok: true }) })
    vi.stubGlobal('fetch', fetchMock)
    configureHttp(() => 'token-de-prueba', () => undefined)

    await expect(request('/prueba', SuccessSchema)).resolves.toEqual({ ok: true })
    const [, options] = fetchMock.mock.calls[0] as [string, RequestInit]
    expect(new Headers(options.headers).get('Authorization')).toBe('Bearer token-de-prueba')
    expect(fetchMock.mock.calls[0]?.[0]).toBe('/api/prueba')
  })

  it('convierte ErrorResponse en ApiError conservando los detalles de campos', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
      ok: false,
      status: 400,
      json: async () => ({ status: 400, message: 'Datos inválidos', details: { name: 'Obligatorio' } }),
    }))

    await expect(request('/prueba', SuccessSchema)).rejects.toMatchObject({
      name: 'ApiError', status: 400, message: 'Datos inválidos', details: { name: 'Obligatorio' },
    })
  })

  it('cierra la sesión cuando una petición autenticada responde 401', async () => {
    const logout = vi.fn()
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({ ok: false, status: 401, json: async () => ({ message: 'Token expirado' }) }))
    configureHttp(() => 'token-expirado', logout)

    await expect(request('/privado', SuccessSchema)).rejects.toMatchObject({ status: 401 })
    expect(logout).toHaveBeenCalledOnce()
  })

  it('normaliza los fallos de red como ApiError de estado cero', async () => {
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new TypeError('Failed to fetch')))
    await expect(request('/prueba', SuccessSchema)).rejects.toMatchObject({ status: 0 })
  })
  it('muestra tal cual los mensajes de errores propios de la UI y oculta los técnicos', () => {
    expect(mensajeError(new Error('Credenciales inválidas. Verifica tus datos e inténtalo de nuevo.'))).toBe('Credenciales inválidas. Verifica tus datos e inténtalo de nuevo.')
    expect(mensajeError(new TypeError('x is undefined'))).toBe('Ocurrió un error inesperado. Inténtalo de nuevo.')
  })

  it('un cuerpo de error inválido produce un ApiError genérico sin filtrar datos internos', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({ ok: false, status: 500, json: async () => '<html>stack trace</html>' }))
    await expect(request('/prueba', SuccessSchema)).rejects.toMatchObject({ status: 500, message: 'No se pudo completar la solicitud.' })
  })

  it('una respuesta que no cumple el esquema se rechaza', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({ ok: true, status: 200, json: async () => ({ ok: 'no-es-booleano' }) }))
    await expect(request('/prueba', SuccessSchema)).rejects.toMatchObject({ status: 500 })
  })

  it('un 401 sin token (login incorrecto) no dispara el cierre de sesión', async () => {
    const logout = vi.fn()
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({ ok: false, status: 401, json: async () => ({ message: 'Bad credentials' }) }))
    configureHttp(() => null, logout)
    await expect(request('/auth/login', SuccessSchema, { method: 'POST' })).rejects.toMatchObject({ status: 401 })
    expect(logout).not.toHaveBeenCalled()
  })
})
