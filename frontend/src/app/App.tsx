import { useEffect, useMemo, useState } from "react";
import { Navigate, Outlet, Route, Routes, useLocation, useNavigate } from "react-router-dom";
import { ShellNavItem } from "../components/ui";
import { AdminPage } from "../pages/admin-page";
import { AiPredictionPage } from "../pages/ai-prediction-page";
import { LoginPage, NotFoundPage, SetPasswordPage } from "../pages/auth-pages";
import { AdminQueueDisplayPreviewPage } from "../pages/admin-ops-pages";
import { AdminDashboardPage, CitiesPage, CustomersPage, OverviewPage, ProductsPage, ProfilePage } from "../pages/core-pages";
import { CustomerTrackingPage } from "../pages/customer-tracking-page";
import { InvoicePage, ProductionPage, QueueControlPage, QueuePage, ReportsPage } from "../pages/ops-pages";
import { QueueDisplayPage } from "../pages/queue-display-page";
import { ReceptionPage } from "../pages/reception-page";
import { publicAsset } from "../lib/assets";
import type { PageKey } from "../types/models";
import { useAuth } from "./auth-context";
import { useI18n } from "./i18n-context";
import { canView } from "./policy";

function resolveHome(role: string | null): string {
  switch (role) {
    case "ADMIN":
      return "/app/overview";
    case "ACCOUNTANT":
      return "/app/invoice";
    case "RECEPTIONIST":
      return "/app/orders";
    case "TECHNICIAN":
      return "/app/queue-control";
    default:
      return "/app/overview";
  }
}

function RequireAuth(): JSX.Element {
  const auth = useAuth();
  const location = useLocation();
  const { t } = useI18n();

  if (!auth.initialized) {
    return <div className="auth-shell"><div className="card">{t("Loading session...")}</div></div>;
  }

  if (!auth.tokens) {
    return <Navigate replace to="/login" state={{ from: location.pathname }} />;
  }

  return <Outlet />;
}

function RequirePage({ page }: { page: PageKey }): JSX.Element {
  const auth = useAuth();
  const access = canView(auth, page);

  if (!access.allowed) {
    return <Navigate replace to={resolveHome(auth.role)} />;
  }

  return <Outlet />;
}

function getDisplayName(auth: ReturnType<typeof useAuth>): string {
  if (auth.profile) {
    return `${auth.profile.firstName} ${auth.profile.lastName}`;
  }

  return auth.tokens?.username ?? "OPS";
}

function PersonIcon(): JSX.Element {
  return (
    <svg aria-hidden="true" className="shell-user-name-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M20 21a8 8 0 0 0-16 0" />
      <circle cx="12" cy="7" r="4" />
    </svg>
  );
}

