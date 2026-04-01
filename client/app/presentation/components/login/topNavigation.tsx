"use client";

import React from "react";
import { LogOut } from "lucide-react";
import { useLogout } from "../../../infrastructure/hooks/useLogout";

export function TopNavigation() {
  const { executeLogout } = useLogout();

  return (
    <button
      onClick={executeLogout}
      type="button"
      className="inline-flex items-center gap-2 px-4 py-2 bg-red-600 hover:bg-red-700 text-white font-mono font-bold rounded-lg transition-colors shadow-sm focus:outline-none focus:ring-2 focus:ring-red-400 focus:ring-offset-2"
      aria-label="Encerrar sessão no sistema"
    >
      <LogOut className="w-4 h-4" aria-hidden="true" />
      Logout
    </button>
  );
}