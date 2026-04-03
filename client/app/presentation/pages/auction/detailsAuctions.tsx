import React, { useState, useEffect } from "react";
import { Link } from "react-router";
import { AuctionDetailsView } from "~/presentation/components/auction/viewDetails/auctionDetailsView";
import { PlaceBidForm } from "~/presentation/components/bid/form";
import type { AuctionRow } from "~/application/model/auction";

interface CreateBidPayload {
  auctionId: string;
  valueAmount: string;
}

interface AuctionBidsPageProps {
  targetNodePort: string;
  auction: AuctionRow | null; 
  isSimulating: boolean;
  isBidding?: boolean;
  onSimulateDuplicate: () => Promise<any>;
  onSimulateRollback: () => Promise<any>;
  onSimulateStress: () => Promise<any>;
  onSubmitBidToAction: (data: CreateBidPayload) => Promise<void>;
}

export default function AuctionBidsPage({ 
  targetNodePort,
  auction: initialAuction, 
  isSimulating,
  isBidding,
  onSimulateDuplicate,
  onSimulateRollback,
  onSimulateStress,
  onSubmitBidToAction,
}: AuctionBidsPageProps) {
  const [auction, setAuction] = useState<AuctionRow | null>(initialAuction);
  const [simResult, setSimResult] = useState<{ type: 'success' | 'error', message: string } | null>(null);
  const [bidNotification, setBidNotification] = useState<{ type: 'success' | 'error', message: string } | null>(null);

  useEffect(() => {
    setAuction(initialAuction);
  }, [initialAuction]);

  const handlePlaceBid = async (id: string, bidValue: string) => {
    if (!auction) return;

    try {
      setBidNotification(null);
      
      await onSubmitBidToAction({ auctionId: id, valueAmount: bidValue });

      setBidNotification({ 
          type: 'success', 
          message: "A licitação foi submetida com sucesso e aguarda validação criptográfica (PENDING_MINING)." 
      });

      setTimeout(() => {
          window.location.reload();
      }, 3000);

    } catch (err: any) {
      const errorMsg = err.response?.data?.error || err.message || "Falha catastrófica ao processar licitação.";
      setBidNotification({ type: 'error', message: errorMsg });
    }
  };

  const executeSimulation = async (actionName: string, simulationFn: () => Promise<any>) => {
    try {
      setSimResult(null); 
      const result = await simulationFn();
      setSimResult({ type: 'success', message: `SUCESSO [${actionName}]: ${JSON.stringify(result, null, 2)}` });
    } catch (error: any) {
      const errorMsg = error.response?.data?.error || error.message || "Anomalia desconhecida.";
      setSimResult({ type: 'error', message: `FALHA [${actionName}]: ${errorMsg}` });
    }
  };

  if (!auction) {
    return (
      <main className="max-w-7xl mx-auto p-6 space-y-4">
        <h1 className="text-2xl font-bold text-gray-900 dark:text-white">
          Auction not found
        </h1>
        <Link to={`/node/${targetNodePort}/auction`} className="text-indigo-600 hover:underline text-sm">
          Back to Auctions
        </Link>
      </main>
    );
  }

  return (
    <main className="max-w-7xl mx-auto p-6 space-y-8">
      <header className="flex items-center justify-between border-b pb-4 border-gray-200 dark:border-gray-800">
        <div>
          <h1 className="text-3xl font-extrabold tracking-tight text-gray-900 dark:text-white">
            Auction Explorer
          </h1>
          <p className="text-sm font-mono text-gray-500 mt-1">Operational Target Node: {targetNodePort}</p>
        </div>
        <Link
          to={`/node/${targetNodePort}/auction`}
          className="px-4 py-2 rounded-lg bg-gray-100 dark:bg-gray-800 text-sm font-semibold hover:bg-gray-200 dark:hover:bg-gray-700 transition"
        >
          Back
        </Link>
      </header>

      <section className="bg-white dark:bg-gray-900 border border-gray-200 dark:border-gray-800 rounded-lg p-4 shadow-sm">
        <h2 className="text-sm font-bold text-gray-700 dark:text-gray-300 uppercase tracking-wider mb-4">
          Simulation Panel
        </h2>
        <div className="flex flex-wrap gap-3 mb-4">
          <button
            onClick={() => executeSimulation('Duplicação de Licitações', onSimulateDuplicate)}
            disabled={isSimulating}
            className="px-4 py-2 bg-amber-100 text-amber-800 dark:bg-amber-900 dark:text-amber-100 text-sm font-semibold rounded hover:bg-amber-200 disabled:opacity-50 transition"
          >
            Inject Duplicate Bids
          </button>
          <button
            onClick={() => executeSimulation('Teste de Carga', onSimulateStress)}
            disabled={isSimulating}
            className="px-4 py-2 bg-rose-100 text-rose-800 dark:bg-rose-900 dark:text-rose-100 text-sm font-semibold rounded hover:bg-rose-200 disabled:opacity-50 transition"
          >
           Network Overload
          </button>
          <button
            onClick={() => executeSimulation('Reversão de Estado', onSimulateRollback)}
            disabled={isSimulating}
            className="px-4 py-2 bg-purple-100 text-purple-800 dark:bg-purple-900 dark:text-purple-100 text-sm font-semibold rounded hover:bg-purple-200 disabled:opacity-50 transition"
          >
            Force Rollback
          </button>
        </div>

        {simResult && (
            <div className={`p-4 rounded border font-mono text-xs whitespace-pre-wrap ${
                simResult.type === 'success' 
                ? 'bg-green-50 border-green-200 text-green-900 dark:bg-green-900/30 dark:border-green-800 dark:text-green-300'
                : 'bg-red-50 border-red-200 text-red-900 dark:bg-red-900/30 dark:border-red-800 dark:text-red-300'
            }`}>
                <strong>Log Transacional:</strong>
                <br />
                {simResult.message}
            </div>
        )}
      </section>

      <AuctionDetailsView auction={auction} />
      {bidNotification && (
          <div className={`p-4 rounded-lg font-mono text-sm shadow-sm border ${
              bidNotification.type === 'success' 
              ? 'bg-green-50 border-green-200 text-green-800 dark:bg-green-900/30 dark:border-green-800 dark:text-green-300' 
              : 'bg-red-50 border-red-200 text-red-800 dark:bg-red-900/30 dark:border-red-800 dark:text-red-300'
          }`}>
              <strong>{bidNotification.type === 'success' ? 'STATUS [202 ACCEPTED]:' : 'ERRO:'}</strong> {bidNotification.message}
              {bidNotification.type === 'success' && <span className="block mt-2 opacity-75 animate-pulse">Forcing topological synchronization in 3 seconds...</span>}
          </div>
      )}

      {auction.open ? (
        <div className="relative">
            {isBidding && (
                <div className="absolute inset-0 bg-white/50 dark:bg-gray-900/50 backdrop-blur-sm z-10 flex items-center justify-center rounded-xl">
                    <p className="font-mono text-indigo-600 animate-pulse font-bold">Forging bids online...</p>
                </div>
            )}
            <PlaceBidForm auctionId={auction.auctionId} onSubmit={handlePlaceBid} />
        </div>
      ) : (
        <div className="border rounded-xl p-4 bg-red-50 dark:bg-red-950 text-red-700 dark:text-red-300">
            This auction is CLOSED. Bidding is blocked.
        </div>
      )}
    </main>
  );
}