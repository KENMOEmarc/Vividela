import { Link, useLocation, useNavigate } from 'react-router-dom'
import { Shirt, ChevronLeft, ChevronRight, LogOut } from 'lucide-react'
import { ROUTES, ROLE_LABELS } from '../../utils/constants'
import { useAuth } from '../../context/AuthContext'
import { NAV_SECTIONS, ROLE_BADGE } from './SidebarData'

const Sidebar = ({ collapsed, mobileOpen, onCollapse, onMobileClose }) => {
  const location = useLocation()
  const navigate  = useNavigate()
  const { user, logout } = useAuth()

  const isActive = (path, exact) => {
    if (!path) return false
    return exact
      ? location.pathname === path
      : location.pathname.startsWith(path)
  }

  const initials = [user?.firstName?.[0], user?.lastName?.[0]]
    .filter(Boolean).join('').toUpperCase() || '?'

  const handleLogout = async () => {
    onMobileClose?.()
    await logout()
    navigate(ROUTES.LOGIN)
  }

  const inner = (
    <div className="flex flex-col h-full bg-slate-950 overflow-hidden select-none">

       {/* ── Logo ───────────────────────────────────────────────── */}
       <div className={`flex items-center h-16 px-4 border-b border-white/5 flex-shrink-0 ${collapsed ? 'justify-center' : 'gap-3'}`}>
         <div className="w-8 h-8 rounded-xl bg-gradient-to-br from-blue-500 to-indigo-600 flex items-center justify-center flex-shrink-0 shadow-lg shadow-blue-500/30">
           <Shirt size={15} className="text-white" />
         </div>
        {!collapsed && (
          <div className="overflow-hidden">
            <p className="font-bold text-white text-sm leading-tight">Vividela</p>
            <p className="text-[10px] text-slate-500 leading-tight">Administration</p>
          </div>
        )}
      </div>

      {/* ── Navigation ─────────────────────────────────────────── */}
      <nav className="flex-1 px-2.5 py-4 overflow-y-auto overflow-x-hidden space-y-4">
        {NAV_SECTIONS.map((section) => (
          <div key={section.label}>
            {!collapsed && (
              <p className="text-[9px] font-bold uppercase tracking-[0.12em] text-slate-600 px-3 mb-1.5">
                {section.label}
              </p>
            )}
            <div className="space-y-0.5">
              {section.items.filter(item => !item.roles || item.roles.includes(user?.role)).map(({ icon: Icon, label, path, exact, disabled }) => {
                const active = !disabled && isActive(path, exact)

                if (disabled) {
                  return (
                    <div
                      key={label}
                      title={collapsed ? label : undefined}
                      className={`flex items-center gap-3 px-3 py-2.5 rounded-lg text-xs text-slate-700 cursor-not-allowed ${collapsed ? 'justify-center' : ''}`}
                    >
                      <Icon size={16} className="flex-shrink-0" />
                      {!collapsed && <span className="truncate">{label}</span>}
                    </div>
                  )
                }

                return (
                  <Link
                    key={label}
                    to={path}
                    onClick={onMobileClose}
                    title={collapsed ? label : undefined}
                    className={[
                      'group relative flex items-center gap-3 px-3 py-2.5 rounded-xl text-sm font-medium transition-all duration-150',
                      active
                        ? 'bg-blue-600 text-white shadow-lg shadow-blue-600/25'
                        : 'text-slate-400 hover:bg-white/5 hover:text-slate-200',
                      collapsed ? 'justify-center' : '',
                    ].join(' ')}
                  >
                    <Icon
                      size={16}
                      className={`flex-shrink-0 transition-colors ${active ? 'text-white' : 'text-slate-500 group-hover:text-slate-300'}`}
                    />
                    {!collapsed && (
                      <>
                        <span className="truncate">{label}</span>
                        {active && (
                          <span className="ml-auto w-1.5 h-1.5 rounded-full bg-white/60 flex-shrink-0" />
                        )}
                      </>
                    )}
                  </Link>
                )
              })}
            </div>
          </div>
        ))}
      </nav>

      {/* ── Profil utilisateur ─────────────────────────────────── */}
      <div className="border-t border-white/5 p-2.5 flex-shrink-0 space-y-0.5">
        {!collapsed ? (
          <div className="group flex items-center gap-3 px-3 py-2.5 rounded-xl hover:bg-white/5 transition-colors cursor-default">
            <div className="w-8 h-8 rounded-full bg-gradient-to-br from-blue-500 to-indigo-600 flex items-center justify-center text-white text-xs font-bold flex-shrink-0 ring-2 ring-white/10">
              {initials}
            </div>
            <div className="flex-1 min-w-0">
              <div className="flex items-center gap-1.5 min-w-0">
                <p className="text-xs font-semibold text-slate-200 truncate leading-tight">
                  {user?.firstName} {user?.lastName}
                </p>
                {user?.role && (
                  <span className={`flex-shrink-0 text-[9px] font-bold px-1.5 py-0.5 rounded-md ${ROLE_BADGE[user.role] ?? 'bg-slate-500/20 text-slate-400'}`}>
                    {ROLE_LABELS[user.role] ?? user.role}
                  </span>
                )}
              </div>
              <p className="text-[10px] text-slate-500 truncate leading-tight">{user?.email}</p>
            </div>
            <button
              onClick={handleLogout}
              title="Déconnexion"
              className="p-1.5 text-slate-600 hover:text-red-400 rounded-lg hover:bg-red-400/10 transition-all opacity-0 group-hover:opacity-100"
            >
              <LogOut size={13} />
            </button>
          </div>
        ) : (
          <div
            className="flex justify-center py-2 cursor-default"
            title={`${user?.firstName} ${user?.lastName}`}
          >
            <div className="w-8 h-8 rounded-full bg-gradient-to-br from-blue-500 to-indigo-600 flex items-center justify-center text-white text-xs font-bold ring-2 ring-white/10">
              {initials}
            </div>
          </div>
        )}

        {/* Bouton réduire */}
        <button
          onClick={onCollapse}
          className={`w-full flex items-center gap-2 px-3 py-2 rounded-xl text-slate-600 hover:bg-white/5 hover:text-slate-400 transition-colors text-xs ${collapsed ? 'justify-center' : ''}`}
        >
          {collapsed
            ? <ChevronRight size={14} />
            : <><ChevronLeft size={14} /><span>Réduire la barre</span></>
          }
        </button>
      </div>
    </div>
  )

  return (
    <>
      {/* Desktop — push layout */}
      <aside className={`hidden lg:block flex-shrink-0 transition-all duration-300 ease-in-out ${collapsed ? 'w-[68px]' : 'w-60'}`}>
        {inner}
      </aside>

      {/* Mobile — overlay */}
      <aside className={`lg:hidden fixed inset-y-0 left-0 z-30 w-60 transform transition-transform duration-300 ease-in-out ${mobileOpen ? 'translate-x-0' : '-translate-x-full'}`}>
        {inner}
      </aside>
    </>
  )
}

export default Sidebar
