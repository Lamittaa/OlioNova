import type { ActionKey, AuthSession, PageKey, Role } from "../types/models";

interface GuardResult {
  allowed: boolean;
  reason?: string;
}

const ALL_ROLES: Role[] = ["ADMIN", "ACCOUNTANT", "RECEPTIONIST", "TECHNICIAN"];

function hasRole(session: AuthSession, roles: Role[]): boolean {
  return session.role !== null && roles.includes(session.role);
}

function hasAuthority(session: AuthSession, authority: string): boolean {
  return session.authorities.includes(authority);
}

export function canView(session: AuthSession, page: PageKey): GuardResult {
  if (!session.tokens) {
    return { allowed: false, reason: "Sign in required." };
  }

  switch (page) {
    case "overview":
    case "profile":
      return { allowed: true };
    case "queue":
      return {
        allowed: hasRole(session, ["ADMIN"]),
        reason: "Queue operations are limited to admin users."
      };
    case "queue-control":
      return {
        allowed: hasRole(session, ["ADMIN", "TECHNICIAN"]),
        reason: "Queue control is limited to admin and technician users."
      };
    case "cities":
      return { allowed: hasRole(session, ["ADMIN", "RECEPTIONIST"]) };
    case "customers":
      return { allowed: hasRole(session, ["ADMIN", "ACCOUNTANT", "RECEPTIONIST"]) };
    case "products":
      return {
        allowed: hasRole(session, ["ADMIN"]),
        reason: "Products and pricing are limited to admin users."
      };
    case "orders":
      return {
        allowed: hasRole(session, ["ADMIN", "RECEPTIONIST"]),
        reason: "Orders are limited to admin and reception users."
      };
    case "ai":
      return {
        allowed: hasRole(session, ["RECEPTIONIST"]),
        reason: "AI prediction is limited to reception users."
      };
    case "invoice":
      return {
        allowed: hasRole(session, ["ADMIN", "ACCOUNTANT"]),
        reason: "Invoices are limited to accounting and admin users."
      };
    case "payments":
      return {
        allowed: hasRole(session, ["ADMIN"]),
        reason: "Payments ledger is limited to admin users."
      };
    case "reports":
      return {
        allowed: hasRole(session, ["ADMIN", "ACCOUNTANT"]),
        reason: "Reports are limited to accounting and admin users."
      };
    case "pricing":
    case "queue-display-admin":
    case "order-management":
      return {
        allowed: hasRole(session, ["ADMIN"]),
        reason: "Admin workspace only."
      };
    case "production":
      return {
        allowed: hasRole(session, ["ADMIN", "TECHNICIAN"]),
        reason: "Production workspace is limited to admin and technician users."
      };
    case "admin":
      return {
        allowed: hasRole(session, ["ADMIN"]),
        reason: "Admin workspace only."
      };
    default:
      return { allowed: false, reason: "Page not available." };
  }
}

