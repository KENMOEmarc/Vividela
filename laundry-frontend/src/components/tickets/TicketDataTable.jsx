import { Ticket, Receipt, Loader2 } from 'lucide-react'
import DataTable from '../common/DataTable'
import ActionButton from '../common/ActionButton'
import { TICKET_TABLE_COLUMNS } from '../../types/TicketsType'

const TicketDataTable = ({
  orders = [],
  loading,
  onGenerateTicket,
  onGenerateReceipt,
  ticketLoadingId  = null,
  receiptLoadingId = null,
}) => (
  <div className="bg-white rounded-2xl border border-slate-100 overflow-hidden shadow-sm">

    <div className="flex items-center justify-between px-5 py-4 border-b border-slate-50">
      <h2 className="text-sm font-bold text-slate-800">Commandes éligibles</h2>
    </div>

    <DataTable
      columns={TICKET_TABLE_COLUMNS}
      data={orders}
      loading={loading}
      searchFields={['ticketNumber', 'userName', 'customerName', 'customerLastName', 'customerEmail']}
      searchPlaceholder="Rechercher par n° de ticket ou nom d'utilisateur…"
      extraActions={(order) => {
        const hasItems = (order.itemCount ?? 0) > 0
        // CORRECTION : TicketServiceImpl.generateReceiptPdf() exige désormais
        // paymentStatus == COMPLETED. On désactive donc le bouton en amont
        // plutôt que de laisser l'utilisateur se heurter systématiquement à
        // une erreur 400/409 après clic.
        const isPaid = order.paymentStatus === 'COMPLETED'
        const isGeneratingTicket  = ticketLoadingId  === order.id
        const isGeneratingReceipt = receiptLoadingId === order.id

        return (
          <>
            <ActionButton
              icon={isGeneratingTicket ? Loader2 : Ticket}
              label={hasItems ? 'Générer le ticket' : "Ajoutez d'abord un vêtement à la commande"}
              variant="view"
              disabled={!hasItems || isGeneratingTicket}
              className={isGeneratingTicket ? 'animate-pulse' : ''}
              onClick={() => onGenerateTicket(order)}
            />
            <ActionButton
              icon={isGeneratingReceipt ? Loader2 : Receipt}
              label={
                !hasItems ? "Ajoutez d'abord un vêtement à la commande"
                  : !isPaid ? 'La commande doit être intégralement payée pour générer le reçu'
                  : 'Générer le reçu'
              }
              variant="add"
              disabled={!hasItems || !isPaid || isGeneratingReceipt}
              className={isGeneratingReceipt ? 'animate-pulse' : ''}
              onClick={() => onGenerateReceipt(order)}
            />
          </>
        )
      }}
      emptyMessage="Aucune commande enregistrée."
    />
  </div>
)

export default TicketDataTable
