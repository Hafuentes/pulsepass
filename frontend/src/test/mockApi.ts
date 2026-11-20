import { vi } from 'vitest'

/** Servidor simulado a nivel de fetch: tabla "MÉTODO /ruta" → respuesta. Sin red real (QT-FE-009). */
export interface MockRequest { method: string; path: string; url: URL; headers: Headers; body: unknown }
export interface MockReply { status?: number; body?: unknown }
export type MockHandler = MockReply | ((req: MockRequest) => MockReply)
export type MockRoutes = Record<string, MockHandler>

export function error(status: number, message: string, details: Record<string, string> = {}): MockReply {
  return { status, body: { timestamp: '2026-11-17T10:00:00', status, error: 'Error', message, details } }
}

export function mockApi(routes: MockRoutes) {
  const calls: MockRequest[] = []
  const fetchMock = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
    const href = typeof input === 'string' ? input : input instanceof URL ? input.href : input.url
    const url = new URL(href, 'http://localhost')
    const method = (init?.method ?? 'GET').toUpperCase()
    const raw = typeof init?.body === 'string' ? init.body : undefined
    const req: MockRequest = { method, path: url.pathname, url, headers: new Headers(init?.headers), body: raw ? (JSON.parse(raw) as unknown) : undefined }
    calls.push(req)
    const handler = routes[`${method} ${url.pathname}`]
    const reply: MockReply = handler === undefined ? error(404, `Sin handler para ${method} ${url.pathname}`) : typeof handler === 'function' ? handler(req) : handler
    const status = reply.status ?? 200
    return new Response(status === 204 ? null : JSON.stringify(reply.body ?? null), { status, headers: { 'Content-Type': 'application/json' } })
  })
  vi.stubGlobal('fetch', fetchMock)
  return { calls, fetchMock, count: (method: string, path: string) => calls.filter((c) => c.method === method && c.path === path).length }
}
