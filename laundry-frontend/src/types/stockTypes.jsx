import { TrendingDown, TrendingUp, RefreshCw } from 'lucide-react'

export const MOVEMENT_TYPE_LABELS = {
  RESTOCK:     'Réapprovisionnement',
  CONSUMPTION: 'Consommation',
  ADJUSTMENT:  'Ajustement',
}

export const MOVEMENT_TYPE_COLORS = {
  RESTOCK:     'text-emerald-600 bg-emerald-50',
  CONSUMPTION: 'text-red-600 bg-red-50',
  ADJUSTMENT:  'text-blue-600 bg-blue-50',
}

export const MOVEMENT_TYPE_ICONS = {
  RESTOCK:     TrendingUp,
  CONSUMPTION: TrendingDown,
  ADJUSTMENT:  RefreshCw,
}

export const REGISTRATION_TYPE_LABELS = {
  IN:         'Entrée',
  ADJUSTMENT: 'Ajustement',
  SACHET:     'Sachet',
}

export const MEASUREMENT_UNIT_LABELS_BACKEND = {
  KG:     'kg',
  LITER:  'L',
  ML:     'mL',
  UNIT:   'pièce(s)',
  PACKET: 'Sachet(s)',
}

export const STOCK_TABLE_COLUMNS = [
  { key: 'productName', label: 'Produit', sortable: true },
  { key: 'currentQuantity', label: 'Quantité actuelle', sortable: true },
  { key: 'thresholdValue', label: 'Seuil d\'alerte', sortable: true },
  { key: 'measurementUnit', label: 'Unité', sortable: false },
  {
    key: 'belowThreshold',
    label: 'État',
    render: (val) => val
      ? <span className="px-2 py-0.5 rounded-full text-xs font-semibold bg-red-100 text-red-700">⚠ Alerte</span>
      : <span className="px-2 py-0.5 rounded-full text-xs font-semibold bg-emerald-100 text-emerald-700">✓ OK</span>,
  },
]

// ─── Lots de stock (batches) ────────────────────────────────────────────────
// Un produit peut avoir plusieurs lots, chacun avec sa propre quantité, son
// prix d'achat, sa date d'entrée en stock et sa date d'expiration.

export const BATCH_TABLE_COLUMNS = [
  { key: 'entryDate', label: 'Entrée en stock', sortable: true },
  { key: 'expirationDate', label: 'Expiration', sortable: true },
  { key: 'currentQuantity', label: 'Quantité', sortable: true },
  { key: 'unitPrice', label: 'Prix unitaire', sortable: true },
]

export const formatDateFr = (isoDate) => {
  if (!isoDate) return '—'
  const d = new Date(isoDate)
  if (Number.isNaN(d.getTime())) return '—'
  return d.toLocaleDateString('fr-FR', { day: '2-digit', month: '2-digit', year: 'numeric' })
}
