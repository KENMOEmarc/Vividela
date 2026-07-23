import { useState, useEffect, useCallback, useMemo } from 'react'
import { useNavigate } from 'react-router-dom'
import { toast } from 'react-toastify'
import { ArrowLeft, Bell, BellRing, Inbox, CheckCheck, Hash, Calendar, Shirt } from 'lucide-react'
import { getMyNotifications, markNotificationAsRead } from '../api/notificationApi'

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
 * Page "Notifications" de l'espace ADMIN / MANAGER.
 *
 * Contrairement aux autres pages du dashboard, celle-ci est conçue pour être
 * ouverte dans une FENÊTRE À PART (voir le bouton cloche dans DashboardNavbar,
 * qui l'ouvre via window.open) plutôt que dans le layout principal avec
 * sidebar — d'où l'absence de Sidebar/DashboardNavbar ici et la présence
 * d'un bouton "retour" dédié qui referme cette fenêtre.
 */
const StaffNotificationsPage = () => {
  const navigate = useNavigate()
  const [notifications, setNotifications] = useState([])
  const [loading, setLoading] = useState(true)
  const [filter, setFilter] = useState('all')
  const [markingAll, setMarkingAll] = useState(false)

  const loadNotifications = useCallback(async () => {
    setLoading(true)
    try {
      const res = await getMyNotifications()
      setNotifications(res.data?.data ?? [])
    } catch {
      toast.error('Impossible de charger les notifications')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => { loadNotifications() }, [loadNotifications])

  const unreadCount = useMemo(() => notifications.filter((n) => !n.isRead).length, [notifications])

  const filteredNotifs = useMemo(() => {
    if (filter === 'unread') return notifications.filter((n) => !n.isRead)
    if (filter === 'read') return notifications.filter((n) => n.isRead)
    return notifications
  }, [notifications, filter])

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

  /**
   * "Revenir en arrière" : cette page est presque toujours ouverte comme une
   * fenêtre distincte (window.open depuis la navbar). Si c'est le cas
   * (window.opener défini, fenêtre ouverte par script), on la referme pour
   * revenir naturellement sur l'onglet d'origine. Sinon (page ouverte
   * directement via son URL, ou navigation interne), on retombe sur
   * l'historique classique du navigateur.
   */
  const handleBack = () => {
    if (window.opener && window.opener !== window) {
      window.close()
    } else {
      navigate(-1)
    }
  }

  return (
    <div className="min-h-screen bg-slate-50">
      <header className="sticky top-0 z-10 bg-white border-b border-slate-100 px-4 py-3 flex items-center gap-3">
        <button
          onClick={handleBack}
          aria-label="Revenir en arrière"
          className="p-2 rounded-lg text-slate-500 hover:bg-slate-100 hover:text-slate-800 transition-colors flex-shrink-0"
        >
          <ArrowLeft size={18} />
        </button>
        <div className="min-w-0">
          <h1 className="text-base font-bold text-slate-900 flex items-center gap-1.5 truncate">
            <Bell size={16} className="text-amber-600 flex-shrink-0" /> Notifications
          </h1>
          <p className="text-[11px] text-slate-400 truncate">
            {unreadCount > 0 ? `${unreadCount} non lue${unreadCount > 1 ? 's' : ''}` : 'Tout est à jour'}
          </p>
        </div>
        {unreadCount > 0 && (
          <button
            onClick={handleMarkAllRead}
            disabled={markingAll}
            className="ml-auto inline-flex items-center gap-1.5 text-[11px] font-semibold text-blue-600 hover:text-blue-700 bg-blue-50 hover:bg-blue-100 px-2.5 py-1.5 rounded-lg transition-colors disabled:opacity-50 flex-shrink-0"
          >
            <CheckCheck size={13} /> Tout marquer lu
          </button>
        )}
      </header>

      <div className="flex items-center gap-1.5 px-4 py-2.5 border-b border-slate-100 bg-white">
        {FILTERS.map((f) => (
          <button
            key={f.key}
            onClick={() => setFilter(f.key)}
            className={`px-3 py-1.5 rounded-lg text-[11px] font-semibold transition-colors ${
              filter === f.key ? 'bg-amber-100 text-amber-700' : 'text-slate-500 hover:bg-slate-100'
            }`}
          >
            {f.label}
          </button>
        ))}
      </div>

      {loading ? (
        <div className="p-4 space-y-2">
          {[...Array(5)].map((_, i) => (
            <div key={i} className="h-14 bg-white rounded-xl animate-pulse" />
          ))}
        </div>
      ) : filteredNotifs.length === 0 ? (
        <div className="py-16 text-center px-4">
          <Inbox size={30} className="text-slate-200 mx-auto mb-2" />
          <p className="text-sm text-slate-400">Aucune notification à afficher.</p>
        </div>
      ) : (
        <ul className="divide-y divide-slate-100 bg-white">
          {filteredNotifs.map((n) => {
            const Icon = NOTIF_ICON[n.notificationType] || Bell
            return (
              <li key={n.id}>
                <div
                  role="button"
                  tabIndex={0}
                  onClick={() => handleMarkRead(n)}
                  onKeyDown={(e) => { if (e.key === 'Enter' || e.key === ' ') handleMarkRead(n) }}
                  className={`w-full flex items-start gap-3 px-4 py-3.5 text-left cursor-pointer hover:bg-slate-50/80 transition-colors ${!n.isRead ? 'bg-blue-50/40' : ''}`}
                >
                  <span className={`w-8 h-8 rounded-lg flex items-center justify-center flex-shrink-0 mt-0.5 ${!n.isRead ? 'bg-blue-100 text-blue-600' : 'bg-slate-100 text-slate-400'}`}>
                    <Icon size={14} />
                  </span>
                  <div className="flex-1 min-w-0">
                    <p className={`text-[13px] leading-snug ${!n.isRead ? 'font-semibold text-slate-800' : 'text-slate-500'}`}>
                      {n.subject || n.message || 'Notification'}
                    </p>
                    {n.subject && n.message && (
                      <p className="text-[11px] text-slate-400 mt-0.5">{n.message}</p>
                    )}
                    <p className="text-[10px] text-slate-300 mt-1">{fullDate(n.sentAt)}</p>

                    {n.orderId && (
                      <div className="mt-2 flex flex-wrap items-center gap-x-2.5 gap-y-1 rounded-lg bg-slate-50 border border-slate-100 px-2.5 py-1.5">
                        <span className="inline-flex items-center gap-1 text-[10px] font-semibold text-slate-600">
                          <Hash size={10} className="text-slate-400" /> Commande #{n.orderId}
                        </span>
                        <span className="inline-flex items-center gap-1 text-[10px] text-slate-500">
                          <Calendar size={10} className="text-slate-400" /> {shortDate(n.orderDepositDate)}
                        </span>
                        <span className="inline-flex items-center gap-1 text-[10px] text-slate-500">
                          <Shirt size={10} className="text-slate-400" />
                          {n.orderArticleCount ?? 0} article{(n.orderArticleCount ?? 0) !== 1 ? 's' : ''}
                        </span>
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
  )
}

export default StaffNotificationsPage
