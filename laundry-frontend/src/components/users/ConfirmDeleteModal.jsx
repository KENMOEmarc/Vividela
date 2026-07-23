import DeleteConfirmModal from '../common/DeleteConfirmModal'
import { renderUserDeletePreview } from '../../types/userTypes'

const ConfirmDeleteModal = ({ user, onConfirm, onClose }) => (
  <DeleteConfirmModal
    isOpen
    title="Supprimer l'utilisateur"
    description="Toutes les données de ce compte seront supprimées sans possibilité de récupération."
    onConfirm={onConfirm}
    onClose={onClose}
    renderPreview={() => renderUserDeletePreview(user)}
  />
)

export default ConfirmDeleteModal
