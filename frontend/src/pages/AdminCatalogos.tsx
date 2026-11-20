import { useState } from 'react'
import { Link } from 'react-router'
import { useActiveVenues, useCreateVenueMutation } from '../features/venues/hooks'
import { useActiveArtists, useCreateArtistMutation } from '../features/artists/hooks'
import { RUTAS } from '../lib/rutas'
import { Button, SectionTitle, FormError } from '../components/UI'

export default function AdminCatalogos() {
  const venues = useActiveVenues()
  const artists = useActiveArtists()
  const [venue, setVenue] = useState({ code: '', name: '', city: '', address: '', capacity: '100' })
  const [artist, setArtist] = useState({ stageName: '', country: '', genre: '' })
  const [venueError, setVenueError] = useState<unknown>(null)
  const [artistError, setArtistError] = useState<unknown>(null)
  const saveVenue = useCreateVenueMutation()
  const saveArtist = useCreateArtistMutation()
  return <><Link to={RUTAS.adminEventos} className="text-sm text-violet-700">← Administración</Link><SectionTitle eyebrow="Administración · Catálogos" title="Sedes y artistas" description="Registra los espacios y artistas que luego podrás seleccionar al crear y gestionar eventos." />
    <div className="grid gap-6 lg:grid-cols-2">
      <section className="surface rounded-[2rem] p-6 sm:p-8"><h2 className="font-display text-xl font-bold">Registrar sede</h2><p className="mt-2 text-sm text-slate-500">La sede quedará activa y disponible para los nuevos eventos.</p><form className="mt-6 grid gap-4" onSubmit={(e)=>{e.preventDefault();setVenueError(null);saveVenue.mutate({ ...venue, code: venue.code.trim().toUpperCase(), name: venue.name.trim(), city: venue.city.trim(), address: venue.address.trim() || undefined, capacity: Number(venue.capacity) }, { onSuccess: () => { setVenue({ code: '', name: '', city: '', address: '', capacity: '100' }); setVenueError(null) }, onError: setVenueError })}}>
        <label className="space-y-2 text-sm text-slate-600">Código único<input required maxLength={50} className="field" placeholder="VEN-SMR-01" value={venue.code} onChange={e=>setVenue({...venue,code:e.target.value})}/></label>
        <label className="space-y-2 text-sm text-slate-600">Nombre de la sede<input required maxLength={120} className="field" placeholder="Marina Convention Center" value={venue.name} onChange={e=>setVenue({...venue,name:e.target.value})}/></label>
        <label className="space-y-2 text-sm text-slate-600">Ciudad<input required maxLength={100} className="field" placeholder="Santa Marta" value={venue.city} onChange={e=>setVenue({...venue,city:e.target.value})}/></label>
        <label className="space-y-2 text-sm text-slate-600">Dirección<input maxLength={200} className="field" placeholder="Dirección de la sede" value={venue.address} onChange={e=>setVenue({...venue,address:e.target.value})}/></label>
        <label className="space-y-2 text-sm text-slate-600">Capacidad<input required type="number" min="1" className="field" value={venue.capacity} onChange={e=>setVenue({...venue,capacity:e.target.value})}/></label>
        <FormError error={venueError}/><Button type="submit" disabled={saveVenue.isPending}>{saveVenue.isPending?'Guardando…':'Guardar sede'}</Button>
      </form><div className="mt-7 border-t border-slate-200 pt-5"><h3 className="font-semibold">Sedes activas ({venues.data?.length??0})</h3>{venues.isError?<FormError error={venues.error}/>:<ul className="mt-3 space-y-2 text-sm">{(venues.data??[]).map(v=><li key={v.code} className="flex justify-between gap-3"><span>{v.name} · {v.city}</span><span className="text-slate-500">{v.code}</span></li>)}</ul>}</div></section>
      <section className="surface rounded-[2rem] p-6 sm:p-8"><h2 className="font-display text-xl font-bold">Registrar artista</h2><p className="mt-2 text-sm text-slate-500">El artista quedará activo y aparecerá en la pantalla de asociación del evento.</p><form className="mt-6 grid gap-4" onSubmit={(e)=>{e.preventDefault();setArtistError(null);saveArtist.mutate({ stageName: artist.stageName.trim(), country: artist.country.trim() || undefined, genre: artist.genre.trim() || undefined }, { onSuccess: () => { setArtist({ stageName: '', country: '', genre: '' }); setArtistError(null) }, onError: setArtistError })}}>
        <label className="space-y-2 text-sm text-slate-600">Nombre artístico<input required maxLength={100} className="field" placeholder="Solar Beat" value={artist.stageName} onChange={e=>setArtist({...artist,stageName:e.target.value})}/></label>
        <label className="space-y-2 text-sm text-slate-600">País<input maxLength={100} className="field" placeholder="Colombia" value={artist.country} onChange={e=>setArtist({...artist,country:e.target.value})}/></label>
        <label className="space-y-2 text-sm text-slate-600">Género / estilo<input maxLength={50} className="field" placeholder="Electrónica" value={artist.genre} onChange={e=>setArtist({...artist,genre:e.target.value})}/></label>
        <FormError error={artistError}/><Button type="submit" disabled={saveArtist.isPending}>{saveArtist.isPending?'Guardando…':'Guardar artista'}</Button>
      </form><div className="mt-7 border-t border-slate-200 pt-5"><h3 className="font-semibold">Artistas activos ({artists.data?.length??0})</h3>{artists.isError?<FormError error={artists.error}/>:<ul className="mt-3 space-y-2 text-sm">{(artists.data??[]).map(a=><li key={a.id} className="flex justify-between gap-3"><span>{a.stageName}</span><span className="text-slate-500">{a.genre||a.country||'Artista'}</span></li>)}</ul>}</div></section>
    </div>
  </>
}
