import { User, Mail, Phone, TrendingUp } from 'lucide-react'
import { formatCurrencyXAF } from '../utils/format'

// ─── Avatar ──────────────────────────────────────────────────────────────────
const CUSTOMER_AVATAR_PALETTE = [
  'from-blue-400 to-blue-600',
  'from-cyan-400 to-cyan-600',
  'from-teal-400 to-teal-600',
  'from-emerald-400 to-emerald-600',
  'from-green-400 to-green-600',
  'from-lime-400 to-lime-600',
  'from-yellow-400 to-yellow-600',
  'from-amber-400 to-amber-600',
]

export const getCustomerInitials = (customer) =>
  `${customer?.firstName?.[0] ?? '?'}${customer?.lastName?.[0] ?? '?'}`.toUpperCase()

export const getCustomerAvatarGradient = (firstName = '') =>
  CUSTOMER_AVATAR_PALETTE[(firstName?.charCodeAt(0) ?? 0) % CUSTOMER_AVATAR_PALETTE.length]

// ─── Colonnes du tableau ──────────────────────────────────────────────────────
export const CUSTOMER_TABLE_COLUMNS = [
  {
    key: 'id',
    label: 'ID',
    sortable: true,
    className: 'w-16',
    render: (customer) => (
      <span className="font-mono text-[11px] text-slate-400">#{customer.id}</span>
    ),
  },
  {
    key: 'firstName',
    label: 'Client',
    sortable: true,
    render: (customer) => (
      <div className="flex items-center gap-2.5">
        <div
          className={`w-9 h-9 rounded-full bg-gradient-to-br ${getCustomerAvatarGradient(customer.firstName)} text-white font-bold text-[11px] flex items-center justify-center flex-shrink-0 shadow-sm`}
        >
          {getCustomerInitials(customer)}
        </div>
        <div className="min-w-0">
          <p className="font-semibold text-slate-800 leading-tight truncate">
            {customer.firstName} {customer.lastName}
          </p>
          <p className="text-[10px] text-slate-400 truncate">{customer.email}</p>
        </div>
      </div>
    ),
  },
  {
    key: 'email',
    label: 'Email',
    sortable: true,
    className: 'hidden md:table-cell',
    render: (customer) => (
      <span className="text-[12px] text-slate-500 flex items-center gap-1.5">
        <Mail size={12} className="text-slate-400" />
        {customer.email}
      </span>
    ),
  },
  {
    key: 'phone',
    label: 'Téléphone',
    sortable: false,
    className: 'hidden lg:table-cell',
    render: (customer) => (
      <span className="text-[12px] text-slate-500 flex items-center gap-1.5">
        <Phone size={12} className="text-slate-400" />
        {customer.phone || '—'}
      </span>
    ),
  },
  {
    key: 'orderCount',
    label: 'Commandes',
    sortable: true,
    className: 'hidden xl:table-cell',
    render: (customer) => (
      <div className="flex items-center gap-1.5 text-[12px] text-slate-600">
        <TrendingUp size={12} className="text-slate-400" />
        <span>{customer.orderCount ?? 0} commande{(customer.orderCount ?? 0) !== 1 ? 's' : ''}</span>
      </div>
    ),
  },
  {
    key: 'totalSpent',
    label: 'Montant total',
    sortable: true,
    className: 'hidden xl:table-cell',
    render: (customer) => (
      <span className="text-[12px] font-semibold text-slate-700">
        {formatCurrencyXAF(customer.totalSpent)}
      </span>
    ),
  },
  {
    key: 'createdAt',
    label: 'Client depuis',
    sortable: true,
    className: 'hidden xl:table-cell',
    render: (customer) => {
      if (!customer.createdAt) return <span className="text-slate-400 text-[11px]">—</span>
      return (
        <span className="text-[11px] text-slate-400">
          {new Date(customer.createdAt).toLocaleDateString('fr-FR')}
        </span>
      )
    },
  },
]

// ─── Champs du formulaire ─────────────────────────────────────────────────────
export const getCustomerFormFields = () => [
  {
    name: 'firstName',
    label: 'Prénom',
    type: 'text',
    placeholder: 'Jean',
    icon: 'user',
    required: true,
  },
  {
    name: 'lastName',
    label: 'Nom',
    type: 'text',
    placeholder: 'Dupont',
    icon: 'user',
    required: true,
  },
  {
    name: 'email',
    label: 'Email',
    type: 'email',
    placeholder: 'jean.dupont@example.com',
    icon: 'mail',
    required: true,
  },
  {
    name: 'phone',
    label: 'Téléphone',
    type: 'tel',
    placeholder: '+33612345678',
    icon: 'phone',
    required: false,
  },
  {
    name: 'address',
    label: 'Adresse',
    type: 'textarea',
    placeholder: 'Adresse complète',
    icon: 'map-pin',
    required: false,
    rows: 3,
  },
  {
    name: 'notes',
    label: 'Notes',
    type: 'textarea',
    placeholder: 'Remarques additionnelles',
    icon: 'sticky-note',
    required: false,
    rows: 2,
  },
]
