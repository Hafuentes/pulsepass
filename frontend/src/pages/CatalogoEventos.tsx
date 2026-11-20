import { useSearchParams } from 'react-router'
import { usePublishedEvents, useEventsByArtist } from '../features/events/hooks'
import { useActiveArtists, useArtistByName } from '../features/artists/hooks'
import { EventCategorySchema } from '../lib/schemas'
import { EmptyState, ErrorState, LoadingGrid, SectionTitle } from '../components/UI'
import { categoryNames } from '../lib/dominio'
import { EventCard } from '../components/EventCard'

export default function CatalogoEventos() {
  const [params, setParams] = useSearchParams()
  const q = params.get('q') ?? ''; const category = params.get('categoria') ?? ''; const artistName = params.get('artista') ?? ''
  const events = usePublishedEvents(!artistName)
  const artistLookup = useArtistByName(artistName)
  const artistEvents = useEventsByArtist(artistName)
  const artists = useActiveArtists()
  const filtered = (artistName ? (artistEvents.data ?? []) : (events.data ?? [])).filter((event) => `${event.name} ${event.venueName} ${event.venueCity ?? ''}`.toLowerCase().includes(q.toLowerCase()) && (!category || event.category === category))
  return <><SectionTitle eyebrow="La agenda empieza aquí" title="Explora la cartelera" description="Encuentra el evento que va contigo. Filtra por nombre, ciudad, categoría o artista."/>
    {artistName && <div className="surface mb-5 flex flex-wrap items-center justify-between gap-3 rounded-xl p-4 text-sm"><span>Eventos de <strong>{artistLookup.data?.stageName ?? artistName}</strong></span><button className="text-violet-700" onClick={() => setParams((prev) => { prev.delete('artista'); return prev })}>Quitar filtro ×</button></div>}<div className="surface mb-7 grid gap-3 rounded-2xl p-4 sm:grid-cols-[1fr_220px]"><input aria-label="Buscar eventos" className="field" placeholder="Busca por evento o ciudad…" value={q} onChange={(e) => setParams((prev) => { if (e.target.value) prev.set('q', e.target.value); else prev.delete('q'); return prev }, { replace: true })}/><select aria-label="Filtrar por categoría" className="field" value={category} onChange={(e) => setParams((prev) => { if (e.target.value) prev.set('categoria', e.target.value); else prev.delete('categoria'); return prev }, { replace: true })}><option value="">Todas las categorías</option>{EventCategorySchema.options.map((item) => <option key={item} value={item}>{categoryNames[item]}</option>)}</select></div>{(artistName ? (artistLookup.isPending || artistEvents.isPending) : events.isPending) ? <LoadingGrid/> : !artistName && events.isError ? <ErrorState error={events.error} retry={() => void events.refetch()}/> : artistName && (artistLookup.isError || artistEvents.isError) ? <ErrorState error={artistLookup.error ?? artistEvents.error}/> : filtered.length ? <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">{filtered.map((event) => <EventCard key={event.eventCode} event={event}/>)}</div> : <EmptyState title="No encontramos eventos" description="Prueba con otra búsqueda o cambia la categoría."/>}<section className="mt-14"><SectionTitle eyebrow="Encuentra tu sonido" title="Buscar eventos por artista" description="Selecciona un artista para ver sus próximos eventos."/>{artists.isPending ? <p className="text-sm text-slate-500">Cargando artistas…</p> : artists.isError ? <ErrorState error={artists.error}/> : <div className="flex flex-wrap gap-2">{artists.data.map((artist) => <button key={artist.id} onClick={() => setParams((prev) => { prev.set('artista', artist.stageName); return prev })} className="btn-secondary text-sm">{artist.stageName}</button>)}</div>}</section></>
}
