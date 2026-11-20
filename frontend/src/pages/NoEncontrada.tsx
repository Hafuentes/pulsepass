import { Link } from 'react-router'
import { RUTAS } from '../lib/rutas'

export default function NoEncontrada() { return <div className="mx-auto max-w-xl py-16 text-center"><p className="eyebrow">404 · Página no encontrada</p><h1 className="page-title mt-3 text-4xl">Parece que tomaste otra ruta.</h1><p className="mt-4 text-slate-500">El contenido que buscas no está disponible o cambió de dirección.</p><Link to={RUTAS.inicio} className="btn-primary mt-7">Ir al inicio</Link></div> }
