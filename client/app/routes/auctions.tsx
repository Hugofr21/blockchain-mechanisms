import { AuctionsPage } from "~/presentation/pages/auction/auctions";
import { auctions } from "~/data/auction";
import {useCreateAuction, useGetAllAuction } from "../infrastructure/hooks/auctionHook"
import { useParams } from "react-router";

export default function AuctionRouter() {
  const { targetNodePort } = useParams<{ targetNodePort: string }>();
  const { auctions, isLoading, error } = useGetAllAuction(targetNodePort || null);
  const createAuctionHook = useCreateAuction(); 

  if (!targetNodePort) {
    return (
      <main className="max-w-7xl mx-auto p-6">
        <div className="bg-red-50 text-red-700 p-6 rounded-lg font-mono">
          Falha de Navegação: Identificador do contentor alvo ausente.
        </div>
      </main>
    );
  }

  if (isLoading) return <p className="p-6 font-mono text-indigo-600 animate-pulse">A sincronizar rede Kademlia...</p>;
  if (error) return <p className="p-6 font-mono text-red-600">Erro topológico: {error}</p>;

  const handleCreation = async (payload: { description: string; startingPrice: string }) => {
    await createAuctionHook.execute(targetNodePort, payload.description, payload.startingPrice);
  };

  return (
    <AuctionsPage 
      targetNodePort={targetNodePort}
      initialAuctions={auctions || []}
      onCreatedAuction={handleCreation} 
      isCreating={createAuctionHook.isLoading}
    />
  );
}