import React from "react";
import { LoginCard } from "../../presentation/components/login/index";

export function LoginPage() {
  return (
    <main className="min-h-screen flex items-center justify-center bg-gray-50 dark:bg-gray-950 p-6">
      <div className="w-full max-w-4xl space-y-6 text-center">
        <h2 className="text-4xl font-extrabold tracking-tight text-gray-900 dark:text-white">
          Authentication Portal
        </h2>

        <p className="text-gray-500 dark:text-gray-400">
          Validate your identity to access the DHT Ledger.
        </p>

        <LoginCard />
      </div>
    </main>
  );
}