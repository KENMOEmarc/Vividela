/**
 * Validations pour les commandes — alignées sur le schéma SQL réel.
 *
 * Table orders :
 *   client_user_id (FK users.id) — sélectionné via userName
 *   deposit_date DATE NOT NULL
 *   expected_delivery_date DATE (optionnel)
 *   status ENUM('PENDING','RECEIVED','IN_PROGRESS','READY','DELIVERED','CANCELLED')
 *   payment_status ENUM('PENDING','COMPLETED','FAILED','REFUNDED')
 *   shipping_address VARCHAR(255)
 *   notes VARCHAR(1000)
 *   discount_amount DECIMAL (optionnel, ≥ 0)
 *   loyalty_points_used INT (optionnel, ≥ 0)
 */

// Statuts valides selon le schéma SQL (OrderStatus.java)
export const ORDER_STATUSES = [
  { value: 'PENDING',     label: 'En attente' },
  { value: 'RECEIVED',    label: 'Réceptionné' },
  { value: 'IN_PROGRESS', label: 'En traitement' },
  { value: 'READY',       label: 'Prêt' },
  { value: 'DELIVERED',   label: 'Livré' },
  { value: 'CANCELLED',   label: 'Annulé' },
]

// Statuts de paiement valides (PaymentStatus.java) — utilisés pour l'affichage
// (badges, libellés). Ne pas utiliser directement pour peupler un <select>
// éditable : voir PAYMENT_STATUSES_EDITABLE ci-dessous.
export const PAYMENT_STATUSES = [
  { value: 'PENDING',   label: 'En attente' },
  { value: 'COMPLETED', label: 'Payé' },
  { value: 'FAILED',    label: 'Échoué' },
  { value: 'REFUNDED',  label: 'Remboursé' },
]

// CORRECTION : COMPLETED ne peut plus être positionné manuellement depuis la
// commande — OrderServiceImpl.updateOrder() le rejette désormais explicitement
// (même en renvoyant la valeur déjà en place), car ce statut est réservé à la
// réconciliation automatique de PaymentServiceImpl (voir /payments/{id}/confirm).
// Un formulaire d'édition de commande ne doit donc proposer/soumettre que ces
// trois valeurs.
export const PAYMENT_STATUSES_EDITABLE = PAYMENT_STATUSES.filter(s => s.value !== 'COMPLETED')

const VALID_ORDER_STATUSES  = ORDER_STATUSES.map(s => s.value)
const VALID_PAYMENT_STATUSES = PAYMENT_STATUSES.map(s => s.value)

/**
 * Valide un champ individuel du formulaire commande.
 * Retourne un message d'erreur ou null si valide.
 */
export const validateOrderField = (key, value, allData = {}) => {
  switch (key) {
    // userName sert à résoudre le client en base (findByUserNameIgnoreCase)
    case 'userName':
      if (!value?.trim()) return 'Le nom d\'utilisateur client est requis.'
      if (value.trim().length < 3) return 'Minimum 3 caractères.'
      if (value.trim().length > 50) return 'Maximum 50 caractères.'
      return null

    case 'depositDate':
      if (!value) return 'La date de dépôt est requise.'
      if (isNaN(new Date(value).getTime())) return 'Date invalide.'
      return null

    case 'expectedDeliveryDate': {
      if (!value) return null // optionnel
      if (isNaN(new Date(value).getTime())) return 'Date invalide.'
      // La date de livraison doit être après la date de dépôt
      if (allData.depositDate && new Date(value) < new Date(allData.depositDate)) {
        return 'La date de livraison doit être après la date de dépôt.'
      }
      return null
    }

    case 'status':
      if (!value) return null // optionnel : non applicable à la création (auto RECEIVED côté backend), ni tant que la commande n'a pas atteint READY
      if (!VALID_ORDER_STATUSES.includes(value)) return 'Statut invalide.'
      return null

    case 'paymentStatus':
      if (!value) return 'Le statut de paiement est requis.'
      if (!VALID_PAYMENT_STATUSES.includes(value)) return 'Statut de paiement invalide.'
      return null

    case 'shippingAddress':
      if (value && value.length > 255) return 'Maximum 255 caractères.'
      return null

    case 'notes':
      if (value && value.length > 1000) return 'Maximum 1000 caractères.'
      return null

    case 'discountAmount':
      if (value === '' || value === null || value === undefined) return null // optionnel
      if (isNaN(Number(value))) return 'Montant invalide.'
      if (Number(value) < 0) return 'La remise ne peut pas être négative.'
      return null

    case 'loyaltyPointsUsed':
      if (value === '' || value === null || value === undefined) return null // optionnel
      if (!Number.isInteger(Number(value))) return 'Entier requis.'
      if (Number(value) < 0) return 'Valeur négative non autorisée.'
      return null

    default:
      return null
  }
}

/**
 * Valide l'ensemble du formulaire commande.
 * Les champs requis dépendent du mode : à la création, depositDate/status/
 * paymentStatus sont fixés automatiquement par le backend (OrderServiceImpl.
 * createOrder) et ne font pas partie du formulaire — les exiger ici bloquait
 * systématiquement la création avant même l'appel API.
 * Retourne un objet { champ: message } pour les champs en erreur.
 */
export const validateOrderForm = (data, mode = 'create') => {
  const requiredFields = mode === 'edit'
    ? ['userName', 'depositDate', 'paymentStatus']
    : ['userName']

  const optionalFields = mode === 'edit'
    ? ['expectedDeliveryDate', 'status', 'shippingAddress', 'notes', 'discountAmount', 'loyaltyPointsUsed']
    : ['expectedDeliveryDate', 'shippingAddress', 'notes']

  return [...requiredFields, ...optionalFields].reduce((errors, key) => {
    const error = validateOrderField(key, data[key], data)
    if (error) errors[key] = error
    return errors
  }, {})
}