import { useState, useEffect, useCallback, useMemo } from 'react'
import { Link } from 'react-router-dom'
import { toast } from 'react-toastify'
import { useAuth } from '../context/AuthContext'
import { getCustomerOrders, getCustomerStats } from '../api/customerApi'
import { getMyNotifications, markNotificationAsRead } from '../api/notificationApi'
import CustomerOrderCardsGrid from '../components/orders/CustomerOrderCardsGrid'
import { ROUTES } from '../utils/constants'
import {
  ShoppingCart, Coins, TrendingUp, Package, Bell, BellRing,
  ArrowRight, CheckCheck, CalendarDays, ShieldCheck, Inbox,
} from 'lucide-react'

const RECENT_ORDERS_LIMIT = 6
const RECENT_NOTIFICATIONS_LIMIT = 20

const NOTIF_ICON = { EMAIL: Bell, SMS: Bell, PUSH: BellRing, IN_APP: BellRing, PHONE_CALL: Bell }

const formatCurrency = (amount) =>
  new Intl.NumberFormat('fr-FR', { style: 'currency', currency: 'XAF', maximumFractionDigits: 0 }).format(amount ?? 0)

const timeLabel = (iso) => {
  if (!iso) return '—'
  const diff = Date.now() - new Date(iso)
  if (diff < 3_600_000)  return "À l'instant"
  if (diff < 86_400_000) return `Il y a ${Math.max(1, Math.round(diff / 3_600_000))} h`
  if (diff < 172_800_000) return 'Hier'
  return new Date(iso).toLocaleDateString('fr-FR', { day: '2-digit', month: 'short' })
}

