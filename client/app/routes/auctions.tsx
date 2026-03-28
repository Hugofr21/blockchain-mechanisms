import { AuctionsPage } from "~/presentation/pages/auction/auctions";
import { auctions } from "~/data/auction";

export default function AuctionRouter() {
  return <AuctionsPage initialAuctions={auctions} />;
}