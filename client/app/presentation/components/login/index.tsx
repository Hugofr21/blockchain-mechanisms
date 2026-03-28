import React from "react";
import { useKeycloak } from "@react-keycloak/web";

export function LoginPage() {
  const { keycloak, initialized } = useKeycloak();

  if (!initialized) {
    return (
      <main className="p-6">
        <h1>Loading authentication...</h1>
      </main>
    );
  }

  if (keycloak.authenticated) {
    return (
      <main className="p-6 space-y-4">
        <h1 className="text-2xl font-bold">Authenticated</h1>

        <p>
          <strong>User:</strong> {keycloak.tokenParsed?.preferred_username}
        </p>

        <p className="break-all text-sm">
          <strong>Token:</strong> {keycloak.token}
        </p>

        <button
          className="px-4 py-2 bg-red-600 text-white rounded"
          onClick={() => keycloak.logout()}
        >
          Logout
        </button>
      </main>
    );
  }

  return (
    <main className="p-6 space-y-4">
      <h1 className="text-3xl font-bold">DHT Ledger Login</h1>
      <p>Please authenticate using Keycloak.</p>

      <button
        className="px-4 py-2 bg-blue-600 text-white rounded"
        onClick={() => keycloak.login()}
      >
        Login
      </button>
    </main>
  );
}