const CustomerHomeDashboard = () => {
  const { user } = useAuth()

  const [orders, setOrders]   = useState([])
  const [stats, setStats]     = useState(null)
  const [loading, setLoading] = useState(true)

  const [notifications, setNotifications] = useState([])
  const [notifLoading, setNotifLoading]   = useState(true)

  const loadDashboard = useCallback(async () => {
    if (!user?.id) return
    setLoading(true)
    try {
      const [ordersRes, statsRes] = await Promise.all([
        getCustomerOrders(user.id),
        getCustomerStats(user.id),
      ])
      const list = ordersRes.data?.data ?? []
      list.sort((a, b) => new Date(b.orderDate ?? b.createdAt) - new Date(a.orderDate ?? a.createdAt))
      setOrders(list)
      setStats(statsRes.data?.data ?? {})
    } catch {
      toast.error('Impossible de charger vos données')
    } finally {
      setLoading(false)
    }
  }, [user?.id])

  const loadNotifications = useCallback(async () => {
    setNotifLoading(true)
    try {
      const res = await getMyNotifications()
      setNotifications(res.data?.data ?? [])
    } catch {
      /* silencieux : les notifications ne sont pas critiques à l'affichage */
    } finally {
      setNotifLoading(false)
    }
  }, [])

  useEffect(() => { loadDashboard() }, [loadDashboard])
  useEffect(() => { loadNotifications() }, [loadNotifications])

  const unreadCount = useMemo(() => notifications.filter((n) => !n.isRead).length, [notifications])
  const recentNotifs = notifications.slice(0, RECENT_NOTIFICATIONS_LIMIT)
  const recentOrders = orders.slice(0, RECENT_ORDERS_LIMIT)

  const handleMarkRead = async (notif) => {
    if (notif.isRead) return
    setNotifications((prev) => prev.map((n) => (n.id === notif.id ? { ...n, isRead: true } : n)))
    try {
      await markNotificationAsRead(notif.id)
    } catch {
      toast.error('Impossible de marquer la notification comme lue')
      setNotifications((prev) => prev.map((n) => (n.id === notif.id ? { ...n, isRead: false } : n)))
    }
  }

  const initials = (user?.firstName?.[0] ?? '?').toUpperCase()

  return (
    <div className="space-y-5">

      {/* ── Bannière bienvenue ─────────────────────────────────── */}
      <div className="relative overflow-hidden rounded-2xl bg-gradient-to-br from-blue-600 via-blue-700 to-indigo-800 p-6 text-white shadow-xl shadow-blue-900/20">
        <div
          className="absolute inset-0 opacity-[0.07] pointer-events-none"
          style={{ backgroundImage: 'radial-gradient(circle, white 1.5px, transparent 1.5px)', backgroundSize: '22px 22px' }}
        />
        <div className="absolute -right-12 -top-12 w-56 h-56 rounded-full bg-indigo-400/25 blur-3xl pointer-events-none" />
        <div className="relative flex items-start justify-between gap-4 flex-wrap">
          <div className="flex items-center gap-4">
            <div className="w-14 h-14 rounded-2xl bg-white/15 backdrop-blur-sm flex items-center justify-center text-white font-bold text-2xl flex-shrink-0 ring-2 ring-white/20 shadow-xl shadow-black/20">
              {initials}
            </div>
            <div>
              <p className="text-blue-200/80 text-[10px] font-semibold tracking-[0.15em] uppercase mb-0.5">
                Espace client
              </p>
              <h1 className="text-[22px] font-bold tracking-tight leading-tight">
                Bonjour, {user?.firstName} !
              </h1>
              <div className="flex flex-wrap items-center gap-3 mt-1.5">
                <span className="inline-flex items-center gap-1 text-[10px] font-semibold bg-white/15 text-white/90 px-2 py-0.5 rounded-md">
                  <ShieldCheck size={9} /> Client
                </span>
                <span className="inline-flex items-center gap-1 text-[10px] text-blue-200/50">
                  <CalendarDays size={9} />
                  {new Date().toLocaleDateString('fr-FR', { weekday: 'long', day: 'numeric', month: 'long' })}
                </span>
              </div>
            </div>
          </div>
          <div className="text-right hidden sm:block flex-shrink-0">
            <p className="text-white/70 text-xs truncate max-w-[200px]">{user?.email}</p>
          </div>
        </div>
      </div>

      {/* ── Statistiques ───────────────────────────────────────── */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <StatCard icon={ShoppingCart} label="Commandes passées" value={stats?.totalOrders ?? 0} loading={loading} accent="cyan" />
        <StatCard icon={Coins} label="Montant total dépensé" value={formatCurrency(stats?.totalSpent)} loading={loading} accent="emerald" />
        <StatCard icon={TrendingUp} label="Ticket moyen" value={formatCurrency((stats?.totalSpent ?? 0) / (stats?.totalOrders || 1))} loading={loading} accent="purple" />
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-5">

        {/* ── Colonne principale : commandes (toujours en cartes) ── */}
        <div className="lg:col-span-2 space-y-5">
          <div className="bg-white rounded-2xl border border-slate-100 overflow-hidden shadow-sm">
            <div className="flex items-center justify-between px-5 py-3.5 border-b border-slate-50">
              <div className="flex items-center gap-2">
                <span className="w-7 h-7 rounded-lg bg-blue-100 text-blue-600 flex items-center justify-center flex-shrink-0">
                  <Package size={14} />
                </span>
                <div>
                  <h2 className="text-sm font-bold text-slate-800">Mes commandes</h2>
                  <p className="text-[11px] text-slate-400">Articles et documents par commande</p>
                </div>
              </div>
              <Link
                to={ROUTES.MY_ORDERS}
                className="flex items-center gap-1 text-[11px] font-semibold text-blue-600 hover:text-blue-700 bg-blue-50 hover:bg-blue-100 px-2.5 py-1.5 rounded-lg transition-colors flex-shrink-0"
              >
                Voir tout <ArrowRight size={11} />
              </Link>
            </div>

            <div className="p-4">
              <CustomerOrderCardsGrid orders={recentOrders} loading={loading} />
            </div>
          </div>
        </div>

        {/* ── Colonne latérale : notifications ────────────────────── */}
        <div className="space-y-4">
          <div className="bg-white rounded-2xl border border-slate-100 overflow-hidden shadow-sm">
            <div className="flex items-center justify-between px-5 py-3.5 border-b border-slate-50">
              <div className="flex items-center gap-2">
                <span className="relative w-7 h-7 rounded-lg bg-amber-100 text-amber-600 flex items-center justify-center flex-shrink-0">
                  <Bell size={14} />
                  {unreadCount > 0 && (
                    <span className="absolute -top-1 -right-1 w-4 h-4 rounded-full bg-red-500 text-white text-[9px] font-bold flex items-center justify-center ring-2 ring-white">
                      {unreadCount > 9 ? '9+' : unreadCount}
                    </span>
                  )}
                </span>
                <div>
                  <h3 className="text-sm font-bold text-slate-800">Notifications</h3>
                  <p className="text-[11px] text-slate-400">{unreadCount > 0 ? `${unreadCount} non lue${unreadCount > 1 ? 's' : ''}` : 'Tout est à jour'}</p>
                </div>
              </div>
            </div>

            {notifLoading ? (
              <div className="p-3 space-y-2">
                {[...Array(3)].map((_, i) => (
                  <div key={i} className="h-12 bg-slate-50 rounded-xl animate-pulse" />
                ))}
              </div>
            ) : notifications.length === 0 ? (
              <div className="py-10 text-center px-4">
                <Inbox size={26} className="text-slate-200 mx-auto mb-2" />
                <p className="text-xs text-slate-400">Aucune notification pour le moment.</p>
              </div>
            ) : (
              <>
                <ul className="divide-y divide-slate-50 max-h-[420px] overflow-y-auto">
                  {recentNotifs.map((n) => {
                    const Icon = NOTIF_ICON[n.notificationType] || Bell
                    return (
                      <li key={n.id}>
                        <button
                          onClick={() => handleMarkRead(n)}
                          className={`w-full flex items-start gap-2.5 px-4 py-3 text-left hover:bg-slate-50/80 transition-colors ${!n.isRead ? 'bg-blue-50/40' : ''}`}
                        >
                          <span className={`w-7 h-7 rounded-lg flex items-center justify-center flex-shrink-0 mt-0.5 ${!n.isRead ? 'bg-blue-100 text-blue-600' : 'bg-slate-100 text-slate-400'}`}>
                            <Icon size={13} />
                          </span>
                          <div className="flex-1 min-w-0">
                            <p className={`text-xs leading-snug ${!n.isRead ? 'font-semibold text-slate-800' : 'text-slate-500'}`}>
                              {n.subject || n.message || 'Notification'}
                            </p>
                            {n.subject && n.message && (
                              <p className="text-[11px] text-slate-400 truncate mt-0.5">{n.message}</p>
                            )}
                            <p className="text-[10px] text-slate-300 mt-1">{timeLabel(n.sentAt)}</p>
                          </div>
                          {!n.isRead && <span className="w-1.5 h-1.5 rounded-full bg-blue-500 flex-shrink-0 mt-1.5" />}
                        </button>
                      </li>
                    )
                  })}
                </ul>
                <Link
                  to={ROUTES.MY_NOTIFICATIONS}
                  className="w-full flex items-center justify-center gap-1 text-[11px] font-semibold text-blue-600 hover:text-blue-700 py-2.5 border-t border-slate-50 hover:bg-slate-50 transition-colors"
                >
                  Voir toutes les notifications ({notifications.length}) <ArrowRight size={11} />
                </Link>
              </>
            )}
          </div>

          {/* Assistance / tickets support */}
          <div className="bg-gradient-to-br from-slate-50 to-blue-50/50 rounded-2xl border border-slate-100 p-5 shadow-sm">
            <div className="flex items-center gap-2 mb-2">
              <CheckCheck size={14} className="text-blue-500" />
              <p className="text-xs font-bold text-slate-700">Besoin d'aide ?</p>
            </div>
            <p className="text-[11px] text-slate-500 leading-relaxed">
              Le ticket de dépôt et le reçu de chaque commande sont disponibles dès leur génération en boutique,
              directement depuis vos cartes de commande.
            </p>
          </div>
        </div>
      </div>
    </div>
  )
}

const ACCENT = {
  cyan:    { bg: 'bg-cyan-100', text: 'text-cyan-600' },
  emerald: { bg: 'bg-emerald-100', text: 'text-emerald-600' },
  purple:  { bg: 'bg-purple-100', text: 'text-purple-600' },
}

const StatCard = ({ icon: Icon, label, value, loading, accent }) => {
  const a = ACCENT[accent]
  return (
    <div className="bg-white rounded-2xl border border-slate-100 p-5 shadow-sm hover:shadow-md transition-shadow">
      <div className="flex items-center justify-between">
        <div>
          <p className="text-xs font-semibold text-slate-600 uppercase tracking-wide">{label}</p>
          {loading ? (
            <div className="h-7 w-20 bg-slate-100 rounded-lg animate-pulse mt-2" />
          ) : (
            <p className="text-2xl font-bold text-slate-900 mt-2">{value}</p>
          )}
        </div>
        <div className={`w-12 h-12 rounded-xl ${a.bg} ${a.text} flex items-center justify-center flex-shrink-0`}>
          <Icon size={24} />
        </div>
      </div>
    </div>
  )
}

export default CustomerHomeDashboard
