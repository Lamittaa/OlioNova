import {
  createContext,
  useCallback,
  useContext,
  useMemo,
  useRef,
  useState,
  type PropsWithChildren
} from "react";
import { useI18n } from "./i18n-context";
import "./toast.css";

export type ToastTone = "neutral" | "success" | "danger";

export type ToastPayload = {
  id: string;
  tone: ToastTone;
  message: string;
  detail?: string;
  createdAt: number;
  durationMs: number;
};

type ToastInput = Omit<ToastPayload, "id" | "createdAt" | "durationMs"> & { id?: string; durationMs?: number };

type ToastContextValue = {
  toasts: ToastPayload[];
  pushToast: (toast: ToastInput) => void;
  dismissToast: (id: string) => void;
  clearToasts: () => void;
};

const ToastContext = createContext<ToastContextValue | undefined>(undefined);

function makeId(): string {
  return `t_${Math.random().toString(16).slice(2)}_${Date.now().toString(16)}`;
}

function getToastToneClass(tone: ToastTone): string {
  if (tone === "success") {
    return "toast-success";
  }

  if (tone === "danger") {
    return "toast-danger";
  }

  return "toast-neutral";
}

function getToastLiveMode(tone: ToastTone): "assertive" | "polite" {
  return tone === "danger" ? "assertive" : "polite";
}

function ToneIcon({ tone }: Readonly<{ tone: ToastTone }>): JSX.Element {
  if (tone === "success") {
    return (
      <svg width="20" height="20" viewBox="0 0 24 24" fill="none" aria-hidden>
        <path d="M20 6L9 17l-5-5" stroke="currentColor" strokeWidth={2} strokeLinecap="round" strokeLinejoin="round" />
      </svg>
    );
  }

  if (tone === "danger") {
    return (
      <svg width="20" height="20" viewBox="0 0 24 24" fill="none" aria-hidden>
        <path d="M18 6L6 18M6 6l12 12" stroke="currentColor" strokeWidth={2} strokeLinecap="round" strokeLinejoin="round" />
      </svg>
    );
  }

  return (
    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" aria-hidden>
      <circle cx="12" cy="12" r="8" stroke="currentColor" strokeWidth={2} />
    </svg>
  );
}

function ToastItem({ toast, onDismiss }: Readonly<{ toast: ToastPayload; onDismiss: (id: string) => void }>): JSX.Element {
  const toneClass = getToastToneClass(toast.tone);
  const liveMode = getToastLiveMode(toast.tone);

  return (
    <output className={`toast-card ${toneClass}`} aria-live={liveMode}>
      <div className="icon" aria-hidden>
        <ToneIcon tone={toast.tone} />
      </div>
      <div className="content">
        <div className="title">{toast.message}</div>
        {toast.detail ? <div className="detail">{toast.detail}</div> : null}
      </div>
      <div className="actions">
        <button className="toast-dismiss" aria-label="Dismiss notification" onClick={() => onDismiss(toast.id)}>
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" aria-hidden>
            <path d="M18 6L6 18M6 6l12 12" stroke="currentColor" strokeWidth={2} strokeLinecap="round" strokeLinejoin="round" />
          </svg>
        </button>
      </div>

      <div className="toast-timer" style={{ animation: `toastProgress ${toast.durationMs}ms linear forwards` }} aria-hidden />
    </output>
  );
}

function ToastViewport({ toasts, onDismiss }: Readonly<{ toasts: ToastPayload[]; onDismiss: (id: string) => void }>): JSX.Element {
  return (
    <div className="toast-viewport" aria-live="polite">
      {toasts.map((toast) => (
        <div key={toast.id} className="toast-enter">
          <ToastItem toast={toast} onDismiss={onDismiss} />
        </div>
      ))}
    </div>
  );
}

export function ToastProvider({ children }: Readonly<PropsWithChildren>): JSX.Element {
  const [toasts, setToasts] = useState<ToastPayload[]>([]);
  const toastsRef = useRef<ToastPayload[]>([]);
  const timersRef = useRef<Map<string, ReturnType<typeof globalThis.setTimeout>>>(new Map());

  const commitToasts = useCallback((nextToasts: ToastPayload[]) => {
    toastsRef.current = nextToasts;
    setToasts(nextToasts);
  }, []);

  const dismissToast = useCallback((id: string) => {
    const timerId = timersRef.current.get(id);
    if (timerId) {
      globalThis.clearTimeout(timerId);
      timersRef.current.delete(id);
    }

    commitToasts(toastsRef.current.filter((toast) => toast.id !== id));
  }, [commitToasts]);

  const clearToasts = useCallback(() => {
    for (const timerId of timersRef.current.values()) {
      globalThis.clearTimeout(timerId);
    }
    timersRef.current.clear();
    commitToasts([]);
  }, [commitToasts]);

  const pushToast = useCallback(
    (toast: ToastInput) => {
      const id = toast.id ?? makeId();
      const next: ToastPayload = {
        id,
        tone: toast.tone,
        message: toast.message,
        detail: toast.detail,
        createdAt: Date.now(),
        durationMs: toast.durationMs ?? 5000
      };

      const nextList = [...toastsRef.current, next].slice(-3);
      commitToasts(nextList);

      const timeoutMs = toast.durationMs ?? 5000;
      const timer = globalThis.setTimeout(() => dismissToast(id), timeoutMs);
      timersRef.current.set(id, timer);
    },
    [dismissToast, commitToasts]
  );

  const value = useMemo<ToastContextValue>(
    () => ({ toasts, pushToast, dismissToast, clearToasts }),
    [toasts, pushToast, dismissToast, clearToasts]
  );

  return (
    <ToastContext.Provider value={value}>
      {children}
      <ToastViewport toasts={toasts} onDismiss={dismissToast} />
    </ToastContext.Provider>
  );
}

// Convenience hook: use inside components to show success/error toasts easily.
export function useToastHelpers() {
  const { pushToast } = useToasts();
  const { t } = useI18n();

  const success = useCallback(
    (message: string, detail?: string, durationMs = 5000) =>
      pushToast({ tone: "success", message: t(message), detail: detail ? t(detail) : undefined, durationMs }),
    [pushToast, t]
  );

  const error = useCallback(
    (message: string, detail?: string, durationMs = 5000) =>
      pushToast({ tone: "danger", message: t(message), detail: detail ? t(detail) : undefined, durationMs }),
    [pushToast, t]
  );

  const neutral = useCallback(
    (message: string, detail?: string, durationMs = 5000) =>
      pushToast({ tone: "neutral", message: t(message), detail: detail ? t(detail) : undefined, durationMs }),
    [pushToast, t]
  );

  return { success, error, neutral };
}

export function useToasts(): ToastContextValue {
  const ctx = useContext(ToastContext);
  if (!ctx) {
    throw new Error("useToasts must be used inside ToastProvider");
  }
  return ctx;
}

