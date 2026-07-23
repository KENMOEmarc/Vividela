import { Coins, Shirt } from 'lucide-react'
import {
  getOrderInitials,
  getOrderAvatarGradient,
  ORDER_PAYMENT_BADGE,
  ORDER_PAYMENT_LABELS,
} from './OrdersType'

// ─── Colonnes du tableau des tickets ────────────────────────────────────────
export const TICKET_TABLE_COLUMNS = [
  {
    key: 'id',
    label: 'N° Commande',
    sortable: true,
    className: 'w-20',
    render: (order) => (
      <span className="font-mono text-[11px] font-semibold text-slate-700">#{order.id}</span>
    ),
  },
  {
    key: 'ticketNumber',
    label: 'N° Ticket',
    sortable: true,
    className: 'hidden sm:table-cell',
    render: (order) => (
      order.ticketNumber
        ? <span className="font-mono text-[11px] font-semibold text-teal-700 bg-teal-50 px-2 py-0.5 rounded-md">{order.ticketNumber}</span>
        : <span className="text-[11px] text-slate-300">—</span>
    ),
  },
  {
    key: 'customerName',
    label: 'Client',
    sortable: true,
    render: (order) => (
      <div className="flex items-center gap-2.5">
        <div
          className={`w-9 h-9 rounded-full bg-gradient-to-br ${getOrderAvatarGradient(order.customerName)} text-white font-bold text-[11px] flex items-center justify-center flex-shrink-0 shadow-sm`}
        >
          {getOrderInitials(order)}
        </div>
        <div className="min-w-0">
          <p className="font-semibold text-slate-800 leading-tight truncate">
            {order.customerName} {order.customerLastName}
          </p>
          <p className="text-[10px] text-slate-400 truncate">{order.userName ? `@${order.userName}` : order.customerEmail}</p>
        </div>
      </div>
    ),
  },
  {
    key: 'itemCount',
    label: 'Vêtements',
    sortable: false,
    className: 'hidden sm:table-cell',
    render: (order) => (
      <div className="flex items-center gap-1.5 text-[12px] text-slate-600">
        <Shirt size={12} className="text-slate-400" />
        <span>{order.itemCount ?? 0} article{(order.itemCount ?? 0) !== 1 ? 's' : ''}</span>
      </div>
    ),
  },
  {
    key: 'totalAmount',
    label: 'Montant (FCFA)',
    sortable: true,
    className: 'hidden sm:table-cell',
    render: (order) => {
      const formatted = new Intl.NumberFormat('fr-FR', {
        style: 'currency',
        currency: 'XAF',
        maximumFractionDigits: 0,
      }).format(order.totalAmount ?? 0)
      return (
        <span className="text-[12px] font-semibold text-slate-700 flex items-center gap-1">
          <Coins size={12} className="text-slate-400" />
          {formatted}
        </span>
      )
    },
  },
  {
    key: 'paymentStatus',
    label: 'Paiement',
    sortable: true,
    className: 'hidden md:table-cell',
    render: (order) => {
      const cls = ORDER_PAYMENT_BADGE[order.paymentStatus] || 'bg-slate-100 text-slate-500 ring-slate-200'
      return (
        <span className={`inline-flex items-center px-2.5 py-1 rounded-full text-[10px] font-semibold ring-1 ${cls}`}>
          {ORDER_PAYMENT_LABELS[order.paymentStatus] || order.paymentStatus}
        </span>
      )
    },
  },
]
