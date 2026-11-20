import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { error, mockApi } from './mockApi'
import { evento, sede, ticket, usuarioAuth } from './fixtures'
import { renderApp, sesionPrevia } from './utils'

const staff = { 'GET /api/auth/me': { body: usuarioAuth(['STAFF'], { username: 'laura', email: 'laura@pulsepass.test' }) } }
const admin = { 'GET /api/auth/me': { body: usuarioAuth(['ADMIN'], { username: 'admin', email: 'admin@pulsepass.local' }) } }

async function buscarTicket(codigo = 'TKT-8F2A1C') {
  const user = userEvent.setup()
  await user.type(await screen.findByLabelText('Código del ticket'), codigo)
  await user.click(screen.getByRole('button', { name: 'Buscar entrada' }))
  return user
}

describe('STAFF: validación de ingreso', () => {
  it('busca un ticket PAID, lo marca como usado y muestra el éxito', async () => {
    let usado = false
    const api = mockApi({
      ...staff,
      'GET /api/tickets/TKT-8F2A1C': () => ({ body: ticket({ status: usado ? 'USED' : 'PAID' }) }),
      'PATCH /api/tickets/TKT-8F2A1C/use': () => { usado = true; return { body: ticket({ status: 'USED' }) } },
    })
    sesionPrevia()
    renderApp('/staff/validacion')
    const user = await buscarTicket()
    await user.click(await screen.findByRole('button', { name: 'Marcar como utilizada' }))
    expect(await screen.findByText('Ingreso registrado correctamente.')).toBeInTheDocument()
    expect(api.count('PATCH', '/api/tickets/TKT-8F2A1C/use')).toBe(1)
    expect(await screen.findByText('Usado')).toBeInTheDocument()
  })

  it('un segundo intento (409) muestra el mensaje del backend', async () => {
    mockApi({
      ...staff,
      'GET /api/tickets/TKT-8F2A1C': { body: ticket() },
      'PATCH /api/tickets/TKT-8F2A1C/use': error(409, 'El ticket ya fue utilizado.'),
    })
    sesionPrevia()
    renderApp('/staff/validacion')
    const user = await buscarTicket()
    await user.click(await screen.findByRole('button', { name: 'Marcar como utilizada' }))
    expect(await screen.findByText('El ticket ya fue utilizado.')).toBeInTheDocument()
  })

  it('un ticket inexistente muestra «Ticket no encontrado»', async () => {
    mockApi({ ...staff, 'GET /api/tickets/TKT-NOEXISTE': error(404, 'Ticket no encontrado') })
    sesionPrevia()
    renderApp('/staff/validacion')
    await buscarTicket('TKT-NOEXISTE')
    expect(await screen.findByText('Ticket no encontrado.')).toBeInTheDocument()
  })

  it('un código vacío no consulta la API', async () => {
    const api = mockApi({ ...staff })
    sesionPrevia()
    renderApp('/staff/validacion')
    const user = userEvent.setup()
    await user.click(await screen.findByRole('button', { name: 'Buscar entrada' }))
    expect(await screen.findByText('Ingresa el código de la entrada')).toBeInTheDocument()
    expect(api.calls.filter((c) => c.path.startsWith('/api/tickets'))).toHaveLength(0)
  })

  it('lista los tickets pagados de un evento', async () => {
    mockApi({
      ...staff,
      'GET /api/events/CMF-2026/tickets/paid': { body: [ticket(), ticket({ ticketCode: 'TKT-91B07D', type: 'GENERAL', price: 120000, userEmail: 'carlos@pulsepass.test' })] },
    })
    sesionPrevia()
    renderApp('/staff/eventos/CMF-2026/tickets')
    expect(await screen.findByRole('heading', { name: 'Tickets pagados' })).toBeInTheDocument()
    expect((await screen.findAllByText('carlos@pulsepass.test')).length).toBeGreaterThan(0)
  })

  it('sin tickets pagados muestra el estado vacío', async () => {
    mockApi({ ...staff, 'GET /api/events/CMF-2026/tickets/paid': { body: [] } })
    sesionPrevia()
    renderApp('/staff/eventos/CMF-2026/tickets')
    expect(await screen.findByText('No hay tickets pagados')).toBeInTheDocument()
  })
})

