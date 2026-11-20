import { Link } from 'react-router'
import { RUTAS } from '../lib/rutas'

export default function SinPermisos() { return <div className="mx-auto max-w-xl py-16 text-center"><p className="eyebrow">403 · Acceso restringido</p><h1 className="page-title mt-3 text-4xl">Esta zona no es para todos.</h1><p className="mt-4 text-slate-500">Tu cuenta no tiene el rol necesario para acceder a esta página.</p><Link to={RUTAS.inicio} className="btn-primary mt-7">Volver al inicio</Link></div> }
