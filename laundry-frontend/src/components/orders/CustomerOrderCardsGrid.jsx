import { useState } from 'react'
import { Package } from 'lucide-react'
import CustomerOrderCard from './CustomerOrderCard'
import CustomerOrderArticlesModal from './CustomerOrderArticlesModal'
import CustomerOrderTicketModal from './CustomerOrderTicketModal'
import CustomerOrderFeedbackModal from './CustomerOrderFeedbackModal'

/**
 * Grille de cartes "commande" pour l'espace client. Les commandes sont
 * toujours affichées sous forme de cartes (jamais en tableau). Chaque carte
 * expose deux actions : voir les articles, ou voir le ticket & le reçu.
 */
const CustomerOrderCardsGrid = ({ orders = [], loading = false, emptyMessage = "Vous n'avez encore passé aucune commande.", highlightOrderId = null }) => {
  const [articlesOrder, setArticlesOrder] = useState(null)
  const [ticketOrder, setTicketOrder] = useState(null)
  const [feedbackOrder, setFeedbackOrder] = useState(null)
  // AJOUT : statuts d'avis mis à jour localement dès qu'un avis est soumis,
  // pour rafraîchir immédiatement le badge de la carte sans recharger toute
  // la liste de commandes depuis le serveur.
  const [localFeedbackStatuses, setLocalFeedbackStatuses] = useState({})

  if (loading) {
    return (
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
        {[...Array(3)].map((_, i) => (
          <div key={i} className="bg-white rounded-2xl border border-slate-100 p-4 h-44 animate-pulse" />
        ))}
      </div>
    )
  }

  if (orders.length === 0) {
    return (
      <div className="flex flex-col items-center justify-center py-14 text-slate-400 gap-2 bg-white rounded-2xl border border-slate-100">
        <Package size={28} className="text-slate-300" />
        <p className="text-sm">{emptyMessage}</p>
      </div>
    )
  }

  return (
    <>
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
        {orders.map((order) => (
          <CustomerOrderCard
            key={order.id}
            order={{ ...order, feedbackStatus: localFeedbackStatuses[order.id] ?? order.feedbackStatus }}
            onViewArticles={setArticlesOrder}
            onViewTicket={setTicketOrder}
            onGiveFeedback={setFeedbackOrder}
            highlighted={highlightOrderId != null && String(order.id) === String(highlightOrderId)}
          />
        ))}
      </div>

      {articlesOrder && (
        <CustomerOrderArticlesModal order={articlesOrder} onClose={() => setArticlesOrder(null)} />
      )}
      {ticketOrder && (
        <CustomerOrderTicketModal order={ticketOrder} onClose={() => setTicketOrder(null)} />
      )}
      {feedbackOrder && (
        <CustomerOrderFeedbackModal
          order={feedbackOrder}
          onClose={() => setFeedbackOrder(null)}
          onSubmitted={(orderId) => setLocalFeedbackStatuses((prev) => ({ ...prev, [orderId]: 'SUBMITTED' }))}
        />
      )}
    </>
  )
}

export default CustomerOrderCardsGrid
