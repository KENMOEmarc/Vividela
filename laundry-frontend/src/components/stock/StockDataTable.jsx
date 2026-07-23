import { Boxes, TrendingDown, Clock, AlertTriangle, Package, CalendarClock } from 'lucide-react'
import { MEASUREMENT_UNIT_LABELS_BACKEND, formatDateFr } from '../../types/stockTypes'

const StockDataTable = ({
  stocks = [],
  loading,
  onManageBatches,
  onHistory,
}) => {
  if (loading) {
    return (
      <div className="bg-white rounded-2xl border border-slate-100 shadow-sm">
        <div className="flex justify-center items-center h-48">
          <div className="w-7 h-7 border-2 border-blue-500 border-t-transparent rounded-full animate-spin" />
        </div>
      </div>
    )
  }

  if (stocks.length === 0) {
    return (
      <div className="bg-white rounded-2xl border border-slate-100 shadow-sm">
        <div className="flex flex-col items-center justify-center py-16 text-slate-400">
          <Package size={32} className="mb-3 opacity-40" />
          <p className="text-sm">Aucun stock enregistré</p>
        </div>
      </div>
    )
  }

  return (
    <div className="bg-white rounded-2xl border border-slate-100 shadow-sm overflow-hidden">
      <div className="overflow-x-auto">
        <table className="w-full text-sm">
          <thead>
            <tr className="border-b border-slate-100 bg-slate-50">
              <th className="text-left px-5 py-3 text-xs font-semibold text-slate-500 uppercase tracking-wide">Produit</th>
              <th className="text-right px-5 py-3 text-xs font-semibold text-slate-500 uppercase tracking-wide">Qté totale</th>
              <th className="text-right px-5 py-3 text-xs font-semibold text-slate-500 uppercase tracking-wide">Seuil alerte</th>
              <th className="text-center px-5 py-3 text-xs font-semibold text-slate-500 uppercase tracking-wide">Unité</th>
              <th className="text-center px-5 py-3 text-xs font-semibold text-slate-500 uppercase tracking-wide">Lots</th>
              <th className="text-center px-5 py-3 text-xs font-semibold text-slate-500 uppercase tracking-wide">Expiration proche</th>
              <th className="text-center px-5 py-3 text-xs font-semibold text-slate-500 uppercase tracking-wide">État</th>
              <th className="text-right px-5 py-3 text-xs font-semibold text-slate-500 uppercase tracking-wide">Actions</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-50">
            {stocks.map(stock => (
              <tr key={stock.productId} className={`hover:bg-slate-50/60 transition-colors ${stock.belowThreshold ? 'bg-red-50/30' : ''}`}>
                <td className="px-5 py-3.5">
                  <div className="flex items-center gap-2.5">
                    {stock.belowThreshold && (
                      <AlertTriangle size={13} className="text-red-500 flex-shrink-0" />
                    )}
                    <span className="font-semibold text-slate-800">{stock.productName}</span>
                  </div>
                </td>
                <td className="px-5 py-3.5 text-right">
                  <span className={`font-bold tabular-nums ${stock.belowThreshold ? 'text-red-600' : 'text-slate-800'}`}>
                    {parseFloat(stock.currentQuantity).toFixed(2)}
                  </span>
                </td>
                <td className="px-5 py-3.5 text-right">
                  <span className="text-slate-500 tabular-nums">
                    {parseFloat(stock.thresholdValue).toFixed(2)}
                  </span>
                </td>
                <td className="px-5 py-3.5 text-center">
                  <span className="px-2 py-0.5 rounded-md bg-slate-100 text-slate-600 text-xs font-medium">
                    {MEASUREMENT_UNIT_LABELS_BACKEND[stock.measurementUnit] ?? stock.measurementUnit}
                  </span>
                </td>
                <td className="px-5 py-3.5 text-center">
                  <span className="px-2 py-0.5 rounded-full bg-blue-50 text-blue-600 text-xs font-semibold">
                    {stock.batchCount ?? 0}
                  </span>
                </td>
                <td className="px-5 py-3.5 text-center">
                  {stock.nearestExpirationDate ? (
                    <span className="inline-flex items-center gap-1 text-xs font-medium text-slate-500">
                      <CalendarClock size={12} />
                      {formatDateFr(stock.nearestExpirationDate)}
                    </span>
                  ) : (
                    <span className="text-xs text-slate-300">—</span>
                  )}
                </td>
                <td className="px-5 py-3.5 text-center">
                  {stock.belowThreshold ? (
                    <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-xs font-semibold bg-red-100 text-red-700">
                      <TrendingDown size={10} /> Alerte
                    </span>
                  ) : (
                    <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-xs font-semibold bg-emerald-100 text-emerald-700">
                      ✓ OK
                    </span>
                  )}
                </td>
                <td className="px-5 py-3.5 text-right">
                  <div className="flex items-center justify-end gap-2">
                    <button onClick={() => onHistory(stock)}
                      title="Historique des mouvements"
                      className="p-1.5 text-slate-400 hover:text-blue-600 hover:bg-blue-50 rounded-lg transition-colors">
                      <Clock size={14} />
                    </button>
                    <button onClick={() => onManageBatches(stock)}
                      title="Gérer les lots de stock"
                      className="p-1.5 text-slate-400 hover:text-emerald-600 hover:bg-emerald-50 rounded-lg transition-colors">
                      <Boxes size={14} />
                    </button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  )
}

export default StockDataTable
