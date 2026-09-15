import { Navigate, Route, Routes } from 'react-router-dom'
import { AuthProvider } from './auth/AuthProvider'
import { useAuth } from './auth/useAuth'
import { AppShell } from './components/AppShell'
import { LoginPage, RegisterPage } from './pages/AuthPages'
import { AppointmentsPage } from './pages/AppointmentsPage'
import { BillingPage } from './pages/BillingPage'
import { ClientsPage, EmployeesPage, ServicesPage } from './pages/DirectoryPages'
import { NotificationsPage } from './pages/NotificationsPage'
import { OverviewPage } from './pages/OverviewPage'

function ProtectedShell() {
  const { auth } = useAuth()
  return auth ? <AppShell /> : <Navigate to="/login" replace />
}

export default function App() {
  return <AuthProvider><Routes><Route path="/login" element={<LoginPage />} /><Route path="/register" element={<RegisterPage />} /><Route element={<ProtectedShell />}><Route index element={<OverviewPage />} /><Route path="clients" element={<ClientsPage />} /><Route path="employees" element={<EmployeesPage />} /><Route path="services" element={<ServicesPage />} /><Route path="appointments" element={<AppointmentsPage />} /><Route path="notifications" element={<NotificationsPage />} /><Route path="billing" element={<BillingPage />} /></Route><Route path="*" element={<Navigate to="/" replace />} /></Routes></AuthProvider>
}
