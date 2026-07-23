import { useState, useEffect, useCallback, useMemo } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { getAllUsers } from '../api/userApi'
import { getAllOrders } from '../api/orderApi'
import { getLowStock } from '../api/stockApi'
import {
  Users, UserCheck, UserX, ArrowRight, CalendarDays,
  Zap, UserPlus, RefreshCw, TrendingUp, ShieldCheck,
  Truck, AlertTriangle, Clock, PackageCheck, PackageX, Boxes,
} from 'lucide-react'
import { ROUTES, ROLES, ROLE_LABELS } from '../utils/constants'
import { ORDER_STATUS_BADGE, ORDER_STATUS_LABELS } from '../types/OrdersType'
import { MEASUREMENT_UNIT_LABELS_BACKEND } from '../types/stockTypes'

const ROLE_PILL = {
  [ROLES.ADMIN]:    'bg-white/15 text-white/90',
  [ROLES.MANAGER]:  'bg-white/15 text-white/90',
  [ROLES.EMPLOYEE]: 'bg-white/15 text-white/90',
  [ROLES.CUSTOMER]: 'bg-white/15 text-white/90',
}

const AVATAR_PALETTE = [
  'from-blue-400 to-blue-600',
  'from-violet-400 to-violet-600',
  'from-emerald-400 to-emerald-600',
  'from-orange-400 to-orange-600',
  'from-rose-400 to-rose-600',
  'from-cyan-400 to-cyan-600',
  'from-amber-400 to-amber-600',
  'from-indigo-400 to-indigo-600',
]
const avatarGradient = (name = '') =>
  AVATAR_PALETTE[(name?.charCodeAt(0) ?? 0) % AVATAR_PALETTE.length]

// ─── Livraisons proches ────────────────────────────────────────────────────
// Une commande est signalée si sa livraison est prévue dans le délai choisi
// (ou déjà dépassée) et qu'elle n'est pas encore livrée / annulée.
const DEFAULT_DUE_THRESHOLD_DAYS = 3
const DUE_FILTER_OPTIONS = [
  { value: 1,  label: "Aujourd'hui/Demain" },
  { value: 3,  label: '3 jours' },
  { value: 7,  label: '7 jours' },
  { value: 14, label: '14 jours' },
]
const ACTIVE_ORDER_STATUSES = new Set(['PENDING', 'RECEIVED', 'IN_PROGRESS', 'READY'])

/** Nombre de jours (entier, peut être négatif si en retard) entre aujourd'hui et la date donnée. */
const daysUntil = (dateStr) => {
  if (!dateStr) return null
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  const target = new Date(dateStr)
  target.setHours(0, 0, 0, 0)
  return Math.round((target - today) / 86_400_000)
}

const dueLabel = (daysLeft) => {
  if (daysLeft < 0)  return `En retard de ${Math.abs(daysLeft)} j`
  if (daysLeft === 0) return "Aujourd'hui"
  if (daysLeft === 1) return 'Demain'
  return `Dans ${daysLeft} j`
}

const dueBadgeClass = (daysLeft) => {
  if (daysLeft < 0)  return 'bg-red-50 text-red-600 ring-red-100'
  if (daysLeft === 0) return 'bg-orange-50 text-orange-600 ring-orange-100'
  if (daysLeft === 1) return 'bg-amber-50 text-amber-600 ring-amber-100'
  return 'bg-yellow-50 text-yellow-600 ring-yellow-100'
}

