import { Plus } from 'lucide-react'
import DataTable from '../common/DataTable'
import Button from '../common/Button'
import { SERVICE_PRICE_TABLE_COLUMNS } from '../../types/servicePriceTypes'

const ServicePriceDataTable = ({
  servicePrices = [],
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
        <h2 className="text-sm font-bold text-slate-800">Tarifs des services</h2>
      </div>
      {canAdd && (
        <Button icon={Plus} size="md" onClick={onAdd}>
          Ajouter un tarif
        </Button>
      )}
    </div>

    <DataTable
      columns={SERVICE_PRICE_TABLE_COLUMNS}
      data={servicePrices}
      loading={loading}
      searchFields={['clothingType', 'service', 'price']}
      searchPlaceholder="Rechercher un tarif…"
      onEdit={canEdit ? onEdit : null}
      onDelete={canDelete ? onDelete : null}
      emptyMessage="Aucun tarif de service enregistré."
    />
  </div>
)

export default ServicePriceDataTable
