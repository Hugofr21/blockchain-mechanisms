"use client";

import React from "react";
import { useAuth } from "../../../infrastructure/hooks/useAuth";
import { LogoutButton } from "../login/logoutButton";

export function TopHeader() {
  const { keycloak, token } = useAuth();
  if (!keycloak || !token) return null;

  return (
    <header className="sticky top-0 z-50 w-full border-b border-gray-200 bg-white/80 backdrop-blur-md dark:border-gray-800 dark:bg-gray-950/80 shadow-sm">
      <div className="flex items-center justify-between h-16 px-6 max-w-7xl mx-auto">
        <div className="flex flex-col">
          <span className="text-sm font-bold text-gray-900 dark:text-white uppercase tracking-wider">
            Manage data and functionality of the DHT leger system.
          </span>
          <span className="font-mono text-xs text-indigo-600 dark:text-indigo-400">
            Identity: {keycloak.tokenParsed?.preferred_username}
          </span>
        </div>

        <div className="flex items-center gap-6">
          <div className="hidden md:flex flex-col text-right max-w-xs">
            <span className="text-[10px] font-bold text-gray-400 uppercase">
             Bearer's Signature (JWT)
            </span>
            <span className="font-mono text-[10px] text-gray-500 truncate" title={token}>
              {token.substring(0, 32)}...
            </span>
          </div>
          
          <LogoutButton />
        </div>
      </div>
    </header>
  );
}