import { Navigate, Outlet, useLocation } from 'react-router'
import { useAuth } from '../auth/AuthContext'
import { RUTAS } from '../lib/rutas'
import type { RoleName } from '../lib/schemas'

export default function RutaPorRol({ roles }: { roles: RoleName[] }) {
  const { sesion } = useAuth()
  const location = useLocation()
  if (sesion.estado === 'cargando') return <div className="py-20 text-center text-slate-400" role="status">Verificando permisos…</div>
  if (sesion.estado === 'anonimo') return <Navigate to={RUTAS.login} replace state={{ desde: `${location.pathname}${location.search}` }} />
  if (!roles.some((rol) => sesion.usuario.roles.includes(rol))) return <Navigate to={RUTAS.sinPermisos} replace />
  return <Outlet />
}
