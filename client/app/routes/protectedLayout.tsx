import { Outlet, Navigate } from "react-router";
import { useAuth } from "../infrastructure/hooks/useAuth";
import React from "react";
import { TopHeader } from "../presentation/components/layout/TopHeader";

export default function ProtectedLayout() {
  const { isAuthenticated, isInitializing } = useAuth();

  if (isInitializing) {
    return (
      <div className="flex items-center justify-center min-h-screen font-mono text-indigo-600 animate-pulse bg-gray-50 dark:bg-gray-950">
        A validar a integridade da sessão criptográfica...
      </div>
    );
  }

  if (!isAuthenticated) {
    return <Navigate to="/" replace />;
  }

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-950 flex flex-col">
      <TopHeader />
    
      <div className="flex-1">
        <Outlet />
      </div>
    </div>
  );
}