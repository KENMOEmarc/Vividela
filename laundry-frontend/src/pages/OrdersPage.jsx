import { useState, useEffect, useCallback, useMemo } from 'react'
import { useNavigate } from 'react-router-dom'
import { toast } from 'react-toastify'
import { getOrdersByStatus, deleteOrder, cancelOrder } from '../api/orderApi'
import OrderCardsGrid       from '../components/orders/OrderCardsGrid'
import OrderStatusFilter    from '../components/orders/OrderStatusFilter'
import OrderDelayFilter     from '../components/orders/OrderDelayFilter'
import OrderFormModal       from '../components/orders/OrderFormModal'
import OrderPaymentsModal   from '../components/orders/OrderPaymentsModal'
import ArticleFormModal     from '../components/articles/ArticleFormModal'
import OrderConfirmDeleteModal from '../components/orders/OrderConfirmDeleteModal'
import { useAuth }           from '../context/AuthContext'
import { ShoppingCart, Layers } from 'lucide-react'
import { DEFAULT_DELAY_FILTER, matchesDelayFilter } from '../types/OrdersType'
import { ROUTES } from '../utils/constants'

const OrdersPage = () => {
  const { user } = useAuth()
  const navigate = useNavigate()
  const [orders,        setOrders]        = useState([])
  const [loading,       setLoading]       = useState(true)
  const [statusFilter,  setStatusFilter]  = useState([]) // [] == tous les statuts
  const [delayFilter,   setDelayFilter]   = useState(DEFAULT_DELAY_FILTER) // délai de livraison
  const [showForm,      setShowForm]      = useState(false)
  const [editOrder,     setEditOrder]     = useState(null)
  const [showItemForm,  setShowItemForm]  = useState(false)
  const [activeOrder,   setActiveOrder]   = useState(null)
  const [deleteTarget,  setDeleteTarget]  = useState(null)
  const [paymentsOrder, setPaymentsOrder] = useState(null)

  // Tous les utilisateurs authentifiés peuvent gérer les commandes
  const canManage = !!user

  const loadOrders = useCallback(async (statuses) => {
    setLoading(true)
    try {
      const res = await getOrdersByStatus(statuses)
      setOrders(res.data?.data ?? [])
    } catch {
      toast.error('Impossible de charger les commandes')
    } finally {
      setLoading(false)
    }
  }, [])

  // Recharge depuis le serveur à chaque changement de filtre de statut
  // (le statut est filtré côté backend ; le délai est filtré côté client ci-dessous)
  useEffect(() => { loadOrders(statusFilter) }, [statusFilter, loadOrders])

  // Filtre par délai de livraison, appliqué en complément du filtre de statut.
  // Le tableau `orders` est déjà restreint aux statuts sélectionnés par le serveur.
  const visibleOrders = useMemo(
    () => orders.filter((o) => matchesDelayFilter(o, delayFilter)),
    [orders, delayFilter]
  )

  const handleAdd    = () => { setEditOrder(null); setShowForm(true) }
  const handleEdit   = (o) => { setEditOrder(o);   setShowForm(true) }
  const handleSaved  = (savedOrder) => {
    // On ne propose l'ajout immédiat d'articles qu'à la CRÉATION d'une
    // commande (editOrder === null à ce moment-là). En modification,
    // savedOrder a lui aussi un `.id` (c'est la commande mise à jour) : sans
    // cette distinction, fermer une édition rouvrait à tort le formulaire
    // d'article.
    const wasCreating = editOrder === null

    setShowForm(false)
    setEditOrder(null)
    loadOrders(statusFilter)

    if (wasCreating && savedOrder?.id) {
      setActiveOrder(savedOrder)
      setShowItemForm(true)
    }
  }
  const handleItemSaved = () => {
    setShowItemForm(false)
    setActiveOrder(null)
    loadOrders(statusFilter)
  }
  const handleDelete   = (o) => setDeleteTarget(o)
  const handleAddItems = (order) => { setActiveOrder(order); setShowItemForm(true) }
  const handlePayments = (order) => setPaymentsOrder(order)

  // Cliquer sur une commande ouvre la rubrique Articles, filtrée sur cette commande
  const handleSelectOrder = (order) => {
    navigate(`${ROUTES.ARTICLES}?orderId=${order.id}`)
  }

  const handleDeleteConfirm = async () => {
    try {
      await deleteOrder(deleteTarget.id)
      toast.success(`Commande N°${deleteTarget.id} supprimée`)
      setDeleteTarget(null)
      loadOrders(statusFilter)
    } catch {
      toast.error('Erreur lors de la suppression')
    }
  }

  // AJOUT : annulation dédiée (PATCH /orders/{id}/cancel), disponible sur les
  // commandes qui n'ont pas encore atteint READY — voir CORRECTIONS-APPLIQUEES.md.
  const handleCancel = async (order) => {
    if (!window.confirm(`Annuler la commande N°${order.id} ? Cette action est irréversible.`)) {
      return
    }
    try {
      await cancelOrder(order.id)
      toast.success(`Commande N°${order.id} annulée`)
      loadOrders(statusFilter)
    } catch (error) {
      toast.error(error.response?.data?.message || "Impossible d'annuler la commande")
    }
  }

  return (
    <div className="space-y-5">

      {/* En-tête de page */}
      <div className="flex items-start justify-between gap-4">
        <div>
          <h1 className="text-lg font-bold text-slate-900 flex items-center gap-2">
            <span className="w-7 h-7 rounded-lg bg-cyan-100 text-cyan-600 flex items-center justify-center">
              <ShoppingCart size={15} />
            </span>
            Gestion des commandes
          </h1>
          <p className="text-xs text-slate-500 mt-0.5 ml-9">
            {canManage
              ? 'Créez, modifiez et gérez les commandes de vos clients.'
              : 'Vous n\'avez pas les droits pour gérer les commandes.'
            }
          </p>
        </div>
        {/* Nombre total de commandes (après filtres statut + délai) */}
        <span className="flex-shrink-0 flex items-center gap-1.5 px-2.5 py-1 rounded-lg text-xs font-semibold bg-slate-100 text-slate-600">
          <Layers size={12} />
          {visibleOrders.length} commande{visibleOrders.length !== 1 && 's'}
        </span>
      </div>

      {/* Filtres : statut + délai de livraison (combinables) */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
        <OrderStatusFilter selected={statusFilter} onChange={setStatusFilter} />
        <OrderDelayFilter value={delayFilter} onChange={setDelayFilter} />
      </div>

      {/* Cartes de commandes */}
      <OrderCardsGrid
        orders={visibleOrders}
        loading={loading}
        onSelectOrder={handleSelectOrder}
        onAdd={handleAdd}
        onEdit={canManage ? handleEdit : null}
        onDelete={canManage ? handleDelete : null}
        onAddItems={canManage ? handleAddItems : null}
        onPayments={canManage ? handlePayments : null}
        onCancel={canManage ? handleCancel : null}
        canAdd={canManage}
        canEdit={canManage}
        canDelete={canManage}
        canAddItems={canManage}
      />

      {/* Modale de formulaire */}
      {showForm && (
        <OrderFormModal
          order={editOrder}
          onSave={handleSaved}
          onClose={() => setShowForm(false)}
        />
      )}

      {showItemForm && activeOrder && (
        <ArticleFormModal
          orderId={activeOrder.id}
          onSave={handleItemSaved}
          onClose={() => {
            setShowItemForm(false)
            setActiveOrder(null)
          }}
        />
      )}

      {/* Modale des paiements */}
      {paymentsOrder && (
        <OrderPaymentsModal
          order={paymentsOrder}
          onClose={() => setPaymentsOrder(null)}
          onOrderChanged={() => loadOrders(statusFilter)}
        />
      )}

      {/* Modale de suppression */}
      {deleteTarget && (
        <OrderConfirmDeleteModal
          order={deleteTarget}
          isOpen={!!deleteTarget}
          isLoading={false}
          onConfirm={handleDeleteConfirm}
          onCancel={() => setDeleteTarget(null)}
        />
      )}
    </div>
  )
}

export default OrdersPage
