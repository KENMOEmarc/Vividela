import { useMemo } from 'react'
import { ArrowRight, Coins } from 'lucide-react'
import { CLOTHING_TYPES, CLOTHING_TYPE_LABELS } from '../../utils/constants'
import { getClothingEmoji } from '../../utils/clothingIcons'
import { formatCurrencyXAF } from '../../utils/format'

// ─── Palette d'avatars par catégorie (même esprit que getArticleAvatarGradient) ──
const CATEGORY_AVATAR_PALETTE = [
  'from-indigo-400 to-indigo-600',
  'from-purple-400 to-purple-600',
  'from-pink-400 to-pink-600',
  'from-red-400 to-red-600',
  'from-orange-400 to-orange-600',
  'from-amber-400 to-amber-600',
  'from-emerald-400 to-emerald-600',
  'from-teal-400 to-teal-600',
  'from-sky-400 to-sky-600',
  'from-blue-400 to-blue-600',
  'from-violet-400 to-violet-600',
  'from-fuchsia-400 to-fuchsia-600',
]

const getCategoryGradient = (clothingType = '') =>
  CATEGORY_AVATAR_PALETTE[(clothingType?.charCodeAt(0) ?? 0) % CATEGORY_AVATAR_PALETTE.length]

/**
 * Grille de cartes — une carte par catégorie de vêtement (ClothingType.java).
 * Chaque carte résume les tarifs de service définis pour cette catégorie et
 * propose un lien pour ouvrir le détail (tableau des services de la catégorie).
 */
const ClothingCategoryCards = ({ servicePrices = [], loading = false, onViewCategory }) => {
  const categories = useMemo(() => {
    return Object.values(CLOTHING_TYPES).map((clothingType) => {
      const prices = servicePrices.filter((sp) => sp.clothingType === clothingType)
      const activeCount = prices.filter((sp) => sp.active).length
      const amounts = prices.map((sp) => Number(sp.price ?? 0))
      const min = amounts.length ? Math.min(...amounts) : null
      const max = amounts.length ? Math.max(...amounts) : null

      return {
        clothingType,
        label: CLOTHING_TYPE_LABELS[clothingType] ?? clothingType,
        count: prices.length,
        activeCount,
        min,
        max,
      }
    })
  }, [servicePrices])

  return (
    <div className="bg-white rounded-2xl border border-slate-100 shadow-sm overflow-hidden">
      <div className="flex items-center justify-between px-5 py-4 border-b border-slate-50">
        <h2 className="text-sm font-bold text-slate-800">Tarifs par catégorie de vêtement</h2>
      </div>

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
              </div>
            ))}
          </div>
        ) : (
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
            {categories.map((cat) => (
              <CategoryCard key={cat.clothingType} category={cat} onView={onViewCategory} />
            ))}
          </div>
        )}
      </div>
    </div>
  )
}

const CategoryCard = ({ category, onView }) => {
  const { clothingType, label, count, activeCount, min, max } = category
  const gradient = getCategoryGradient(clothingType)
  const hasPrices = count > 0

  return (
    <button
      type="button"
      onClick={() => onView(clothingType)}
      className="group text-left rounded-2xl border border-slate-100 hover:border-slate-200 hover:shadow-md transition-all p-4 flex flex-col gap-3 bg-white"
    >
      {/* En-tête : avatar + nom de la catégorie */}
      <div className="flex items-center gap-3 min-w-0">
        <div className={`w-10 h-10 rounded-xl bg-gradient-to-br ${gradient} flex items-center justify-center text-white flex-shrink-0`}>
          <span className="text-lg">{getClothingEmoji(clothingType)}</span>
        </div>
        <div className="min-w-0">
          <p className="text-sm font-bold text-slate-800 truncate">{label}</p>
          <p className="text-[11px] text-slate-400">
            {count} service{count !== 1 && 's'}
            {hasPrices && (
              <>
                <span className="mx-1 text-slate-200">·</span>
                {activeCount} actif{activeCount !== 1 && 's'}
              </>
            )}
          </p>
        </div>
      </div>

      {/* Fourchette de prix */}
      <div className="flex items-center gap-1.5 text-[12px]">
        <Coins size={12} className="text-slate-300 flex-shrink-0" />
        {hasPrices ? (
          <span className="text-slate-700 font-medium">
            {min === max ? formatCurrencyXAF(min) : `${formatCurrencyXAF(min)} – ${formatCurrencyXAF(max)}`}
          </span>
        ) : (
          <span className="text-slate-300">Aucun tarif défini</span>
        )}
      </div>

      {/* Lien vers le détail */}
      <div className="flex items-center justify-between pt-2 border-t border-slate-50 mt-auto">
        <span className="text-[11px] font-semibold text-indigo-600 flex items-center gap-1 group-hover:gap-1.5 transition-all">
          Voir les tarifs
          <ArrowRight size={12} />
        </span>
      </div>
    </button>
  )
}

export default ClothingCategoryCards
