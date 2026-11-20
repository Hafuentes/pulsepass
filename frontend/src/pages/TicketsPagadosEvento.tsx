import { useParams } from 'react-router'
import { usePaidTickets, useUseTicketMutation } from '../features/tickets/hooks'
import { moneda } from '../lib/formato'
import { Badge, Button, EmptyState, ErrorState, FormError, SectionTitle } from '../components/UI'
import { statusTone, statusNames, ticketTypeNames } from '../lib/dominio'

export function TicketsPagadosEventoPanel({ eventCode, inModal = false }: { eventCode: string; inModal?: boolean }) {
  const q = usePaidTickets(eventCode, true)
  const useIt = useUseTicketMutation()
  return <section className={inModal ? '' : 'mt-8 border-t border-slate-200 pt-7'} aria-label="Tickets pagados del evento">
    <div className="mb-5 flex flex-wrap items-center justify-between gap-3">
      {!inModal && <div><h2 className="font-display text-xl font-bold">Tickets pagados</h2><p className="mt-1 text-sm text-slate-500">Entradas del evento {eventCode}; la lista se actualiza automáticamente.</p></div>}
      <Button variant="secondary" disabled={q.isFetching} onClick={() => void q.refetch()}>{q.isFetching ? 'Actualizando…' : 'Actualizar lista'}</Button>
    </div>
    {q.isPending ? <p className="text-sm text-slate-500">Cargando tickets…</p> : q.isError ? <ErrorState error={q.error} retry={() => void q.refetch()} /> : <>
      <div className="surface mb-5 flex flex-wrap items-center justify-between gap-3 rounded-2xl p-5"><div><p className="text-sm text-slate-500">Total de tickets pagados</p><p className="mt-1 font-display text-3xl font-extrabold">{q.data.length}</p></div><Badge tone="success">Solo estado PAID</Badge></div>
      {q.data.length ? <>
        <div className="surface hidden overflow-hidden rounded-2xl md:block"><div className="overflow-x-auto"><table className="w-full text-left text-sm"><thead className="bg-slate-50 text-xs uppercase tracking-wide text-slate-500 dark:bg-slate-800"><tr><th className="px-5 py-4">Ticket</th><th className="px-5 py-4">Comprador</th><th className="px-5 py-4">Tipo</th><th className="px-5 py-4">Valor</th><th className="px-5 py-4">Estado</th><th className="px-5 py-4">Acción</th></tr></thead><tbody>{q.data.map((ticket) => <tr key={ticket.ticketCode} className="border-t border-slate-200 dark:border-slate-700"><td className="px-5 py-4"><p className="font-semibold">{ticket.ticketCode}</p><p className="mt-1 text-xs text-slate-500">{ticket.eventName}</p></td><td className="break-all px-5 py-4">{ticket.userEmail}</td><td className="px-5 py-4">{ticketTypeNames[ticket.type]}</td><td className="px-5 py-4 font-semibold">{moneda(ticket.price)}</td><td className="px-5 py-4"><Badge tone={statusTone(ticket.status)}>{statusNames[ticket.status]}</Badge></td><td className="px-5 py-4">{ticket.status === 'PAID' && <Button disabled={useIt.isPending} onClick={() => useIt.mutate(ticket.ticketCode)}>Validar ingreso</Button>}</td></tr>)}</tbody></table></div></div>
        <div className="space-y-3 md:hidden">{q.data.map((ticket) => <article key={ticket.ticketCode} className="surface rounded-2xl p-5"><div className="flex items-start justify-between gap-3"><div className="min-w-0"><p className="break-all text-xs font-semibold text-slate-500">{ticket.ticketCode}</p><h3 className="mt-1 font-display font-bold">{ticket.eventName}</h3></div><Badge tone={statusTone(ticket.status)}>{statusNames[ticket.status]}</Badge></div><p className="mt-3 break-all text-sm text-slate-500">{ticket.userEmail}</p><div className="mt-3 flex items-center justify-between gap-3 text-sm"><span>{ticketTypeNames[ticket.type]}</span><strong>{moneda(ticket.price)}</strong></div>{ticket.status === 'PAID' && <Button className="mt-4 w-full" disabled={useIt.isPending} onClick={() => useIt.mutate(ticket.ticketCode)}>Validar ingreso</Button>}</article>)}</div>
      </> : <EmptyState title="No hay tickets pagados" description="Todavía no hay compras PAID para este evento." />}
    </>}
    <FormError error={useIt.error} />
  </section>
}

export default function TicketsPagadosEvento() {
  const { eventCode = '' } = useParams()
  return <><SectionTitle eyebrow="Operación · Staff y administración" title="Tickets pagados" description={`Entradas registradas como PAID para el evento ${eventCode}.`} /><TicketsPagadosEventoPanel eventCode={eventCode} /></>
}
