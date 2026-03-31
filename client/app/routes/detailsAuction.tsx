import { useParams } from "react-router";
import AuctionBidsPage from "~/presentation/pages/auction/detailsAuctions";
import {
  useSimulateDuplicateBids,
  useSimulateRollbackAttack,
  useSimulateStressBids
} from "../infrastructure/hooks/network/enviromentTest";
import { useGetAuctionById, usePlaceBid } from "../infrastructure/hooks/auctionHook";

export default function AuctionBidsRouter() {
  const { targetNodePort, id: auctionId } = useParams<{ targetNodePort: string; id: string }>();
  
  const { auction, isLoading: isAuctionLoading, error: auctionError } = useGetAuctionById(targetNodePort || null, auctionId || "");

  const duplicateBids = useSimulateDuplicateBids();
  const rollbackAttack = useSimulateRollbackAttack();
  const stressBids = useSimulateStressBids();
  const createdBid = usePlaceBid();

  if (!targetNodePort || !auctionId) {
    return (
      <main className="max-w-7xl mx-auto p-6">
        <div className="bg-red-50 text-red-700 p-6 rounded-lg border border-red-300 shadow-sm font-mono">
          <h2 className="font-bold mb-2">Falha de Navegação Intercetada</h2>
          <p>Parâmetros de identificação topológica ou de leilão ausentes.</p>
        </div>
      </main>
    );
  }

  if (isAuctionLoading) return <p className="p-6 font-mono text-indigo-600 animate-pulse">A extrair detalhes do leilão no nó {targetNodePort}...</p>;
  if (auctionError) return <p className="p-6 font-mono text-red-600">Erro topológico: {auctionError}</p>;

  const isSimulating = duplicateBids.isLoading || rollbackAttack.isLoading || stressBids.isLoading;

  return (
    <AuctionBidsPage 
      targetNodePort={targetNodePort} 
      auction={auction}
      isSimulating={isSimulating}
      isBidding={createdBid.isLoading}
      onSimulateDuplicate={() => duplicateBids.execute(targetNodePort, auctionId)}
      onSimulateRollback={() => rollbackAttack.execute(targetNodePort, auctionId)}
      onSimulateStress={() => stressBids.execute(targetNodePort, auctionId)}
      onSubmitBidToAction={async (data) => {
        await createdBid.execute(targetNodePort, data.auctionId, data.valueAmount);
      }}
    />
  );
}