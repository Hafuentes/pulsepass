import { describe, expect, it } from 'vitest'
import { fecha, fechaCorta, moneda } from '../lib/formato'

describe('formato colombiano', () => {
  it('formatea precios como pesos colombianos sin decimales', () => {
    expect(moneda(250000)).toContain('250.000')
    expect(moneda(250000)).toContain('$')
  })

  it('presenta un texto alternativo cuando no existe fecha', () => {
    expect(fecha(null)).toBe('Fecha por confirmar')
    expect(fechaCorta(undefined)).toBe('Por confirmar')
  })

  it('formatea fechas con la configuración regional colombiana', () => {
    expect(fechaCorta('2026-12-12T20:00:00')).toMatch(/12/)
  })
})
