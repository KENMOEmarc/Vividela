/**
 * OrderForm.jsx — Formulaire de création/édition d'une commande de pressing.
 *
 * CORRECTION : les deux modes ont désormais des champs et une validation
 * réellement adaptés à ce que le backend attend :
 *
 * - Création (mode="create") : formulaire minimaliste. depositDate, status et
 *   paymentStatus ne sont PAS demandés — OrderServiceImpl.createOrder les fixe
 *   automatiquement (depositDate=aujourd'hui, status=RECEIVED,
 *   paymentStatus=PENDING). Les exiger ici bloquait systématiquement la
 *   création (validateOrderForm renvoyait toujours 3 erreurs sur des champs
 *   absents du formulaire, avant même l'appel API).
 *
 * - Édition (mode="edit") : formulaire complet. Ajout des champs date de
 *   dépôt et statut de paiement, absents du formulaire mais renvoyés quand
 *   même avec des valeurs par défaut ('PENDING', date du jour) — ce qui
 *   écrasait silencieusement un paiement déjà encaissé et la vraie date de
 *   dépôt à chaque modification. Remise et points fidélité sont maintenant
 *   toujours visibles (suppression du repli "options avancées").
 */

import { useState, useEffect, useCallback } from 'react'
import { toast } from 'react-toastify'
import {
  User, Calendar, MapPin, MessageSquare, Tag, CreditCard,
  Loader2, TriangleAlert, Percent, Star,
} from 'lucide-react'
import {
  validateOrderField,
  validateOrderForm,
  PAYMENT_STATUSES_EDITABLE,
} from '../../validations/orderValidations'
import { ORDER_STATUS_LABELS } from '../../types/OrdersType'

// ─── Statuts gérés automatiquement à partir des articles (RECEIVED → PENDING/
// IN_PROGRESS → READY). Le changement manuel n'est proposé qu'une fois ce
// stade dépassé (voir garde-fou côté backend OrderServiceImpl.updateOrder).
const PAST_READY_STATUSES = ['READY', 'DELIVERED', 'CANCELLED']

// CORRECTION : depuis READY, seules DELIVERED et CANCELLED sont des cibles
// valides (machine à états stricte post-READY, OrderServiceImpl.updateOrder).
// READY n'est plus proposé comme cible : le backend rejette désormais toute
// valeur qui n'est pas une transition vers DELIVERED/CANCELLED, y compris
// READY renvoyé inchangé, ce qui faisait échouer la sauvegarde dès qu'on
// rouvrait le formulaire sans modifier le statut.
const MANUAL_STATUS_OPTIONS = ['DELIVERED', 'CANCELLED']

// CORRECTION : une commande livrée, annulée, ou déjà intégralement payée est
// verrouillée côté backend (OrderServiceImpl.updateOrder) — seul le statut de
// paiement (ex: remboursement) reste modifiable, tout le reste est rejeté.
const isOrderLocked = (order) =>
  !!order && (order.status === 'DELIVERED' || order.status === 'CANCELLED' || order.paymentStatus === 'COMPLETED')

// ─── Styles de base compacts ──────────────────────────────────────────────────
const INPUT_BASE = [
  'w-full px-2.5 py-2 border rounded-lg text-xs transition-all duration-150',
  'focus:outline-none focus:ring-2 disabled:bg-slate-50 disabled:cursor-not-allowed',
].join(' ')

const inputClass = (name, formData, fieldErrors) => {
  const hasValue = Boolean(formData[name])
  const hasError = Boolean(fieldErrors[name])
  if (hasError) return `${INPUT_BASE} border-red-400 focus:border-red-400 focus:ring-red-100 bg-red-50`
  if (hasValue) return `${INPUT_BASE} border-emerald-400 focus:border-emerald-400 focus:ring-emerald-100`
  return `${INPUT_BASE} border-slate-300 focus:border-indigo-400 focus:ring-indigo-100`
}

