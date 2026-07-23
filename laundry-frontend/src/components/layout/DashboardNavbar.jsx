import { useState, useEffect, useCallback } from 'react'
import { Link, useNavigate, useLocation } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext'
import { ROUTES, ROLES } from '../../utils/constants'
import { Menu, LogOut, ChevronRight, Bell } from 'lucide-react'
import { getMyNotifications } from '../../api/notificationApi'

const BREADCRUMBS = {
  [ROUTES.DASHBOARD]: [{ label: 'Aperçu' }],
  [ROUTES.USERS]:     [{ label: 'Aperçu', path: ROUTES.DASHBOARD }, { label: 'Utilisateurs' }],
}

const DashboardNavbar = ({ onToggle }) => {
  const { user, logout, hasAnyRole } = useAuth()
  const navigate  = useNavigate()
  const location  = useLocation()

  const crumbs = BREADCRUMBS[location.pathname] ?? [{ label: 'Dashboard' }]
  const canSeeStaffNotifications = hasAnyRole([ROLES.ADMIN, ROLES.MANAGER])
  const canSeeCustomerNotifications = hasAnyRole([ROLES.CUSTOMER])
  const canSeeNotifications = canSeeStaffNotifications || canSeeCustomerNotifications

  const [unreadCount, setUnreadCount] = useState(0)

  const loadUnreadCount = useCallback(async () => {
    if (!canSeeNotifications) return
    try {
      const res = await getMyNotifications()
      const list = res.data?.data ?? []
      setUnreadCount(list.filter((n) => !n.isRead).length)
    } catch {
      // Silencieux : un badge de notifications non chargé n'est pas bloquant.
    }
  }, [canSeeNotifications])

  useEffect(() => { loadUnreadCount() }, [loadUnreadCount])

  // Ouvre la page notifications dans une VRAIE fenêtre de navigateur à part
  // (pas un simple onglet) : c'est ce petit format popup qui permet, côté
  // StaffNotificationsPage, au bouton retour de simplement refermer la
  // fenêtre pour revenir sur le dashboard, comme dans les applis modernes.
  const handleOpenNotifications = () => {
    // Espace client : navigation interne (React Router) vers la route
    // Notifications, toujours dans l'application — pas de nouvel onglet/fenêtre.
    if (canSeeCustomerNotifications) {
      navigate(ROUTES.MY_NOTIFICATIONS)
      return
    }

    const url = `${window.location.origin}${ROUTES.NOTIFICATIONS}`
    window.open(
      url,
      'staff-notifications',
      'width=420,height=680,left=200,top=80,resizable=yes,scrollbars=yes'
    )
  }

  const handleLogout = async () => {
    await logout()
    navigate(ROUTES.LOGIN)
  }

  const initials = [user?.firstName?.[0], user?.lastName?.[0]]
    .filter(Boolean).join('').toUpperCase() || '?'

  return (
    <header className="h-14 bg-white border-b border-slate-100 flex items-center justify-between px-4 lg:px-5 flex-shrink-0">

      {/* ── Gauche : toggle + breadcrumb ───────────────────────── */}
      <div className="flex items-center gap-2">
        <button
          onClick={onToggle}
          className="p-2 rounded-lg text-slate-400 hover:bg-slate-100 hover:text-slate-700 transition-colors"
          aria-label="Basculer la sidebar"
        >
          <Menu size={18} />
        </button>

        {/* Breadcrumb */}
        <nav className="flex items-center gap-1 text-sm">
          {crumbs.map((crumb, i) => (
            <span key={crumb.label} className="flex items-center gap-1">
              {i > 0 && <ChevronRight size={13} className="text-slate-300" />}
              {crumb.path ? (
                <Link
                  to={crumb.path}
                  className="text-slate-400 hover:text-slate-600 transition-colors font-medium"
                >
                  {crumb.label}
                </Link>
              ) : (
                <span className="text-slate-700 font-semibold">{crumb.label}</span>
              )}
            </span>
          ))}
        </nav>
      </div>

      {/* ── Droite : notifications + user info + logout ─────────── */}
      <div className="flex items-center gap-2">
        {canSeeNotifications && (
          <button
            onClick={handleOpenNotifications}
            aria-label="Ouvrir les notifications"
            title="Notifications"
            className="relative p-2 rounded-lg text-slate-400 hover:bg-slate-100 hover:text-slate-700 transition-colors"
          >
            <Bell size={18} />
            {unreadCount > 0 && (
              <span className="absolute top-1 right-1 min-w-[15px] h-[15px] px-[3px] rounded-full bg-red-500 text-white text-[9px] font-bold flex items-center justify-center leading-none">
                {unreadCount > 9 ? '9+' : unreadCount}
              </span>
            )}
          </button>
        )}

        <div className="hidden sm:flex items-center gap-2.5 pr-2 border-r border-slate-100">
          <div className="w-7 h-7 rounded-full bg-gradient-to-br from-blue-500 to-indigo-600 flex items-center justify-center text-white text-[10px] font-bold flex-shrink-0">
            {initials}
          </div>
          <div className="leading-tight">
            <p className="text-xs font-semibold text-slate-700 leading-tight">
              {user?.firstName} {user?.lastName}
            </p>
            <p className="text-[10px] text-slate-400 leading-tight">{user?.email}</p>
          </div>
        </div>

        <button
          onClick={handleLogout}
          className="flex items-center gap-1.5 px-2.5 py-1.5 text-xs text-slate-500 rounded-lg hover:bg-red-50 hover:text-red-600 transition-colors font-medium border border-transparent hover:border-red-100"
        >
          <LogOut size={13} />
          <span className="hidden sm:inline">Déconnexion</span>
        </button>
      </div>
    </header>
  )
}

export default DashboardNavbar
