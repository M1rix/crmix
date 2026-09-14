import { createContext } from 'react'
import type { AuthTokens } from '../lib/api'

export type LoginInput = { tenantSlug: string; email: string; password: string }
export type RegisterInput = {
  slug: string
  businessName: string
  businessType: string
  ownerFullName: string
  email: string
  password: string
  phone?: string
}

export type AuthContextValue = {
  auth: AuthTokens | null
  login: (input: LoginInput) => Promise<void>
  register: (input: RegisterInput) => Promise<void>
  logout: () => Promise<void>
}

export const AuthContext = createContext<AuthContextValue | null>(null)
