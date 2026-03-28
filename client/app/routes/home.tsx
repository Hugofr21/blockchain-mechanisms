import type { Route } from "./+types/home";
import { Dashboard } from "~/presentation/pages/dashboard/dashboardPage";

export function meta({}: Route.MetaArgs) {
  return [
    { title: "DHT Ledger - Nodes Dashboard" },
    {
      name: "description",
      content: "Monitorização e visualização da rede DHT Ledger e nós replicados.",
    },
  ];
}

export default function Home() {
  return <Dashboard />;
}