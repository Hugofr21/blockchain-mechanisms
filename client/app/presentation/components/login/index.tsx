"use client";

import React from "react";
import { useAuth } from "../../../infrastructure/hooks/useAuth";

export function LoginCard() {
  const { isInitializing, isAuthenticated, keycloak, token, login, logout } = useAuth();

  if (isInitializing) {
    return (
      <main className="p-6 font-mono text-indigo-600 animate-pulse">
        A forjar túnel com a infraestrutura de identidade...
      </main>
    );
  }

  if (isAuthenticated) {
    return (
      <main className="p-6 space-y-4 border rounded-xl bg-green-50 border-green-200 shadow-sm">
        <h1 className="text-2xl font-bold text-green-800">Sessão Criptográfica Ativa</h1>

        <p className="font-mono text-sm text-green-900">
          <strong>Identidade Subjacente:</strong> {keycloak?.tokenParsed?.preferred_username}
        </p>

        <p className="break-all text-xs font-mono text-gray-600 bg-white p-3 rounded border border-green-100 shadow-inner">
          <strong>Token de Portador (JWT):</strong> {token}
        </p>

        <button
          className="px-4 py-2 bg-red-600 hover:bg-red-700 text-white font-semibold rounded transition shadow-sm"
          onClick={logout}
        >
          Destruir Sessão
        </button>
      </main>
    );
  }

  return (
    <main className="p-6 space-y-4 border rounded-xl bg-gray-50 border-gray-200 shadow-sm">
      <h1 className="text-3xl font-bold text-gray-900">Acesso Restrito</h1>
      <p className="text-gray-600">
        A topologia da rede requer validação criptográfica. O acesso encontra-se atualmente bloqueado.
      </p>

      <button
        className="px-6 py-2 bg-indigo-600 hover:bg-indigo-700 text-white font-bold rounded-lg transition shadow-sm"
        onClick={login}
      >
        Autenticar no Keycloak
      </button>
    </main>
  );
}