function AppLayout(): JSX.Element {
  const auth = useAuth();
  const { language, setLanguage, t } = useI18n();
  const navigate = useNavigate();
  const location = useLocation();
  const [sidebarOpen, setSidebarOpen] = useState<boolean>(() =>
    typeof window === "undefined" ? true : window.innerWidth > 1100
  );
  const displayName = getDisplayName(auth);
  const isArabic = language === "ar";
  const isReceptionist = auth.role === "RECEPTIONIST";
  const isAccountant = auth.role === "ACCOUNTANT";
  const isTechnician = auth.role === "TECHNICIAN";

  useEffect(() => {
    if (typeof window === "undefined") {
      return undefined;
    }

    const mediaQuery = window.matchMedia("(max-width: 1100px)");
    const syncSidebar = (matchesMobile: boolean): void => {
      setSidebarOpen(!matchesMobile);
    };

    syncSidebar(mediaQuery.matches);

    const handleChange = (event: MediaQueryListEvent): void => {
      syncSidebar(event.matches);
    };

    mediaQuery.addEventListener("change", handleChange);
    return () => mediaQuery.removeEventListener("change", handleChange);
  }, []);

  useEffect(() => {
    if (typeof window !== "undefined" && window.innerWidth <= 1100) {
      setSidebarOpen(false);
    }
  }, [location.pathname]);

  const navItems = useMemo(
    () =>
      [
        { page: "overview" as const, to: "/app/overview", label: "Overview", visible: !isReceptionist && !isTechnician },
        { page: "profile" as const, to: "/app/profile", label: "Profile" },
        { page: "customers" as const, to: "/app/customers", label: isAccountant ? "Customer Directory" : "Customers", visible: !isReceptionist },
        { page: "cities" as const, to: "/app/cities", label: "Cities", visible: !isReceptionist && !isAccountant },
        { page: "products" as const, to: "/app/products", label: "Products", visible: auth.role === "ADMIN" },
        { page: "orders" as const, to: "/app/orders", label: "Orders", visible: !isAccountant },
        { page: "ai" as const, to: "/app/ai-prediction", label: "AI Prediction", visible: isReceptionist },
        { page: "invoice" as const, to: "/app/invoice", label: "Invoice", visible: isAccountant || auth.role === "ADMIN" },
        { page: "reports" as const, to: "/app/reports", label: "Reports", visible: isAccountant || auth.role === "ADMIN" },
        { page: "queue-control" as const, to: "/app/queue-control", label: isArabic ? "التحكم بالأدوار" : "Queue Control", visible: auth.role === "ADMIN" || isTechnician },
        { page: "queue-display-admin" as const, to: "/app/queue-display-preview", label: isArabic ? "شاشة الأدوار" : "Queue Display", visible: auth.role === "ADMIN" },
        { page: "admin" as const, to: "/app/admin", label: isArabic ? "إدارة الموظفين" : "Employee Management", visible: !isReceptionist }
      ].filter((item) => canView(auth, item.page).allowed && (item.visible ?? true)),
    [auth, isAccountant, isArabic, isReceptionist, isTechnician]
  );

  return (
    <div className={`app-shell ${sidebarOpen ? "sidebar-open" : "sidebar-closed"}`}>
      <div
        aria-hidden={!sidebarOpen}
        className={`shell-scrim ${sidebarOpen ? "visible" : ""}`}
        onClick={() => setSidebarOpen(false)}
      />
      <aside aria-hidden={!sidebarOpen} className="shell-sidebar">
        <div className="shell-sidebar-header">
          <button
            aria-label={isArabic ? "إغلاق القائمة" : "Close navigation"}
            className="shell-sidebar-close"
            onClick={() => setSidebarOpen(false)}
            type="button"
          >
            <span />
            <span />
          </button>
          <div className="shell-user-badge">
            <div className="shell-user-avatar" aria-hidden="true">
              <img alt="" src={publicAsset("app-icon.png")} />
            </div>
            <div className="shell-user-copy">
              <strong className="shell-user-name">
                <PersonIcon />
                <span>{displayName}</span>
              </strong>
              <span>{t(auth.role ?? "Unknown role")}</span>
            </div>
          </div>
        </div>
        <nav className="shell-nav">
          {navItems.map((item) => (
            <ShellNavItem key={item.to} to={item.to} label={item.label} />
          ))}
        </nav>
        <div className="shell-meta">
          <div className="chip">{isArabic ? "الجمعية التعاونية لعصر الزيتون" : "Cooperative Association for Olive Pressing"}</div>
          <div>{isArabic ? "© حقوق النشر محفوظة لدى مطورين النظام" : "© Copyright reserved by the system developers"}</div>
        </div>
      </aside>
      <main className="shell-main">
        <div className="topbar">
          <div className="topbar-primary">
            <button
              aria-expanded={sidebarOpen}
              aria-label={sidebarOpen ? (isArabic ? "إغلاق القائمة" : "Close navigation") : isArabic ? "فتح القائمة" : "Open navigation"}
              className="shell-menu-button"
              onClick={() => setSidebarOpen((current) => !current)}
              type="button"
            >
              <span />
              <span />
              <span />
            </button>
            <div className="topbar-context">
              <strong>{isArabic ? "مساحة العمل" : "Workspace"}</strong>
              <span>
                {isArabic
                  ? `مرحباً ${displayName}`
                  : `Welcome back, ${displayName}`}
              </span>
            </div>
          </div>
          <div className="topbar-controls">
            <div className="language-toggle" aria-label={t("Arabic / English")} role="group">
              <button
                className={language === "en" ? "active" : ""}
                onClick={() => setLanguage("en")}
                aria-label={t("English")}
                title={t("English")}
                type="button"
              >
                E
              </button>
              <button
                className={language === "ar" ? "active" : ""}
                onClick={() => setLanguage("ar")}
                aria-label={t("Arabic")}
                title={t("Arabic")}
                type="button"
              >
                ع
              </button>
            </div>
            <button
              className="btn ghost"
              onClick={() => {
                void auth.logout().then(() => navigate("/login", { replace: true }));
              }}
              type="button"
            >
              {t("Logout")}
            </button>
          </div>
        </div>
        <Outlet />
      </main>
    </div>
  );
}

export default function App(): JSX.Element {
  const auth = useAuth();

  return (
    <Routes>
      <Route path="/" element={<Navigate replace to={auth.tokens ? resolveHome(auth.role) : "/login"} />} />
      <Route path="/login" element={<LoginPage />} />
      <Route path="/track" element={<CustomerTrackingPage />} />
      <Route path="/queue-display" element={<QueueDisplayPage />} />
      <Route path="/set-password" element={<SetPasswordPage />} />
      <Route element={<RequireAuth />}>
        <Route path="/app" element={<AppLayout />}>
          <Route index element={<Navigate replace to={resolveHome(auth.role)} />} />
          <Route path="overview" element={auth.role === "ADMIN" ? <AdminDashboardPage /> : <OverviewPage />} />
          <Route path="profile" element={<ProfilePage />} />
          <Route element={<RequirePage page="customers" />}>
            <Route path="customers" element={<CustomersPage />} />
          </Route>
          <Route element={<RequirePage page="cities" />}>
            <Route path="cities" element={<CitiesPage />} />
          </Route>
          <Route element={<RequirePage page="products" />}>
            <Route path="products" element={<ProductsPage />} />
          </Route>
          <Route element={<RequirePage page="orders" />}>
            <Route path="orders" element={<ReceptionPage />} />
          </Route>
          <Route element={<RequirePage page="ai" />}>
            <Route path="ai-prediction" element={<AiPredictionPage />} />
          </Route>
          <Route element={<RequirePage page="invoice" />}>
            <Route path="invoice" element={<InvoicePage />} />
          </Route>
          <Route element={<RequirePage page="reports" />}>
            <Route path="reports" element={<ReportsPage />} />
          </Route>
          <Route element={<RequirePage page="queue-display-admin" />}>
            <Route path="queue-display-preview" element={<AdminQueueDisplayPreviewPage />} />
          </Route>
          <Route element={<RequirePage page="queue" />}>
            <Route path="queue" element={<QueuePage />} />
          </Route>
          <Route element={<RequirePage page="queue-control" />}>
            <Route path="queue-control" element={<QueueControlPage />} />
          </Route>
          <Route element={<RequirePage page="production" />}>
            <Route path="production" element={<ProductionPage />} />
          </Route>
          <Route element={<RequirePage page="admin" />}>
            <Route path="admin" element={<AdminPage />} />
          </Route>
        </Route>
      </Route>
      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  );
}
