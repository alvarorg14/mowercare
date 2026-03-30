import { useRouter } from 'expo-router';
import React, { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';

import { verifyTenantScope } from './api';
import { loginApi, logoutApi, refreshApi } from './auth-api';
import { clearRefreshToken, getRefreshToken, setRefreshToken } from './auth-storage';
import { getOrganizationIdFromAccessToken, getRoleFromAccessToken } from './jwt-org';
import { queryClient } from './queryClient';
import { setSession } from './auth/session';
import type { LoginFormValues } from './auth/login-schema';

type AuthContextValue = {
  isAuthenticated: boolean;
  isRestoringSession: boolean;
  signIn: (data: LoginFormValues) => Promise<void>;
  signOut: () => Promise<void>;
};

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const router = useRouter();
  const [isRestoringSession, setIsRestoringSession] = useState(true);
  const [isAuthenticated, setIsAuthenticated] = useState(false);

  useEffect(() => {
    let cancelled = false;

    (async () => {
      const refresh = await getRefreshToken();
      if (!refresh) {
        if (!cancelled) {
          setIsRestoringSession(false);
        }
        return;
      }
      try {
        const tokens = await refreshApi({ refreshToken: refresh });
        await setRefreshToken(tokens.refreshToken);
        const orgId = getOrganizationIdFromAccessToken(tokens.accessToken);
        setSession(tokens.accessToken, tokens.tokenType, orgId, getRoleFromAccessToken(tokens.accessToken));
        await verifyTenantScope();
        if (!cancelled) {
          setIsAuthenticated(true);
        }
      } catch {
        await clearRefreshToken();
        setSession(null, 'Bearer', null, null);
        if (!cancelled) {
          setIsAuthenticated(false);
        }
      } finally {
        if (!cancelled) {
          setIsRestoringSession(false);
        }
      }
    })();

    return () => {
      cancelled = true;
    };
  }, []);

  const signIn = useCallback(async (data: LoginFormValues) => {
    const tokens = await loginApi({
      organizationId: data.organizationId,
      email: data.email,
      password: data.password,
    });
    await setRefreshToken(tokens.refreshToken);
    const orgId = getOrganizationIdFromAccessToken(tokens.accessToken);
    if (!orgId) {
      await clearRefreshToken();
      setSession(null, 'Bearer', null, null);
      throw new Error('Access token missing organization id');
    }
    setSession(tokens.accessToken, tokens.tokenType, orgId, getRoleFromAccessToken(tokens.accessToken));
    try {
      await verifyTenantScope();
    } catch (e) {
      await clearRefreshToken();
      setSession(null, 'Bearer', null, null);
      throw e;
    }
    setIsAuthenticated(true);
    router.replace('/');
  }, [router]);

  const signOut = useCallback(async () => {
    const refresh = await getRefreshToken();
    if (refresh) {
      try {
        await logoutApi({ refreshToken: refresh });
      } catch {
        /* idempotent local sign-out even if 401 */
      }
    }
    await clearRefreshToken();
    setSession(null, 'Bearer', null, null);
    setIsAuthenticated(false);
    queryClient.clear();
    router.replace('/');
  }, [router]);

  const value = useMemo(
    () => ({
      isAuthenticated,
      isRestoringSession,
      signIn,
      signOut,
    }),
    [isAuthenticated, isRestoringSession, signIn, signOut],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error('useAuth must be used within AuthProvider');
  }
  return ctx;
}