// ─── État initial ──────────────────────────────────────────────────────────────
const buildInitialForm = (order, mode) => {
  if (mode !== 'edit' || !order) {
    // Création : uniquement ce que le backend attend réellement
    // (voir OrderCreateRequest) — depositDate/status/paymentStatus sont
    // fixés automatiquement côté serveur, pas demandés ici.
    return {
      userName:             '',
      expectedDeliveryDate: '',
      shippingAddress:      '',
      notes:                '',
    }
  }
  return {
    userName:             order.userName             || '',
    depositDate:          order.depositDate           || new Date().toISOString().split('T')[0],
    expectedDeliveryDate: order.expectedDeliveryDate || '',
    // CORRECTION : le statut n'est modifiable manuellement qu'une fois le
    // stade READY dépassé (et alors seules DELIVERED/CANCELLED sont des
    // cibles valides — voir MANUAL_STATUS_OPTIONS). On ne pré-remplit donc
    // plus ce champ avec le statut courant : le laisser vide oblige à un
    // choix explicite et évite de renvoyer READY inchangé, que le backend
    // rejette désormais.
    status:               '',
    // CORRECTION : COMPLETED ne peut plus être envoyé manuellement (voir
    // PAYMENT_STATUSES_EDITABLE). On garde la valeur réelle ici pour
    // l'affichage (badge lecture seule si COMPLETED), mais elle est exclue
    // du payload de soumission tant qu'elle reste sur COMPLETED.
    paymentStatus:        order.paymentStatus        || 'PENDING',
    shippingAddress:      order.shippingAddress      || '',
    notes:                order.notes               || '',
    discountAmount:       order.discountAmount != null  ? String(order.discountAmount)       : '0',
    loyaltyPointsUsed:    order.loyaltyPointsUsed  != null ? String(order.loyaltyPointsUsed) : '0',
  }
}

// ─── Sous-composants réutilisables (taille réduite) ────────────────────────────

const FieldError = ({ message }) =>
  message ? (
    <p className="mt-0.5 flex items-center gap-1 text-[10px] text-red-500">
      <TriangleAlert size={10} />
      {message}
    </p>
  ) : null

const SectionTitle = ({ children }) => (
  <h3 className="mb-1.5 text-[10px] font-bold uppercase tracking-wider text-slate-400">
    {children}
  </h3>
)

const FieldWrapper = ({ label, required, children, error, hint, className = '' }) => (
  <div className={className}>
    <label className="mb-1 flex items-center gap-1 text-[11px] font-semibold text-slate-700">
      {label}
      {required && <span className="text-red-500">*</span>}
    </label>
    {children}
    {hint && !error && <p className="mt-0.5 text-[10px] text-slate-400">{hint}</p>}
    <FieldError message={error} />
  </div>
)

