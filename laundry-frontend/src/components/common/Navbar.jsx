import { useState } from 'react'
import { Link, useNavigate, useLocation } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext'
import { ROUTES } from '../../utils/constants'
import {
  Shield, LayoutDashboard, User, LogOut,
  LogIn, UserPlus, Menu, X
} from 'lucide-react'

const Navbar = () => {
  const { isAuthenticated, user, logout } = useAuth()
  const navigate  = useNavigate()
  const location  = useLocation()
  const [open, setOpen] = useState(false)

  const handleLogout = async () => {
    await logout()
    navigate(ROUTES.LOGIN)
    setOpen(false)
  }

  const isActive = (path) => location.pathname === path

  const linkClass = (path) =>
    `flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-sm font-medium transition-colors ${
      isActive(path)
        ? 'bg-blue-800 text-white'
        : 'text-blue-100 hover:bg-blue-800 hover:text-white'
    }`

  return (
    <nav className="bg-blue-700 shadow-md">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between h-16">

          {/* Logo */}
          <Link to={ROUTES.HOME} className="flex items-center gap-2 text-white font-bold text-lg hover:text-blue-200 transition-colors">
            <Shield size={20} />
            Vividela
          </Link>

          {/* Desktop */}
          <div className="hidden sm:flex items-center gap-3">
            {isAuthenticated ? (
              <>
                <Link to={ROUTES.DASHBOARD} className={linkClass(ROUTES.DASHBOARD)}>
                  <LayoutDashboard size={16} />
                  Tableau de bord
                </Link>
                <span className="flex items-center gap-1.5 text-blue-200 text-sm px-2">
                  <User size={15} />
                  {user?.firstName} {user?.lastName}
                </span>
                <button
                  onClick={handleLogout}
                  className="flex items-center gap-1.5 px-3 py-1.5 border border-blue-400 text-blue-100 rounded-lg text-sm font-medium hover:bg-blue-800 hover:border-blue-200 transition-colors"
                >
                  <LogOut size={15} />
                  Déconnexion
                </button>
              </>
            ) : (
              <>
                <Link to={ROUTES.LOGIN} className={linkClass(ROUTES.LOGIN)}>
                  <LogIn size={16} />
                  Connexion
                </Link>
                <Link
                  to={ROUTES.REGISTER}
                  className="flex items-center gap-1.5 px-3 py-1.5 bg-white text-blue-700 rounded-lg text-sm font-semibold hover:bg-blue-50 transition-colors"
                >
                  <UserPlus size={16} />
                  Inscription
                </Link>
              </>
            )}
          </div>

          {/* Hamburger mobile */}
          <button
            onClick={() => setOpen(v => !v)}
            className="sm:hidden p-2 text-blue-100 hover:text-white rounded-lg"
            aria-label="Menu"
          >
            {open ? <X size={22} /> : <Menu size={22} />}
          </button>
        </div>

        {/* Mobile menu */}
        {open && (
          <div className="sm:hidden border-t border-blue-600 py-3 space-y-1">
            {isAuthenticated ? (
              <>
                <Link
                  to={ROUTES.DASHBOARD}
                  onClick={() => setOpen(false)}
                  className="flex items-center gap-2 px-3 py-2 text-blue-100 hover:bg-blue-800 hover:text-white rounded-lg text-sm font-medium"
                >
                  <LayoutDashboard size={16} />
                  Tableau de bord
                </Link>
                <div className="px-3 py-2 text-blue-200 text-sm flex items-center gap-2">
                  <User size={15} />
                  {user?.firstName} {user?.lastName}
                </div>
                <button
                  onClick={handleLogout}
                  className="w-full flex items-center gap-2 px-3 py-2 text-blue-100 hover:bg-blue-800 hover:text-white rounded-lg text-sm font-medium"
                >
                  <LogOut size={16} />
                  Déconnexion
                </button>
              </>
            ) : (
              <>
                <Link
                  to={ROUTES.LOGIN}
                  onClick={() => setOpen(false)}
                  className="flex items-center gap-2 px-3 py-2 text-blue-100 hover:bg-blue-800 hover:text-white rounded-lg text-sm font-medium"
                >
                  <LogIn size={16} />
                  Connexion
                </Link>
                <Link
                  to={ROUTES.REGISTER}
                  onClick={() => setOpen(false)}
                  className="flex items-center gap-2 px-3 py-2 text-blue-100 hover:bg-blue-800 hover:text-white rounded-lg text-sm font-medium"
                >
                  <UserPlus size={16} />
                  Inscription
                </Link>
              </>
            )}
          </div>
        )}
      </div>
    </nav>
  )
}

export default Navbar
