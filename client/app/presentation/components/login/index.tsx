"use client";

import React from "react";
import { useNavigate } from "react-router";
import {
  ShieldCheck,
  ShieldAlert,
  LogIn,
  LogOut,
  LayoutDashboard,
  Loader2,
} from "lucide-react";

import { useAuth } from "../../../infrastructure/hooks/useAuth";

export function LoginCard() {
  const { isInitializing, isAuthenticated, keycloak, token, login, logout } =
    useAuth();

  const navigate = useNavigate();

  if (isInitializing) {
    return (
      <div className="flex items-center justify-center w-full">
        <div className="flex items-center gap-3 text-indigo-600 font-mono text-sm animate-pulse">
          <Loader2 className="w-5 h-5 animate-spin" />
          Forging a tunnel with the identity infrastructure...
        </div>
      </div>
    );
  }

  if (isAuthenticated) {
    return (
      <section className="w-full max-w-2xl rounded-2xl border border-green-200 dark:border-green-900 bg-white dark:bg-gray-900 shadow-sm p-6 space-y-6 mx-auto">
        <header className="flex items-start justify-between gap-4">
          <div>
            <h1 className="text-2xl font-extrabold text-green-800 dark:text-green-300 flex items-center gap-2">
              <ShieldCheck className="w-6 h-6" />
              Active Cryptographic Session
            </h1>
            <p className="text-green-700 dark:text-green-400 text-sm mt-1">
              His identity was confirmed by the Keycloak server.
            </p>
          </div>

          <span className="text-xs px-3 py-1 rounded-full bg-green-100 text-green-800 dark:bg-green-950 dark:text-green-300 font-mono">
            AUTH OK
          </span>
        </header>

        <div className="space-y-3">
          <p className="font-mono text-sm text-gray-900 dark:text-gray-200">
            <strong>Subject Identity:</strong>{" "}
            {keycloak?.tokenParsed?.preferred_username}
          </p>

          <div className="rounded-xl border border-green-100 dark:border-green-900 bg-gray-50 dark:bg-gray-950 shadow-inner p-4">
            <p className="text-xs font-mono text-gray-600 dark:text-gray-400 mb-2">
              <strong>Token the provider (JWT):</strong>
            </p>

            <p className="break-all text-xs font-mono text-gray-700 dark:text-gray-300">
              {token}
            </p>
          </div>
        </div>

        <div className="flex flex-col sm:flex-row gap-3 pt-4 border-t border-green-200 dark:border-green-900">
          <button
            className="flex-1 inline-flex items-center justify-center gap-2 px-6 py-2 bg-green-700 hover:bg-green-800 text-white font-bold rounded-lg transition shadow-sm focus:outline-none focus:ring-2 focus:ring-green-400 focus:ring-offset-2"
            onClick={() => navigate("/index")}
            type="button"
          >
            <LayoutDashboard className="w-4 h-4" />
            Dashboard
          </button>

          <button
            className="inline-flex items-center justify-center gap-2 px-6 py-2 bg-red-100 hover:bg-red-200 text-red-800 dark:bg-red-950 dark:hover:bg-red-900 dark:text-red-200 font-semibold rounded-lg transition shadow-sm focus:outline-none focus:ring-2 focus:ring-red-400 focus:ring-offset-2"
            onClick={logout}
            type="button"
          >
            <LogOut className="w-4 h-4" />
            Logout
          </button>
        </div>
      </section>
    );
  }

  return (
    <section className="w-full max-w-md rounded-2xl border border-gray-200 dark:border-gray-800 bg-white dark:bg-gray-900 shadow-sm p-6 space-y-4 mx-auto">
      <header>
        <h1 className="text-3xl font-extrabold text-gray-900 dark:text-white flex items-center gap-2">
          <ShieldAlert className="w-7 h-7 text-indigo-600" />
          Restricted Access
        </h1>

        <p className="text-gray-600 dark:text-gray-400 mt-2 text-sm">
          The network topology requires cryptographic validation. Access is
          currently blocked.
        </p>
      </header>

      <button
        className="w-full inline-flex items-center justify-center gap-2 px-6 py-3 bg-indigo-600 hover:bg-indigo-700 text-white font-bold rounded-lg transition shadow-sm focus:outline-none focus:ring-2 focus:ring-indigo-400 focus:ring-offset-2"
        onClick={login}
        type="button"
      >
        <LogIn className="w-4 h-4" />
        Authenticate with Keycloak
      </button>

      <div className="pt-3 border-t border-gray-200 dark:border-gray-800 text-xs font-mono text-gray-500 dark:text-gray-400 flex items-center gap-2">
        <ShieldCheck className="w-4 h-4" />
        Session required to access distributed services.
      </div>
    </section>
  );
}