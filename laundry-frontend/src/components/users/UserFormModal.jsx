import { toast } from 'react-toastify'
import { X, UserCircle2 } from 'lucide-react'
import { createUser, updateUser } from '../../api/userApi'
import { useAuth } from '../../context/AuthContext'
import FormModal from '../common/FormModal'
import { getUserFormFields, getUserInitials } from '../../types/userTypes'
import { validateUserForm } from '../../validations/userValidations'

const UserFormModal = ({ user, onSave, onClose }) => {
  const { user: currentUser } = useAuth()
  const isEdit  = Boolean(user)
  const mode    = isEdit ? 'edit' : 'create'
  const fields  = getUserFormFields(currentUser?.role, mode)

  const initialData = isEdit
    ? {
        firstName: user.firstName ?? '',
        lastName:  user.lastName  ?? '',
        userName:  user.userName  ?? '',
        email:     user.email     ?? '',
        phone:     user.phone     ?? '',
        password:  '',
        enabled:   user.enabled   ?? true,
        role:      user.role,
      }
    : {}

  const handleSubmit = async (formData) => {
    const payload = {
      ...formData,
      ...(isEdit ? {} : { adminId: currentUser?.id })
    }

    if (isEdit && !payload.password) delete payload.password

    try {
      if (isEdit) {
        await updateUser(user.id, payload)
        toast.success('Utilisateur mis à jour')
      } else {
        await createUser(payload)
        toast.success('Utilisateur créé avec succès')
      }
      onSave()
    } catch (error) {
      toast.error(error.response?.data?.message || 'Une erreur est survenue')
      throw error
    }
  }

  const renderHeader = (formData, close, loading) => {
    const initials = isEdit ? getUserInitials(user) : null
    const subtitle = isEdit
      ? `Édition · ${user.firstName} ${user.lastName}`
      : 'Remplissez les informations du compte'

    return (
      <div className="relative overflow-hidden rounded-t-2xl bg-gradient-to-br from-slate-800 to-slate-900 px-6 pt-6 pb-5">
        <div className="absolute -right-4 -top-4 w-24 h-24 rounded-full bg-white/5" />
        <div className="absolute right-8 bottom-0 w-12 h-12 rounded-full bg-white/5" />
        <div className="relative flex items-start justify-between gap-3">
          <div className="flex items-center gap-3.5">
            <div className="w-11 h-11 rounded-xl bg-white/10 flex items-center justify-center flex-shrink-0 ring-1 ring-white/10">
              {initials
                ? <span className="text-white font-bold text-sm">{initials}</span>
                : <UserCircle2 size={22} className="text-white/70" />
              }
            </div>
            <div>
              <h2 className="font-bold text-white text-[15px] leading-tight">
                {isEdit ? "Modifier l'utilisateur" : 'Nouvel utilisateur'}
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
    )
  }

  return (
    <FormModal
      isOpen
      fields={fields}
      initialData={initialData}
      mode={mode}
      onSubmit={handleSubmit}
      onClose={onClose}
      validate={validateUserForm}
      renderHeader={renderHeader}
    />
  )
}

export default UserFormModal
