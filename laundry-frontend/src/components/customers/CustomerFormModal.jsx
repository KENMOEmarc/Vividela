import { toast } from 'react-toastify'
import { X } from 'lucide-react'
import FormModal from '../common/FormModal'
import { createCustomer, updateCustomer } from '../../api/customerApi'
import { getCustomerFormFields } from '../../types/CustomerType'
import { validateCustomerForm } from '../../validations/customerValidations'

const CustomerFormModal = ({ customer, onSave, onClose }) => {
  const isEdit = Boolean(customer)
  const mode = isEdit ? 'edit' : 'create'

  const fields = getCustomerFormFields()
  const initialData = {
    firstName: customer?.firstName ?? '',
    lastName: customer?.lastName ?? '',
    email: customer?.email ?? '',
    phone: customer?.phone ?? '',
  }

  const handleSubmit = async (formData) => {
    try {
      if (isEdit) {
        await updateCustomer(customer.id, formData)
        toast.success('Client mis à jour')
      } else {
        await createCustomer(formData)
        toast.success('Client créé avec succès')
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
      title={isEdit ? 'Modifier le client' : 'Nouveau client'}
      subtitle={isEdit ? `Édition · ${customer?.firstName} ${customer?.lastName}` : 'Remplissez les informations du client'}
      fields={fields}
      initialData={initialData}
      mode={mode}
      onSubmit={handleSubmit}
      onClose={onClose}
      validate={validateCustomerForm}
      renderHeader={(formData, close, loading) => (
        <div className="relative overflow-hidden rounded-t-2xl bg-gradient-to-br from-emerald-600 to-emerald-800 px-6 pt-6 pb-5">
          <div className="absolute -right-4 -top-4 w-24 h-24 rounded-full bg-white/5" />
          <div className="absolute right-8 bottom-0 w-12 h-12 rounded-full bg-white/5" />
          <div className="relative flex items-start justify-between gap-3">
            <div>
              <h2 className="font-bold text-white text-[15px] leading-tight">
                {isEdit ? 'Modifier le client' : 'Nouveau client'}
              </h2>
              <p className="text-[11px] text-emerald-100 mt-0.5">
                {isEdit ? `Édition · ${customer?.firstName} ${customer?.lastName}` : 'Remplissez les informations du client'}
              </p>
            </div>
            <button
              onClick={close}
              disabled={loading}
              className="p-1.5 text-emerald-200 hover:text-white hover:bg-white/10 rounded-lg transition-colors mt-0.5 flex-shrink-0"
            >
              <X size={16} />
            </button>
          </div>
        </div>
      )}
    />
  )
}

export default CustomerFormModal
