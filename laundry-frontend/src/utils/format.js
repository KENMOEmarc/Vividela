/**
 * format.js — Formatage partagé (devise, dates).
 *
 * Auparavant dupliqué presque à l'identique dans OrderCard.jsx,
 * CustomerOrderCard.jsx et OrderPaymentsModal.jsx. Centralisé ici pour que
 * toute évolution (ex: passage à une autre devise, format de date) se fasse
 * à un seul endroit.
 */

/** Formate un montant en Francs CFA (XAF), sans décimales. */
export const formatCurrencyXAF = (value) =>
  new Intl.NumberFormat('fr-FR', {
    style: 'currency',
    currency: 'XAF',
    maximumFractionDigits: 0,
  }).format(value ?? 0)

/** Formate une date ISO en date courte fr-FR (ex: 09/07/2026), ou '—' si absente. */
export const formatDateFR = (value) => (value ? new Date(value).toLocaleDateString('fr-FR') : '—')

/** Formate une date ISO en date + heure fr-FR, ou '—' si absente. */
export const formatDateTimeFR = (value) => (value ? new Date(value).toLocaleString('fr-FR') : '—')
