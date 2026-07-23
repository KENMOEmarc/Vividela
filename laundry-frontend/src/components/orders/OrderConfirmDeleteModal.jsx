import { Trash2 } from 'lucide-react'
import Button from '../common/Button'
import DeleteConfirmModal from '../common/DeleteConfirmModal'

const OrderConfirmDeleteModal = ({
  order,
  isOpen,
  isLoading,
  onConfirm,
  onCancel,
}) => (
  <DeleteConfirmModal
    isOpen={isOpen}
    title="Supprimer la commande"
    message={`Êtes-vous sûr de vouloir supprimer la commande N°${order?.id} de ${order?.customerName} ${order?.customerLastName} ? Cette action est irréversible.`}
    itemName={`N°${order?.id}`}
    isLoading={isLoading}
    onConfirm={onConfirm}
    onCancel={onCancel}
    icon={Trash2}
    dangerColor="from-red-500 to-red-600"
  />
)

export default OrderConfirmDeleteModal
