import {
  CLOTHING_TYPES,
  CLOTHING_TYPE_LABELS,
  CLOTHING_SIZES,
  CLOTHING_SIZE_LABELS,
  FABRIC_TYPES,
  FABRIC_TYPE_LABELS,
} from '../utils/constants'

// ─── Avatar ──────────────────────────────────────────────────────────────────
const ARTICLE_AVATAR_PALETTE = [
  'from-indigo-400 to-indigo-600',
  'from-purple-400 to-purple-600',
  'from-pink-400 to-pink-600',
  'from-red-400 to-red-600',
  'from-orange-400 to-orange-600',
  'from-yellow-400 to-yellow-600',
  'from-green-400 to-green-600',
  'from-teal-400 to-teal-600',
]

export const getArticleInitials = (article) =>
  article?.clothingType?.[0]?.toUpperCase() || '?'

export const getArticleAvatarGradient = (clothingType = '') =>
  ARTICLE_AVATAR_PALETTE[(clothingType?.charCodeAt(0) ?? 0) % ARTICLE_AVATAR_PALETTE.length]

// ─── Statuts article — alignés sur ArticleStatus.java ────────────────────────
// CORRECTION : remplacement des statuts blog (DRAFT/PUBLISHED/ARCHIVED)
//              par les vrais statuts pressing définis dans ArticleStatus.java
export const ARTICLE_STATUS_BADGE = {
  PENDING:       'bg-yellow-50 text-yellow-600 ring-yellow-100',
  WASHING:       'bg-blue-50 text-blue-600 ring-blue-100',
  IRONING:       'bg-purple-50 text-purple-600 ring-purple-100',
  QUALITY_CHECK: 'bg-orange-50 text-orange-600 ring-orange-100',
  PACKED:        'bg-teal-50 text-teal-600 ring-teal-100',
  COMPLETED:     'bg-green-50 text-green-600 ring-green-100',
}

export const ARTICLE_STATUS_LABELS = {
  PENDING:       'En attente',
  WASHING:       'Lavage',
  IRONING:       'Repassage',
  QUALITY_CHECK: 'Contrôle qualité',
  PACKED:        'Emballé',
  COMPLETED:     'Terminé',
}

export const SERVICE_LABELS = {
  DRY_CLEAN:     'Nettoyage à sec',
  WASH:          'Lavage',
  IRON:          'Repassage',
  STAIN_REMOVAL: 'Détachage',
  DEYING:        'Teinture',
  ALTERATION:    'Retouche',
}

// ─── Colonnes du tableau ──────────────────────────────────────────────────────
export const ARTICLE_TABLE_COLUMNS = [
  {
    key: 'id',
    label: 'ID',
    sortable: true,
    className: 'w-16',
    render: (article) => (
      <span className="font-mono text-[11px] text-slate-400">#{article.id}</span>
    ),
  },
  {
    key: 'orderId',
    label: 'Commande',
    sortable: true,
    className: 'hidden sm:table-cell',
    render: (article) => (
      <span className="text-[12px] font-medium text-slate-600">#{article.orderId ?? '—'}</span>
    ),
  },
  {
    key: 'clothingType',
    label: 'Type de vêtement',
    sortable: true,
    render: (article) => (
      <span className="text-[12px] text-slate-700">
        {CLOTHING_TYPE_LABELS[article.clothingType] || article.clothingType || '—'}
      </span>
    ),
  },
  {
    key: 'size',
    label: 'Taille',
    sortable: true,
    className: 'hidden sm:table-cell',
    render: (article) => (
      <span className="text-[12px] text-slate-500">
        {CLOTHING_SIZE_LABELS[article.size] || article.sizeName || article.size || '—'}
      </span>
    ),
  },
  {
    key: 'fabric',
    label: 'Tissu',
    sortable: true,
    className: 'hidden md:table-cell',
    render: (article) => (
      <span className="text-[12px] text-slate-500">
        {FABRIC_TYPE_LABELS[article.fabric] || article.fabric || '—'}
      </span>
    ),
  },
  {
    key: 'color',
    label: 'Couleur',
    sortable: false,
    className: 'hidden lg:table-cell',
    render: (article) => {
      const color = article.color
      if (!color) return <span className="text-[12px] text-slate-400">—</span>
      const hex = color.startsWith('#') ? color : `#${color}`
      return (
        <div className="flex items-center gap-2">
          <span
            className="w-3 h-3 rounded-full ring-1 ring-slate-200"
            style={{ backgroundColor: hex }}
          />
          <span className="text-[12px] text-slate-500">{hex}</span>
        </div>
      )
    },
  },
  {
    key: 'distinction',
    label: 'Distinction',
    sortable: false,
    className: 'hidden xl:table-cell',
    render: (article) => (
      <span className="text-[12px] text-slate-500 truncate">{article.distinction || '—'}</span>
    ),
  },
  {
    key: 'services',
    label: 'Services',
    sortable: false,
    className: 'hidden md:table-cell',
    render: (article) => {
      const services = article.services ?? []
      if (!services.length) return <span className="text-[12px] text-slate-400">—</span>
      return (
        <div className="flex flex-wrap gap-1 max-w-[180px]">
          {services.map((s) => (
            <span
              key={s.service}
              className="inline-flex items-center px-2 py-0.5 rounded-full text-[10px] font-medium bg-slate-100 text-slate-600"
              title={`${SERVICE_LABELS[s.service] || s.service} — ${s.appliedPrice ?? 0} FCFA`}
            >
              {SERVICE_LABELS[s.service] || s.service}
            </span>
          ))}
        </div>
      )
    },
  },
  {
    key: 'totalPrice',
    label: 'Prix',
    sortable: true,
    className: 'hidden sm:table-cell',
    render: (article) => (
      <span className="text-[12px] font-semibold text-slate-700">
        {new Intl.NumberFormat('fr-FR', {
          style: 'currency',
          currency: 'XAF',
          maximumFractionDigits: 0,
        }).format(article.totalPrice ?? 0)}
      </span>
    ),
  },
  {
    key: 'status',
    label: 'Statut',
    sortable: true,
    render: (article) => {
      // CORRECTION : utilisation des vrais statuts ArticleStatus.java
      const cls = ARTICLE_STATUS_BADGE[article.status] || 'bg-slate-100 text-slate-500 ring-slate-200'
      return (
        <span className={`inline-flex items-center px-2.5 py-1 rounded-full text-[10px] font-semibold ring-1 ${cls}`}>
          {ARTICLE_STATUS_LABELS[article.status] || article.status}
        </span>
      )
    },
  },
  {
    key: 'createdAt',
    label: 'Créé le',
    sortable: true,
    className: 'hidden xl:table-cell',
    render: (article) => {
      if (!article.createdAt) return <span className="text-slate-400 text-[11px]">—</span>
      return (
        <span className="text-[11px] text-slate-400">
          {new Date(article.createdAt).toLocaleDateString('fr-FR')}
        </span>
      )
    },
  },
]

