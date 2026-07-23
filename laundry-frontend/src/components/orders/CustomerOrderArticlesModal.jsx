import { useState, useEffect } from 'react'
import { X, Shirt } from 'lucide-react'
import { getOrderArticles } from '../../api/orderApi'
import { ARTICLE_STATUS_BADGE, ARTICLE_STATUS_LABELS, SERVICE_LABELS } from '../../types/ArticlesType'
import { CLOTHING_TYPE_LABELS } from '../../utils/constants'

/**
 * Modale listant les articles d'une commande, accessible depuis la carte
 * de commande dans l'espace client (bouton "Voir les articles").
 */
const CustomerOrderArticlesModal = ({ order, onClose }) => {
  const [articles, setArticles] = useState([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    let active = true
    setLoading(true)
    getOrderArticles(order.id)
      .then((res) => { if (active) setArticles(res.data?.data?.articles ?? []) })
      .catch(() => { if (active) setArticles([]) })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [order.id])

  const formatCurrency = (amount) =>
    new Intl.NumberFormat('fr-FR', { style: 'currency', currency: 'XAF', maximumFractionDigits: 0 }).format(amount ?? 0)

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/40 backdrop-blur-sm">
      <div className="bg-white rounded-2xl shadow-2xl w-full max-w-lg max-h-[85vh] flex flex-col">
        <div className="flex items-center justify-between px-6 pt-5 pb-4 border-b border-slate-100 flex-shrink-0">
          <div>
            <h2 className="text-sm font-bold text-slate-900 flex items-center gap-2">
              <Shirt size={15} className="text-blue-500" />
              Articles de la commande
            </h2>
            <p className="text-xs text-slate-500 mt-0.5">Commande #{order.id}</p>
          </div>
          <button onClick={onClose} className="p-1.5 text-slate-400 hover:text-slate-600 hover:bg-slate-100 rounded-lg">
            <X size={16} />
          </button>
        </div>

        <div className="overflow-y-auto flex-1 px-6 py-4">
          {loading ? (
            <div className="flex justify-center py-10">
              <div className="w-6 h-6 border-2 border-blue-500 border-t-transparent rounded-full animate-spin" />
            </div>
          ) : articles.length === 0 ? (
            <p className="text-center text-slate-400 text-sm py-8">Aucun article enregistré pour cette commande.</p>
          ) : (
            <div className="space-y-2.5">
              {articles.map((a) => (
                <div key={a.id} className="bg-slate-50 rounded-xl border border-slate-100 px-3.5 py-3">
                  <div className="flex items-center justify-between gap-2 mb-1.5">
                    <p className="text-sm font-semibold text-slate-700">
                      {CLOTHING_TYPE_LABELS[a.clothingType] || a.clothingType}
                    </p>
                    <span className={`flex-shrink-0 inline-flex items-center px-2 py-0.5 rounded-full text-[10px] font-semibold ring-1 ${ARTICLE_STATUS_BADGE[a.status] || 'bg-slate-100 text-slate-500 ring-slate-200'}`}>
                      {ARTICLE_STATUS_LABELS[a.status] || a.status}
                    </span>
                  </div>
                  <p className="text-[11px] text-slate-500 mb-1.5">
                    {(a.services ?? []).map((s) => SERVICE_LABELS[s.service] || s.service).join(', ') || 'Aucun service'}
                  </p>
                  <p className="text-[11px] font-semibold text-slate-600">{formatCurrency(a.totalPrice)}</p>
                </div>
              ))}
            </div>
          )}
        </div>

        <div className="px-6 pb-5 pt-1 flex-shrink-0">
          <button
            onClick={onClose}
            className="w-full py-2.5 border border-slate-200 rounded-xl text-sm font-semibold text-slate-600 hover:bg-slate-50 transition-colors"
          >
            Fermer
          </button>
        </div>
      </div>
    </div>
  )
}

export default CustomerOrderArticlesModal
