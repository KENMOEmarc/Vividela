import { useState } from 'react'
import { Trash2, X, TriangleAlert } from 'lucide-react'
import Button from './Button'

const DeleteConfirmModal = ({
  isOpen,
  title = 'Confirmer la suppression',
  description = 'Cette action est irréversible.',
  onConfirm,
  onClose,
  renderPreview,
  confirmLabel = 'Supprimer',
}) => {
  const [loading, setLoading] = useState(false)

  if (!isOpen) return null

  const handleConfirm = async () => {
    setLoading(true)
    try { await onConfirm() }
    finally { setLoading(false) }
  }

  return (
    <div
      className="fixed inset-0 bg-black/40 backdrop-blur-sm z-50 flex items-center justify-center p-4"
      onClick={(e) => !loading && e.target === e.currentTarget && onClose()}
    >
      <div className="bg-white rounded-2xl shadow-2xl shadow-black/10 w-full max-w-sm animate-fade-in-up overflow-hidden">

        {/* ── En-tête rouge ─────────────────────────────────────── */}
        <div className="relative overflow-hidden bg-gradient-to-br from-red-500 to-rose-700 px-6 pt-6 pb-5">
          <div className="absolute -right-4 -top-4 w-20 h-20 rounded-full bg-white/10 pointer-events-none" />
          <div className="absolute right-6 bottom-0 w-10 h-10 rounded-full bg-white/10 pointer-events-none" />
          <div className="relative flex items-start justify-between gap-3">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-xl bg-white/20 flex items-center justify-center flex-shrink-0">
                <TriangleAlert size={20} className="text-white" />
              </div>
              <div>
                <h2 className="font-bold text-white text-[15px] leading-tight">{title}</h2>
                <p className="text-red-200 text-[11px] mt-0.5">Cette action est irréversible</p>
              </div>
            </div>
            <button
              onClick={onClose}
              disabled={loading}
              className="p-1.5 text-red-200 hover:text-white hover:bg-white/15 rounded-lg transition-colors flex-shrink-0"
            >
              <X size={16} />
            </button>
          </div>
        </div>

        {/* ── Corps ─────────────────────────────────────────────── */}
        <div className="p-6 space-y-4">
          <p className="text-sm text-slate-600">
            Vous êtes sur le point de supprimer définitivement l'élément suivant :
          </p>

          {renderPreview && renderPreview()}

          <div className="flex items-start gap-2.5 p-3 bg-red-50 border border-red-100 rounded-xl">
            <TriangleAlert size={14} className="text-red-500 flex-shrink-0 mt-0.5" />
            <p className="text-[11px] text-red-600 font-medium leading-relaxed">{description}</p>
          </div>
        </div>

        {/* ── Boutons ───────────────────────────────────────────── */}
        <div className="flex gap-2.5 px-6 pb-6">
          <Button variant="secondary" onClick={onClose} disabled={loading} className="flex-1">
            Annuler
          </Button>
          <Button
            variant="danger"
            icon={Trash2}
            loading={loading}
            onClick={handleConfirm}
            className="flex-1"
          >
            {confirmLabel}
          </Button>
        </div>
      </div>
    </div>
  )
}

export default DeleteConfirmModal