describe('ADMIN: eventos', () => {
  it('crear evento con campos vacíos muestra errores por campo y no llama a la API', async () => {
    const api = mockApi({ ...admin, 'GET /api/venues/active': { body: [sede()] } })
    sesionPrevia()
    renderApp('/admin/eventos/nuevo')
    const user = userEvent.setup()
    await screen.findByRole('option', { name: /Marina Convention Center/ })
    await user.click(screen.getByRole('button', { name: 'Crear borrador' }))
    expect(await screen.findByText('Código obligatorio')).toBeInTheDocument()
    expect(screen.getByText('Nombre obligatorio')).toBeInTheDocument()
    expect(screen.getByText('Fecha obligatoria')).toBeInTheDocument()
    expect(api.count('POST', '/api/events')).toBe(0)
  })

  it('publicar un evento DRAFT pide confirmación y actualiza el estado', async () => {
    let publicado = false
    const api = mockApi({
      ...admin,
      'GET /api/events/CMF-2026': () => ({ body: evento({ status: publicado ? 'PUBLISHED' : 'DRAFT' }) }),
      'GET /api/artists/active': { body: [] },
      'PATCH /api/events/CMF-2026/publish': () => { publicado = true; return { body: evento({ status: 'PUBLISHED' }) } },
    })
    sesionPrevia()
    renderApp('/admin/eventos/CMF-2026')
    const user = userEvent.setup()
    await user.click(await screen.findByRole('button', { name: 'Publicar evento' }))
    const dialogo = await screen.findByRole('dialog', { name: 'Publicar evento' })
    expect(api.count('PATCH', '/api/events/CMF-2026/publish')).toBe(0)
    await user.click(within(dialogo).getByRole('button', { name: 'Confirmar publicación' }))
    expect(await screen.findByText('Publicado')).toBeInTheDocument()
    await waitFor(() => expect(screen.queryByRole('button', { name: 'Publicar evento' })).not.toBeInTheDocument())
  })

  it('un 409 al publicar se muestra al administrador', async () => {
    mockApi({
      ...admin,
      'GET /api/events/CMF-2026': { body: evento({ status: 'DRAFT' }) },
      'GET /api/artists/active': { body: [] },
      'PATCH /api/events/CMF-2026/publish': error(409, 'Transición de estado no permitida.'),
    })
    sesionPrevia()
    renderApp('/admin/eventos/CMF-2026')
    const user = userEvent.setup()
    await user.click(await screen.findByRole('button', { name: 'Publicar evento' }))
    const dialogo = await screen.findByRole('dialog', { name: 'Publicar evento' })
    await user.click(within(dialogo).getByRole('button', { name: 'Confirmar publicación' }))
    expect((await screen.findAllByText('Transición de estado no permitida.')).length).toBeGreaterThan(0)
  })

  it('asocia un artista activo y muestra el 409 si el backend rechaza un duplicado', async () => {
    let intento = 0
    const api = mockApi({
      ...admin,
      'GET /api/events/CMF-2026': { body: evento({ status: 'DRAFT' }) },
      'GET /api/artists/active': { body: [{ id: 1, stageName: 'Solar Beat', active: true }, { id: 2, stageName: 'Neon Waves', active: true }] },
      'POST /api/events/CMF-2026/artists/2': () => { intento += 1; return intento === 1 ? { body: evento({ status: 'DRAFT' }) } : error(409, 'El artista ya está asociado al evento.') },
    })
    sesionPrevia()
    renderApp('/admin/eventos/CMF-2026')
    const user = userEvent.setup()
    await screen.findByRole('option', { name: 'Neon Waves' })
    await user.selectOptions(screen.getByRole('combobox'), '2')
    await user.click(screen.getByRole('button', { name: 'Asociar artista' }))
    await waitFor(() => expect(api.count('POST', '/api/events/CMF-2026/artists/2')).toBe(1))
    await user.selectOptions(screen.getByRole('combobox'), '2')
    await user.click(screen.getByRole('button', { name: 'Asociar artista' }))
    expect(await screen.findByText('El artista ya está asociado al evento.')).toBeInTheDocument()
  })
})
