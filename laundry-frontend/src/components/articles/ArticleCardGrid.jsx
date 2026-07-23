import { useState, useMemo } from 'react'
import {
  Search, Pencil, Trash2, Eye, Hash, Palette, Plus,
  ChevronLeft, ChevronRight,
} from 'lucide-react'
import Button from '../common/Button'
import ActionButton from '../common/ActionButton'
import {
  CLOTHING_TYPE_LABELS,
  CLOTHING_SIZE_LABELS,
  FABRIC_TYPE_LABELS,
} from '../../utils/constants'
import { getArticleAvatarGradient } from '../../types/ArticlesType'

const PAGE_SIZE = 12

const ARTICLE_STATUS_BADGE = {
  PENDING:       'bg-yellow-50 text-yellow-600 ring-yellow-100',
  WASHING:       'bg-blue-50 text-blue-600 ring-blue-100',
  IRONING:       'bg-purple-50 text-purple-600 ring-purple-100',
  QUALITY_CHECK: 'bg-orange-50 text-orange-600 ring-orange-100',
  PACKED:        'bg-teal-50 text-teal-600 ring-teal-100',
  COMPLETED:     'bg-green-50 text-green-600 ring-green-100',
}

const ARTICLE_STATUS_LABELS = {
  PENDING:       'En attente',
  WASHING:       'Lavage',
  IRONING:       'Repassage',
  QUALITY_CHECK: 'Contrôle qualité',
  PACKED:        'Emballé',
  COMPLETED:     'Terminé',
}

const SERVICE_LABELS = {
  DRY_CLEAN:     'Nettoyage à sec',
  WASH:          'Lavage',
  IRON:          'Repassage',
  STAIN_REMOVAL: 'Détachage',
  DEYING:        'Teinture',
  ALTERATION:    'Retouche',
}

const formatPrice = (amount) =>
  new Intl.NumberFormat('fr-FR', {
    style: 'currency',
    currency: 'XAF',
    maximumFractionDigits: 0,
  }).format(amount ?? 0)

const SEARCH_FIELDS = ['id', 'orderId', 'clothingType', 'sizeName', 'fabric', 'status', 'distinction']

