import { useState, useEffect, useCallback, useMemo } from 'react'
import { useSearchParams } from 'react-router-dom'
import { toast } from 'react-toastify'
import { getAllArticles, deleteArticle } from '../api/articleApi'
import { getOrderById } from '../api/orderApi'
import ArticleCardGrid           from '../components/articles/ArticleCardGrid'
import ArticleFormModal         from '../components/articles/ArticleFormModal'
import ArticleConfirmDeleteModal from '../components/articles/ArticleConfirmDeleteModal'
import { useAuth }              from '../context/AuthContext'
import { Shirt, Layers, X }     from 'lucide-react'
import { ORDER_STATUS_BADGE, ORDER_STATUS_LABELS } from '../types/OrdersType'

const ArticlesPage = () => {
  const { user, isAdmin, isManager, isEmployee } = useAuth()
  const [searchParams, setSearchParams] = useSearchParams()
  // Filtre optionnel : n'afficher que les articles d'une commande précise
  // (ouvert depuis la rubrique Commandes en cliquant sur une carte de commande).
  const orderIdParam = searchParams.get('orderId')
  const orderId = orderIdParam ? Number(orderIdParam) : null

  const [articles,     setArticles]     = useState([])
  const [loading,      setLoading]      = useState(true)
  const [filterOrder,  setFilterOrder]  = useState(null) // détails de la commande filtrée
  const [showForm,     setShowForm]     = useState(false)
  const [editArticle,  setEditArticle]  = useState(null)
  const [deleteTarget, setDeleteTarget] = useState(null)

  // Seuls Admin et Manager peuvent créer un article : un Employé peut
  // uniquement consulter la liste et modifier les articles existants.
  // Seul un Admin peut supprimer un article (même règle que pour les
  // utilisateurs).
  const canAdd    = isAdmin() || isManager()
  const canEdit   = isAdmin() || isManager() || isEmployee()
  const canDelete = isAdmin()
  const canManage = canEdit

  const loadArticles = useCallback(async () => {
    setLoading(true)
    try {
      const res = await getAllArticles()
      setArticles(res.data?.data ?? [])
    } catch {
      toast.error('Impossible de charger les articles')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => { loadArticles() }, [loadArticles])

  // Charge les infos de la commande dont on filtre les articles, pour l'afficher dans le bandeau
  useEffect(() => {
    if (!orderId) { setFilterOrder(null); return }
    let cancelled = false
    getOrderById(orderId)
      .then((res) => { if (!cancelled) setFilterOrder(res.data?.data ?? null) })
      .catch(() => { if (!cancelled) setFilterOrder(null) })
    return () => { cancelled = true }
  }, [orderId])

  const visibleArticles = useMemo(
    () => (orderId ? articles.filter((a) => a.orderId === orderId) : articles),
    [articles, orderId]
  )

  const clearOrderFilter = () => setSearchParams({})

  const handleAdd    = () => { setEditArticle(null); setShowForm(true) }
  const handleEdit   = (a) => { setEditArticle(a);   setShowForm(true) }
  const handleSaved  = () => { setShowForm(false); setEditArticle(null); loadArticles() }
  const handleDelete = (a) => setDeleteTarget(a)

  const handleDeleteConfirm = async () => {
    try {
      await deleteArticle(deleteTarget.id)
      toast.success(`Article #${deleteTarget.id} supprimé`)
      setDeleteTarget(null)
      loadArticles()
    } catch {
      toast.error('Erreur lors de la suppression')
    }
  }

  return (
      <div className="space-y-5">

        <div className="flex items-start justify-between gap-4">
          <div>
            <h1 className="text-lg font-bold text-slate-900 flex items-center gap-2">
            <span className="w-7 h-7 rounded-lg bg-indigo-100 text-indigo-600 flex items-center justify-center">
              <Shirt size={15} />
            </span>
              Gestion des vêtements (articles)
            </h1>
            <p className="text-xs text-slate-500 mt-0.5 ml-9">
              {canManage
                  ? 'Ajoutez et suivez les vêtements déposés pour nettoyage.'
                  : "Vous n'avez pas les droits pour gérer les articles."
              }
            </p>
          </div>
          <span className="flex-shrink-0 flex items-center gap-1.5 px-2.5 py-1 rounded-lg text-xs font-semibold bg-slate-100 text-slate-600">
          <Layers size={12} />
            {visibleArticles.length} article{visibleArticles.length !== 1 && 's'}
        </span>
        </div>

        {/* Bandeau de filtre actif : articles d'une commande précise (ouvert depuis Commandes) */}
        {orderId && (
          <div className="flex items-center justify-between gap-3 bg-cyan-50 border border-cyan-100 rounded-2xl px-4 py-3">
            <div className="flex items-center gap-2.5 min-w-0 text-xs">
              <span className="font-semibold text-cyan-700">
                Articles de la commande <span className="font-mono">#{orderId}</span>
              </span>
              {filterOrder && (
                <>
                  <span className="text-cyan-300">·</span>
                  <span className="text-cyan-700 truncate">
                    {filterOrder.customerName} {filterOrder.customerLastName}
                  </span>
                  <span
                    className={`inline-flex items-center px-2 py-0.5 rounded-full text-[10px] font-semibold ring-1 ${
                      ORDER_STATUS_BADGE[filterOrder.status] || 'bg-slate-100 text-slate-500 ring-slate-200'
                    }`}
                  >
                    {ORDER_STATUS_LABELS[filterOrder.status] || filterOrder.status}
                  </span>
                </>
              )}
            </div>
            <button
              type="button"
              onClick={clearOrderFilter}
              className="flex-shrink-0 inline-flex items-center gap-1 text-[11px] font-semibold text-cyan-700 hover:text-cyan-900 hover:bg-cyan-100 rounded-lg px-2 py-1"
            >
              <X size={12} />
              Voir tous les articles
            </button>
          </div>
        )}

        <ArticleCardGrid
            articles={visibleArticles}
            loading={loading}
            onAdd={handleAdd}
            onEdit={canEdit ? handleEdit : null}
            onDelete={canDelete ? handleDelete : null}
            canAdd={canAdd}
            canEdit={canEdit}
            canDelete={canDelete}
            title={orderId ? `Articles · Commande #${orderId}` : 'Articles'}
        />

        {showForm && (
            <ArticleFormModal
                article={editArticle}
                orderId={!editArticle ? orderId : undefined}
                onSave={handleSaved}
                onClose={() => setShowForm(false)}
            />
        )}

        {deleteTarget && (
            <ArticleConfirmDeleteModal
                article={deleteTarget}
                isOpen={!!deleteTarget}
                isLoading={false}
                onConfirm={handleDeleteConfirm}
                onCancel={() => setDeleteTarget(null)}
            />
        )}
      </div>
  )
}

export default ArticlesPage