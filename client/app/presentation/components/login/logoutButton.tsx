"use client";

import React from "react";
import { useAuth } from "../../../infrastructure/hooks/useAuth";

export function LogoutButton() {
  const { logout } = useAuth();

  return (
    <button
      onClick={logout}
      className="px-4 py-2 bg-red-600 hover:bg-red-700 text-white font-mono font-bold rounded-lg transition-colors shadow-sm"
      aria-label="Encerrar sessão no sistema distribuído"
    >
      Encerrar Sessão
    </button>
  );
}