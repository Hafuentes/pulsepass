import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '../../lib/api'

export const venueKeys = { active: ['sedes', 'activas'] as const, detail: (code: string) => ['sede', code] as const }
export function useActiveVenues() { return useQuery({ queryKey: venueKeys.active, queryFn: api.venues, staleTime: 300_000 }) }
export function useVenue(code: string) { return useQuery({ queryKey: venueKeys.detail(code), queryFn: () => api.venue(code), enabled: Boolean(code), staleTime: 300_000 }) }

export function useCreateVenueMutation() {
  const client = useQueryClient()
  return useMutation({ mutationFn: api.createVenue, onSuccess: async () => { await client.invalidateQueries({ queryKey: venueKeys.active }) } })
}
