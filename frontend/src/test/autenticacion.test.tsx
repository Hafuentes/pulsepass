import { fireEvent, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { error, mockApi } from './mockApi'
import { login, usuarioAuth } from './fixtures'
import { renderApp, sesionPrevia } from './utils'

async function completarLogin(identificador = 'andrea', clave = 'ClaveSegura-123') {
  const user = userEvent.setup()
  await user.type(await screen.findByLabelText('Usuario o correo'), identificador)
  await user.type(screen.getByLabelText('Contraseña'), clave)
  await user.click(screen.getByRole('button', { name: 'Ingresar' }))
}

describe('login, registro y sesión', () => {
  it('login válido guarda el token solo en sessionStorage, consulta /me con Bearer y vuelve a la ruta de origen', async () => {
    const api = mockApi({
      'POST /api/auth/login': { body: login },
      'GET /api/auth/me': { body: usuarioAuth(['USER']) },
      'GET /api/events/published': { body: [] },
      'GET /api/artists/active': { body: [] },
    })
    const { router } = renderApp({ pathname: '/login', state: { desde: '/eventos' } })
    await completarLogin()
    await waitFor(() => expect(router.state.location.pathname).toBe('/eventos'))
    const me = api.calls.find((c) => c.path === '/api/auth/me')
    expect(me?.headers.get('Authorization')).toBe('Bearer jwt-de-prueba')
    expect(api.calls.find((c) => c.path === '/api/auth/login')?.headers.get('Authorization')).toBeNull()
    expect(JSON.parse(sessionStorage.getItem('pulsepass.session') ?? '{}')).toMatchObject({ token: 'jwt-de-prueba' })
    expect(localStorage.getItem('pulsepass.session')).toBeNull()
    expect(api.calls.every((c) => !c.url.search.includes('jwt-de-prueba'))).toBe(true)
  })

  it('login sin ruta de origen vuelve al inicio', async () => {
    mockApi({
      'POST /api/auth/login': { body: login },
      'GET /api/auth/me': { body: usuarioAuth(['USER']) },
      'GET /api/events/published': { body: [] },
      'GET /api/artists/active': { body: [] },
    })
    const { router } = renderApp('/login')
    await completarLogin()
    await waitFor(() => expect(router.state.location.pathname).toBe('/'))
  })

  it('credenciales inválidas (401) muestran un mensaje genérico y no guardan sesión', async () => {
    mockApi({ 'POST /api/auth/login': error(401, 'Bad credentials') })
    renderApp('/login')
    await completarLogin('nadie', 'ClaveIncorrecta-1')
    expect(await screen.findByText('Credenciales inválidas. Verifica tus datos e inténtalo de nuevo.')).toBeInTheDocument()
    expect(sessionStorage.getItem('pulsepass.session')).toBeNull()
  })

  it('con sesión activa, /login redirige fuera del formulario', async () => {
    mockApi({ 'GET /api/auth/me': { body: usuarioAuth(['USER']) }, 'GET /api/events/published': { body: [] }, 'GET /api/artists/active': { body: [] } })
    sesionPrevia()
    const { router } = renderApp('/login')
    await waitFor(() => expect(router.state.location.pathname).toBe('/'))
  })

  it('cerrar sesión elimina el token, limpia la caché y vuelve al inicio', async () => {
    mockApi({ 'GET /api/auth/me': { body: usuarioAuth(['USER']) }, 'GET /api/events/published': { body: [] }, 'GET /api/artists/active': { body: [] } })
    sesionPrevia()
    const { router, queryClient } = renderApp('/')
    queryClient.setQueryData(['tickets', 'usuario', 'andrea@pulsepass.test'], [])
    const user = userEvent.setup()
    await user.click(await screen.findByRole('button', { name: 'Salir' }))
    await waitFor(() => expect(sessionStorage.getItem('pulsepass.session')).toBeNull())
    expect(queryClient.getQueryData(['tickets', 'usuario', 'andrea@pulsepass.test'])).toBeUndefined()
    expect(router.state.location.pathname).toBe('/')
    expect(await screen.findByRole('link', { name: 'Ingresar' })).toBeInTheDocument()
  })

  async function completarRegistro() {
    const user = userEvent.setup()
    await user.type(await screen.findByLabelText('Nombre de usuario'), 'andrea')
    await user.type(screen.getByLabelText('Correo electrónico'), 'andrea@pulsepass.test')
    await user.type(screen.getByLabelText('Nombres'), 'Andrea')
    await user.type(screen.getByLabelText('Apellidos'), 'Ruiz')
    fireEvent.change(screen.getByLabelText('Fecha de nacimiento'), { target: { value: '1998-04-12' } })
    await user.type(screen.getByLabelText('Contraseña'), 'ClaveSegura-123')
    await user.click(screen.getByRole('button', { name: 'Crear cuenta' }))
  }

  it('registro válido (201) redirige a /login con aviso de éxito', async () => {
    const api = mockApi({ 'POST /api/users': { status: 201, body: { id: 5, username: 'andrea', email: 'andrea@pulsepass.test', active: true } } })
    const { router } = renderApp('/registro')
    await completarRegistro()
    expect(await screen.findByText('Cuenta creada. Ahora inicia sesión.')).toBeInTheDocument()
    expect(router.state.location.pathname).toBe('/login')
    expect(api.count('POST', '/api/users')).toBe(1)
    expect(JSON.stringify(api.calls.find((c) => c.method === 'POST')?.body)).not.toContain('roles')
  })

  it('registro duplicado (409) muestra un mensaje genérico sin detalles internos', async () => {
    mockApi({ 'POST /api/users': error(409, 'duplicate key value violates unique constraint uk_user_email') })
    renderApp('/registro')
    await completarRegistro()
    expect(await screen.findByText('El usuario o correo ya está registrado.')).toBeInTheDocument()
    expect(screen.queryByText(/duplicate key/)).not.toBeInTheDocument()
  })

  it('registro con 400 asigna details al campo homónimo', async () => {
    mockApi({ 'POST /api/users': error(400, 'Datos inválidos', { email: 'El correo no es válido para el servidor' }) })
    renderApp('/registro')
    await completarRegistro()
    expect(await screen.findByText('El correo no es válido para el servidor')).toBeInTheDocument()
  })
})
