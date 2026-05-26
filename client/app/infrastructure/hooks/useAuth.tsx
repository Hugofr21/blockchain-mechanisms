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
  const [isMounted, setIsMounted] = useState(false);
  const [kcInstance, setKcInstance] = useState<Keycloak | null>(null);
  const isRun = useRef(false);

  useEffect(() => {
    setIsMounted(true);
  }, []);

  useEffect(() => {
    if (!isMounted || isRun.current) return;
    isRun.current = true;

    const kc = new Keycloak({
      url: import.meta.env.VITE_KEYCLOAK_URL as string,
      realm: import.meta.env.VITE_KEYCLOAK_REALM as string,
      clientId: import.meta.env.VITE_KEYCLOAK_CLIENT as string,
    });

    kc.init({
      onLoad: 'login-required',
      responseMode: 'fragment',
      checkLoginIframe: false
    })
    .then((auth) => {
      setIsAuthenticated(auth);
      setKcInstance(kc);
      
      if (auth && kc.token) {
        window.localStorage.setItem('auth_token', kc.token);
        const cleanUrl = window.location.protocol + "//" + window.location.host + window.location.pathname;
        window.history.replaceState({}, document.title, cleanUrl);
      }
    })
    .catch((err) => {
      console.error('Fail critical the negotiation of tokens via WAF:', err);
    })
    .finally(() => {
      setIsInitializing(false);
    });

  }, [isMounted]);

  const login = () => kcInstance?.login();
  const logout = () => {
    window.localStorage.removeItem('auth_token');
    kcInstance?.logout({ redirectUri: window.location.origin });
  };

  if (!isMounted) return null;

  return (
    <AuthContext.Provider value={{ isAuthenticated, token: kcInstance?.token, login, logout, keycloak: kcInstance, isInitializing }}>
      {isInitializing ? (
        <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100vh', fontFamily: 'sans-serif' }}>
          <strong>Establishing secure session through the perimeter...</strong>
        </div>
      ) : (
        isAuthenticated && children
      )}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) throw new Error("Problem with authentication context");
  return context;
};