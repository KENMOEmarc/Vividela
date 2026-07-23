import { toast } from 'react-toastify'
import { X } from 'lucide-react'
import FormModal from '../common/FormModal'
import { createArticle, updateArticle } from '../../api/articleApi'
import { addOrderItem } from '../../api/orderApi'
import { getArticleFormFields } from '../../types/ArticlesType'
import { validateArticleForm } from '../../validations/articleValidations'

const parseColorValue = (color) => {
  if (!color) return '#000000'
  if (typeof color === 'string') return color.startsWith('#') ? color : `#${color}`
  if (typeof color === 'object' && typeof color.red === 'number') {
    return `#${[color.red, color.green, color.blue]
      .map((value) => value.toString(16).padStart(2, '0'))
      .join('')}`
  }
  return '#000000'
}

const ArticleFormModal = ({ article, orderId, onSave, onClose }) => {
  const isEdit = Boolean(article)
  const mode = isEdit ? 'edit' : 'create'

  const fields = getArticleFormFields(mode)
  const initialData = {
    clothingType: article?.clothingType ?? '',
    size: article?.size ?? '',
    fabric: article?.fabric ?? '',
    color: parseColorValue(article?.color),
    distinction: article?.distinction ?? '',
    services: article?.services?.map((s) => s.service) ?? [],
    status: article?.status ?? 'DRAFT',
  }

  const handleSubmit = async (formData) => {
    const payload = {
      ...formData,
    }

    try {
      if (isEdit) {
        await updateArticle(article.id, payload)
        toast.success('Article mis à jour')
      } else {
        if (orderId) {
          await addOrderItem(orderId, payload)
          toast.success('Article ajouté à la commande')
        } else {
          await createArticle(payload)
          toast.success('Article créé avec succès')
        }
      }
      onSave()
    } catch (error) {
      toast.error(error.response?.data?.message || 'Une erreur est survenue')
      throw error
    }
  }

  return (
    <FormModal
      isOpen
      title={isEdit ? 'Modifier l\'article' : 'Nouvel article'}
      subtitle={isEdit ? `Édition · ${article?.title}` : orderId ? `Ajouter un article à la commande N°${orderId}` : 'Remplissez les informations de l\'article'}
      fields={fields}
      initialData={initialData}
      mode={mode}
      onSubmit={handleSubmit}
      onClose={onClose}
      validate={validateArticleForm}
      renderHeader={(formData, close, loading) => (
        <div className="relative overflow-hidden rounded-t-2xl bg-gradient-to-br from-slate-800 to-slate-900 px-6 pt-6 pb-5">
          <div className="absolute -right-4 -top-4 w-24 h-24 rounded-full bg-white/5" />
          <div className="absolute right-8 bottom-0 w-12 h-12 rounded-full bg-white/5" />
          <div className="relative flex items-start justify-between gap-3">
            <div>
              <h2 className="font-bold text-white text-[15px] leading-tight">
                {isEdit ? 'Modifier l\'article' : 'Nouvel article'}
              </h2>
              <p className="text-[11px] text-slate-400 mt-0.5">
                {isEdit ? `Édition · ${article?.title}` : 'Remplissez les informations de l\'article'}
              </p>
            </div>
            <button
              onClick={close}
              disabled={loading}
              className="p-1.5 text-slate-500 hover:text-white hover:bg-white/10 rounded-lg transition-colors mt-0.5 flex-shrink-0"
            >
              <X size={16} />
            </button>
          </div>
        </div>
      )}
    />
  )
}

export default ArticleFormModal
