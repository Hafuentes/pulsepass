import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { error, mockApi } from './mockApi'
import { evento, ticket, usuarioAuth } from './fixtures'
import { renderApp, sesionPrevia } from './utils'

const usuario = { 'GET /api/auth/me': { body: usuarioAuth(['USER']) } }

describe('compra de tickets', () => {
  it('compra válida envía el email de la sesión, recibe 201 y navega a la confirmación', async () => {
    const api = mockApi({
      ...usuario,
      'GET /api/events/CMF-2026': { body: evento() },
      'POST /api/tickets': { status: 201, body: ticket() },
      'GET /api/tickets/TKT-8F2A1C': { body: ticket() },
    })
    sesionPrevia()
    const { router } = renderApp('/eventos/CMF-2026')
    const user = userEvent.setup()
    expect(await screen.findByRole('heading', { name: 'Caribbean Music Fest 2026' })).toBeInTheDocument()
    await user.click((await screen.findAllByRole('button', { name: 'Continuar' }))[0]!)
    const dialogo = await screen.findByRole('dialog', { name: 'Confirmar compra' })
    await user.click(within(dialogo).getByRole('button', { name: 'Confirmar entrada' }))
    expect(await screen.findByRole('heading', { name: '¡Tu entrada está lista!' })).toBeInTheDocument()
    expect(router.state.location.pathname).toBe('/compra/confirmacion/TKT-8F2A1C')
    const post = api.calls.find((c) => c.method === 'POST' && c.path === '/api/tickets')
    expect(post?.body).toEqual({ userEmail: 'andrea@pulsepass.test', eventCode: 'CMF-2026', type: 'GENERAL' })
    expect(post?.headers.get('Authorization')).toBe('Bearer jwt-de-prueba')
  })

  it('un 409 de regla de negocio se muestra dentro del modal y no navega', async () => {
    mockApi({
      ...usuario,
      'GET /api/events/CMF-2026': { body: evento() },
      'POST /api/tickets': error(409, 'El usuario no cumple la edad mínima del evento.'),
    })
    sesionPrevia()
    const { router } = renderApp('/eventos/CMF-2026')
    const user = userEvent.setup()
    await user.click((await screen.findAllByRole('button', { name: 'Continuar' }))[0]!)
    const dialogo = await screen.findByRole('dialog', { name: 'Confirmar compra' })
    await user.click(within(dialogo).getByRole('button', { name: 'Confirmar entrada' }))
    expect(await within(dialogo).findByText('El usuario no cumple la edad mínima del evento.')).toBeInTheDocument()
    expect(screen.getByRole('dialog', { name: 'Confirmar compra' })).toBeInTheDocument()
    expect(router.state.location.pathname).toBe('/eventos/CMF-2026')
  })

  it('un visitante anónimo que pulsa comprar va a /login y conserva la ruta de origen', async () => {
    mockApi({ 'GET /api/events/CMF-2026': { body: evento() } })
    const { router } = renderApp('/eventos/CMF-2026')
    const user = userEvent.setup()
    await user.click(await screen.findByRole('button', { name: 'Iniciar sesión para comprar' }))
    expect(router.state.location.pathname).toBe('/login')
    expect((router.state.location.state as { desde?: string }).desde).toBe('/eventos/CMF-2026')
  })

  it('un evento agotado no permite iniciar la compra', async () => {
    mockApi({ ...usuario, 'GET /api/events/CMF-2026': { body: evento({ status: 'SOLD_OUT' }) } })
    sesionPrevia()
    renderApp('/eventos/CMF-2026')
    expect(await screen.findByText('Las entradas para este evento se agotaron.')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Continuar' })).not.toBeInTheDocument()
  })

  it('STAFF/ADMIN no ven el panel de compra (la API solo autoriza compras a USER)', async () => {
    mockApi({ 'GET /api/auth/me': { body: usuarioAuth(['STAFF'], { username: 'laura' }) }, 'GET /api/events/CMF-2026': { body: evento() } })
    sesionPrevia()
    renderApp('/eventos/CMF-2026')
    expect(await screen.findByRole('heading', { name: 'Caribbean Music Fest 2026' })).toBeInTheDocument()
    await screen.findByRole('link', { name: 'laura' })
    expect(screen.queryByRole('heading', { name: 'Compra tu entrada' })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Continuar' })).not.toBeInTheDocument()
  })
})

describe('mis tickets', () => {
  const lista = [
    ticket(),
    ticket({ ticketCode: 'TKT-5C90E2', status: 'USED', eventName: 'Noche de Jazz y Cultura', type: 'GENERAL', price: 90000 }),
    ticket({ ticketCode: 'TKT-77AD13', status: 'CANCELLED', eventName: 'Stand-up Costeño', type: 'STUDENT', price: 60000 }),
  ]

  it('lista los tickets del email de la sesión y filtra por estado en el cliente', async () => {
    const api = mockApi({ ...usuario, 'GET /api/tickets/by-user': { body: lista } })
    sesionPrevia()
    renderApp('/mis-tickets')
    const user = userEvent.setup()
    expect(await screen.findByText('Noche de Jazz y Cultura')).toBeInTheDocument()
    expect(api.calls.find((c) => c.path === '/api/tickets/by-user')?.url.searchParams.get('email')).toBe('andrea@pulsepass.test')
    await user.click(screen.getByRole('button', { name: 'Usados' }))
    expect(screen.getByText('Noche de Jazz y Cultura')).toBeInTheDocument()
    expect(screen.queryByText('Stand-up Costeño')).not.toBeInTheDocument()
  })

  it('el botón Cancelar solo aparece en tickets PAID', async () => {
    mockApi({ ...usuario, 'GET /api/tickets/by-user': { body: lista } })
    sesionPrevia()
    renderApp('/mis-tickets')
    await screen.findByText('Noche de Jazz y Cultura')
    expect(screen.getAllByRole('button', { name: 'Cancelar' })).toHaveLength(1)
  })

  it('sin tickets muestra el estado vacío', async () => {
    mockApi({ ...usuario, 'GET /api/tickets/by-user': { body: [] } })
    sesionPrevia()
    renderApp('/mis-tickets')
    expect(await screen.findByText('No hay tickets en esta categoría')).toBeInTheDocument()
  })

  it('cancelar un ticket PAID pide confirmación, llama a PATCH y actualiza la lista', async () => {
    let cancelado = false
    const api = mockApi({
      ...usuario,
      'GET /api/tickets/by-user': () => ({ body: [ticket({ status: cancelado ? 'CANCELLED' : 'PAID' })] }),
      'PATCH /api/tickets/TKT-8F2A1C/cancel': () => { cancelado = true; return { body: ticket({ status: 'CANCELLED' }) } },
    })
    sesionPrevia()
    renderApp('/mis-tickets')
    const user = userEvent.setup()
    await user.click(await screen.findByRole('button', { name: 'Cancelar' }))
    const dialogo = await screen.findByRole('dialog', { name: 'Cancelar entrada' })
    expect(api.count('PATCH', '/api/tickets/TKT-8F2A1C/cancel')).toBe(0)
    await user.click(within(dialogo).getByRole('button', { name: 'Confirmar cancelación' }))
    await waitFor(() => expect(api.count('PATCH', '/api/tickets/TKT-8F2A1C/cancel')).toBe(1))
    await waitFor(() => expect(screen.queryByRole('button', { name: 'Cancelar' })).not.toBeInTheDocument())
    expect(await screen.findByText('Cancelado')).toBeInTheDocument()
  })

  it('un 409 al cancelar muestra el mensaje y mantiene el modal abierto', async () => {
    mockApi({
      ...usuario,
      'GET /api/tickets/by-user': { body: [ticket()] },
      'PATCH /api/tickets/TKT-8F2A1C/cancel': error(409, 'El ticket no puede cancelarse en su estado actual.'),
    })
    sesionPrevia()
    renderApp('/mis-tickets')
    const user = userEvent.setup()
    await user.click(await screen.findByRole('button', { name: 'Cancelar' }))
    const dialogo = await screen.findByRole('dialog', { name: 'Cancelar entrada' })
    await user.click(within(dialogo).getByRole('button', { name: 'Confirmar cancelación' }))
    expect(await within(dialogo).findByText('El ticket no puede cancelarse en su estado actual.')).toBeInTheDocument()
    expect(screen.getByRole('dialog', { name: 'Cancelar entrada' })).toBeInTheDocument()
  })

  it('un ticket ajeno (403/404) muestra «Ticket no disponible» sin datos', async () => {
    mockApi({ ...usuario, 'GET /api/tickets/TKT-AJENO': error(403, 'Acceso denegado') })
    sesionPrevia()
    renderApp('/mis-tickets/TKT-AJENO')
    expect(await screen.findByRole('heading', { name: 'Ticket no disponible' })).toBeInTheDocument()
  })

  it('el detalle de un ticket USED no ofrece cancelar', async () => {
    mockApi({ ...usuario, 'GET /api/tickets/TKT-5C90E2': { body: ticket({ ticketCode: 'TKT-5C90E2', status: 'USED' }) } })
    sesionPrevia()
    renderApp('/mis-tickets/TKT-5C90E2')
    await screen.findByText('Tu entrada PulsePass')
    expect(screen.queryByRole('button', { name: 'Cancelar entrada' })).not.toBeInTheDocument()
  })

  it('la confirmación de un ticket ajeno muestra «Ticket no disponible»', async () => {
    mockApi({ ...usuario, 'GET /api/tickets/TKT-AJENO': error(404, 'No encontrado') })
    sesionPrevia()
    renderApp('/compra/confirmacion/TKT-AJENO')
    expect(await screen.findByRole('heading', { name: 'Ticket no disponible' })).toBeInTheDocument()
  })
})
