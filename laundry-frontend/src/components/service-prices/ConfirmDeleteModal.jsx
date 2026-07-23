import DeleteConfirmModal from '../common/DeleteConfirmModal'
import { renderServicePriceDeletePreview } from '../../types/servicePriceTypes'

const ConfirmDeleteModal = ({ servicePrice, onConfirm, onClose }) => (
  <DeleteConfirmModal
    isOpen
    title="Supprimer le tarif"
    description="Ce tarif de service sera supprimé sans possibilité de récupération."
    onConfirm={onConfirm}
    onClose={onClose}
    renderPreview={() => renderServicePriceDeletePreview(servicePrice)}
  />
)

export default ConfirmDeleteModal
