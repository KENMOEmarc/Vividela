import { Tag, TriangleAlert, Ruler } from 'lucide-react'
import { MEASUREMENT_UNITS, MEASUREMENT_UNIT_LABELS } from '../utils/constants'

// ─── Avatar ──────────────────────────────────────────────────────────────────
const PRODUCT_AVATAR_PALETTE = [
  'from-teal-400 to-teal-600',
  'from-sky-400 to-sky-600',
  'from-fuchsia-400 to-fuchsia-600',
  'from-amber-400 to-amber-600',
  'from-lime-400 to-lime-600',
  'from-rose-400 to-rose-600',
  'from-indigo-400 to-indigo-600',
  'from-emerald-400 to-emerald-600',
]

export const getProductInitials = (product) =>
  product?.name?.[0]?.toUpperCase() || '?'

export const getProductAvatarGradient = (name = '') =>
  PRODUCT_AVATAR_PALETTE[(name?.charCodeAt(0) ?? 0) % PRODUCT_AVATAR_PALETTE.length]

// ─── Badge unité de mesure ──────────────────────────────────────────────────
export const MEASUREMENT_UNIT_BADGE = {
  [MEASUREMENT_UNITS.KILOGRAM]:   'bg-amber-50 text-amber-600 ring-amber-100',
  [MEASUREMENT_UNITS.LITER]:      'bg-blue-50 text-blue-600 ring-blue-100',
  [MEASUREMENT_UNITS.MILLILITER]: 'bg-cyan-50 text-cyan-600 ring-cyan-100',
  [MEASUREMENT_UNITS.UNIT]:       'bg-violet-50 text-violet-600 ring-violet-100',
  [MEASUREMENT_UNITS.PACKET]:     'bg-orange-50 text-orange-600 ring-orange-100',
}

// ─── Colonnes du tableau ──────────────────────────────────────────────────────
export const PRODUCT_TABLE_COLUMNS = [
  {
    key: 'id',
    label: 'ID',
    sortable: true,
    className: 'w-16',
    render: (product) => (
      <span className="font-mono text-[11px] text-slate-400">#{product.id}</span>
    ),
  },
  {
    key: 'name',
    label: 'Produit',
    sortable: true,
    render: (product) => (
      <div className="flex items-center gap-2.5">
        <div
          className={`w-9 h-9 rounded-full bg-gradient-to-br ${getProductAvatarGradient(product.name)} text-white font-bold text-[11px] flex items-center justify-center flex-shrink-0 shadow-sm`}
        >
          {getProductInitials(product)}
        </div>
        <div className="min-w-0">
          <p className="font-semibold text-slate-800 leading-tight truncate">
            {product.name}
          </p>
        </div>
      </div>
    ),
  },
  {
    key: 'thresholdValue',
    label: 'Seuil',
    sortable: true,
    className: 'hidden sm:table-cell',
    render: (product) => {
      const formatted = new Intl.NumberFormat('fr-FR', {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2,
      }).format(product.thresholdValue ?? 0)
      return (
        <span className="text-[12px] text-slate-700 font-medium">
          {formatted} {MEASUREMENT_UNIT_LABELS[product.measurementUnit] ?? product.measurementUnit}
        </span>
      )
    },
  },
  {
    key: 'measurementUnit',
    label: 'Unité',
    sortable: true,
    className: 'hidden md:table-cell',
    render: (product) => {
      const cls = MEASUREMENT_UNIT_BADGE[product.measurementUnit] ?? 'bg-slate-100 text-slate-500 ring-slate-200'
      return (
        <span className={`inline-flex items-center px-2.5 py-1 rounded-full text-[10px] font-semibold ring-1 ${cls}`}>
          {MEASUREMENT_UNIT_LABELS[product.measurementUnit] ?? product.measurementUnit ?? '—'}
        </span>
      )
    },
  },
  {
    key: 'createdAt',
    label: 'Créé le',
    sortable: true,
    className: 'hidden xl:table-cell',
    render: (product) => {
      if (!product.createdAt) return <span className="text-slate-400 text-[11px]">—</span>
      return (
        <span className="text-[11px] text-slate-400">
          {new Date(product.createdAt).toLocaleDateString('fr-FR', {
            day: '2-digit',
            month: '2-digit',
            year: 'numeric',
          })}
        </span>
      )
    },
  },
  {
    key: 'updatedAt',
    label: 'Modifié le',
    sortable: true,
    className: 'hidden xl:table-cell',
    render: (product) => {
      if (!product.updatedAt) return <span className="text-slate-400 text-[11px]">—</span>
      return (
        <span className="text-[11px] text-slate-400">
          {new Date(product.updatedAt).toLocaleDateString('fr-FR', {
            day: '2-digit',
            month: '2-digit',
            year: 'numeric',
          })}
        </span>
      )
    },
  },
]

// ─── Champs du formulaire ─────────────────────────────────────────────────────
export const getProductFormFields = (mode = 'create') => [
  {
    key: 'name',
    label: 'Nom du produit',
    type: 'text',
    required: true,
    placeholder: 'Nom du produit',
    prefixIcon: Tag,
  },
  {
    key: 'thresholdValue',
    label: 'Valeur seuil',
    type: 'number',
    required: true,
    placeholder: '0.00',
    step: '0.01',
    min: '0',
    prefixIcon: TriangleAlert,
    hint: 'Quantité minimale avant alerte de stock',
  },
  {
    key: 'measurementUnit',
    label: 'Unité de mesure',
    type: 'select',
    required: true,
    prefixIcon: Ruler,
    options: Object.entries(MEASUREMENT_UNITS).map(([key, value]) => ({
      value,
      label: MEASUREMENT_UNIT_LABELS[value] ?? value,
    })),
  },
]

// ─── Aperçu suppression ───────────────────────────────────────────────────────
export const renderProductDeletePreview = (product) => (
  <div className="flex items-center gap-3 p-3.5 bg-slate-50 border border-slate-100 rounded-xl">
    <div
      className={`w-10 h-10 rounded-full bg-gradient-to-br ${getProductAvatarGradient(product?.name)} text-white font-bold text-sm flex items-center justify-center flex-shrink-0 shadow-sm`}
    >
      {getProductInitials(product)}
    </div>
    <div className="min-w-0">
      <p className="font-semibold text-slate-900 text-sm truncate">
        {product?.name}
      </p>
      <p className="text-[11px] text-slate-500 truncate">
        Seuil : {new Intl.NumberFormat('fr-FR', { minimumFractionDigits: 2 }).format(product?.thresholdValue ?? 0)}{' '}
        {MEASUREMENT_UNIT_LABELS[product?.measurementUnit] ?? product?.measurementUnit}
      </p>
    </div>
  </div>
)