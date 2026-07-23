import { useState, useEffect } from 'react'
import { X, Clock, TrendingUp, TrendingDown, RefreshCw } from 'lucide-react'
import { getMovementsByProduct } from '../../api/stockApi'
import { MOVEMENT_TYPE_LABELS, MOVEMENT_TYPE_COLORS } from '../../types/stockTypes'

const ICONS = { RESTOCK: TrendingUp, CONSUMPTION: TrendingDown, ADJUSTMENT: RefreshCw }

const StockMovementsModal = ({ stock, onClose }) => {
  const [movements, setMovements] = useState([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    getMovementsByProduct(stock.productId)
      .then(res => setMovements(res.data?.data ?? []))
      .catch(() => {})
      .finally(() => setLoading(false))
  }, [stock.productId])

  const fmt = (iso) => iso
    ? new Date(iso).toLocaleString('fr-FR', { dateStyle: 'short', timeStyle: 'short' })
    : '—'

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/40 backdrop-blur-sm">
      <div className="bg-white rounded-2xl shadow-2xl w-full max-w-lg max-h-[85vh] flex flex-col">
        <div className="flex items-center justify-between px-6 pt-5 pb-4 border-b border-slate-100 flex-shrink-0">
          <div>
            <h2 className="text-sm font-bold text-slate-900 flex items-center gap-2">
              <Clock size={15} className="text-blue-500" />
              Historique des mouvements
            </h2>
            <p className="text-xs text-slate-500 mt-0.5">{stock.productName}</p>
          </div>
          <button onClick={onClose} className="p-1.5 text-slate-400 hover:text-slate-600 hover:bg-slate-100 rounded-lg">
            <X size={16} />
          </button>
        </div>

        <div className="overflow-y-auto flex-1 px-6 py-4">
          {loading ? (
            <div className="flex justify-center py-10">
              <div className="w-6 h-6 border-2 border-blue-500 border-t-transparent rounded-full animate-spin" />
            </div>
          ) : movements.length === 0 ? (
            <p className="text-center text-slate-400 text-sm py-8">Aucun mouvement enregistré</p>
          ) : (
            <div className="space-y-3">
              {movements.map(m => {
                const Icon = ICONS[m.movementType] ?? RefreshCw
                const colorClass = MOVEMENT_TYPE_COLORS[m.movementType] ?? 'text-slate-600 bg-slate-50'
                return (
                  <div key={m.id} className="flex items-start gap-3 p-3 rounded-xl bg-slate-50 border border-slate-100">
                    <div className={`flex-shrink-0 w-8 h-8 rounded-lg flex items-center justify-center ${colorClass}`}>
                      <Icon size={14} />
                    </div>
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center justify-between gap-2">
                        <span className="text-xs font-semibold text-slate-800">
                          {MOVEMENT_TYPE_LABELS[m.movementType] ?? m.movementType}
                        </span>
                        <span className={`text-xs font-bold ${m.movementType === 'CONSUMPTION' ? 'text-red-600' : 'text-emerald-600'}`}>
                          {m.movementType === 'CONSUMPTION' ? '-' : m.movementType === 'ADJUSTMENT' ? '' : '+'}{m.quantity} {stock.measurementUnit}
                        </span>
                      </div>
                      {m.notes && <p className="text-xs text-slate-500 mt-0.5 truncate">{m.notes}</p>}
                      <p className="text-[10px] text-slate-400 mt-1">{fmt(m.movementDate)} — {m.userName}</p>
                    </div>
                  </div>
                )
              })}
            </div>
          )}
        </div>

        <div className="px-6 pb-4 flex-shrink-0">
          <button onClick={onClose}
            className="w-full py-2.5 border border-slate-200 rounded-xl text-sm font-semibold text-slate-600 hover:bg-slate-50 transition-colors">
            Fermer
          </button>
        </div>
      </div>
    </div>
  )
}

export default StockMovementsModal
