import DeleteConfirmModal from '../common/DeleteConfirmModal'
import { renderProductDeletePreview } from '../../types/productTypes'

const ConfirmDeleteModal = ({ product, onConfirm, onClose }) => (
  <DeleteConfirmModal
    isOpen
    title="Supprimer le produit"
    description="Toutes les données de ce produit seront supprimées sans possibilité de récupération."
    onConfirm={onConfirm}
    onClose={onClose}
    renderPreview={() => renderProductDeletePreview(product)}
  />
)

export default ConfirmDeleteModal