// ─── Champs du formulaire ─────────────────────────────────────────────────────
// CORRECTION : options construites depuis les constantes synchronisées avec Java,
//              statuts pressing à la place des statuts blog
export const getArticleFormFields = (mode = 'create') => [
  {
    key: 'clothingType',
    label: 'Type de vêtement',
    type: 'select',
    options: Object.entries(CLOTHING_TYPES).map(([key]) => ({
      value: key,
      label: CLOTHING_TYPE_LABELS[key],
    })),
    icon: 'tag',
    required: true,
  },
  {
    key: 'size',
    label: 'Taille',
    type: 'select',
    options: Object.entries(CLOTHING_SIZES).map(([key]) => ({
      value: key,
      label: CLOTHING_SIZE_LABELS[key],
    })),
    icon: 'box',
    required: true,
  },
  {
    key: 'fabric',
    label: 'Type de tissu',
    type: 'select',
    options: Object.entries(FABRIC_TYPES).map(([key]) => ({
      value: key,
      label: FABRIC_TYPE_LABELS[key],
    })),
    icon: 'layers',
    required: true,
  },
  {
    key: 'color',
    label: 'Couleur',
    type: 'color',
    icon: 'palette',
    required: false,
    defaultValue: '#000000',
  },
  {
    key: 'distinction',
    label: 'Distinction',
    type: 'text',
    placeholder: 'Ex: Tache à droite, bouton manquant…',
    icon: 'tag',
    required: false,
  },
  {
    key: 'services',
    label: 'Services (détermine le prix)',
    type: 'checkbox-group',
    options: [
      { value: 'DRY_CLEAN',     label: 'Nettoyage à sec' },
      { value: 'WASH',          label: 'Lavage' },
      { value: 'IRON',          label: 'Repassage' },
      { value: 'STAIN_REMOVAL', label: 'Détachage' },
      { value: 'DEYING',        label: 'Teinture' },
      { value: 'ALTERATION',    label: 'Retouche' },
    ],
    defaultValue: [],
    icon: 'sparkles',
    required: true,
    hint: 'Le prix est calculé à partir du tarif de chaque service pour ce vêtement',
  },
  {
    key: 'status',
    label: 'Statut',
    type: 'select',
    // CORRECTION : statuts pressing réels (ArticleStatus.java)
    options: [
      { value: 'PENDING',       label: 'En attente' },
      { value: 'WASHING',       label: 'Lavage' },
      { value: 'IRONING',       label: 'Repassage' },
      { value: 'QUALITY_CHECK', label: 'Contrôle qualité' },
      { value: 'PACKED',        label: 'Emballé' },
      { value: 'COMPLETED',     label: 'Terminé' },
    ],
    icon: 'check-circle',
    required: true,
  },
]