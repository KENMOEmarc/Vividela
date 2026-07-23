/**
 * OrderPaymentsModal.jsx — Gestion des paiements d'une commande.
 *
 * AJOUT : cette modale n'existait pas côté frontend alors que le backend
 * expose déjà tout le cycle de vie d'un paiement (PaymentController /
 * PaymentServiceImpl). Sans elle, aucune commande ne pouvait jamais
 * atteindre paymentStatus=COMPLETED (réservé à la réconciliation
 * automatique), ce qui bloquait aussi silencieusement la génération du
 * reçu (TicketServiceImpl.generateReceiptPdf exige ce statut).
 *
 * Flux : on enregistre un paiement (PENDING), puis on le confirme (→
 * COMPLETED, avec réconciliation automatique du paiement de la commande
 * dès que le montant net dû est couvert) ou on le marque comme échoué.
 */

import { useState, useEffect, useCallback } from 'react'
import { toast } from 'react-toastify'
import {
  X, CreditCard, Loader2, Plus, Check, Ban, Coins, TriangleAlert,
} from 'lucide-react'
import {
  recordPayment, confirmPayment, failPayment, getPaymentsByOrder,
} from '../../api/paymentApi'
import {
  PAYMENT_METHOD_LABELS, PAYMENT_STATUS_LABELS, PAYMENT_STATUS_BADGE,
  ORDER_PAYMENT_BADGE, ORDER_PAYMENT_LABELS,
} from '../../types/OrdersType'

const formatAmount = (value) =>
  new Intl.NumberFormat('fr-FR', {
    style: 'currency',
    currency: 'XAF',
    maximumFractionDigits: 0,
  }).format(value ?? 0)

const PAYMENT_METHODS = Object.keys(PAYMENT_METHOD_LABELS)