// ─── Composant principal ────────────────────────────────────────────────────────
const OrderForm = ({ mode = 'create', order = null, onSubmit, isSubmitting = false }) => {
  const isEdit = mode === 'edit'
  const [formData, setFormData]       = useState(() => buildInitialForm(order, mode))
  const [fieldErrors, setFieldErrors] = useState({})

  // Tant que la commande n'a pas atteint le stade READY, son statut est géré
  // automatiquement selon l'avancement des articles (voir backend) : on ne
  // propose donc le sélecteur de statut qu'à partir de ce stade.
  const canEditStatus = isEdit && PAST_READY_STATUSES.includes(order?.status)

  // CORRECTION : commande livrée / annulée / intégralement payée → verrouillée
  // côté backend (hors statut de paiement). On désactive les champs
  // correspondants et on les exclut du payload plutôt que de laisser
  // l'utilisateur les modifier pour se heurter à un rejet à la soumission.
  const locked = isEdit && isOrderLocked(order)

  useEffect(() => {
    if (isEdit && order) {
      setFormData(buildInitialForm(order, mode))
      setFieldErrors({})
    }
  }, [order, mode, isEdit])

  const handleChange = useCallback((e) => {
    const { name, value } = e.target
    setFormData(prev => ({ ...prev, [name]: value }))
    if (fieldErrors[name]) {
      setFieldErrors(prev => ({ ...prev, [name]: null }))
    }
  }, [fieldErrors])

  const handleBlur = useCallback((e) => {
    const { name, value } = e.target
    const error = validateOrderField(name, value, formData)
    setFieldErrors(prev => ({ ...prev, [name]: error }))
  }, [formData])

  const handleSubmit = async (e) => {
    e.preventDefault()
    const errors = validateOrderForm(formData, mode)
    if (Object.keys(errors).length > 0) {
      setFieldErrors(errors)
      const firstKey = Object.keys(errors)[0]
      document.getElementById(`order-${firstKey}`)?.focus()
      toast.error('Veuillez corriger les erreurs dans le formulaire.')
      return
    }

    const payload = isEdit
      ? {
          // CORRECTION : sur une commande verrouillée (livrée / annulée /
          // payée), le backend rejette toute tentative de modifier ces champs
          // — même en renvoyant leur valeur inchangée. On les omet donc du
          // payload plutôt que de forcer une erreur à la soumission.
          userName:             locked ? undefined : formData.userName.trim(),
          depositDate:          locked ? undefined : (formData.depositDate || null),
          expectedDeliveryDate: locked ? undefined : (formData.expectedDeliveryDate || null),
          // Le statut n'est envoyé que lorsque son édition manuelle est permise
          // (commande ayant déjà atteint READY) ET qu'une cible a réellement
          // été choisie (DELIVERED/CANCELLED) : avant cela, il est recalculé
          // automatiquement côté backend à partir du statut des articles, et
          // le backend rejette toute autre valeur — y compris READY renvoyé
          // inchangé.
          status:               canEditStatus && formData.status ? formData.status : undefined,
          // CORRECTION : COMPLETED ne peut plus être positionné manuellement
          // (voir PAYMENT_STATUSES_EDITABLE) — même en renvoyant la valeur déjà
          // en place, le backend le rejette explicitement. On n'envoie donc ce
          // champ que si l'utilisateur a choisi une autre valeur.
          paymentStatus:        formData.paymentStatus && formData.paymentStatus !== 'COMPLETED'
                                   ? formData.paymentStatus
                                   : undefined,
          shippingAddress:      locked ? undefined : (formData.shippingAddress.trim() || null),
          notes:                locked ? undefined : (formData.notes.trim() || null),
          discountAmount:       locked ? undefined : (formData.discountAmount !== '' ? parseFloat(formData.discountAmount) : null),
          loyaltyPointsUsed:    locked ? undefined : (formData.loyaltyPointsUsed !== '' ? parseInt(formData.loyaltyPointsUsed, 10) : null),
        }
      : {
          // Création minimaliste : depositDate/status/paymentStatus/remise/
          // points fidélité ne sont pas demandés, le backend les gère.
          userName:             formData.userName.trim(),
          expectedDeliveryDate: formData.expectedDeliveryDate || null,
          shippingAddress:      formData.shippingAddress.trim() || null,
          notes:                formData.notes.trim() || null,
        }

    try {
      await onSubmit?.(payload)
    } catch (err) {
      // Erreur gérée dans le parent
    }
  }

  // ── Rendu ────────────────────────────────────────────────────────────────────
  return (
    <form onSubmit={handleSubmit} noValidate className="space-y-3">

      {/* Client */}
      <div className="rounded-lg border border-slate-200 bg-slate-50/60 p-3">
        <SectionTitle>Client</SectionTitle>
        <FieldWrapper
          label="Nom d'utilisateur"
          required
          error={fieldErrors.userName}
          hint={isEdit ? "Changer le nom réattribue la commande à un autre client" : "Username exact enregistré"}
        >
          <div className="relative">
            <User size={12} className="absolute left-2.5 top-2.5 text-slate-400 pointer-events-none" />
            <input
              id="order-userName"
              type="text"
              name="userName"
              value={formData.userName}
              onChange={handleChange}
              onBlur={handleBlur}
              placeholder="ex: lowona"
              className={`${inputClass('userName', formData, fieldErrors)} pl-7`}
              disabled={isSubmitting || locked}
              autoComplete="off"
            />
          </div>
        </FieldWrapper>
        {locked && (
          <p className="mt-1.5 text-[10px] text-amber-600">
            Commande verrouillée ({order?.status === 'DELIVERED' ? 'livrée' : order?.status === 'CANCELLED' ? 'annulée' : 'déjà payée'}) : seul le statut de paiement reste modifiable.
          </p>
        )}
      </div>

      {/* Dates + Statuts */}
      <div className="rounded-lg border border-slate-200 bg-slate-50/60 p-3">
        <SectionTitle>Dates et statuts</SectionTitle>
        <div className="grid grid-cols-2 gap-2">
          {isEdit && (
            <FieldWrapper label="Date de dépôt" required error={fieldErrors.depositDate}>
              <div className="relative">
                <Calendar size={12} className="absolute left-2.5 top-2.5 text-slate-400" />
                <input
                  id="order-depositDate"
                  type="date"
                  name="depositDate"
                  value={formData.depositDate}
                  onChange={handleChange}
                  onBlur={handleBlur}
                  className={`${inputClass('depositDate', formData, fieldErrors)} pl-7`}
                  disabled={isSubmitting || locked}
                />
              </div>
            </FieldWrapper>
          )}

          <FieldWrapper label="Livraison prévue" error={fieldErrors.expectedDeliveryDate} hint="Optionnel">
            <div className="relative">
              <Calendar size={12} className="absolute left-2.5 top-2.5 text-slate-400" />
              <input
                id="order-expectedDeliveryDate"
                type="date"
                name="expectedDeliveryDate"
                value={formData.expectedDeliveryDate}
                onChange={handleChange}
                onBlur={handleBlur}
                className={`${inputClass('expectedDeliveryDate', formData, fieldErrors)} pl-7`}
                disabled={isSubmitting || locked}
              />
            </div>
          </FieldWrapper>

          {isEdit && (
            <FieldWrapper
              label="Statut de paiement"
              required
              error={fieldErrors.paymentStatus}
              hint={formData.paymentStatus === 'COMPLETED' ? "Payé automatiquement — choisissez une autre valeur pour la modifier (ex: remboursement)" : undefined}
            >
              <div className="relative">
                <CreditCard size={12} className="absolute left-2.5 top-2.5 text-slate-400 pointer-events-none" />
                <select
                  id="order-paymentStatus"
                  name="paymentStatus"
                  value={formData.paymentStatus}
                  onChange={handleChange}
                  onBlur={handleBlur}
                  className={`${inputClass('paymentStatus', formData, fieldErrors)} pl-7 appearance-none`}
                  disabled={isSubmitting}
                >
                  {/* CORRECTION : COMPLETED n'est plus une option sélectionnable
                      (voir PAYMENT_STATUSES_EDITABLE) — le backend rejette
                      désormais toute tentative de la fixer manuellement. On
                      l'affiche seulement, désactivée, si c'est la valeur
                      actuelle, pour que l'utilisateur comprenne l'état réel. */}
                  {formData.paymentStatus === 'COMPLETED' && (
                    <option value="COMPLETED" disabled>Payé (automatique)</option>
                  )}
                  {PAYMENT_STATUSES_EDITABLE.map(({ value, label }) => (
                    <option key={value} value={value}>{label}</option>
                  ))}
                </select>
              </div>
            </FieldWrapper>
          )}

          {isEdit && (
            canEditStatus ? (
              <FieldWrapper
                label="Statut de la commande"
                error={fieldErrors.status}
                hint="Laisser sur « — » pour ne pas modifier"
              >
                <div className="relative">
                  <Tag size={12} className="absolute left-2.5 top-2.5 text-slate-400 pointer-events-none" />
                  <select
                    id="order-status"
                    name="status"
                    value={formData.status}
                    onChange={handleChange}
                    className={`${inputClass('status', formData, fieldErrors)} pl-7 appearance-none`}
                    disabled={isSubmitting}
                  >
                    <option value="">— Ne pas modifier ({ORDER_STATUS_LABELS[order?.status] || order?.status}) —</option>
                    {MANUAL_STATUS_OPTIONS.map((value) => (
                      <option key={value} value={value}>
                        {ORDER_STATUS_LABELS[value] || value}
                      </option>
                    ))}
                  </select>
                </div>
              </FieldWrapper>
            ) : (
              <FieldWrapper label="Statut de la commande" hint="Géré automatiquement selon les articles">
                <div className="flex items-center h-[34px] px-2.5 rounded-lg border border-slate-200 bg-slate-100 text-xs text-slate-500">
                  {ORDER_STATUS_LABELS[order?.status] || order?.status || '—'}
                </div>
              </FieldWrapper>
            )
          )}
        </div>
      </div>

      {/* Infos supplémentaires */}
      <div className="rounded-lg border border-slate-200 bg-slate-50/60 p-3">
        <SectionTitle>Informations supplémentaires</SectionTitle>
        <FieldWrapper
          label="Adresse de livraison"
          error={fieldErrors.shippingAddress}
          hint="Optionnel"
          className="mb-2"
        >
          <div className="relative">
            <MapPin size={12} className="absolute left-2.5 top-2.5 text-slate-400" />
            <input
              id="order-shippingAddress"
              type="text"
              name="shippingAddress"
              value={formData.shippingAddress}
              onChange={handleChange}
              onBlur={handleBlur}
              placeholder="Adresse complète"
              maxLength={255}
              className={`${inputClass('shippingAddress', formData, fieldErrors)} pl-7`}
              disabled={isSubmitting || locked}
            />
          </div>
        </FieldWrapper>

        <FieldWrapper label="Notes" error={fieldErrors.notes}>
          <div className="relative">
            <MessageSquare size={12} className="absolute left-2.5 top-2.5 text-slate-400" />
            <textarea
              id="order-notes"
              name="notes"
              value={formData.notes}
              onChange={handleChange}
              onBlur={handleBlur}
              placeholder="Remarques…"
              rows={2}
              maxLength={1000}
              className={`${inputClass('notes', formData, fieldErrors)} pl-7 resize-none`}
              disabled={isSubmitting || locked}
            />
          </div>
        </FieldWrapper>
      </div>

      {/* Remise & fidélité — édition uniquement, plus de repli "options avancées" */}
      {isEdit && (
        <div className="rounded-lg border border-slate-200 bg-slate-50/60 p-3">
          <SectionTitle>Remise et fidélité</SectionTitle>
          <div className="grid grid-cols-2 gap-2">
            <FieldWrapper label="Remise (FCFA)" error={fieldErrors.discountAmount} hint="Optionnel">
              <div className="relative">
                <Percent size={12} className="absolute left-2.5 top-2.5 text-slate-400" />
                <input
                  id="order-discountAmount"
                  type="number"
                  name="discountAmount"
                  value={formData.discountAmount}
                  onChange={handleChange}
                  onBlur={handleBlur}
                  placeholder="0"
                  min="0"
                  step="0.01"
                  className={`${inputClass('discountAmount', formData, fieldErrors)} pl-7`}
                  disabled={isSubmitting || locked}
                />
              </div>
            </FieldWrapper>

            <FieldWrapper label="Points fidélité" error={fieldErrors.loyaltyPointsUsed} hint="Optionnel">
              <div className="relative">
                <Star size={12} className="absolute left-2.5 top-2.5 text-slate-400" />
                <input
                  id="order-loyaltyPointsUsed"
                  type="number"
                  name="loyaltyPointsUsed"
                  value={formData.loyaltyPointsUsed}
                  onChange={handleChange}
                  onBlur={handleBlur}
                  placeholder="0"
                  min="0"
                  step="1"
                  className={`${inputClass('loyaltyPointsUsed', formData, fieldErrors)} pl-7`}
                  disabled={isSubmitting || locked}
                />
              </div>
            </FieldWrapper>
          </div>
        </div>
      )}

      {/* Bouton soumission */}
      <button
        type="submit"
        disabled={isSubmitting}
        className={[
          'w-full flex items-center justify-center gap-1.5 px-3 py-2.5 rounded-lg',
          'text-xs font-semibold transition-all active:scale-[0.98]',
          isSubmitting
            ? 'bg-slate-200 text-slate-400 cursor-not-allowed'
            : 'bg-indigo-600 text-white hover:bg-indigo-700 shadow-sm',
        ].join(' ')}
      >
        {isSubmitting && <Loader2 size={12} className="animate-spin" />}
        {mode === 'create' ? 'Créer la commande' : 'Enregistrer les modifications'}
      </button>
    </form>
  )
}

export default OrderForm
