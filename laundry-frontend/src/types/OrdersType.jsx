import { Coins, TrendingUp } from 'lucide-react'

// ─── Avatar ──────────────────────────────────────────────────────────────────
const ORDER_AVATAR_PALETTE = [
  'from-cyan-400 to-cyan-600',
  'from-blue-400 to-blue-600',
  'from-violet-400 to-violet-600',
  'from-fuchsia-400 to-fuchsia-600',
  'from-pink-400 to-pink-600',
  'from-rose-400 to-rose-600',
  'from-orange-400 to-orange-600',
  'from-amber-400 to-amber-600',
]

export const getOrderInitials = (order) =>
  `${order?.customerName?.[0] ?? '?'}${order?.customerLastName?.[0] ?? '?'}`.toUpperCase()

export const getOrderAvatarGradient = (customerName = '') =>
  ORDER_AVATAR_PALETTE[(customerName?.charCodeAt(0) ?? 0) % ORDER_AVATAR_PALETTE.length]

// ─── Statut commande — aligné sur OrderStatus.java ───────────────────────────
// CORRECTION : remplacement de CONFIRMED/SHIPPED (inexistants) par
//              RECEIVED/IN_PROGRESS/READY (définis dans OrderStatus.java)
export const ORDER_STATUS_BADGE = {
  PENDING:     'bg-yellow-50 text-yellow-600 ring-yellow-100',
  RECEIVED:    'bg-blue-50 text-blue-600 ring-blue-100',
  IN_PROGRESS: 'bg-purple-50 text-purple-600 ring-purple-100',
  READY:       'bg-teal-50 text-teal-600 ring-teal-100',
  DELIVERED:   'bg-green-50 text-green-600 ring-green-100',
  CANCELLED:   'bg-red-50 text-red-600 ring-red-100',
}

export const ORDER_STATUS_LABELS = {
  PENDING:     'En attente',
  RECEIVED:    'Réceptionné',
  IN_PROGRESS: 'En traitement',
  READY:       'Prêt',
  DELIVERED:   'Livré',
  CANCELLED:   'Annulé',
}

// ─── Statut paiement — aligné sur PaymentStatus.java ─────────────────────────
export const ORDER_PAYMENT_BADGE = {
  PENDING:   'bg-yellow-50 text-yellow-600 ring-yellow-100',
  COMPLETED: 'bg-green-50 text-green-600 ring-green-100',
  FAILED:    'bg-red-50 text-red-600 ring-red-100',
  REFUNDED:  'bg-slate-50 text-slate-600 ring-slate-100',
}

export const ORDER_PAYMENT_LABELS = {
  PENDING:   'En attente',
  COMPLETED: 'Payé',
  FAILED:    'Échoué',
  REFUNDED:  'Remboursé',
}

// ─── Moyens de paiement — alignés sur PaymentMethodType.java ─────────────────
export const PAYMENT_METHOD_LABELS = {
  CASH:           'Espèces',
  CHECK:          'Chèque',
  MOBILE_PAYMENT: 'Paiement mobile',
}

// ─── Statut d'un paiement individuel — aligné sur PaymentStatus.java ─────────
// (un PaymentDto ne peut être que PENDING, COMPLETED ou FAILED : REFUNDED
// concerne le statut de la commande, pas un paiement pris isolément)
export const PAYMENT_STATUS_LABELS = {
  PENDING:   'En attente',
  COMPLETED: 'Confirmé',
  FAILED:    'Échoué',
}

export const PAYMENT_STATUS_BADGE = {
  PENDING:   'bg-yellow-50 text-yellow-600 ring-yellow-100',
  COMPLETED: 'bg-green-50 text-green-600 ring-green-100',
  FAILED:    'bg-red-50 text-red-600 ring-red-100',
}

