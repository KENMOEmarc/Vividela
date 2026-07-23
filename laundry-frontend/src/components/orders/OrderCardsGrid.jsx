import { useState, useMemo } from 'react'
import { Plus, Search, ShoppingCart } from 'lucide-react'
import Button from '../common/Button'
import OrderCard from './OrderCard'

/**
 * Grille de cartes représentant les commandes en cours.
 * Remplace l'ancien affichage en tableau (DataTable). La sélection d'une
 * carte est déléguée au parent (`onSelectOrder`) qui affiche les articles
 * de la commande sélectionnée.
 */
const OrderCardsGrid = ({
  orders = [],
  loading,
  onSelectOrder,
  onAdd,
  onEdit,
  onDelete,
  onAddItems,
  onPayments,
  onCancel,
  canAdd = true,
  canEdit = true,
  canDelete = true,
  canAddItems = true,
}) => {
  const [search, setSearch] = useState('')

  const filtered = useMemo(() => {
    if (!search.trim()) return orders
    const q = search.trim().toLowerCase()
    return orders.filter((o) =>
      [o.customerName, o.customerLastName, o.customerEmail, o.status]
        .filter(Boolean)
        .some((field) => field.toLowerCase().includes(q))
    )
  }, [orders, search])

  return (
    <div className="bg-white rounded-2xl border border-slate-100 overflow-hidden shadow-sm">
      {/* En-tête : titre, recherche, bouton Ajouter */}
      <div className="flex flex-wrap items-center justify-between gap-3 px-5 py-4 border-b border-slate-50">
        <h2 className="text-sm font-bold text-slate-800">Commandes en cours</h2>

        <div className="flex items-center gap-2">
          <div className="relative">
            <Search size={14} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
            <input
              type="text"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              placeholder="Rechercher une commande…"
              className="pl-8 pr-3 py-2 text-xs rounded-xl border border-slate-200 focus:outline-none focus:ring-2 focus:ring-cyan-100 focus:border-cyan-300 w-48 sm:w-64"
            />
          </div>
          {canAdd && (
            <Button type="button" icon={Plus} size="md" onClick={() => onAdd?.()}>
              Ajouter
            </Button>
          )}
        </div>
      </div>

      {/* Contenu */}
      <div className="p-5">
        {loading ? (
          <div className="flex items-center justify-center py-16 text-slate-400 text-sm gap-2">
            <span className="w-4 h-4 border-2 border-current border-t-transparent rounded-full animate-spin" />
            Chargement des commandes…
          </div>
        ) : filtered.length === 0 ? (
          <div className="flex flex-col items-center justify-center py-16 text-slate-400 gap-2">
            <ShoppingCart size={28} className="text-slate-300" />
            <p className="text-sm">Aucune commande à afficher.</p>
          </div>
        ) : (
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
            {filtered.map((order) => (
              <OrderCard
                key={order.id}
                order={order}
                onSelect={onSelectOrder}
                onEdit={canEdit ? onEdit : null}
                onDelete={canDelete ? onDelete : null}
                onAddItems={canAddItems ? onAddItems : null}
                onPayments={onPayments}
                onCancel={canEdit ? onCancel : null}
                canEdit={canEdit}
                canDelete={canDelete}
                canAddItems={canAddItems}
              />
            ))}
          </div>
        )}
      </div>
    </div>
  )
}

export default OrderCardsGrid
