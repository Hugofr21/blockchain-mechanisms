import React, { useState, useEffect } from "react";
import { AuctionCard } from "../../components/auction";
import { CreateAuctionForm } from "../../components/auction/form/index";
import type { AuctionRow } from "../../../application/model/auction";

interface CreateAuctionPayload {
  description: string;
  startingPrice: string;
}

interface Props {
  targetNodePort: string;
  initialAuctions: AuctionRow[];
  onCreatedAuction: (data: CreateAuctionPayload) => Promise<void>;
  isCreating?: boolean;
}

export function AuctionsPage({ targetNodePort, initialAuctions, onCreatedAuction, isCreating }: Props) {
  const [auctions, setAuctions] = useState<AuctionRow[]>(initialAuctions);
  const [notification, setNotification] = useState<{ type: 'success' | 'error', message: string } | null>(null);

  useEffect(() => {
    setAuctions(initialAuctions);
  }, [initialAuctions]);

  const handleCreateAuction = async (data: CreateAuctionPayload) => {
    try {
      setNotification(null); 
      await onCreatedAuction(data);

      setNotification({ 
          type: 'success', 
          message: "Transação submetida à Mempool com sucesso. A aguardar mineração do bloco..." 
      });

      setTimeout(() => {
          window.location.reload();
      }, 3000);

    } catch (err: any) {
  
      const errorMsg = err.response?.data?.error || err.message || "Falha ao propagar criação do leilão na rede.";
      setNotification({ type: 'error', message: `FALHA CATASTRÓFICA: ${errorMsg}` });
    }
  };

  return (
    <main className="max-w-7xl mx-auto p-6 space-y-10">
      <header className="space-y-2">
        <h1 className="text-4xl font-extrabold tracking-tight text-gray-900 dark:text-white">
          Auctions
        </h1>
        <p className="text-sm text-gray-600 dark:text-gray-400">
          Create auctions and monitor bids in the distributed ledger. <br/>
          <span className="font-mono text-indigo-600 font-bold">Nó Operacional: {targetNodePort}</span>
        </p>
      </header>

      {/* Bloco de Notificação Transacional */}
      {notification && (
          <div className={`p-4 rounded-lg font-mono text-sm shadow-sm border ${
              notification.type === 'success' 
              ? 'bg-green-50 border-green-200 text-green-800 dark:bg-green-900/30 dark:border-green-800 dark:text-green-300' 
              : 'bg-red-50 border-red-200 text-red-800 dark:bg-red-900/30 dark:border-red-800 dark:text-red-300'
          }`}>
              <strong>{notification.type === 'success' ? 'STATUS [202 ACCEPTED]:' : 'ERRO:'}</strong> {notification.message}
              {notification.type === 'success' && <span className="block mt-2 opacity-75 animate-pulse">A forçar sincronização topológica em 3 segundos...</span>}
          </div>
      )}

      <section className="rounded-2xl border border-gray-200 dark:border-gray-800 bg-white dark:bg-gray-900 shadow-sm p-6 relative">
        {isCreating && (
            <div className="absolute inset-0 bg-white/50 backdrop-blur-sm z-10 flex items-center justify-center rounded-2xl">
                <p className="font-mono text-indigo-600 animate-pulse font-bold">A forjar transação na rede...</p>
            </div>
        )}
        <CreateAuctionForm onSubmit={handleCreateAuction} />
      </section>

      <section className="space-y-4">
        <h2 className="text-2xl font-bold text-gray-900 dark:text-white">
          Active Auctions
        </h2>

        {auctions.length === 0 ? (
          <p className="text-gray-500 dark:text-gray-400 text-sm">
            No auctions created yet on this node.
          </p>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {auctions.map((auction) => (
              <AuctionCard 
                key={auction.auctionId} 
                data={auction} 
                targetNodePort={targetNodePort}
              />
            ))}
          </div>
        )}
      </section>
    </main>
  );
}