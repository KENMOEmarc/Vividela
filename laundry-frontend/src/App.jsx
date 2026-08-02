import {
  BrowserRouter,
  Routes,
  Route,
  Navigate,
  useLocation,
} from "react-router-dom";
import { ToastContainer } from "react-toastify";
import { AuthProvider, useAuth } from "./context/AuthContext";
import MainLayout from "./components/layout/MainLayout";
import DashboardLayout from "./components/layout/DashboardLayout";
import LoginPage from "./pages/LoginPage";
import RegisterPage from "./pages/RegisterPage";
import LandingPage from "./pages/LandingPage";
import DashboardPage from "./pages/DashboardPage";
import UsersPage from "./pages/UsersPage";
import ProductsPage from "./pages/ProductsPage";
import ArticlesPage from "./pages/ArticlesPage";
import OrdersPage from "./pages/OrdersPage";
import TicketsPage from "./pages/TicketsPage";
import CustomersPage from "./pages/CustomersPage";
import CustomerOrdersPage from "./pages/CustomerOrdersPage";
import CustomerDashboard from "./pages/CustomerDashboard";
import CustomerArticlesPage from "./pages/CustomerArticlesPage";
import CustomerTicketsPage from "./pages/CustomerTicketsPage";
import CustomerNotificationsPage from "./pages/CustomerNotificationsPage";
import StockPage from "./pages/StockPage";
import ServicePricesPage from "./pages/ServicePricesPage";
import StaffNotificationsPage from "./pages/StaffNotificationsPage";
import { ROUTES, ROLES } from "./utils/constants";

const ProtectedRoute = ({ children }) => {
  const { isAuthenticated } = useAuth();
  const location = useLocation();
  if (!isAuthenticated) {
    return <Navigate to={ROUTES.HOME} state={{ from: location }} replace />;
  }
  return children;
};

const RoleProtectedRoute = ({ children, roles }) => {
  const { isAuthenticated, hasAnyRole } = useAuth();
  const location = useLocation();
  if (!isAuthenticated) {
    return <Navigate to={ROUTES.HOME} state={{ from: location }} replace />;
  }
  if (roles && !hasAnyRole(roles)) {
    return <Navigate to={ROUTES.DASHBOARD} replace />;
  }
  return children;
};

