import { useEffect, useState } from 'react'
import { Link, NavLink, Outlet, useLocation, useNavigate } from 'react-router'
import { useAuth } from '../auth/AuthContext'
import { RUTAS } from '../lib/rutas'

export function Layout() {
  const { sesion, cerrarSesion, tieneRol } = useAuth()
  const [menu, setMenu] = useState(false)
  const [theme, setTheme] = useState<'light' | 'dark'>(() => {
    try {
      const saved = localStorage.getItem('pulsepass.theme')
      if (saved === 'light' || saved === 'dark') return saved
    } catch { /* Se utiliza la preferencia del sistema cuando no hay almacenamiento. */ }
    return typeof window !== 'undefined' && window.matchMedia?.('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'
  })
  const navigate = useNavigate()
  const isHome = useLocation().pathname === '/'

  useEffect(() => {
    document.documentElement.dataset.theme = theme
    document.querySelector('meta[name="theme-color"]')?.setAttribute('content', theme === 'dark' ? '#080d1b' : '#ffffff')
    try { localStorage.setItem('pulsepass.theme', theme) } catch { /* La aplicación sigue funcionando sin persistencia. */ }
  }, [theme])

  useEffect(() => {
    if (!menu) return
    const closeOnEscape = (event: KeyboardEvent) => { if (event.key === 'Escape') setMenu(false) }
    document.addEventListener('keydown', closeOnEscape)
    return () => document.removeEventListener('keydown', closeOnEscape)
  }, [menu])

  const salir = () => { cerrarSesion(); setMenu(false); navigate(RUTAS.inicio) }
  const closeMenu = () => setMenu(false)
  const navClass = ({ isActive }: { isActive: boolean }) => `rounded-md px-3 py-2 text-sm font-medium transition ${isActive ? 'bg-indigo-50 text-indigo-800 dark:bg-indigo-400/15 dark:text-violet-200' : 'text-slate-600 hover:bg-slate-100 hover:text-slate-900 dark:text-slate-300 dark:hover:bg-slate-800 dark:hover:text-white'}`
  const themeLabel = theme === 'dark' ? 'Cambiar a modo claro' : 'Cambiar a modo oscuro'
  const roleLinks = <>
    {tieneRol('USER') && <NavLink onClick={closeMenu} to={RUTAS.misTickets} className={navClass}>Mis tickets</NavLink>}
    {tieneRol('STAFF', 'ADMIN') && <NavLink onClick={closeMenu} to={RUTAS.validacion} className={navClass}>Validar ingreso</NavLink>}
    {tieneRol('ADMIN') && <NavLink onClick={closeMenu} to={RUTAS.adminEventos} className={navClass}>Administración</NavLink>}
  </>

  return <div className="flex min-h-screen flex-col">
    <a href="#contenido" className="sr-only focus:not-sr-only focus:fixed focus:left-4 focus:top-4 focus:z-50 focus:rounded-lg focus:bg-white focus:px-4 focus:py-2 focus:text-slate-900 dark:focus:bg-slate-900 dark:focus:text-white">Saltar al contenido</a>
    <header className="sticky top-0 z-40 border-b border-slate-200 bg-white/95 backdrop-blur-xl">
      <div className="mx-auto flex w-full max-w-6xl items-center justify-between gap-3 px-4 py-3 sm:px-6 lg:px-8">
        <Link to={RUTAS.inicio} onClick={closeMenu} className="flex shrink-0 items-center gap-2.5" aria-label="PulsePass, inicio">
          <span className="grid h-9 w-9 place-items-center rounded-lg bg-indigo-600 text-white"><svg viewBox="0 0 24 24" className="h-5 w-5" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true"><path d="M9 18V5l12-2v13"/><circle cx="6" cy="18" r="3"/><circle cx="18" cy="16" r="3"/></svg></span>
          <span className="font-display text-lg font-extrabold tracking-tight text-slate-900 dark:text-white sm:text-xl">PulsePass</span>
        </Link>
        <nav aria-label="Principal" className="hidden min-w-0 items-center gap-1 md:flex">
          <NavLink to={RUTAS.eventos} className={navClass}>Eventos</NavLink>
          <NavLink to={RUTAS.artistas} className={navClass}>Artistas</NavLink>
          <NavLink to={RUTAS.sedes} className={navClass}>Sedes</NavLink>
          {roleLinks}
        </nav>
        <div className="hidden shrink-0 items-center gap-2 md:flex">
          <button type="button" onClick={() => setTheme((current) => current === 'dark' ? 'light' : 'dark')} className="btn-secondary px-3 text-sm" aria-label={themeLabel} title={themeLabel}>{theme === 'dark' ? '☀ Claro' : '☾ Oscuro'}</button>
          {sesion.estado === 'autenticado' ? <><Link to={RUTAS.cuenta} className="btn-secondary max-w-40 truncate text-sm">{sesion.usuario.username}</Link><button type="button" onClick={salir} className="rounded-lg px-3 py-2 text-sm font-semibold text-rose-700 hover:bg-rose-50 dark:text-rose-300 dark:hover:bg-rose-400/10">Salir</button></> : <><Link to={RUTAS.login} className="rounded-lg px-3 py-2 text-sm font-semibold text-indigo-700 hover:bg-indigo-50 dark:text-violet-300 dark:hover:bg-indigo-400/10">Ingresar</Link><Link to={RUTAS.registro} className="btn-primary text-sm">Registrarse</Link></>}
        </div>
        <button type="button" className="inline-flex h-10 w-10 shrink-0 items-center justify-center rounded-lg border border-slate-200 text-xl text-slate-700 hover:bg-slate-100 dark:border-slate-700 dark:text-slate-100 dark:hover:bg-slate-800 md:hidden" aria-label={menu ? 'Cerrar menú' : 'Abrir menú'} aria-controls="menu-movil" aria-expanded={menu} onClick={() => setMenu((value) => !value)}>{menu ? '×' : '☰'}</button>
      </div>
      {menu && <nav id="menu-movil" aria-label="Menú móvil" className="border-t border-slate-200 bg-white px-4 py-4 dark:bg-slate-950">
        <div className="mx-auto flex max-w-6xl flex-col gap-1">
          <NavLink end onClick={closeMenu} to={RUTAS.inicio} className={navClass}>Inicio</NavLink>
          <NavLink onClick={closeMenu} to={RUTAS.eventos} className={navClass}>Eventos</NavLink>
          <NavLink onClick={closeMenu} to={RUTAS.artistas} className={navClass}>Artistas</NavLink>
          <NavLink onClick={closeMenu} to={RUTAS.sedes} className={navClass}>Sedes</NavLink>
          {roleLinks}
          <button type="button" onClick={() => setTheme((current) => current === 'dark' ? 'light' : 'dark')} className="btn-secondary mt-2 w-full justify-start" aria-label={themeLabel}>{theme === 'dark' ? '☀ Cambiar a modo claro' : '☾ Cambiar a modo oscuro'}</button>
          {sesion.estado === 'autenticado' ? <><NavLink onClick={closeMenu} to={RUTAS.cuenta} className={navClass}>Mi cuenta</NavLink><button type="button" onClick={salir} className="mt-1 rounded-lg px-3 py-3 text-left text-sm font-semibold text-rose-700 hover:bg-rose-50 dark:text-rose-300 dark:hover:bg-rose-400/10">Cerrar sesión</button></> : <div className="mt-2 grid grid-cols-2 gap-2"><Link onClick={closeMenu} to={RUTAS.login} className="btn-secondary">Ingresar</Link><Link onClick={closeMenu} to={RUTAS.registro} className="btn-primary">Registrarse</Link></div>}
        </div>
      </nav>}
    </header>
    <main id="contenido" className={`mx-auto min-h-[70vh] w-full max-w-6xl flex-1 px-4 py-8 sm:px-6 sm:py-12 lg:px-8 ${isHome ? 'home-main' : ''}`}><Outlet /></main>
    <footer className="mt-auto border-t border-slate-200 bg-white dark:bg-slate-950"><div className="mx-auto flex max-w-6xl flex-col gap-3 px-4 py-6 text-xs text-slate-500 sm:flex-row sm:items-center sm:justify-between sm:px-6 lg:px-8"><p>© {new Date().getFullYear()} PulsePass · Caso de estudio académico</p><div className="flex gap-4"><Link to={RUTAS.sedes} className="hover:text-indigo-700 dark:hover:text-violet-300">Sedes</Link><Link to={RUTAS.artistas} className="hover:text-indigo-700 dark:hover:text-violet-300">Artistas</Link></div></div></footer>
  </div>
}
