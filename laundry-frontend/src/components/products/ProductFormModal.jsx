import { toast } from 'react-toastify'
import { X } from 'lucide-react'
import FormModal from '../common/FormModal'
import { createProduct, updateProduct } from '../../api/productApi'
import { getProductFormFields } from '../../types/productTypes'
import { validateProductForm } from '../../validations/productValidations'

const ProductFormModal = ({ product, onSave, onClose }) => {
  const isEdit = Boolean(product)
  const mode = isEdit ? 'edit' : 'create'

  const fields = getProductFormFields(mode)
  const initialData = {
    name: product?.name ?? '',
    thresholdValue:
      product?.thresholdValue != null ? String(product.thresholdValue) : '',
    measurementUnit: product?.measurementUnit ?? '',
  }

  const handleSubmit = async (formData) => {
    const payload = {
      ...formData,
      thresholdValue: parseFloat(formData.thresholdValue),
    }

    try {
      if (isEdit) {
        await updateProduct(product.id, payload)
        toast.success('Produit mis à jour')
      } else {
        await createProduct(payload)
        toast.success('Produit créé avec succès')
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
      title={isEdit ? 'Modifier le produit' : 'Nouveau produit'}
      subtitle={isEdit ? `Édition · ${product?.name}` : 'Remplissez les informations du produit'}
      fields={fields}
      initialData={initialData}
      mode={mode}
      onSubmit={handleSubmit}
      onClose={onClose}
      validate={validateProductForm}
      renderHeader={(formData, close, loading) => (
        <div className="relative overflow-hidden rounded-t-2xl bg-gradient-to-br from-slate-800 to-slate-900 px-6 pt-6 pb-5">
          <div className="absolute -right-4 -top-4 w-24 h-24 rounded-full bg-white/5" />
          <div className="absolute right-8 bottom-0 w-12 h-12 rounded-full bg-white/5" />
          <div className="relative flex items-start justify-between gap-3">
            <div>
              <h2 className="font-bold text-white text-[15px] leading-tight">
                {isEdit ? 'Modifier le produit' : 'Nouveau produit'}
              </h2>
              <p className="text-[11px] text-slate-400 mt-0.5">
                {isEdit ? `Édition · ${product?.name}` : 'Remplissez les informations du produit'}
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

export default ProductFormModal