const OrderPaymentsModal = ({ order, onClose, onOrderChanged }) => {
  const [payments, setPayments]     = useState([])
  const [loading, setLoading]       = useState(true)
  const [submitting, setSubmitting] = useState(false)
  const [busyId, setBusyId]         = useState(null)

  const [amount, setAmount]                 = useState('')
  const [method, setMethod]                 = useState('CASH')
  const [payerPhone, setPayerPhone]         = useState('')
  const [transactionReference, setReference] = useState('')

  const netAmountDue = order?.netAmountDue ?? order?.totalAmount ?? 0

  const load = useCallback(async () => {
    if (!order?.id) return
    setLoading(true)
    try {
      const res = await getPaymentsByOrder(order.id)
      setPayments(res.data?.data ?? [])
    } catch {
      toast.error('Impossible de charger les paiements de la commande')
    } finally {
      setLoading(false)
    }
  }, [order?.id])

  useEffect(() => { load() }, [load])

  const handleRecord = async (e) => {
    e.preventDefault()
    const parsedAmount = parseFloat(amount)
    if (!amount || isNaN(parsedAmount) || parsedAmount <= 0) {
      toast.error('Le montant du paiement doit être strictement positif.')
      return
    }
    setSubmitting(true)
    try {
      await recordPayment({
        orderId: order.id,
        paymentMethod: method,
        amount: parsedAmount,
        payerPhone: payerPhone.trim() || null,
        transactionReference: transactionReference.trim() || null,
      })
      toast.success('Paiement enregistré (en attente de confirmation)')
      setAmount('')
      setPayerPhone('')
      setReference('')
      await load()
      onOrderChanged?.()
    } catch (error) {
      toast.error(error.response?.data?.message || "Impossible d'enregistrer le paiement")
    } finally {
      setSubmitting(false)
    }
  }

  const handleConfirm = async (payment) => {
    setBusyId(payment.id)
    try {
      await confirmPayment(payment.id)
      toast.success('Paiement confirmé')
      await load()
      onOrderChanged?.()
    } catch (error) {
      toast.error(error.response?.data?.message || 'Impossible de confirmer ce paiement')
    } finally {
      setBusyId(null)
    }
  }

  const handleFail = async (payment) => {
    setBusyId(payment.id)
    try {
      await failPayment(payment.id)
      toast.success('Paiement marqué comme échoué')
      await load()
      onOrderChanged?.()
    } catch (error) {
      toast.error(error.response?.data?.message || 'Impossible de marquer ce paiement comme échoué')
    } finally {
      setBusyId(null)
    }
  }

  if (!order) return null

  const orderPaymentCls = ORDER_PAYMENT_BADGE[order.paymentStatus] || 'bg-slate-100 text-slate-500 ring-slate-200'

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 backdrop-blur-sm p-4">
      <div className="w-full max-w-2xl max-h-[90vh] overflow-y-auto rounded-3xl shadow-xl bg-white">
        {/* En-tête */}
        <div className="relative overflow-hidden rounded-t-3xl bg-gradient-to-br from-slate-800 to-slate-900 px-6 pt-6 pb-5">
          <div className="absolute -right-4 -top-4 w-24 h-24 rounded-full bg-white/5" />
          <div className="relative flex items-start justify-between gap-3">
            <div>
              <h2 className="font-bold text-white text-[15px] leading-tight flex items-center gap-2">
                <CreditCard size={16} /> Paiements — Commande #{order.id}
              </h2>
              <p className="text-[11px] text-slate-400 mt-0.5">
                Montant net dû : <span className="text-slate-200 font-semibold">{formatAmount(netAmountDue)}</span>
              </p>
            </div>
            <button
              type="button"
              onClick={onClose}
              className="p-1.5 text-slate-200 hover:text-white hover:bg-white/10 rounded-lg transition-colors mt-0.5 flex-shrink-0"
            >
              <X size={16} />
            </button>
          </div>
          <div className="relative mt-3">
            <span className={`inline-flex items-center px-2.5 py-1 rounded-full text-[10px] font-semibold ring-1 ${orderPaymentCls}`}>
              Statut de la commande : {ORDER_PAYMENT_LABELS[order.paymentStatus] || order.paymentStatus}
            </span>
          </div>
        </div>

        <div className="p-6 space-y-5">
          {/* Formulaire d'ajout de paiement */}
          {order.paymentStatus === 'COMPLETED' ? (
            <div className="flex items-start gap-2 rounded-lg border border-emerald-200 bg-emerald-50 p-3 text-[11px] text-emerald-700">
              <Check size={14} className="flex-shrink-0 mt-0.5" />
              Cette commande est intégralement payée. Aucun nouveau paiement n'est nécessaire.
            </div>
          ) : (
            <form onSubmit={handleRecord} className="rounded-lg border border-slate-200 bg-slate-50/60 p-3 space-y-2.5">
              <h3 className="text-[10px] font-bold uppercase tracking-wider text-slate-400 mb-1">Enregistrer un paiement</h3>
              <div className="grid grid-cols-2 gap-2">
                <div>
                  <label className="mb-1 block text-[11px] font-semibold text-slate-700">Montant (FCFA)</label>
                  <input
                    type="number"
                    min="0.01"
                    step="0.01"
                    value={amount}
                    onChange={(e) => setAmount(e.target.value)}
                    placeholder="0"
                    className="w-full px-2.5 py-2 border border-slate-300 rounded-lg text-xs focus:outline-none focus:ring-2 focus:ring-indigo-100 focus:border-indigo-400"
                    disabled={submitting}
                  />
                </div>
                <div>
                  <label className="mb-1 block text-[11px] font-semibold text-slate-700">Moyen de paiement</label>
                  <select
                    value={method}
                    onChange={(e) => setMethod(e.target.value)}
                    className="w-full px-2.5 py-2 border border-slate-300 rounded-lg text-xs focus:outline-none focus:ring-2 focus:ring-indigo-100 focus:border-indigo-400 appearance-none"
                    disabled={submitting}
                  >
                    {PAYMENT_METHODS.map((m) => (
                      <option key={m} value={m}>{PAYMENT_METHOD_LABELS[m]}</option>
                    ))}
                  </select>
                </div>
                <div>
                  <label className="mb-1 block text-[11px] font-semibold text-slate-700">Téléphone payeur</label>
                  <input
                    type="text"
                    value={payerPhone}
                    onChange={(e) => setPayerPhone(e.target.value)}
                    placeholder="Optionnel"
                    className="w-full px-2.5 py-2 border border-slate-300 rounded-lg text-xs focus:outline-none focus:ring-2 focus:ring-indigo-100 focus:border-indigo-400"
                    disabled={submitting}
                  />
                </div>
                <div>
                  <label className="mb-1 block text-[11px] font-semibold text-slate-700">Référence transaction</label>
                  <input
                    type="text"
                    value={transactionReference}
                    onChange={(e) => setReference(e.target.value)}
                    placeholder="Optionnel (unique)"
                    className="w-full px-2.5 py-2 border border-slate-300 rounded-lg text-xs focus:outline-none focus:ring-2 focus:ring-indigo-100 focus:border-indigo-400"
                    disabled={submitting}
                  />
                </div>
              </div>
              <button
                type="submit"
                disabled={submitting}
                className="w-full flex items-center justify-center gap-1.5 px-3 py-2 rounded-lg text-xs font-semibold bg-indigo-600 text-white hover:bg-indigo-700 disabled:bg-slate-200 disabled:text-slate-400 transition-colors"
              >
                {submitting ? <Loader2 size={12} className="animate-spin" /> : <Plus size={12} />}
                Enregistrer le paiement
              </button>
            </form>
          )}

          {/* Liste des paiements */}
          <div>
            <h3 className="text-[10px] font-bold uppercase tracking-wider text-slate-400 mb-2">Historique des paiements</h3>
            {loading ? (
              <div className="flex items-center justify-center py-8 text-slate-400 text-xs gap-2">
                <Loader2 size={14} className="animate-spin" /> Chargement…
              </div>
            ) : payments.length === 0 ? (
              <div className="flex flex-col items-center justify-center py-8 text-slate-400 gap-1.5">
                <Coins size={22} className="text-slate-300" />
                <p className="text-xs">Aucun paiement enregistré pour cette commande.</p>
              </div>
            ) : (
              <div className="space-y-2">
                {payments.map((p) => {
                  const cls = PAYMENT_STATUS_BADGE[p.status] || 'bg-slate-100 text-slate-500 ring-slate-200'
                  return (
                    <div key={p.id} className="flex items-center justify-between gap-2 rounded-lg border border-slate-200 bg-white p-3">
                      <div className="min-w-0">
                        <p className="text-xs font-semibold text-slate-700">
                          {formatAmount(p.amount)} <span className="text-slate-400 font-normal">· {PAYMENT_METHOD_LABELS[p.paymentMethod] || p.paymentMethod}</span>
                        </p>
                        <p className="text-[10px] text-slate-400">
                          {p.paidAt ? new Date(p.paidAt).toLocaleString('fr-FR') : '—'}
                          {p.transactionReference && ` · Réf. ${p.transactionReference}`}
                        </p>
                      </div>
                      <div className="flex items-center gap-2 flex-shrink-0">
                        <span className={`inline-flex items-center px-2 py-0.5 rounded-full text-[10px] font-semibold ring-1 ${cls}`}>
                          {PAYMENT_STATUS_LABELS[p.status] || p.status}
                        </span>
                        {p.status === 'PENDING' && (
                          <>
                            <button
                              type="button"
                              title="Confirmer"
                              onClick={() => handleConfirm(p)}
                              disabled={busyId === p.id}
                              className="p-1.5 rounded-lg text-slate-400 hover:text-emerald-600 hover:bg-emerald-50 disabled:opacity-50"
                            >
                              {busyId === p.id ? <Loader2 size={13} className="animate-spin" /> : <Check size={13} />}
                            </button>
                            <button
                              type="button"
                              title="Marquer comme échoué"
                              onClick={() => handleFail(p)}
                              disabled={busyId === p.id}
                              className="p-1.5 rounded-lg text-slate-400 hover:text-red-600 hover:bg-red-50 disabled:opacity-50"
                            >
                              <Ban size={13} />
                            </button>
                          </>
                        )}
                      </div>
                    </div>
                  )
                })}
              </div>
            )}
            <p className="mt-3 flex items-start gap-1.5 text-[10px] text-slate-400">
              <TriangleAlert size={11} className="flex-shrink-0 mt-0.5" />
              Le statut de paiement de la commande passe automatiquement à « Payé » dès que la somme des paiements confirmés couvre le montant net dû.
            </p>
          </div>
        </div>
      </div>
    </div>
  )
}

export default OrderPaymentsModal