export function canAction(session: AuthSession, action: ActionKey): GuardResult {
  if (!session.tokens) {
    return { allowed: false, reason: "Sign in required." };
  }

  const admin = hasRole(session, ["ADMIN"]);
  const accountant = hasRole(session, ["ACCOUNTANT"]);
  const receptionist = hasRole(session, ["RECEPTIONIST"]);
  const technician = hasRole(session, ["TECHNICIAN"]);

  const requireAuthority = (authority: string, roles: Role[] = ALL_ROLES): GuardResult => ({
    allowed: hasRole(session, roles) && hasAuthority(session, authority),
    reason: `Requires ${authority}.`
  });

  switch (action) {
    case "AUTH_LOGIN":
      return { allowed: true };
    case "PROFILE_VIEW":
    case "PROFILE_UPDATE":
    case "PASSWORD_CHANGE":
      return requireAuthority("VIEW_PROFILE");
    case "CITY_READ":
      return requireAuthority("CITY_READ", ["ADMIN", "ACCOUNTANT", "RECEPTIONIST"]);
    case "CITY_CREATE":
      return requireAuthority("CITY_CREATE", ["ADMIN", "RECEPTIONIST"]);
    case "CITY_UPDATE":
      return requireAuthority("CITY_UPDATE", ["ADMIN", "RECEPTIONIST"]);
    case "CITY_DELETE":
      return requireAuthority("CITY_DELETE", ["ADMIN"]);
    case "CUSTOMER_READ":
      return requireAuthority("CUSTOMER_READ", ["ADMIN", "ACCOUNTANT", "RECEPTIONIST"]);
    case "CUSTOMER_SEARCH":
      return requireAuthority("CUSTOMER_SEARCH", ["ADMIN", "ACCOUNTANT", "RECEPTIONIST"]);
    case "CUSTOMER_CREATE":
      return requireAuthority("CUSTOMER_CREATE", ["ADMIN", "RECEPTIONIST"]);
    case "CUSTOMER_UPDATE":
      return requireAuthority("CUSTOMER_UPDATE", ["ADMIN", "RECEPTIONIST"]);
    case "CUSTOMER_DELETE":
      return requireAuthority("CUSTOMER_DELETE", ["ADMIN"]);
    case "CUSTOMER_UPDATE_NATIONAL_ID":
      return requireAuthority("CUSTOMER_UPDATE_NATIONAL_ID", ["ADMIN", "RECEPTIONIST"]);
    case "PRODUCT_READ":
      return requireAuthority("PRODUCT_READ", ["ADMIN", "ACCOUNTANT", "RECEPTIONIST"]);
    case "PRODUCT_CREATE":
      return requireAuthority("PRODUCT_CREATE", ["ADMIN"]);
    case "PRODUCT_UPDATE":
      return requireAuthority("PRODUCT_UPDATE", ["ADMIN"]);
    case "PRODUCT_UPDATE_INVENTORY":
      return requireAuthority("PRODUCT_UPDATE_INVENTORY", ["ADMIN", "ACCOUNTANT"]);
    case "PRODUCT_DELETE":
      return requireAuthority("PRODUCT_DELETE", ["ADMIN"]);
    case "ORDER_CREATE":
      return { allowed: admin || receptionist || accountant, reason: "Only admin, receptionist, or accountant can create orders." };
    case "ORDER_READ":
      return requireAuthority("ORDER_READ", ALL_ROLES);
    case "ORDER_CANCEL":
      return { allowed: admin || receptionist, reason: "Only admin or receptionist can cancel orders." };
    case "ORDER_UPDATE_STATUS":
      return requireAuthority("ORDER_UPDATE_STATUS", ["ADMIN", "ACCOUNTANT", "TECHNICIAN"]);
    case "ORDER_ITEM_READ":
      return requireAuthority("ORDER_ITEM_READ", ALL_ROLES);
    case "ORDER_ITEM_ADD":
      return requireAuthority("ORDER_ITEM_ADD", ["ADMIN", "RECEPTIONIST"]);
    case "ORDER_ITEM_UPDATE":
      return requireAuthority("ORDER_ITEM_UPDATE", ["ADMIN", "RECEPTIONIST"]);
    case "ORDER_ITEM_DELETE":
      return requireAuthority("ORDER_ITEM_DELETE", ["ADMIN", "RECEPTIONIST"]);
    case "ORDER_ITEM_UPDATE_STATUS":
      return requireAuthority("ORDER_UPDATE_STATUS", ["ADMIN", "ACCOUNTANT", "TECHNICIAN"]);
    case "ORDER_STATUS_READ":
      return requireAuthority("ORDER_STATUS_READ", ALL_ROLES);
    case "PAYMENT_CREATE":
    case "PAYMENT_READ":
    case "PAYMENT_REPORT_EXPORT":
      return {
        allowed: admin || accountant,
        reason: "Payments are limited to accounting and admin users."
      };
    case "PRODUCTION_START":
      return {
        allowed: admin || accountant || receptionist,
        reason: "Production start is limited to admin, accountant, or receptionist."
      };
    case "PRODUCTION_DASHBOARD":
      return {
        allowed: admin,
        reason: "Production dashboard is limited to admin users."
      };
    case "PRODUCTION_PIPELINE":
      return {
        allowed: admin || technician,
        reason: "Pipeline is limited to admin and technician."
      };
    case "STAGE_START":
    case "STAGE_FINISH":
      return {
        allowed: admin || technician,
        reason: "Stage execution is limited to admin and technician."
      };
    case "ETA_VIEW":
      return {
        allowed: admin || receptionist,
        reason: "ETA is limited to receptionist and admin."
      };
    case "QUEUE_STATUS":
      return { allowed: true };
    case "QUEUE_ADVANCE":
      return {
        allowed: admin || accountant || technician,
        reason: "Queue advance is limited to admin, accountant, or technician."
      };
    case "QUEUE_ISSUE_ACCOUNTING":
      return {
        allowed: admin,
        reason: "Accounting ticket issuance is admin-only in the current backend."
      };
    case "QUEUE_ISSUE_PRODUCTION":
      return {
        allowed: admin || accountant,
        reason: "Production tickets are limited to accounting and admin."
      };
    case "ADMIN_MANAGE":
    case "AI_TOOLS":
      return {
        allowed: admin,
        reason: "Admin workspace only."
      };
    default:
      return { allowed: false, reason: "Action not available." };
  }
}
