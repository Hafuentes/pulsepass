import { Link, isRouteErrorResponse, useRouteError } from 'react-router'
import { RUTAS } from '../lib/rutas'

/** errorElement de la ruta raíz: mensaje seguro, sin stack trace ni detalles internos (PRD 9.19). */
export default function ErrorPagina() {
  const error = useRouteError()
  const mensaje = isRouteErrorResponse(error) && error.status === 404
    ? 'No encontramos la página que buscas.'
    : 'Ocurrió un error inesperado. Inténtalo más tarde.'
  return <div className="mx-auto max-w-xl py-16 text-center" role="alert">
    <h1 className="page-title text-3xl">¡Ups! Algo salió mal.</h1>
    <p className="mt-3 text-slate-500">{mensaje}</p>
    <div className="mt-7 flex flex-wrap justify-center gap-3">
      <button type="button" className="btn-primary" onClick={() => window.location.reload()}>Reintentar</button>
      <Link to={RUTAS.inicio} className="btn-secondary">Volver al inicio</Link>
    </div>
  </div>
}
