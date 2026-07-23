import { Plus } from 'lucide-react'
import DataTable from '../common/DataTable'
import Button from '../common/Button'
import ActionButton from '../common/ActionButton'
import { ORDER_TABLE_COLUMNS } from '../../types/OrdersType'

const OrderDataTable = ({
  orders = [],
  loading,
  onAdd,
  onEdit,
  onDelete,
  onAddItems,
  canAdd      = true,
  canEdit     = true,
  canDelete   = true,
  canAddItems = true,
}) => (
  <div className="bg-white rounded-2xl border border-slate-100 overflow-hidden shadow-sm">

    {/* En-tête avec titre et bouton Ajouter */}
    <div className="flex items-center justify-between px-5 py-4 border-b border-slate-50">
      <div>
        <h2 className="text-sm font-bold text-slate-800">Commandes</h2>
      </div>
      {canAdd && (
        <Button
          type="button"
          icon={Plus}
          size="md"
          onClick={(e) => {
            e.preventDefault()
            if (typeof onAdd === 'function') onAdd()
          }}
        >
          Ajouter
        </Button>
      )}
    </div>

    <DataTable
      columns={ORDER_TABLE_COLUMNS}
      data={orders}
      loading={loading}
      searchFields={['ticketNumber', 'userName', 'customerName', 'customerLastName', 'customerEmail', 'status']}
      searchPlaceholder="Rechercher par n° de ticket ou nom d'utilisateur…"
      onEdit={canEdit ? onEdit : null}
      onDelete={canDelete ? onDelete : null}
      extraActions={
        canAddItems && typeof onAddItems === 'function'
          ? (order) => (
              <ActionButton
                variant="add"
                label="Ajouter des articles"
                onClick={() => onAddItems(order)}
              />
            )
          : null
      }
      emptyMessage="Aucune commande enregistrée."
    />
  </div>
)

export default OrderDataTable
