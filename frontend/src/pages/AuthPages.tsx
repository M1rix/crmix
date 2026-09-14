import { zodResolver } from '@hookform/resolvers/zod'
import { useForm } from 'react-hook-form'
import { Link, Navigate, useNavigate } from 'react-router-dom'
import { z } from 'zod'
import { useAuth } from '../auth/AuthProvider'
import { buttonClass, inputClass } from '../components/AppShell'

const loginSchema = z.object({ tenantSlug: z.string().min(3), email: z.email(), password: z.string().min(10) })
const registerSchema = z.object({
  slug: z.string().regex(/^[a-z0-9][a-z0-9-]{2,79}$/),
  businessName: z.string().min(2).max(160),
  businessType: z.string().min(2).max(80),
  ownerFullName: z.string().min(2).max(160),
  email: z.email(),
  password: z.string().min(10).max(100),
  phone: z.string().max(40).optional(),
})

type LoginValues = z.infer<typeof loginSchema>
type RegisterValues = z.infer<typeof registerSchema>

function AuthLayout({ title, subtitle, children }: { title: string; subtitle: string; children: React.ReactNode }) {
  return (
    <main className="grid min-h-screen place-items-center bg-[radial-gradient(circle_at_top,#164e63_0,transparent_32%)] p-5">
      <section className="w-full max-w-md rounded-3xl border border-white/10 bg-slate-950/80 p-7 shadow-2xl backdrop-blur">
        <p className="text-xs font-bold tracking-[0.3em] text-cyan-300">CRMIX</p>
        <h1 className="mt-5 text-3xl font-semibold">{title}</h1>
        <p className="mt-2 text-sm text-slate-400">{subtitle}</p>
        <div className="mt-7">{children}</div>
      </section>
    </main>
  )
}

export function LoginPage() {
  const { auth, login } = useAuth()
  const navigate = useNavigate()
  const form = useForm<LoginValues>({ resolver: zodResolver(loginSchema) })
  if (auth) return <Navigate to="/" replace />
  return (
    <AuthLayout title="Welcome back" subtitle="Use the workspace slug assigned to your business.">
      <form className="space-y-4" onSubmit={form.handleSubmit(async (values) => { await login(values); navigate('/') })}>
        <Field label="Workspace" error={form.formState.errors.tenantSlug?.message}><input className={inputClass} placeholder="mirix-salon" {...form.register('tenantSlug')} /></Field>
        <Field label="Email" error={form.formState.errors.email?.message}><input className={inputClass} type="email" {...form.register('email')} /></Field>
        <Field label="Password" error={form.formState.errors.password?.message}><input className={inputClass} type="password" {...form.register('password')} /></Field>
        {form.formState.errors.root && <p className="text-sm text-rose-300">{form.formState.errors.root.message}</p>}
        <button className={`${buttonClass} w-full`} disabled={form.formState.isSubmitting}>Sign in</button>
      </form>
      <p className="mt-5 text-center text-sm text-slate-500">New business? <Link className="text-cyan-300" to="/register">Create workspace</Link></p>
    </AuthLayout>
  )
}

export function RegisterPage() {
  const { auth, register } = useAuth()
  const navigate = useNavigate()
  const form = useForm<RegisterValues>({ resolver: zodResolver(registerSchema) })
  if (auth) return <Navigate to="/" replace />
  return (
    <AuthLayout title="Create workspace" subtitle="Your first 14 days start on the Starter plan.">
      <form className="space-y-3" onSubmit={form.handleSubmit(async (values) => { await register(values); navigate('/') })}>
        <input className={inputClass} placeholder="Workspace slug" {...form.register('slug')} />
        <input className={inputClass} placeholder="Business name" {...form.register('businessName')} />
        <input className={inputClass} placeholder="Business type" {...form.register('businessType')} />
        <input className={inputClass} placeholder="Your full name" {...form.register('ownerFullName')} />
        <input className={inputClass} type="email" placeholder="Email" {...form.register('email')} />
        <input className={inputClass} type="password" placeholder="Password (10+ chars)" {...form.register('password')} />
        <input className={inputClass} placeholder="Phone (optional)" {...form.register('phone')} />
        <button className={`${buttonClass} w-full`} disabled={form.formState.isSubmitting}>Create CRMIX workspace</button>
      </form>
      <p className="mt-5 text-center text-sm text-slate-500">Already registered? <Link className="text-cyan-300" to="/login">Sign in</Link></p>
    </AuthLayout>
  )
}

function Field({ label, error, children }: { label: string; error?: string; children: React.ReactNode }) {
  return <label className="block text-sm text-slate-300"><span className="mb-1.5 block">{label}</span>{children}{error && <span className="mt-1 block text-xs text-rose-300">{error}</span>}</label>
}
