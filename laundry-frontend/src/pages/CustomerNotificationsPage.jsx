import { useState, useEffect, useCallback, useMemo } from 'react'
import { Link } from 'react-router-dom'
import { toast } from 'react-toastify'
import { Bell, BellRing, Inbox, CheckCheck, Search, Hash, Calendar, Shirt, ArrowRight } from 'lucide-react'
import { getMyNotifications, markNotificationAsRead } from '../api/notificationApi'
import { ROUTES } from '../utils/constants'

const NOTIF_ICON = { EMAIL: Bell, SMS: Bell, PUSH: BellRing, IN_APP: BellRing, PHONE_CALL: Bell }

const FILTERS = [
  { key: 'all',    label: 'Toutes' },
  { key: 'unread', label: 'Non lues' },
  { key: 'read',   label: 'Lues' },
]

const fullDate = (iso) =>
  iso ? new Date(iso).toLocaleString('fr-FR', { dateStyle: 'long', timeStyle: 'short' }) : '—'

const shortDate = (isoDate) =>
  isoDate ? new Date(isoDate).toLocaleDateString('fr-FR', { day: '2-digit', month: 'short', year: 'numeric' }) : '—'

/**
 * Page "Notifications" de l'espace client : liste complète de toutes les
 * notifications reçues par le client connecté.
 */
