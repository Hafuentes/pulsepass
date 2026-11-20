import { useState } from 'react'
import { useMyTickets, useCancelTicketMutation } from '../features/tickets/hooks'
import { type Ticket } from '../lib/schemas'
import { useAuth } from '../auth/AuthContext'
import { Button, EmptyState, ErrorState, Modal, SectionTitle, FormError } from '../components/UI'
import { ticketTypeNames } from '../lib/dominio'
import { TicketRow } from '../components/TicketRow'

export default function MisTickets() {
  const { sesion } = useAuth()
  const email = sesion.estado === 'autenticado' ? sesion.usuario.email : ''
  const q = useMyTickets(email)
  const [filter, setFilter] = useState('ALL')
  const [ticketToCancel, setTicketToCancel] = useState<Ticket | null>(null)
  const cancel = useCancelTicketMutation()
  const tickets = (q.data ?? []).filter((ticket) => filter === 'ALL' || ticket.status === filter)
  return <>
    <SectionTitle eyebrow="Tus experiencias" title="Mis tickets" description="Consulta tus entradas y gestiona tus próximas experiencias." />
    <div className="mb-5 flex flex-wrap gap-2" aria-label="Filtrar tickets">
      {([['ALL', 'Todos'], ['PAID', 'Pagados'], ['RESERVED', 'Reservados'], ['USED', 'Usados'], ['CANCELLED', 'Cancelados']] as Array<[string, string]>).map(([value, label]) => <button key={value} type="button" aria-pressed={filter === value} onClick={() => setFilter(value)} className={`min-h-11 rounded-full px-4 py-2 text-xs font-semibold ${filter === value ? 'bg-violet-500 text-white' : 'border border-slate-200 text-slate-600 hover:bg-slate-100'}`}>{label}</button>)}
    </div>
    {q.isPending ? <div className="space-y-3"><div className="surface h-28 animate-pulse rounded-2xl" /><div className="surface h-28 animate-pulse rounded-2xl" /></div> : q.isError ? <ErrorState error={q.error} retry={() => void q.refetch()} /> : tickets.length ? <div className="space-y-3">{tickets.map((ticket) => <TicketRow key={ticket.ticketCode} ticket={ticket} onCancel={setTicketToCancel} />)}</div> : <EmptyState title="No hay tickets en esta categoría" description="Cuando compres una entrada, aparecerá aquí." />}
    <Modal open={Boolean(ticketToCancel)} title="Cancelar entrada" description="Esta acción solicita al sistema cancelar tu ticket. El backend confirmará si todavía cumple las reglas para cancelarse." onClose={() => { if (!cancel.isPending) { setTicketToCancel(null); cancel.reset() } }} footer={<><Button type="button" variant="secondary" disabled={cancel.isPending} onClick={() => { setTicketToCancel(null); cancel.reset() }}>Conservar entrada</Button><Button type="button" variant="danger" disabled={cancel.isPending || !ticketToCancel} onClick={() => ticketToCancel && cancel.mutate(ticketToCancel.ticketCode, { onSuccess: () => setTicketToCancel(null) })}>{cancel.isPending ? 'Cancelando…' : 'Confirmar cancelación'}</Button></>}>{ticketToCancel && <div className="rounded-xl border border-slate-200 p-4 text-sm"><p className="font-semibold">{ticketToCancel.eventName}</p><p className="mt-1 text-slate-500">{ticketToCancel.ticketCode} · {ticketTypeNames[ticketToCancel.type]}</p></div>}<FormError error={cancel.error} /></Modal>
  </>
}
