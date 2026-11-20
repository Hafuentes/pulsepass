import { Link } from 'react-router'
import { usePublishedEvents } from '../features/events/hooks'
import { useActiveArtists } from '../features/artists/hooks'
import { RUTAS } from '../lib/rutas'
import { useAuth } from '../auth/AuthContext'
import { EmptyState, ErrorState, LoadingGrid, SectionTitle } from '../components/UI'
import { EventCard } from '../components/EventCard'

export default function Inicio() {
  const events = usePublishedEvents()
  const artists = useActiveArtists()
  const { sesion } = useAuth()

  return <div className="space-y-12 sm:space-y-16">
    <section className="home-hero relative overflow-hidden bg-gradient-to-r from-[#6366f1] via-[#7044c6] to-[#f43f5e] px-6 py-14 text-white sm:px-10 sm:py-16 lg:px-[max(2rem,calc((100vw-1120px)/2))] lg:py-20">
      <div className="relative z-10 max-w-2xl">
        <h1 className="font-display text-4xl font-extrabold leading-[1.12] tracking-tight text-white sm:text-5xl lg:text-6xl">Descubre los mejores eventos</h1>
        <p className="mt-5 max-w-xl text-base leading-7 text-white sm:text-lg">Música, cultura, deporte y tecnología en un solo lugar.</p>
        <div className="mt-7 flex flex-wrap gap-3">
          <Link to={RUTAS.eventos} className="home-cta inline-flex min-h-12 items-center justify-center rounded-xl bg-white px-5 py-3 font-bold text-indigo-800 transition hover:bg-indigo-50">Ver cartelera</Link>
          {sesion.estado !== 'autenticado' && <Link to={RUTAS.registro} className="inline-flex min-h-12 items-center justify-center rounded-xl border border-white/80 bg-white/5 px-5 py-3 font-bold text-white transition hover:bg-white/15">Crear cuenta</Link>}
        </div>
      </div>
    </section>

    <section>
      <SectionTitle title="Próximos eventos" action={<Link to={RUTAS.eventos} className="text-sm font-semibold text-indigo-700 dark:text-violet-300">Ver todos →</Link>} />
      {events.isPending ? <LoadingGrid /> : events.isError ? <ErrorState error={events.error} retry={() => void events.refetch()} /> : events.data.length ? <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">{events.data.slice(0, 3).map((event) => <EventCard key={event.eventCode} event={event} />)}</div> : <EmptyState title="Aún no hay eventos publicados" description="Vuelve pronto para descubrir nuevas experiencias." />}
    </section>

    <section>
      <SectionTitle title="Artistas destacados" action={<Link to={RUTAS.artistas} className="text-sm font-semibold text-indigo-700 dark:text-violet-300">Ver todos →</Link>} />
      {artists.isPending ? <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3" aria-busy="true">{[1, 2, 3].map((item) => <div key={item} className="surface h-20 animate-pulse rounded-2xl" />)}</div> : artists.isError ? <ErrorState error={artists.error} retry={() => void artists.refetch()} /> : artists.data.length ? <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">{artists.data.slice(0, 3).map((artist, index) => <Link key={artist.id} to={RUTAS.artista(artist.id)} className="flex min-w-0 items-center gap-3 rounded-xl p-2 transition hover:bg-slate-100 dark:hover:bg-slate-800"><span className={`grid h-14 w-14 shrink-0 place-items-center rounded-full text-base font-extrabold ${['bg-indigo-100 text-indigo-800 dark:bg-indigo-400/20 dark:text-indigo-200', 'bg-emerald-100 text-emerald-800 dark:bg-emerald-400/20 dark:text-emerald-200', 'bg-sky-100 text-sky-800 dark:bg-sky-400/20 dark:text-sky-200'][index % 3]}`}>{artist.stageName.split(/\s+/).slice(0, 2).map((part) => part[0]?.toUpperCase()).join('')}</span><span className="min-w-0"><strong className="block truncate">{artist.stageName}</strong><small className="text-slate-500">{artist.genre || artist.country || 'Artista'}</small></span></Link>)}</div> : <EmptyState title="Aún no hay artistas destacados" description="Los artistas activos aparecerán aquí." />}
    </section>
  </div>
}
