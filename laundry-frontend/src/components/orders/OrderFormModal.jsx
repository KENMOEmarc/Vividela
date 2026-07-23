import { useState } from 'react'
import { toast } from 'react-toastify'
import { X } from 'lucide-react'
import OrderForm from './OrderForm'
import { createOrder, updateOrder } from '../../api/orderApi'

const OrderFormModal = ({ order, onSave, onClose }) => {
  const isEdit = Boolean(order)
  const [isSubmitting, setIsSubmitting] = useState(false)

  const handleSubmit = async (formData) => {
    setIsSubmitting(true)
    try {
      const response = isEdit
        ? await updateOrder(order.id, formData)
        : await createOrder(formData)

      const savedOrder = response?.data?.data ?? response?.data
      toast.success(isEdit ? 'Commande mise à jour' : 'Commande créée avec succès')
      onSave(savedOrder)
    } catch (error) {
      toast.error(error.response?.data?.message || 'Impossible d’enregistrer la commande')
      throw error
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 backdrop-blur-sm p-4">
      <div className="w-full max-w-4xl overflow-hidden rounded-3xl shadow-xl">
        <div className="relative overflow-hidden rounded-t-3xl bg-gradient-to-br from-slate-800 to-slate-900 px-6 pt-6 pb-5">
          <div className="absolute -right-4 -top-4 w-24 h-24 rounded-full bg-white/5" />
          <div className="absolute right-8 bottom-0 w-12 h-12 rounded-full bg-white/5" />
          <div className="relative flex items-start justify-between gap-3">
            <div>
              <h2 className="font-bold text-white text-[15px] leading-tight">
                {isEdit ? 'Modifier la commande' : 'Nouvelle commande'}
              </h2>
              <p className="text-[11px] text-slate-400 mt-0.5">
                {isEdit
                  ? `Édition · Commande #${order?.id}`
                  : 'Créez un lot de vêtements associé à une commande'}
              </p>
            </div>
            <button
              type="button"
              onClick={() => !isSubmitting && onClose()}
              disabled={isSubmitting}
              className="p-1.5 text-slate-200 hover:text-white hover:bg-white/10 rounded-lg transition-colors mt-0.5 flex-shrink-0"
            >
              <X size={16} />
            </button>
          </div>
        </div>

        <div className="bg-white p-6">
          <OrderForm
            mode={isEdit ? 'edit' : 'create'}
            order={order}
            onSubmit={handleSubmit}
            isSubmitting={isSubmitting}
          />
        </div>
      </div>
    </div>
  )
}

export default OrderFormModal
