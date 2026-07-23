import { useState, useEffect, useCallback } from 'react'
import { toast } from 'react-toastify'
import { getAllProducts, deleteProduct } from '../api/productApi'
import ProductDataTable      from '../components/products/ProductDataTable'
import ProductFormModal      from '../components/products/ProductFormModal'
import ConfirmDeleteModal    from '../components/products/ConfirmDeleteModal'
import { useAuth }           from '../context/AuthContext'
import { Package, Layers }   from 'lucide-react'

const ProductsPage = () => {
  const { user } = useAuth()
  const [products,     setProducts]     = useState([])
  const [loading,      setLoading]      = useState(true)
  const [showForm,     setShowForm]     = useState(false)
  const [editProduct,  setEditProduct]  = useState(null)
  const [deleteTarget, setDeleteTarget] = useState(null)

  // Tous les utilisateurs authentifiés peuvent gérer les produits
  const canManage = !!user

  const loadProducts = useCallback(async () => {
    setLoading(true)
    try {
      const res = await getAllProducts()
      setProducts(res.data?.data ?? [])
    } catch {
      toast.error('Impossible de charger les produits')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => { loadProducts() }, [loadProducts])

  const handleAdd    = () => { setEditProduct(null); setShowForm(true) }
  const handleEdit   = (p) => { setEditProduct(p);   setShowForm(true) }
  const handleSaved  = () => { setShowForm(false); setEditProduct(null); loadProducts() }
  const handleDelete = (p) => setDeleteTarget(p)

  const handleDeleteConfirm = async () => {
    try {
      await deleteProduct(deleteTarget.id)
      toast.success(`"${deleteTarget.name}" supprimé`)
      setDeleteTarget(null)
      loadProducts()
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
              <Package size={15} />
            </span>
            Gestion des produits
          </h1>
          <p className="text-xs text-slate-500 mt-0.5 ml-9">
            {canManage
              ? 'Ajoutez, modifiez et gérez les produits de votre catalogue.'
              : 'Vous n’avez pas les droits pour gérer les produits.'
            }
          </p>
        </div>
        {/* Nombre total de produits */}
        <span className="flex-shrink-0 flex items-center gap-1.5 px-2.5 py-1 rounded-lg text-xs font-semibold bg-slate-100 text-slate-600">
          <Layers size={12} />
          {products.length} produit{products.length !== 1 && 's'}
        </span>
      </div>

      <ProductDataTable
        products={products}
        loading={loading}
        onAdd={handleAdd}
        onEdit={handleEdit}
        onDelete={handleDelete}
        canAdd={canManage}
        canEdit={canManage}
        canDelete={canManage}
      />

      {showForm && (
        <ProductFormModal
          product={editProduct}
          onSave={handleSaved}
          onClose={() => { setShowForm(false); setEditProduct(null) }}
        />
      )}

      {deleteTarget && (
        <ConfirmDeleteModal
          product={deleteTarget}
          onConfirm={handleDeleteConfirm}
          onClose={() => setDeleteTarget(null)}
        />
      )}
    </div>
  )
}

export default ProductsPage