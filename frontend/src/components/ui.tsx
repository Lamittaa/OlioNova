import { useEffect, useState, type PropsWithChildren, type ReactNode } from "react";
import { NavLink } from "react-router-dom";
import { useI18n } from "../app/i18n-context";
import { cx } from "../lib/utils";

export function PageHeader(props: Readonly<{
  eyebrow?: string;
  title: string;
  description?: string;
  actions?: ReactNode;
}>): JSX.Element {
  const { t } = useI18n();

  return (
    <div className="page-header">
      <div>
        <h1>{t(props.title)}</h1>
      </div>
      {props.actions ? <div className="page-header-actions">{props.actions}</div> : null}
    </div>
  );
}

export function Card(props: PropsWithChildren<Readonly<{ title?: string; subtitle?: string; actions?: ReactNode }>>): JSX.Element {
  const { t } = useI18n();

  return (
    <section className="card">
      {props.title || props.subtitle || props.actions ? (
        <div className="card-head">
          <div>
            {props.title ? <h2>{t(props.title)}</h2> : null}
            {props.subtitle ? <p>{t(props.subtitle)}</p> : null}
          </div>
          {props.actions}
        </div>
      ) : null}
      {props.children}
    </section>
  );
}

export function Banner(props: Readonly<{ tone?: "neutral" | "success" | "danger"; message: string; detail?: string }>): JSX.Element | null {
  const { t } = useI18n();
  const [visible, setVisible] = useState(true);

  useEffect(() => {
    setVisible(true);
    const timer = globalThis.setTimeout(() => setVisible(false), 5000);
    return () => globalThis.clearTimeout(timer);
  }, [props.message, props.detail, props.tone]);

  if (!visible) {
    return <></>;
  }

  const toneClass = props.tone ? `toast-${props.tone}` : "toast-neutral";
  let icon: JSX.Element;
  if (props.tone === "success") {
    icon = (
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" aria-hidden>
        <path d="M20 6L9 17l-5-5" stroke="currentColor" strokeWidth={2} strokeLinecap="round" strokeLinejoin="round" />
      </svg>
    );
  } else if (props.tone === "danger") {
    icon = (
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" aria-hidden>
        <path d="M18 6L6 18M6 6l12 12" stroke="currentColor" strokeWidth={2} strokeLinecap="round" strokeLinejoin="round" />
      </svg>
    );
  } else {
    icon = (
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" aria-hidden>
        <circle cx="12" cy="12" r="8" stroke="currentColor" strokeWidth={2} />
      </svg>
    );
  }

  return (
    <div className="toast-viewport" aria-live={props.tone === "danger" ? "assertive" : "polite"}>
      <output className={cx("toast-enter", "toast-card", toneClass)} aria-live={props.tone === "danger" ? "assertive" : "polite"}>
        <div className="icon" aria-hidden>
          {icon}
        </div>
        <div className="content">
          <div className="title">{t(props.message)}</div>
          {props.detail ? <div className="detail">{t(props.detail)}</div> : null}
        </div>
        <div className="actions" aria-hidden>
          <span />
        </div>
        <div className="toast-timer" style={{ animation: "toastProgress 5000ms linear forwards" }} aria-hidden />
      </output>
    </div>
  );
}

export function EmptyState(props: Readonly<{ title: string; description: string; action?: ReactNode }>): JSX.Element {
  const { t } = useI18n();

  return (
    <div className="empty-state">
      <h3>{t(props.title)}</h3>
      <p>{t(props.description)}</p>
      {props.action}
    </div>
  );
}

export function StatTile(props: Readonly<{ label: string; value: ReactNode; hint?: string }>): JSX.Element {
  const { t } = useI18n();

  return (
    <div className="stat-tile">
      <span>{t(props.label)}</span>
      <strong>{props.value}</strong>
      {props.hint ? <small>{t(props.hint)}</small> : null}
    </div>
  );
}

export function StatusBadge({ value }: Readonly<{ value: string | number | null | undefined }>): JSX.Element {
  const { t } = useI18n();
  const normalized = String(value ?? "unknown").toLowerCase();
  let tone: "success" | "danger" | "active" | "neutral" = "neutral";
  if (normalized.includes("complete") || normalized.includes("paid") || normalized.includes("success")) {
    tone = "success";
  } else if (normalized.includes("cancel") || normalized.includes("error") || normalized.includes("fail")) {
    tone = "danger";
  } else if (normalized.includes("progress") || normalized.includes("serv")) {
    tone = "active";
  }

  return <span className={cx("status-badge", `status-${tone}`)}>{t(String(value ?? "--"))}</span>;
}

export function ShellNavItem(props: Readonly<{ to: string; label: string }>): JSX.Element {
  const { t } = useI18n();

  return (
    <NavLink className={({ isActive }) => cx("shell-link", isActive && "shell-link-active")} to={props.to}>
      {t(props.label)}
    </NavLink>
  );
}

type Column<T> = {
  key: string;
  header: string;
  render: (row: T) => ReactNode;
};

export function DataTable<T>(props: Readonly<{
  columns: Column<T>[];
  rows: T[];
  getKey: (row: T) => string | number;
}>): JSX.Element {
  const { t } = useI18n();

  return (
    <div className="table-wrap">
      <table className="data-table">
        <thead>
          <tr>
            {props.columns.map((column) => (
              <th key={column.key}>{t(column.header)}</th>
            ))}
          </tr>
        </thead>
        <tbody>
          {props.rows.map((row) => (
            <tr key={props.getKey(row)}>
              {props.columns.map((column) => (
                <td key={column.key}>{column.render(row)}</td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
