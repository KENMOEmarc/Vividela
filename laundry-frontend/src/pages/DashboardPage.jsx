import { useAuth } from '../context/AuthContext'
import AdminDashboard from './AdminDashboard'
import CustomerHomeDashboard from './CustomerHomeDashboard'

/**
 * Point d'entrée du tableau de bord ("Aperçu").
 *
 * CORRECTION SÉCURITÉ / UX : ce composant n'affiche plus le même contenu à
 * tout le monde. Auparavant, un client (CUSTOMER) qui ouvrait "/dashboard"
 * voyait le dashboard interne (nombre total d'utilisateurs, liste des
 * derniers inscrits avec leurs emails, alertes de stock, livraisons de
 * toutes les commandes…) — des informations réservées au personnel.
 *
 * Le rôle détermine maintenant quel composant est rendu :
 *  - CUSTOMER               → CustomerHomeDashboard (commandes, articles,
 *                              tickets, notifications : uniquement ses
 *                              propres données)
 *  - ADMIN / EMPLOYEE / …   → AdminDashboard (vue d'ensemble opérationnelle)
 */
const DashboardPage = () => {
  const { isCustomer } = useAuth()
  return isCustomer() ? <CustomerHomeDashboard /> : <AdminDashboard />
}

export default DashboardPage