const ArticleCardGrid = ({
  articles = [],
  loading = false,
  onAdd,
  onEdit,
  onDelete,
  onView,
  canAdd    = true,
  canEdit   = true,
  canDelete = true,
  title     = 'Articles',
}) => {
  const [search, setSearch] = useState('')
  const [page,   setPage]   = useState(1)

  const filtered = useMemo(() => {
    const q = search.toLowerCase().trim()
    if (!q) return articles
    return articles.filter((a) =>
      SEARCH_FIELDS.some((f) => String(a[f] ?? '').toLowerCase().includes(q))
    )
  }, [articles, search])

  const totalPages = Math.max(1, Math.ceil(filtered.length / PAGE_SIZE))
  const paginated   = filtered.slice((page - 1) * PAGE_SIZE, page * PAGE_SIZE)

  const handleSearchChange = (e) => { setSearch(e.target.value); setPage(1) }
  const hasActions = onEdit || onDelete || onView

  return (
    <div className="bg-white rounded-2xl border border-slate-100 shadow-sm overflow-hidden">

      {/* ── En-tête : titre + bouton Ajouter ─────────────────────────── */}
      <div className="flex items-center justify-between px-5 py-4 border-b border-slate-50">
        <h2 className="text-sm font-bold text-slate-800">{title}</h2>
        {canAdd && (
          <Button icon={Plus} size="md" onClick={onAdd}>
            Ajouter
          </Button>
        )}
      </div>

      {/* ── Barre de recherche ───────────────────────────────────────── */}
      <div className="px-5 py-3 border-b border-slate-50">
        <div className="relative w-full sm:max-w-xs">
          <Search size={14} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400 pointer-events-none" />
          <input
            type="text"
            value={search}
            onChange={handleSearchChange}
            placeholder="Rechercher un article…"
            className="w-full pl-8 pr-3 py-2 text-sm border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-100 focus:border-blue-400 bg-slate-50 focus:bg-white transition-colors placeholder:text-slate-300"
          />
        </div>
        <p className="text-[11px] text-slate-400 mt-1.5">
          {loading ? '…' : (
            <>
              <span className="font-semibold text-slate-600">{filtered.length}</span>
              {' '}{filtered.length !== 1 ? 'résultats' : 'résultat'}
              {search && <span> pour « {search} »</span>}
            </>
          )}
        </p>
      </div>

      {/* ── Grille de cartes ─────────────────────────────────────────── */}
      <div className="p-5">
        {loading ? (
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
            {Array.from({ length: 8 }).map((_, i) => (
              <div key={i} className="rounded-2xl border border-slate-100 p-4 animate-pulse space-y-3">
                <div className="flex items-center gap-3">
                  <div className="w-10 h-10 rounded-xl bg-slate-100 flex-shrink-0" />
                  <div className="flex-1 space-y-1.5">
                    <div className="h-3 bg-slate-100 rounded w-2/3" />
                    <div className="h-2.5 bg-slate-100 rounded w-1/3" />
                  </div>
                </div>
                <div className="h-2.5 bg-slate-100 rounded w-full" />
                <div className="h-2.5 bg-slate-100 rounded w-5/6" />
              </div>
            ))}
          </div>
        ) : paginated.length === 0 ? (
          <div className="py-14 text-center">
            <Search size={28} className="text-slate-200 mx-auto mb-2.5" />
            <p className="text-sm font-medium text-slate-400">
              {search ? `Aucun résultat pour « ${search} »` : 'Aucun article enregistré.'}
            </p>
            {search && (
              <button
                onClick={() => setSearch('')}
                className="mt-2 text-xs text-blue-500 hover:underline"
              >
                Effacer la recherche
              </button>
            )}
          </div>
        ) : (
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
            {paginated.map((article) => (
              <ArticleCard
                key={article.id}
                article={article}
                onEdit={canEdit ? onEdit : null}
                onDelete={canDelete ? onDelete : null}
                onView={onView}
                hasActions={hasActions}
              />
            ))}
          </div>
        )}
      </div>

      {/* ── Pagination ───────────────────────────────────────────────── */}
      {!loading && filtered.length > PAGE_SIZE && (
        <div className="flex items-center justify-between px-5 py-3 border-t border-slate-50 bg-slate-50/40">
          <span className="text-[11px] text-slate-400">
            Page <span className="font-semibold text-slate-600">{page}</span> sur {totalPages}
            <span className="mx-2 text-slate-300">·</span>
            {filtered.length} résultats
          </span>
          <div className="flex items-center gap-1">
            <button
              onClick={() => setPage((p) => Math.max(1, p - 1))}
              disabled={page === 1}
              className="p-1.5 rounded-lg border border-slate-200 text-slate-500 hover:bg-white hover:border-slate-300 disabled:opacity-30 disabled:cursor-not-allowed transition-colors"
            >
              <ChevronLeft size={14} />
            </button>
            {buildPageList(page, totalPages).map((p, idx) =>
              p === '…' ? (
                <span key={`e-${idx}`} className="w-7 text-center text-slate-400 text-xs">…</span>
              ) : (
                <button
                  key={p}
                  onClick={() => setPage(p)}
                  className={`w-7 h-7 rounded-lg text-xs font-semibold transition-colors ${
                    page === p
                      ? 'bg-blue-600 text-white shadow-sm'
                      : 'text-slate-500 hover:bg-white border border-slate-200 hover:border-slate-300'
                  }`}
                >
                  {p}
                </button>
              )
            )}
            <button
              onClick={() => setPage((p) => Math.min(totalPages, p + 1))}
              disabled={page === totalPages}
              className="p-1.5 rounded-lg border border-slate-200 text-slate-500 hover:bg-white hover:border-slate-300 disabled:opacity-30 disabled:cursor-not-allowed transition-colors"
            >
              <ChevronRight size={14} />
            </button>
          </div>
        </div>
      )}
    </div>
  )
}

