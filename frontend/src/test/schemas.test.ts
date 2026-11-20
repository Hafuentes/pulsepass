import { describe, expect, it } from 'vitest'
import { AuthUserSchema, EventSummarySchema, TicketSchema } from '../lib/schemas'

describe('esquemas de contratos API', () => {
  it('normaliza las autoridades Spring ROLE_USER al rol USER del frontend', () => {
    const result = AuthUserSchema.parse({ id: 7, username: 'andrea', email: 'andrea@mail.com', roles: ['ROLE_USER'] })
    expect(result.roles).toEqual(['USER'])
  })
  it('valida el contrato de resumen de evento', () => {
    const result = EventSummarySchema.parse({ id: 1, eventCode: 'CMF-2026', name: 'Caribbean Music Fest', category: 'MUSIC', status: 'PUBLISHED', eventDate: '2026-12-12T18:00:00', venueCode: 'SMR-01', venueName: 'Marina Convention Center', venueCity: 'Santa Marta' })
    expect(result.eventCode).toBe('CMF-2026')
  })
  it('rechaza estados de ticket fuera del dominio', () => {
    const result = TicketSchema.safeParse({ ticketCode: 'T-1', type: 'GENERAL', price: 50000, status: 'INVALID', userEmail: 'a@mail.com', eventCode: 'E-1', eventName: 'Evento' })
    expect(result.success).toBe(false)
  })
})
