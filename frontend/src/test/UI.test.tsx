import { fireEvent, render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { Badge, Modal } from '../components/UI'

describe('componentes visuales accesibles', () => {
  it('muestra el estado del ticket como texto y con tono semántico', () => {
    render(<Badge tone="success">Pagado</Badge>)
    expect(screen.getByText('Pagado')).toHaveClass('badge-success')
  })

  it('expone el modal como diálogo y permite cerrarlo con Escape', () => {
    const onClose = vi.fn()
    render(<Modal open title="Confirmar compra" description="Revisa la entrada" onClose={onClose}>Detalle</Modal>)

    expect(screen.getByRole('dialog', { name: 'Confirmar compra' })).toBeInTheDocument()
    expect(screen.getByText('Revisa la entrada')).toBeInTheDocument()
    fireEvent.keyDown(document, { key: 'Escape' })
    expect(onClose).toHaveBeenCalledOnce()
  })

  it('no renderiza el modal cuando está cerrado', () => {
    render(<Modal open={false} title="Oculto" onClose={() => undefined}>Contenido</Modal>)
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
  })
})
