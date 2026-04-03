"use client";

import { useAuth } from "./useAuth";

export const useLogout = () => {
  const { logout } = useAuth();
const LOG_STORAGE_KEY = "@dht-ledger/global-logs";

  const executeSystemPurgeAndLogout = async () => {
    try {
  
      localStorage.clear();
      sessionStorage.clear();
      localStorage.removeItem(LOG_STORAGE_KEY);

      await logout();
    } catch (error) {
      console.error("Falha catastrófica ao purgar a sessão local ou comunicar com o Keycloak:", error);
    }
  };

  return { executeLogout: executeSystemPurgeAndLogout };
};