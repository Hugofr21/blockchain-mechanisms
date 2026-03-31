"use client";

import { useAuth } from "./useAuth";

export const useLogout = () => {
  const { logout } = useAuth();

  const executeSystemPurgeAndLogout = async () => {
    try {
  
      localStorage.clear();
      sessionStorage.clear();

      await logout();
    } catch (error) {
      console.error("Falha catastrófica ao purgar a sessão local ou comunicar com o Keycloak:", error);
    }
  };

  return { executeLogout: executeSystemPurgeAndLogout };
};