const App = () => {
  return (
    <BrowserRouter>
      <ToastContainer
        position="top-right"
        autoClose={4000}
        hideProgressBar={false}
        newestOnTop
        closeOnClick
        pauseOnFocusLoss
        draggable
        pauseOnHover
        theme="colored"
      />
      <AuthProvider>
        <Routes>
          {/* Pages publiques — top navbar + footer */}
          <Route path="/" element={<MainLayout />}>
            <Route index element={<LandingPage />} />
            <Route path={ROUTES.LOGIN.slice(1)} element={<LoginPage />} />
            <Route path={ROUTES.REGISTER.slice(1)} element={<RegisterPage />} />
          </Route>

          {/* Pages protégées — sidebar + dashboard navbar + footer */}
          <Route
            path={ROUTES.DASHBOARD.slice(1)}
            element={
              <ProtectedRoute>
                <DashboardLayout />
              </ProtectedRoute>
            }
          >
            <Route index element={<DashboardPage />} />
            <Route
              path="users"
              element={
                <RoleProtectedRoute roles={[ROLES.ADMIN, ROLES.MANAGER, ROLES.EMPLOYEE]}>
                  <UsersPage />
                </RoleProtectedRoute>
              }
            />
            <Route
              path="products"
              element={
                <RoleProtectedRoute roles={[ROLES.ADMIN, ROLES.MANAGER, ROLES.EMPLOYEE]}>
                  <ProductsPage />
                </RoleProtectedRoute>
              }
            />
            <Route
              path="articles"
              element={
                <RoleProtectedRoute roles={[ROLES.ADMIN, ROLES.MANAGER, ROLES.EMPLOYEE]}>
                  <ArticlesPage />
                </RoleProtectedRoute>
              }
            />
            <Route
              path="orders"
              element={
                <RoleProtectedRoute roles={[ROLES.ADMIN, ROLES.MANAGER, ROLES.EMPLOYEE]}>
                  <OrdersPage />
                </RoleProtectedRoute>
              }
            />
            <Route
              path="tickets"
              element={
                <RoleProtectedRoute roles={[ROLES.ADMIN, ROLES.MANAGER, ROLES.EMPLOYEE]}>
                  <TicketsPage />
                </RoleProtectedRoute>
              }
            />
            <Route
              path="customers"
              element={
                <RoleProtectedRoute roles={[ROLES.ADMIN, ROLES.MANAGER, ROLES.EMPLOYEE]}>
                  <CustomersPage />
                </RoleProtectedRoute>
              }
            />
            <Route
              path="customers/:customerId/orders"
              element={
                <RoleProtectedRoute roles={[ROLES.ADMIN, ROLES.MANAGER, ROLES.EMPLOYEE]}>
                  <CustomerOrdersPage />
                </RoleProtectedRoute>
              }
            />
            <Route
              path="stock"
              element={
                <RoleProtectedRoute roles={[ROLES.ADMIN, ROLES.MANAGER, ROLES.EMPLOYEE]}>
                  <StockPage />
                </RoleProtectedRoute>
              }
            />
            <Route
              path="service-prices"
              element={
                <RoleProtectedRoute roles={[ROLES.ADMIN, ROLES.MANAGER, ROLES.EMPLOYEE]}>
                  <ServicePricesPage />
                </RoleProtectedRoute>
              }
            />
            <Route
              path="my-orders"
              element={
                <RoleProtectedRoute roles={[ROLES.CUSTOMER]}>
                  <CustomerDashboard />
                </RoleProtectedRoute>
              }
            />
            <Route
              path="my-articles"
              element={
                <RoleProtectedRoute roles={[ROLES.CUSTOMER]}>
                  <CustomerArticlesPage />
                </RoleProtectedRoute>
              }
            />
            <Route
              path="my-tickets"
              element={
                <RoleProtectedRoute roles={[ROLES.CUSTOMER]}>
                  <CustomerTicketsPage />
                </RoleProtectedRoute>
              }
            />
            <Route
              path="my-notifications"
              element={
                <RoleProtectedRoute roles={[ROLES.CUSTOMER]}>
                  <CustomerNotificationsPage />
                </RoleProtectedRoute>
              }
            />
          </Route>

          {/*
            Page "Notifications" ADMIN/MANAGER — volontairement HORS du
            DashboardLayout (pas de sidebar/navbar dupliquée) car elle est
            ouverte dans sa propre fenêtre de navigateur (window.open) par le
            bouton cloche de DashboardNavbar, avec juste un bouton retour.
          */}
          <Route
            path={ROUTES.NOTIFICATIONS.slice(1)}
            element={
              <RoleProtectedRoute roles={[ROLES.ADMIN, ROLES.MANAGER]}>
                <StaffNotificationsPage />
              </RoleProtectedRoute>
            }
          />

          <Route
            path="*"
            element={
              <div className="flex flex-col items-center justify-center min-h-screen bg-slate-50 text-center px-4">
                <div className="text-9xl font-bold text-slate-200 select-none">
                  404
                </div>
                <h1 className="text-2xl font-bold text-slate-700 mt-2">
                  Page introuvable
                </h1>
                <p className="text-slate-500 mt-2">
                  La page que vous cherchez n'existe pas.
                </p>
                <a
                  href={ROUTES.HOME}
                  className="mt-6 px-6 py-2.5 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors font-semibold text-sm"
                >
                  Retour à l'accueil
                </a>
              </div>
            }
          />
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  );
};

export default App;
