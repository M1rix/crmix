import { cardClass, PageHeader } from '../components/AppShell'

export function OverviewPage() {
  return <><PageHeader title="Overview" description="Operational snapshot for the current workspace."/><div className="grid gap-4 md:grid-cols-3">{['Revenue', 'Appointments', 'No-show rate'].map((label) => <div key={label} className={cardClass}><p className="text-sm text-slate-500">{label}</p><p className="mt-3 text-3xl font-semibold">—</p><p className="mt-2 text-xs text-slate-600">Analytics activates in M06</p></div>)}</div></>
}
