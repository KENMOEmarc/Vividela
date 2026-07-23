import { useState, useEffect, useCallback } from 'react'
import { toast } from 'react-toastify'
import { getAllStocks, getLowStock } from '../api/stockApi'
import { getAllProducts } from '../api/productApi'
import StockDataTable from '../components/stock/StockDataTable'
import StockBatchesModal from '../components/stock/StockBatchesModal'
import StockMovementsModal from '../components/stock/StockMovementsModal'
import { Warehouse, AlertTriangle, Package, TrendingUp } from 'lucide-react'

const StockPage = () => {
  const [stocks, setStocks]           = useState([])
  const [lowCount, setLowCount]       = useState(0)
  const [totalProducts, setTotal]     = useState(0)
  const [loading, setLoading]         = useState(true)
  const [batchesTarget, setBatchesTarget] = useState(null)
  const [historyTarget, setHistoryTarget] = useState(null)
  const [filter, setFilter]           = useState('all') // all | low

  const loadData = useCallback(async () => {
    setLoading(true)
    try {
      const [stocksRes, lowRes, productsRes] = await Promise.all([
        getAllStocks(),
        getLowStock(),
        getAllProducts(),
      ])
      setStocks(stocksRes.data?.data ?? [])
      setLowCount((lowRes.data?.data ?? []).length)
      setTotal((productsRes.data?.data ?? []).length)
    } catch {
      toast.error('Impossible de charger les données de stock')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => { loadData() }, [loadData])

  const displayed = filter === 'low'
    ? stocks.filter(s => s.belowThreshold)
    : stocks

  const handleBatchesSuccess = () => {
    loadData()
  }

  return (
    <div className="space-y-5">

      {/* Page header */}
      <div className="flex items-start justify-between gap-4">
        <div>
          <h1 className="text-lg font-bold text-slate-900 flex items-center gap-2">
            <span className="w-7 h-7 rounded-lg bg-emerald-100 text-emerald-600 flex items-center justify-center">
              <Warehouse size={15} />
            </span>
            Gestion du stock
          </h1>
          <p className="text-xs text-slate-500 mt-0.5 ml-9">
            Suivez et mettez à jour les niveaux de stock de vos produits, lot par lot.
          </p>
        </div>
      </div>

      {/* KPI cards */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        <div className="bg-white rounded-2xl border border-slate-100 shadow-sm p-4 flex items-center gap-4">
          <div className="w-10 h-10 rounded-xl bg-blue-100 flex items-center justify-center">
            <Package size={18} className="text-blue-600" />
          </div>
          <div>
            <p className="text-xs text-slate-500">Produits</p>
            <p className="text-xl font-bold text-slate-800">{totalProducts}</p>
          </div>
        </div>
        <div className="bg-white rounded-2xl border border-slate-100 shadow-sm p-4 flex items-center gap-4">
          <div className="w-10 h-10 rounded-xl bg-emerald-100 flex items-center justify-center">
            <TrendingUp size={18} className="text-emerald-600" />
          </div>
          <div>
            <p className="text-xs text-slate-500">Stocks OK</p>
            <p className="text-xl font-bold text-slate-800">{stocks.length - lowCount}</p>
          </div>
        </div>
        <div className={`rounded-2xl border shadow-sm p-4 flex items-center gap-4 cursor-pointer transition-colors ${
          lowCount > 0 ? 'bg-red-50 border-red-200' : 'bg-white border-slate-100'
        }`} onClick={() => setFilter(f => f === 'low' ? 'all' : 'low')}>
          <div className="w-10 h-10 rounded-xl bg-red-100 flex items-center justify-center">
            <AlertTriangle size={18} className="text-red-600" />
          </div>
          <div>
            <p className="text-xs text-slate-500">Alertes stock</p>
            <p className={`text-xl font-bold ${lowCount > 0 ? 'text-red-600' : 'text-slate-800'}`}>{lowCount}</p>
          </div>
        </div>
      </div>

      {/* Filter bar */}
      <div className="flex items-center gap-2">
        <button onClick={() => setFilter('all')}
          className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition-colors ${
            filter === 'all' ? 'bg-blue-600 text-white' : 'bg-white border border-slate-200 text-slate-600 hover:bg-slate-50'
          }`}>
          Tous ({stocks.length})
        </button>
        <button onClick={() => setFilter('low')}
          className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition-colors ${
            filter === 'low' ? 'bg-red-600 text-white' : 'bg-white border border-slate-200 text-slate-600 hover:bg-slate-50'
          }`}>
          Alertes ({lowCount})
        </button>
      </div>

      {/* Table */}
      <StockDataTable
        stocks={displayed}
        loading={loading}
        onManageBatches={setBatchesTarget}
        onHistory={setHistoryTarget}
      />

      {/* Modals */}
      {batchesTarget && (
        <StockBatchesModal
          stock={batchesTarget}
          onClose={() => setBatchesTarget(null)}
          onSuccess={handleBatchesSuccess}
        />
      )}
      {historyTarget && (
        <StockMovementsModal
          stock={historyTarget}
          onClose={() => setHistoryTarget(null)}
        />
      )}
    </div>
  )
}

export default StockPage
