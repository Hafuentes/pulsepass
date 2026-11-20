import { useQuery } from '@tanstack/react-query'
import { api } from '../../lib/api'

export function useUserByEmail(email: string) { return useQuery({ queryKey: ['usuario', email], queryFn: () => api.userByEmail(email), enabled: Boolean(email), staleTime: 60_000 }) }
export function useUserByUsername(username: string) { return useQuery({ queryKey: ['usuario', 'username', username], queryFn: () => api.userByUsername(username), enabled: Boolean(username), staleTime: 60_000 }) }
