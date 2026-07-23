import { CalendarClock } from 'lucide-react'
import { DELAY_PRESETS, DEFAULT_DELAY_FILTER } from '../../types/OrdersType'

/**
 * Filtre les commandes par délai de livraison (expectedDeliveryDate).
 *
 * - Boutons de raccourci (En retard / Aujourd'hui / 7 jours / 30 jours).
 * - "Période personnalisée" révèle deux champs date (du / au).
 * - `value.preset === 'all'` == aucun filtre actif == toutes les commandes.
 */
const OrderDelayFilter = ({ value = DEFAULT_DELAY_FILTER, onChange }) => {
  const setPreset = (preset) => {
    if (preset === 'custom') {
      onChange({ ...value, preset })
    } else {
      onChange({ preset, from: '', to: '' })
    }
  }

  const setRange = (field, val) => {
    onChange({ ...value, preset: 'custom', [field]: val })
  }

  const isActive = value.preset !== 'all'

  return (
    <div className="bg-white rounded-2xl border border-slate-100 p-4 shadow-sm">
      <div className="flex items-center justify-between mb-3">
        <div className="flex items-center gap-1.5 text-xs font-bold text-slate-700">
          <CalendarClock size={14} className="text-slate-400" />
          Filtrer par délai
        </div>
        {isActive && (
          <button
            type="button"
            onClick={() => onChange(DEFAULT_DELAY_FILTER)}
            className="text-[11px] font-semibold text-slate-400 hover:text-slate-600"
          >
            Réinitialiser
          </button>
        )}
      </div>

      <div className="flex flex-wrap gap-2">
        {DELAY_PRESETS.map(({ key, label }) => {
          const checked = value.preset === key
          return (
            <button
              key={key}
              type="button"
              onClick={() => setPreset(key)}
              className={[
                'px-2.5 py-1.5 rounded-full text-[11px] font-semibold ring-1 transition-colors',
                checked
                  ? key === 'overdue'
                    ? 'bg-red-50 text-red-600 ring-red-100'
                    : 'bg-cyan-50 text-cyan-600 ring-cyan-100'
                  : 'bg-slate-50 text-slate-500 ring-slate-100 hover:bg-slate-100',
              ].join(' ')}
            >
              {label}
            </button>
          )
        })}
      </div>

      {value.preset === 'custom' && (
        <div className="flex flex-wrap items-center gap-2 mt-3 pt-3 border-t border-slate-50">
          <label className="flex items-center gap-1.5 text-[11px] text-slate-500">
            Du
            <input
              type="date"
              value={value.from}
              onChange={(e) => setRange('from', e.target.value)}
              className="px-2 py-1.5 text-xs rounded-lg border border-slate-200 focus:outline-none focus:ring-2 focus:ring-cyan-100 focus:border-cyan-300"
            />
          </label>
          <label className="flex items-center gap-1.5 text-[11px] text-slate-500">
            Au
            <input
              type="date"
              value={value.to}
              onChange={(e) => setRange('to', e.target.value)}
              className="px-2 py-1.5 text-xs rounded-lg border border-slate-200 focus:outline-none focus:ring-2 focus:ring-cyan-100 focus:border-cyan-300"
            />
          </label>
        </div>
      )}
    </div>
  )
}

export default OrderDelayFilter