const AdminDashboard = () => {
  const { user, isAdmin, isManager, isEmployee } = useAuth()
  const canManageUsers = isAdmin() || isManager() || isEmployee()
  const [users,       setUsers]       = useState([])
  const [orders,      setOrders]      = useState([])
  const [lowStock,    setLowStock]    = useState([])
  const [loading,     setLoading]     = useState(true)
  const [ordersLoading, setOrdersLoading] = useState(true)
  const [stockLoading,  setStockLoading]  = useState(true)
  const [lastRefresh, setLastRefresh] = useState(new Date())
  const [dueFilterDays, setDueFilterDays] = useState(DEFAULT_DUE_THRESHOLD_DAYS)

  const loadUsers = useCallback(async () => {
    setLoading(true)
    try {
      const res = await getAllUsers()
      setUsers(res.data?.data ?? [])
      setLastRefresh(new Date())
    } catch { /* silent */ }
    finally { setLoading(false) }
  }, [])

  const loadOrders = useCallback(async () => {
    if (!canManageUsers) { setOrdersLoading(false); return }
    setOrdersLoading(true)
    try {
      const res = await getAllOrders()
      setOrders(res.data?.data ?? [])
    } catch { /* silent */ }
    finally { setOrdersLoading(false) }
  }, [canManageUsers])

  const loadLowStock = useCallback(async () => {
    if (!canManageUsers) { setStockLoading(false); return }
    setStockLoading(true)
    try {
      const res = await getLowStock()
      setLowStock(res.data?.data ?? [])
    } catch { /* silent */ }
    finally { setStockLoading(false) }
  }, [canManageUsers])

  useEffect(() => { loadUsers() }, [loadUsers])
  useEffect(() => { loadOrders() }, [loadOrders])
  useEffect(() => { loadLowStock() }, [loadLowStock])

  const handleRefresh = () => { loadUsers(); loadOrders(); loadLowStock() }

  // Commandes actives dont la livraison prévue est proche (ou dépassée),
  // selon le délai choisi via le filtre (dueFilterDays).
  const upcomingDeliveries = useMemo(() => {
    return orders
      .filter((o) => o.expectedDeliveryDate && ACTIVE_ORDER_STATUSES.has(o.status))
      .map((o) => ({ ...o, daysLeft: daysUntil(o.expectedDeliveryDate) }))
      .filter((o) => o.daysLeft !== null && o.daysLeft <= dueFilterDays)
      .sort((a, b) => a.daysLeft - b.daysLeft)
  }, [orders, dueFilterDays])

  const overdueCount = upcomingDeliveries.filter((o) => o.daysLeft < 0).length
  const todayCount    = upcomingDeliveries.filter((o) => o.daysLeft === 0).length

  // Produits dont le stock est en alerte (au ou sous le seuil défini).
  const outOfStockCount = lowStock.filter((s) => Number(s.currentQuantity) <= 0).length

  const totalUsers    = users.length
  const activeUsers   = users.filter(u => u.enabled).length
  const inactiveUsers = totalUsers - activeUsers
  const activeRate    = totalUsers > 0 ? Math.round((activeUsers / totalUsers) * 100) : 0

  const recentUsers = [...users]
    .sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))
    .slice(0, 6)

  const initials  = (u) => [u.firstName?.[0], u.lastName?.[0]].filter(Boolean).join('').toUpperCase() || '?'
  const timeLabel = (iso) => {
    if (!iso) return '—'
    const diff = Date.now() - new Date(iso)
    if (diff < 86_400_000)   return "Aujourd'hui"
    if (diff < 172_800_000)  return 'Hier'
    return new Date(iso).toLocaleDateString('fr-FR', { day: '2-digit', month: 'short' })
  }

  return (
    <div className="space-y-5">

      {/* ── Bannière bienvenue ──────────────────────────────── */}
      <div className="relative overflow-hidden rounded-2xl bg-gradient-to-br from-blue-600 via-blue-700 to-indigo-800 p-6 text-white shadow-xl shadow-blue-900/20">
        {/* Dot pattern */}
        <div
          className="absolute inset-0 opacity-[0.07] pointer-events-none"
          style={{ backgroundImage: 'radial-gradient(circle, white 1.5px, transparent 1.5px)', backgroundSize: '22px 22px' }}
        />
        {/* Blobs décoratifs */}
        <div className="absolute -right-12 -top-12 w-56 h-56 rounded-full bg-indigo-400/25 blur-3xl pointer-events-none" />
        <div className="absolute right-20 -bottom-8 w-32 h-32 rounded-full bg-blue-300/20 blur-2xl pointer-events-none" />

        <div className="relative flex items-start justify-between gap-4 flex-wrap">
          <div className="flex items-center gap-4">
            <div className="w-14 h-14 rounded-2xl bg-white/15 backdrop-blur-sm flex items-center justify-center text-white font-bold text-2xl flex-shrink-0 ring-2 ring-white/20 shadow-xl shadow-black/20">
              {(user?.firstName?.[0] ?? '?').toUpperCase()}
            </div>
            <div>
              <p className="text-blue-200/80 text-[10px] font-semibold tracking-[0.15em] uppercase mb-0.5">
                Tableau de bord
              </p>
              <h1 className="text-[22px] font-bold tracking-tight leading-tight">
                Bonjour, {user?.firstName} !
              </h1>
              <div className="flex flex-wrap items-center gap-3 mt-1.5">
                {user?.role && (
                  <span className="inline-flex items-center gap-1 text-[10px] font-semibold bg-white/15 text-white/90 px-2 py-0.5 rounded-md">
                    <ShieldCheck size={9} />
                    {ROLE_LABELS[user.role] ?? user.role}
                  </span>
                )}
                <span className="inline-flex items-center gap-1 text-[10px] text-blue-200/70">
                  <Zap size={9} className="text-yellow-300" />
                  Session active
                </span>
                <span className="inline-flex items-center gap-1 text-[10px] text-blue-200/50">
                  <CalendarDays size={9} />
                  {new Date().toLocaleDateString('fr-FR', { weekday: 'long', day: 'numeric', month: 'long' })}
                </span>
              </div>
            </div>
          </div>

          <div className="text-right hidden sm:block flex-shrink-0">
            <p className="text-white/70 text-xs truncate max-w-[180px]">{user?.email}</p>
            <p className="text-blue-300/40 text-[10px] mt-1">
              Mis à jour à {lastRefresh.toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' })}
            </p>
          </div>
        </div>
      </div>

      {/* ── Alerte livraisons proches ─────────────────────────── */}
      {canManageUsers && !ordersLoading && upcomingDeliveries.length > 0 && (
        <Link
          to={ROUTES.ORDERS}
          className={`flex items-center gap-3 rounded-2xl border p-4 shadow-sm transition-colors ${
            overdueCount > 0
              ? 'bg-red-50 border-red-100 hover:bg-red-100/70'
              : 'bg-amber-50 border-amber-100 hover:bg-amber-100/70'
          }`}
        >
          <div className={`w-10 h-10 rounded-xl flex items-center justify-center flex-shrink-0 text-white shadow-md ${
            overdueCount > 0 ? 'bg-red-500 shadow-red-200' : 'bg-amber-500 shadow-amber-200'
          }`}>
            <AlertTriangle size={18} />
          </div>
          <div className="flex-1 min-w-0">
            <p className={`text-sm font-bold ${overdueCount > 0 ? 'text-red-700' : 'text-amber-700'}`}>
              {upcomingDeliveries.length} commande{upcomingDeliveries.length > 1 ? 's' : ''} à livrer bientôt
            </p>
            <p className={`text-[11px] ${overdueCount > 0 ? 'text-red-600/70' : 'text-amber-600/70'}`}>
              {overdueCount > 0 && <>{overdueCount} en retard{todayCount > 0 ? ' · ' : ''}</>}
              {todayCount > 0 && <>{todayCount} à livrer aujourd'hui</>}
              {overdueCount === 0 && todayCount === 0 && `Échéance dans les ${dueFilterDays} prochains jours`}
            </p>
          </div>
          <ArrowRight size={16} className={overdueCount > 0 ? 'text-red-400' : 'text-amber-400'} />
        </Link>
      )}

      {/* ── Alerte stock bas ──────────────────────────────────── */}
      {canManageUsers && !stockLoading && lowStock.length > 0 && (
        <Link
          to={ROUTES.STOCK}
          className={`flex items-center gap-3 rounded-2xl border p-4 shadow-sm transition-colors ${
            outOfStockCount > 0
              ? 'bg-red-50 border-red-100 hover:bg-red-100/70'
              : 'bg-orange-50 border-orange-100 hover:bg-orange-100/70'
          }`}
        >
          <div className={`w-10 h-10 rounded-xl flex items-center justify-center flex-shrink-0 text-white shadow-md ${
            outOfStockCount > 0 ? 'bg-red-500 shadow-red-200' : 'bg-orange-500 shadow-orange-200'
          }`}>
            <PackageX size={18} />
          </div>
          <div className="flex-1 min-w-0">
            <p className={`text-sm font-bold ${outOfStockCount > 0 ? 'text-red-700' : 'text-orange-700'}`}>
              {lowStock.length} produit{lowStock.length > 1 ? 's' : ''} en alerte de stock
            </p>
            <p className={`text-[11px] ${outOfStockCount > 0 ? 'text-red-600/70' : 'text-orange-600/70'}`}>
              {outOfStockCount > 0
                ? `${outOfStockCount} rupture${outOfStockCount > 1 ? 's' : ''} de stock`
                : 'Quantité au ou sous le seuil d\'alerte'}
            </p>
          </div>
          <ArrowRight size={16} className={outOfStockCount > 0 ? 'text-red-400' : 'text-orange-400'} />
        </Link>
      )}

      {/* ── Cartes statistiques ─────────────────────────────── */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        <StatCard icon={Users}     label="Total"    sublabel="utilisateurs"      value={totalUsers}    accent="blue"    loading={loading} />
        <StatCard icon={UserCheck} label="Actifs"   sublabel="comptes activés"   value={activeUsers}   accent="emerald" loading={loading} />
        <StatCard icon={UserX}     label="Inactifs" sublabel="comptes désactivés" value={inactiveUsers} accent="rose"    loading={loading} />

        {/* Taux d'activité */}
        <div className="bg-white rounded-2xl border border-violet-100 p-4 shadow-sm hover:shadow-md transition-shadow">
          <div className="flex items-start justify-between mb-3">
            <div className="w-9 h-9 rounded-xl bg-violet-600 text-white flex items-center justify-center shadow-md shadow-violet-200">
              <TrendingUp size={16} />
            </div>
            {loading
              ? <div className="h-7 w-12 bg-slate-100 rounded-lg animate-pulse" />
              : <span className="text-2xl font-bold text-slate-900">{activeRate}%</span>
            }
          </div>
          <div className="w-full bg-slate-100 rounded-full h-1.5 mb-1.5">
            <div
              className="h-1.5 rounded-full bg-gradient-to-r from-violet-400 to-violet-600 transition-all duration-700 ease-out"
              style={{ width: `${loading ? 0 : activeRate}%` }}
            />
          </div>
          <p className="text-[10px] font-semibold text-slate-700">Taux d'activité</p>
          {!loading && <p className="text-[10px] text-slate-400">{activeUsers}/{totalUsers} actifs</p>}
        </div>
      </div>

      {/* ── Livraisons proches ───────────────────────────────── */}
      {canManageUsers && (
        <div className="bg-white rounded-2xl border border-slate-100 overflow-hidden shadow-sm">
          <div className="flex items-center justify-between gap-3 px-5 py-3.5 border-b border-slate-50 flex-wrap">
            <div className="flex items-center gap-2">
              <span className={`w-7 h-7 rounded-lg flex items-center justify-center flex-shrink-0 ${
                overdueCount > 0 ? 'bg-red-100 text-red-600' : 'bg-amber-100 text-amber-600'
              }`}>
                <Truck size={14} />
              </span>
              <div>
                <h2 className="text-sm font-bold text-slate-800">Livraisons proches</h2>
                <p className="text-[11px] text-slate-400">Commandes à livrer dans le délai sélectionné</p>
              </div>
            </div>
            <div className="flex items-center gap-2 flex-shrink-0">
              {/* Filtrage par délai */}
              <select
                value={dueFilterDays}
                onChange={(e) => setDueFilterDays(Number(e.target.value))}
                className="text-[11px] font-semibold text-slate-600 border border-slate-200 rounded-lg pl-2 pr-6 py-1.5 bg-slate-50 hover:bg-white focus:outline-none focus:ring-2 focus:ring-blue-100 focus:border-blue-400 transition-colors cursor-pointer"
              >
                {DUE_FILTER_OPTIONS.map((opt) => (
                  <option key={opt.value} value={opt.value}>{opt.label}</option>
                ))}
              </select>
              <Link
                to={ROUTES.ORDERS}
                className="flex items-center gap-1 text-[11px] font-semibold text-blue-600 hover:text-blue-700 bg-blue-50 hover:bg-blue-100 px-2.5 py-1.5 rounded-lg transition-colors flex-shrink-0"
              >
                Voir tout <ArrowRight size={11} />
              </Link>
            </div>
          </div>

          {ordersLoading ? (
            <div className="divide-y divide-slate-50">
              {Array.from({ length: 3 }).map((_, i) => (
                <div key={i} className="flex items-center gap-3 px-5 py-3 animate-pulse">
                  <div className="w-9 h-9 rounded-full bg-slate-100 flex-shrink-0" />
                  <div className="flex-1 space-y-1.5">
                    <div className="h-3 bg-slate-100 rounded w-1/3" />
                    <div className="h-2.5 bg-slate-50 rounded w-1/4" />
                  </div>
                  <div className="h-5 w-16 bg-slate-100 rounded-full" />
                </div>
              ))}
            </div>
          ) : upcomingDeliveries.length === 0 ? (
            <div className="py-10 text-center">
              <PackageCheck size={30} className="text-emerald-200 mx-auto mb-2" />
              <p className="text-sm text-slate-400">Aucune livraison urgente pour le moment.</p>
            </div>
          ) : (
            <ul>
              {upcomingDeliveries.map((o, idx) => (
                <li
                  key={o.id}
                  className={`flex items-center gap-3 px-5 py-3 hover:bg-slate-50/80 transition-colors ${idx < upcomingDeliveries.length - 1 ? 'border-b border-slate-50' : ''}`}
                >
                  <div className={`w-9 h-9 rounded-full bg-gradient-to-br ${avatarGradient(o.customerName)} text-white font-bold text-xs flex items-center justify-center flex-shrink-0 shadow-sm`}>
                    {[o.customerName?.[0], o.customerLastName?.[0]].filter(Boolean).join('').toUpperCase() || '?'}
                  </div>
                  <div className="flex-1 min-w-0">
                    <p className="text-sm font-semibold text-slate-800 truncate leading-tight">
                      Commande #{o.id} — {o.customerName} {o.customerLastName}
                    </p>
                    <p className="text-[11px] text-slate-400 flex items-center gap-1">
                      <Clock size={10} />
                      Livraison prévue le {new Date(o.expectedDeliveryDate).toLocaleDateString('fr-FR')}
                    </p>
                  </div>
                  <div className="text-right flex-shrink-0 space-y-1">
                    <span className={`inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-semibold ring-1 ${dueBadgeClass(o.daysLeft)}`}>
                      {dueLabel(o.daysLeft)}
                    </span>
                    <span className={`block w-fit ml-auto text-[10px] font-semibold px-2 py-0.5 rounded-full ring-1 ${ORDER_STATUS_BADGE[o.status] || 'bg-slate-100 text-slate-500 ring-slate-200'}`}>
                      {ORDER_STATUS_LABELS[o.status] || o.status}
                    </span>
                  </div>
                </li>
              ))}
            </ul>
          )}
        </div>
      )}

      {/* ── Produits en alerte de stock ───────────────────────── */}
      {canManageUsers && (
        <div className="bg-white rounded-2xl border border-slate-100 overflow-hidden shadow-sm">
          <div className="flex items-center justify-between px-5 py-3.5 border-b border-slate-50">
            <div className="flex items-center gap-2">
              <span className={`w-7 h-7 rounded-lg flex items-center justify-center flex-shrink-0 ${
                outOfStockCount > 0 ? 'bg-red-100 text-red-600' : 'bg-orange-100 text-orange-600'
              }`}>
                <Boxes size={14} />
              </span>
              <div>
                <h2 className="text-sm font-bold text-slate-800">Produits en alerte de stock</h2>
                <p className="text-[11px] text-slate-400">Quantité au ou sous le seuil d'alerte configuré</p>
              </div>
            </div>
            <Link
              to={ROUTES.STOCK}
              className="flex items-center gap-1 text-[11px] font-semibold text-blue-600 hover:text-blue-700 bg-blue-50 hover:bg-blue-100 px-2.5 py-1.5 rounded-lg transition-colors flex-shrink-0"
            >
              Voir tout <ArrowRight size={11} />
            </Link>
          </div>

          {stockLoading ? (
            <div className="divide-y divide-slate-50">
              {Array.from({ length: 3 }).map((_, i) => (
                <div key={i} className="flex items-center gap-3 px-5 py-3 animate-pulse">
                  <div className="w-9 h-9 rounded-full bg-slate-100 flex-shrink-0" />
                  <div className="flex-1 space-y-1.5">
                    <div className="h-3 bg-slate-100 rounded w-1/3" />
                    <div className="h-2.5 bg-slate-50 rounded w-1/4" />
                  </div>
                  <div className="h-5 w-16 bg-slate-100 rounded-full" />
                </div>
              ))}
            </div>
          ) : lowStock.length === 0 ? (
            <div className="py-10 text-center">
              <PackageCheck size={30} className="text-emerald-200 mx-auto mb-2" />
              <p className="text-sm text-slate-400">Aucun produit en alerte pour le moment.</p>
            </div>
          ) : (
            <ul>
              {lowStock.map((s, idx) => {
                const isOut = Number(s.currentQuantity) <= 0
                const unitLabel = MEASUREMENT_UNIT_LABELS_BACKEND[s.measurementUnit] || s.measurementUnit
                return (
                  <li
                    key={s.id ?? s.productId}
                    className={`flex items-center gap-3 px-5 py-3 hover:bg-slate-50/80 transition-colors ${idx < lowStock.length - 1 ? 'border-b border-slate-50' : ''}`}
                  >
                    <div className={`w-9 h-9 rounded-full flex items-center justify-center flex-shrink-0 shadow-sm ${
                      isOut ? 'bg-gradient-to-br from-red-400 to-red-600' : 'bg-gradient-to-br from-orange-400 to-orange-600'
                    } text-white`}>
                      <PackageX size={15} />
                    </div>
                    <div className="flex-1 min-w-0">
                      <p className="text-sm font-semibold text-slate-800 truncate leading-tight">
                        {s.productName}
                      </p>
                      <p className="text-[11px] text-slate-400">
                        Seuil d'alerte : {s.thresholdValue} {unitLabel}
                      </p>
                    </div>
                    <div className="text-right flex-shrink-0">
                      <span className={`inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-semibold ring-1 ${
                        isOut ? 'bg-red-50 text-red-600 ring-red-100' : 'bg-orange-50 text-orange-600 ring-orange-100'
                      }`}>
                        {isOut ? 'Rupture' : `${s.currentQuantity} ${unitLabel} restant(s)`}
                      </span>
                    </div>
                  </li>
                )
              })}
            </ul>
          )}
        </div>
      )}

      {/* ── 2 colonnes ─────────────────────────────────────── */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-5">

        {/* Derniers inscrits (2/3) */}
        <div className="lg:col-span-2 bg-white rounded-2xl border border-slate-100 overflow-hidden shadow-sm">
          <div className="flex items-center justify-between px-5 py-3.5 border-b border-slate-50">
            <div>
              <h2 className="text-sm font-bold text-slate-800">Derniers inscrits</h2>
              <p className="text-[11px] text-slate-400">6 comptes les plus récents</p>
            </div>
            {canManageUsers && (
              <Link
                to={ROUTES.USERS}
                className="flex items-center gap-1 text-[11px] font-semibold text-blue-600 hover:text-blue-700 bg-blue-50 hover:bg-blue-100 px-2.5 py-1.5 rounded-lg transition-colors"
              >
                Voir tout <ArrowRight size={11} />
              </Link>
            )}
          </div>

          {loading ? (
            <div className="divide-y divide-slate-50">
              {[...Array(4)].map((_, i) => (
                <div key={i} className="flex items-center gap-3 px-5 py-3 animate-pulse">
                  <div className="w-9 h-9 rounded-full bg-slate-100 flex-shrink-0" />
                  <div className="flex-1 space-y-1.5">
                    <div className="h-3 bg-slate-100 rounded w-28" />
                    <div className="h-2.5 bg-slate-50 rounded w-44" />
                  </div>
                  <div className="h-5 w-14 bg-slate-100 rounded-full" />
                </div>
              ))}
            </div>
          ) : recentUsers.length === 0 ? (
            <div className="py-12 text-center">
              <Users size={32} className="text-slate-200 mx-auto mb-2" />
              <p className="text-sm text-slate-400">Aucun utilisateur enregistré.</p>
              {canManageUsers && (
                <Link to={ROUTES.USERS} className="mt-3 inline-block text-xs text-blue-600 hover:underline">
                  Ajouter le premier utilisateur →
                </Link>
              )}
            </div>
          ) : (
            <ul>
              {recentUsers.map((u, idx) => (
                <li
                  key={u.id}
                  className={`flex items-center gap-3 px-5 py-3 hover:bg-slate-50/80 transition-colors ${idx < recentUsers.length - 1 ? 'border-b border-slate-50' : ''}`}
                >
                  <div className={`w-9 h-9 rounded-full bg-gradient-to-br ${avatarGradient(u.firstName)} text-white font-bold text-xs flex items-center justify-center flex-shrink-0 shadow-sm`}>
                    {initials(u)}
                  </div>
                  <div className="flex-1 min-w-0">
                    <p className="text-sm font-semibold text-slate-800 truncate leading-tight">
                      {u.firstName} {u.lastName}
                    </p>
                    <p className="text-[11px] text-slate-400 truncate">{u.email}</p>
                  </div>
                  <div className="text-right flex-shrink-0 space-y-0.5">
                    <span className={`inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-semibold ${u.enabled ? 'bg-emerald-50 text-emerald-600' : 'bg-slate-100 text-slate-500'}`}>
                      <span className={`w-1.5 h-1.5 rounded-full ${u.enabled ? 'bg-emerald-500' : 'bg-slate-400'}`} />
                      {u.enabled ? 'Actif' : 'Inactif'}
                    </span>
                    <p className="text-[10px] text-slate-400">{timeLabel(u.createdAt)}</p>
                  </div>
                </li>
              ))}
            </ul>
          )}
        </div>

        {/* Panneau droit (1/3) */}
        <div className="space-y-4">

          {/* Actions rapides */}
          <div className="bg-white rounded-2xl border border-slate-100 overflow-hidden shadow-sm">
            <div className="px-5 py-3.5 border-b border-slate-50">
              <h3 className="text-[10px] font-bold text-slate-400 uppercase tracking-widest">Actions rapides</h3>
            </div>
            <div className="p-3 space-y-1">
              {canManageUsers && (
                <QuickAction
                  icon={UserPlus}
                  label="Ajouter un utilisateur"
                  sublabel="Créer un nouveau compte"
                  to={ROUTES.USERS}
                  accent="blue"
                />
              )}
              {canManageUsers && (
                <QuickAction
                  icon={Users}
                  label="Gérer les utilisateurs"
                  sublabel="Liste complète · filtres · tri"
                  to={ROUTES.USERS}
                  accent="slate"
                />
              )}
              <button
                onClick={handleRefresh}
                className="w-full flex items-center gap-3 p-3 rounded-xl hover:bg-slate-50 transition-colors group"
              >
                <div className="w-8 h-8 rounded-lg bg-slate-100 flex items-center justify-center flex-shrink-0 group-hover:bg-slate-200 transition-colors">
                  <RefreshCw size={14} className={`text-slate-500 ${(loading || ordersLoading || stockLoading) ? 'animate-spin' : ''}`} />
                </div>
                <div className="flex-1 text-left min-w-0">
                  <p className="text-xs font-semibold text-slate-700">Actualiser</p>
                  <p className="text-[10px] text-slate-400">
                    {(loading || ordersLoading || stockLoading) ? 'Chargement…' : `${lastRefresh.toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' })}`}
                  </p>
                </div>
                <ArrowRight size={13} className="text-slate-300 group-hover:translate-x-0.5 group-hover:text-slate-400 transition-all flex-shrink-0" />
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}

/* ── Sous-composants ────────────────────────────────────── */

const ACCENT_MAP = {
  blue:    { border: 'border-blue-100',    icon: 'bg-blue-600 shadow-blue-100',    text: 'text-white' },
  emerald: { border: 'border-emerald-100', icon: 'bg-emerald-600 shadow-emerald-100', text: 'text-white' },
  rose:    { border: 'border-rose-100',    icon: 'bg-rose-500 shadow-rose-100',    text: 'text-white' },
}

const StatCard = ({ icon: Icon, label, sublabel, value, accent, loading }) => {
  const a = ACCENT_MAP[accent]
  return (
    <div className={`bg-white rounded-2xl border ${a.border} p-4 shadow-sm hover:shadow-md transition-shadow`}>
      <div className="flex items-start justify-between mb-3">
        <div className={`w-9 h-9 rounded-xl flex items-center justify-center ${a.icon} ${a.text} shadow-md`}>
          <Icon size={16} />
        </div>
        {loading
          ? <div className="h-7 w-10 bg-slate-100 rounded-lg animate-pulse" />
          : <p className="text-2xl font-bold text-slate-900">{value}</p>
        }
      </div>
      <p className="text-xs font-semibold text-slate-700">{label}</p>
      <p className="text-[10px] text-slate-400">{sublabel}</p>
    </div>
  )
}

const QUICK_ACCENT = {
  blue:  { bg: 'bg-blue-100 group-hover:bg-blue-200', icon: 'text-blue-600' },
  slate: { bg: 'bg-slate-100 group-hover:bg-slate-200', icon: 'text-slate-600' },
}

const QuickAction = ({ icon: Icon, label, sublabel, to, accent }) => {
  const a = QUICK_ACCENT[accent]
  return (
    <Link to={to} className="flex items-center gap-3 p-3 rounded-xl hover:bg-slate-50 transition-colors group">
      <div className={`w-8 h-8 rounded-lg ${a.bg} flex items-center justify-center flex-shrink-0 transition-colors`}>
        <Icon size={14} className={a.icon} />
      </div>
      <div className="flex-1 min-w-0">
        <p className="text-xs font-semibold text-slate-700">{label}</p>
        <p className="text-[10px] text-slate-400">{sublabel}</p>
      </div>
      <ArrowRight size={13} className="text-slate-300 group-hover:translate-x-0.5 group-hover:text-slate-400 transition-all flex-shrink-0" />
    </Link>
  )
}

export default AdminDashboard
