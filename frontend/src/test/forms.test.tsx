import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router'
import { describe, expect, it, vi } from 'vitest'
import { AuthProvider } from '../auth/AuthContext'
import Login from '../pages/Login'
import Registro from '../pages/Registro'

function renderForm(path: '/login' | '/registro') {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } })
  return render(
    <QueryClientProvider client={queryClient}>
      <AuthProvider>
        <MemoryRouter initialEntries={[path]}>
          <Routes>
            <Route path="/login" element={<Login />} />
            <Route path="/registro" element={<Registro />} />
          </Routes>
        </MemoryRouter>
      </AuthProvider>
    </QueryClientProvider>,
  )
}

describe('validación de formularios antes de llamar a la API', () => {
  it('login vacío muestra errores de campos y no hace peticiones', async () => {
    const fetchMock = vi.fn()
    vi.stubGlobal('fetch', fetchMock)
    const user = userEvent.setup()
    renderForm('/login')

    await user.click(screen.getByRole('button', { name: 'Ingresar' }))

    expect(await screen.findByText('Ingresa tu usuario o correo')).toBeInTheDocument()
    expect(screen.getByText('Ingresa tu contraseña')).toBeInTheDocument()
    expect(fetchMock).not.toHaveBeenCalled()
  })

  it('registro con contraseña de menos de 12 caracteres no se envía', async () => {
    const fetchMock = vi.fn()
    vi.stubGlobal('fetch', fetchMock)
    const user = userEvent.setup()
    renderForm('/registro')

    await user.type(screen.getByLabelText('Nombre de usuario'), 'andrea')
    await user.type(screen.getByLabelText('Correo electrónico'), 'andrea@example.com')
    await user.type(screen.getByLabelText('Nombres'), 'Andrea')
    await user.type(screen.getByLabelText('Apellidos'), 'Ruiz')
    fireEvent.change(screen.getByLabelText('Fecha de nacimiento'), { target: { value: '1998-01-01' } })
    await user.type(screen.getByLabelText('Contraseña'), '12345678901')
    await user.click(screen.getByRole('button', { name: 'Crear cuenta' }))

    expect(await screen.findByText('La contraseña debe tener mínimo 12 caracteres')).toBeInTheDocument()
    await waitFor(() => expect(fetchMock).not.toHaveBeenCalled())
  })
})
