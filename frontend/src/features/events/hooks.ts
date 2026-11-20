import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '../../lib/api'

export const eventKeys = {
  published: ['eventos', 'publicados'] as const,
  admin: ['eventos', 'admin'] as const,
  detail: (code: string) => ['evento', code] as const,
  adminDetail: (code: string) => ['evento', 'admin', code] as const,
  byArtist: (stageName: string) => ['eventos', 'artista', stageName] as const,
}

export function usePublishedEvents(enabled = true) {
  return useQuery({ queryKey: eventKeys.published, queryFn: api.events, enabled, staleTime: 60_000 })
}
export function useAdminEvents() {
  return useQuery({ queryKey: eventKeys.admin, queryFn: api.adminEvents })
}
export function useEvent(code: string, enabled = Boolean(code)) {
  return useQuery({ queryKey: eventKeys.detail(code), queryFn: () => api.event(code), enabled: enabled && Boolean(code), staleTime: 30_000 })
}
export function useAdminEvent(code: string) {
  return useQuery({ queryKey: eventKeys.adminDetail(code), queryFn: () => api.event(code), enabled: Boolean(code) })
}
export function useEventsByArtist(stageName: string, enabled = Boolean(stageName)) {
  return useQuery({ queryKey: eventKeys.byArtist(stageName), queryFn: () => api.eventsByArtist(stageName), enabled: enabled && Boolean(stageName) })
}


export function useCreateEventMutation() {
  const client = useQueryClient()
  return useMutation({ mutationFn: api.createEvent, onSuccess: async (event) => { client.setQueryData(eventKeys.detail(event.eventCode), event); await client.invalidateQueries({ queryKey: eventKeys.admin }) } })
}
export function usePublishEventMutation(eventCode: string) {
  const client = useQueryClient()
  return useMutation({ mutationFn: () => api.publishEvent(eventCode), onSuccess: async () => {
    await client.invalidateQueries({ queryKey: eventKeys.detail(eventCode) })
    await client.invalidateQueries({ queryKey: eventKeys.published })
    await client.invalidateQueries({ queryKey: eventKeys.admin })
  } })
}
export function useChangeEventStatusMutation(eventCode: string) {
  const client = useQueryClient()
  return useMutation({ mutationFn: (status: Parameters<typeof api.changeEventStatus>[1]) => api.changeEventStatus(eventCode, status), onSuccess: async (event) => {
    client.setQueryData(eventKeys.detail(eventCode), event)
    await client.invalidateQueries({ queryKey: eventKeys.admin })
    await client.invalidateQueries({ queryKey: eventKeys.published })
  } })
}
export function useUpdateEventMutation(eventCode: string) {
  const client = useQueryClient()
  return useMutation({ mutationFn: (data: Parameters<typeof api.updateEvent>[1]) => api.updateEvent(eventCode, data), onSuccess: async (event) => {
    client.setQueryData(eventKeys.detail(eventCode), event)
    await client.invalidateQueries({ queryKey: eventKeys.admin })
    await client.invalidateQueries({ queryKey: eventKeys.published })
  } })
}
export function useAssociateArtistMutation(eventCode: string) {
  const client = useQueryClient()
  return useMutation({ mutationFn: (artistId: number) => api.addArtist(eventCode, artistId), onSuccess: async (event) => { client.setQueryData(eventKeys.detail(eventCode), event); await client.invalidateQueries({ queryKey: eventKeys.admin }) } })
}
export function useRemoveArtistMutation(eventCode: string) {
  const client = useQueryClient()
  return useMutation({ mutationFn: (artistId: number) => api.removeArtist(eventCode, artistId), onSuccess: async (event) => { client.setQueryData(eventKeys.detail(eventCode), event); await client.invalidateQueries({ queryKey: eventKeys.admin }) } })
}
