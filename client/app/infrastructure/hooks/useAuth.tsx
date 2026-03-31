"use client"; 

import React, { createContext, useContext, useEffect, useState, useRef } from 'react';
import Keycloak from 'keycloak-js';

interface AuthContextType {
  isAuthenticated: boolean;
  token: string | undefined;
  login: () => void;
  logout: () => void;
  keycloak: Keycloak | null;
  isInitializing: boolean;
}

const AuthContext = createContext<AuthContextType | null>(null);

export const AuthProvider = ({ children }: { children: React.ReactNode }) => {
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [isInitializing, setIsInitializing] = useState(true);
  const [kcInstance, setKcInstance] = useState<Keycloak | null>(null);
  const isRun = useRef(false);

  useEffect(() => {
    if (typeof window === "undefined" || isRun.current) return;
    isRun.current = true;

    const kc = new Keycloak({
      url: import.meta.env.VITE_KEYCLOAK_URL as string,
      realm: import.meta.env.VITE_KEYCLOAK_REALM as string,
      clientId: import.meta.env.VITE_KEYCLOAK_CLIENT as string,
    });

    kc.init({
      onLoad: 'check-sso',
      checkLoginIframe: false
    })
    .then((auth) => {
      setIsAuthenticated(auth);
      setKcInstance(kc);
    })
    .catch((err) => console.error('Falha de ligação ao Keycloak:', err))
    .finally(() => setIsInitializing(false));

  }, []);

  const login = () => kcInstance?.login();
  const logout = () => kcInstance?.logout({ redirectUri: window.location.origin });

  return (
    <AuthContext.Provider value={{ isAuthenticated, token: kcInstance?.token, login, logout, keycloak: kcInstance, isInitializing }}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) throw new Error("useAuth tem de ser invocado dentro do AuthProvider.");
  return context;
};