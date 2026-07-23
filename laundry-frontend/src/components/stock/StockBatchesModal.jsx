import { useState, useEffect, useCallback } from 'react'
import { toast } from 'react-toastify'
import {
  X, Package, Loader2, Pencil, Trash2, TrendingDown, PackagePlus, CalendarClock,
} from 'lucide-react'
import {
  getBatchesByProduct, createStockBatch, updateStockBatch, deleteStockBatch, consumeStock,
} from '../../api/stockApi'
import { formatDateFr } from '../../types/stockTypes'

const emptyBatchForm = { quantity: '', unitPrice: '', entryDate: '', expirationDate: '', notes: '' }
const emptyConsumeForm = { quantity: '', notes: '' }

const todayIso = () => new Date().toISOString().slice(0, 10)

const StockBatchesModal = ({ stock, onClose, onSuccess }) => {
  const [batches, setBatches] = useState([])
  const [loading, setLoading] = useState(true)

  const [mode, setMode] = useState(null) // null | 'add' | 'edit' | 'consume'
  const [editingBatchId, setEditingBatchId] = useState(null)
  const [batchForm, setBatchForm] = useState(emptyBatchForm)
  const [consumeForm, setConsumeForm] = useState(emptyConsumeForm)
  const [errors, setErrors] = useState({})
  const [saving, setSaving] = useState(false)

  const loadBatches = useCallback(async () => {
    setLoading(true)
    try {
      const res = await getBatchesByProduct(stock.productId)
      setBatches(res.data?.data ?? [])
    } catch {
      toast.error('Impossible de charger les lots de stock')
    } finally {
      setLoading(false)
    }
  }, [stock.productId])

  useEffect(() => { loadBatches() }, [loadBatches])

  const totalQuantity = batches.reduce((sum, b) => sum + parseFloat(b.currentQuantity || 0), 0)

  const openAddForm = () => {
    setBatchForm({ ...emptyBatchForm, entryDate: todayIso() })
    setErrors({})
    setMode('add')
  }

  const openEditForm = (batch) => {
    setEditingBatchId(batch.id)
    setBatchForm({
      quantity: batch.currentQuantity ?? '',
      unitPrice: batch.unitPrice ?? '',
      entryDate: batch.entryDate ?? todayIso(),
      expirationDate: batch.expirationDate ?? '',
      notes: '',
    })
    setErrors({})
    setMode('edit')
  }

  const openConsumeForm = () => {
    setConsumeForm(emptyConsumeForm)
    setErrors({})
    setMode('consume')
  }

  const closeForm = () => {
    setMode(null)
    setEditingBatchId(null)
    setErrors({})
  }

  const handleBatchSubmit = async (e) => {
    e.preventDefault()
    const errs = {}
    if (!batchForm.quantity || isNaN(batchForm.quantity) || parseFloat(batchForm.quantity) < 0)
      errs.quantity = 'Quantité invalide'
    if (mode === 'add' && parseFloat(batchForm.quantity) <= 0)
      errs.quantity = 'La quantité doit être > 0'
    if (batchForm.unitPrice && (isNaN(batchForm.unitPrice) || parseFloat(batchForm.unitPrice) < 0))
      errs.unitPrice = 'Prix invalide'
    if (batchForm.expirationDate && batchForm.entryDate && batchForm.expirationDate < batchForm.entryDate)
      errs.expirationDate = 'Doit être postérieure à la date d\'entrée'
    if (Object.keys(errs).length) { setErrors(errs); return }

    setSaving(true)
    try {
      const payload = {
        quantity: parseFloat(batchForm.quantity),
        unitPrice: batchForm.unitPrice ? parseFloat(batchForm.unitPrice) : null,
        entryDate: batchForm.entryDate || null,
        expirationDate: batchForm.expirationDate || null,
        notes: batchForm.notes || null,
      }
      if (mode === 'add') {
        await createStockBatch({ ...payload, productId: stock.productId })
        toast.success('Lot de stock ajouté avec succès')
      } else {
        await updateStockBatch(editingBatchId, payload)
        toast.success('Lot de stock mis à jour avec succès')
      }
      closeForm()
      await loadBatches()
      onSuccess?.()
    } catch (err) {
      toast.error(err?.response?.data?.message || 'Erreur lors de l\'enregistrement du lot')
    } finally {
      setSaving(false)
    }
  }

  const handleConsumeSubmit = async (e) => {
    e.preventDefault()
    const errs = {}
    if (!consumeForm.quantity || isNaN(consumeForm.quantity) || parseFloat(consumeForm.quantity) <= 0)
      errs.quantity = 'Quantité invalide (doit être > 0)'
    if (Object.keys(errs).length) { setErrors(errs); return }

    setSaving(true)
    try {
      await consumeStock({
        productId: stock.productId,
        quantity: parseFloat(consumeForm.quantity),
        notes: consumeForm.notes || null,
      })
      toast.success('Stock consommé avec succès')
      closeForm()
      await loadBatches()
      onSuccess?.()
    } catch (err) {
      toast.error(err?.response?.data?.message || 'Erreur lors de la consommation')
    } finally {
      setSaving(false)
    }
  }

  const handleDelete = async (batch) => {
    if (parseFloat(batch.currentQuantity || 0) !== 0) {
      toast.error('Ce lot doit être à quantité nulle pour être supprimé')
      return
    }
    if (!window.confirm('Supprimer définitivement ce lot de stock ?')) return
    try {
      await deleteStockBatch(batch.id)
      toast.success('Lot supprimé')
      await loadBatches()
      onSuccess?.()
    } catch (err) {
      toast.error(err?.response?.data?.message || 'Suppression impossible')
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/40 backdrop-blur-sm">
      <div className="bg-white rounded-2xl shadow-2xl w-full max-w-2xl max-h-[90vh] flex flex-col">

        <div className="flex items-center justify-between px-6 pt-5 pb-4 border-b border-slate-100 flex-shrink-0">
          <div className="flex items-center gap-3">
            <div className="w-9 h-9 rounded-xl bg-blue-100 flex items-center justify-center">
              <Package size={17} className="text-blue-600" />
            </div>
            <div>
              <h2 className="text-sm font-bold text-slate-900">Lots de stock</h2>
              <p className="text-xs text-slate-500">{stock.productName} · {totalQuantity.toFixed(2)} {stock.measurementUnit} au total</p>
            </div>
          </div>
          <button onClick={onClose} className="p-1.5 text-slate-400 hover:text-slate-600 hover:bg-slate-100 rounded-lg transition-colors">
            <X size={16} />
          </button>
        </div>

        <div className="overflow-y-auto flex-1 px-6 py-4">

          {/* Action bar */}
          {mode === null && (
            <div className="flex gap-2 mb-4">
              <button onClick={openAddForm}
                className="flex items-center gap-1.5 px-3 py-2 bg-emerald-600 hover:bg-emerald-700 text-white rounded-xl text-xs font-semibold transition-colors">
                <PackagePlus size={14} /> Ajouter un lot
              </button>
              <button onClick={openConsumeForm}
                className="flex items-center gap-1.5 px-3 py-2 border border-slate-200 hover:bg-slate-50 text-slate-600 rounded-xl text-xs font-semibold transition-colors">
                <TrendingDown size={14} /> Consommer
              </button>
            </div>
          )}

          {/* Add / edit batch form */}
          {(mode === 'add' || mode === 'edit') && (
            <form onSubmit={handleBatchSubmit} className="mb-4 p-4 rounded-xl bg-slate-50 border border-slate-100 space-y-3">
              <p className="text-xs font-semibold text-slate-700">
                {mode === 'add' ? 'Nouveau lot de stock' : 'Modifier le lot'}
              </p>
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-[11px] font-semibold text-slate-600 mb-1">
                    Quantité <span className="text-slate-400">({stock.measurementUnit})</span>
                  </label>
                  <input type="number" step="0.01" min="0"
                    value={batchForm.quantity}
                    onChange={e => setBatchForm(f => ({ ...f, quantity: e.target.value }))}
                    className={`w-full px-3 py-2 border rounded-lg text-sm focus:outline-none focus:ring-2 ${
                      errors.quantity ? 'border-red-400 focus:ring-red-100' : 'border-slate-300 focus:border-blue-500 focus:ring-blue-100'
                    }`} placeholder="0.00" disabled={saving} />
                  {errors.quantity && <p className="mt-1 text-[11px] text-red-500">{errors.quantity}</p>}
                </div>
                <div>
                  <label className="block text-[11px] font-semibold text-slate-600 mb-1">Prix unitaire</label>
                  <input type="number" step="0.01" min="0"
                    value={batchForm.unitPrice}
                    onChange={e => setBatchForm(f => ({ ...f, unitPrice: e.target.value }))}
                    className={`w-full px-3 py-2 border rounded-lg text-sm focus:outline-none focus:ring-2 ${
                      errors.unitPrice ? 'border-red-400 focus:ring-red-100' : 'border-slate-300 focus:border-blue-500 focus:ring-blue-100'
                    }`} placeholder="0.00" disabled={saving} />
                  {errors.unitPrice && <p className="mt-1 text-[11px] text-red-500">{errors.unitPrice}</p>}
                </div>
                <div>
                  <label className="block text-[11px] font-semibold text-slate-600 mb-1">Date d'entrée en stock</label>
                  <input type="date"
                    value={batchForm.entryDate}
                    onChange={e => setBatchForm(f => ({ ...f, entryDate: e.target.value }))}
                    className="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:border-blue-500 focus:ring-blue-100"
                    disabled={saving} />
                </div>
                <div>
                  <label className="block text-[11px] font-semibold text-slate-600 mb-1">
                    Date d'expiration <span className="text-slate-400">(optionnel)</span>
                  </label>
                  <input type="date"
                    value={batchForm.expirationDate}
                    onChange={e => setBatchForm(f => ({ ...f, expirationDate: e.target.value }))}
                    className={`w-full px-3 py-2 border rounded-lg text-sm focus:outline-none focus:ring-2 ${
                      errors.expirationDate ? 'border-red-400 focus:ring-red-100' : 'border-slate-300 focus:border-blue-500 focus:ring-blue-100'
                    }`} disabled={saving} />
                  {errors.expirationDate && <p className="mt-1 text-[11px] text-red-500">{errors.expirationDate}</p>}
                </div>
              </div>
              <div>
                <label className="block text-[11px] font-semibold text-slate-600 mb-1">Notes (optionnel)</label>
                <input type="text" value={batchForm.notes}
                  onChange={e => setBatchForm(f => ({ ...f, notes: e.target.value }))}
                  placeholder="Ex: Livraison fournisseur..."
                  className="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:border-blue-500 focus:ring-blue-100"
                  disabled={saving} />
              </div>
              <div className="flex gap-2 pt-1">
                <button type="button" onClick={closeForm} disabled={saving}
                  className="flex-1 py-2 border border-slate-200 rounded-lg text-xs font-semibold text-slate-600 hover:bg-white transition-colors">
                  Annuler
                </button>
                <button type="submit" disabled={saving}
                  className="flex-1 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-lg text-xs font-semibold transition-colors flex items-center justify-center gap-2 disabled:opacity-60">
                  {saving ? <><Loader2 size={13} className="animate-spin" />Enregistrement…</> : 'Confirmer'}
                </button>
              </div>
            </form>
          )}

          {/* Consume form */}
          {mode === 'consume' && (
            <form onSubmit={handleConsumeSubmit} className="mb-4 p-4 rounded-xl bg-slate-50 border border-slate-100 space-y-3">
              <p className="text-xs font-semibold text-slate-700">Consommer du stock</p>
              <p className="text-[11px] text-slate-500">
                La quantité est automatiquement prélevée sur les lots les plus proches de leur date d'expiration.
              </p>
              <div>
                <label className="block text-[11px] font-semibold text-slate-600 mb-1">
                  Quantité <span className="text-slate-400">({stock.measurementUnit})</span>
                </label>
                <input type="number" step="0.01" min="0.01"
                  value={consumeForm.quantity}
                  onChange={e => setConsumeForm(f => ({ ...f, quantity: e.target.value }))}
                  className={`w-full px-3 py-2 border rounded-lg text-sm focus:outline-none focus:ring-2 ${
                    errors.quantity ? 'border-red-400 focus:ring-red-100' : 'border-slate-300 focus:border-blue-500 focus:ring-blue-100'
                  }`} placeholder="0.00" disabled={saving} />
                {errors.quantity && <p className="mt-1 text-[11px] text-red-500">{errors.quantity}</p>}
              </div>
              <div>
                <label className="block text-[11px] font-semibold text-slate-600 mb-1">Notes (optionnel)</label>
                <input type="text" value={consumeForm.notes}
                  onChange={e => setConsumeForm(f => ({ ...f, notes: e.target.value }))}
                  className="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:border-blue-500 focus:ring-blue-100"
                  disabled={saving} />
              </div>
              <div className="flex gap-2 pt-1">
                <button type="button" onClick={closeForm} disabled={saving}
                  className="flex-1 py-2 border border-slate-200 rounded-lg text-xs font-semibold text-slate-600 hover:bg-white transition-colors">
                  Annuler
                </button>
                <button type="submit" disabled={saving}
                  className="flex-1 py-2 bg-red-600 hover:bg-red-700 text-white rounded-lg text-xs font-semibold transition-colors flex items-center justify-center gap-2 disabled:opacity-60">
                  {saving ? <><Loader2 size={13} className="animate-spin" />Enregistrement…</> : 'Confirmer'}
                </button>
              </div>
            </form>
          )}

          {/* Batches list */}
          {loading ? (
            <div className="flex justify-center py-10">
              <div className="w-6 h-6 border-2 border-blue-500 border-t-transparent rounded-full animate-spin" />
            </div>
          ) : batches.length === 0 ? (
            <p className="text-center text-slate-400 text-sm py-8">Aucun lot de stock pour ce produit</p>
          ) : (
            <div className="space-y-2">
              {batches.map(batch => {
                const isEmpty = parseFloat(batch.currentQuantity || 0) === 0
                return (
                  <div key={batch.id}
                    className={`flex items-center justify-between gap-3 p-3 rounded-xl border ${
                      batch.expired ? 'bg-red-50/50 border-red-100' : batch.expiringSoon ? 'bg-amber-50/50 border-amber-100' : 'bg-slate-50 border-slate-100'
                    }`}>
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center gap-2">
                        <span className={`text-sm font-bold ${isEmpty ? 'text-slate-400' : 'text-slate-800'}`}>
                          {parseFloat(batch.currentQuantity).toFixed(2)} {stock.measurementUnit}
                        </span>
                        {batch.unitPrice != null && (
                          <span className="text-xs text-slate-500">· {parseFloat(batch.unitPrice).toFixed(2)} / unité</span>
                        )}
                        {batch.expired && (
                          <span className="px-1.5 py-0.5 rounded-full text-[10px] font-semibold bg-red-100 text-red-700">Expiré</span>
                        )}
                        {!batch.expired && batch.expiringSoon && (
                          <span className="px-1.5 py-0.5 rounded-full text-[10px] font-semibold bg-amber-100 text-amber-700">Expire bientôt</span>
                        )}
                      </div>
                      <div className="flex items-center gap-3 mt-1 text-[11px] text-slate-500">
                        <span>Entrée : {formatDateFr(batch.entryDate)}</span>
                        <span className="flex items-center gap-1">
                          <CalendarClock size={11} /> Expiration : {formatDateFr(batch.expirationDate)}
                        </span>
                      </div>
                    </div>
                    <div className="flex items-center gap-1.5 flex-shrink-0">
                      <button onClick={() => openEditForm(batch)}
                        title="Modifier le lot"
                        className="p-1.5 text-slate-400 hover:text-blue-600 hover:bg-blue-50 rounded-lg transition-colors">
                        <Pencil size={13} />
                      </button>
                      <button onClick={() => handleDelete(batch)}
                        title={isEmpty ? 'Supprimer le lot' : 'Quantité non nulle : videz le lot avant suppression'}
                        disabled={!isEmpty}
                        className="p-1.5 text-slate-400 hover:text-red-600 hover:bg-red-50 rounded-lg transition-colors disabled:opacity-30 disabled:hover:bg-transparent disabled:hover:text-slate-400">
                        <Trash2 size={13} />
                      </button>
                    </div>
                  </div>
                )
              })}
            </div>
          )}
        </div>

        <div className="px-6 pb-4 pt-3 border-t border-slate-100 flex-shrink-0">
          <button onClick={onClose}
            className="w-full py-2.5 border border-slate-200 rounded-xl text-sm font-semibold text-slate-600 hover:bg-slate-50 transition-colors">
            Fermer
          </button>
        </div>
      </div>
    </div>
  )
}

export default StockBatchesModal
