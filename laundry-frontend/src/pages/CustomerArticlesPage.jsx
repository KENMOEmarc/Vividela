import { useState, useEffect, useCallback, useMemo } from 'react'
import { toast } from 'react-toastify'
import { Shirt, Search, Package } from 'lucide-react'
import { useAuth } from '../context/AuthContext'
import { getCustomerOrders } from '../api/customerApi'
import { getOrderArticles } from '../api/orderApi'
import { ARTICLE_STATUS_BADGE, ARTICLE_STATUS_LABELS, SERVICE_LABELS } from '../types/ArticlesType'
import { CLOTHING_TYPE_LABELS } from '../utils/constants'

const formatCurrency = (amount) =>
  new Intl.NumberFormat('fr-FR', { style: 'currency', currency: 'XAF', maximumFractionDigits: 0 }).format(amount ?? 0)

/**
 * Page "Articles" de l'espace client : liste tous les articles de toutes
 * les commandes du client connecté, groupés par commande.
 */
const CustomerArticlesPage = () => {
  const { user } = useAuth()
  const [groups, setGroups] = useState([]) // [{ order, articles }]
  const [loading, setLoading] = useState(true)
  const [search, setSearch] = useState('')

  const loadData = useCallback(async () => {
    if (!user?.id) return
    setLoading(true)
    try {
      const ordersRes = await getCustomerOrders(user.id)
      const orders = ordersRes.data?.data ?? []
      const results = await Promise.allSettled(orders.map((o) => getOrderArticles(o.id)))
      const built = orders.map((o, idx) => ({
        order: o,
        articles: results[idx].status === 'fulfilled' ? (results[idx].value.data?.data?.articles ?? []) : [],
      }))
      built.sort((a, b) => new Date(b.order.orderDate ?? b.order.createdAt) - new Date(a.order.orderDate ?? a.order.createdAt))
      setGroups(built)
    } catch {
      toast.error('Impossible de charger vos articles')
    } finally {
      setLoading(false)
    }
  }, [user?.id])

  useEffect(() => { loadData() }, [loadData])

  const filteredGroups = useMemo(() => {
    if (!search.trim()) return groups
    const q = search.trim().toLowerCase()
    return groups
      .map((g) => ({
        ...g,
        articles: g.articles.filter((a) =>
          [CLOTHING_TYPE_LABELS[a.clothingType], a.clothingType, ARTICLE_STATUS_LABELS[a.status], String(g.order.id)]
            .filter(Boolean)
            .some((f) => f.toLowerCase().includes(q))
        ),
      }))
      .filter((g) => g.articles.length > 0)
  }, [groups, search])

  const totalArticles = groups.reduce((sum, g) => sum + g.articles.length, 0)

  return (
    <div className="space-y-5">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-2xl font-bold text-slate-900 flex items-center gap-2">
            <Shirt size={22} className="text-blue-600" /> Mes articles
          </h1>
          <p className="text-sm text-slate-500 mt-1">
            {totalArticles} article{totalArticles !== 1 ? 's' : ''} répartis dans vos commandes
          </p>
        </div>
        <div className="relative">
          <Search size={14} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
          <input
            type="text"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Rechercher un article…"
            className="pl-8 pr-3 py-2 text-xs rounded-xl border border-slate-200 focus:outline-none focus:ring-2 focus:ring-blue-100 focus:border-blue-300 w-48 sm:w-64"
          />
        </div>
      </div>

      {loading ? (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {[...Array(3)].map((_, i) => (
            <div key={i} className="bg-white rounded-2xl border border-slate-100 p-4 h-28 animate-pulse" />
          ))}
        </div>
      ) : filteredGroups.length === 0 ? (
        <div className="flex flex-col items-center justify-center py-16 text-slate-400 gap-2 bg-white rounded-2xl border border-slate-100">
          <Package size={28} className="text-slate-300" />
          <p className="text-sm">Aucun article trouvé.</p>
        </div>
      ) : (
        <div className="space-y-6">
          {filteredGroups.map(({ order, articles }) => (
            <div key={order.id} className="bg-white rounded-2xl border border-slate-100 overflow-hidden shadow-sm">
              <div className="flex items-center justify-between px-5 py-3.5 border-b border-slate-50">
                <div className="flex items-center gap-2">
                  <span className="w-7 h-7 rounded-lg bg-blue-100 text-blue-600 flex items-center justify-center flex-shrink-0 text-[11px] font-bold">
                    #{order.id}
                  </span>
                  <h2 className="text-sm font-bold text-slate-800">
                    Commande #{order.id} · {articles.length} article{articles.length !== 1 ? 's' : ''}
                  </h2>
                </div>
              </div>
              <div className="p-4 grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
                {articles.map((a) => (
                  <div key={a.id} className="bg-slate-50 rounded-xl border border-slate-100 px-3.5 py-3">
                    <div className="flex items-center justify-between gap-2 mb-1.5">
                      <p className="text-sm font-semibold text-slate-700 truncate">
                        {CLOTHING_TYPE_LABELS[a.clothingType] || a.clothingType}
                      </p>
                      <span className={`flex-shrink-0 inline-flex items-center px-2 py-0.5 rounded-full text-[10px] font-semibold ring-1 ${ARTICLE_STATUS_BADGE[a.status] || 'bg-slate-100 text-slate-500 ring-slate-200'}`}>
                        {ARTICLE_STATUS_LABELS[a.status] || a.status}
                      </span>
                    </div>
                    <p className="text-[11px] text-slate-500 mb-1.5 truncate">
                      {(a.services ?? []).map((s) => SERVICE_LABELS[s.service] || s.service).join(', ') || 'Aucun service'}
                    </p>
                    <p className="text-[11px] font-semibold text-slate-600">{formatCurrency(a.totalPrice)}</p>
                  </div>
                ))}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}

export default CustomerArticlesPage
