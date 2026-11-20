import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '../../lib/api'

export const ticketKeys = { mine: (email: string) => ['tickets', 'usuario', email] as const, detail: (code: string) => ['ticket', code] as const, staffDetail: (code: string) => ['ticket', 'staff', code] as const, paidByEvent: (eventCode: string) => ['tickets', 'pagados', eventCode] as const }
export function useMyTickets(email: string) { return useQuery({ queryKey: ticketKeys.mine(email), queryFn: () => api.myTickets(email), enabled: Boolean(email), staleTime: 15_000 }) }
export function useTicket(code: string) { return useQuery({ queryKey: ticketKeys.detail(code), queryFn: () => api.ticket(code), enabled: Boolean(code), staleTime: 15_000 }) }
export function useStaffTicket(code: string) { return useQuery({ queryKey: ticketKeys.staffDetail(code), queryFn: () => api.ticket(code), enabled: Boolean(code), staleTime: 0 }) }
export function usePaidTickets(eventCode: string, poll = false) { return useQuery({ queryKey: ticketKeys.paidByEvent(eventCode), queryFn: () => api.paidTickets(eventCode), enabled: Boolean(eventCode), staleTime: 15_000, refetchInterval: poll ? 10_000 : false, refetchOnWindowFocus: poll }) }

export function useBuyTicketMutation() {
  const client = useQueryClient()
  return useMutation({ mutationFn: api.buyTicket, onSuccess: async (ticket, variables) => {
    await client.invalidateQueries({ queryKey: ticketKeys.mine(variables.userEmail) })
    await client.invalidateQueries({ queryKey: ['evento', ticket.eventCode] })
    await client.invalidateQueries({ queryKey: ticketKeys.paidByEvent(ticket.eventCode) })
  } })
}
export function useCancelTicketMutation() {
  const client = useQueryClient()
  return useMutation({ mutationFn: api.cancelTicket, onSuccess: async (ticket) => {
    await client.invalidateQueries({ queryKey: ticketKeys.detail(ticket.ticketCode) })
    await client.invalidateQueries({ queryKey: ['tickets', 'usuario'] })
    await client.invalidateQueries({ queryKey: ticketKeys.paidByEvent(ticket.eventCode) })
  } })
}
export function useUseTicketMutation() {
  const client = useQueryClient()
  return useMutation({ mutationFn: api.useTicket, onSuccess: async (ticket) => {
    await client.invalidateQueries({ queryKey: ticketKeys.staffDetail(ticket.ticketCode) })
    await client.invalidateQueries({ queryKey: ticketKeys.paidByEvent(ticket.eventCode) })
    await client.invalidateQueries({ queryKey: ticketKeys.detail(ticket.ticketCode) })
  } })
}
