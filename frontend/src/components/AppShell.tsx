import { BarChart3, CalendarDays, ContactRound, LogOut, Scissors, UsersRound } from 'lucide-react'
import { NavLink, Outlet } from 'react-router-dom'
import { useAuth } from '../auth/AuthProvider'

const links = [
  { to: '/', label: 'Overview', icon: BarChart3 },
  { to: '/clients', label: 'Clients', icon: ContactRound },
  { to: '/employees', label: 'Employees', icon: UsersRound },
  { to: '/services', label: 'Services', icon: Scissors },
  { to: '/appointments', label: 'Appointments', icon: CalendarDays },
]

export function AppShell() {
  const { auth, logout } = useAuth()
  return (
    <div className="min-h-screen bg-slate-950 text-slate-100">
      <aside className="fixed inset-y-0 left-0 hidden w-64 border-r border-white/10 bg-slate-950/95 p-5 lg:block">
        <div className="mb-8">
          <p className="text-xs font-bold tracking-[0.3em] text-cyan-300">CRMIX</p>
          <p className="mt-2 text-sm text-slate-400">Tenant {auth?.tenantId.slice(0, 8)}</p>
        </div>
        <nav className="space-y-1">
          {links.map(({ to, label, icon: Icon }) => (
            <NavLink
              key={to}
              to={to}
              end={to === '/'}
              className={({ isActive }) =>
                `flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm ${isActive ? 'bg-cyan-400/10 text-cyan-200' : 'text-slate-400 hover:bg-white/5 hover:text-white'}`
              }
            >
              <Icon size={18} /> {label}
            </NavLink>
          ))}
        </nav>
        <button onClick={() => logout()} className="absolute bottom-6 flex items-center gap-2 text-sm text-slate-500 hover:text-white">
          <LogOut size={17} /> Sign out
        </button>
      </aside>
      <main className="lg:pl-64">
        <div className="mx-auto max-w-7xl p-5 md:p-8">
          <Outlet />
        </div>
      </main>
    </div>
  )
}

export function PageHeader({ title, description }: { title: string; description: string }) {
  return (
    <div className="mb-6">
      <h1 className="text-2xl font-semibold tracking-tight">{title}</h1>
      <p className="mt-1 text-sm text-slate-400">{description}</p>
    </div>
  )
}

export const inputClass = 'w-full rounded-xl border border-white/10 bg-slate-900 px-3 py-2.5 text-sm outline-none focus:border-cyan-400/50'
export const buttonClass = 'rounded-xl bg-cyan-300 px-4 py-2.5 text-sm font-semibold text-slate-950 hover:bg-cyan-200 disabled:opacity-50'
export const cardClass = 'rounded-2xl border border-white/10 bg-white/[0.035] p-5'
