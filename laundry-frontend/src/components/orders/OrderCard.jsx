import { Coins, TrendingUp, Calendar, Truck, Pencil, Trash2, CreditCard, Ban } from 'lucide-react'
import ActionButton from '../common/ActionButton'
import {
  getOrderInitials,
  getOrderAvatarGradient,
  ORDER_STATUS_BADGE,
  ORDER_STATUS_LABELS,
  ORDER_PAYMENT_BADGE,
  ORDER_PAYMENT_LABELS,
} from '../../types/OrdersType'

const formatAmount = (value) =>
  new Intl.NumberFormat('fr-FR', {
    style: 'currency',
    currency: 'XAF',
    maximumFractionDigits: 0,
  }).format(value ?? 0)

const formatDate = (value) => (value ? new Date(value).toLocaleDateString('fr-FR') : '—')

/**
 * Carte représentant une commande. Cliquer dessus la sélectionne pour
 * afficher ses articles (géré par le parent via `onSelect`).
 */
const OrderCard = ({
  order,
  onSelect,
  onEdit,
  onDelete,
  onAddItems,
  onPayments,
  onCancel,
  canEdit = true,
  canDelete = true,
  canAddItems = true,
}) => {
  const statusCls = ORDER_STATUS_BADGE[order.status] || 'bg-slate-100 text-slate-500 ring-slate-200'
  const paymentCls = ORDER_PAYMENT_BADGE[order.paymentStatus] || 'bg-slate-100 text-slate-500 ring-slate-200'
  // AJOUT : l'annulation dédiée (PATCH /orders/{id}/cancel) n'est utile
  // qu'AVANT le stade READY — au-delà, le changement de statut manuel
  // (DELIVERED/CANCELLED) passe par le formulaire d'édition, qui applique la
  // machine à états stricte post-READY.
  const canCancel = !['READY', 'DELIVERED', 'CANCELLED'].includes(order.status)
  // CORRECTION : une commande livrée/annulée/déjà payée est verrouillée côté
  // backend — ArticleServiceImpl refuse tout ajout d'article dans ce cas.
  const isLocked = order.status === 'DELIVERED' || order.status === 'CANCELLED' || order.paymentStatus === 'COMPLETED'
  // CORRECTION : OrderServiceImpl.deleteOrder() refuse la suppression d'une
  // commande déjà payée (utiliser l'annulation pour préserver l'historique).
  const canDeleteOrder = order.paymentStatus !== 'COMPLETED'

  return (
    <div
      role="button"
      tabIndex={0}
      title="Voir les articles de cette commande"
      onClick={() => onSelect?.(order)}
      onKeyDown={(e) => (e.key === 'Enter' || e.key === ' ') && onSelect?.(order)}
      className="bg-white rounded-2xl border border-slate-100 p-4 shadow-sm cursor-pointer transition-all hover:border-slate-200 hover:shadow-md"
    >
      {/* En-tête : client + n° commande */}
      <div className="flex items-start justify-between gap-2 mb-3">
        <div className="flex items-center gap-2.5 min-w-0">
          <div
            className={`w-10 h-10 rounded-full bg-gradient-to-br ${getOrderAvatarGradient(order.customerName)} text-white font-bold text-xs flex items-center justify-center flex-shrink-0 shadow-sm`}
          >
            {getOrderInitials(order)}
          </div>
          <div className="min-w-0">
            <p className="font-semibold text-slate-800 text-sm leading-tight truncate">
              {order.customerName} {order.customerLastName}
            </p>
            <p className="text-[11px] text-slate-400 font-mono">#{order.id}</p>
          </div>
        </div>
        <span className={`flex-shrink-0 inline-flex items-center px-2.5 py-1 rounded-full text-[10px] font-semibold ring-1 ${statusCls}`}>
          {ORDER_STATUS_LABELS[order.status] || order.status}
        </span>
      </div>

      {/* Corps : montant, articles, paiement */}
      <div className="grid grid-cols-2 gap-2 mb-3">
        <div className="flex items-center gap-1.5 text-[12px] font-semibold text-slate-700">
          <Coins size={12} className="text-slate-400 flex-shrink-0" />
          {formatAmount(order.totalAmount)}
        </div>
        <div className="flex items-center gap-1.5 text-[12px] text-slate-600">
          <TrendingUp size={12} className="text-slate-400 flex-shrink-0" />
          {order.itemCount ?? 0} article{(order.itemCount ?? 0) !== 1 ? 's' : ''}
        </div>
        <div className="flex items-center gap-1.5 text-[11px] text-slate-500">
          <Calendar size={12} className="text-slate-400 flex-shrink-0" />
          {formatDate(order.depositDate)}
        </div>
        <div className="flex items-center gap-1.5 text-[11px] text-slate-500">
          <Truck size={12} className="text-slate-400 flex-shrink-0" />
          {formatDate(order.expectedDeliveryDate)}
        </div>
      </div>

      {/* Pied : statut paiement + actions */}
      <div className="flex items-center justify-between pt-3 border-t border-slate-50">
        <span className={`inline-flex items-center px-2.5 py-1 rounded-full text-[10px] font-semibold ring-1 ${paymentCls}`}>
          {ORDER_PAYMENT_LABELS[order.paymentStatus] || order.paymentStatus}
        </span>

        <div className="flex items-center gap-1" onClick={(e) => e.stopPropagation()}>
          {canAddItems && !isLocked && onAddItems && (
            <ActionButton variant="add" label="Ajouter des articles" onClick={() => onAddItems(order)} />
          )}
          {onPayments && (
            <ActionButton
              icon={CreditCard}
              label="Paiements"
              onClick={() => onPayments(order)}
              className="text-slate-400 hover:text-indigo-600 hover:bg-indigo-50"
            />
          )}
          {canEdit && canCancel && onCancel && (
            <ActionButton
              icon={Ban}
              label="Annuler la commande"
              onClick={() => onCancel(order)}
              className="text-slate-400 hover:text-orange-600 hover:bg-orange-50"
            />
          )}
          {canEdit && onEdit && (
            <button
              type="button"
              onClick={() => onEdit(order)}
              className="p-1.5 rounded-lg text-slate-400 hover:text-blue-600 hover:bg-blue-50"
              title="Modifier"
            >
              <Pencil size={14} />
            </button>
          )}
          {canDelete && canDeleteOrder && onDelete && (
            <button
              type="button"
              onClick={() => onDelete(order)}
              className="p-1.5 rounded-lg text-slate-400 hover:text-red-600 hover:bg-red-50"
              title="Supprimer"
            >
              <Trash2 size={14} />
            </button>
          )}
        </div>
      </div>
    </div>
  )
}

export default OrderCard
