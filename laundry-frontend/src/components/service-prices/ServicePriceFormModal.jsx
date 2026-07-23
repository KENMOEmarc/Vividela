import { toast } from 'react-toastify'
import { X, Coins } from 'lucide-react'
import FormModal from '../common/FormModal'
import { createServicePrice, updateServicePrice } from '../../api/servicePriceApi'
import { getServicePriceFormFields } from '../../types/servicePriceTypes'
import { validateServicePriceForm } from '../../validations/servicePriceValidations'
import { CLOTHING_TYPE_LABELS, SERVICE_TYPE_LABELS } from '../../utils/constants'

const ServicePriceFormModal = ({ servicePrice, onSave, onClose }) => {
  const isEdit = Boolean(servicePrice)
  const mode = isEdit ? 'edit' : 'create'

  const fields = getServicePriceFormFields(mode)
  const initialData = {
    clothingType: servicePrice?.clothingType ?? '',
    service: servicePrice?.service ?? '',
    price: servicePrice?.price != null ? String(servicePrice.price) : '',
    active: servicePrice?.active ?? true,
  }

  const subtitle = isEdit
    ? `${CLOTHING_TYPE_LABELS[servicePrice?.clothingType] ?? servicePrice?.clothingType} · ${SERVICE_TYPE_LABELS[servicePrice?.service] ?? servicePrice?.service}`
    : 'Définissez le prix appliqué pour un vêtement et un service donnés'

  const handleSubmit = async (formData) => {
    try {
      if (isEdit) {
        const payload = {
          price: parseFloat(formData.price),
          active: !!formData.active,
        }
        await updateServicePrice(servicePrice.id, payload)
        toast.success('Tarif mis à jour')
      } else {
        const payload = {
          clothingType: formData.clothingType,
          service: formData.service,
          price: parseFloat(formData.price),
          active: formData.active === '' ? true : !!formData.active,
        }
        await createServicePrice(payload)
        toast.success('Tarif créé avec succès')
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
      title={isEdit ? 'Modifier le tarif' : 'Nouveau tarif de service'}
      subtitle={subtitle}
      fields={fields}
      initialData={initialData}
      mode={mode}
      onSubmit={handleSubmit}
      onClose={onClose}
      validate={validateServicePriceForm}
      renderHeader={(formData, close, loading) => (
        <div className="relative overflow-hidden rounded-t-2xl bg-gradient-to-br from-slate-800 to-slate-900 px-6 pt-6 pb-5">
          <div className="absolute -right-4 -top-4 w-24 h-24 rounded-full bg-white/5" />
          <div className="absolute right-8 bottom-0 w-12 h-12 rounded-full bg-white/5" />
          <div className="relative flex items-start justify-between gap-3">
            <div className="flex items-center gap-3.5">
              <div className="w-11 h-11 rounded-xl bg-white/10 flex items-center justify-center flex-shrink-0 ring-1 ring-white/10">
                <Coins size={20} className="text-white/70" />
              </div>
              <div>
                <h2 className="font-bold text-white text-[15px] leading-tight">
                  {isEdit ? 'Modifier le tarif' : 'Nouveau tarif de service'}
                </h2>
                <p className="text-[11px] text-slate-400 mt-0.5">{subtitle}</p>
              </div>
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

export default ServicePriceFormModal
