import { Navigate, Outlet, useLocation } from 'react-router'
import { useAuth } from '../auth/AuthContext'
import { destinoSeguro } from '../lib/rutas'

/** Login/registro: con sesión activa se vuelve a la ruta de origen o al inicio. */
export default function SoloAnonimo() {
  const { sesion } = useAuth()
  const location = useLocation()
  if (sesion.estado === 'cargando') return <div className="py-20 text-center text-slate-400" role="status">Verificando sesión…</div>
  if (sesion.estado === 'autenticado') {
    const desde = (location.state as { desde?: string } | null)?.desde
    return <Navigate to={destinoSeguro(desde)} replace />
  }
  return <Outlet />
}
