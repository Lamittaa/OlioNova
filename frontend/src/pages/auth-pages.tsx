import { useState } from "react";
import { Link, Navigate, useLocation, useNavigate } from "react-router-dom";
import { useAuth } from "../app/auth-context";
import { useI18n } from "../app/i18n-context";
import { useToastHelpers } from "../app/toast-context";
import { ActionButton, ErrorSummary, FieldError } from "../components/form-ui";
import { Card, PageHeader } from "../components/ui";
import { endpoints } from "../lib/endpoints";
import { publicAsset } from "../lib/assets";
import { apiRequest } from "../lib/http";
import { useActionState } from "../lib/use-action-state";
import {
  getApiFieldErrors,
  getApiErrorMessage,
  type FieldErrors,
  validateLogin,
  validateSetPassword
} from "../lib/validation";
import type { SetPasswordRequest } from "../types/models";

function resolveHome(role: string | null): string {
  switch (role) {
    case "ADMIN":
      return "/app/admin";
    case "ACCOUNTANT":
      return "/app/payments";
    case "RECEPTIONIST":
      return "/app/orders";
    case "TECHNICIAN":
      return "/app/production";
    default:
      return "/app/overview";
  }
}

function AuthLanguageSwitch(): JSX.Element {
  const { language, setLanguage, t } = useI18n();

  return (
    <div className="auth-language-switch language-toggle" aria-label={t("Arabic / English")}>
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
        {"\u0639"}
      </button>
    </div>
  );
}

export function LoginPage(): JSX.Element {
  const auth = useAuth();
  const { t } = useI18n();
  const { success: toastSuccess, error: toastError } = useToastHelpers();
  const navigate = useNavigate();
  const location = useLocation();
  const from = (location.state as { from?: string } | null)?.from;
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({});
  const actionState = useActionState();

  if (auth.tokens) {
    return <Navigate replace to={resolveHome(auth.role)} />;
  }

  return (
    <div className="auth-shell auth-shell-modern">
      <div className="auth-login-panel">
        <div className="auth-login-toolbar">
          <AuthLanguageSwitch />
        </div>
        <div className="auth-press-badge" aria-hidden="true">
          <img alt={t("Olive Press logo")} className="auth-press-badge-image" src={publicAsset("opslogo.jpeg")} />
        </div>
        <div className="auth-login-card auth-login-card-modern">
          <Card>
            <div className="auth-form-shell">
              <div className="auth-form-header">
                <div className="auth-wordmark-frame">
                  <img alt={t("OlioNova AI")} className="auth-wordmark-logo" src={publicAsset("ops-logo.jpeg")} />
                </div>
                <div className="auth-form-copy">
                  <h1>{t("Sign in")}</h1>
                  <p>{t("Use your work account.")}</p>
                </div>
              </div>
              <div className="auth-form-content">
                <ErrorSummary errors={fieldErrors} />
                <form
                  className="form-grid auth-login-form"
                  onSubmit={(event) => {
                    event.preventDefault();

                    const nextFieldErrors = validateLogin({ username, password });
                    setFieldErrors(nextFieldErrors);

                    if (Object.keys(nextFieldErrors).length) {
                      return;
                    }

                    void actionState.runAction("login", async () => {
                      try {
                        const role = await auth.login({ username, password });
                        toastSuccess(t("Welcome back"), role ? t(role) : undefined);
                        navigate(from || resolveHome(role), { replace: true });
                      } catch (requestError: unknown) {
                        const message = getApiErrorMessage(requestError);
                        setFieldErrors(getApiFieldErrors(requestError));
                        toastError(t("Login failed"), message);
                      }
                    });
                  }}
                >
                  <div className="field">
                    <label htmlFor="username">{t("Username")}</label>
                    <input
                      autoComplete="username"
                      id="username"
                      onChange={(event) => setUsername(event.target.value)}
                      placeholder={t("Enter your username")}
                      value={username}
                    />
                    <FieldError errors={fieldErrors} name="username" />
                  </div>
                  <div className="field">
                    <label htmlFor="password">{t("Password")}</label>
                    <input
                      autoComplete="current-password"
                      id="password"
                      onChange={(event) => setPassword(event.target.value)}
                      placeholder={t("Enter your password")}
                      type="password"
                      value={password}
                    />
                    <FieldError errors={fieldErrors} name="password" />
                  </div>
                  <div className="inline-actions">
                    <ActionButton
                      busyLabel="Signing in..."
                      className="auth-submit"
                      isBusy={actionState.isBusy("login")}
                      type="submit"
                    >
                      {t("Log In")}
                    </ActionButton>
                  </div>
                </form>
                <div className="auth-login-footer">
                  <Link to="/set-password">{t("Forgot Password?")}</Link>
                  <span className="auth-footer-divider" />
                  <span>{t("Need Help?")}</span>
                </div>
              </div>
            </div>
          </Card>
        </div>
      </div>
    </div>
  );
}

