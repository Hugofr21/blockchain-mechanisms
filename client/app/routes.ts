import { type RouteConfig, index ,route} from "@react-router/dev/routes";

export default [
  index("routes/home.tsx"),
  route("blockchain", "routes/blockchain.tsx"),
  route("auction", "routes/auctions.tsx"),
  route("auctions/:id/bids", "routes/detailsAuction.tsx"),
  route("blockchain/:height", "routes/detailsBlock.tsx"),
] satisfies RouteConfig;