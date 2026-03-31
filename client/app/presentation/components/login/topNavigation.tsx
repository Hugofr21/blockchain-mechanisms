import React from "react";
import { useLogout } from "../../../infrastructure/hooks/useLogout";

export function TopNavigation() {
  const { executeLogout } = useLogout();

  return (
    <button onClick={executeLogout}>
      Sair do Sistema
    </button>
  );
}