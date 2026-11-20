import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '../../lib/api'

export const artistKeys = { active: ['artistas', 'activos'] as const, detail: (id: string) => ['artista', id] as const, byName: (name: string) => ['artista', 'nombre', name] as const }
export function useActiveArtists() { return useQuery({ queryKey: artistKeys.active, queryFn: api.artists, staleTime: 300_000 }) }
export function useArtist(id: string, enabled = true) { return useQuery({ queryKey: artistKeys.detail(id), queryFn: () => api.artist(id), enabled: enabled && Boolean(id), staleTime: 300_000 }) }
export function useArtistByName(name: string) { return useQuery({ queryKey: artistKeys.byName(name), queryFn: () => api.artistByName(name), enabled: Boolean(name), staleTime: 300_000 }) }

export function useCreateArtistMutation() {
  const client = useQueryClient()
  return useMutation({ mutationFn: api.createArtist, onSuccess: async () => { await client.invalidateQueries({ queryKey: artistKeys.active }) } })
}
