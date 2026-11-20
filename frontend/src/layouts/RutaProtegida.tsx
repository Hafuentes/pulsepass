import { Navigate, Outlet, useLocation } from 'react-router'
import { useAuth } from '../auth/AuthContext'
import { RUTAS } from '../lib/rutas'

/** Solo experiencia de usuario: la seguridad real la aplica el servidor (FE-004). */
export default function RutaProtegida() {
  const { sesion } = useAuth()
  const location = useLocation()
  if (sesion.estado === 'cargando') return <div className="py-20 text-center text-slate-400" role="status">Verificando sesión…</div>
  if (sesion.estado === 'anonimo') {
    const state = { desde: `${location.pathname}${location.search}`, ...(sesion.motivo === 'expirada' ? { motivo: 'expirada' } : {}) }
    return <Navigate to={RUTAS.login} replace state={state} />
  }
  return <Outlet />
}
