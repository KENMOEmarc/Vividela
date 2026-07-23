import { useState } from 'react'
import { Outlet } from 'react-router-dom'
import Sidebar from './Sidebar'
import DashboardNavbar from './DashboardNavbar'
import DashboardFooter from './DashboardFooter'

const DashboardLayout = () => {
  const [collapsed, setCollapsed]     = useState(false) // desktop: w-16 ↔ w-64
  const [mobileOpen, setMobileOpen]   = useState(false) // mobile: overlay visible

  const handleToggle = () => {
    if (window.innerWidth >= 1024) {
      setCollapsed(v => !v)
    } else {
      setMobileOpen(v => !v)
    }
  }

  return (
    <div className="flex h-screen bg-slate-50 overflow-hidden">

      <Sidebar
        collapsed={collapsed}
        mobileOpen={mobileOpen}
        onCollapse={() => setCollapsed(v => !v)}
        onMobileClose={() => setMobileOpen(false)}
      />

      {/* Backdrop mobile */}
      {mobileOpen && (
        <div
          className="fixed inset-0 bg-black/40 z-20 lg:hidden"
          onClick={() => setMobileOpen(false)}
        />
      )}

      {/* Zone droite : navbar + contenu + footer */}
      <div className="flex flex-col flex-1 min-w-0 overflow-hidden">
        <DashboardNavbar onToggle={handleToggle} />

        <main className="flex-1 overflow-y-auto p-4 sm:p-6">
          <Outlet />
        </main>

        <DashboardFooter />
      </div>
    </div>
  )
}

export default DashboardLayout
