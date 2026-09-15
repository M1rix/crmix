import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Check, CreditCard, ExternalLink } from 'lucide-react'
import { useState } from 'react'
import { buttonClass, cardClass, PageHeader } from '../components/AppShell'
import { api } from '../lib/api'

type Plan = {
  id: string
  code: string
  name: string
  priceMonthly: number
  maxEmployees: number
  features: Record<string, unknown>
}

type BillingState = {
  status: 'TRIAL' | 'ACTIVE' | 'SUSPENDED'
  currentPlan?: string
  nextPlan?: string
  trialEndsAt?: string
  subscriptionExpiresAt?: string
}

type Payment = {
  id: string
  planId?: string
  amount: number
  currency: string
  provider: string
  status: string
  providerTransactionId?: string
  checkoutUrl?: string
  createdAt: string
}

export function BillingPage() {
  const queryClient = useQueryClient()
  const [message, setMessage] = useState<string | null>(null)
  const plans = useQuery({ queryKey: ['billing-plans'], queryFn: () => api<Plan[]>('/billing/plans') })
  const state = useQuery({ queryKey: ['billing-state'], queryFn: () => api<BillingState>('/billing/state') })
  const payments = useQuery({ queryKey: ['billing-payments'], queryFn: () => api<Payment[]>('/billing/payments') })

  const createPayment = useMutation({
    mutationFn: (planCode: string) => api<Payment>('/billing/payments', {
      method: 'POST',
      headers: { 'Idempotency-Key': crypto.randomUUID() },
      body: JSON.stringify({ planCode }),
    }),
    onSuccess: async (payment) => {
      await queryClient.invalidateQueries({ queryKey: ['billing-payments'] })
      if (payment.checkoutUrl) window.location.assign(payment.checkoutUrl)
      else setMessage('Payment intent created. Configure PAYME_MERCHANT_ID to enable checkout redirect.')
    },
    onError: (error) => setMessage(error instanceof Error ? error.message : 'Unable to create payment'),
  })

  const changePlan = useMutation({
    mutationFn: (planCode: string) => api<BillingState>('/billing/plan-change', {
      method: 'POST',
      body: JSON.stringify({ planCode }),
    }),
    onSuccess: async () => {
      setMessage('Plan change scheduled for the end of the current billing period.')
      await queryClient.invalidateQueries({ queryKey: ['billing-state'] })
    },
    onError: (error) => setMessage(error instanceof Error ? error.message : 'Unable to change plan'),
  })

  const current = plans.data?.find((plan) => plan.code === state.data?.currentPlan)
  const busy = createPayment.isPending || changePlan.isPending

  return <>
    <PageHeader title="Billing" description="Manage your subscription and review Payme payment history." />

    <section className={`${cardClass} mb-6 flex flex-wrap items-center justify-between gap-4`}>
      <div>
        <p className="text-xs font-semibold uppercase tracking-[0.2em] text-slate-500">Subscription</p>
        <div className="mt-2 flex items-center gap-3">
          <span className="text-xl font-semibold">{state.data?.currentPlan ?? '—'}</span>
          <span className="rounded-full bg-cyan-400/10 px-2.5 py-1 text-xs font-semibold text-cyan-200">{state.data?.status ?? '...'}</span>
        </div>
        {state.data?.nextPlan && <p className="mt-2 text-sm text-amber-300">Scheduled next plan: {state.data.nextPlan}</p>}
      </div>
      <div className="text-right text-sm text-slate-400">
        {state.data?.status === 'TRIAL' && state.data.trialEndsAt && <p>Trial ends {formatDate(state.data.trialEndsAt)}</p>}
        {state.data?.subscriptionExpiresAt && <p>Paid until {formatDate(state.data.subscriptionExpiresAt)}</p>}
      </div>
    </section>

    {message && <div className="mb-5 rounded-xl border border-cyan-400/20 bg-cyan-400/5 px-4 py-3 text-sm text-cyan-100">{message}</div>}

    <div className="mb-8 grid gap-4 md:grid-cols-2">
      {(plans.data ?? []).map((plan) => {
        const isCurrent = plan.code === state.data?.currentPlan
        const isScheduled = plan.code === state.data?.nextPlan
        const isUpgrade = !current || plan.priceMonthly > current.priceMonthly
        return <article key={plan.id} className={`${cardClass} relative`}>
          {(isCurrent || isScheduled) && <span className="absolute right-4 top-4 rounded-full bg-white/5 px-2.5 py-1 text-xs text-slate-300">{isCurrent ? 'Current' : 'Scheduled'}</span>}
          <h2 className="text-lg font-semibold">{plan.name}</h2>
          <p className="mt-2 text-3xl font-semibold">{plan.priceMonthly === 0 ? 'Free' : `${formatMoney(plan.priceMonthly)} UZS`}</p>
          <p className="mt-1 text-sm text-slate-500">per month · up to {plan.maxEmployees} employees</p>
          <div className="mt-5 space-y-2 text-sm text-slate-300">
            {Object.entries(plan.features).filter(([, enabled]) => enabled === true).map(([feature]) => <p key={feature} className="flex items-center gap-2"><Check size={15} className="text-cyan-300" />{humanize(feature)}</p>)}
          </div>
          <button
            disabled={busy || isCurrent || isScheduled}
            className={`${buttonClass} mt-6 w-full`}
            onClick={() => isUpgrade ? createPayment.mutate(plan.code) : changePlan.mutate(plan.code)}
          >
            {isCurrent ? 'Current plan' : isScheduled ? 'Change scheduled' : isUpgrade ? 'Upgrade with Payme' : 'Schedule downgrade'}
          </button>
        </article>
      })}
    </div>

    <div className="mb-4 flex items-center gap-2"><CreditCard size={18} /><h2 className="text-lg font-semibold">Payment history</h2></div>
    <div className="overflow-hidden rounded-2xl border border-white/10">
      <table className="w-full text-left text-sm">
        <thead className="bg-white/5 text-xs uppercase tracking-wide text-slate-500"><tr><th className="px-4 py-3">Created</th><th className="px-4 py-3">Amount</th><th className="px-4 py-3">Status</th><th className="px-4 py-3">Provider</th><th className="px-4 py-3">Action</th></tr></thead>
        <tbody>
          {(payments.data ?? []).map((payment) => <tr key={payment.id} className="border-t border-white/5"><td className="px-4 py-3 text-slate-400">{formatDate(payment.createdAt)}</td><td className="px-4 py-3">{formatMoney(payment.amount)} {payment.currency}</td><td className="px-4 py-3">{payment.status}</td><td className="px-4 py-3">{payment.provider}</td><td className="px-4 py-3">{payment.checkoutUrl && payment.status === 'PENDING' ? <a className="inline-flex items-center gap-1 text-cyan-300 hover:text-cyan-200" href={payment.checkoutUrl}>Pay <ExternalLink size={14} /></a> : '—'}</td></tr>)}
          {(payments.data?.length ?? 0) === 0 && <tr><td colSpan={5} className="px-4 py-10 text-center text-slate-500">No payments yet</td></tr>}
        </tbody>
      </table>
    </div>
  </>
}

function formatMoney(value: number) {
  return new Intl.NumberFormat('ru-RU', { maximumFractionDigits: 0 }).format(value)
}

function formatDate(value: string) {
  return new Intl.DateTimeFormat('ru-RU', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value))
}

function humanize(value: string) {
  return value.replaceAll('_', ' ').replace(/\b\w/g, (letter) => letter.toUpperCase())
}
