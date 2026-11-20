import { useState } from 'react'
import { Link, useParams } from 'react-router'
import { zodResolver } from '@hookform/resolvers/zod'
import { useForm } from 'react-hook-form'
import { z } from 'zod'
import { useEvent, usePublishEventMutation, useChangeEventStatusMutation, useAssociateArtistMutation, useRemoveArtistMutation, useUpdateEventMutation } from '../features/events/hooks'
import { useActiveArtists } from '../features/artists/hooks'
import { useActiveVenues } from '../features/venues/hooks'
import { TicketsPagadosEventoPanel } from './TicketsPagadosEvento'
import { fecha } from '../lib/formato'
import { RUTAS } from '../lib/rutas'
import { EventCategorySchema } from '../lib/schemas'
import { Badge, Button, ErrorState, Modal, FormError } from '../components/UI'
import { categoryNames, statusTone, statusNames } from '../lib/dominio'

const updateEventSchema = z.object({
  name: z.string().min(1, 'El nombre es obligatorio'),
  description: z.string().max(2000, 'Máximo 2000 caracteres'),
  category: EventCategorySchema,
  eventDate: z.string().min(1, 'La fecha es obligatoria'),
  minimumAge: z.number().int().min(0, 'La edad mínima no puede ser negativa'),
  venueCode: z.string().min(1, 'Selecciona una sede'),
})
type UpdateEventValues = z.infer<typeof updateEventSchema>

