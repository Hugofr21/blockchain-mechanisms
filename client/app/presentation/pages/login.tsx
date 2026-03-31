import React from "react";
import { LoginCard } from "../../presentation/components/login/index";

export function LoginPage() {
  return (
    <main className="max-w-4xl mx-auto p-8 space-y-6">
      <h2 className="text-4xl font-extrabold tracking-tight">
        Portal de Autenticação
      </h2>
      <p className="text-gray-500">Valide a sua identidade para aceder ao DHT Ledger.</p>
       
      <LoginCard />
    </main>
  );
}