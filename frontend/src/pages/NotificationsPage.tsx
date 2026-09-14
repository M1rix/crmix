import { useQuery } from '@tanstack/react-query'
import { PageHeader } from '../components/AppShell'
import { api } from '../lib/api'

type Notification = {
  id: string
  clientId: string
  appointmentId?: string
  templateCode: string
  status: 'PENDING' | 'RETRY' | 'SENT' | 'FAILED' | 'CANCELLED' | 'SKIPPED'
  scheduledFor: string
  attempts: number
  sentAt?: string
  error?: string
}

export function NotificationsPage() {
  const list = useQuery({ queryKey: ['notifications'], queryFn: () => api<Notification[]>('/notifications'), refetchInterval: 30_000 })
  return <>
    <PageHeader title="Notifications" description="Telegram reminder queue, delivery attempts and terminal outcomes." />
    <div className="overflow-hidden rounded-2xl border border-white/10">
      <table className="w-full text-left text-sm">
        <thead className="bg-white/5 text-xs uppercase tracking-wide text-slate-500"><tr><th className="px-4 py-3">Reminder</th><th className="px-4 py-3">Scheduled</th><th className="px-4 py-3">Status</th><th className="px-4 py-3">Attempts</th><th className="px-4 py-3">Details</th></tr></thead>
        <tbody>
          {(list.data ?? []).map((item) => <tr key={item.id} className="border-t border-white/5"><td className="px-4 py-3 text-slate-300">{item.templateCode.replace('APPOINTMENT_REMINDER_', '')}</td><td className="px-4 py-3 text-slate-400">{new Date(item.scheduledFor).toLocaleString()}</td><td className="px-4 py-3"><span className="rounded-full border border-white/10 bg-white/5 px-2.5 py-1 text-xs text-cyan-200">{item.status}</span></td><td className="px-4 py-3 text-slate-400">{item.attempts}/3</td><td className="max-w-md truncate px-4 py-3 text-slate-500">{item.error ?? (item.sentAt ? `Sent ${new Date(item.sentAt).toLocaleString()}` : '—')}</td></tr>)}
          {!list.isLoading && (list.data?.length ?? 0) === 0 && <tr><td colSpan={5} className="px-4 py-12 text-center text-slate-500">No reminders have been queued yet.</td></tr>}
        </tbody>
      </table>
    </div>
  </>
}
