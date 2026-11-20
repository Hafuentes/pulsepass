import { useState } from 'react'
import { Link, useNavigate } from 'react-router'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useActiveVenues, useCreateVenueMutation } from '../features/venues/hooks'
import { useCreateEventMutation } from '../features/events/hooks'
import { ApiError } from '../lib/http'
import { RUTAS } from '../lib/rutas'
import { EventCategorySchema } from '../lib/schemas'
import { Button, FormError } from '../components/UI'
import { categoryNames } from '../lib/dominio'

const createSchema=z.object({eventCode:z.string().min(1,'Código obligatorio'),name:z.string().min(1,'Nombre obligatorio'),description:z.string().max(2000,'Máximo 2000 caracteres'),category:EventCategorySchema,eventDate:z.string().min(1,'Fecha obligatoria'),minimumAge:z.number().int().min(0,'La edad mínima no puede ser negativa'),venueCode:z.string().min(1,'Selecciona una sede')})
type CreateValues=z.infer<typeof createSchema>
export default function AdminCrearEvento() {
  const venues = useActiveVenues()
  const nav = useNavigate()
  const [serverError, setServerError] = useState<unknown>(null)
  const [showVenueForm, setShowVenueForm] = useState(false)
  const [venueDraft, setVenueDraft] = useState({ code: '', name: '', city: '', address: '', capacity: '100' })
  const [venueError, setVenueError] = useState<unknown>(null)
  const form = useForm<CreateValues>({ resolver: zodResolver(createSchema), defaultValues: { description: '', category: 'MUSIC', minimumAge: 0 } })
  const createVenue = useCreateVenueMutation()
  const create = useCreateEventMutation()
  const submit = form.handleSubmit(async (values) => {
    setServerError(null)
    try { const created = await create.mutateAsync({ ...values, eventDate: values.eventDate.length === 16 ? `${values.eventDate}:00` : values.eventDate }); nav(RUTAS.adminEvento(created.eventCode)) }
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
    createVenue.mutate({ code: venueDraft.code.trim().toUpperCase(), name: venueDraft.name.trim(), city: venueDraft.city.trim(), address: venueDraft.address.trim() || undefined, capacity: Number(venueDraft.capacity) }, { onSuccess: (venue) => { form.setValue('venueCode', venue.code, { shouldDirty: true, shouldValidate: true }); setVenueDraft({ code: '', name: '', city: '', address: '', capacity: '100' }); setVenueError(null); setShowVenueForm(false) }, onError: setVenueError })
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
