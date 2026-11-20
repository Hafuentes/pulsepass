type Datos = Record<string, unknown>

export const usuarioAuth = (roles: string[], over: Datos = {}) => ({ id: 1, username: 'andrea', email: 'andrea@pulsepass.test', roles, ...over })
export const resumen = (over: Datos = {}) => ({ id: 1, eventCode: 'CMF-2026', name: 'Caribbean Music Fest 2026', category: 'MUSIC', status: 'PUBLISHED', eventDate: '2026-12-12T20:00:00', venueCode: 'VEN-SMR-01', venueName: 'Marina Convention Center', venueCity: 'Santa Marta', ...over })
export const evento = (over: Datos = {}) => ({ ...resumen(), description: 'Tres noches de música caribeña.', minimumAge: 18, artists: [{ id: 1, stageName: 'Solar Beat', active: true }], ...over })
export const sede = (over: Datos = {}) => ({ id: 1, code: 'VEN-SMR-01', name: 'Marina Convention Center', city: 'Santa Marta', capacity: 3, active: true, ...over })
export const ticket = (over: Datos = {}) => ({ id: 1, ticketCode: 'TKT-8F2A1C', type: 'VIP', price: 250000, status: 'PAID', purchaseDate: '2026-11-05T14:32:00', userEmail: 'andrea@pulsepass.test', eventCode: 'CMF-2026', eventName: 'Caribbean Music Fest 2026', ...over })
export const login = { accessToken: 'jwt-de-prueba', tokenType: 'Bearer', expiresIn: 900 }