// ─── Carte individuelle ──────────────────────────────────────────────────────
const ArticleCard = ({ article, onEdit, onDelete, onView, hasActions }) => {
  const statusCls   = ARTICLE_STATUS_BADGE[article.status] || 'bg-slate-100 text-slate-500 ring-slate-200'
  const statusLabel = ARTICLE_STATUS_LABELS[article.status] || article.status || '—'
  const gradient     = getArticleAvatarGradient(article.clothingType)
  const initials     = article?.clothingType?.[0]?.toUpperCase() || '?'
  const services      = article.services ?? []
  const color          = article.color ? (article.color.startsWith('#') ? article.color : `#${article.color}`) : null

  return (
    <div className="group relative rounded-2xl border border-slate-100 hover:border-slate-200 hover:shadow-md transition-all p-4 flex flex-col gap-3 bg-white">

      {/* En-tête carte : avatar + type + commande */}
      <div className="flex items-start justify-between gap-2">
        <div className="flex items-center gap-3 min-w-0">
          <div className={`w-10 h-10 rounded-xl bg-gradient-to-br ${gradient} flex items-center justify-center text-white text-sm font-bold flex-shrink-0`}>
            {initials}
          </div>
          <div className="min-w-0">
            <p className="text-sm font-bold text-slate-800 truncate">
              {CLOTHING_TYPE_LABELS[article.clothingType] || article.clothingType || '—'}
            </p>
            <p className="text-[11px] text-slate-400 flex items-center gap-1">
              <Hash size={10} />
              {article.id}
              <span className="mx-1 text-slate-200">·</span>
              Commande #{article.orderId ?? '—'}
            </p>
          </div>
        </div>
        <span className={`flex-shrink-0 inline-flex items-center px-2.5 py-1 rounded-full text-[10px] font-semibold ring-1 ${statusCls}`}>
          {statusLabel}
        </span>
      </div>

      {/* Détails : taille / tissu / couleur */}
      <div className="grid grid-cols-2 gap-y-1.5 gap-x-2 text-[12px]">
        <div>
          <span className="text-slate-400">Taille</span>
          <p className="text-slate-700 font-medium">
            {CLOTHING_SIZE_LABELS[article.size] || article.sizeName || article.size || '—'}
          </p>
        </div>
        <div>
          <span className="text-slate-400">Tissu</span>
          <p className="text-slate-700 font-medium">
            {FABRIC_TYPE_LABELS[article.fabric] || article.fabric || '—'}
          </p>
        </div>
        <div className="col-span-2 flex items-center gap-1.5">
          <span className="text-slate-400">Couleur</span>
          {color ? (
            <span className="inline-flex items-center gap-1.5">
              <span className="w-3 h-3 rounded-full ring-1 ring-slate-200" style={{ backgroundColor: color }} />
              <span className="text-slate-700 font-medium">{color}</span>
            </span>
          ) : (
            <span className="text-slate-300">
              <Palette size={12} className="inline mr-1" />—
            </span>
          )}
        </div>
        {article.distinction && (
          <div className="col-span-2">
            <span className="text-slate-400">Distinction</span>
            <p className="text-slate-600 truncate">{article.distinction}</p>
          </div>
        )}
      </div>

      {/* Services */}
      {services.length > 0 && (
        <div className="flex flex-wrap gap-1">
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
      )}

      {/* Pied de carte : prix + date + actions CRUD */}
      <div className="flex items-center justify-between pt-2 border-t border-slate-50 mt-auto">
        <div>
          <p className="text-sm font-bold text-slate-800">{formatPrice(article.totalPrice)}</p>
          {article.createdAt && (
            <p className="text-[10px] text-slate-400">
              {new Date(article.createdAt).toLocaleDateString('fr-FR')}
            </p>
          )}
        </div>

        {hasActions && (
          <div className="flex items-center gap-1">
            {onView && (
              <ActionButton icon={Eye} variant="view" label="Voir" onClick={() => onView(article)} />
            )}
            {onEdit && (
              <ActionButton icon={Pencil} variant="edit" label="Modifier" onClick={() => onEdit(article)} />
            )}
            {onDelete && (
              <ActionButton icon={Trash2} variant="delete" label="Supprimer" onClick={() => onDelete(article)} />
            )}
          </div>
        )}
      </div>
    </div>
  )
}

const buildPageList = (current, total) =>
  Array.from({ length: total }, (_, i) => i + 1)
    .filter((p) => p === 1 || p === total || Math.abs(p - current) <= 1)
    .reduce((acc, p, idx, arr) => {
      if (idx > 0 && arr[idx - 1] !== p - 1) acc.push('…')
      acc.push(p)
      return acc
    }, [])

export default ArticleCardGrid
