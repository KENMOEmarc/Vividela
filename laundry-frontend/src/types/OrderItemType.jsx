import { Shirt, Coins, Percent } from 'lucide-react'

// ─── Colonnes du tableau des articles de commande ──────────────────────────────
export const ORDER_ITEM_TABLE_COLUMNS = [
  {
    key: 'id',
    label: 'ID',
    sortable: true,
    className: 'w-12',
    render: (item) => (
      <span className="font-mono text-[11px] text-slate-400">#{item.id}</span>
    ),
  },
  {
    key: 'articleId',
    label: 'Article',
    sortable: true,
    render: (item) => (
      <div className="flex items-center gap-2">
        <Shirt size={14} className="text-slate-400" />
        <span className="text-[12px] font-medium text-slate-700">
          {item.article?.title || `Article #${item.articleId}`}
        </span>
      </div>
    ),
  },
  {
    key: 'quantity',
    label: 'Quantité',
    sortable: true,
    className: 'hidden sm:table-cell',
    render: (item) => (
      <span className="text-[12px] text-slate-600 font-medium">
        {item.quantity}
      </span>
    ),
  },
  {
    key: 'unitPrice',
    label: 'Prix unitaire',
    sortable: true,
    className: 'hidden sm:table-cell',
    render: (item) => {
      const formatted = new Intl.NumberFormat('fr-FR', {
        style: 'currency',
        currency: 'EUR',
      }).format(item.unitPrice ?? 0)
      return (
        <span className="text-[12px] text-slate-600 flex items-center gap-1">
          <Coins size={12} className="text-slate-400" />
          {formatted}
        </span>
      )
    },
  },
  {
    key: 'subtotal',
    label: 'Sous-total',
    sortable: true,
    className: 'hidden md:table-cell',
    render: (item) => {
      const subtotal = (item.quantity ?? 0) * (item.unitPrice ?? 0)
      const formatted = new Intl.NumberFormat('fr-FR', {
        style: 'currency',
        currency: 'EUR',
      }).format(subtotal)
      return (
        <span className="text-[12px] font-semibold text-slate-700">
          {formatted}
        </span>
      )
    },
  },
  {
    key: 'discount',
    label: 'Réduction',
    sortable: false,
    className: 'hidden lg:table-cell',
    render: (item) => (
      <span className="text-[12px] text-slate-600 flex items-center gap-1">
        <Percent size={12} className="text-slate-400" />
        {item.discount ?? 0}%
      </span>
    ),
  },
]

// ─── Champs du formulaire ─────────────────────────────────────────────────────
export const getOrderItemFormFields = () => [
  {
    name: 'articleId',
    label: 'Article',
    type: 'select',
    placeholder: 'Sélectionner un article',
    icon: 'shirt',
    required: true,
  },
  {
    name: 'quantity',
    label: 'Quantité',
    type: 'number',
    placeholder: '1',
    icon: 'hash',
    required: true,
    min: '1',
    step: '1',
  },
  {
    name: 'unitPrice',
    label: 'Prix unitaire (€)',
    type: 'number',
    placeholder: '0.00',
    icon: 'coins',
    required: true,
    min: '0',
    step: '0.01',
  },
  {
    name: 'discount',
    label: 'Réduction (%)',
    type: 'number',
    placeholder: '0',
    icon: 'percent',
    required: false,
    min: '0',
    max: '100',
    step: '1',
  },
]