export function SetPasswordPage(): JSX.Element {
  const { t } = useI18n();
  const { success: toastSuccess, error: toastError } = useToastHelpers();
  const navigate = useNavigate();
  const [token, setToken] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({});
  const actionState = useActionState();

  return (
    <div className="auth-shell">
      <div className="auth-card">
        <AuthLanguageSwitch />
        <Card>
          <PageHeader
            eyebrow="Public Flow"
            title="Set Password"
            description="Complete first-time setup or password reset using a valid token."
          />
          <ErrorSummary errors={fieldErrors} />
          <form
            className="form-grid"
            onSubmit={(event) => {
              event.preventDefault();

              const nextFieldErrors = validateSetPassword({ token, newPassword });
              setFieldErrors(nextFieldErrors);

              if (Object.keys(nextFieldErrors).length) {
                return;
              }

              const payload: SetPasswordRequest = {
                token,
                newPassword
              };

              void actionState.runAction("set-password", async () => {
                try {
                  await apiRequest(endpoints.auth.setPassword, {
                    method: "POST",
                    auth: false,
                    body: payload
                  });
                  toastSuccess(t("Password updated"), t("Redirecting to login..."));
                  globalThis.setTimeout(() => navigate("/login", { replace: true }), 1200);
                } catch (requestError: unknown) {
                  const message = getApiErrorMessage(requestError);
                  setFieldErrors(getApiFieldErrors(requestError));
                  toastError(t("Password reset failed"), message);
                }
              });
            }}
          >
            <div className="field">
              <label htmlFor="token">{t("Token")}</label>
              <input id="token" onChange={(event) => setToken(event.target.value)} value={token} />
              <FieldError errors={fieldErrors} name="token" />
            </div>
            <div className="field">
              <label htmlFor="new-password">{t("New Password")}</label>
              <input
                id="new-password"
                minLength={8}
                onChange={(event) => setNewPassword(event.target.value)}
                type="password"
                value={newPassword}
              />
              <FieldError errors={fieldErrors} name="newPassword" />
            </div>
            <div className="inline-actions">
              <ActionButton busyLabel="Submitting..." isBusy={actionState.isBusy("set-password")} type="submit">
                {t("Set Password")}
              </ActionButton>
              <ActionButton className="ghost" onClick={() => navigate("/login")} type="button">
                {t("Back to sign in")}
              </ActionButton>
            </div>
          </form>
        </Card>
      </div>
    </div>
  );
}

export function NotFoundPage(): JSX.Element {
  const { t } = useI18n();

  return (
    <div className="auth-shell">
      <div className="auth-card">
        <Card>
          <PageHeader title="Page Not Found" description="The route you requested is not registered in this frontend." />
          <Link className="btn" to="/login">
            {t("Go to login")}
          </Link>
        </Card>
      </div>
    </div>
  );
}
