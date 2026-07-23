import { Coins, Package, Calendar, Truck, Shirt, Receipt, MessageSquareHeart, CheckCircle2 } from 'lucide-react'
import {
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
 * Carte "commande" affichée côté client (espace client).
 * Toujours présentée sous forme de carte (jamais de tableau) avec deux
 * actions : voir les articles de la commande, ou voir le ticket / reçu.
 */
const CustomerOrderCard = ({ order, onViewArticles, onViewTicket, onGiveFeedback, highlighted = false }) => {
  const statusCls = ORDER_STATUS_BADGE[order.status] || 'bg-slate-100 text-slate-500 ring-slate-200'
  const paymentCls = ORDER_PAYMENT_BADGE[order.paymentStatus] || 'bg-slate-100 text-slate-500 ring-slate-200'

  return (
    <div
      id={`order-card-${order.id}`}
      className={`bg-white rounded-2xl border p-4 shadow-sm transition-all hover:shadow-md ${
        highlighted
          ? 'border-blue-300 ring-2 ring-blue-200'
          : 'border-slate-100 hover:border-slate-200'
      }`}
    >
      {/* En-tête : n° commande + statut */}
      <div className="flex items-start justify-between gap-2 mb-3">
        <div className="flex items-center gap-2.5 min-w-0">
          <div className="w-10 h-10 rounded-full bg-gradient-to-br from-blue-400 to-blue-600 text-white font-bold text-xs flex items-center justify-center flex-shrink-0 shadow-sm">
            #{order.id}
          </div>
          <div className="min-w-0">
            <p className="font-semibold text-slate-800 text-sm leading-tight truncate">
              Commande #{order.id}
            </p>
            <p className="text-[11px] text-slate-400">
              {order.itemCount ?? 0} article{(order.itemCount ?? 0) !== 1 ? 's' : ''}
            </p>
          </div>
        </div>
        <span className={`flex-shrink-0 inline-flex items-center px-2.5 py-1 rounded-full text-[10px] font-semibold ring-1 ${statusCls}`}>
          {ORDER_STATUS_LABELS[order.status] || order.status}
        </span>
      </div>

      {/* Corps : montant, dates */}
      <div className="grid grid-cols-2 gap-2 mb-3">
        <div className="flex items-center gap-1.5 text-[12px] font-semibold text-slate-700">
          <Coins size={12} className="text-slate-400 flex-shrink-0" />
          {formatAmount(order.totalAmount)}
        </div>
        <div className="flex items-center gap-1.5 text-[12px] text-slate-600">
          <Package size={12} className="text-slate-400 flex-shrink-0" />
          {order.ticketNumber ? `Ticket ${order.ticketNumber}` : 'Sans ticket'}
        </div>
        <div className="flex items-center gap-1.5 text-[11px] text-slate-500">
          <Calendar size={12} className="text-slate-400 flex-shrink-0" />
          Déposé le {formatDate(order.depositDate)}
        </div>
        <div className="flex items-center gap-1.5 text-[11px] text-slate-500">
          <Truck size={12} className="text-slate-400 flex-shrink-0" />
          {formatDate(order.expectedDeliveryDate)}
        </div>
      </div>

      {/* Statut paiement */}
      <div className="mb-3">
        <span className={`inline-flex items-center px-2.5 py-1 rounded-full text-[10px] font-semibold ring-1 ${paymentCls}`}>
          Paiement · {ORDER_PAYMENT_LABELS[order.paymentStatus] || order.paymentStatus}
        </span>
      </div>

      {/* Actions : articles / ticket & reçu */}
      <div className="grid grid-cols-2 gap-2 pt-3 border-t border-slate-50">
        <button
          type="button"
          onClick={() => onViewArticles?.(order)}
          className="inline-flex items-center justify-center gap-1.5 text-[11px] font-semibold text-blue-600 hover:text-blue-700 bg-blue-50 hover:bg-blue-100 px-2.5 py-2 rounded-lg transition-colors"
        >
          <Shirt size={13} /> Voir les articles
        </button>
        <button
          type="button"
          onClick={() => onViewTicket?.(order)}
          className="inline-flex items-center justify-center gap-1.5 text-[11px] font-semibold text-emerald-600 hover:text-emerald-700 bg-emerald-50 hover:bg-emerald-100 px-2.5 py-2 rounded-lg transition-colors"
        >
          <Receipt size={13} /> Ticket &amp; reçu
        </button>
      </div>

      {/* Formulaire d'avis — visible uniquement une fois la commande livrée */}
      {order.status === 'DELIVERED' && (
        <div className="pt-2 mt-2 border-t border-slate-50">
          {order.feedbackStatus === 'SUBMITTED' ? (
            <button
              type="button"
              onClick={() => onGiveFeedback?.(order)}
              className="w-full inline-flex items-center justify-center gap-1.5 text-[11px] font-semibold text-slate-400 hover:text-slate-500 bg-slate-50 hover:bg-slate-100 px-2.5 py-2 rounded-lg transition-colors"
            >
              <CheckCircle2 size={13} /> Avis envoyé — merci !
            </button>
          ) : (
            <button
              type="button"
              onClick={() => onGiveFeedback?.(order)}
              className="w-full inline-flex items-center justify-center gap-1.5 text-[11px] font-semibold text-pink-600 hover:text-pink-700 bg-pink-50 hover:bg-pink-100 px-2.5 py-2 rounded-lg transition-colors"
            >
              <MessageSquareHeart size={13} /> Donner mon avis
            </button>
          )}
        </div>
      )}
    </div>
  )
}

export default CustomerOrderCard
