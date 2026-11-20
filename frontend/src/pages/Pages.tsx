import { useState } from 'react'
import { Link, useLocation, useNavigate, useParams, useSearchParams } from 'react-router'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { api } from '../lib/api'
import { ApiError, mensajeError } from '../lib/http'
import { fecha, fechaCorta, moneda } from '../lib/formato'
import { RUTAS } from '../lib/rutas'
import { EventCategorySchema, TicketTypeSchema, type EventSummary, type Ticket, type TicketType } from '../lib/schemas'
import { useAuth } from '../auth/AuthContext'
import { Badge, Button, EmptyState, ErrorState, LoadingGrid, Modal, SectionTitle } from '../components/UI'

const categoryNames: Record<string, string> = { MUSIC: 'Música', SPORTS: 'Deportes', TECHNOLOGY: 'Tecnología', EDUCATION: 'Educación', CULTURE: 'Cultura', ENTERTAINMENT: 'Entretenimiento' }
const statusTone = (status: string): 'neutral' | 'success' | 'warning' | 'danger' | 'brand' => status === 'PUBLISHED' || status === 'PAID' ? 'success' : status === 'SOLD_OUT' || status === 'USED' ? 'warning' : status === 'CANCELLED' ? 'danger' : 'brand'
const statusNames: Record<string, string> = { PUBLISHED: 'Publicado', DRAFT: 'Borrador', SOLD_OUT: 'Agotado', CANCELLED: 'Cancelado', FINISHED: 'Finalizado', PAID: 'Pagado', RESERVED: 'Reservado', USED: 'Usado' }
const ticketTypeNames: Record<string, string> = { GENERAL: 'General', VIP: 'VIP', BACKSTAGE: 'Backstage', STUDENT: 'Estudiante' }
function EventCard({ event }: { event: EventSummary }) {
  const color: Record<string, string> = { MUSIC: 'from-violet-600/70 to-fuchsia-500/20', SPORTS: 'from-emerald-500/50 to-cyan-500/10', TECHNOLOGY: 'from-blue-600/60 to-cyan-500/10', EDUCATION: 'from-amber-500/50 to-orange-500/10', CULTURE: 'from-rose-500/50 to-pink-500/10', ENTERTAINMENT: 'from-indigo-500/60 to-purple-500/10' }
  const symbol: Record<string, string> = { MUSIC: '♫', SPORTS: '✦', TECHNOLOGY: '⌘', EDUCATION: '✧', CULTURE: '◈', ENTERTAINMENT: '✺' }
  return <Link to={RUTAS.evento(event.eventCode)} className="surface card-hover group overflow-hidden rounded-2xl"><div className={`relative flex h-40 items-end justify-between overflow-hidden bg-gradient-to-br ${color[event.category] ?? 'from-violet-600/50 to-slate-700/10'} p-5`}><div className="absolute -right-3 -top-10 text-[150px] font-black leading-none text-white/[.07]">{symbol[event.category] ?? '✦'}</div><span className="relative rounded-full border border-white/20 bg-black/20 px-3 py-1 text-[10px] font-bold tracking-[.16em] text-white">{categoryNames[event.category] ?? event.category}</span><span className="relative text-xs text-white/80">{fechaCorta(event.eventDate)}</span></div><div className="p-5"><div className="mb-3 flex items-center justify-between gap-2"><Badge tone={statusTone(event.status)}>{statusNames[event.status] ?? event.status}</Badge><span className="text-xs text-slate-500">{event.venueCity ?? event.venueName}</span></div><h3 className="font-display text-lg font-bold transition group-hover:text-violet-700 dark:group-hover:text-violet-700 dark:hover:text-violet-200">{event.name}</h3><p className="mt-2 text-sm text-slate-500">{event.venueName}</p><div className="mt-5 flex items-center justify-between border-t border-slate-200 pt-4 text-sm"><span className="text-slate-500">Ver detalles</span><span className="text-violet-700 transition group-hover:translate-x-1">→</span></div></div></Link>
}
function TicketRow({ ticket, onCancel }: { ticket: Ticket; onCancel?: (ticket: Ticket) => void }) { return <div className="surface flex flex-col gap-4 rounded-2xl p-5 sm:flex-row sm:items-center sm:justify-between"><div className="min-w-0"><div className="mb-2 flex flex-wrap items-center gap-2"><Badge tone={statusTone(ticket.status)}>{statusNames[ticket.status] ?? ticket.status}</Badge><span className="text-xs text-slate-500">{ticket.ticketCode}</span></div><Link to={RUTAS.ticket(ticket.ticketCode)} className="font-display text-lg font-bold hover:text-violet-700 dark:hover:text-violet-200">{ticket.eventName}</Link><p className="mt-1 text-sm text-slate-500">{ticketTypeNames[ticket.type] ?? ticket.type} · {moneda(ticket.price)}</p></div><div className="flex items-center gap-2"><Link to={RUTAS.ticket(ticket.ticketCode)} className="btn-secondary text-sm">Ver entrada</Link>{onCancel && ticket.status === 'PAID' && <Button variant="danger" onClick={() => onCancel(ticket)}>Cancelar</Button>}</div></div> }
function FormError({ error }: { error: unknown }) { return error ? <p className="rounded-xl border border-rose-400/30 bg-rose-50 p-3 text-sm text-rose-800 dark:border-rose-400/20 dark:bg-rose-400/10 dark:text-rose-200" role="alert" aria-live="assertive">{mensajeError(error)}</p> : null }