// ─── Colonnes du tableau ──────────────────────────────────────────────────────
export const ORDER_TABLE_COLUMNS = [
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
    key: 'totalAmount',
    label: 'Montant (FCFA)',
    sortable: true,
    className: 'hidden sm:table-cell',
    render: (order) => {
      // CORRECTION : devise FCFA pour le contexte camerounais
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
    key: 'status',
    label: 'Statut',
    sortable: true,
    className: 'hidden md:table-cell',
    render: (order) => {
      // CORRECTION : utilisation des vrais statuts OrderStatus.java
      const cls = ORDER_STATUS_BADGE[order.status] || 'bg-slate-100 text-slate-500 ring-slate-200'
      return (
        <span className={`inline-flex items-center px-2.5 py-1 rounded-full text-[10px] font-semibold ring-1 ${cls}`}>
          {ORDER_STATUS_LABELS[order.status] || order.status}
        </span>
      )
    },
  },
  {
    key: 'paymentStatus',
    label: 'Paiement',
    sortable: true,
    className: 'hidden lg:table-cell',
    render: (order) => {
      const cls = ORDER_PAYMENT_BADGE[order.paymentStatus] || 'bg-slate-100 text-slate-500 ring-slate-200'
      return (
        <span className={`inline-flex items-center px-2.5 py-1 rounded-full text-[10px] font-semibold ring-1 ${cls}`}>
          {ORDER_PAYMENT_LABELS[order.paymentStatus] || order.paymentStatus}
        </span>
      )
    },
  },
  {
    key: 'itemCount',
    label: 'Articles',
    sortable: false,
    className: 'hidden xl:table-cell',
    render: (order) => (
      <div className="flex items-center gap-1.5 text-[12px] text-slate-600">
        <TrendingUp size={12} className="text-slate-400" />
        <span>{order.itemCount ?? 0} article{(order.itemCount ?? 0) !== 1 ? 's' : ''}</span>
      </div>
    ),
  },
  {
    key: 'depositDate',
    label: 'Date dépôt',
    sortable: true,
    className: 'hidden xl:table-cell',
    render: (order) => {
      if (!order.depositDate) return <span className="text-slate-400 text-[11px]">—</span>
      return (
        <span className="text-[11px] text-slate-400">
          {new Date(order.depositDate).toLocaleDateString('fr-FR')}
        </span>
      )
    },
  },
  {
    key: 'expectedDeliveryDate',
    label: 'Livraison prévue',
    sortable: true,
    className: 'hidden xl:table-cell',
    render: (order) => {
      if (!order.expectedDeliveryDate) return <span className="text-slate-400 text-[11px]">—</span>
      return (
        <span className="text-[11px] text-slate-400">
          {new Date(order.expectedDeliveryDate).toLocaleDateString('fr-FR')}
        </span>
      )
    },
  },
]

// ─── Filtre par délai (date de livraison prévue) ─────────────────────────────
// AJOUT : filtrage des commandes selon leur délai de livraison, en complément
// du filtre par statut. Conçu pour être facilement complété par d'autres
// critères de filtrage à l'avenir (client, paiement, etc.).
export const DELAY_PRESETS = [
  { key: 'all',     label: 'Tous les délais' },
  { key: 'overdue', label: 'En retard' },
  { key: 'today',   label: "Aujourd'hui" },
  { key: 'week',    label: '7 prochains jours' },
  { key: 'month',   label: '30 prochains jours' },
  { key: 'custom',  label: 'Période personnalisée' },
]

export const DEFAULT_DELAY_FILTER = { preset: 'all', from: '', to: '' }

const startOfDay = (date) => {
  const d = new Date(date)
  d.setHours(0, 0, 0, 0)
  return d
}

const isSameDay = (a, b) =>
  a.getFullYear() === b.getFullYear() &&
  a.getMonth() === b.getMonth() &&
  a.getDate() === b.getDate()

/**
 * Détermine si une commande correspond au filtre de délai sélectionné,
 * en se basant sur sa date de livraison prévue (`expectedDeliveryDate`).
 */
export const matchesDelayFilter = (order, filter = DEFAULT_DELAY_FILTER) => {
  const { preset, from, to } = filter
  if (!preset || preset === 'all') return true

  const dueDate = order.expectedDeliveryDate ? startOfDay(order.expectedDeliveryDate) : null
  const today = startOfDay(new Date())

  switch (preset) {
    case 'overdue':
      return !!dueDate && dueDate < today && !['DELIVERED', 'CANCELLED'].includes(order.status)
    case 'today':
      return !!dueDate && isSameDay(dueDate, today)
    case 'week': {
      const end = new Date(today)
      end.setDate(end.getDate() + 7)
      return !!dueDate && dueDate >= today && dueDate <= end
    }
    case 'month': {
      const end = new Date(today)
      end.setDate(end.getDate() + 30)
      return !!dueDate && dueDate >= today && dueDate <= end
    }
    case 'custom': {
      if (!from && !to) return true
      if (!dueDate) return false
      if (from && dueDate < startOfDay(from)) return false
      if (to && dueDate > startOfDay(to)) return false
      return true
    }
    default:
      return true
  }
}

// ─── Champs du formulaire (utilisés si OrderFormModal passe par FormModal) ────
export const getOrderFormFields = (mode = 'create') => [
  {
    name: 'userName',
    label: "Nom d'utilisateur du client",
    type: 'text',
    placeholder: 'Ex: jpmbarga',
    icon: 'user',
    required: true,
  },
  {
    name: 'depositDate',
    label: 'Date de dépôt',
    type: 'date',
    icon: 'calendar',
    required: true,
  },
  {
    name: 'expectedDeliveryDate',
    label: 'Livraison prévue',
    type: 'date',
    icon: 'calendar',
    required: false,
  },
  {
    name: 'shippingAddress',
    label: 'Adresse de livraison',
    type: 'textarea',
    placeholder: 'Ex: Quartier Bastos, Yaoundé',
    icon: 'map-pin',
    required: false,
    rows: 2,
  },
  {
    name: 'notes',
    label: 'Notes',
    type: 'textarea',
    placeholder: 'Remarques supplémentaires',
    icon: 'sticky-note',
    required: false,
    rows: 2,
  },
]