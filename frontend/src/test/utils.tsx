import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render } from '@testing-library/react'
import { createMemoryRouter } from 'react-router'
import { RouterProvider } from 'react-router/dom'
import { AuthProvider } from '../auth/AuthContext'
import { routes } from '../router'

/** Simula una pestaña con token vigente (la sesión se restaura con GET /api/auth/me). */
export function sesionPrevia(minutos = 10, token = 'jwt-de-prueba') {
  sessionStorage.setItem('pulsepass.session', JSON.stringify({ token, expiraEn: Date.now() + minutos * 60_000 }))
}

/** Renderiza la aplicación real (rutas, guardas, layout) con Query + Auth + Router en memoria. */
export function renderApp(entrada: string | { pathname: string; state?: unknown }) {
  const router = createMemoryRouter(routes, { initialEntries: [entrada] })
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } })
  const view = render(
    <QueryClientProvider client={queryClient}>
      <AuthProvider>
        <RouterProvider router={router} />
      </AuthProvider>
    </QueryClientProvider>,
  )
  return { router, queryClient, ...view }
}
