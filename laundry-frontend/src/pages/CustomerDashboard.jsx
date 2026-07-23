import { useState, useEffect, useCallback, useRef } from 'react'
import { useSearchParams } from 'react-router-dom'
import { toast } from 'react-toastify'
import { getCustomerOrders, getCustomerStats } from '../api/customerApi'
import CustomerOrderCardsGrid from '../components/orders/CustomerOrderCardsGrid'
import { useAuth } from '../context/AuthContext'
import { ShoppingCart, TrendingUp, Coins, Package } from 'lucide-react'

const CustomerDashboard = () => {
  const { user } = useAuth()
  const [searchParams] = useSearchParams()
  const highlightOrderId = searchParams.get('orderId')
  const hasScrolled = useRef(false)
  const [orders, setOrders] = useState([])
  const [stats, setStats] = useState(null)
  const [loading, setLoading] = useState(true)

  const loadData = useCallback(async () => {
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

  useEffect(() => { loadData() }, [loadData])

  // Arrivée depuis un lien de notification (?orderId=…) : on scroll une
  // seule fois jusqu'à la carte de la commande visée, une fois les
  // commandes chargées.
  useEffect(() => {
    if (!highlightOrderId || loading || hasScrolled.current) return
    const el = document.getElementById(`order-card-${highlightOrderId}`)
    if (el) {
      el.scrollIntoView({ behavior: 'smooth', block: 'center' })
      hasScrolled.current = true
    }
  }, [highlightOrderId, loading, orders])

  const formatCurrency = (amount) =>
    new Intl.NumberFormat('fr-FR', {
      style: 'currency',
      currency: 'EUR',
    }).format(amount ?? 0)

  return (
    <div className="space-y-6">
      {/* En-tête */}
      <div>
        <h1 className="text-2xl font-bold text-slate-900">
          Bienvenue, {user?.firstName} {user?.lastName} 👋
        </h1>
        <p className="text-sm text-slate-500 mt-1">
          Consultez votre historique de commandes et vos statistiques
        </p>
      </div>

      {/* Statistiques */}
      {stats && (
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          {/* Nombre de commandes */}
          <div className="bg-white rounded-2xl border border-slate-100 p-5 shadow-sm">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-xs font-semibold text-slate-600 uppercase tracking-wide">
                  Commandes passées
                </p>
                <p className="text-2xl font-bold text-slate-900 mt-2">
                  {stats.totalOrders ?? 0}
                </p>
              </div>
              <div className="w-12 h-12 rounded-xl bg-cyan-100 text-cyan-600 flex items-center justify-center">
                <ShoppingCart size={24} />
              </div>
            </div>
          </div>

          {/* Montant total dépensé */}
          <div className="bg-white rounded-2xl border border-slate-100 p-5 shadow-sm">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-xs font-semibold text-slate-600 uppercase tracking-wide">
                  Montant total dépensé
                </p>
                <p className="text-2xl font-bold text-slate-900 mt-2">
                  {formatCurrency(stats.totalSpent)}
                </p>
              </div>
              <div className="w-12 h-12 rounded-xl bg-emerald-100 text-emerald-600 flex items-center justify-center">
                <Coins size={24} />
              </div>
            </div>
          </div>

          {/* Ticket moyen */}
          <div className="bg-white rounded-2xl border border-slate-100 p-5 shadow-sm">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-xs font-semibold text-slate-600 uppercase tracking-wide">
                  Ticket moyen
                </p>
                <p className="text-2xl font-bold text-slate-900 mt-2">
                  {formatCurrency(
                    (stats.totalSpent ?? 0) / (stats.totalOrders ?? 1)
                  )}
                </p>
              </div>
              <div className="w-12 h-12 rounded-xl bg-purple-100 text-purple-600 flex items-center justify-center">
                <TrendingUp size={24} />
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Commandes sous forme de cartes */}
      <div className="space-y-3">
        <div className="flex items-center gap-2">
          <Package size={20} className="text-slate-600" />
          <h2 className="text-lg font-bold text-slate-900">
            Vos commandes ({orders.length})
          </h2>
        </div>
        <CustomerOrderCardsGrid orders={orders} loading={loading} highlightOrderId={highlightOrderId} />
      </div>
    </div>
  )
}

export default CustomerDashboard
