import { useState } from 'react'
import { Link } from 'react-router'
import { useAdminEvents, useAdminEvent } from '../features/events/hooks'
import { fecha } from '../lib/formato'
import { RUTAS } from '../lib/rutas'
import { Badge, Button, EmptyState, ErrorState, LoadingGrid, SectionTitle, FormError } from '../components/UI'
import { statusTone, statusNames } from '../lib/dominio'

export default function AdminEventos() {
  const [code, setCode] = useState('')
  const [searched, setSearched] = useState('')
  const [filter, setFilter] = useState('ALL')
  const allEvents = useAdminEvents()
  const search = useAdminEvent(searched)
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
