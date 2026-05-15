import type { ReactNode } from "react";
import { useI18n } from "../app/i18n-context";
import type { FieldErrors } from "../lib/validation";
import { cx } from "../lib/utils";

export function FieldError({ errors, name }: { errors: FieldErrors; name: string }): JSX.Element | null {
  const { t } = useI18n();
  const message = errors[name];
  return message ? <small className="field-error">{t(message)}</small> : null;
}

export function ErrorSummary({ errors }: { errors: FieldErrors }): JSX.Element | null {
  return null;
}

export function PermissionNote({ allowed, reason }: Readonly<{ allowed: boolean; reason?: string }>): JSX.Element | null {
  const { t } = useI18n();
  if (allowed || !reason) {
    return null;
  }

  return <p className="permission-note">{t(reason)}</p>;
}

export function ActionButton(props: Readonly<{
  children: ReactNode;
  busyLabel?: string;
  isBusy?: boolean;
  disabled?: boolean;
  disabledReason?: string;
  className?: string;
  onClick?: () => void;
  type?: "button" | "submit";
}>): JSX.Element {
  const { t } = useI18n();

  return (
    <button
      className={cx("btn", props.className)}
      disabled={props.disabled || props.isBusy}
      onClick={props.onClick}
      title={props.disabled && props.disabledReason ? t(props.disabledReason) : undefined}
      type={props.type ?? "button"}
    >
      {props.isBusy ? t(props.busyLabel ?? "Working...") : props.children}
    </button>
  );
}
