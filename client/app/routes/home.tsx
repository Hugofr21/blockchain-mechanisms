import type { Route } from "./+types/home";
import { Dashboard } from "~/presentation/pages/dashboard/dashboardPage";
import { useGlobalNetworkData } from "~/infrastructure/hooks/network/globalNetwork";

import {
  useSimulateSybilAttack,
  useSimulateEclipseAttack,
  useSimulatePoisonedBlock
} from "../infrastructure/hooks/network/enviromentTest";

export function meta({}: Route.MetaArgs) {
  return [
    { title: "DHT Ledger - Global Command Center" },
    {
      name: "description",
      content: "Monitorização e controlo global da topologia descentralizada.",
    },
  ];
}

export default function Home() {
  const { nodes, loading, error } = useGlobalNetworkData();
  
  const sybil = useSimulateSybilAttack();
  const eclipse = useSimulateEclipseAttack();
  const poison = useSimulatePoisonedBlock();
  
  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-screen bg-gray-50 dark:bg-gray-950">
        <p className="text-indigo-600 animate-pulse font-mono font-semibold tracking-wide">
          A varrer infraestrutura e a estabelecer túneis de telemetria...
        </p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="flex items-center justify-center min-h-screen bg-gray-50 dark:bg-gray-950">
        <div className="p-6 bg-red-100 border-l-4 border-red-600 text-red-800 rounded-lg shadow-md max-w-2xl">
          <h2 className="font-bold text-xl mb-3">Colapso de Observabilidade</h2>
          <p className="font-mono text-sm break-words">{error}</p>
        </div>
      </div>
    );
  }

  return (
      <Dashboard 
          nodes={nodes} 
          onSimulateSybil={(nodeId) => sybil.execute(nodeId)}
          onSimulateEclipse={(nodeId) => eclipse.execute(nodeId)}
          onSimulatePoison={(nodeId) => poison.execute(nodeId)}
      />
  );
}