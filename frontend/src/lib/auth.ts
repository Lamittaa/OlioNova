import type { JwtClaims, Role, TokenBundle } from "../types/models";

const STORAGE_KEY = "ops.auth.session";

function decodeBase64Url(value: string): string {
  const normalized = value.replace(/-/g, "+").replace(/_/g, "/");
  const padding = normalized.length % 4 === 0 ? "" : "=".repeat(4 - (normalized.length % 4));
  return atob(normalized + padding);
}

export function decodeJwtClaims(token: string): JwtClaims | null {
  try {
    const payload = token.split(".")[1];
    if (!payload) {
      return null;
    }
    return JSON.parse(decodeBase64Url(payload)) as JwtClaims;
  } catch {
    return null;
  }
}

export function deriveRole(role?: string, authorities: string[] = []): Role | null {
  if (role && ["ADMIN", "ACCOUNTANT", "RECEPTIONIST", "TECHNICIAN"].includes(role)) {
    return role as Role;
  }

  const match = authorities.find((authority) => authority.startsWith("ROLE_"));
  return match ? (match.replace("ROLE_", "") as Role) : null;
}

export function readStoredTokens(): TokenBundle | null {
  const raw = window.sessionStorage.getItem(STORAGE_KEY);
  if (!raw) {
    return null;
  }

  try {
    return JSON.parse(raw) as TokenBundle;
  } catch {
    return null;
  }
}

export function storeTokens(tokens: TokenBundle | null): void {
  if (!tokens) {
    window.sessionStorage.removeItem(STORAGE_KEY);
    return;
  }

  window.sessionStorage.setItem(STORAGE_KEY, JSON.stringify(tokens));
}