const CustomerNotificationsPage = () => {
  const [notifications, setNotifications] = useState([])
  const [loading, setLoading] = useState(true)
  const [filter, setFilter] = useState('all')
  const [search, setSearch] = useState('')
  const [markingAll, setMarkingAll] = useState(false)

  const loadNotifications = useCallback(async () => {
    setLoading(true)
    try {
      const res = await getMyNotifications()
      setNotifications(res.data?.data ?? [])
    } catch {
      toast.error('Impossible de charger vos notifications')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => { loadNotifications() }, [loadNotifications])

  const unreadCount = useMemo(() => notifications.filter((n) => !n.isRead).length, [notifications])

  const filteredNotifs = useMemo(() => {
    let list = notifications
    if (filter === 'unread') list = list.filter((n) => !n.isRead)
    if (filter === 'read') list = list.filter((n) => n.isRead)
    if (search.trim()) {
      const q = search.trim().toLowerCase()
      list = list.filter((n) => [n.subject, n.message].filter(Boolean).some((f) => f.toLowerCase().includes(q)))
    }
    return list
  }, [notifications, filter, search])

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

  const handleMarkAllRead = async () => {
    const unread = notifications.filter((n) => !n.isRead)
    if (unread.length === 0) return
    setMarkingAll(true)
    try {
      await Promise.all(unread.map((n) => markNotificationAsRead(n.id)))
      setNotifications((prev) => prev.map((n) => ({ ...n, isRead: true })))
    } catch {
      toast.error('Impossible de marquer toutes les notifications comme lues')
    } finally {
      setMarkingAll(false)
    }
  }

  return (
    <div className="space-y-5">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-2xl font-bold text-slate-900 flex items-center gap-2">
            <Bell size={22} className="text-amber-600" /> Notifications
          </h1>
          <p className="text-sm text-slate-500 mt-1">
            {unreadCount > 0 ? `${unreadCount} notification${unreadCount > 1 ? 's' : ''} non lue${unreadCount > 1 ? 's' : ''}` : 'Tout est à jour'}
          </p>
        </div>
        <div className="flex items-center gap-2">
          <div className="relative">
            <Search size={14} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
            <input
              type="text"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              placeholder="Rechercher…"
              className="pl-8 pr-3 py-2 text-xs rounded-xl border border-slate-200 focus:outline-none focus:ring-2 focus:ring-amber-100 focus:border-amber-300 w-40 sm:w-56"
            />
          </div>
          {unreadCount > 0 && (
            <button
              onClick={handleMarkAllRead}
              disabled={markingAll}
              className="inline-flex items-center gap-1.5 text-[12px] font-semibold text-blue-600 hover:text-blue-700 bg-blue-50 hover:bg-blue-100 px-3 py-2 rounded-xl transition-colors disabled:opacity-50 flex-shrink-0"
            >
              <CheckCheck size={14} /> Tout marquer comme lu
            </button>
          )}
        </div>
      </div>

      <div className="bg-white rounded-2xl border border-slate-100 overflow-hidden shadow-sm">
        <div className="flex items-center gap-1.5 px-5 py-3 border-b border-slate-50">
          {FILTERS.map((f) => (
            <button
              key={f.key}
              onClick={() => setFilter(f.key)}
              className={`px-3 py-1.5 rounded-lg text-[11px] font-semibold transition-colors ${
                filter === f.key ? 'bg-amber-100 text-amber-700' : 'text-slate-500 hover:bg-slate-50'
              }`}
            >
              {f.label}
            </button>
          ))}
        </div>

        {loading ? (
          <div className="p-4 space-y-2">
            {[...Array(5)].map((_, i) => (
              <div key={i} className="h-14 bg-slate-50 rounded-xl animate-pulse" />
            ))}
          </div>
        ) : filteredNotifs.length === 0 ? (
          <div className="py-16 text-center px-4">
            <Inbox size={30} className="text-slate-200 mx-auto mb-2" />
            <p className="text-sm text-slate-400">Aucune notification à afficher.</p>
          </div>
        ) : (
          <ul className="divide-y divide-slate-50">
            {filteredNotifs.map((n) => {
              const Icon = NOTIF_ICON[n.notificationType] || Bell
              return (
                <li key={n.id}>
                  <div
                    role="button"
                    tabIndex={0}
                    onClick={() => handleMarkRead(n)}
                    onKeyDown={(e) => { if (e.key === 'Enter' || e.key === ' ') handleMarkRead(n) }}
                    className={`w-full flex items-start gap-3 px-5 py-4 text-left cursor-pointer hover:bg-slate-50/80 transition-colors ${!n.isRead ? 'bg-blue-50/40' : ''}`}
                  >
                    <span className={`w-9 h-9 rounded-lg flex items-center justify-center flex-shrink-0 mt-0.5 ${!n.isRead ? 'bg-blue-100 text-blue-600' : 'bg-slate-100 text-slate-400'}`}>
                      <Icon size={15} />
                    </span>
                    <div className="flex-1 min-w-0">
                      <p className={`text-sm leading-snug ${!n.isRead ? 'font-semibold text-slate-800' : 'text-slate-500'}`}>
                        {n.subject || n.message || 'Notification'}
                      </p>
                      {n.subject && n.message && (
                        <p className="text-xs text-slate-400 mt-0.5">{n.message}</p>
                      )}
                      <p className="text-[11px] text-slate-300 mt-1.5">{fullDate(n.sentAt)}</p>

                      {/* Détails de la commande liée */}
                      {n.orderId && (
                        <div className="mt-2.5 flex flex-wrap items-center gap-x-3 gap-y-1.5 rounded-lg bg-slate-50 border border-slate-100 px-3 py-2">
                          <span className="inline-flex items-center gap-1 text-[11px] font-semibold text-slate-600">
                            <Hash size={11} className="text-slate-400" /> Commande #{n.orderId}
                          </span>
                          <span className="inline-flex items-center gap-1 text-[11px] text-slate-500">
                            <Calendar size={11} className="text-slate-400" /> Déposée le {shortDate(n.orderDepositDate)}
                          </span>
                          <span className="inline-flex items-center gap-1 text-[11px] text-slate-500">
                            <Shirt size={11} className="text-slate-400" />
                            {n.orderArticleCount ?? 0} article{(n.orderArticleCount ?? 0) !== 1 ? 's' : ''}
                          </span>
                          <Link
                            to={`${ROUTES.MY_ORDERS}?orderId=${n.orderId}`}
                            onClick={(e) => e.stopPropagation()}
                            className="ml-auto inline-flex items-center gap-1 text-[11px] font-semibold text-amber-600 hover:text-amber-700"
                          >
                            Voir la commande <ArrowRight size={11} />
                          </Link>
                        </div>
                      )}
                    </div>
                    {!n.isRead && <span className="w-2 h-2 rounded-full bg-blue-500 flex-shrink-0 mt-1.5" />}
                  </div>
                </li>
              )
            })}
          </ul>
        )}
      </div>
    </div>
  )
}

export default CustomerNotificationsPage
