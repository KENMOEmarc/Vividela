import { useState, useEffect, useCallback } from 'react'
import { toast } from 'react-toastify'
import { getAllServicePrices, deleteServicePrice } from '../api/servicePriceApi'
import ServicePriceDataTable from '../components/service-prices/ServicePriceDataTable'
import ServicePriceFormModal from '../components/service-prices/ServicePriceFormModal'
import ConfirmDeleteModal    from '../components/service-prices/ConfirmDeleteModal'
import ClothingCategoryCards from '../components/service-prices/ClothingCategoryCards'
import CategoryServicesModal from '../components/service-prices/CategoryServicesModal'
import { useAuth }           from '../context/AuthContext'
import { CLOTHING_TYPE_LABELS, SERVICE_TYPE_LABELS } from '../utils/constants'
import { Coins, Layers } from 'lucide-react'

const ServicePricesPage = () => {
  const { hasAnyRole } = useAuth()
  const [servicePrices, setServicePrices] = useState([])
  const [loading,       setLoading]       = useState(true)
  const [showForm,      setShowForm]      = useState(false)
  const [editTarget,    setEditTarget]    = useState(null)
  const [deleteTarget,  setDeleteTarget]  = useState(null)
  const [activeCategory, setActiveCategory] = useState(null)

  // Création / modification : ADMIN et MANAGER (voir ServicePriceController côté backend)
  const canManage = hasAnyRole(['ADMIN', 'MANAGER'])
  // Suppression : ADMIN uniquement
  const canDelete = hasAnyRole(['ADMIN'])

  const loadServicePrices = useCallback(async () => {
    setLoading(true)
    try {
      const res = await getAllServicePrices()
      setServicePrices(res.data?.data ?? [])
    } catch {
      toast.error('Impossible de charger les tarifs de service')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => { loadServicePrices() }, [loadServicePrices])

  const handleAdd    = () => { setEditTarget(null); setShowForm(true) }
  const handleEdit   = (sp) => { setEditTarget(sp);  setShowForm(true) }
  const handleSaved  = () => { setShowForm(false); setEditTarget(null); loadServicePrices() }
  const handleDelete = (sp) => setDeleteTarget(sp)

  const handleDeleteConfirm = async () => {
    try {
      await deleteServicePrice(deleteTarget.id)
      toast.success(
        `Tarif "${CLOTHING_TYPE_LABELS[deleteTarget.clothingType] ?? deleteTarget.clothingType} · ${SERVICE_TYPE_LABELS[deleteTarget.service] ?? deleteTarget.service}" supprimé`
      )
      setDeleteTarget(null)
      loadServicePrices()
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
            <span className="w-7 h-7 rounded-lg bg-indigo-100 text-indigo-600 flex items-center justify-center">
              <Coins size={15} />
            </span>
            Tarifs des services
          </h1>
          <p className="text-xs text-slate-500 mt-0.5 ml-9">
            {canManage
              ? "Définissez le prix de chaque service en fonction du type de vêtement."
              : "Consultez les tarifs appliqués par type de vêtement et par service."
            }
          </p>
        </div>
        {/* Nombre total de tarifs */}
        <span className="flex-shrink-0 flex items-center gap-1.5 px-2.5 py-1 rounded-lg text-xs font-semibold bg-slate-100 text-slate-600">
          <Layers size={12} />
          {servicePrices.length} tarif{servicePrices.length !== 1 && 's'}
        </span>
      </div>

      <ClothingCategoryCards
        servicePrices={servicePrices}
        loading={loading}
        onViewCategory={setActiveCategory}
      />

      {activeCategory && (
        <CategoryServicesModal
          clothingType={activeCategory}
          servicePrices={servicePrices}
          onClose={() => setActiveCategory(null)}
          onEdit={handleEdit}
          onDelete={handleDelete}
          canEdit={canManage}
          canDelete={canDelete}
        />
      )}

      {showForm && (
        <ServicePriceFormModal
          servicePrice={editTarget}
          onSave={handleSaved}
          onClose={() => { setShowForm(false); setEditTarget(null) }}
        />
      )}

      {deleteTarget && (
        <ConfirmDeleteModal
          servicePrice={deleteTarget}
          onConfirm={handleDeleteConfirm}
          onClose={() => setDeleteTarget(null)}
        />
      )}
    </div>
  )
}

export default ServicePricesPage
