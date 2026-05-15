import {
  createContext,
  useContext,
  useEffect,
  useMemo,
  useState,
  type PropsWithChildren
} from "react";
import { endpoints } from "../lib/endpoints";
import { decodeJwtClaims, deriveRole, readStoredTokens, storeTokens } from "../lib/auth";
import { apiRequest, configureHttpAuth } from "../lib/http";
import type {
  AuthSession,
  LoginRequest,
  Profile,
  RefreshRequest,
  Role,
  TokenBundle
} from "../types/models";

interface AuthContextValue extends AuthSession {
  initialized: boolean;
  login: (request: LoginRequest) => Promise<Role | null>;
  logout: () => Promise<void>;
  refreshSession: () => Promise<boolean>;
  reloadProfile: () => Promise<Profile | null>;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);
const profileRequestPromises = new Map<string, Promise<Profile>>();

function buildSession(tokens: TokenBundle | null, profile: Profile | null): AuthSession {
  const claims = tokens ? decodeJwtClaims(tokens.accessToken) : null;
  const authorities = claims?.authorities ?? [];
  const role = deriveRole(tokens?.role, authorities);

  return {
    tokens,
    claims,
    authorities,
    role,
    profile
  };
}

export function AuthProvider({ children }: PropsWithChildren): JSX.Element {
  const [tokens, setTokens] = useState<TokenBundle | null>(() => readStoredTokens());
  const [profile, setProfile] = useState<Profile | null>(null);
  const [initialized, setInitialized] = useState(false);

  const session = useMemo(() => buildSession(tokens, profile), [tokens, profile]);

  const applyTokens = (nextTokens: TokenBundle | null): void => {
    setTokens(nextTokens);
    storeTokens(nextTokens);
  };

  const reloadProfile = async (accessTokenOverride?: string): Promise<Profile | null> => {
    const accessToken = accessTokenOverride ?? tokens?.accessToken;

    if (!accessToken) {
      setProfile(null);
      return null;
    }

    try {
      let profileRequest = profileRequestPromises.get(accessToken);
      if (!profileRequest) {
        profileRequest = apiRequest<Profile>(endpoints.profile.me, {
          auth: !accessTokenOverride,
          headers: accessTokenOverride
            ? {
                Authorization: `Bearer ${accessTokenOverride}`
              }
            : undefined
        }).finally(() => {
          profileRequestPromises.delete(accessToken);
        });
        profileRequestPromises.set(accessToken, profileRequest);
      }

      const nextProfile = await profileRequest;
      setProfile(nextProfile);
      return nextProfile;
    } catch {
      setProfile(null);
      return null;
    }
  };

  const logout = async (): Promise<void> => {
    try {
      if (tokens?.accessToken) {
        await apiRequest(endpoints.auth.logout, { method: "POST" });
      }
    } catch {
      // Best-effort logout.
    } finally {
      applyTokens(null);
      setProfile(null);
    }
  };

  const refreshSession = async (): Promise<boolean> => {
    if (!tokens?.refreshToken) {
      return false;
    }

    try {
      const nextTokens = await apiRequest<TokenBundle>(endpoints.auth.refresh, {
        method: "POST",
        auth: false,
        body: {
          refreshToken: tokens.refreshToken
        } satisfies RefreshRequest
      });

      applyTokens(nextTokens);
      await reloadProfile(nextTokens.accessToken);
      return true;
    } catch {
      applyTokens(null);
      setProfile(null);
      return false;
    }
  };

  const login = async (request: LoginRequest): Promise<Role | null> => {
    const nextTokens = await apiRequest<TokenBundle>(endpoints.auth.login, {
      method: "POST",
      auth: false,
      body: request
    });

    applyTokens(nextTokens);
    await reloadProfile(nextTokens.accessToken);
    return deriveRole(nextTokens.role, decodeJwtClaims(nextTokens.accessToken)?.authorities ?? []);
  };

  useEffect(() => {
    configureHttpAuth({
      getAccessToken: () => tokens?.accessToken ?? null,
      refreshTokens: refreshSession,
      handleAuthFailure: () => {
        void logout();
      }
    });
  }, [tokens]);

  useEffect(() => {
    let cancelled = false;

    async function bootstrap(): Promise<void> {
      if (tokens?.accessToken) {
        await reloadProfile();
      }
      if (!cancelled) {
        setInitialized(true);
      }
    }

    void bootstrap();

    return () => {
      cancelled = true;
    };
  }, []);

  const value = useMemo<AuthContextValue>(
    () => ({
      ...session,
      initialized,
      login,
      logout,
      refreshSession,
      reloadProfile
    }),
    [session, initialized]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth must be used inside AuthProvider");
  }
  return context;
}
