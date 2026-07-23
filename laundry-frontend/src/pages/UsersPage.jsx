import { useState, useEffect, useCallback } from 'react'
import { toast } from 'react-toastify'
import { getAllUsers, deleteUser } from '../api/userApi'
import UserDataTable      from '../components/users/UserDataTable'
import UserFormModal      from '../components/users/UserFormModal'
import ConfirmDeleteModal from '../components/users/ConfirmDeleteModal'
import { useAuth } from '../context/AuthContext'
import { ROLES, ROLE_LABELS, getRoleRank } from '../utils/constants'
import { Users, ShieldCheck } from 'lucide-react'

const ROLE_PILL = {
  [ROLES.ADMIN]:    'bg-purple-100 text-purple-700',
  [ROLES.MANAGER]:  'bg-amber-100 text-amber-700',
  [ROLES.EMPLOYEE]: 'bg-blue-100 text-blue-700',
  [ROLES.CUSTOMER]: 'bg-emerald-100 text-emerald-700',
}

const UsersPage = () => {
  const { user, isAdmin, isManager, isEmployee } = useAuth()
  const [users,        setUsers]        = useState([])
  const [loading,      setLoading]      = useState(true)
  const [showForm,     setShowForm]     = useState(false)
  const [editUser,     setEditUser]     = useState(null)
  const [deleteTarget, setDeleteTarget] = useState(null)

  // Seuls Admin et Manager peuvent créer un utilisateur : un Employé peut
  // uniquement consulter la liste et modifier les fiches existantes.
  const canAdd    = isAdmin() || isManager()
  const canEdit   = isAdmin() || isManager() || isEmployee()
  const canDelete = isAdmin()

  // Un Manager ou un Employé ne peut jamais modifier un compte ADMIN, ni
  // attribuer/conserver un rôle supérieur au sien (miroir de la règle
  // appliquée côté backend dans UserServiceImpl#update). L'Admin peut tout
  // modifier.
  const canEditRow = (target) =>
    canEdit && getRoleRank(target?.role) <= getRoleRank(user?.role)

  const loadUsers = useCallback(async () => {
    setLoading(true)
    try {
      const res = await getAllUsers()
      setUsers(res.data?.data ?? [])
    } catch {
      toast.error('Impossible de charger les utilisateurs')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => { loadUsers() }, [loadUsers])

  const handleAdd    = () => { setEditUser(null); setShowForm(true) }
  const handleEdit   = (u) => {
    if (!canEditRow(u)) return
    setEditUser(u)
    setShowForm(true)
  }
  const handleSaved  = () => { setShowForm(false); setEditUser(null); loadUsers() }
  const handleDelete = (u) => setDeleteTarget(u)

  const handleDeleteConfirm = async () => {
    try {
      await deleteUser(deleteTarget.id)
      toast.success(`"${deleteTarget.firstName} ${deleteTarget.lastName}" supprimé`)
      setDeleteTarget(null)
      loadUsers()
    } catch {
      toast.error('Erreur lors de la suppression')
    }
  }

  return (
    <div className="space-y-5">

      {/* En-tête de page */}
      <div className="flex items-start justify-between gap-4">
        <div>
          <h1 className="text-lg font-bold text-slate-900 flex items-center gap-2">
            <span className="w-7 h-7 rounded-lg bg-blue-100 text-blue-600 flex items-center justify-center">
              <Users size={15} />
            </span>
            Gestion des utilisateurs
          </h1>
          <p className="text-xs text-slate-500 mt-0.5 ml-9">
            {canDelete
              ? 'Créez, modifiez et gérez les comptes utilisateurs de l\'application.'
              : 'Consultez et modifiez les comptes utilisateurs de l\'application.'
            }
          </p>
        </div>
        {user?.role && (
          <span className={`flex-shrink-0 flex items-center gap-1.5 px-2.5 py-1 rounded-lg text-xs font-semibold ${ROLE_PILL[user.role] ?? 'bg-slate-100 text-slate-600'}`}>
            <ShieldCheck size={12} />
            {ROLE_LABELS[user.role] ?? user.role}
          </span>
        )}
      </div>

      <UserDataTable
        users={users}
        loading={loading}
        onAdd={handleAdd}
        onEdit={handleEdit}
        onDelete={handleDelete}
        canAdd={canAdd}
        canEdit={canEdit}
        canDelete={canDelete}
        canEditRow={canEditRow}
      />

      {showForm && (
        <UserFormModal
          user={editUser}
          onSave={handleSaved}
          onClose={() => { setShowForm(false); setEditUser(null) }}
        />
      )}

      {deleteTarget && (
        <ConfirmDeleteModal
          user={deleteTarget}
          onConfirm={handleDeleteConfirm}
          onClose={() => setDeleteTarget(null)}
        />
      )}
    </div>
  )
}

export default UsersPage
