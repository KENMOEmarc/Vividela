import { Outlet } from 'react-router-dom'
import Navbar from '../common/Navbar'

const MainLayout = () => {
  return (
    <div className="flex flex-col min-h-screen">
      <Navbar />

      <main className="flex-1 bg-slate-50">
        <Outlet />
      </main>

      <footer className="bg-slate-900 text-slate-400 py-4 text-center text-sm">
        <div className="max-w-7xl mx-auto px-4">
          © {new Date().getFullYear()} Vividela
        </div>
      </footer>
    </div>
  )
}

export default MainLayout
