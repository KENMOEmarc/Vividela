import { useState, useEffect, useCallback } from 'react'
import { toast } from 'react-toastify'
import { getAllCustomers, deleteUser } from '../api/userApi'
import { getAllOrders } from '../api/orderApi'
import CustomerDataTable from '../components/customers/CustomerDataTable'
import CustomerFormModal from '../components/customers/CustomerFormModal'
import CustomerConfirmDeleteModal from '../components/customers/CustomerConfirmDeleteModal'
import { useAuth } from '../context/AuthContext'
import { Users, Layers } from 'lucide-react'
import { withCustomerStats } from '../utils/customerStats'

const CustomersPage = () => {
  const { user } = useAuth()
  const [customers, setCustomers] = useState([])
  const [loading, setLoading] = useState(true)
  const [showForm, setShowForm] = useState(false)
  const [editCustomer, setEditCustomer] = useState(null)
  const [deleteTarget, setDeleteTarget] = useState(null)

  const canManage = !!user

  const loadCustomers = useCallback(async () => {
    setLoading(true)
    try {
      // CORRECTION : GET /users/customers ne renvoie pas orderCount/totalSpent
      // (voir utils/customerStats.js) — on charge donc aussi les commandes
      // pour calculer ces statistiques côté client et les fusionner.
      const [customersRes, ordersRes] = await Promise.all([
        getAllCustomers(),
        getAllOrders(),
      ])
      const rawCustomers = customersRes.data?.data ?? []
      const orders = ordersRes.data?.data ?? []
      setCustomers(withCustomerStats(rawCustomers, orders))
    } catch {
      toast.error('Impossible de charger les clients')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => { loadCustomers() }, [loadCustomers])

  const handleAdd = () => { setEditCustomer(null); setShowForm(true) }
  const handleEdit = (c) => { setEditCustomer(c); setShowForm(true) }
  const handleSaved = () => { setShowForm(false); setEditCustomer(null); loadCustomers() }
  const handleDelete = (c) => setDeleteTarget(c)

  const handleDeleteConfirm = async () => {
    try {
      await deleteUser(deleteTarget.id)
      toast.success(`${deleteTarget.firstName} ${deleteTarget.lastName} supprimé`)
      setDeleteTarget(null)
      loadCustomers()
    } catch {
      toast.error('Erreur lors de la suppression')
    }
  }

  return (
    <div className="space-y-5">
      <div className="flex items-start justify-between gap-4">
        <div>
          <h1 className="text-lg font-bold text-slate-900 flex items-center gap-2">
            <span className="w-7 h-7 rounded-lg bg-emerald-100 text-emerald-600 flex items-center justify-center">
              <Users size={15} />
            </span>
            Gestion des clients
          </h1>
          <p className="text-xs text-slate-500 mt-0.5 ml-9">
            {canManage
              ? 'Gérez votre base de clients et consultez leurs commandes.'
              : 'Vous n\'avez pas les droits pour gérer les clients.'
            }
          </p>
        </div>
        <span className="flex-shrink-0 flex items-center gap-1.5 px-2.5 py-1 rounded-lg text-xs font-semibold bg-slate-100 text-slate-600">
          <Layers size={12} />
          {customers.length} client{customers.length !== 1 && 's'}
        </span>
      </div>

      <CustomerDataTable
        customers={customers}
        loading={loading}
        onAdd={handleAdd}
        onEdit={canManage ? handleEdit : null}
        onDelete={canManage ? handleDelete : null}
        canAdd={canManage}
        canEdit={canManage}
        canDelete={canManage}
      />

      {showForm && (
        <CustomerFormModal
          customer={editCustomer}
          onSave={handleSaved}
          onClose={() => setShowForm(false)}
        />
      )}

      {deleteTarget && (
        <CustomerConfirmDeleteModal
          customer={deleteTarget}
          isOpen={!!deleteTarget}
          isLoading={false}
          onConfirm={handleDeleteConfirm}
          onCancel={() => setDeleteTarget(null)}
        />
      )}
    </div>
  )
}

export default CustomersPage
