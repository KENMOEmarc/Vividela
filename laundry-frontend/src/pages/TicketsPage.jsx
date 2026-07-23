import { useState, useEffect, useCallback } from 'react'
import { toast } from 'react-toastify'
import { getAllOrders } from '../api/orderApi'
import { downloadTicketPdf, downloadReceiptPdf } from '../api/ticketApi'
import { openBlobInNewTab } from '../utils/downloadFile'
import TicketDataTable from '../components/tickets/TicketDataTable'
import { Ticket, Layers } from 'lucide-react'

const TicketsPage = () => {
  const [orders,            setOrders]            = useState([])
  const [loading,           setLoading]           = useState(true)
  const [ticketLoadingId,   setTicketLoadingId]   = useState(null)
  const [receiptLoadingId,  setReceiptLoadingId]  = useState(null)

  const loadOrders = useCallback(async () => {
    setLoading(true)
    try {
      const res = await getAllOrders()
      setOrders(res.data?.data ?? [])
    } catch {
      toast.error('Impossible de charger les commandes')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => { loadOrders() }, [loadOrders])

  const eligibleCount = orders.filter((o) => (o.itemCount ?? 0) > 0).length

  const handleGenerateTicket = async (order) => {
    setTicketLoadingId(order.id)
    try {
      const res = await downloadTicketPdf(order.id)
      openBlobInNewTab(res.data)
      toast.success(`Ticket de la commande N°${order.id} généré`)
    } catch (error) {
      toast.error(error.response?.data?.message || 'Erreur lors de la génération du ticket')
    } finally {
      setTicketLoadingId(null)
    }
  }

  const handleGenerateReceipt = async (order) => {
    setReceiptLoadingId(order.id)
    try {
      const res = await downloadReceiptPdf(order.id)
      openBlobInNewTab(res.data)
      toast.success(`Reçu de la commande N°${order.id} généré`)
    } catch (error) {
      toast.error(error.response?.data?.message || 'Erreur lors de la génération du reçu')
    } finally {
      setReceiptLoadingId(null)
    }
  }

  return (
    <div className="space-y-5">

      {/* En-tête de page */}
      <div className="flex items-start justify-between gap-4">
        <div>
          <h1 className="text-lg font-bold text-slate-900 flex items-center gap-2">
            <span className="w-7 h-7 rounded-lg bg-teal-100 text-teal-600 flex items-center justify-center">
              <Ticket size={15} />
            </span>
            Tickets &amp; reçus
          </h1>
          <p className="text-xs text-slate-500 mt-0.5 ml-9">
            Générez le ticket de dépôt et le reçu d'une commande dès qu'elle possède au moins un vêtement.
          </p>
        </div>
        <span className="flex-shrink-0 flex items-center gap-1.5 px-2.5 py-1 rounded-lg text-xs font-semibold bg-slate-100 text-slate-600">
          <Layers size={12} />
          {eligibleCount} commande{eligibleCount !== 1 && 's'} éligible{eligibleCount !== 1 && 's'}
        </span>
      </div>

      <TicketDataTable
        orders={orders}
        loading={loading}
        onGenerateTicket={handleGenerateTicket}
        onGenerateReceipt={handleGenerateReceipt}
        ticketLoadingId={ticketLoadingId}
        receiptLoadingId={receiptLoadingId}
      />
    </div>
  )
}

export default TicketsPage