export function Inicio() {
  const events = useQuery({ queryKey: ['eventos','publicados'], queryFn: api.events })
  const artists = useQuery({ queryKey: ['artistas','activos'], queryFn: api.artists })
  const { sesion } = useAuth()
  return <div className="space-y-16"><section className="hero-dark relative overflow-hidden rounded-[2rem] border border-violet-300/15 bg-gradient-to-br from-[#20164f] via-[#171b42] to-[#0c3540] px-6 py-14 sm:px-12 sm:py-20"><div className="absolute -right-20 -top-20 h-80 w-80 rounded-full bg-violet-500/20 blur-3xl"/><div className="absolute -bottom-24 left-1/3 h-64 w-64 rounded-full bg-cyan-400/10 blur-3xl"/><div className="relative max-w-3xl"><p className="eyebrow mb-5">Tu próxima historia comienza aquí</p><h1 className="page-title max-w-2xl text-4xl text-white sm:text-6xl">Descubre eventos.<br/><span className="bg-gradient-to-r from-violet-300 to-cyan-300 bg-clip-text text-transparent">Vive el momento.</span></h1><p className="mt-6 max-w-xl text-base leading-7 text-slate-300 sm:text-lg">Música, cultura, deporte y experiencias increíbles en un solo lugar. Encuentra eso que te emociona y haz parte de ello.</p><div className="mt-8 flex flex-wrap gap-3"><Link to={RUTAS.eventos} className="btn-primary">Explorar eventos <span>→</span></Link>{sesion.estado !== 'autenticado' && <Link to={RUTAS.registro} className="btn-secondary">Crear cuenta</Link>}</div><div className="mt-12 flex flex-wrap gap-8 text-sm"><div><p className="font-display text-2xl font-extrabold">∞</p><p className="mt-1 text-xs text-white/70">Experiencias por vivir</p></div><div><p className="font-display text-2xl font-extrabold">1 lugar</p><p className="mt-1 text-xs text-white/70">Para descubrir y reservar</p></div></div></div><div className="absolute bottom-8 right-12 hidden h-52 w-52 rotate-12 rounded-[2.5rem] border border-white/10 bg-white/[.04] p-4 lg:block"><div className="flex h-full flex-col justify-between rounded-[1.8rem] border border-white/10 bg-gradient-to-br from-violet-500/20 to-cyan-400/10 p-5"><span className="text-4xl">✦</span><div><p className="text-xs uppercase tracking-widest text-violet-200">PulsePass</p><p className="font-display text-2xl font-extrabold">Feel it.</p></div></div></div></section>
    <section><SectionTitle eyebrow="No te lo pierdas" title="Próximos eventos" description="Planes que merecen un espacio en tu agenda." action={<Link to={RUTAS.eventos} className="text-sm font-semibold text-violet-700">Ver cartelera →</Link>}/>{events.isPending ? <LoadingGrid/> : events.isError ? <ErrorState error={events.error} retry={() => void events.refetch()}/> : events.data.length ? <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">{events.data.slice(0,3).map((event) => <EventCard key={event.eventCode} event={event}/>)}</div> : <EmptyState title="Aún no hay eventos publicados" description="Vuelve pronto para descubrir nuevas experiencias."/>}</section>
    <section><SectionTitle eyebrow="Talento que inspira" title="Artistas destacados" action={<Link to={RUTAS.artistas} className="text-sm font-semibold text-violet-700">Ver artistas →</Link>}/>{artists.isPending ? <div className="h-28 animate-pulse rounded-2xl bg-white/5"/> : artists.isError ? <ErrorState error={artists.error} retry={() => void artists.refetch()}/> : <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">{artists.data.slice(0,6).map((artist) => <Link key={artist.id} to={RUTAS.artista(artist.id)} className="surface card-hover flex items-center gap-4 rounded-2xl p-4"><span className="grid h-12 w-12 place-items-center rounded-2xl bg-gradient-to-br from-violet-500/30 to-cyan-400/20 text-xl">♫</span><span><strong className="block">{artist.stageName}</strong><small className="text-slate-500">{artist.genre || artist.country || 'Artista'}</small></span><span className="ml-auto text-violet-700">→</span></Link>)}</div>}</section></div>
}

export function CatalogoEventos() {
  const [params, setParams] = useSearchParams()
  const q = params.get('q') ?? ''; const category = params.get('categoria') ?? ''; const artistName = params.get('artista') ?? ''
  const events = useQuery({ queryKey: ['eventos','publicados'], queryFn: api.events, enabled: !artistName })
  const artistLookup = useQuery({ queryKey: ['artista','nombre',artistName], queryFn: () => api.artistByName(artistName), enabled: Boolean(artistName) })
  const artistEvents = useQuery({ queryKey: ['eventos','artista',artistName], queryFn: () => api.eventsByArtist(artistName), enabled: Boolean(artistName) })
  const artists = useQuery({ queryKey: ['artistas','activos'], queryFn: api.artists })
  const filtered = (artistName ? (artistEvents.data ?? []) : (events.data ?? [])).filter((event) => `${event.name} ${event.venueName} ${event.venueCity ?? ''}`.toLowerCase().includes(q.toLowerCase()) && (!category || event.category === category))
  return <><SectionTitle eyebrow="La agenda empieza aquí" title="Explora la cartelera" description="Encuentra el evento que va contigo. Filtra por nombre, ciudad, categoría o artista."/>
    {artistName && <div className="surface mb-5 flex flex-wrap items-center justify-between gap-3 rounded-xl p-4 text-sm"><span>Eventos de <strong>{artistLookup.data?.stageName ?? artistName}</strong></span><button className="text-violet-700" onClick={() => setParams((prev) => { prev.delete('artista'); return prev })}>Quitar filtro ×</button></div>}<div className="surface mb-7 grid gap-3 rounded-2xl p-4 sm:grid-cols-[1fr_220px]"><input aria-label="Buscar eventos" className="field" placeholder="Busca por evento o ciudad…" value={q} onChange={(e) => setParams((prev) => { if (e.target.value) prev.set('q', e.target.value); else prev.delete('q'); return prev }, { replace: true })}/><select aria-label="Filtrar por categoría" className="field" value={category} onChange={(e) => setParams((prev) => { if (e.target.value) prev.set('categoria', e.target.value); else prev.delete('categoria'); return prev }, { replace: true })}><option value="">Todas las categorías</option>{EventCategorySchema.options.map((item) => <option key={item} value={item}>{categoryNames[item]}</option>)}</select></div>{(artistName ? (artistLookup.isPending || artistEvents.isPending) : events.isPending) ? <LoadingGrid/> : !artistName && events.isError ? <ErrorState error={events.error} retry={() => void events.refetch()}/> : artistName && (artistLookup.isError || artistEvents.isError) ? <ErrorState error={artistLookup.error ?? artistEvents.error}/> : filtered.length ? <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">{filtered.map((event) => <EventCard key={event.eventCode} event={event}/>)}</div> : <EmptyState title="No encontramos eventos" description="Prueba con otra búsqueda o cambia la categoría."/>}<section className="mt-14"><SectionTitle eyebrow="Encuentra tu sonido" title="Buscar eventos por artista" description="Selecciona un artista para ver sus próximos eventos."/>{artists.isPending ? <p className="text-sm text-slate-500">Cargando artistas…</p> : artists.isError ? <ErrorState error={artists.error}/> : <div className="flex flex-wrap gap-2">{artists.data.map((artist) => <button key={artist.id} onClick={() => setParams((prev) => { prev.set('artista', artist.stageName); return prev })} className="btn-secondary text-sm">{artist.stageName}</button>)}</div>}</section></>
}

export function DetalleEvento() {
  const { eventCode = '' } = useParams()
  const event = useQuery({ queryKey: ['evento', eventCode], queryFn: () => api.event(eventCode), enabled: Boolean(eventCode) })
  const { sesion } = useAuth()
  const [type, setType] = useState<TicketType>('GENERAL')
  const [purchaseOpen, setPurchaseOpen] = useState(false)
  const client = useQueryClient()
  const navigate = useNavigate()
  const buy = useMutation({
    mutationFn: () => {
      if (sesion.estado !== 'autenticado') throw new Error('Inicia sesión para comprar.')
      if (!sesion.usuario.roles.includes('USER')) throw new Error('Esta cuenta no tiene el rol USER necesario para comprar entradas. Ingresa con una cuenta de asistente.')
      return api.buyTicket({ userEmail: sesion.usuario.email, eventCode, type })
    },
    onSuccess: (ticket) => {
      if (sesion.estado === 'autenticado') void client.invalidateQueries({ queryKey: ['tickets', 'usuario', sesion.usuario.email] })
      void client.invalidateQueries({ queryKey: ['evento', eventCode] })
      // Mantener sincronizada la vista de operación/admin si permanece abierta.
      void client.invalidateQueries({ queryKey: ['tickets', 'pagados', eventCode] })
      setPurchaseOpen(false)
      navigate(RUTAS.confirmacion(ticket.ticketCode))
    },
  })
  if (event.isPending) return <div className="py-20 text-center text-slate-500">Cargando evento…</div>
  if (event.isError) return event.error instanceof ApiError && event.error.status === 404 ? <div className="surface mx-auto max-w-xl rounded-2xl p-8 text-center" role="alert"><h1 className="font-display text-2xl font-bold">Evento no disponible</h1><p className="mt-2 text-sm text-slate-500">El evento no existe o todavía no está publicado.</p><Link to={RUTAS.eventos} className="btn-primary mt-5">Volver a la cartelera</Link></div> : <ErrorState error={event.error} retry={() => void event.refetch()} />

  const data = event.data
  const published = data.status === 'PUBLISHED'
  const canStartPurchase = published
  const hasUserRole = sesion.estado === 'autenticado' && sesion.usuario.roles.includes('USER')
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
    <div className="grid gap-8 lg:grid-cols-[1fr_380px]">
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
      <aside className="surface h-fit rounded-[1.75rem] p-6 shadow-xl shadow-violet-950/5 sm:p-7">
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
          {sesion.estado === 'autenticado' && !hasUserRole && <p className="mt-4 rounded-xl border border-amber-300/40 bg-amber-50 p-3 text-sm text-amber-800">Tu cuenta no tiene el rol USER para comprar. Ingresa con una cuenta de asistente.</p>}
          <Button className="mt-5 hidden w-full md:inline-flex" disabled={buy.isPending || (sesion.estado === 'autenticado' && !hasUserRole)} onClick={() => { if (sesion.estado !== 'autenticado') navigate(RUTAS.login, { state: { desde: RUTAS.evento(eventCode) } }); else setPurchaseOpen(true) }}>
            {buy.isPending ? 'Procesando compra…' : sesion.estado === 'autenticado' ? 'Continuar' : 'Iniciar sesión para comprar'}
          </Button>
        </> : <div className="mt-5 rounded-xl border border-amber-300/40 bg-amber-50 p-4 text-sm leading-6 text-amber-900" role="status">{unavailableMessage}</div>}
      </aside>
      {canStartPurchase && <div className="fixed inset-x-0 bottom-0 z-30 border-t border-slate-200 bg-white/95 p-3 shadow-[0_-8px_30px_rgba(15,23,42,.12)] backdrop-blur md:hidden dark:border-slate-700 dark:bg-slate-950/95"><div className="mx-auto flex max-w-xl items-center gap-3"><div className="min-w-0 flex-1"><p className="text-xs text-slate-500">Entrada {ticketTypeNames[type]}</p><p className="truncate text-sm font-semibold">{data.name}</p></div><Button className="shrink-0" disabled={buy.isPending || (sesion.estado === 'autenticado' && !hasUserRole)} onClick={() => { if (sesion.estado !== 'autenticado') navigate(RUTAS.login, { state: { desde: RUTAS.evento(eventCode) } }); else setPurchaseOpen(true) }}>{sesion.estado === 'autenticado' ? 'Continuar' : 'Ingresar'}</Button></div></div>}
      <Modal open={purchaseOpen} title="Confirmar compra" description="Revisa los datos antes de registrar tu entrada. No se realizará ningún cobro en línea." onClose={() => { if (!buy.isPending) setPurchaseOpen(false) }} footer={<><Button type="button" variant="secondary" disabled={buy.isPending} onClick={() => setPurchaseOpen(false)}>Volver</Button><Button type="button" disabled={buy.isPending} onClick={() => buy.mutate()}>{buy.isPending ? 'Procesando…' : 'Confirmar entrada'}</Button></>}>
        <dl className="space-y-3 rounded-2xl border border-slate-200 p-4 text-sm"><div className="flex justify-between gap-4"><dt className="text-slate-500">Evento</dt><dd className="text-right font-semibold">{data.name}</dd></div><div className="flex justify-between gap-4"><dt className="text-slate-500">Tipo de entrada</dt><dd className="font-semibold">{ticketTypeNames[type]}</dd></div><div className="flex justify-between gap-4"><dt className="text-slate-500">Cuenta</dt><dd className="break-all text-right font-semibold">{sesion.estado === 'autenticado' ? sesion.usuario.email : ''}</dd></div></dl>
        <p className="mt-3 text-xs leading-5 text-slate-500">El precio lo determina el backend y se mostrará en el comprobante cuando la solicitud sea aceptada.</p><FormError error={buy.error}/>
      </Modal>
    </div>
  </div>
}

export function ListaArtistas() { const q = useQuery({ queryKey: ['artistas','activos'], queryFn: api.artists }); return <><SectionTitle eyebrow="Voces y talento" title="Conoce a los artistas" description="Descubre quiénes están detrás de las experiencias que te gustan."/>{q.isPending ? <LoadingGrid/> : q.isError ? <ErrorState error={q.error} retry={() => void q.refetch()}/> : q.data.length ? <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">{q.data.map((a) => <Link key={a.id} to={RUTAS.artista(a.id)} className="surface card-hover rounded-2xl p-6"><div className="mb-5 grid h-14 w-14 place-items-center rounded-2xl bg-gradient-to-br from-violet-500/30 to-cyan-400/20 text-2xl">♫</div><h2 className="font-display text-xl font-bold">{a.stageName}</h2><p className="mt-2 text-sm text-slate-500">{[a.genre,a.country].filter(Boolean).join(' · ') || 'Artista PulsePass'}</p><span className="mt-6 inline-flex text-sm font-semibold text-violet-700">Ver perfil →</span></Link>)}</div> : <EmptyState title="Aún no hay artistas" description="Los perfiles aparecerán aquí cuando estén activos."/>}</> }
export function DetalleArtista() { const { id = '' } = useParams(); const validId = /^\d+$/.test(id) && Number(id) > 0; const artist = useQuery({ queryKey: ['artista',id], queryFn: () => api.artist(id), enabled: validId }); const events = useQuery({ queryKey: ['eventos','artista',artist.data?.stageName], queryFn: () => api.eventsByArtist(artist.data!.stageName), enabled: Boolean(artist.data?.stageName) }); if (!validId) return <NoEncontrada/>; if (artist.isPending) return <p className="py-20 text-center text-slate-500">Cargando perfil…</p>; if (artist.isError) return <ErrorState error={artist.error}/>; return <><Link to={RUTAS.artistas} className="text-sm text-violet-700">← Artistas</Link><div className="surface my-6 rounded-[2rem] p-8 sm:p-12"><div className="grid h-20 w-20 place-items-center rounded-3xl bg-gradient-to-br from-violet-500/40 to-cyan-400/20 text-4xl">♫</div><p className="eyebrow mt-7">Artista</p><h1 className="page-title mt-2 text-4xl">{artist.data.stageName}</h1><p className="mt-3 text-slate-500">{[artist.data.genre,artist.data.country].filter(Boolean).join(' · ') || 'Talento en PulsePass'}</p><Badge tone={artist.data.active ? 'success' : 'neutral'}>{artist.data.active ? 'Activo' : 'Inactivo'}</Badge></div><SectionTitle title="Eventos del artista"/>{events.isPending ? <LoadingGrid/> : events.isError ? <ErrorState error={events.error}/> : events.data.length ? <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">{events.data.map((e) => <EventCard key={e.eventCode} event={e}/>)}</div> : <EmptyState title="Sin eventos por ahora" description="Este artista todavía no tiene eventos publicados."/>}</> }
export function ListaSedes() { const q = useQuery({ queryKey: ['sedes','activas'], queryFn: api.venues }); return <><SectionTitle eyebrow="Espacios para encontrarnos" title="Nuestras sedes" description="Conoce los lugares donde suceden las experiencias PulsePass."/>{q.isPending ? <LoadingGrid/> : q.isError ? <ErrorState error={q.error}/> : q.data.length ? <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">{q.data.map((v) => <Link key={v.code} to={RUTAS.sede(v.code)} className="surface card-hover rounded-2xl p-6"><div className="mb-5 grid h-12 w-12 place-items-center rounded-2xl bg-cyan-400/10 text-2xl">⌖</div><h2 className="font-display text-xl font-bold">{v.name}</h2><p className="mt-2 text-sm text-slate-500">{v.city}</p><div className="mt-5 flex justify-between border-t border-slate-200 pt-4 text-sm"><span className="text-slate-500">Capacidad</span><strong>{v.capacity.toLocaleString('es-CO')} personas</strong></div></Link>)}</div> : <EmptyState title="No hay sedes activas" description="Las sedes disponibles aparecerán aquí."/>}</> }
export function DetalleSede() { const { code = '' } = useParams(); const q = useQuery({ queryKey: ['sede',code], queryFn: () => api.venue(code) }); if (q.isPending) return <p className="py-20 text-center text-slate-500">Cargando sede…</p>; if (q.isError) return <ErrorState error={q.error}/>; const v=q.data; return <><Link to={RUTAS.sedes} className="text-sm text-violet-700">← Sedes</Link><div className="surface mt-6 rounded-[2rem] p-8 sm:p-12"><div className="grid h-16 w-16 place-items-center rounded-2xl bg-cyan-400/10 text-3xl">⌖</div><p className="eyebrow mt-7">Sede PulsePass</p><h1 className="page-title mt-2 text-4xl">{v.name}</h1><p className="mt-3 text-slate-500">{v.city}</p><p className="mt-2 text-sm text-slate-500">{v.address || 'Dirección no disponible'}</p><div className="mt-8 flex flex-wrap gap-8"><div><p className="text-xs text-slate-500">Capacidad</p><p className="mt-1 text-2xl font-bold">{v.capacity.toLocaleString('es-CO')}</p></div><div><p className="text-xs text-slate-500">Estado</p><p className="mt-1"><Badge tone={v.active?'success':'neutral'}>{v.active?'Activa':'Inactiva'}</Badge></p></div></div></div></> }

const loginSchema = z.object({ identifier: z.string().min(1,'Ingresa tu usuario o correo'), password: z.string().min(1,'Ingresa tu contraseña') })
type LoginValues = z.infer<typeof loginSchema>
export function Login() { const { iniciarSesion } = useAuth(); const nav=useNavigate(); const location=useLocation(); const routeState=location.state as {mensaje?:string;motivo?:string}|null; const notice=routeState?.mensaje ?? (routeState?.motivo === 'expirada' ? 'Tu sesión expiró. Inicia sesión nuevamente.' : undefined); const [serverError,setServerError]=useState<unknown>(null); const form=useForm<LoginValues>({ resolver:zodResolver(loginSchema), defaultValues:{identifier:'',password:''} }); const submit=form.handleSubmit(async (values) => { setServerError(null); try { await iniciarSesion(values.identifier,values.password); const state=location.state as {desde?:string}|null; nav(state?.desde || RUTAS.cuenta,{replace:true}) } catch(e) { setServerError(e instanceof ApiError && e.status===401 ? new Error('Credenciales inválidas. Verifica tus datos e inténtalo de nuevo.') : e) } }); return <div className="mx-auto grid max-w-4xl overflow-hidden rounded-[2rem] border border-slate-200 bg-white shadow-2xl shadow-black/20 md:grid-cols-2"><div className="hero-dark hidden flex-col justify-between bg-gradient-to-br from-violet-700/70 to-cyan-700/40 p-10 md:flex"><div className="text-3xl font-black">pulse<span className="text-cyan-200">pass.</span></div><div><p className="eyebrow">Tu acceso a nuevas experiencias</p><h1 className="page-title mt-4 text-4xl text-white">Vuelve a donde pasan las cosas.</h1><p className="mt-4 text-sm leading-6 text-slate-200">Ingresa para gestionar tus entradas y seguir descubriendo eventos.</p></div><p className="text-xs text-white/70">PulsePass · Vive el momento</p></div><form onSubmit={submit} className="space-y-5 p-7 sm:p-10"><p className="eyebrow">Qué bueno tenerte de vuelta</p><h2 className="page-title text-3xl">Iniciar sesión</h2><p className="text-sm text-slate-500">Ingresa con tu usuario o correo electrónico.</p>{notice && <p className="rounded-xl border border-emerald-300/30 bg-emerald-50 p-3 text-sm text-emerald-700" role="status">{notice}</p>}<label className="block space-y-2 text-sm text-slate-700">Usuario o correo<input className="field" autoComplete="username" aria-invalid={Boolean(form.formState.errors.identifier)} aria-describedby={form.formState.errors.identifier ? 'login-identifier-error' : undefined} {...form.register('identifier')}/>{form.formState.errors.identifier && <small id="login-identifier-error" className="text-rose-700 dark:text-rose-300">{form.formState.errors.identifier.message}</small>}</label><label className="block space-y-2 text-sm text-slate-700">Contraseña<input className="field" type="password" autoComplete="current-password" aria-invalid={Boolean(form.formState.errors.password)} aria-describedby={form.formState.errors.password ? 'login-password-error' : undefined} {...form.register('password')}/>{form.formState.errors.password && <small id="login-password-error" className="text-rose-700 dark:text-rose-300">{form.formState.errors.password.message}</small>}</label><FormError error={serverError}/><Button type="submit" className="w-full" disabled={form.formState.isSubmitting}>{form.formState.isSubmitting?'Ingresando…':'Ingresar'}</Button><p className="text-center text-sm text-slate-500">¿Aún no tienes cuenta? <Link to={RUTAS.registro} className="font-semibold text-violet-700">Regístrate</Link></p></form></div> }

const registerSchema = z.object({ username:z.string().min(1,'Ingresa un usuario'), email:z.string().email('Correo no válido'), firstName:z.string().min(1,'Ingresa tus nombres'), lastName:z.string().min(1,'Ingresa tus apellidos'), phone:z.string().optional(), city:z.string().optional(), birthDate:z.string().min(1,'Selecciona tu fecha de nacimiento').refine((value)=>new Date(value)<=new Date(),'La fecha no puede ser futura'), password:z.string().min(12,'La contraseña debe tener mínimo 12 caracteres').refine((v)=>new TextEncoder().encode(v).length<=72,'La contraseña supera el límite permitido de 72 bytes') })
type RegisterValues=z.infer<typeof registerSchema>
export function Registro() { const nav=useNavigate(); const [serverError,setServerError]=useState<unknown>(null); const form=useForm<RegisterValues>({resolver:zodResolver(registerSchema)}); const submit=form.handleSubmit(async (values)=>{setServerError(null);try{await api.register(values);nav(RUTAS.login,{state:{mensaje:'Cuenta creada. Ahora inicia sesión.'}})}catch(e){if(e instanceof ApiError && e.status===409){setServerError(new Error('El usuario o correo ya está registrado.'))}else if(e instanceof ApiError && e.status===400){const keys=['username','email','firstName','lastName','birthDate','password'] as const;let mapped=false;for(const key of keys){const message=e.details[key];if(message){form.setError(key,{type:'server',message});mapped=true}}setServerError(mapped?null:e)}else{setServerError(e)}}}); return <div className="mx-auto max-w-2xl"><div className="surface rounded-[2rem] p-7 sm:p-10"><p className="eyebrow">Únete a PulsePass</p><h1 className="page-title mt-2 text-3xl">Crea tu cuenta</h1><p className="mt-3 text-sm text-slate-500">Completa tus datos para comenzar a descubrir y comprar entradas.</p><form onSubmit={submit} className="mt-8 grid gap-5 sm:grid-cols-2">{([{name:'username',label:'Nombre de usuario',auto:'username'},{name:'email',label:'Correo electrónico',auto:'email'},{name:'firstName',label:'Nombres',auto:'given-name'},{name:'lastName',label:'Apellidos',auto:'family-name'},{name:'phone',label:'Teléfono',auto:'tel'},{name:'city',label:'Ciudad',auto:'address-level2'}] as const).map((field)=> <label key={field.name} className="space-y-2 text-sm text-slate-700">{field.label}<input className="field" autoComplete={field.auto} {...form.register(field.name)}/>{form.formState.errors[field.name] && <small className="text-rose-700 dark:text-rose-300">{form.formState.errors[field.name]?.message}</small>}</label>)}<label className="space-y-2 text-sm text-slate-700">Fecha de nacimiento<input type="date" className="field" {...form.register('birthDate')}/>{form.formState.errors.birthDate && <small className="text-rose-700 dark:text-rose-300">{form.formState.errors.birthDate.message}</small>}</label><label className="space-y-2 text-sm text-slate-700">Contraseña<input type="password" className="field" autoComplete="new-password" aria-invalid={Boolean(form.formState.errors.password)} aria-describedby={form.formState.errors.password ? 'register-password-error' : undefined} {...form.register('password')}/>{form.formState.errors.password && <small id="register-password-error" className="text-rose-700 dark:text-rose-300">{form.formState.errors.password.message}</small>}</label><div className="sm:col-span-2"><FormError error={serverError}/><Button type="submit" className="mt-2 w-full" disabled={form.formState.isSubmitting}>{form.formState.isSubmitting?'Creando cuenta…':'Crear cuenta'}</Button></div></form><p className="mt-5 text-center text-sm text-slate-500">¿Ya tienes cuenta? <Link to={RUTAS.login} className="text-violet-700">Inicia sesión</Link></p></div></div> }

export function Cuenta() { const {sesion}=useAuth(); const email=sesion.estado==='autenticado'?sesion.usuario.email:''; const username=sesion.estado==='autenticado'?sesion.usuario.username:''; const user=useQuery({queryKey:['usuario',email],queryFn:()=>api.userByEmail(email),enabled:Boolean(email)}); const userByName=useQuery({queryKey:['usuario','username',username],queryFn:()=>api.userByUsername(username),enabled:Boolean(username)}); if(sesion.estado!=='autenticado')return null; return <><SectionTitle eyebrow="Tu espacio personal" title="Mi cuenta" description="Consulta los datos asociados a tu perfil PulsePass."/><div className="surface max-w-3xl rounded-2xl p-6 sm:p-8">{user.isPending?<p className="text-slate-500">Cargando perfil…</p>:user.isError?<ErrorState error={user.error}/>:<div className="grid gap-6 sm:grid-cols-2">{[['Nombre',`${user.data.firstName??''} ${user.data.lastName??''}`],['Usuario',user.data.username],['Correo',user.data.email],['Teléfono',user.data.phone||'No registrado'],['Ciudad',user.data.city||'No registrada'],['Fecha de nacimiento',user.data.birthDate||'No registrada']].map(([label,value])=><div key={label}><p className="text-xs uppercase tracking-wider text-slate-500">{label}</p><p className="mt-2 font-semibold">{value}</p></div>)}</div>}<div className="mt-8 border-t border-slate-200 pt-5"><p className="text-xs text-slate-500">Roles de acceso</p><div className="mt-2 flex gap-2">{sesion.usuario.roles.map((role)=><Badge key={role} tone="brand">{role}</Badge>)}</div><p className="mt-4 text-xs text-slate-500">Validación adicional de usuario: {userByName.isSuccess?'Correcta':userByName.isPending?'Consultando…':'No disponible'}</p></div></div></> }

export function MisTickets() {
  const { sesion } = useAuth()
  const email = sesion.estado === 'autenticado' ? sesion.usuario.email : ''
  const q = useQuery({ queryKey: ['tickets', 'usuario', email], queryFn: () => api.myTickets(email), enabled: Boolean(email) })
  const client = useQueryClient()
  const [filter, setFilter] = useState('ALL')
  const [ticketToCancel, setTicketToCancel] = useState<Ticket | null>(null)
  const cancel = useMutation({
    mutationFn: (ticket: Ticket) => api.cancelTicket(ticket.ticketCode),
    onSuccess: async (updated) => {
      await client.invalidateQueries({ queryKey: ['tickets', 'usuario', email] })
      await client.invalidateQueries({ queryKey: ['ticket', updated.ticketCode] })
      await client.invalidateQueries({ queryKey: ['tickets', 'pagados', updated.eventCode] })
      setTicketToCancel(null)
    },
  })
  const tickets = (q.data ?? []).filter((ticket) => filter === 'ALL' || ticket.status === filter)
  return <>
    <SectionTitle eyebrow="Tus experiencias" title="Mis tickets" description="Consulta tus entradas y gestiona tus próximas experiencias." />
    <div className="mb-5 flex flex-wrap gap-2" aria-label="Filtrar tickets">
      {([['ALL', 'Todos'], ['PAID', 'Pagados'], ['RESERVED', 'Reservados'], ['USED', 'Usados'], ['CANCELLED', 'Cancelados']] as Array<[string, string]>).map(([value, label]) => <button key={value} type="button" aria-pressed={filter === value} onClick={() => setFilter(value)} className={`min-h-11 rounded-full px-4 py-2 text-xs font-semibold ${filter === value ? 'bg-violet-500 text-white' : 'border border-slate-200 text-slate-600 hover:bg-slate-100'}`}>{label}</button>)}
    </div>
    {q.isPending ? <div className="space-y-3"><div className="surface h-28 animate-pulse rounded-2xl" /><div className="surface h-28 animate-pulse rounded-2xl" /></div> : q.isError ? <ErrorState error={q.error} retry={() => void q.refetch()} /> : tickets.length ? <div className="space-y-3">{tickets.map((ticket) => <TicketRow key={ticket.ticketCode} ticket={ticket} onCancel={setTicketToCancel} />)}</div> : <EmptyState title="No hay tickets en esta categoría" description="Cuando compres una entrada, aparecerá aquí." />}
    <Modal open={Boolean(ticketToCancel)} title="Cancelar entrada" description="Esta acción solicita al sistema cancelar tu ticket. El backend confirmará si todavía cumple las reglas para cancelarse." onClose={() => { if (!cancel.isPending) { setTicketToCancel(null); cancel.reset() } }} footer={<><Button type="button" variant="secondary" disabled={cancel.isPending} onClick={() => { setTicketToCancel(null); cancel.reset() }}>Conservar entrada</Button><Button type="button" variant="danger" disabled={cancel.isPending || !ticketToCancel} onClick={() => ticketToCancel && cancel.mutate(ticketToCancel)}>{cancel.isPending ? 'Cancelando…' : 'Confirmar cancelación'}</Button></>}>{ticketToCancel && <div className="rounded-xl border border-slate-200 p-4 text-sm"><p className="font-semibold">{ticketToCancel.eventName}</p><p className="mt-1 text-slate-500">{ticketToCancel.ticketCode} · {ticketTypeNames[ticketToCancel.type]}</p></div>}<FormError error={cancel.error} /></Modal>
  </>
}

export function DetalleTicket() {
  const { ticketCode = '' } = useParams()
  const q = useQuery({ queryKey: ['ticket', ticketCode], queryFn: () => api.ticket(ticketCode), enabled: Boolean(ticketCode) })
  const client = useQueryClient()
  const [cancelOpen, setCancelOpen] = useState(false)
  const cancel = useMutation({
    mutationFn: () => api.cancelTicket(ticketCode),
    onSuccess: async (updated) => {
      await client.invalidateQueries({ queryKey: ['ticket', ticketCode] })
      await client.invalidateQueries({ queryKey: ['tickets', 'usuario'] })
      await client.invalidateQueries({ queryKey: ['tickets', 'pagados', updated.eventCode] })
      setCancelOpen(false)
    },
  })
  if (q.isPending) return <p className="py-20 text-center text-slate-500">Cargando entrada…</p>
  if (q.isError) return q.error instanceof ApiError && [403, 404].includes(q.error.status) ? <div className="surface mx-auto max-w-xl rounded-2xl p-8 text-center" role="alert"><h1 className="font-display text-2xl font-bold">Ticket no disponible</h1><p className="mt-2 text-sm text-slate-500">No encontramos esta entrada o no tienes permiso para consultarla.</p><Link to={RUTAS.misTickets} className="btn-primary mt-5">Volver a mis tickets</Link></div> : <ErrorState error={q.error} retry={() => void q.refetch()} />
  return <>
    <Link to={RUTAS.misTickets} className="text-sm font-semibold text-violet-700">← Mis tickets</Link>
    <div className="surface mx-auto mt-6 max-w-2xl overflow-hidden rounded-[2rem]">
      <div className="bg-gradient-to-r from-violet-700/90 to-cyan-700/70 p-8"><p className="eyebrow">Tu entrada PulsePass</p><h1 className="page-title mt-3 text-3xl text-white">{q.data.eventName}</h1><p className="mt-2 break-all text-sm text-white/80">{q.data.ticketCode}</p></div>
      <div className="space-y-5 p-8"><div className="flex items-center justify-between"><span className="text-sm text-slate-500">Estado</span><Badge tone={statusTone(q.data.status)}>{statusNames[q.data.status]}</Badge></div><div className="grid grid-cols-2 gap-5"><div><p className="text-xs text-slate-500">Tipo</p><p className="mt-1 font-semibold">{ticketTypeNames[q.data.type]}</p></div><div><p className="text-xs text-slate-500">Precio</p><p className="mt-1 font-semibold">{moneda(q.data.price)}</p></div><div><p className="text-xs text-slate-500">Código</p><p className="mt-1 break-all font-semibold">{q.data.ticketCode}</p></div><div><p className="text-xs text-slate-500">Compra</p><p className="mt-1 font-semibold">{fecha(q.data.purchaseDate)}</p></div></div>{q.data.status === 'PAID' && <Button variant="danger" disabled={cancel.isPending} onClick={() => setCancelOpen(true)}>Cancelar entrada</Button>}<p className="border-t border-slate-200 pt-4 text-xs text-slate-500">Esta vista es tu comprobante de compra. La validación de ingreso se realiza en el acceso del evento.</p></div>
    </div>
    <Modal open={cancelOpen} title="Cancelar entrada" description="Confirma si deseas solicitar la cancelación de este ticket." onClose={() => { if (!cancel.isPending) { setCancelOpen(false); cancel.reset() } }} footer={<><Button type="button" variant="secondary" disabled={cancel.isPending} onClick={() => setCancelOpen(false)}>Volver</Button><Button type="button" variant="danger" disabled={cancel.isPending} onClick={() => cancel.mutate()}>{cancel.isPending ? 'Cancelando…' : 'Confirmar cancelación'}</Button></>}> <p className="text-sm text-slate-600">{q.data.eventName} · {q.data.ticketCode}</p><FormError error={cancel.error}/></Modal>
  </>
}

export function ConfirmacionCompra() { const {ticketCode=''}=useParams(); const q=useQuery({queryKey:['ticket',ticketCode],queryFn:()=>api.ticket(ticketCode)}); if(q.isPending)return <p className="py-20 text-center text-slate-500">Confirmando tu compra…</p>;if(q.isError)return <ErrorState error={q.error}/>;return <div className="mx-auto max-w-2xl text-center"><div className="mx-auto grid h-20 w-20 place-items-center rounded-full border border-emerald-300/20 bg-emerald-400/10 text-4xl text-emerald-700 dark:text-emerald-300">✓</div><p className="eyebrow mt-6">Compra registrada</p><h1 className="page-title mt-2 text-4xl">¡Tu entrada está lista!</h1><p className="mt-4 text-slate-500">{q.data.eventName} · {ticketTypeNames[q.data.type]}</p><div className="surface mt-8 rounded-2xl p-7 text-left"><div className="flex justify-between gap-3"><span className="text-sm text-slate-500">Código de entrada</span><strong className="text-right">{q.data.ticketCode}</strong></div><div className="mt-4 flex justify-between"><span className="text-sm text-slate-500">Valor registrado</span><strong>{moneda(q.data.price)}</strong></div><div className="mt-4 flex justify-between"><span className="text-sm text-slate-500">Estado</span><Badge tone={statusTone(q.data.status)}>{statusNames[q.data.status]}</Badge></div></div><div className="mt-7 flex flex-wrap justify-center gap-3"><Link to={RUTAS.ticket(q.data.ticketCode)} className="btn-primary">Ver mi entrada</Link><Link to={RUTAS.misTickets} className="btn-secondary">Mis tickets</Link></div></div> }

const codeSchema=z.object({ticketCode:z.string().min(1,'Ingresa el código de la entrada')})
export function ValidacionIngreso() { const form=useForm<{ticketCode:string}>({resolver:zodResolver(codeSchema)});const [code,setCode]=useState('');const [validatedCode,setValidatedCode]=useState('');const [eventCode,setEventCode]=useState('');const navigate=useNavigate();const q=useQuery({queryKey:['ticket','staff',code],queryFn:()=>api.ticket(code),enabled:Boolean(code)});const client=useQueryClient();const useIt=useMutation({mutationFn:()=>api.useTicket(code),onSuccess:async()=>{setValidatedCode(code);await client.invalidateQueries({queryKey:['ticket','staff',code]});await client.invalidateQueries({queryKey:['tickets','pagados']})}});const submit=form.handleSubmit((v)=>setCode(v.ticketCode.trim()));return <><SectionTitle eyebrow="Operación · Staff" title="Validar ingreso" description="Busca el código de una entrada y confirma su uso en el acceso."/><form onSubmit={submit} className="surface flex flex-col gap-3 rounded-2xl p-5 sm:flex-row"><label className="sr-only" htmlFor="staff-ticket-code">Código del ticket</label><input id="staff-ticket-code" className="field flex-1" placeholder="Código de ticket" aria-invalid={Boolean(form.formState.errors.ticketCode)} aria-describedby={form.formState.errors.ticketCode ? 'staff-ticket-error' : undefined} {...form.register('ticketCode')}/><Button type="submit">Buscar entrada</Button></form><form onSubmit={(e)=>{e.preventDefault();if(eventCode.trim())navigate(RUTAS.ticketsEvento(eventCode.trim()))}} className="mt-3 flex flex-col gap-3 sm:flex-row"><input aria-label="Código de evento" className="field flex-1" placeholder="Código de evento para ver tickets pagados" value={eventCode} onChange={(e)=>setEventCode(e.target.value)}/><Button type="submit" variant="secondary">Ver tickets pagados</Button></form>{form.formState.errors.ticketCode&&<p id="staff-ticket-error" className="mt-2 text-sm text-rose-700 dark:text-rose-300" role="alert">{form.formState.errors.ticketCode.message}</p>}{q.isFetching && code && <p className="mt-6 text-slate-500" aria-live="polite">Buscando entrada…</p>}{q.isError&&<div className="mt-6"><ErrorState error={q.error}/></div>}{q.data&&<div className="surface mt-6 max-w-2xl rounded-2xl p-6">{validatedCode===code&&<p className="mb-4 rounded-xl border border-emerald-300/30 bg-emerald-50 p-3 text-sm font-semibold text-emerald-800 dark:border-emerald-400/20 dark:bg-emerald-400/10 dark:text-emerald-200" role="status" aria-live="polite">Ingreso registrado correctamente.</p>}<div className="flex items-start justify-between gap-3"><div><p className="eyebrow">Entrada encontrada</p><h2 className="mt-2 font-display text-2xl font-bold">{q.data.eventName}</h2><p className="mt-1 text-sm text-slate-500">{q.data.ticketCode} · {q.data.userEmail}</p></div><Badge tone={statusTone(q.data.status)}>{statusNames[q.data.status]}</Badge></div><div className="mt-6 grid grid-cols-2 gap-4 text-sm"><div><p className="text-slate-500">Tipo</p><p className="mt-1 font-semibold">{ticketTypeNames[q.data.type]}</p></div><div><p className="text-slate-500">Precio</p><p className="mt-1 font-semibold">{moneda(q.data.price)}</p></div></div>{q.data.status==='PAID'?<Button className="mt-6" disabled={useIt.isPending} onClick={()=>useIt.mutate()}>{useIt.isPending?'Validando…':'Marcar como utilizada'}</Button>:<p className="mt-6 text-sm text-amber-800 dark:text-amber-200">Esta entrada no está en estado pagado y no puede validarse desde aquí.</p>}<FormError error={useIt.error}/></div>}</> }

export function TicketsPagadosEvento() {
  const { eventCode = '' } = useParams()
  const client = useQueryClient()
  const q = useQuery({
    queryKey: ['tickets', 'pagados', eventCode],
    queryFn: () => api.paidTickets(eventCode),
    enabled: Boolean(eventCode),
    // La compra puede realizarse desde otra sesión/dispositivo.
    refetchInterval: 10_000,
    refetchOnWindowFocus: true,
  })
  const useIt = useMutation({
    mutationFn: (code: string) => api.useTicket(code),
    onSuccess: async (_ticket, code) => { await client.invalidateQueries({ queryKey: ['tickets', 'pagados', eventCode] }); await client.invalidateQueries({ queryKey: ['ticket', 'staff', code] }) },
  })
  return <>
    <SectionTitle
      eyebrow="Operación · Staff y administración"
      title="Tickets pagados"
      description={`Entradas registradas como PAID para el evento ${eventCode}. La lista se actualiza automáticamente.`}
      action={<Button variant="secondary" disabled={q.isFetching} onClick={() => void q.refetch()}>{q.isFetching ? 'Actualizando…' : 'Actualizar lista'}</Button>}
    />
    {q.isPending ? <p className="text-slate-500">Cargando tickets…</p> : q.isError ? <ErrorState error={q.error} retry={() => void q.refetch()} /> : <>
      <div className="surface mb-5 flex flex-wrap items-center justify-between gap-3 rounded-2xl p-5">
        <div><p className="text-sm text-slate-500">Total de tickets pagados</p><p className="mt-1 font-display text-3xl font-extrabold">{q.data.length}</p></div>
        <Badge tone="success">Solo estado PAID</Badge>
      </div>
      {q.data.length ? <>
        <div className="surface hidden overflow-hidden rounded-2xl md:block"><div className="overflow-x-auto"><table className="w-full text-left text-sm"><thead className="bg-slate-50 text-xs uppercase tracking-wide text-slate-500 dark:bg-slate-800"><tr><th className="px-5 py-4">Ticket</th><th className="px-5 py-4">Comprador</th><th className="px-5 py-4">Tipo</th><th className="px-5 py-4">Valor</th><th className="px-5 py-4">Estado</th><th className="px-5 py-4">Acción</th></tr></thead><tbody>{q.data.map((ticket) => <tr key={ticket.ticketCode} className="border-t border-slate-200 dark:border-slate-700"><td className="px-5 py-4"><p className="font-semibold">{ticket.ticketCode}</p><p className="mt-1 text-xs text-slate-500">{ticket.eventName}</p></td><td className="px-5 py-4 break-all">{ticket.userEmail}</td><td className="px-5 py-4">{ticketTypeNames[ticket.type]}</td><td className="px-5 py-4 font-semibold">{moneda(ticket.price)}</td><td className="px-5 py-4"><Badge tone={statusTone(ticket.status)}>{statusNames[ticket.status]}</Badge></td><td className="px-5 py-4">{ticket.status === 'PAID' && <Button disabled={useIt.isPending} onClick={() => useIt.mutate(ticket.ticketCode)}>Validar ingreso</Button>}</td></tr>)}</tbody></table></div></div>
        <div className="space-y-3 md:hidden">{q.data.map((ticket) => <article key={ticket.ticketCode} className="surface rounded-2xl p-5"><div className="flex items-start justify-between gap-3"><div className="min-w-0"><p className="break-all text-xs font-semibold text-slate-500">{ticket.ticketCode}</p><h3 className="mt-1 font-display font-bold">{ticket.eventName}</h3></div><Badge tone={statusTone(ticket.status)}>{statusNames[ticket.status]}</Badge></div><p className="mt-3 break-all text-sm text-slate-500">{ticket.userEmail}</p><div className="mt-3 flex items-center justify-between gap-3 text-sm"><span>{ticketTypeNames[ticket.type]}</span><strong>{moneda(ticket.price)}</strong></div>{ticket.status === 'PAID' && <Button className="mt-4 w-full" disabled={useIt.isPending} onClick={() => useIt.mutate(ticket.ticketCode)}>Validar ingreso</Button>}</article>)}</div>
      </> : <EmptyState title="No hay tickets pagados" description="Todavía no hay compras PAID para este evento. Verifica que estés consultando el mismo código de evento usado al comprar. Si una entrada ya fue validada, cambia a USED y deja de aparecer en esta lista." />}
    </>}
    <FormError error={useIt.error}/>
  </>
}

export function AdminEventos() {
  const [code, setCode] = useState('')
  const [searched, setSearched] = useState('')
  const [filter, setFilter] = useState('ALL')
  const allEvents = useQuery({ queryKey: ['eventos', 'admin'], queryFn: api.adminEvents })
  const search = useQuery({ queryKey: ['evento', 'admin', searched], queryFn: () => api.event(searched), enabled: Boolean(searched) })
  const visibleEvents = (allEvents.data ?? []).filter((event) => filter === 'ALL' || event.status === filter)
  return <>
    <SectionTitle eyebrow="Administración" title="Gestionar eventos" description="Consulta todos los eventos, incluidos los borradores, y administra su publicación." action={<div className="flex flex-wrap gap-2"><Link to={RUTAS.adminCatalogos} className="btn-secondary">Gestionar sedes y artistas</Link><Link to={RUTAS.adminNuevo} className="btn-primary">+ Crear evento</Link></div>} />
    <form onSubmit={(e) => { e.preventDefault(); setSearched(code.trim()) }} className="surface flex flex-col gap-3 rounded-2xl p-5 sm:flex-row">
      <input className="field flex-1" placeholder="Buscar por código exacto" value={code} onChange={(e) => setCode(e.target.value)} aria-label="Código del evento" />
      <Button type="submit" disabled={!code.trim()}>Buscar</Button>
    </form>
    {search.isFetching && <p className="mt-4 text-sm text-slate-500">Buscando evento…</p>}
    {search.isError && <div className="mt-4"><FormError error={search.error}/></div>}
    {search.data && <Link to={RUTAS.adminEvento(search.data.eventCode)} className="surface mt-4 block rounded-2xl p-5 hover:border-violet-300/30"><div className="flex items-center justify-between gap-3"><div><p className="font-bold">{search.data.name}</p><p className="mt-1 text-xs text-slate-500">{search.data.eventCode}</p></div><Badge tone={statusTone(search.data.status)}>{statusNames[search.data.status]}</Badge></div></Link>}
    <section className="mt-9">
      <div className="mb-4 flex flex-wrap items-end justify-between gap-3"><div><h2 className="font-display text-xl font-bold">Todos los eventos</h2><p className="mt-1 text-sm text-slate-500">{allEvents.data?.length ?? 0} eventos registrados</p></div><select aria-label="Filtrar por estado" className="field w-full sm:w-56" value={filter} onChange={(e) => setFilter(e.target.value)}><option value="ALL">Todos los estados</option>{['DRAFT','PUBLISHED','SOLD_OUT','CANCELLED','FINISHED'].map((status) => <option key={status} value={status}>{statusNames[status]}</option>)}</select></div>
      {allEvents.isPending ? <LoadingGrid/> : allEvents.isError ? <ErrorState error={allEvents.error} retry={() => void allEvents.refetch()}/> : visibleEvents.length ? <div className="grid gap-3">{visibleEvents.map((event) => <Link key={event.eventCode} to={RUTAS.adminEvento(event.eventCode)} className="surface flex flex-col gap-3 rounded-2xl p-4 transition hover:border-violet-300/40 sm:flex-row sm:items-center sm:justify-between"><div><p className="font-bold">{event.name}</p><p className="mt-1 text-xs text-slate-500">{event.eventCode} · {fecha(event.eventDate)} · {event.venueName}</p></div><div className="flex items-center gap-3"><Badge tone={statusTone(event.status)}>{statusNames[event.status]}</Badge><span className="text-sm font-semibold text-violet-700">Gestionar →</span></div></Link>)}</div> : <EmptyState title="No hay eventos en este estado" description="Crea un evento nuevo o selecciona otro filtro."/>}
    </section>
  </>
}

const createSchema=z.object({eventCode:z.string().min(1,'Código obligatorio'),name:z.string().min(1,'Nombre obligatorio'),description:z.string().max(2000,'Máximo 2000 caracteres'),category:EventCategorySchema,eventDate:z.string().min(1,'Fecha obligatoria'),minimumAge:z.number().int().min(0,'La edad mínima no puede ser negativa'),venueCode:z.string().min(1,'Selecciona una sede')})
type CreateValues=z.infer<typeof createSchema>
export function AdminCrearEvento() {
  const queryClient = useQueryClient()
  const venues = useQuery({ queryKey: ['sedes', 'activas'], queryFn: api.venues })
  const nav = useNavigate()
  const [serverError, setServerError] = useState<unknown>(null)
  const [showVenueForm, setShowVenueForm] = useState(false)
  const [venueDraft, setVenueDraft] = useState({ code: '', name: '', city: '', address: '', capacity: '100' })
  const [venueError, setVenueError] = useState<unknown>(null)
  const form = useForm<CreateValues>({ resolver: zodResolver(createSchema), defaultValues: { description: '', category: 'MUSIC', minimumAge: 0 } })
  const createVenue = useMutation({
    mutationFn: () => api.createVenue({
      code: venueDraft.code.trim().toUpperCase(), name: venueDraft.name.trim(), city: venueDraft.city.trim(),
      address: venueDraft.address.trim() || undefined, capacity: Number(venueDraft.capacity),
    }),
    onSuccess: async (venue) => {
      await queryClient.invalidateQueries({ queryKey: ['sedes', 'activas'] })
      form.setValue('venueCode', venue.code, { shouldDirty: true, shouldValidate: true })
      setVenueDraft({ code: '', name: '', city: '', address: '', capacity: '100' })
      setVenueError(null)
      setShowVenueForm(false)
    },
    onError: setVenueError,
  })
  const create = useMutation({
    mutationFn: api.createEvent,
    onSuccess: (event) => {
      void queryClient.invalidateQueries({ queryKey: ['eventos', 'admin'] })
      nav(RUTAS.adminEvento(event.eventCode))
    },
  })
  const submit = form.handleSubmit(async (values) => {
    setServerError(null)
    try { await create.mutateAsync({ ...values, eventDate: values.eventDate.length === 16 ? `${values.eventDate}:00` : values.eventDate }) }
    catch (error) {
      if (error instanceof ApiError && error.status === 400) {
        const keys = ['eventCode', 'name', 'description', 'category', 'eventDate', 'minimumAge', 'venueCode'] as const
        let mapped = false
        for (const key of keys) { const message = error.details[key]; if (message) { form.setError(key, { type: 'server', message }); mapped = true } }
        setServerError(mapped ? null : error)
      } else setServerError(error)
    }
  })
  const saveVenue = () => {
    setVenueError(null)
    if (!venueDraft.code.trim() || !venueDraft.name.trim() || !venueDraft.city.trim() || Number(venueDraft.capacity) < 1) {
      setVenueError(new Error('Completa el código, nombre, ciudad y una capacidad mayor que cero.'))
      return
    }
    createVenue.mutate()
  }

  return <>
    <Link to={RUTAS.adminEventos} className="text-sm font-semibold text-violet-700">← Administración</Link>
    <div className="surface mx-auto mt-5 max-w-3xl rounded-[2rem] p-7 sm:p-10">
      <p className="eyebrow">Administración · Nuevo</p>
      <h1 className="page-title mt-2 text-3xl">Crear evento</h1>
      <p className="mt-3 text-sm leading-6 text-slate-500">Completa la información principal. El evento se guardará como borrador y podrás asociar artistas y publicarlo después.</p>
      <form onSubmit={submit} className="mt-8 grid gap-5 sm:grid-cols-2">
        {([{ name: 'eventCode', label: 'Código del evento' }, { name: 'name', label: 'Nombre del evento' }] as const).map((field) => <label key={field.name} className="space-y-2 text-sm font-semibold text-slate-600">{field.label}<input className="field" placeholder={field.name === 'eventCode' ? 'CMF-2026' : 'Caribbean Music Fest'} {...form.register(field.name)} />{form.formState.errors[field.name] && <small className="text-rose-500">{form.formState.errors[field.name]?.message}</small>}</label>)}
        <label className="space-y-2 text-sm font-semibold text-slate-600">Categoría<select className="field" {...form.register('category')}>{EventCategorySchema.options.map((category) => <option key={category} value={category}>{categoryNames[category]}</option>)}</select></label>
        <label className="space-y-2 text-sm font-semibold text-slate-600">Fecha y hora<input type="datetime-local" className="field" {...form.register('eventDate')} />{form.formState.errors.eventDate && <small className="text-rose-500">{form.formState.errors.eventDate.message}</small>}</label>
        <label className="space-y-2 text-sm font-semibold text-slate-600">Edad mínima<input type="number" min="0" className="field" {...form.register('minimumAge', { valueAsNumber: true })} />{form.formState.errors.minimumAge && <small className="text-rose-500">{form.formState.errors.minimumAge.message}</small>}</label>
        <div className="space-y-2 text-sm font-semibold text-slate-600">
          <label htmlFor="event-venue">Sede</label>
          <div className="flex gap-2"><select id="event-venue" className="field min-w-0 flex-1" {...form.register('venueCode')}><option value="">Selecciona una sede</option>{(venues.data ?? []).map((venue) => <option key={venue.code} value={venue.code}>{venue.name} · {venue.city}</option>)}</select><Button type="button" variant="secondary" className="shrink-0" onClick={() => { setVenueError(null); setShowVenueForm((value) => !value) }}>{showVenueForm ? 'Cerrar' : '+ Nueva'}</Button></div>
          {venues.isError && <small className="text-rose-500">No se pudieron cargar las sedes.</small>}
          {!venues.isPending && !venues.isError && (venues.data?.length ?? 0) === 0 && <small className="text-amber-700">No hay sedes activas. Registra una para continuar.</small>}
          {form.formState.errors.venueCode && <small className="text-rose-500">{form.formState.errors.venueCode.message}</small>}
        </div>
        <label className="space-y-2 text-sm font-semibold text-slate-600 sm:col-span-2">Descripción<textarea rows={4} placeholder="Describe brevemente la experiencia…" className="field" {...form.register('description')} />{form.formState.errors.description && <small className="text-rose-500">{form.formState.errors.description.message}</small>}</label>
        {showVenueForm && <section className="sm:col-span-2 rounded-2xl border border-violet-300/40 bg-violet-50/60 p-5 sm:p-6">
          <p className="eyebrow">Nuevo registro</p><h2 className="mt-1 font-display text-xl font-bold text-slate-900">Crear sede sin salir del formulario</h2><p className="mt-2 text-sm leading-6 text-slate-600">Al guardar, la sede se seleccionará automáticamente para este evento.</p>
          <div className="mt-4 grid gap-3 sm:grid-cols-2">
            <label className="space-y-1 text-xs font-semibold text-slate-600">Código<input className="field" placeholder="VEN-SMR-01" value={venueDraft.code} onChange={(e) => setVenueDraft({ ...venueDraft, code: e.target.value })} /></label>
            <label className="space-y-1 text-xs font-semibold text-slate-600">Nombre<input className="field" placeholder="Marina Convention Center" value={venueDraft.name} onChange={(e) => setVenueDraft({ ...venueDraft, name: e.target.value })} /></label>
            <label className="space-y-1 text-xs font-semibold text-slate-600">Ciudad<input className="field" placeholder="Santa Marta" value={venueDraft.city} onChange={(e) => setVenueDraft({ ...venueDraft, city: e.target.value })} /></label>
            <label className="space-y-1 text-xs font-semibold text-slate-600">Dirección<input className="field" placeholder="Dirección de la sede" value={venueDraft.address} onChange={(e) => setVenueDraft({ ...venueDraft, address: e.target.value })} /></label>
            <label className="space-y-1 text-xs font-semibold text-slate-600 sm:col-span-2">Capacidad<input type="number" min="1" className="field" value={venueDraft.capacity} onChange={(e) => setVenueDraft({ ...venueDraft, capacity: e.target.value })} /></label>
          </div>
          <FormError error={venueError} />
          <div className="mt-4 flex flex-wrap gap-2"><Button type="button" disabled={createVenue.isPending} onClick={saveVenue}>{createVenue.isPending ? 'Guardando sede…' : 'Guardar y seleccionar sede'}</Button><Button type="button" variant="secondary" onClick={() => setShowVenueForm(false)}>Cancelar</Button></div>
        </section>}
        <div className="sm:col-span-2"><FormError error={serverError} /><Button type="submit" className="mt-2 w-full" disabled={create.isPending || createVenue.isPending || venues.isPending || (venues.data?.length ?? 0) === 0 && !form.getValues('venueCode')}>{create.isPending ? 'Creando evento…' : 'Crear borrador'}</Button></div>
      </form>
    </div>
  </>
}

const updateEventSchema = z.object({ name: z.string().min(1, 'Nombre obligatorio'), description: z.string().max(2000, 'Máximo 2000 caracteres'), category: EventCategorySchema, eventDate: z.string().min(1, 'Fecha obligatoria'), minimumAge: z.number().int().min(0, 'La edad mínima no puede ser negativa'), venueCode: z.string().min(1, 'Selecciona una sede') })
type UpdateEventValues = z.infer<typeof updateEventSchema>

export function AdminGestionEvento() {
  const { eventCode = '' } = useParams()
  const client = useQueryClient()
  const [publishOpen, setPublishOpen] = useState(false)
  const [editOpen, setEditOpen] = useState(false)
  const [statusOpen, setStatusOpen] = useState(false)
  const [statusTarget, setStatusTarget] = useState('')
  const [artistId, setArtistId] = useState('')
  const q = useQuery({ queryKey: ['evento', eventCode], queryFn: () => api.event(eventCode) })
  const artists = useQuery({ queryKey: ['artistas', 'activos'], queryFn: api.artists })
  const venues = useQuery({ queryKey: ['sedes', 'activas'], queryFn: api.venues })
  const form = useForm<UpdateEventValues>({ resolver: zodResolver(updateEventSchema) })
  const refresh = () => {
    void client.invalidateQueries({ queryKey: ['evento', eventCode] })
    void client.invalidateQueries({ queryKey: ['eventos', 'publicados'] })
    void client.invalidateQueries({ queryKey: ['eventos', 'admin'] })
  }
  const publish = useMutation({ mutationFn: () => api.publishEvent(eventCode), onSuccess: () => { setPublishOpen(false); refresh() } })
  const update = useMutation({ mutationFn: (values: UpdateEventValues) => api.updateEvent(eventCode, { ...values, eventDate: values.eventDate.length === 16 ? `${values.eventDate}:00` : values.eventDate }), onSuccess: () => { setEditOpen(false); refresh() } })
  const changeStatus = useMutation({ mutationFn: () => api.changeEventStatus(eventCode, statusTarget), onSuccess: () => { setStatusOpen(false); refresh() } })
  const add = useMutation({ mutationFn: () => api.addArtist(eventCode, Number(artistId)), onSuccess: () => { refresh(); setArtistId('') } })
  if (q.isPending) return <p className="py-20 text-center text-slate-500">Cargando evento…</p>
  if (q.isError) return <ErrorState error={q.error} />
  const event = q.data
  const editable = event.status !== 'CANCELLED' && event.status !== 'FINISHED'
  const openEdit = () => {
    form.reset({ name: event.name, description: event.description ?? '', category: event.category, eventDate: event.eventDate.slice(0, 16), minimumAge: event.minimumAge, venueCode: event.venueCode })
    update.reset()
    setEditOpen(true)
  }
  const submitEdit = form.handleSubmit((values) => update.mutate(values))
  return <>
    <Link to={RUTAS.adminEventos} className="text-sm text-violet-700">← Administración</Link>
    <div className="surface mt-5 rounded-[2rem] p-7 sm:p-10">
      <div className="flex flex-wrap items-start justify-between gap-4"><div><p className="eyebrow">Gestionar evento</p><h1 className="page-title mt-2 text-3xl">{event.name}</h1><p className="mt-2 text-sm text-slate-500">{event.eventCode} · {fecha(event.eventDate)}</p></div><Badge tone={statusTone(event.status)}>{statusNames[event.status]}</Badge></div>
      <div className="mt-8 grid gap-4 sm:grid-cols-3"><div><p className="text-xs text-slate-500">Categoría</p><p className="mt-1 font-semibold">{categoryNames[event.category]}</p></div><div><p className="text-xs text-slate-500">Sede</p><p className="mt-1 font-semibold">{event.venueName}</p></div><div><p className="text-xs text-slate-500">Edad mínima</p><p className="mt-1 font-semibold">{event.minimumAge} años</p></div></div>
      <div className="mt-8 flex flex-wrap gap-3">{editable && <Button variant="secondary" onClick={openEdit}>Editar evento</Button>}{event.status === 'DRAFT' && <Button disabled={publish.isPending} onClick={() => setPublishOpen(true)}>Publicar evento</Button>}{(event.status === 'PUBLISHED' || event.status === 'SOLD_OUT') && <Button variant="secondary" onClick={() => { setStatusTarget(''); changeStatus.reset(); setStatusOpen(true) }}>Cambiar estado</Button>}<Link to={RUTAS.ticketsEvento(eventCode)} className="btn-secondary">Ver tickets pagados</Link></div>
      <FormError error={publish.error} />
      <Modal open={publishOpen} title="Publicar evento" description="El evento quedará visible en la cartelera pública si el servidor acepta la operación." onClose={() => { if (!publish.isPending) setPublishOpen(false) }} footer={<><Button type="button" variant="secondary" disabled={publish.isPending} onClick={() => setPublishOpen(false)}>Volver</Button><Button type="button" disabled={publish.isPending} onClick={() => publish.mutate()}>{publish.isPending ? 'Publicando…' : 'Confirmar publicación'}</Button></>}><p className="text-sm font-semibold">{event.name} · {event.eventCode}</p><FormError error={publish.error} /></Modal>
      <Modal open={editOpen} title="Editar evento" description="Actualiza los datos del evento. El código y el estado se conservan." onClose={() => { if (!update.isPending) setEditOpen(false) }} footer={<><Button type="button" variant="secondary" disabled={update.isPending} onClick={() => setEditOpen(false)}>Cancelar</Button><Button type="submit" form="edit-event-form" disabled={update.isPending}>{update.isPending ? 'Guardando…' : 'Guardar cambios'}</Button></>}>
        <form id="edit-event-form" onSubmit={submitEdit} className="grid gap-4 sm:grid-cols-2">
          <label className="space-y-2 text-sm font-semibold text-slate-600">Nombre<input className="field" {...form.register('name')} />{form.formState.errors.name && <small className="text-rose-500">{form.formState.errors.name.message}</small>}</label>
          <label className="space-y-2 text-sm font-semibold text-slate-600">Categoría<select className="field" {...form.register('category')}>{EventCategorySchema.options.map((category) => <option key={category} value={category}>{categoryNames[category]}</option>)}</select></label>
          <label className="space-y-2 text-sm font-semibold text-slate-600">Fecha y hora<input type="datetime-local" className="field" {...form.register('eventDate')} />{form.formState.errors.eventDate && <small className="text-rose-500">{form.formState.errors.eventDate.message}</small>}</label>
          <label className="space-y-2 text-sm font-semibold text-slate-600">Edad mínima<input type="number" min="0" className="field" {...form.register('minimumAge', { valueAsNumber: true })} />{form.formState.errors.minimumAge && <small className="text-rose-500">{form.formState.errors.minimumAge.message}</small>}</label>
          <label className="space-y-2 text-sm font-semibold text-slate-600 sm:col-span-2">Sede<select className="field" {...form.register('venueCode')}>{(venues.data ?? []).map((venue) => <option key={venue.code} value={venue.code}>{venue.name} · {venue.city}</option>)}</select>{form.formState.errors.venueCode && <small className="text-rose-500">{form.formState.errors.venueCode.message}</small>}</label>
          <label className="space-y-2 text-sm font-semibold text-slate-600 sm:col-span-2">Descripción<textarea rows={4} className="field" {...form.register('description')} />{form.formState.errors.description && <small className="text-rose-500">{form.formState.errors.description.message}</small>}</label>
          <div className="sm:col-span-2"><FormError error={update.error} /></div>
        </form>
      </Modal>
      <Modal open={statusOpen} title="Cambiar estado del evento" description="Selecciona el nuevo estado." onClose={() => { if (!changeStatus.isPending) setStatusOpen(false) }} footer={<><Button type="button" variant="secondary" disabled={changeStatus.isPending} onClick={() => setStatusOpen(false)}>Volver</Button><Button type="button" disabled={!statusTarget || changeStatus.isPending} onClick={() => changeStatus.mutate()}>{changeStatus.isPending ? 'Guardando…' : 'Guardar estado'}</Button></>}><label className="block space-y-2 text-sm font-semibold text-slate-600">Nuevo estado<select className="field" value={statusTarget} onChange={(e) => setStatusTarget(e.target.value)}><option value="">Selecciona un estado</option>{(event.status === 'PUBLISHED' ? ['SOLD_OUT', 'FINISHED', 'CANCELLED'] : ['FINISHED', 'CANCELLED']).map((status) => <option key={status} value={status}>{statusNames[status]}</option>)}</select></label><FormError error={changeStatus.error} /></Modal>
      <div className="mt-10 border-t border-slate-200 pt-8"><div className="flex flex-wrap items-center justify-between gap-3"><h2 className="font-display text-xl font-bold">Artistas asociados</h2><Link to={RUTAS.adminCatalogos} className="text-sm font-semibold text-violet-700">+ Crear artista</Link></div>{event.artists.length ? <div className="mt-4 flex flex-wrap gap-2">{event.artists.map((a) => <Badge key={a.id} tone="brand">{a.stageName}</Badge>)}</div> : <p className="mt-2 text-sm text-slate-500">Aún no hay artistas asociados.</p>}<div className="mt-5 flex flex-col gap-3 sm:flex-row"><select className="field flex-1" value={artistId} onChange={(e) => setArtistId(e.target.value)}><option value="">Selecciona un artista</option>{(artists.data ?? []).filter((a) => !event.artists.some((existing) => existing.id === a.id)).map((a) => <option key={a.id} value={a.id}>{a.stageName}</option>)}</select><Button disabled={!artistId || add.isPending} onClick={() => add.mutate()}>{add.isPending ? 'Asociando…' : 'Asociar artista'}</Button></div>{(artists.data ?? []).filter((a) => !event.artists.some((existing) => existing.id === a.id)).length === 0 && <p className="mt-2 text-xs text-slate-500">No hay artistas activos disponibles para asociar. <Link to={RUTAS.adminCatalogos} className="font-semibold text-violet-700">Registra uno aquí.</Link></p>}<FormError error={add.error} /></div>
    </div>
  </>
}
export function AdminCatalogos() {
  const client = useQueryClient()
  const venues = useQuery({ queryKey: ['sedes','activas'], queryFn: api.venues })
  const artists = useQuery({ queryKey: ['artistas','activos'], queryFn: api.artists })
  const [venue, setVenue] = useState({ code: '', name: '', city: '', address: '', capacity: '100' })
  const [artist, setArtist] = useState({ stageName: '', country: '', genre: '' })
  const [venueError, setVenueError] = useState<unknown>(null)
  const [artistError, setArtistError] = useState<unknown>(null)
  const saveVenue = useMutation({ mutationFn: () => api.createVenue({ ...venue, code: venue.code.trim().toUpperCase(), name: venue.name.trim(), city: venue.city.trim(), address: venue.address.trim() || undefined, capacity: Number(venue.capacity) }), onSuccess: async () => { setVenue({ code: '', name: '', city: '', address: '', capacity: '100' }); setVenueError(null); await client.invalidateQueries({ queryKey: ['sedes','activas'] }) }, onError: setVenueError })
  const saveArtist = useMutation({ mutationFn: () => api.createArtist({ stageName: artist.stageName.trim(), country: artist.country.trim() || undefined, genre: artist.genre.trim() || undefined }), onSuccess: async () => { setArtist({ stageName: '', country: '', genre: '' }); setArtistError(null); await client.invalidateQueries({ queryKey: ['artistas','activos'] }) }, onError: setArtistError })
  return <><Link to={RUTAS.adminEventos} className="text-sm text-violet-700">← Administración</Link><SectionTitle eyebrow="Administración · Catálogos" title="Sedes y artistas" description="Registra los espacios y artistas que luego podrás seleccionar al crear y gestionar eventos." />
    <div className="grid gap-6 lg:grid-cols-2">
      <section className="surface rounded-[2rem] p-6 sm:p-8"><h2 className="font-display text-xl font-bold">Registrar sede</h2><p className="mt-2 text-sm text-slate-500">La sede quedará activa y disponible para los nuevos eventos.</p><form className="mt-6 grid gap-4" onSubmit={(e)=>{e.preventDefault();setVenueError(null);saveVenue.mutate()}}>
        <label className="space-y-2 text-sm text-slate-600">Código único<input required maxLength={50} className="field" placeholder="VEN-SMR-01" value={venue.code} onChange={e=>setVenue({...venue,code:e.target.value})}/></label>
        <label className="space-y-2 text-sm text-slate-600">Nombre de la sede<input required maxLength={120} className="field" placeholder="Marina Convention Center" value={venue.name} onChange={e=>setVenue({...venue,name:e.target.value})}/></label>
        <label className="space-y-2 text-sm text-slate-600">Ciudad<input required maxLength={100} className="field" placeholder="Santa Marta" value={venue.city} onChange={e=>setVenue({...venue,city:e.target.value})}/></label>
        <label className="space-y-2 text-sm text-slate-600">Dirección<input maxLength={200} className="field" placeholder="Dirección de la sede" value={venue.address} onChange={e=>setVenue({...venue,address:e.target.value})}/></label>
        <label className="space-y-2 text-sm text-slate-600">Capacidad<input required type="number" min="1" className="field" value={venue.capacity} onChange={e=>setVenue({...venue,capacity:e.target.value})}/></label>
        <FormError error={venueError}/><Button type="submit" disabled={saveVenue.isPending}>{saveVenue.isPending?'Guardando…':'Guardar sede'}</Button>
      </form><div className="mt-7 border-t border-slate-200 pt-5"><h3 className="font-semibold">Sedes activas ({venues.data?.length??0})</h3>{venues.isError?<FormError error={venues.error}/>:<ul className="mt-3 space-y-2 text-sm">{(venues.data??[]).map(v=><li key={v.code} className="flex justify-between gap-3"><span>{v.name} · {v.city}</span><span className="text-slate-500">{v.code}</span></li>)}</ul>}</div></section>
      <section className="surface rounded-[2rem] p-6 sm:p-8"><h2 className="font-display text-xl font-bold">Registrar artista</h2><p className="mt-2 text-sm text-slate-500">El artista quedará activo y aparecerá en la pantalla de asociación del evento.</p><form className="mt-6 grid gap-4" onSubmit={(e)=>{e.preventDefault();setArtistError(null);saveArtist.mutate()}}>
        <label className="space-y-2 text-sm text-slate-600">Nombre artístico<input required maxLength={100} className="field" placeholder="Solar Beat" value={artist.stageName} onChange={e=>setArtist({...artist,stageName:e.target.value})}/></label>
        <label className="space-y-2 text-sm text-slate-600">País<input maxLength={100} className="field" placeholder="Colombia" value={artist.country} onChange={e=>setArtist({...artist,country:e.target.value})}/></label>
        <label className="space-y-2 text-sm text-slate-600">Género / estilo<input maxLength={50} className="field" placeholder="Electrónica" value={artist.genre} onChange={e=>setArtist({...artist,genre:e.target.value})}/></label>
        <FormError error={artistError}/><Button type="submit" disabled={saveArtist.isPending}>{saveArtist.isPending?'Guardando…':'Guardar artista'}</Button>
      </form><div className="mt-7 border-t border-slate-200 pt-5"><h3 className="font-semibold">Artistas activos ({artists.data?.length??0})</h3>{artists.isError?<FormError error={artists.error}/>:<ul className="mt-3 space-y-2 text-sm">{(artists.data??[]).map(a=><li key={a.id} className="flex justify-between gap-3"><span>{a.stageName}</span><span className="text-slate-500">{a.genre||a.country||'Artista'}</span></li>)}</ul>}</div></section>
    </div>
  </>
}

export function SinPermisos() { return <div className="mx-auto max-w-xl py-16 text-center"><p className="eyebrow">403 · Acceso restringido</p><h1 className="page-title mt-3 text-4xl">Esta zona no es para todos.</h1><p className="mt-4 text-slate-500">Tu cuenta no tiene el rol necesario para acceder a esta página.</p><Link to={RUTAS.inicio} className="btn-primary mt-7">Volver al inicio</Link></div> }
export function NoEncontrada() { return <div className="mx-auto max-w-xl py-16 text-center"><p className="eyebrow">404 · Página no encontrada</p><h1 className="page-title mt-3 text-4xl">Parece que tomaste otra ruta.</h1><p className="mt-4 text-slate-500">El contenido que buscas no está disponible o cambió de dirección.</p><Link to={RUTAS.inicio} className="btn-primary mt-7">Ir al inicio</Link></div> }
export function ErrorPagina() { return <div className="mx-auto max-w-xl py-16"><ErrorState error={new Error('No se pudo cargar esta página.')} retry={()=>window.location.reload()}/></div> }
