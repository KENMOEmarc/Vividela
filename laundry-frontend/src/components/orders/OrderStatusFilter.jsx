import { ListFilter } from 'lucide-react'
import { ORDER_STATUS_LABELS, ORDER_STATUS_BADGE } from '../../types/OrdersType'

const STATUS_KEYS = Object.keys(ORDER_STATUS_LABELS)

/**
 * Filtre les commandes par statut à l'aide de checkboxs.
 *
 * - `selected` vide == aucun filtre actif == toutes les commandes affichées.
 * - Chaque checkbox porte la couleur du badge de statut correspondant pour
 *   rester cohérent visuellement avec les cartes de commande.
 */
const OrderStatusFilter = ({ selected = [], onChange }) => {
  const toggle = (status) => {
    if (selected.includes(status)) {
      onChange(selected.filter((s) => s !== status))
    } else {
      onChange([...selected, status])
    }
  }

  const clearAll = () => onChange([])

  return (
    <div className="bg-white rounded-2xl border border-slate-100 p-4 shadow-sm">
      <div className="flex items-center justify-between mb-3">
        <div className="flex items-center gap-1.5 text-xs font-bold text-slate-700">
          <ListFilter size={14} className="text-slate-400" />
          Filtrer par statut
        </div>
        {selected.length > 0 && (
          <button
            type="button"
            onClick={clearAll}
            className="text-[11px] font-semibold text-slate-400 hover:text-slate-600"
          >
            Réinitialiser
          </button>
        )}
      </div>

      <div className="flex flex-wrap gap-2">
        {STATUS_KEYS.map((status) => {
          const checked = selected.includes(status)
          const badgeCls = ORDER_STATUS_BADGE[status] || 'bg-slate-100 text-slate-500 ring-slate-200'
          return (
            <label
              key={status}
              className={[
                'flex items-center gap-1.5 px-2.5 py-1.5 rounded-full text-[11px] font-semibold ring-1 cursor-pointer select-none transition-opacity',
                badgeCls,
                checked ? 'opacity-100' : 'opacity-45 hover:opacity-75',
              ].join(' ')}
            >
              <input
                type="checkbox"
                checked={checked}
                onChange={() => toggle(status)}
                className="w-3.5 h-3.5 rounded accent-current"
              />
              {ORDER_STATUS_LABELS[status]}
            </label>
          )
        })}
      </div>
    </div>
  )
}

export default OrderStatusFilter
