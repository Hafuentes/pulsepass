export const fecha = (value?: string | null) => value ? new Intl.DateTimeFormat('es-CO', { dateStyle: 'long', timeStyle: 'short' }).format(new Date(value)) : 'Fecha por confirmar'
export const fechaCorta = (value?: string | null) => value ? new Intl.DateTimeFormat('es-CO', { day: '2-digit', month: 'short', year: 'numeric' }).format(new Date(value)) : 'Por confirmar'
export const moneda = (value?: number | string | null) => value == null ? 'Por definir' : new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', maximumFractionDigits: 0 }).format(Number(value))
export const titulo = (value: string) => value.toLowerCase().replace(/(^|\s)\S/g, (letter) => letter.toUpperCase())
