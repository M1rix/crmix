import { useQuery } from '@tanstack/react-query'
import { Activity, CalendarCheck2, CircleX, TrendingUp, UserRoundCheck } from 'lucide-react'
import { Area, AreaChart, Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { cardClass, PageHeader } from '../components/AppShell'
import { api } from '../lib/api'

type Dashboard = {
  from: string
  to: string
  revenue: number
  appointments: number
  cancellationRate: number
  noShowRate: number
  dailyRevenue: { day: string; revenue: number }[]
  employeeLoad: { employeeId: string; employeeName: string; appointmentCount: number; bookedMinutes: number }[]
  topServices: { serviceId: string; serviceName: string; appointmentCount: number }[]
}

export function OverviewPage() {
  const to = new Date()
  const from = new Date(to)
  from.setDate(from.getDate() - 29)
  const fromValue = localIsoDate(from)
  const toValue = localIsoDate(to)
  const dashboard = useQuery({
    queryKey: ['analytics-dashboard', fromValue, toValue],
    queryFn: () => api<Dashboard>(`/analytics/dashboard?from=${fromValue}&to=${toValue}`),
  })

  const data = dashboard.data
  return <>
    <PageHeader title="Overview" description="Business performance for the last 30 days, calculated from tenant-isolated operational data." />

    {dashboard.error && <div className="mb-5 rounded-xl border border-rose-400/20 bg-rose-400/5 px-4 py-3 text-sm text-rose-200">{dashboard.error.message}</div>}

    <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
      <MetricCard label="Revenue" value={data ? `${formatMoney(data.revenue)} UZS` : '—'} icon={<TrendingUp size={19}/>} />
      <MetricCard label="Appointments" value={data ? String(data.appointments) : '—'} icon={<CalendarCheck2 size={19}/>} />
      <MetricCard label="Cancellation rate" value={data ? `${data.cancellationRate.toFixed(1)}%` : '—'} icon={<CircleX size={19}/>} />
      <MetricCard label="No-show rate" value={data ? `${data.noShowRate.toFixed(1)}%` : '—'} icon={<Activity size={19}/>} />
    </div>

    <div className="mt-6 grid gap-5 xl:grid-cols-[1.5fr_1fr]">
      <section className={cardClass}>
        <div className="mb-5 flex items-center justify-between"><div><h2 className="font-semibold">Revenue trend</h2><p className="mt-1 text-xs text-slate-500">Completed appointment payments only</p></div><span className="text-xs text-slate-500">{fromValue} → {toValue}</span></div>
        <div className="h-72">
          <ResponsiveContainer width="100%" height="100%">
            <AreaChart data={data?.dailyRevenue ?? []} margin={{ left: 4, right: 10 }}>
              <defs><linearGradient id="revenueFill" x1="0" y1="0" x2="0" y2="1"><stop offset="5%" stopColor="currentColor" stopOpacity={0.28}/><stop offset="95%" stopColor="currentColor" stopOpacity={0}/></linearGradient></defs>
              <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="rgba(148,163,184,.12)" />
              <XAxis dataKey="day" tick={{ fill: '#64748b', fontSize: 11 }} axisLine={false} tickLine={false} minTickGap={24} />
              <YAxis tick={{ fill: '#64748b', fontSize: 11 }} axisLine={false} tickLine={false} tickFormatter={compactMoney} width={55} />
              <Tooltip contentStyle={{ background: '#0f172a', border: '1px solid rgba(255,255,255,.1)', borderRadius: 12 }} formatter={(value) => [`${formatMoney(Number(value ?? 0))} UZS`, 'Revenue']} />
              <Area type="monotone" dataKey="revenue" stroke="currentColor" fill="url(#revenueFill)" className="text-cyan-300" strokeWidth={2} />
            </AreaChart>
          </ResponsiveContainer>
        </div>
        {!dashboard.isLoading && (data?.dailyRevenue.length ?? 0) === 0 && <EmptyOverlay text="Record completed appointment payments to populate revenue." />}
      </section>

      <section className={cardClass}>
        <div className="mb-5"><h2 className="font-semibold">Top services</h2><p className="mt-1 text-xs text-slate-500">By non-cancelled appointment count</p></div>
        <div className="h-72">
          <ResponsiveContainer width="100%" height="100%">
            <BarChart data={data?.topServices ?? []} layout="vertical" margin={{ left: 10, right: 10 }}>
              <CartesianGrid strokeDasharray="3 3" horizontal={false} stroke="rgba(148,163,184,.12)" />
              <XAxis type="number" allowDecimals={false} tick={{ fill: '#64748b', fontSize: 11 }} axisLine={false} tickLine={false} />
              <YAxis type="category" dataKey="serviceName" width={90} tick={{ fill: '#94a3b8', fontSize: 11 }} axisLine={false} tickLine={false} />
              <Tooltip contentStyle={{ background: '#0f172a', border: '1px solid rgba(255,255,255,.1)', borderRadius: 12 }} />
              <Bar dataKey="appointmentCount" fill="currentColor" className="text-cyan-300" radius={[0, 5, 5, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </div>
      </section>
    </div>

    <section className={`${cardClass} mt-5`}>
      <div className="mb-4 flex items-center gap-2"><UserRoundCheck size={18} className="text-cyan-300"/><div><h2 className="font-semibold">Employee load</h2><p className="text-xs text-slate-500">Booked minutes and appointments for active employees</p></div></div>
      <div className="overflow-x-auto"><table className="w-full text-left text-sm"><thead className="text-xs uppercase tracking-wide text-slate-500"><tr><th className="py-3">Employee</th><th className="py-3 text-right">Appointments</th><th className="py-3 text-right">Booked time</th></tr></thead><tbody>{(data?.employeeLoad ?? []).map((row) => <tr key={row.employeeId} className="border-t border-white/5"><td className="py-3 text-slate-200">{row.employeeName}</td><td className="py-3 text-right text-slate-400">{row.appointmentCount}</td><td className="py-3 text-right text-slate-400">{formatMinutes(row.bookedMinutes)}</td></tr>)}</tbody></table></div>
    </section>
  </>
}

function MetricCard({ label, value, icon }: { label: string; value: string; icon: React.ReactNode }) {
  return <div className={cardClass}><div className="flex items-center justify-between"><p className="text-sm text-slate-500">{label}</p><span className="text-cyan-300">{icon}</span></div><p className="mt-3 text-2xl font-semibold tracking-tight">{value}</p></div>
}

function EmptyOverlay({ text }: { text: string }) {
  return <p className="-mt-8 text-center text-xs text-slate-600">{text}</p>
}

function localIsoDate(value: Date) {
  const year = value.getFullYear()
  const month = String(value.getMonth() + 1).padStart(2, '0')
  const day = String(value.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function formatMoney(value: number) {
  return new Intl.NumberFormat('ru-RU', { maximumFractionDigits: 0 }).format(value)
}

function compactMoney(value: number) {
  return new Intl.NumberFormat('en', { notation: 'compact', maximumFractionDigits: 1 }).format(value)
}

function formatMinutes(value: number) {
  if (value < 60) return `${value}m`
  const hours = Math.floor(value / 60)
  const minutes = value % 60
  return minutes ? `${hours}h ${minutes}m` : `${hours}h`
}
