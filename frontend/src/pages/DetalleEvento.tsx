import { useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router'
import { useEvent } from '../features/events/hooks'
import { useBuyTicketMutation } from '../features/tickets/hooks'
import { ApiError } from '../lib/http'
import { fecha } from '../lib/formato'
import { RUTAS } from '../lib/rutas'
import { TicketTypeSchema, type TicketType } from '../lib/schemas'
import { useAuth } from '../auth/AuthContext'
import { Badge, Button, ErrorState, Modal, FormError } from '../components/UI'
import { categoryNames, statusTone, statusNames, ticketTypeNames } from '../lib/dominio'

export default function DetalleEvento() {
  const { eventCode = '' } = useParams()
  const event = useEvent(eventCode)
  const { sesion } = useAuth()
  const [type, setType] = useState<TicketType>('GENERAL')
  const [purchaseOpen, setPurchaseOpen] = useState(false)
  const navigate = useNavigate()
  const buy = useBuyTicketMutation()
  if (event.isPending) return <div className="py-20 text-center text-slate-500">Cargando evento…</div>
  if (event.isError) return event.error instanceof ApiError && [403, 404].includes(event.error.status) ? <div className="surface mx-auto max-w-xl rounded-2xl p-8 text-center" role="alert"><h1 className="font-display text-2xl font-bold">Evento no disponible</h1><p className="mt-2 text-sm text-slate-500">El evento no existe o todavía no está publicado.</p><Link to={RUTAS.eventos} className="btn-primary mt-5">Volver a la cartelera</Link></div> : <ErrorState error={event.error} retry={() => void event.refetch()} />

  const data = event.data
  const published = data.status === 'PUBLISHED'
  const canStartPurchase = published
  const hasUserRole = sesion.estado === 'autenticado' && sesion.usuario.roles.includes('USER')
  const staffView = sesion.estado === 'autenticado' && !hasUserRole
  const unavailableMessage = data.status === 'DRAFT'
      ? 'Este evento todavía es un borrador. El administrador debe publicarlo antes de habilitar la compra.'
      : data.status === 'SOLD_OUT'
        ? 'Las entradas para este evento se agotaron.'
        : data.status === 'CANCELLED'
          ? 'Este evento fue cancelado y no admite compras.'
          : data.status === 'FINISHED'
            ? 'Este evento ya finalizó.'
            : ''

  return <div className="space-y-8 pb-24 md:pb-0">
    <Link to={RUTAS.eventos} className="text-sm font-semibold text-violet-700">← Volver a eventos</Link>
    <div className="hero-dark relative overflow-hidden rounded-[2rem] border border-violet-300/15 bg-gradient-to-br from-[#2b1c68] via-[#17234a] to-[#10363c] p-7 sm:p-12">
      <div className="absolute -right-10 -top-20 text-[260px] font-black text-white/[.04]">✦</div>
      <div className="relative max-w-3xl">
        <Badge tone={statusTone(data.status)}>{statusNames[data.status] ?? data.status}</Badge>
        <p className="eyebrow mt-6">{categoryNames[data.category]}</p>
        <h1 className="page-title mt-3 text-4xl text-white sm:text-6xl">{data.name}</h1>
        <p className="mt-5 max-w-2xl text-slate-300">{data.description || 'Una experiencia que no querrás perderte.'}</p>
        <div className="mt-8 grid gap-4 text-sm sm:grid-cols-3">
          <div><p className="text-white/60">Fecha y hora</p><p className="mt-1 font-semibold text-white">{fecha(data.eventDate)}</p></div>
          <div><p className="text-white/60">Lugar</p><p className="mt-1 font-semibold text-white">{data.venueName}</p></div>
          <div><p className="text-white/60">Edad mínima</p><p className="mt-1 font-semibold text-white">{data.minimumAge} años</p></div>
        </div>
      </div>
    </div>
    <div className={staffView ? 'grid gap-8' : 'grid gap-8 lg:grid-cols-[1fr_380px]'}>
      <section className="space-y-6">
        <div className="surface rounded-2xl p-6 sm:p-7">
          <h2 className="font-display text-xl font-bold">Sobre el evento</h2>
          <p className="mt-3 leading-7 text-slate-500">{data.description || 'Consulta los detalles del evento y prepárate para vivir una gran experiencia.'}</p>
          <Link to={RUTAS.sede(data.venueCode)} className="mt-5 inline-flex text-sm font-semibold text-violet-700">Conocer la sede →</Link>
        </div>
        <div className="surface rounded-2xl p-6 sm:p-7">
          <h2 className="font-display text-xl font-bold">Artistas</h2>
          {data.artists.length ? <div className="mt-4 flex flex-wrap gap-3">{data.artists.map((artist) => <Link key={artist.id} to={RUTAS.artista(artist.id)} className="rounded-xl border border-slate-200 px-4 py-3 transition hover:border-violet-300/60"><strong>{artist.stageName}</strong><span className="mt-1 block text-xs text-slate-500">{artist.genre || artist.country}</span></Link>)}</div> : <p className="mt-3 text-sm text-slate-500">Los artistas se anunciarán próximamente.</p>}
        </div>
      </section>
      {!staffView && <aside className="surface h-fit rounded-[1.75rem] p-6 shadow-xl shadow-violet-950/5 sm:p-7">
        <div className="flex items-center justify-between gap-3"><p className="eyebrow">Asegura tu lugar</p><span className="rounded-full bg-violet-500/10 px-3 py-1 text-[11px] font-bold text-violet-700">PulsePass</span></div>
        <h2 className="mt-3 font-display text-2xl font-extrabold">Compra tu entrada</h2>
        <p className="mt-2 text-sm leading-6 text-slate-500">Selecciona la categoría de tu entrada. El precio definitivo lo calcula el sistema y se confirma después de la solicitud.</p>
        <label className="mt-6 block text-sm font-semibold text-slate-600" htmlFor="ticket-type">Tipo de entrada</label>
        <select id="ticket-type" className="field mt-2" value={type} onChange={(e) => { setType(e.target.value as TicketType); buy.reset() }}>
          {TicketTypeSchema.options.map((value) => <option key={value} value={value}>{ticketTypeNames[value]}</option>)}
        </select>
        <div className="mt-4 rounded-xl border border-slate-200 bg-slate-50/80 p-4 text-xs leading-5 text-slate-500">
          <p className="font-bold text-slate-700">Antes de continuar</p>
          <p className="mt-1">La compra académica registra una entrada en el sistema. No se solicitan datos de tarjeta ni se conecta una pasarela de pago.</p>
        </div>
        {canStartPurchase ? <>
                    <Button className="mt-5 hidden w-full md:inline-flex" disabled={buy.isPending} onClick={() => { if (sesion.estado !== 'autenticado') navigate(RUTAS.login, { state: { desde: RUTAS.evento(eventCode) } }); else setPurchaseOpen(true) }}>
            {buy.isPending ? 'Procesando compra…' : sesion.estado === 'autenticado' ? 'Continuar' : 'Iniciar sesión para comprar'}
          </Button>
        </> : <div className="mt-5 rounded-xl border border-amber-300/40 bg-amber-50 p-4 text-sm leading-6 text-amber-900" role="status">{unavailableMessage}</div>}
      </aside>}
      {canStartPurchase && !staffView && <div className="fixed inset-x-0 bottom-0 z-30 border-t border-slate-200 bg-white/95 p-3 shadow-[0_-8px_30px_rgba(15,23,42,.12)] backdrop-blur md:hidden dark:border-slate-700 dark:bg-slate-950/95"><div className="mx-auto flex max-w-xl items-center gap-3"><div className="min-w-0 flex-1"><p className="text-xs text-slate-500">Entrada {ticketTypeNames[type]}</p><p className="truncate text-sm font-semibold">{data.name}</p></div><Button className="shrink-0" disabled={buy.isPending} onClick={() => { if (sesion.estado !== 'autenticado') navigate(RUTAS.login, { state: { desde: RUTAS.evento(eventCode) } }); else setPurchaseOpen(true) }}>{sesion.estado === 'autenticado' ? 'Continuar' : 'Ingresar'}</Button></div></div>}
      <Modal open={purchaseOpen} title="Confirmar compra" description="Revisa los datos antes de registrar tu entrada. No se realizará ningún cobro en línea." onClose={() => { if (!buy.isPending) setPurchaseOpen(false) }} footer={<><Button type="button" variant="secondary" disabled={buy.isPending} onClick={() => setPurchaseOpen(false)}>Volver</Button><Button type="button" disabled={buy.isPending} onClick={() => { if (sesion.estado === 'autenticado') buy.mutate({ userEmail: sesion.usuario.email, eventCode, type }, { onSuccess: (ticket) => { setPurchaseOpen(false); navigate(RUTAS.confirmacion(ticket.ticketCode)) } }) }}>{buy.isPending ? 'Procesando…' : 'Confirmar entrada'}</Button></>}>
        <dl className="space-y-3 rounded-2xl border border-slate-200 p-4 text-sm"><div className="flex justify-between gap-4"><dt className="text-slate-500">Evento</dt><dd className="text-right font-semibold">{data.name}</dd></div><div className="flex justify-between gap-4"><dt className="text-slate-500">Tipo de entrada</dt><dd className="font-semibold">{ticketTypeNames[type]}</dd></div><div className="flex justify-between gap-4"><dt className="text-slate-500">Cuenta</dt><dd className="break-all text-right font-semibold">{sesion.estado === 'autenticado' ? sesion.usuario.email : ''}</dd></div></dl>
        <p className="mt-3 text-xs leading-5 text-slate-500">El precio lo determina el backend y se mostrará en el comprobante cuando la solicitud sea aceptada.</p><FormError error={buy.error}/>
      </Modal>
    </div>
  </div>
}
