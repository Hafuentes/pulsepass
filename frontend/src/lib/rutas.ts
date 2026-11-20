export const RUTAS = {
  inicio: '/', eventos: '/eventos', evento: (code: string) => `/eventos/${encodeURIComponent(code)}`,
  artistas: '/artistas', artista: (id: number | string) => `/artistas/${id}`,
  sedes: '/sedes', sede: (code: string) => `/sedes/${encodeURIComponent(code)}`,
  login: '/login', registro: '/registro', cuenta: '/cuenta', misTickets: '/mis-tickets',
  ticket: (code: string) => `/mis-tickets/${encodeURIComponent(code)}`,
  confirmacion: (code: string) => `/compra/confirmacion/${encodeURIComponent(code)}`,
  validacion: '/staff/validacion', ticketsEvento: (code: string) => `/staff/eventos/${encodeURIComponent(code)}/tickets`,
  adminEventos: '/admin/eventos', adminCatalogos: '/admin/catalogos', adminNuevo: '/admin/eventos/nuevo', adminEvento: (code: string) => `/admin/eventos/${encodeURIComponent(code)}`,
  sinPermisos: '/403',
} as const

/** Acepta solo rutas internas (evita redirecciones abiertas) y usa el inicio por defecto. */
export const destinoSeguro = (destino?: string | null): string => destino && destino.startsWith('/') && !destino.startsWith('//') ? destino : RUTAS.inicio
