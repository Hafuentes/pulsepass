import { z } from 'zod'
import { request, jsonBody } from './http'
import { ArtistSchema, AuthResponseSchema, AuthUserSchema, EventSchema, EventSummarySchema, TicketSchema, UserSchema, VenueSchema, type TicketType } from './schemas'

const list = <T extends z.ZodType>(schema: T) => z.array(schema)
const query = (key: string, value: string) => `?${key}=${encodeURIComponent(value)}`
export const api = {
  login: (identifier: string, password: string) => request('/auth/login', AuthResponseSchema, { method: 'POST', body: jsonBody({ identifier, password }) }),
  me: () => request('/auth/me', AuthUserSchema),
  register: (data: { username: string; email: string; firstName: string; lastName: string; phone?: string; city?: string; birthDate: string; password: string }) => request('/users', UserSchema, { method: 'POST', body: jsonBody(data) }),
  userByEmail: (email: string) => request(`/users/by-email${query('email', email)}`, UserSchema),
  userByUsername: (username: string) => request(`/users/by-username${query('username', username)}`, UserSchema),
  events: () => request('/events/published', list(EventSummarySchema)),
  adminEvents: () => request('/events/admin', list(EventSummarySchema)),
  event: (code: string) => request(`/events/${encodeURIComponent(code)}`, EventSchema),
  eventsByArtist: (stageName: string) => request(`/events/by-artist${query('stageName', stageName)}`, list(EventSummarySchema)),
  createEvent: (data: { eventCode: string; name: string; description: string; category: string; eventDate: string; minimumAge: number; venueCode: string }) => request('/events', EventSchema, { method: 'POST', body: jsonBody(data) }),
  updateEvent: (code: string, data: { name: string; description: string; category: string; eventDate: string; minimumAge: number; venueCode: string }) => request(`/events/${encodeURIComponent(code)}`, EventSchema, { method: 'PUT', body: jsonBody(data) }),
  publishEvent: (code: string) => request(`/events/${encodeURIComponent(code)}/publish`, EventSchema, { method: 'PATCH' }),
  changeEventStatus: (code: string, status: string) => request(`/events/${encodeURIComponent(code)}/status`, EventSchema, { method: 'PATCH', body: jsonBody({ status }) }),
  addArtist: (code: string, artistId: number) => request(`/events/${encodeURIComponent(code)}/artists/${artistId}`, EventSchema, { method: 'POST' }),
  removeArtist: (code: string, artistId: number) => request(`/events/${encodeURIComponent(code)}/artists/${artistId}`, EventSchema, { method: 'DELETE' }),
  artists: () => request('/artists/active', list(ArtistSchema)),
  artist: (id: string) => request(`/artists/${encodeURIComponent(id)}`, ArtistSchema),
  artistByName: (stageName: string) => request(`/artists/by-stage-name${query('stageName', stageName)}`, ArtistSchema),
  venues: () => request('/venues/active', list(VenueSchema)),
  createVenue: (data: { code: string; name: string; city: string; address?: string; capacity: number }) => request('/venues', VenueSchema, { method: 'POST', body: jsonBody(data) }),
  createArtist: (data: { stageName: string; country?: string; genre?: string }) => request('/artists', ArtistSchema, { method: 'POST', body: jsonBody(data) }),
  venue: (code: string) => request(`/venues/${encodeURIComponent(code)}`, VenueSchema),
  buyTicket: (data: { userEmail: string; eventCode: string; type: TicketType }) => request('/tickets', TicketSchema, { method: 'POST', body: jsonBody(data) }),
  ticket: (code: string) => request(`/tickets/${encodeURIComponent(code)}`, TicketSchema),
  myTickets: (email: string) => request(`/tickets/by-user${query('email', email)}`, list(TicketSchema)),
  paidTickets: (eventCode: string) => request(`/events/${encodeURIComponent(eventCode)}/tickets/paid`, list(TicketSchema)),
  cancelTicket: (code: string) => request(`/tickets/${encodeURIComponent(code)}/cancel`, TicketSchema, { method: 'PATCH' }),
  useTicket: (code: string) => request(`/tickets/${encodeURIComponent(code)}/use`, TicketSchema, { method: 'PATCH' }),
}
export type Api = typeof api
