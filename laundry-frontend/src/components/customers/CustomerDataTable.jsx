import { Plus } from 'lucide-react'
import DataTable from '../common/DataTable'
import Button from '../common/Button'
import { CUSTOMER_TABLE_COLUMNS } from '../../types/CustomerType'

const CustomerDataTable = ({
  customers = [],
  loading,
  onAdd,
  onEdit,
  onDelete,
  canAdd    = true,
  canEdit   = true,
  canDelete = true,
}) => (
  <div className="bg-white rounded-2xl border border-slate-100 overflow-hidden shadow-sm">
    <div className="flex items-center justify-between px-5 py-4 border-b border-slate-50">
      <div>
        <h2 className="text-sm font-bold text-slate-800">Clients</h2>
      </div>
      {canAdd && (
        <Button icon={Plus} size="md" onClick={onAdd}>
          Ajouter
        </Button>
      )}
    </div>
    <DataTable
      columns={CUSTOMER_TABLE_COLUMNS}
      data={customers}
      loading={loading}
      searchFields={['firstName', 'lastName', 'email', 'phone']}
      searchPlaceholder="Rechercher un client…"
      onEdit={canEdit ? onEdit : null}
      onDelete={canDelete ? onDelete : null}
      emptyMessage="Aucun client enregistré."
    />
  </div>
)

export default CustomerDataTable
