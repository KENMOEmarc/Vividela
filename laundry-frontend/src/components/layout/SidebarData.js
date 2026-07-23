import { LayoutDashboard, Users, Package, Shirt, Ticket, ShoppingCart, UserCheck, Settings, Warehouse, Bell, Coins } from 'lucide-react'
import { ROUTES, ROLES } from '../../utils/constants'

export const ROLE_BADGE = {
  [ROLES.ADMIN]:    'bg-purple-500/20 text-purple-300',
  [ROLES.MANAGER]:  'bg-amber-500/20 text-amber-300',
  [ROLES.EMPLOYEE]: 'bg-blue-500/20 text-blue-300',
  [ROLES.CUSTOMER]: 'bg-emerald-500/20 text-emerald-300',
}

export const NAV_SECTIONS = [
  {
    label: 'Principal',
    items: [
      {
        icon: LayoutDashboard,
        label: 'Aperçu',
        path: ROUTES.DASHBOARD,
        exact: true,
        roles: null,
      },
      {
        icon: Users,
        label: 'Utilisateurs',
        path: ROUTES.USERS,
        exact: false,
        roles: [ROLES.ADMIN, ROLES.MANAGER, ROLES.EMPLOYEE],
      },
      {
        icon: UserCheck,
        label: 'Clients',
        path: ROUTES.CUSTOMERS,
        exact: false,
        roles: [ROLES.ADMIN, ROLES.MANAGER, ROLES.EMPLOYEE],
      },
      {
        icon: ShoppingCart,
        label: 'Mes commandes',
        path: ROUTES.MY_ORDERS,
        exact: false,
        roles: [ROLES.CUSTOMER],
      },
      {
        icon: Shirt,
        label: 'Articles',
        path: ROUTES.MY_ARTICLES,
        exact: false,
        roles: [ROLES.CUSTOMER],
      },
      {
        icon: Ticket,
        label: 'Tickets',
        path: ROUTES.MY_TICKETS,
        exact: false,
        roles: [ROLES.CUSTOMER],
      },
      {
        icon: Bell,
        label: 'Notifications',
        path: ROUTES.MY_NOTIFICATIONS,
        exact: false,
        roles: [ROLES.CUSTOMER],
      },
      {
        icon: Package,
        label: 'Produits',
        path: ROUTES.PRODUCTS,
        exact: false,
        roles: [ROLES.ADMIN, ROLES.MANAGER, ROLES.EMPLOYEE],
      },
      {
        icon: Warehouse,
        label: 'Stock',
        path: ROUTES.STOCK,
        exact: false,
        roles: [ROLES.ADMIN, ROLES.MANAGER, ROLES.EMPLOYEE],
      },
      {
        icon: Coins,
        label: 'Tarifs des services',
        path: ROUTES.SERVICE_PRICES,
        exact: false,
        roles: [ROLES.ADMIN, ROLES.MANAGER, ROLES.EMPLOYEE],
      },
      {
        icon: Shirt,
        label: 'Articles',
        path: ROUTES.ARTICLES,
        exact: false,
        roles: [ROLES.ADMIN, ROLES.MANAGER, ROLES.EMPLOYEE],
      },
      {
        icon: ShoppingCart,
        label: 'Commandes',
        path: ROUTES.ORDERS,
        exact: false,
        roles: [ROLES.ADMIN, ROLES.MANAGER, ROLES.EMPLOYEE],
      },
      {
        icon: Ticket,
        label: 'Tickets',
        path: ROUTES.TICKETS,
        exact: false,
        roles: [ROLES.ADMIN, ROLES.MANAGER, ROLES.EMPLOYEE],
      },
    ],
  },
  {
    label: 'Système',
    items: [
      {
        icon: Settings,
        label: 'Paramètres',
        path: null,
        disabled: true,
        roles: null,
      },
    ],
  },
]
