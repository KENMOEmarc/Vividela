import { useState, useEffect, useCallback } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { toast } from 'react-toastify'
import { getCustomerById, getCustomerOrders } from '../api/customerApi'
import OrderDataTable from '../components/orders/OrderDataTable'
import { ArrowLeft, ShoppingCart } from 'lucide-react'

const CustomerOrdersPage = () => {
  const { customerId } = useParams()
  const navigate = useNavigate()
  const [customer, setCustomer] = useState(null)
  const [orders, setOrders] = useState([])
  const [loading, setLoading] = useState(true)

  const loadData = useCallback(async () => {
    setLoading(true)
    try {
      const customerRes = await getCustomerById(customerId)
      setCustomer(customerRes.data?.data)
      const ordersRes = await getCustomerOrders(customerId)
      setOrders(ordersRes.data?.data ?? [])
    } catch {
      toast.error('Impossible de charger les données')
    } finally {
      setLoading(false)
    }
  }, [customerId])

  useEffect(() => { loadData() }, [loadData])

  return (
    <div className="space-y-5">
      <div className="flex items-center gap-3">
        <button
          onClick={() => navigate(-1)}
          className="p-2 hover:bg-slate-100 rounded-lg transition-colors"
        >
          <ArrowLeft size={20} className="text-slate-600" />
        </button>
        <div>
          <h1 className="text-lg font-bold text-slate-900 flex items-center gap-2">
            <span className="w-7 h-7 rounded-lg bg-blue-100 text-blue-600 flex items-center justify-center">
              <ShoppingCart size={15} />
            </span>
            Commandes de {customer?.firstName} {customer?.lastName}
          </h1>
          <p className="text-xs text-slate-500 mt-0.5 ml-9">
            {orders.length} commande{orders.length !== 1 && 's'}
          </p>
        </div>
      </div>

      <OrderDataTable
        orders={orders}
        loading={loading}
        canAdd={false}
        canEdit={false}
        canDelete={false}
      />
    </div>
  )
}

export default CustomerOrdersPage
