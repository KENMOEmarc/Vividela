import { useState, useEffect, useCallback, useMemo } from 'react'
import { toast } from 'react-toastify'
import { Ticket as TicketIcon, Search, Eye, Receipt, Loader2, Inbox } from 'lucide-react'
import { useAuth } from '../context/AuthContext'
import { getCustomerOrders } from '../api/customerApi'
import { getTicket, downloadTicketPdf, downloadReceiptPdf } from '../api/ticketApi'
import { openBlobInNewTab } from '../utils/downloadFile'

const TICKET_STATUS_LABELS = {
  GENERATED:  'Généré',
  DOWNLOADED: 'Téléchargé',
  EXPIRED:    'Expiré',
}

const TICKET_STATUS_BADGE = {
  GENERATED:  'bg-blue-50 text-blue-600 ring-blue-100',
  DOWNLOADED: 'bg-emerald-50 text-emerald-600 ring-emerald-100',
  EXPIRED:    'bg-red-50 text-red-600 ring-red-100',
}

const formatCurrency = (amount) =>
  new Intl.NumberFormat('fr-FR', { style: 'currency', currency: 'XAF', maximumFractionDigits: 0 }).format(amount ?? 0)

/**
 * Page "Tickets" de l'espace client : liste les tickets de dépôt et reçus
 * disponibles pour chaque commande du client connecté.
 */
const CustomerTicketsPage = () => {
  const { user } = useAuth()
  const [rows, setRows] = useState([]) // [{ order, ticket }]
  const [loading, setLoading] = useState(true)
  const [search, setSearch] = useState('')
  const [downloadingKey, setDownloadingKey] = useState(null)

  const loadData = useCallback(async () => {
    if (!user?.id) return
    setLoading(true)
    try {
      const ordersRes = await getCustomerOrders(user.id)
      const orders = ordersRes.data?.data ?? []
      const results = await Promise.allSettled(orders.map((o) => getTicket(o.id)))
      const built = orders
        .map((o, idx) => ({
          order: o,
          ticket: results[idx].status === 'fulfilled' ? results[idx].value.data?.data : null,
        }))
        .filter((r) => r.ticket)
      built.sort((a, b) => new Date(b.order.orderDate ?? b.order.createdAt) - new Date(a.order.orderDate ?? a.order.createdAt))
      setRows(built)
    } catch {
      toast.error('Impossible de charger vos tickets')
    } finally {
      setLoading(false)
    }
  }, [user?.id])

  useEffect(() => { loadData() }, [loadData])

  const filteredRows = useMemo(() => {
    if (!search.trim()) return rows
    const q = search.trim().toLowerCase()
    return rows.filter((r) =>
      [String(r.order.id), r.ticket?.barcode, r.ticket?.status]
        .filter(Boolean)
        .some((f) => f.toLowerCase().includes(q))
    )
  }, [rows, search])

  const handleDownload = async (orderId, kind) => {
    const key = `${orderId}-${kind}`
    setDownloadingKey(key)
    try {
      const res = kind === 'ticket' ? await downloadTicketPdf(orderId) : await downloadReceiptPdf(orderId)
      openBlobInNewTab(res.data)
    } catch {
      toast.error("Impossible de télécharger le document")
    } finally {
      setDownloadingKey(null)
    }
  }

  return (
    <div className="space-y-5">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-2xl font-bold text-slate-900 flex items-center gap-2">
            <TicketIcon size={22} className="text-emerald-600" /> Mes tickets
          </h1>
          <p className="text-sm text-slate-500 mt-1">
            Vos tickets de dépôt et reçus, prêts à consulter ou télécharger
          </p>
        </div>
        <div className="relative">
          <Search size={14} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
          <input
            type="text"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Rechercher un ticket…"
            className="pl-8 pr-3 py-2 text-xs rounded-xl border border-slate-200 focus:outline-none focus:ring-2 focus:ring-emerald-100 focus:border-emerald-300 w-48 sm:w-64"
          />
        </div>
      </div>

      {loading ? (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {[...Array(3)].map((_, i) => (
            <div key={i} className="bg-white rounded-2xl border border-slate-100 p-4 h-40 animate-pulse" />
          ))}
        </div>
      ) : filteredRows.length === 0 ? (
        <div className="flex flex-col items-center justify-center py-16 text-slate-400 gap-2 bg-white rounded-2xl border border-slate-100">
          <Inbox size={28} className="text-slate-300" />
          <p className="text-sm">Aucun ticket disponible pour l'instant.</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {filteredRows.map(({ order, ticket }) => (
            <div key={order.id} className="bg-white rounded-2xl border border-slate-100 p-4 shadow-sm">
              <div className="flex items-center justify-between gap-2 mb-3">
                <div>
                  <p className="text-sm font-semibold text-slate-800">Commande #{order.id}</p>
                  {ticket.barcode && <p className="text-[11px] text-slate-400 font-mono">{ticket.barcode}</p>}
                </div>
                <span className={`flex-shrink-0 inline-flex items-center px-2.5 py-1 rounded-full text-[10px] font-semibold ring-1 ${TICKET_STATUS_BADGE[ticket.status] || 'bg-slate-100 text-slate-500 ring-slate-200'}`}>
                  {TICKET_STATUS_LABELS[ticket.status] || ticket.status}
                </span>
              </div>

              <p className="text-[11px] text-slate-500 mb-3">
                Montant : <span className="font-semibold text-slate-700">{formatCurrency(order.totalAmount)}</span>
              </p>

              <div className="grid grid-cols-2 gap-2">
                <button
                  onClick={() => handleDownload(order.id, 'ticket')}
                  disabled={downloadingKey === `${order.id}-ticket`}
                  className="inline-flex items-center justify-center gap-1.5 text-[11px] font-semibold text-blue-600 hover:text-blue-700 bg-blue-50 hover:bg-blue-100 px-2.5 py-2 rounded-lg transition-colors disabled:opacity-50"
                >
                  {downloadingKey === `${order.id}-ticket` ? <Loader2 size={13} className="animate-spin" /> : <Eye size={13} />}
                  Ticket
                </button>
                <button
                  onClick={() => handleDownload(order.id, 'receipt')}
                  disabled={order.paymentStatus !== 'COMPLETED' || downloadingKey === `${order.id}-receipt`}
                  title={order.paymentStatus !== 'COMPLETED' ? 'Disponible une fois la commande intégralement payée' : undefined}
                  className="inline-flex items-center justify-center gap-1.5 text-[11px] font-semibold text-emerald-600 hover:text-emerald-700 bg-emerald-50 hover:bg-emerald-100 px-2.5 py-2 rounded-lg transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  {downloadingKey === `${order.id}-receipt` ? <Loader2 size={13} className="animate-spin" /> : <Receipt size={13} />}
                  Reçu
                </button>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}

export default CustomerTicketsPage
