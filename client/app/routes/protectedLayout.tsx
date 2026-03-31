import { Outlet, Navigate } from "react-router";
import { useAuth } from "../infrastructure/hooks/useAuth";
import React from "react";

export default function ProtectedLayout() {
  const { isAuthenticated, isInitializing } = useAuth();

  if (isInitializing) {
    return (
      <div className="flex items-center justify-center min-h-screen font-mono text-indigo-600 animate-pulse">
        A validar a integridade da sessão criptográfica...
      </div>
    );
  }

  if (!isAuthenticated) {
    return <Navigate to="/" replace />;
  }

  return <Outlet />;
}