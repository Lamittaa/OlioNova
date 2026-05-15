import type { ApiError } from "../types/models";

type ResponseType = "json" | "blob" | "text";

interface ApiRequestOptions {
  method?: "GET" | "POST" | "PUT" | "PATCH" | "DELETE";
  body?: unknown;
  headers?: Record<string, string>;
  auth?: boolean;
  responseType?: ResponseType;
  signal?: AbortSignal;
}

interface AuthRuntime {
  getAccessToken: () => string | null;
  refreshTokens: () => Promise<boolean>;
  handleAuthFailure: () => void;
}

const API_BASE_URL =
  (import.meta.env.VITE_API_BASE_URL as string | undefined) ?? (import.meta.env.DEV ? "" : "http://localhost:8080");
const IS_DEV = import.meta.env.DEV;

let authRuntime: AuthRuntime = {
  getAccessToken: () => null,
  refreshTokens: async () => false,
  handleAuthFailure: () => undefined
};

let refreshPromise: Promise<boolean> | null = null;

export function configureHttpAuth(runtime: Partial<AuthRuntime>): void {
  authRuntime = { ...authRuntime, ...runtime };
}

function createApiError(fallbackStatus: number, fallbackPath: string, payload?: Partial<ApiError>): ApiError {
  return {
    status: payload?.status ?? fallbackStatus,
    message: payload?.message ?? "Request failed",
    error: payload?.error,
    code: payload?.code,
    path: payload?.path ?? fallbackPath,
    timestamp: payload?.timestamp,
    errors: payload?.errors ?? []
  };
}

async function parseError(response: Response): Promise<ApiError> {
  const path = new URL(response.url).pathname;
  const contentType = response.headers.get("content-type") ?? "";

  if (contentType.includes("application/json")) {
    const payload = (await response.json()) as Partial<ApiError>;
    return createApiError(response.status, path, payload);
  }

  const text = await response.text();
  return createApiError(response.status, path, {
    message: text || response.statusText || "Request failed"
  });
}

function createNetworkError(path: string, error: unknown): ApiError {
  if (error instanceof DOMException && error.name === "AbortError") {
    return createApiError(499, path, {
      message: "The request was interrupted before it finished."
    });
  }

  return createApiError(0, path, {
    message:
      "Unable to reach the API. In development, restart the frontend so requests go through the Vite proxy, then verify the gateway is running."
  });
}

function trace(message: string, meta?: unknown): void {
  if (IS_DEV) {
    console.info(`[OPS TRACE] ${message}`, meta ?? "");
  }
}

async function consumeResponse<T>(response: Response, responseType: ResponseType): Promise<T> {
  if (response.status === 204) {
    return undefined as T;
  }

  if (responseType === "blob") {
    return (await response.blob()) as T;
  }

  if (responseType === "text") {
    return (await response.text()) as T;
  }

  const contentType = response.headers.get("content-type") ?? "";
  const text = await response.text();
  if (!text) {
    return undefined as T;
  }

  if (contentType.includes("application/json")) {
    return JSON.parse(text) as T;
  }

  return text as T;
}

export async function apiRequest<T>(path: string, options: ApiRequestOptions = {}, canRetry = true): Promise<T> {
  const { method = "GET", body, headers = {}, auth = true, responseType = "json", signal } = options;
  const requestHeaders = new Headers(headers);
  const init: RequestInit = {
    method,
    headers: requestHeaders,
    signal
  };

  if (auth) {
    const accessToken = authRuntime.getAccessToken();
    if (accessToken) {
      requestHeaders.set("Authorization", `Bearer ${accessToken}`);
    }
  }

  if (body instanceof FormData) {
    init.body = body;
  } else if (body !== undefined) {
    requestHeaders.set("Content-Type", "application/json");
    init.body = JSON.stringify(body);
  }

  const url = `${API_BASE_URL}${path}`;
  trace(`${method} ${path}`);

  let response: Response;

  try {
    response = await fetch(url, init);
  } catch (requestError: unknown) {
    const error = createNetworkError(path, requestError);
    trace(`ERROR ${method} ${path}`, error);
    throw error;
  }

  if (response.status === 401 && auth && canRetry) {
    if (!refreshPromise) {
      refreshPromise = authRuntime.refreshTokens().finally(() => {
        refreshPromise = null;
      });
    }

    const refreshed = await refreshPromise;
    if (refreshed) {
      return apiRequest<T>(path, options, false);
    }

    authRuntime.handleAuthFailure();
  }

  if (!response.ok) {
    const error = await parseError(response);
    trace(`ERROR ${method} ${path}`, error);
    throw error;
  }

  const payload = await consumeResponse<T>(response, responseType);
  trace(`OK ${method} ${path}`, payload);
  return payload;
}
