import { useState, useEffect } from 'react'
import { X, Ticket as TicketIcon, Eye, Receipt, Loader2 } from 'lucide-react'
import { getTicket, downloadTicketPdf, downloadReceiptPdf } from '../../api/ticketApi'
import { openBlobInNewTab } from '../../utils/downloadFile'
import { toast } from 'react-toastify'

const TICKET_STATUS_LABELS = {
  GENERATED:  'Généré',
  DOWNLOADED: 'Téléchargé',
  EXPIRED:    'Expiré',
}

/**
 * Modale affichant le ticket de dépôt d'une commande, accessible depuis la
 * carte de commande dans l'espace client (bouton "Ticket & reçu").
 */
const CustomerOrderTicketModal = ({ order, onClose }) => {
  const [ticket, setTicket] = useState(null)
  const [loading, setLoading] = useState(true)
  const [downloadingKind, setDownloadingKind] = useState(null)

  useEffect(() => {
    let active = true
    setLoading(true)
    getTicket(order.id)
      .then((res) => { if (active) setTicket(res.data?.data ?? null) })
      .catch(() => { if (active) setTicket(null) })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [order.id])

  const handleDownload = async (kind) => {
    setDownloadingKind(kind)
    try {
      const res = kind === 'ticket' ? await downloadTicketPdf(order.id) : await downloadReceiptPdf(order.id)
      openBlobInNewTab(res.data)
    } catch {
      toast.error("Impossible de télécharger le document")
    } finally {
      setDownloadingKind(null)
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/40 backdrop-blur-sm">
      <div className="bg-white rounded-2xl shadow-2xl w-full max-w-md max-h-[85vh] flex flex-col">
        <div className="flex items-center justify-between px-6 pt-5 pb-4 border-b border-slate-100 flex-shrink-0">
          <div>
            <h2 className="text-sm font-bold text-slate-900 flex items-center gap-2">
              <TicketIcon size={15} className="text-emerald-500" />
              Ticket &amp; reçu
            </h2>
            <p className="text-xs text-slate-500 mt-0.5">Commande #{order.id}</p>
          </div>
          <button onClick={onClose} className="p-1.5 text-slate-400 hover:text-slate-600 hover:bg-slate-100 rounded-lg">
            <X size={16} />
          </button>
        </div>

        <div className="overflow-y-auto flex-1 px-6 py-5">
          {loading ? (
            <div className="flex justify-center py-10">
              <div className="w-6 h-6 border-2 border-emerald-500 border-t-transparent rounded-full animate-spin" />
            </div>
          ) : ticket ? (
            <div className="space-y-4">
              <div className="bg-slate-50 rounded-xl border border-slate-100 p-4">
                <p className="text-[11px] text-slate-400 uppercase tracking-wide font-semibold mb-1">Statut du ticket</p>
                <p className="text-sm font-semibold text-slate-700">
                  {TICKET_STATUS_LABELS[ticket.status] || ticket.status}
                </p>
                {ticket.barcode && (
                  <p className="text-[11px] text-slate-500 mt-1 font-mono">{ticket.barcode}</p>
                )}
              </div>

              <div className="grid grid-cols-1 gap-2.5">
                <button
                  onClick={() => handleDownload('ticket')}
                  disabled={downloadingKind === 'ticket'}
                  className="inline-flex items-center justify-center gap-2 text-sm font-semibold text-blue-600 hover:text-blue-700 bg-blue-50 hover:bg-blue-100 px-4 py-3 rounded-xl transition-colors disabled:opacity-50"
                >
                  {downloadingKind === 'ticket' ? <Loader2 size={15} className="animate-spin" /> : <Eye size={15} />}
                  Voir le ticket (PDF)
                </button>
                <button
                  onClick={() => handleDownload('receipt')}
                  disabled={order.paymentStatus !== 'COMPLETED' || downloadingKind === 'receipt'}
                  title={order.paymentStatus !== 'COMPLETED' ? 'Disponible une fois la commande intégralement payée' : undefined}
                  className="inline-flex items-center justify-center gap-2 text-sm font-semibold text-emerald-600 hover:text-emerald-700 bg-emerald-50 hover:bg-emerald-100 px-4 py-3 rounded-xl transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  {downloadingKind === 'receipt' ? <Loader2 size={15} className="animate-spin" /> : <Receipt size={15} />}
                  Voir le reçu (PDF)
                </button>
              </div>
              {order.paymentStatus !== 'COMPLETED' && (
                <p className="text-[11px] text-slate-400 text-center">
                  Le reçu sera disponible une fois la commande intégralement payée.
                </p>
              )}
            </div>
          ) : (
            <p className="text-center text-slate-400 text-sm py-8">
              Aucun ticket disponible pour l'instant — il sera généré à la réception de vos articles en boutique.
            </p>
          )}
        </div>

        <div className="px-6 pb-5 pt-1 flex-shrink-0">
          <button
            onClick={onClose}
            className="w-full py-2.5 border border-slate-200 rounded-xl text-sm font-semibold text-slate-600 hover:bg-slate-50 transition-colors"
          >
            Fermer
          </button>
        </div>
      </div>
    </div>
  )
}

export default CustomerOrderTicketModal
