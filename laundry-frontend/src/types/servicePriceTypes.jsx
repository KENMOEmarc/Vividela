import { Shirt, Sparkles, Coins } from 'lucide-react'
import {
  CLOTHING_TYPES,
  CLOTHING_TYPE_LABELS,
  SERVICE_TYPES,
  SERVICE_TYPE_LABELS,
} from '../utils/constants'
import { formatCurrencyXAF, formatDateFR } from '../utils/format'
import { getClothingEmoji } from '../utils/clothingIcons'

// ─── Badge type de vêtement ────────────────────────────────────────────────────
const CLOTHING_BADGE = 'bg-blue-50 text-blue-600 ring-blue-100'

// ─── Badge type de service ─────────────────────────────────────────────────────
export const SERVICE_TYPE_BADGE = {
  [SERVICE_TYPES.DRY_CLEAN]:     'bg-violet-50 text-violet-600 ring-violet-100',
  [SERVICE_TYPES.WASH]:          'bg-sky-50 text-sky-600 ring-sky-100',
  [SERVICE_TYPES.IRON]:          'bg-amber-50 text-amber-600 ring-amber-100',
  [SERVICE_TYPES.STAIN_REMOVAL]: 'bg-rose-50 text-rose-600 ring-rose-100',
  [SERVICE_TYPES.DEYING]:        'bg-fuchsia-50 text-fuchsia-600 ring-fuchsia-100',
  [SERVICE_TYPES.ALTERATION]:    'bg-emerald-50 text-emerald-600 ring-emerald-100',
}

// ─── Colonnes du tableau ────────────────────────────────────────────────────────
export const SERVICE_PRICE_TABLE_COLUMNS = [
  {
    key: 'id',
    label: 'ID',
    sortable: true,
    className: 'w-16',
    render: (sp) => (
      <span className="font-mono text-[11px] text-slate-400">#{sp.id}</span>
    ),
  },
  {
    key: 'clothingType',
    label: 'Type de vêtement',
    sortable: true,
    render: (sp) => (
      <span className={`inline-flex items-center px-2.5 py-1 rounded-full text-[10px] font-semibold ring-1 ${CLOTHING_BADGE}`}>
        <span className="mr-2 text-sm">{getClothingEmoji(sp.clothingType)}</span>
        {CLOTHING_TYPE_LABELS[sp.clothingType] ?? sp.clothingType}
      </span>
    ),
  },
  {
    key: 'service',
    label: 'Service',
    sortable: true,
    render: (sp) => {
      const cls = SERVICE_TYPE_BADGE[sp.service] ?? 'bg-slate-100 text-slate-500 ring-slate-200'
      return (
        <span className={`inline-flex items-center px-2.5 py-1 rounded-full text-[10px] font-semibold ring-1 ${cls}`}>
          {SERVICE_TYPE_LABELS[sp.service] ?? sp.service}
        </span>
      )
    },
  },
  {
    key: 'price',
    label: 'Prix',
    sortable: true,
    className: 'hidden sm:table-cell',
    render: (sp) => (
      <span className="text-[12px] text-slate-700 font-semibold">
        {formatCurrencyXAF(sp.price)}
      </span>
    ),
  },
  {
    key: 'active',
    label: 'Statut',
    sortable: true,
    className: 'hidden md:table-cell',
    render: (sp) => (
      <span
        className={`inline-flex items-center px-2.5 py-1 rounded-full text-[10px] font-semibold ring-1 ${
          sp.active
            ? 'bg-emerald-50 text-emerald-600 ring-emerald-100'
            : 'bg-slate-100 text-slate-500 ring-slate-200'
        }`}
      >
        {sp.active ? 'Actif' : 'Inactif'}
      </span>
    ),
  },
  {
    key: 'createdAt',
    label: 'Créé le',
    sortable: true,
    className: 'hidden xl:table-cell',
    render: (sp) => (
      <span className="text-[11px] text-slate-400">{formatDateFR(sp.createdAt)}</span>
    ),
  },
]

// ─── Champs du formulaire ───────────────────────────────────────────────────────
// En édition, le couple (type de vêtement, type de service) est la clé du tarif :
// il ne peut pas être modifié (voir ServicePriceUpdateRequest côté backend, qui
// n'accepte que le prix et le statut actif). Ces deux champs ne sont donc
// proposés qu'à la création ; en édition ils restent affichés en lecture seule
// dans l'en-tête de la modale (voir ServicePriceFormModal).
export const getServicePriceFormFields = (mode = 'create') => {
  const priceAndStatus = [
    {
      key: 'price',
      label: 'Prix (XAF)',
      type: 'number',
      required: true,
      placeholder: '0',
      step: '1',
      min: '0',
      prefixIcon: Coins,
      hint: 'Prix appliqué pour ce vêtement + ce service',
    },
    {
      key: 'active',
      label: 'Tarif actif',
      type: 'toggle',
      hint: 'Un tarif inactif ne peut pas être appliqué à une nouvelle commande',
      defaultValue: true,
    },
  ]

  if (mode === 'edit') return priceAndStatus

  return [
    {
      key: 'clothingType',
      label: 'Type de vêtement',
      type: 'select',
      required: true,
      prefixIcon: Shirt,
      options: Object.values(CLOTHING_TYPES).map((value) => ({
        value,
        label: CLOTHING_TYPE_LABELS[value] ?? value,
      })),
    },
    {
      key: 'service',
      label: 'Type de service',
      type: 'select',
      required: true,
      prefixIcon: Sparkles,
      options: Object.values(SERVICE_TYPES).map((value) => ({
        value,
        label: SERVICE_TYPE_LABELS[value] ?? value,
      })),
    },
    ...priceAndStatus,
  ]
}

// ─── Aperçu suppression ──────────────────────────────────────────────────────────
export const renderServicePriceDeletePreview = (sp) => (
  <div className="flex items-center gap-3 p-3.5 bg-slate-50 border border-slate-100 rounded-xl">
    <div className="w-10 h-10 rounded-full bg-gradient-to-br from-indigo-400 to-indigo-600 text-white flex items-center justify-center flex-shrink-0 shadow-sm">
      <span className="text-lg">{getClothingEmoji(sp?.clothingType)}</span>
    </div>
    <div className="min-w-0">
      <p className="font-semibold text-slate-900 text-sm truncate">
        {CLOTHING_TYPE_LABELS[sp?.clothingType] ?? sp?.clothingType} · {SERVICE_TYPE_LABELS[sp?.service] ?? sp?.service}
      </p>
      <p className="text-[11px] text-slate-500 truncate">
        Prix : {formatCurrencyXAF(sp?.price)}
      </p>
    </div>
  </div>
)
