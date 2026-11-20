import { useState } from 'react'
import { Link, useParams } from 'react-router'
import { useTicket, useCancelTicketMutation } from '../features/tickets/hooks'
import { ApiError } from '../lib/http'
import { fecha, moneda } from '../lib/formato'
import { RUTAS } from '../lib/rutas'
import { Badge, Button, ErrorState, Modal, FormError } from '../components/UI'
import { statusTone, statusNames, ticketTypeNames } from '../lib/dominio'
import { TicketNoDisponible } from '../components/TicketNoDisponible'

export default function DetalleTicket() {
  const { ticketCode = '' } = useParams()
  const q = useTicket(ticketCode)
  const [cancelOpen, setCancelOpen] = useState(false)
  const cancel = useCancelTicketMutation()
  if (q.isPending) return <p className="py-20 text-center text-slate-500">Cargando entrada…</p>
  if (q.isError) return q.error instanceof ApiError && [403, 404].includes(q.error.status) ? <TicketNoDisponible /> : <ErrorState error={q.error} retry={() => void q.refetch()} />
  return <>
    <Link to={RUTAS.misTickets} className="text-sm font-semibold text-violet-700">← Mis tickets</Link>
    <div className="surface mx-auto mt-6 max-w-2xl overflow-hidden rounded-[2rem]">
      <div className="bg-gradient-to-r from-violet-700/90 to-cyan-700/70 p-8"><p className="eyebrow">Tu entrada PulsePass</p><h1 className="page-title mt-3 text-3xl text-white">{q.data.eventName}</h1><p className="mt-2 break-all text-sm text-white/80">{q.data.ticketCode}</p></div>
      <div className="space-y-5 p-8"><div className="flex items-center justify-between"><span className="text-sm text-slate-500">Estado</span><Badge tone={statusTone(q.data.status)}>{statusNames[q.data.status]}</Badge></div><div className="grid grid-cols-2 gap-5"><div><p className="text-xs text-slate-500">Tipo</p><p className="mt-1 font-semibold">{ticketTypeNames[q.data.type]}</p></div><div><p className="text-xs text-slate-500">Precio</p><p className="mt-1 font-semibold">{moneda(q.data.price)}</p></div><div><p className="text-xs text-slate-500">Código</p><p className="mt-1 break-all font-semibold">{q.data.ticketCode}</p></div><div><p className="text-xs text-slate-500">Compra</p><p className="mt-1 font-semibold">{fecha(q.data.purchaseDate)}</p></div></div>{q.data.status === 'PAID' && <Button variant="danger" disabled={cancel.isPending} onClick={() => setCancelOpen(true)}>Cancelar entrada</Button>}<p className="border-t border-slate-200 pt-4 text-xs text-slate-500">Esta vista es tu comprobante de compra. La validación de ingreso se realiza en el acceso del evento.</p></div>
    </div>
    <Modal open={cancelOpen} title="Cancelar entrada" description="Confirma si deseas solicitar la cancelación de este ticket." onClose={() => { if (!cancel.isPending) { setCancelOpen(false); cancel.reset() } }} footer={<><Button type="button" variant="secondary" disabled={cancel.isPending} onClick={() => setCancelOpen(false)}>Volver</Button><Button type="button" variant="danger" disabled={cancel.isPending} onClick={() => cancel.mutate(ticketCode, { onSuccess: () => setCancelOpen(false) })}>{cancel.isPending ? 'Cancelando…' : 'Confirmar cancelación'}</Button></>}> <p className="text-sm text-slate-600">{q.data.eventName} · {q.data.ticketCode}</p><FormError error={cancel.error}/></Modal>
  </>
}
