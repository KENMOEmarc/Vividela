import { Plus } from 'lucide-react'
import DataTable from '../common/DataTable'
import Button from '../common/Button'
import { PRODUCT_TABLE_COLUMNS } from '../../types/productTypes'

const ProductDataTable = ({
  products = [],
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
        <h2 className="text-sm font-bold text-slate-800">Produits</h2>
      </div>
      {canAdd && (
        <Button icon={Plus} size="md" onClick={onAdd}>
          Ajouter
        </Button>
      )}
    </div>

    <DataTable
      columns={PRODUCT_TABLE_COLUMNS}
      data={products}
      loading={loading}
      searchFields={['name', 'thresholdValue', 'measurementUnit']}
      searchPlaceholder="Rechercher un produit…"
      onEdit={canEdit ? onEdit : null}
      onDelete={canDelete ? onDelete : null}
      emptyMessage="Aucun produit enregistré."
    />
  </div>
)

export default ProductDataTable
