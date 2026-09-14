import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Plus, Search } from 'lucide-react'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { z } from 'zod'
import { buttonClass, cardClass, inputClass, PageHeader } from '../components/AppShell'
import { api, type PageResponse } from '../lib/api'

type Client = { id: string; fullName: string; phone: string; telegramChatId?: number; notes?: string }
type Employee = { id: string; fullName: string; specialization?: string; active: boolean; workSchedule: Record<string, unknown> }
type Service = { id: string; name: string; durationMinutes: number; price: number; active: boolean }

const clientSchema = z.object({ fullName: z.string().min(2), phone: z.string().min(7), notes: z.string().max(4000).optional() })
const employeeSchema = z.object({ fullName: z.string().min(2), specialization: z.string().max(160).optional() })
const serviceSchema = z.object({ name: z.string().min(2), durationMinutes: z.number().min(5).max(1440), price: z.number().min(0) })

export function ClientsPage() {
  const [query, setQuery] = useState('')
  const [showForm, setShowForm] = useState(false)
  const client = useQueryClient()
  const list = useQuery({ queryKey: ['clients', query], queryFn: () => api<PageResponse<Client>>(`/clients?query=${encodeURIComponent(query)}&size=50`) })
  const form = useForm<z.infer<typeof clientSchema>>({ resolver: zodResolver(clientSchema) })
  const create = useMutation({ mutationFn: (values: z.infer<typeof clientSchema>) => api<Client>('/clients', { method: 'POST', body: JSON.stringify(values) }), onSuccess: async () => { form.reset(); setShowForm(false); await client.invalidateQueries({ queryKey: ['clients'] }) } })
  return <>
    <PageHeader title="Clients" description="One customer record per phone number inside this workspace." />
    <Toolbar query={query} setQuery={setQuery} onAdd={() => setShowForm((v) => !v)} label="Add client" />
    {showForm && <form className={`${cardClass} mb-5 grid gap-3 md:grid-cols-3`} onSubmit={form.handleSubmit((v) => create.mutate(v))}>
      <input className={inputClass} placeholder="Full name" {...form.register('fullName')} />
      <input className={inputClass} placeholder="Phone" {...form.register('phone')} />
      <div className="flex gap-2"><input className={inputClass} placeholder="Notes" {...form.register('notes')} /><button className={buttonClass}>Save</button></div>
    </form>}
    <Table headers={['Client', 'Phone', 'Telegram']} rows={(list.data?.content ?? []).map((row) => [row.fullName, row.phone, row.telegramChatId ? 'Connected' : 'Not linked'])} />
  </>
}

export function EmployeesPage() {
  const [showForm, setShowForm] = useState(false)
  const client = useQueryClient()
  const list = useQuery({ queryKey: ['employees'], queryFn: () => api<PageResponse<Employee>>('/employees?size=50') })
  const form = useForm<z.infer<typeof employeeSchema>>({ resolver: zodResolver(employeeSchema) })
  const create = useMutation({ mutationFn: (v: z.infer<typeof employeeSchema>) => api<Employee>('/employees', { method: 'POST', body: JSON.stringify({ ...v, active: true, workSchedule: {} }) }), onSuccess: async () => { form.reset(); setShowForm(false); await client.invalidateQueries({ queryKey: ['employees'] }) } })
  return <>
    <PageHeader title="Employees" description="Plan limits are enforced server-side; inactive employees remain in history." />
    <div className="mb-5 flex justify-end"><button className={buttonClass} onClick={() => setShowForm((v) => !v)}><Plus className="mr-2 inline" size={16}/>Add employee</button></div>
    {showForm && <form className={`${cardClass} mb-5 grid gap-3 md:grid-cols-3`} onSubmit={form.handleSubmit((v) => create.mutate(v))}><input className={inputClass} placeholder="Full name" {...form.register('fullName')} /><input className={inputClass} placeholder="Specialization" {...form.register('specialization')} /><button className={buttonClass}>Save</button></form>}
    <Table headers={['Employee', 'Specialization', 'Status']} rows={(list.data?.content ?? []).map((row) => [row.fullName, row.specialization ?? '—', row.active ? 'Active' : 'Inactive'])} />
  </>
}

export function ServicesPage() {
  const [showForm, setShowForm] = useState(false)
  const client = useQueryClient()
  const list = useQuery({ queryKey: ['services'], queryFn: () => api<PageResponse<Service>>('/services?size=50') })
  const form = useForm<z.infer<typeof serviceSchema>>({ resolver: zodResolver(serviceSchema), defaultValues: { durationMinutes: 60, price: 0 } })
  const create = useMutation({ mutationFn: (v: z.infer<typeof serviceSchema>) => api<Service>('/services', { method: 'POST', body: JSON.stringify({ ...v, active: true }) }), onSuccess: async () => { form.reset(); setShowForm(false); await client.invalidateQueries({ queryKey: ['services'] }) } })
  return <>
    <PageHeader title="Services" description="Duration and price are snapshotted when an appointment is created." />
    <div className="mb-5 flex justify-end"><button className={buttonClass} onClick={() => setShowForm((v) => !v)}><Plus className="mr-2 inline" size={16}/>Add service</button></div>
    {showForm && <form className={`${cardClass} mb-5 grid gap-3 md:grid-cols-4`} onSubmit={form.handleSubmit((v) => create.mutate(v))}><input className={inputClass} placeholder="Service name" {...form.register('name')} /><input className={inputClass} type="number" {...form.register('durationMinutes', { valueAsNumber: true })} /><input className={inputClass} type="number" step="0.01" {...form.register('price', { valueAsNumber: true })} /><button className={buttonClass}>Save</button></form>}
    <Table headers={['Service', 'Duration', 'Price', 'Status']} rows={(list.data?.content ?? []).map((row) => [row.name, `${row.durationMinutes} min`, new Intl.NumberFormat().format(row.price), row.active ? 'Active' : 'Inactive'])} />
  </>
}

function Toolbar({ query, setQuery, onAdd, label }: { query: string; setQuery: (v: string) => void; onAdd: () => void; label: string }) {
  return <div className="mb-5 flex gap-3"><label className="relative flex-1"><Search className="absolute left-3 top-3 text-slate-500" size={17}/><input className={`${inputClass} pl-10`} value={query} onChange={(e) => setQuery(e.target.value)} placeholder="Search by name or phone" /></label><button className={buttonClass} onClick={onAdd}><Plus className="mr-2 inline" size={16}/>{label}</button></div>
}

function Table({ headers, rows }: { headers: string[]; rows: (string | number)[][] }) {
  return <div className="overflow-hidden rounded-2xl border border-white/10"><table className="w-full text-left text-sm"><thead className="bg-white/5 text-xs uppercase tracking-wide text-slate-500"><tr>{headers.map((h) => <th key={h} className="px-4 py-3">{h}</th>)}</tr></thead><tbody>{rows.map((row, index) => <tr key={index} className="border-t border-white/5">{row.map((cell, i) => <td key={i} className="px-4 py-3 text-slate-300">{cell}</td>)}</tr>)}{rows.length === 0 && <tr><td colSpan={headers.length} className="px-4 py-10 text-center text-slate-500">No data yet</td></tr>}</tbody></table></div>
}
