import React, { createContext, useCallback, useContext, useMemo, useState } from 'react';

type AuthContextValue = {
  isAuthenticated: boolean;
  signInPlaceholder: () => void;
  signOutPlaceholder: () => void;
};

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [isAuthenticated, setIsAuthenticated] = useState(false);

  const signInPlaceholder = useCallback(() => setIsAuthenticated(true), []);
  const signOutPlaceholder = useCallback(() => setIsAuthenticated(false), []);

  const value = useMemo(
    () => ({ isAuthenticated, signInPlaceholder, signOutPlaceholder }),
    [isAuthenticated, signInPlaceholder, signOutPlaceholder],
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
