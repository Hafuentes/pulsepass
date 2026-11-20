import { describe, expect, it } from 'vitest'
import { RUTAS, destinoSeguro } from '../lib/rutas'

describe('rutas centralizadas', () => {
  it('codifica los parámetros dinámicos', () => {
    expect(RUTAS.evento('CMF 2026/x')).toBe('/eventos/CMF%202026%2Fx')
    expect(RUTAS.ticketsEvento('CMF-2026')).toBe('/staff/eventos/CMF-2026/tickets')
  })

  it('destinoSeguro solo acepta rutas internas', () => {
    expect(destinoSeguro('/mis-tickets')).toBe('/mis-tickets')
    expect(destinoSeguro(undefined)).toBe('/')
    expect(destinoSeguro('https://malicioso.example')).toBe('/')
    expect(destinoSeguro('//malicioso.example')).toBe('/')
  })
})
