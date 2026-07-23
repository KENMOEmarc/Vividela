import { X } from 'lucide-react'
import DataTable from '../common/DataTable'
import { CLOTHING_TYPE_LABELS } from '../../utils/constants'
import { SERVICE_PRICE_TABLE_COLUMNS } from '../../types/servicePriceTypes'
import { getClothingEmoji } from '../../utils/clothingIcons'

// Colonnes du tableau de détail : on retire "Type de vêtement" (déjà connu — c'est
// la catégorie cliquée) pour ne garder que les colonnes utiles au sein de la modale.
const CATEGORY_TABLE_COLUMNS = SERVICE_PRICE_TABLE_COLUMNS.filter((col) => col.key !== 'clothingType')

/**
 * Modale affichant le tableau de données des tarifs de service pour une seule
 * catégorie de vêtement (ouverte depuis une carte de ClothingCategoryCards).
 */
const CategoryServicesModal = ({
  clothingType,
  servicePrices = [],
  onClose,
  onEdit,
  onDelete,
  canEdit = false,
  canDelete = false,
}) => {
  if (!clothingType) return null

  const categoryLabel = CLOTHING_TYPE_LABELS[clothingType] ?? clothingType
  const filtered = servicePrices.filter((sp) => sp.clothingType === clothingType)

  return (
    <div
      className="fixed inset-0 bg-black/40 backdrop-blur-sm z-50 flex items-center justify-center p-4"
      onClick={(e) => e.target === e.currentTarget && onClose()}
    >
      <div className="bg-white rounded-2xl shadow-2xl shadow-black/10 w-full max-w-3xl max-h-[85vh] flex flex-col animate-fade-in-up overflow-hidden">

        {/* ── En-tête ────────────────────────────────────────────────── */}
        <div className="relative overflow-hidden bg-gradient-to-br from-indigo-500 to-indigo-700 px-6 pt-6 pb-5 flex-shrink-0">
          <div className="absolute -right-4 -top-4 w-20 h-20 rounded-full bg-white/10 pointer-events-none" />
          <div className="absolute right-6 bottom-0 w-10 h-10 rounded-full bg-white/10 pointer-events-none" />
          <div className="relative flex items-start justify-between gap-3">
            <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-xl bg-white/20 flex items-center justify-center flex-shrink-0">
                  <span className="text-lg text-white">{getClothingEmoji(clothingType)}</span>
                </div>
              <div>
                <h3 className="text-white font-bold text-sm">{categoryLabel}</h3>
                <p className="text-indigo-100 text-[11px] mt-0.5">
                  {filtered.length} service{filtered.length !== 1 && 's'} de tarification
                </p>
              </div>
            </div>
            <button
              onClick={onClose}
              className="text-white/70 hover:text-white hover:bg-white/10 rounded-lg p-1.5 transition-colors flex-shrink-0"
            >
              <X size={16} />
            </button>
          </div>
        </div>

        {/* ── Tableau des services de la catégorie ──────────────────────── */}
        <div className="overflow-y-auto flex-1">
          <DataTable
            columns={CATEGORY_TABLE_COLUMNS}
            data={filtered}
            searchFields={['service', 'price']}
            searchPlaceholder="Rechercher un service…"
            onEdit={canEdit ? onEdit : null}
            onDelete={canDelete ? onDelete : null}
            emptyMessage="Aucun tarif défini pour cette catégorie."
          />
        </div>
      </div>
    </div>
  )
}

export default CategoryServicesModal
