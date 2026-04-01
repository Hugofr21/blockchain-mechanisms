import { useParams } from "react-router";
import { AuctionsPage } from "~/presentation/pages/auction/auctions";
import { useCreateAuction, useGetAllAuction, useTestAuctionThisNode } from "../infrastructure/hooks/auctionHook";

export default function AuctionRouter() {
  const { targetNodePort } = useParams<{ targetNodePort: string }>();
  
  const { auctions, isLoading, error } = useGetAllAuction(targetNodePort || null);
  const createAuctionHook = useCreateAuction(); 
  const testAuctionHook = useTestAuctionThisNode(); 

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

  const handleTest = async () => {
    await testAuctionHook.execute(targetNodePort);
  };

  return (
    <>
      <AuctionsPage 
        targetNodePort={targetNodePort}
        initialAuctions={auctions || []}
        onCreatedAuction={handleCreation} 
        isCreating={createAuctionHook.isLoading}
        onTestAuction={handleTest}
        isTesting={testAuctionHook.isLoading}
      />
    </>
  );
}