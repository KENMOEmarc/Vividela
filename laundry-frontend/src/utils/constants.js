/**
 * Constantes globales de l'application.
 * CORRECTION : les enums CLOTHING_TYPES, CLOTHING_SIZES, FABRIC_TYPES
 * sont maintenant synchronisés avec les enums Java et le schéma SQL.
 */

export const API_BASE_URL = import.meta.env.VITE_API_URL || '/api'

export const STORAGE_KEYS = {
  ACCESS_TOKEN: 'auth_token',
  USER_DATA:    'auth_user'
}

export const ROUTES = {
  HOME:      '/',
  LOGIN:     '/login',
  REGISTER:  '/register',
  DASHBOARD: '/dashboard',
  USERS:     '/dashboard/users',
  PRODUCTS:  '/dashboard/products',
  ARTICLES:  '/dashboard/articles',
  ORDERS:    '/dashboard/orders',
  TICKETS:   '/dashboard/tickets',
  CUSTOMERS: '/dashboard/customers',
  CUSTOMER_ORDERS: '/dashboard/customers/:customerId/orders',
  MY_ORDERS: '/dashboard/my-orders',
  MY_ARTICLES: '/dashboard/my-articles',
  MY_TICKETS: '/dashboard/my-tickets',
  MY_NOTIFICATIONS: '/dashboard/my-notifications',
  STOCK:     '/dashboard/stock',
  SERVICE_PRICES: '/dashboard/service-prices',
  // Page notifications ADMIN/MANAGER — hors layout dashboard car ouverte
  // dans une fenêtre à part (voir DashboardNavbar → window.open).
  NOTIFICATIONS: '/notifications',
}

export const ROLES = {
  ADMIN:    'ADMIN',
  MANAGER:  'MANAGER',
  EMPLOYEE: 'EMPLOYEE',
  CUSTOMER: 'CUSTOMER',
}

export const ROLE_LABELS = {
  ADMIN:    'Admin',
  MANAGER:  'Manager',
  EMPLOYEE: 'Employé',
  CUSTOMER: 'Client',
}

// Rang hiérarchique des rôles, en miroir de UserServiceImpl#roleRank (backend).
// Utilisé côté frontend uniquement pour l'UX (masquer des actions non permises) ;
// la vérification faisant foi reste toujours celle du backend.
export const ROLE_RANK = {
  [ROLES.ADMIN]:    4,
  [ROLES.MANAGER]:  3,
  [ROLES.EMPLOYEE]: 2,
  [ROLES.CUSTOMER]: 1,
}

export const getRoleRank = (role) => ROLE_RANK[role] ?? 0

export const PASSWORD_MIN_LENGTH = 8
export const PASSWORD_REGEX = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]{8,}$/
export const USERNAME_REGEX = /^[a-zA-Z0-9_]{3,50}$/

export const MEASUREMENT_UNITS = {
  KILOGRAM:   'KG',
  LITER:      'L',
  MILLILITER: 'mL',
  UNIT:       'UNIT',
  PACKET:     'PACKET',
}

export const MEASUREMENT_UNIT_LABELS = {
  KILOGRAM:   'kg',
  LITER:      'L',
  MILLILITER: 'mL',
  UNIT:       'pièce',
  PACKET:     'Sachet',
}

// ─── Vêtements — synchronisés avec ClothingType.java ─────────────────────────
// CORRECTION : suppression de HOODIE, POLO (absents du Java) ;
//              ajout de PAGNE, T_SHIRT, JEANS, BLOUSE, SUIT, VEST, SCARF, GLOVES, BELT, UNDERWEAR, SOCKS
export const CLOTHING_TYPES = {
  SHIRT:      'SHIRT',
  T_SHIRT:    'T_SHIRT',
  PANTS:      'PANTS',
  JEANS:      'JEANS',
  SKIRT:      'SKIRT',
  DRESS:      'DRESS',
  JACKET:     'JACKET',
  COAT:       'COAT',
  SWEATER:    'SWEATER',
  BLOUSE:     'BLOUSE',
  UNDERWEAR:  'UNDERWEAR',
  SOCKS:      'SOCKS',
  SUIT:       'SUIT',
  VEST:       'VEST',
  SHORTS:     'SHORTS',
  SCARF:      'SCARF',
  GLOVES:     'GLOVES',
  BELT:       'BELT',
}

export const CLOTHING_TYPE_LABELS = {
  SHIRT:     'Chemise',
  T_SHIRT:   'T-shirt',
  PANTS:     'Pantalon',
  JEANS:     'Jeans',
  SKIRT:     'Jupe',
  DRESS:     'Robe',
  JACKET:    'Veste',
  COAT:      'Manteau',
  SWEATER:   'Pull',
  BLOUSE:    'Blouse',
  UNDERWEAR: 'Sous-vêtement',
  SOCKS:     'Chaussettes',
  SUIT:      'Costume',
  VEST:      'Gilet',
  SHORTS:    'Short',
  SCARF:     'Écharpe',
  GLOVES:    'Gants',
  BELT:      'Ceinture',
}

// ─── Tailles — synchronisées avec SizeType.java ───────────────────────────────
// CORRECTION : ajout de S et UNIQUE, suppression de XS et XXXL
export const CLOTHING_SIZES = {
  S:      'S',
  M:      'M',
  L:      'L',
  XL:     'XL',
  XXL:    'XXL',
  UNIQUE: 'UNIQUE',
}

export const CLOTHING_SIZE_LABELS = {
  S:      'S — Small',
  M:      'M — Medium',
  L:      'L — Large',
  XL:     'XL — Extra Large',
  XXL:    'XXL — Double XL',
  UNIQUE: 'Taille unique',
}

// ─── Services — synchronisés avec ServiceType.java ────────────────────────────
export const SERVICE_TYPES = {
  DRY_CLEAN:     'DRY_CLEAN',
  WASH:          'WASH',
  IRON:          'IRON',
  STAIN_REMOVAL: 'STAIN_REMOVAL',
  DEYING:        'DEYING',
  ALTERATION:    'ALTERATION',
}

export const SERVICE_TYPE_LABELS = {
  DRY_CLEAN:     'Nettoyage à sec',
  WASH:          'Lavage',
  IRON:          'Repassage',
  STAIN_REMOVAL: 'Détachage',
  DEYING:        'Teinture',
  ALTERATION:    'Retouche',
}

// ─── Tissus — synchronisés avec FabricType.java ───────────────────────────────
// CORRECTION : suppression de COTTON_BLEND, WOOL_BLEND, DENIM ;
//              ajout de BLENDED, ACRYLIC, NYLON, LINEN
export const FABRIC_TYPES = {
  COTTON:    'COTTON',
  POLYESTER: 'POLYESTER',
  WOOL:      'WOOL',
  SILK:      'SILK',
  LINEN:     'LINEN',
  SYNTHETIC: 'SYNTHETIC',
  BLENDED:   'BLENDED',
  ACRYLIC:   'ACRYLIC',
  NYLON:     'NYLON',
}

export const FABRIC_TYPE_LABELS = {
  COTTON:    'Coton',
  POLYESTER: 'Polyester',
  WOOL:      'Laine',
  SILK:      'Soie',
  LINEN:     'Lin',
  SYNTHETIC: 'Synthétique',
  BLENDED:   'Tissu mixte',
  ACRYLIC:   'Acrylique',
  NYLON:     'Nylon',
}