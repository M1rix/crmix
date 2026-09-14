export type AuthTokens = {
  accessToken: string
  refreshToken: string
  expiresIn: number
  tenantId: string
  role: 'OWNER' | 'ADMIN' | 'STAFF'
}

export type PageResponse<T> = {
  content: T[]
  totalElements: number
  totalPages: number
  page: number
  size: number
}

const TOKEN_KEY = 'crmix.auth'

export function loadAuth(): AuthTokens | null {
  const raw = localStorage.getItem(TOKEN_KEY)
  if (!raw) return null
  try {
    return JSON.parse(raw) as AuthTokens
  } catch {
    localStorage.removeItem(TOKEN_KEY)
    return null
  }
}

export function saveAuth(tokens: AuthTokens | null) {
  if (tokens) localStorage.setItem(TOKEN_KEY, JSON.stringify(tokens))
  else localStorage.removeItem(TOKEN_KEY)
}

export async function api<T>(path: string, init: RequestInit = {}, retry = true): Promise<T> {
  const auth = loadAuth()
  const headers = new Headers(init.headers)
  if (!headers.has('Content-Type') && init.body) headers.set('Content-Type', 'application/json')
  if (auth?.accessToken) headers.set('Authorization', `Bearer ${auth.accessToken}`)

  const response = await fetch(`/api/v1${path}`, { ...init, headers })
  if (response.status === 401 && retry && auth?.refreshToken && !path.startsWith('/auth/')) {
    const refreshed = await refresh(auth.refreshToken)
    if (refreshed) return api<T>(path, init, false)
  }
  if (!response.ok) {
    const problem = (await response.json().catch(() => null)) as { detail?: string; title?: string } | null
    throw new Error(problem?.detail ?? problem?.title ?? `Request failed with ${response.status}`)
  }
  if (response.status === 204) return undefined as T
  return (await response.json()) as T
}

async function refresh(refreshToken: string) {
  const response = await fetch('/api/v1/auth/refresh', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ refreshToken }),
  })
  if (!response.ok) {
    saveAuth(null)
    return false
  }
  saveAuth((await response.json()) as AuthTokens)
  return true
}