export default function AdminGestionEvento() {
  const { eventCode = '' } = useParams()
  const [publishOpen, setPublishOpen] = useState(false)
  const [statusOpen, setStatusOpen] = useState(false)
  const [statusTarget, setStatusTarget] = useState<'' | 'SOLD_OUT' | 'FINISHED' | 'CANCELLED'>('')
  const [editOpen, setEditOpen] = useState(false)
  const [ticketsOpen, setTicketsOpen] = useState(false)
  const [artistId, setArtistId] = useState('')
  const q = useEvent(eventCode)
  const artists = useActiveArtists()
  const venues = useActiveVenues()
  const publish = usePublishEventMutation(eventCode)
  const changeStatus = useChangeEventStatusMutation(eventCode)
  const update = useUpdateEventMutation(eventCode)
  const add = useAssociateArtistMutation(eventCode)
  const removeArtist = useRemoveArtistMutation(eventCode)
  const form = useForm<UpdateEventValues>({ resolver: zodResolver(updateEventSchema) })

  if (q.isPending) return <p className="py-20 text-center text-slate-500">Cargando evento…</p>
  if (q.isError) return <ErrorState error={q.error} />

  const event = q.data
  const editable = event.status !== 'CANCELLED' && event.status !== 'FINISHED'
  const canChangeStatus = event.status === 'PUBLISHED' || event.status === 'SOLD_OUT'
  const statusOptions = event.status === 'PUBLISHED'
    ? [{ value: 'SOLD_OUT' as const, label: 'Agotado' }, { value: 'FINISHED' as const, label: 'Finalizado' }, { value: 'CANCELLED' as const, label: 'Cancelado' }]
    : [{ value: 'FINISHED' as const, label: 'Finalizado' }, { value: 'CANCELLED' as const, label: 'Cancelado' }]
  const openEdit = () => {
    form.reset({
      name: event.name,
      description: event.description ?? '',
      category: event.category,
      eventDate: event.eventDate.slice(0, 16),
      minimumAge: event.minimumAge,
      venueCode: event.venueCode,
    })
    update.reset()
    setEditOpen(true)
  }
  const submitEdit = form.handleSubmit((values) => update.mutate({
    ...values,
    eventDate: values.eventDate.length === 16 ? `${values.eventDate}:00` : values.eventDate,
  }, { onSuccess: () => setEditOpen(false) }))

  return <>
    <Link to={RUTAS.adminEventos} className="text-sm text-violet-700">← Administración</Link>
    <div className="surface mt-5 rounded-[2rem] p-7 sm:p-10">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div><p className="eyebrow">Gestionar evento</p><h1 className="page-title mt-2 text-3xl">{event.name}</h1><p className="mt-2 text-sm text-slate-500">{event.eventCode} · {fecha(event.eventDate)}</p></div>
        <Badge tone={statusTone(event.status)}>{statusNames[event.status]}</Badge>
      </div>
      <div className="mt-8 grid gap-4 sm:grid-cols-3">
        <div><p className="text-xs text-slate-500">Categoría</p><p className="mt-1 font-semibold">{categoryNames[event.category]}</p></div>
        <div><p className="text-xs text-slate-500">Sede</p><p className="mt-1 font-semibold">{event.venueName}</p></div>
        <div><p className="text-xs text-slate-500">Edad mínima</p><p className="mt-1 font-semibold">{event.minimumAge} años</p></div>
      </div>
      <div className="mt-8 flex flex-wrap gap-3">
        {editable && <Button variant="secondary" onClick={openEdit}>Editar evento</Button>}
        {event.status === 'DRAFT' && <Button disabled={publish.isPending} onClick={() => setPublishOpen(true)}>Publicar evento</Button>}
        {canChangeStatus && <Button variant="secondary" onClick={() => { setStatusTarget(''); changeStatus.reset(); setStatusOpen(true) }}>Cambiar estado</Button>}
        <Button variant="secondary" onClick={() => setTicketsOpen(true)}>Tickets pagados</Button>
      </div>
      <FormError error={publish.error} />

      <Modal open={publishOpen} title="Publicar evento" description="El evento quedará visible en la cartelera pública si el servidor acepta la operación." onClose={() => { if (!publish.isPending) setPublishOpen(false) }} footer={<><Button type="button" variant="secondary" disabled={publish.isPending} onClick={() => setPublishOpen(false)}>Volver</Button><Button type="button" disabled={publish.isPending} onClick={() => publish.mutate(undefined, { onSuccess: () => setPublishOpen(false) })}>{publish.isPending ? 'Publicando…' : 'Confirmar publicación'}</Button></>}>
        <p className="text-sm font-semibold">{event.name} · {event.eventCode}</p><FormError error={publish.error} />
      </Modal>

      <Modal open={statusOpen} title="Cambiar estado del evento" description="Selecciona el nuevo estado. No podrás reabrir un evento finalizado o cancelado desde esta pantalla." onClose={() => { if (!changeStatus.isPending) setStatusOpen(false) }} footer={<><Button type="button" variant="secondary" disabled={changeStatus.isPending} onClick={() => setStatusOpen(false)}>Volver</Button><Button type="button" disabled={!statusTarget || changeStatus.isPending} onClick={() => statusTarget && changeStatus.mutate(statusTarget, { onSuccess: () => setStatusOpen(false) })}>{changeStatus.isPending ? 'Guardando…' : 'Guardar estado'}</Button></>}>
        <label className="block space-y-2 text-sm font-semibold text-slate-600">Nuevo estado<select className="field" value={statusTarget} onChange={(e) => setStatusTarget(e.target.value as typeof statusTarget)}><option value="">Selecciona un estado</option>{statusOptions.map((option) => <option key={option.value} value={option.value}>{option.label}</option>)}</select></label>
        {statusTarget === 'SOLD_OUT' && <p className="mt-3 text-sm text-slate-500">El evento se marcará como agotado y no aceptará más compras.</p>}
        {statusTarget === 'FINISHED' && <p className="mt-3 text-sm text-slate-500">El evento quedará finalizado.</p>}
        {statusTarget === 'CANCELLED' && <p className="mt-3 text-sm text-slate-500">El evento dejará de aceptar compras. Las entradas existentes se conservan, no podrán validarse y esta aplicación no procesa reembolsos.</p>}
        <div className="mt-4"><FormError error={changeStatus.error} /></div>
      </Modal>

      <Modal open={editOpen} title="Editar evento" description="Actualiza los datos del evento. El código y el estado se conservan." onClose={() => { if (!update.isPending) setEditOpen(false) }} footer={<><Button type="button" variant="secondary" disabled={update.isPending} onClick={() => setEditOpen(false)}>Cancelar</Button><Button type="submit" form="edit-event-form" disabled={update.isPending}>{update.isPending ? 'Guardando…' : 'Guardar cambios'}</Button></>}>
        <form id="edit-event-form" onSubmit={submitEdit} className="grid gap-4 sm:grid-cols-2">
          <label className="space-y-2 text-sm font-semibold text-slate-600">Nombre<input className="field" {...form.register('name')} />{form.formState.errors.name && <small className="text-rose-500">{form.formState.errors.name.message}</small>}</label>
          <label className="space-y-2 text-sm font-semibold text-slate-600">Categoría<select className="field" {...form.register('category')}>{EventCategorySchema.options.map((category) => <option key={category} value={category}>{categoryNames[category]}</option>)}</select></label>
          <label className="space-y-2 text-sm font-semibold text-slate-600">Fecha y hora<input type="datetime-local" className="field" {...form.register('eventDate')} />{form.formState.errors.eventDate && <small className="text-rose-500">{form.formState.errors.eventDate.message}</small>}</label>
          <label className="space-y-2 text-sm font-semibold text-slate-600">Edad mínima<input type="number" min="0" className="field" {...form.register('minimumAge', { valueAsNumber: true })} />{form.formState.errors.minimumAge && <small className="text-rose-500">{form.formState.errors.minimumAge.message}</small>}</label>
          <label className="space-y-2 text-sm font-semibold text-slate-600 sm:col-span-2">Sede<select className="field" {...form.register('venueCode')}>{(venues.data ?? []).map((venue) => <option key={venue.code} value={venue.code}>{venue.name} · {venue.city}</option>)}</select>{form.formState.errors.venueCode && <small className="text-rose-500">{form.formState.errors.venueCode.message}</small>}{venues.isError && <small className="text-rose-500">No se pudieron cargar las sedes.</small>}</label>
          <label className="space-y-2 text-sm font-semibold text-slate-600 sm:col-span-2">Descripción<textarea rows={4} className="field" {...form.register('description')} />{form.formState.errors.description && <small className="text-rose-500">{form.formState.errors.description.message}</small>}</label>
          <div className="sm:col-span-2"><FormError error={update.error} /></div>
        </form>
      </Modal>

      <Modal open={ticketsOpen} title="Tickets pagados" description={`Entradas del evento ${eventCode}. La lista se actualiza automáticamente.`} onClose={() => setTicketsOpen(false)}>
        <TicketsPagadosEventoPanel eventCode={eventCode} inModal />
      </Modal>

      <div className="mt-10 border-t border-slate-200 pt-8">
        <div className="flex flex-wrap items-center justify-between gap-3"><h2 className="font-display text-xl font-bold">Artistas asociados</h2><Link to={RUTAS.adminCatalogos} className="text-sm font-semibold text-violet-700">+ Crear artista</Link></div>
        {event.artists.length ? <div className="mt-4 flex flex-wrap gap-2">{event.artists.map((artist) => <Badge key={artist.id} tone="brand"><span className="group inline-flex items-center gap-1">{artist.stageName}<button type="button" aria-label={`Quitar ${artist.stageName} del evento`} title={`Quitar ${artist.stageName}`} disabled={removeArtist.isPending} onClick={() => removeArtist.mutate(artist.id)} className="rounded-full px-1 text-violet-800/70 opacity-0 transition hover:bg-rose-100 hover:text-rose-700 focus:opacity-100 focus:outline-none focus:ring-2 focus:ring-violet-500 group-hover:opacity-100 group-focus-within:opacity-100 max-md:opacity-100 disabled:opacity-50">×</button></span></Badge>)}</div> : <p className="mt-2 text-sm text-slate-500">Aún no hay artistas asociados.</p>}
        <div className="mt-5 flex flex-col gap-3 sm:flex-row"><select className="field flex-1" value={artistId} onChange={(e) => setArtistId(e.target.value)}><option value="">Selecciona un artista</option>{(artists.data ?? []).filter((artist) => !event.artists.some((existing) => existing.id === artist.id)).map((artist) => <option key={artist.id} value={artist.id}>{artist.stageName}</option>)}</select><Button disabled={!artistId || add.isPending} onClick={() => add.mutate(Number(artistId), { onSuccess: () => setArtistId('') })}>{add.isPending ? 'Asociando…' : 'Asociar artista'}</Button></div>
        {(artists.data ?? []).filter((artist) => !event.artists.some((existing) => existing.id === artist.id)).length === 0 && <p className="mt-2 text-xs text-slate-500">No hay artistas activos disponibles para asociar. <Link to={RUTAS.adminCatalogos} className="font-semibold text-violet-700">Registra uno aquí.</Link></p>}
        <FormError error={add.error} /><FormError error={removeArtist.error} />
      </div>
    </div>
  </>
}
