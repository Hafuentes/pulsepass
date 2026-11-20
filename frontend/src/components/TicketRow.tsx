import { Link } from 'react-router'
import { moneda } from '../lib/formato'
import { RUTAS } from '../lib/rutas'
import { type Ticket } from '../lib/schemas'
import { Badge, Button } from '../components/UI'
import { statusTone, statusNames, ticketTypeNames } from '../lib/dominio'

export function TicketRow({ ticket, onCancel }: { ticket: Ticket; onCancel?: (ticket: Ticket) => void }) { return <div className="surface flex flex-col gap-4 rounded-2xl p-5 sm:flex-row sm:items-center sm:justify-between"><div className="min-w-0"><div className="mb-2 flex flex-wrap items-center gap-2"><Badge tone={statusTone(ticket.status)}>{statusNames[ticket.status] ?? ticket.status}</Badge><span className="text-xs text-slate-500">{ticket.ticketCode}</span></div><Link to={RUTAS.ticket(ticket.ticketCode)} className="font-display text-lg font-bold hover:text-violet-700 dark:hover:text-violet-200">{ticket.eventName}</Link><p className="mt-1 text-sm text-slate-500">{ticketTypeNames[ticket.type] ?? ticket.type} · {moneda(ticket.price)}</p></div><div className="flex items-center gap-2"><Link to={RUTAS.ticket(ticket.ticketCode)} className="btn-secondary text-sm">Ver entrada</Link>{onCancel && ticket.status === 'PAID' && <Button variant="danger" onClick={() => onCancel(ticket)}>Cancelar</Button>}</div></div> }
