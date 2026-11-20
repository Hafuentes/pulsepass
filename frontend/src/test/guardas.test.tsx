import { screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { error, mockApi } from './mockApi'
import { evento, resumen, usuarioAuth } from './fixtures'
import { renderApp, sesionPrevia } from './utils'

describe('guardas de ruta y navegación por rol', () => {
  it('anónimo en una ruta protegida va a /login guardando la ruta de origen', async () => {
    mockApi({})
    const { router } = renderApp('/cuenta')
    expect(await screen.findByRole('heading', { name: 'Iniciar sesión' })).toBeInTheDocument()
    expect(router.state.location.pathname).toBe('/login')
    expect((router.state.location.state as { desde?: string }).desde).toBe('/cuenta')
  })

  it('USER que abre /admin/eventos es enviado a /403', async () => {
    mockApi({ 'GET /api/auth/me': { body: usuarioAuth(['USER']) } })
    sesionPrevia()
    const { router } = renderApp('/admin/eventos')
    expect(await screen.findByRole('heading', { name: 'Esta zona no es para todos.' })).toBeInTheDocument()
    expect(router.state.location.pathname).toBe('/403')
  })

  it('STAFF no accede a la administración, pero sí a la validación de ingreso', async () => {
    mockApi({ 'GET /api/auth/me': { body: usuarioAuth(['STAFF']) } })
    sesionPrevia()
    const admin = renderApp('/admin/eventos')
    expect(await screen.findByRole('heading', { name: 'Esta zona no es para todos.' })).toBeInTheDocument()
    expect(admin.router.state.location.pathname).toBe('/403')
    admin.unmount()
    renderApp('/staff/validacion')
    expect(await screen.findByRole('heading', { name: 'Validar ingreso' })).toBeInTheDocument()
  })

  it('ADMIN accede a /admin/eventos y ve el listado administrativo', async () => {
    mockApi({
      'GET /api/auth/me': { body: usuarioAuth(['ADMIN'], { username: 'admin' }) },
      'GET /api/events/admin': { body: [resumen({ status: 'DRAFT', name: 'Evento en borrador' })] },
    })
    sesionPrevia()
    renderApp('/admin/eventos')
    expect(await screen.findByRole('heading', { name: 'Gestionar eventos' })).toBeInTheDocument()
    expect(await screen.findByText('Evento en borrador')).toBeInTheDocument()
  })

  it('un token vencido al restaurar la sesión deja la sesión anónima y avisa que expiró', async () => {
    const api = mockApi({ 'GET /api/auth/me': { body: usuarioAuth(['USER']) } })
    sesionPrevia(-1)
    renderApp('/cuenta')
    expect(await screen.findByText('Tu sesión expiró. Inicia sesión nuevamente.')).toBeInTheDocument()
    expect(api.count('GET', '/api/auth/me')).toBe(0)
    expect(sessionStorage.getItem('pulsepass.session')).toBeNull()
  })

  it('un 401 del servidor en una ruta protegida cierra la sesión y redirige a /login', async () => {
    mockApi({
      'GET /api/auth/me': { body: usuarioAuth(['USER']) },
      'GET /api/tickets/by-user': error(401, 'Token expirado'),
    })
    sesionPrevia()
    const { router } = renderApp('/mis-tickets')
    expect(await screen.findByText('Tu sesión expiró. Inicia sesión nuevamente.')).toBeInTheDocument()
    expect(router.state.location.pathname).toBe('/login')
    expect(sessionStorage.getItem('pulsepass.session')).toBeNull()
  })

  it('el navbar muestra enlaces según el rol', async () => {
    mockApi({ 'GET /api/auth/me': { body: usuarioAuth(['ADMIN'], { username: 'admin' }) }, 'GET /api/events/published': { body: [] }, 'GET /api/artists/active': { body: [] } })
    sesionPrevia()
    renderApp('/')
    expect(await screen.findByRole('link', { name: 'Administración' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Validar ingreso' })).toBeInTheDocument()
    expect(screen.queryByRole('link', { name: 'Ingresar' })).not.toBeInTheDocument()
  })

  it('el navbar de un USER no incluye administración ni validación', async () => {
    mockApi({ 'GET /api/auth/me': { body: usuarioAuth(['USER']) }, 'GET /api/events/published': { body: [] }, 'GET /api/artists/active': { body: [] } })
    sesionPrevia()
    renderApp('/')
    expect(await screen.findByRole('link', { name: 'andrea' })).toBeInTheDocument()
    expect(screen.queryByRole('link', { name: 'Administración' })).not.toBeInTheDocument()
    expect(screen.queryByRole('link', { name: 'Validar ingreso' })).not.toBeInTheDocument()
  })

  it('una ruta inexistente muestra la página 404', async () => {
    mockApi({})
    renderApp('/ruta-que-no-existe')
    expect(await screen.findByRole('heading', { name: 'Parece que tomaste otra ruta.' })).toBeInTheDocument()
  })

  it('un evento inexistente o no publicado muestra «Evento no disponible» sin datos', async () => {
    mockApi({ 'GET /api/events/BORRADOR-1': error(404, 'Evento no encontrado') })
    renderApp('/eventos/BORRADOR-1')
    expect(await screen.findByRole('heading', { name: 'Evento no disponible' })).toBeInTheDocument()
  })

  it('un 403 sobre un borrador también se presenta como «Evento no disponible» (GAP-02)', async () => {
    mockApi({ 'GET /api/events/BORRADOR-1': error(403, 'Acceso denegado'), 'GET /api/events/CMF-2026': { body: evento() } })
    renderApp('/eventos/BORRADOR-1')
    expect(await screen.findByRole('heading', { name: 'Evento no disponible' })).toBeInTheDocument()
  })
})
