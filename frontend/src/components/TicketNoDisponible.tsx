import { Link } from 'react-router'
import { RUTAS } from '../lib/rutas'

/** Pantalla neutra para 403/404 en tickets: no revela si el ticket existe (PRD 9.12). */
export function TicketNoDisponible() {
  return <div className="surface mx-auto max-w-xl rounded-2xl p-8 text-center" role="alert"><h1 className="font-display text-2xl font-bold">Ticket no disponible</h1><p className="mt-2 text-sm text-slate-500">No encontramos esta entrada o no tienes permiso para consultarla.</p><Link to={RUTAS.misTickets} className="btn-primary mt-5">Volver a mis tickets</Link></div>
}
