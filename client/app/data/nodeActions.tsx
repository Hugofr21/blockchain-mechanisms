import type { NodeAction } from "../presentation/components/nodeAction/types"

export const observabilityActions: NodeAction[] = [
  { id: "VIEW_ROUTING_TABLE", label: "Inspecionar KBuckets (Tabela de Roteamento)" },
  { id: "VIEW_BLOCKCHAIN", label: "Extrair Blockchain" },
  { id: "AUCTION_BID", label: "View/Criar Novo Leilão" },
  { id: "CHAOS_SYBIL", label: "Simular Ataque Sybil (Injeção de Identidades)" },
  { id: "CHAOS_ECLIPSE", label: "Simular Ataque Eclipse (Saturação Espacial)" },
  { id: "CHAOS_POISONED_BLOCK", label: "Injetar Bloco Envenenado (Falha PoW)" },
  { id: "SHUTDOWN_NODE", label: "Forçar Encerramento do Nó (Shutdown)" }
];