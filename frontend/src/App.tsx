import { Navigate, Route, Routes } from 'react-router-dom'
import { AuthProvider, useAuth } from './auth/AuthProvider'
import { AppShell } from './components/AppShell'
import { LoginPage, RegisterPage } from './pages/AuthPages'
import { AppointmentsPage } from './pages/AppointmentsPage'
import { ClientsPage, EmployeesPage, ServicesPage } from './pages/DirectoryPages'
import { OverviewPage } from './pages/OverviewPage'

function ProtectedShell() {
  const { auth } = useAuth()
  return auth ? <AppShell /> : <Navigate to="/login" replace />
}

export default function App() {
  return <AuthProvider><Routes><Route path="/login" element={<LoginPage />} /><Route path="/register" element={<RegisterPage />} /><Route element={<ProtectedShell />}><Route index element={<OverviewPage />} /><Route path="clients" element={<ClientsPage />} /><Route path="employees" element={<EmployeesPage />} /><Route path="services" element={<ServicesPage />} /><Route path="appointments" element={<AppointmentsPage />} /></Route><Route path="*" element={<Navigate to="/" replace />} /></Routes></AuthProvider>
}
