import { useEffect, useRef, type ChangeEvent, type ReactNode } from 'react'
import clsx from 'clsx'
import { mensajeError } from '../lib/http'

export function Button({ children, variant = 'primary', className, ...props }: { children: ReactNode; variant?: 'primary' | 'secondary' | 'danger'; className?: string; type?: 'button' | 'submit'; form?: string; disabled?: boolean; onClick?: () => void }) {
  const style = variant === 'primary' ? 'btn-primary' : variant === 'danger' ? 'min-h-11 rounded-full border border-rose-500/30 bg-rose-50 px-4 py-2.5 font-semibold text-rose-800 hover:bg-rose-100 dark:border-rose-400/30 dark:bg-rose-500/10 dark:text-rose-200 dark:hover:bg-rose-500/20' : 'btn-secondary'
  return <button className={clsx(style, 'disabled:cursor-not-allowed disabled:opacity-50', className)} {...props}>{children}</button>
}
export function Badge({ children, tone = 'neutral' }: { children: ReactNode; tone?: 'neutral' | 'success' | 'warning' | 'danger' | 'brand' }) {
  const tones = { neutral: 'badge-neutral border-slate-500/20 bg-slate-500/10 text-slate-700', success: 'badge-success border-emerald-400/30 bg-emerald-50 text-emerald-800', warning: 'badge-warning border-amber-400/30 bg-amber-50 text-amber-900', danger: 'badge-danger border-rose-400/30 bg-rose-50 text-rose-800', brand: 'badge-brand border-violet-400/30 bg-violet-50 text-violet-800' }
  return <span className={clsx('inline-flex items-center rounded-full border px-2.5 py-1 text-[11px] font-bold tracking-wide', tones[tone])}>{children}</span>
}
export function SectionTitle({ eyebrow, title, description, action }: { eyebrow?: string; title: string; description?: string; action?: ReactNode }) {
  return <div className="mb-7 flex flex-col justify-between gap-4 sm:flex-row sm:items-end"><div>{eyebrow && <p className="eyebrow mb-2">{eyebrow}</p>}<h1 className="page-title text-3xl sm:text-4xl">{title}</h1>{description && <p className="mt-3 max-w-2xl text-sm leading-6 text-slate-500">{description}</p>}</div>{action}</div>
}
export function ErrorState({ error, retry }: { error: unknown; retry?: () => void }) { return <div className="surface rounded-2xl p-6 text-center" role="alert"><div className="mx-auto mb-3 grid h-11 w-11 place-items-center rounded-full bg-rose-400/10 text-xl text-rose-700 dark:text-rose-300">!</div><p className="font-semibold">No pudimos cargar la información</p><p className="mt-2 text-sm text-slate-500">{mensajeError(error)}</p>{retry && <Button className="mt-4" onClick={retry}>Intentar de nuevo</Button>}</div> }
export function EmptyState({ title, description }: { title: string; description: string }) { return <div className="surface rounded-2xl px-6 py-14 text-center"><div className="mx-auto mb-4 grid h-14 w-14 place-items-center rounded-2xl bg-violet-400/10 text-2xl">✦</div><h3 className="font-display text-lg font-bold">{title}</h3><p className="mt-2 text-sm text-slate-500">{description}</p></div> }
export function LoadingGrid() { return <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3" aria-busy="true" aria-label="Cargando contenido">{[1,2,3].map((n) => <div key={n} className="surface h-72 animate-pulse rounded-2xl" />)}</div> }
export function Field({ label, error, ...props }: { label: string; error?: string; name?: string; type?: string; placeholder?: string; required?: boolean; min?: string | number; max?: string | number; step?: string | number; autoComplete?: string; value?: string | number; onChange?: (event: ChangeEvent<HTMLInputElement>) => void }) { const id = props.name ?? label.toLowerCase().replaceAll(' ', '-'); return <label className="block space-y-2 text-sm font-medium text-slate-600" htmlFor={id}>{label}{props.required && <span className="text-violet-700"> *</span>}<input id={id} className="field" aria-invalid={Boolean(error)} {...props} />{error && <span className="block text-xs text-rose-700 dark:text-rose-300">{error}</span>}</label> }


export function Modal({ open, title, description, onClose, children, footer }: { open: boolean; title: string; description?: string; onClose: () => void; children: ReactNode; footer?: ReactNode }) {
  const dialogRef = useRef<HTMLDivElement>(null)
  useEffect(() => {
    if (!open) return
    const previousOverflow = document.body.style.overflow
    document.body.style.overflow = 'hidden'
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') onClose()
      if (event.key === 'Tab' && dialogRef.current) {
        const focusable = dialogRef.current.querySelectorAll<HTMLElement>('button:not([disabled]), input:not([disabled]), select:not([disabled]), textarea:not([disabled]), a[href], [tabindex]:not([tabindex="-1"])')
        if (!focusable.length) return
        const first = focusable[0]
        const last = focusable[focusable.length - 1]
        if (event.shiftKey && document.activeElement === first) { event.preventDefault(); last?.focus() }
        else if (!event.shiftKey && document.activeElement === last) { event.preventDefault(); first?.focus() }
      }
    }
    document.addEventListener('keydown', onKeyDown)
    const frame = window.requestAnimationFrame(() => dialogRef.current?.querySelector<HTMLElement>('button, input, select, textarea')?.focus())
    return () => { document.body.style.overflow = previousOverflow; document.removeEventListener('keydown', onKeyDown); window.cancelAnimationFrame(frame) }
  }, [open, onClose])
  if (!open) return null
  return <div className="fixed inset-0 z-[100] flex items-center justify-center overflow-y-auto bg-slate-950/70 p-4 backdrop-blur-sm" onMouseDown={(event) => { if (event.target === event.currentTarget) onClose() }}>
    <section ref={dialogRef} role="dialog" aria-modal="true" aria-labelledby="pulsepass-modal-title" aria-describedby={description ? 'pulsepass-modal-description' : undefined} className="surface my-auto w-full max-w-lg rounded-3xl p-6 shadow-2xl sm:p-8">
      <div className="flex items-start justify-between gap-4"><div><h2 id="pulsepass-modal-title" className="font-display text-xl font-extrabold">{title}</h2>{description && <p id="pulsepass-modal-description" className="mt-2 text-sm leading-6 text-slate-500">{description}</p>}</div><button type="button" onClick={onClose} className="rounded-full border border-slate-200 px-3 py-1 text-lg leading-6" aria-label="Cerrar ventana">×</button></div>
      <div className="mt-5">{children}</div>
      {footer && <div className="mt-6 flex flex-wrap justify-end gap-2">{footer}</div>}
    </section>
  </div>
}
export function FormError({ error }: { error: unknown }) { return error ? <p className="rounded-xl border border-rose-400/30 bg-rose-50 p-3 text-sm text-rose-800 dark:border-rose-400/20 dark:bg-rose-400/10 dark:text-rose-200" role="alert" aria-live="assertive">{mensajeError(error)}</p> : null }
