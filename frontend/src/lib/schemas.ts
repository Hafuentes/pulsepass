import { z } from 'zod'

export const EventCategorySchema = z.enum(['MUSIC', 'SPORTS', 'TECHNOLOGY', 'EDUCATION', 'CULTURE', 'ENTERTAINMENT'])
export const EventStatusSchema = z.enum(['DRAFT', 'PUBLISHED', 'SOLD_OUT', 'CANCELLED', 'FINISHED'])
export const TicketTypeSchema = z.enum(['GENERAL', 'VIP', 'BACKSTAGE', 'STUDENT'])
export const TicketStatusSchema = z.enum(['RESERVED', 'PAID', 'CANCELLED', 'USED'])
export const RoleSchema = z.enum(['USER', 'STAFF', 'ADMIN'])
const dateLike = z.string().or(z.null()).optional()
export const ArtistSchema = z.object({ id: z.number(), stageName: z.string(), country: z.string().nullable().optional(), genre: z.string().nullable().optional(), active: z.boolean() }).passthrough()
export const VenueSchema = z.object({ id: z.number().optional(), code: z.string(), name: z.string(), city: z.string(), address: z.string().nullable().optional(), capacity: z.number(), active: z.boolean() }).passthrough()
export const EventSummarySchema = z.object({ id: z.number().optional(), eventCode: z.string(), name: z.string(), category: EventCategorySchema, status: EventStatusSchema, eventDate: z.string(), venueCode: z.string(), venueName: z.string(), venueCity: z.string().nullable().optional() }).passthrough()
export const EventSchema = z.object({ id: z.number().optional(), eventCode: z.string(), name: z.string(), description: z.string().nullable().optional(), category: EventCategorySchema, status: EventStatusSchema, eventDate: z.string(), minimumAge: z.number(), venueCode: z.string(), venueName: z.string(), artists: z.array(ArtistSchema).default([]) }).passthrough()
export const TicketSchema = z.object({ id: z.number().optional(), ticketCode: z.string(), type: TicketTypeSchema, price: z.union([z.number(), z.string()]), status: TicketStatusSchema, purchaseDate: z.string().nullable().optional(), userEmail: z.string(), eventCode: z.string(), eventName: z.string() }).passthrough()
export const UserSchema = z.object({ id: z.number().optional(), username: z.string(), email: z.string(), active: z.boolean(), firstName: z.string().optional(), lastName: z.string().optional(), phone: z.string().nullable().optional(), city: z.string().nullable().optional(), birthDate: dateLike }).passthrough()
export const AuthUserSchema = z.preprocess((value: unknown) => { if (typeof value !== 'object' || value === null || !('roles' in value)) return value; const source = value as { roles: unknown }; return { ...value, roles: Array.isArray(source.roles) ? source.roles.map((role) => typeof role === 'string' ? role.replace(/^ROLE_/, '') : role) : source.roles } }, z.object({ id: z.number().optional(), username: z.string(), email: z.string().email(), roles: z.array(RoleSchema) }).passthrough())
export const AuthResponseSchema = z.object({ accessToken: z.string(), tokenType: z.string(), expiresIn: z.number() })
export const ErrorResponseSchema = z.object({ timestamp: z.string().optional(), status: z.number().optional(), error: z.string().optional(), message: z.string().optional(), details: z.record(z.string(), z.string()).optional() }).passthrough()
export type Artist = z.infer<typeof ArtistSchema>
export type Venue = z.infer<typeof VenueSchema>
export type EventSummary = z.infer<typeof EventSummarySchema>
export type Event = z.infer<typeof EventSchema>
export type Ticket = z.infer<typeof TicketSchema>
export type User = z.infer<typeof UserSchema>
export type AuthUser = z.infer<typeof AuthUserSchema>
export type EventCategory = z.infer<typeof EventCategorySchema>
export type TicketType = z.infer<typeof TicketTypeSchema>
export type RoleName = z.infer<typeof RoleSchema>
