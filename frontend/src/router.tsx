import { lazy, Suspense, type ComponentType } from 'react'
import { createBrowserRouter, type RouteObject } from 'react-router'
import { Layout } from './layouts/Layout'
import RutaProtegida from './layouts/RutaProtegida'
import RutaPorRol from './layouts/RutaPorRol'
import SoloAnonimo from './layouts/SoloAnonimo'
import ErrorPagina from './pages/ErrorPagina'
import Inicio from './pages/Inicio'

const Cargando = () => <div className="py-20 text-center text-sm text-slate-400" role="status" aria-busy="true">Cargando PulsePass…</div>

/** Cada página es un módulo propio con import() dinámico: Inicio no descarga el código de STAFF/ADMIN (FE-012). */
const perezosa = (loader: () => Promise<{ default: ComponentType }>): Pick<RouteObject, 'element'> => {
  const Componente = lazy(loader)
  return { element: <Suspense fallback={<Cargando />}><Componente /></Suspense> }
}

export const routes: RouteObject[] = [
  {
    path: '/', element: <Layout />, errorElement: <ErrorPagina />, children: [
      { index: true, element: <Inicio /> },
      { path: 'eventos', ...perezosa(() => import('./pages/CatalogoEventos')) },
      { path: 'eventos/:eventCode', ...perezosa(() => import('./pages/DetalleEvento')) },
      { path: 'artistas', ...perezosa(() => import('./pages/ListaArtistas')) },
      { path: 'artistas/:id', ...perezosa(() => import('./pages/DetalleArtista')) },
      { path: 'sedes', ...perezosa(() => import('./pages/ListaSedes')) },
      { path: 'sedes/:code', ...perezosa(() => import('./pages/DetalleSede')) },
      { element: <SoloAnonimo />, children: [
        { path: 'login', ...perezosa(() => import('./pages/Login')) },
        { path: 'registro', ...perezosa(() => import('./pages/Registro')) },
      ] },
      { element: <RutaProtegida />, children: [
        { path: 'cuenta', ...perezosa(() => import('./pages/Cuenta')) },
        { element: <RutaPorRol roles={['USER']} />, children: [
          { path: 'mis-tickets', ...perezosa(() => import('./pages/MisTickets')) },
          { path: 'mis-tickets/:ticketCode', ...perezosa(() => import('./pages/DetalleTicket')) },
          { path: 'compra/confirmacion/:ticketCode', ...perezosa(() => import('./pages/ConfirmacionCompra')) },
        ] },
        { element: <RutaPorRol roles={['STAFF', 'ADMIN']} />, children: [
          { path: 'staff/validacion', ...perezosa(() => import('./pages/ValidacionIngreso')) },
          { path: 'staff/eventos/:eventCode/tickets', ...perezosa(() => import('./pages/TicketsPagadosEvento')) },
        ] },
        { element: <RutaPorRol roles={['ADMIN']} />, children: [
          { path: 'admin/eventos', ...perezosa(() => import('./pages/AdminEventos')) },
          { path: 'admin/catalogos', ...perezosa(() => import('./pages/AdminCatalogos')) },
          { path: 'admin/eventos/nuevo', ...perezosa(() => import('./pages/AdminCrearEvento')) },
          { path: 'admin/eventos/:eventCode', ...perezosa(() => import('./pages/AdminGestionEvento')) },
        ] },
      ] },
      { path: '403', ...perezosa(() => import('./pages/SinPermisos')) },
      { path: '*', ...perezosa(() => import('./pages/NoEncontrada')) },
    ],
  },
]

export const router = createBrowserRouter(routes)
