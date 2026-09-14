import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { CalendarPlus, Check, CircleX } from 'lucide-react'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { z } from 'zod'
import { buttonClass, cardClass, inputClass, PageHeader } from '../components/AppShell'
import { api, type PageResponse } from '../lib/api'

type Client = { id: string; fullName: string; phone: string }
type Employee = { id: string; fullName: string; active: boolean }
type Service = { id: string; name: string; durationMinutes: number; active: boolean }
type Appointment = {
  id: string
  clientId: string
  employeeId: string
  serviceId: string
  scheduledAt: string
  durationMinutes: number
  status: 'SCHEDULED' | 'CONFIRMED' | 'COMPLETED' | 'CANCELLED' | 'NO_SHOW'
  cancellationReason?: string
}

const schema = z.object({
  clientId: z.string().uuid(),
  employeeId: z.string().uuid(),
  serviceId: z.string().uuid(),
  scheduledAt: z.string().min(1),
})

type Values = z.infer<typeof schema>

export function AppointmentsPage() {
  const [showForm, setShowForm] = useState(false)
  const queryClient = useQueryClient()
  const appointments = useQuery({ queryKey: ['appointments'], queryFn: () => api<PageResponse<Appointment>>('/appointments?size=100') })
  const clients = useQuery({ queryKey: ['clients', 'appointment-select'], queryFn: () => api<PageResponse<Client>>('/clients?size=100') })
  const employees = useQuery({ queryKey: ['employees'], queryFn: () => api<PageResponse<Employee>>('/employees?size=100') })
  const services = useQuery({ queryKey: ['services'], queryFn: () => api<PageResponse<Service>>('/services?size=100') })
  const form = useForm<Values>({ resolver: zodResolver(schema) })
  const invalidate = () => queryClient.invalidateQueries({ queryKey: ['appointments'] })
  const create = useMutation({
    mutationFn: (values: Values) => api<Appointment>('/appointments', { method: 'POST', body: JSON.stringify({ ...values, scheduledAt: new Date(values.scheduledAt).toISOString() }) }),
    onSuccess: async () => { form.reset(); setShowForm(false); await invalidate() },
  })
  const transition = useMutation({
    mutationFn: ({ id, status, cancellationReason }: { id: string; status: Appointment['status']; cancellationReason?: string }) =>
      api<Appointment>(`/appointments/${id}/status`, { method: 'PATCH', body: JSON.stringify({ status, cancellationReason }) }),
    onSuccess: invalidate,
  })

  const clientById = new Map((clients.data?.content ?? []).map((item) => [item.id, item]))
  const employeeById = new Map((employees.data?.content ?? []).map((item) => [item.id, item]))
  const serviceById = new Map((services.data?.content ?? []).map((item) => [item.id, item]))

  return <>
    <div className="flex items-start justify-between gap-4">
      <PageHeader title="Appointments" description="The database rejects overlapping slots even when requests race." />
      <button className={buttonClass} onClick={() => setShowForm((value) => !value)}><CalendarPlus className="mr-2 inline" size={17}/>New appointment</button>
    </div>

    {showForm && <form className={`${cardClass} mb-6 grid gap-3 md:grid-cols-2 xl:grid-cols-5`} onSubmit={form.handleSubmit((values) => create.mutate(values))}>
      <Select className={inputClass} {...form.register('clientId')} placeholder="Client" options={(clients.data?.content ?? []).map((item) => ({ value: item.id, label: `${item.fullName} · ${item.phone}` }))} />
      <Select className={inputClass} {...form.register('employeeId')} placeholder="Employee" options={(employees.data?.content ?? []).filter((item) => item.active).map((item) => ({ value: item.id, label: item.fullName }))} />
      <Select className={inputClass} {...form.register('serviceId')} placeholder="Service" options={(services.data?.content ?? []).filter((item) => item.active).map((item) => ({ value: item.id, label: `${item.name} · ${item.durationMinutes}m` }))} />
      <input className={inputClass} type="datetime-local" {...form.register('scheduledAt')} />
      <button className={buttonClass} disabled={create.isPending}>Create</button>
      {create.error && <p className="text-sm text-rose-300 md:col-span-2 xl:col-span-5">{create.error.message}</p>}
    </form>}

    <div className="space-y-3">
      {(appointments.data?.content ?? []).map((item) => {
        const client = clientById.get(item.clientId)
        const employee = employeeById.get(item.employeeId)
        const service = serviceById.get(item.serviceId)
        return <article key={item.id} className={`${cardClass} flex flex-col gap-4 md:flex-row md:items-center md:justify-between`}>
          <div>
            <div className="flex items-center gap-2"><StatusBadge status={item.status}/><span className="text-sm text-slate-500">{new Date(item.scheduledAt).toLocaleString()}</span></div>
            <h3 className="mt-2 font-medium">{client?.fullName ?? 'Client'} · {service?.name ?? 'Service'}</h3>
            <p className="mt-1 text-sm text-slate-500">{employee?.fullName ?? 'Employee'} · {item.durationMinutes} min</p>
          </div>
          <div className="flex flex-wrap gap-2">
            {item.status === 'SCHEDULED' && <Action onClick={() => transition.mutate({ id: item.id, status: 'CONFIRMED' })}><Check size={15}/>Confirm</Action>}
            {item.status === 'CONFIRMED' && <Action onClick={() => transition.mutate({ id: item.id, status: 'COMPLETED' })}><Check size={15}/>Complete</Action>}
            {(item.status === 'SCHEDULED' || item.status === 'CONFIRMED') && <Action onClick={() => { const reason = window.prompt('Cancellation reason'); if (reason) transition.mutate({ id: item.id, status: 'CANCELLED', cancellationReason: reason }) }}><CircleX size={15}/>Cancel</Action>}
          </div>
        </article>
      })}
      {!appointments.isLoading && (appointments.data?.content.length ?? 0) === 0 && <div className={`${cardClass} py-16 text-center text-slate-500`}>No appointments in the selected period.</div>}
    </div>
  </>
}

function Select({ placeholder, options, ...props }: React.SelectHTMLAttributes<HTMLSelectElement> & { placeholder: string; options: { value: string; label: string }[] }) {
  return <select {...props}><option value="">{placeholder}</option>{options.map((option) => <option key={option.value} value={option.value}>{option.label}</option>)}</select>
}

function Action({ children, onClick }: { children: React.ReactNode; onClick: () => void }) {
  return <button onClick={onClick} className="flex items-center gap-1.5 rounded-lg border border-white/10 px-3 py-2 text-xs text-slate-300 hover:bg-white/5">{children}</button>
}

function StatusBadge({ status }: { status: Appointment['status'] }) {
  return <span className="rounded-full border border-white/10 bg-white/5 px-2.5 py-1 text-[11px] font-semibold tracking-wide text-cyan-200">{status.replace('_', ' ')}</span>
}
