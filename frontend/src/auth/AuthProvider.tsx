import { createContext, useContext, useMemo, useState, type ReactNode } from 'react'
import { api, loadAuth, saveAuth, type AuthTokens } from '../lib/api'

type LoginInput = { tenantSlug: string; email: string; password: string }
type RegisterInput = {
  slug: string
  businessName: string
  businessType: string
  ownerFullName: string
  email: string
  password: string
  phone?: string
}

type AuthContextValue = {
  auth: AuthTokens | null
  login: (input: LoginInput) => Promise<void>
  register: (input: RegisterInput) => Promise<void>
  logout: () => Promise<void>
}

const AuthContext = createContext<AuthContextValue | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [auth, setAuth] = useState<AuthTokens | null>(() => loadAuth())
  const value = useMemo<AuthContextValue>(
    () => ({
      auth,
      login: async (input) => {
        const tokens = await api<AuthTokens>('/auth/login', { method: 'POST', body: JSON.stringify(input) })
        saveAuth(tokens)
        setAuth(tokens)
      },
      register: async (input) => {
        const tokens = await api<AuthTokens>('/tenants/register', { method: 'POST', body: JSON.stringify(input) })
        saveAuth(tokens)
        setAuth(tokens)
      },
      logout: async () => {
        if (auth?.refreshToken) {
          await api<void>('/auth/logout', { method: 'POST', body: JSON.stringify({ refreshToken: auth.refreshToken }) }).catch(() => undefined)
        }
        saveAuth(null)
        setAuth(null)
      },
    }),
    [auth],
  )
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) throw new Error('useAuth must be used inside AuthProvider')
  return context
}
