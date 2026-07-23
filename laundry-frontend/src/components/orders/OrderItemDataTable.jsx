import { Plus, Trash2 } from 'lucide-react'
import DataTable from '../common/DataTable'
import Button from '../common/Button'
import { ORDER_ITEM_TABLE_COLUMNS } from '../../types/OrderItemType'

const OrderItemDataTable = ({
  items = [],
  loading,
  onAdd,
  onEdit,
  onDelete,
  canAdd    = true,
  canEdit   = true,
  canDelete = true,
}) => (
  <div className="bg-white rounded-2xl border border-slate-100 overflow-hidden shadow-sm">

    {/* En-tête avec titre et bouton Ajouter */}
    <div className="flex items-center justify-between px-5 py-4 border-b border-slate-50">
      <div>
        <h2 className="text-sm font-bold text-slate-800">Articles de la commande</h2>
      </div>
      {canAdd && (
        <Button icon={Plus} size="md" onClick={onAdd}>
          Ajouter un article
        </Button>
      )}
    </div>

    <DataTable
      columns={ORDER_ITEM_TABLE_COLUMNS}
      data={items}
      loading={loading}
      searchFields={['article.title']}
      searchPlaceholder="Rechercher un article…"
      onEdit={canEdit ? onEdit : null}
      onDelete={canDelete ? onDelete : null}
      emptyMessage="Aucun article dans cette commande."
    />
  </div>
)

export default OrderItemDataTable
