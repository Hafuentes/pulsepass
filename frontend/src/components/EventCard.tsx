import { Link } from 'react-router'
import { fechaCorta } from '../lib/formato'
import { RUTAS } from '../lib/rutas'
import { type EventSummary } from '../lib/schemas'
import { Badge } from '../components/UI'
import { categoryNames, statusTone, statusNames } from '../lib/dominio'

const categoryStyle: Record<string, { panel: string; icon: string; path: string }> = {
  MUSIC: { panel: 'bg-violet-100 dark:bg-violet-950', icon: 'text-violet-700 dark:text-violet-200', path: 'M9 18V5l12-2v13 M9 18a3 3 0 1 1-3-3 3 3 0 0 1 3 3Zm12-2a3 3 0 1 1-3-3 3 3 0 0 1 3 3Z' },
  SPORTS: { panel: 'bg-sky-100 dark:bg-sky-950', icon: 'text-sky-700 dark:text-sky-200', path: 'M12 3a9 9 0 1 0 0 18 9 9 0 0 0 0-18Zm-6.4 2.7 12.8 12.6M5 15l14-6' },
  TECHNOLOGY: { panel: 'bg-blue-100 dark:bg-blue-950', icon: 'text-blue-700 dark:text-blue-200', path: 'M4 5h16v11H4z M8 20h8 M12 16v4 M8 9h8 M8 12h5' },
  EDUCATION: { panel: 'bg-amber-100 dark:bg-amber-950', icon: 'text-amber-700 dark:text-amber-200', path: 'M12 3 2 8l10 5 10-5-10-5Zm-7 8v5c4 3 8 3 14 0v-5 M22 8v7' },
  CULTURE: { panel: 'bg-rose-100 dark:bg-rose-950', icon: 'text-rose-700 dark:text-rose-200', path: 'M4 19V5l8-2 8 2v14l-8 2-8-2Zm0-14 8 3 8-3M12 8v13' },
  ENTERTAINMENT: { panel: 'bg-indigo-100 dark:bg-indigo-950', icon: 'text-indigo-700 dark:text-indigo-200', path: 'm12 3 2.7 5.5 6.1.9-4.4 4.3 1 6.1-5.4-2.9-5.4 2.9 1-6.1-4.4-4.3 6.1-.9L12 3Z' },
}

export function EventCard({ event }: { event: EventSummary }) {
  const visual = categoryStyle[event.category] ?? categoryStyle.ENTERTAINMENT!
  return <Link to={RUTAS.evento(event.eventCode)} className="surface card-hover group block min-w-0 overflow-hidden rounded-2xl">
    <div className={`category-panel relative grid h-36 place-items-center ${visual.panel}`} aria-hidden="true"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round" className={`h-14 w-14 ${visual.icon}`}><path d={visual.path} /></svg></div>
    <div className="p-4 sm:p-5">
      <p className={`mb-2 text-[10px] font-extrabold uppercase tracking-[.14em] ${visual.icon}`}>{categoryNames[event.category] ?? event.category}</p>
      <h3 className="line-clamp-2 font-display text-base font-bold leading-snug text-slate-900 transition group-hover:text-violet-700 dark:text-slate-100 dark:group-hover:text-violet-200">{event.name}</h3>
      <p className="mt-2 flex items-center gap-2 text-sm text-slate-500"><svg aria-hidden="true" viewBox="0 0 24 24" className="h-4 w-4 shrink-0" fill="none" stroke="currentColor" strokeWidth="1.6"><rect x="3" y="4" width="18" height="17" rx="2"/><path d="M16 2v4M8 2v4M3 9h18"/></svg>{fechaCorta(event.eventDate)}</p>
      <p className="mt-1 flex items-center gap-2 text-sm text-slate-500"><svg aria-hidden="true" viewBox="0 0 24 24" className="h-4 w-4 shrink-0" fill="none" stroke="currentColor" strokeWidth="1.6"><path d="M20 10c0 5-8 12-8 12S4 15 4 10a8 8 0 1 1 16 0Z"/><circle cx="12" cy="10" r="2.5"/></svg>{event.venueCity ?? event.venueName}</p>
      <div className="mt-3"><Badge tone={statusTone(event.status)}>{statusNames[event.status] ?? event.status}</Badge></div>
    </div>
  </Link>
}
