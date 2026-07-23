import { Trash2 } from 'lucide-react'
import Button from '../common/Button'
import DeleteConfirmModal from '../common/DeleteConfirmModal'

const ArticleConfirmDeleteModal = ({
  article,
  isOpen,
  isLoading,
  onConfirm,
  onCancel,
}) => (
  <DeleteConfirmModal
    isOpen={isOpen}
    title="Supprimer l'article"
    message={`Êtes-vous sûr de vouloir supprimer l'article "${article?.title}" ? Cette action est irréversible.`}
    itemName={article?.title}
    isLoading={isLoading}
    onConfirm={onConfirm}
    onCancel={onCancel}
    icon={Trash2}
    dangerColor="from-red-500 to-red-600"
  />
)

export default ArticleConfirmDeleteModal
