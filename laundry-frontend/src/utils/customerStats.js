/**
 * customerStats.js — Agrège les commandes par client (nombre + montant total).
 *
 * CORRECTION : la page "Gestion des clients" (CustomersPage) affichait des
 * colonnes "Commandes" et "Montant total" toujours à zéro. Cause : elle
 * charge les clients via GET /users/customers, qui renvoie un UserDto brut
 * (id, nom, email...) sans aucune statistique de commande — orderCount et
 * totalSpent n'existent tout simplement pas sur cet objet.
 *
 * Le backend expose bien ces stats, mais uniquement par client
 * (GET /customers/{id}/stats — voir customerApi.getCustomerStats), pas pour
 * toute la liste en un seul appel. Plutôt que de faire un appel par client
 * (N+1), on réutilise GET /orders (déjà accessible au staff, voir OrdersPage)
 * et on agrège côté frontend par clientUserId.
 */

/**
 * @param {Array} orders - liste de commandes (OrderDto), doit contenir
 *   `clientUserId` et `totalAmount`.
 * @returns {Map<number, {orderCount: number, totalSpent: number}>}
 */
export const computeCustomerStatsMap = (orders = []) => {
  const map = new Map()
  for (const order of orders) {
    const customerId = order.clientUserId
    if (customerId == null) continue
    const amount = Number(order.totalAmount) || 0
    const current = map.get(customerId) ?? { orderCount: 0, totalSpent: 0 }
    current.orderCount += 1
    current.totalSpent += amount
    map.set(customerId, current)
  }
  return map
}

/** Fusionne les stats agrégées sur chaque client (orderCount/totalSpent à 0 si aucune commande). */
export const withCustomerStats = (customers = [], orders = []) => {
  const statsMap = computeCustomerStatsMap(orders)
  return customers.map((customer) => {
    const stats = statsMap.get(customer.id) ?? { orderCount: 0, totalSpent: 0 }
    return { ...customer, ...stats }
  })
}
