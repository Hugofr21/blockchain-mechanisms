import React from "react";
import { LoginCard } from "../../presentation/components/login/index";

export function LoginPage() {
  return (
    <main className="min-h-screen flex items-center justify-center bg-gray-50 dark:bg-gray-950 p-6">
      <div className="w-full max-w-xl">
        <div className="rounded-2xl border border-gray-200 dark:border-gray-800 bg-white dark:bg-gray-900 shadow-sm p-8 space-y-6 text-center">
          <div className="space-y-2">
            <h2 className="text-4xl font-extrabold tracking-tight text-gray-900 dark:text-white">
              Authentication Portal
            </h2>

            <p className="text-sm text-gray-600 dark:text-gray-400">
              Validate your identity to access the DHT Ledger.
            </p>
          </div>

          <div className="pt-2">
            <LoginCard />
          </div>

          <p className="text-xs font-mono text-gray-400 dark:text-gray-500">
            ACCESS CONTROLLED ENVIRONMENT · AUTH REQUIRED
          </p>
        </div>
      </div>
    </main>
  );
}