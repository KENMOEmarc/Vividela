import { Trash2 } from 'lucide-react'
import DeleteConfirmModal from '../common/DeleteConfirmModal'

const CustomerConfirmDeleteModal = ({
  customer,
  isOpen,
  isLoading,
  onConfirm,
  onCancel,
}) => (
  <DeleteConfirmModal
    isOpen={isOpen}
    title="Supprimer le client"
    message={`Êtes-vous sûr de vouloir supprimer ${customer?.firstName} ${customer?.lastName} ? Cette action est irréversible.`}
    itemName={`${customer?.firstName} ${customer?.lastName}`}
    isLoading={isLoading}
    onConfirm={onConfirm}
    onCancel={onCancel}
    icon={Trash2}
    dangerColor="from-red-500 to-red-600"
  />
)

export default CustomerConfirmDeleteModal
