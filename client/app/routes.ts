import { type RouteConfig, index ,route} from "@react-router/dev/routes";

export default [
    index("routes/home.tsx"),
    route("node/:targetNodePort/blockchain", "routes/blockchain.tsx"),
    route("node/:targetNodePort/blockchain/:hash", "routes/detailsBlock.tsx"),
    route("node/:targetNodePort/auction", "routes/auctions.tsx"),
    route("node/:targetNodePort/auctions/:id/bids", "routes/detailsAuction.tsx"),
    route("node/:targetNodePort/network", "routes/network.tsx"),
] satisfies RouteConfig;