import { Plus } from 'lucide-react'
import DataTable from '../common/DataTable'
import Button from '../common/Button'
import { USER_TABLE_COLUMNS } from '../../types/userTypes'

const UserDataTable = ({
  users = [],
  loading,
  onAdd,
  onEdit,
  onDelete,
  canAdd     = true,
  canEdit    = true,
  canDelete  = true,
  canEditRow = null,
}) => (
  <div className="bg-white rounded-2xl border border-slate-100 overflow-hidden shadow-sm">

    {/* En-tête avec titre et bouton Ajouter */}
    <div className="flex items-center justify-between px-5 py-4 border-b border-slate-50">
      <div>
        <h2 className="text-sm font-bold text-slate-800">Utilisateurs</h2>
      </div>
      {canAdd && (
        <Button icon={Plus} size="md" onClick={onAdd}>
          Ajouter
        </Button>
      )}
    </div>

    <DataTable
      columns={USER_TABLE_COLUMNS}
      data={users}
      loading={loading}
      searchFields={['firstName', 'lastName', 'userName', 'email', 'phone']}
      searchPlaceholder="Rechercher un utilisateur…"
      onEdit={canEdit ? onEdit : null}
      onDelete={canDelete ? onDelete : null}
      canEditRow={canEditRow}
      emptyMessage="Aucun utilisateur enregistré."
    />
  </div>
)

export default UserDataTable
