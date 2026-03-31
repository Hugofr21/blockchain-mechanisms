import type { NodeRow } from "../application/model/node";

export const nodes: NodeRow[] = [
  { id: "node-1", host: "172.23.0.11", port: 8001, difficulty: 4, httpPort: "80" },
  { id: "node-2", host: "172.23.0.12", port: 8002, difficulty: 4,  httpPort: "80"},
  { id: "node-3", host: "172.23.0.13", port: 8003,difficulty: 4 ,httpPort: "80"},
  { id: "node-4", host: "172.23.0.14", port: 8004,  difficulty: 4 ,httpPort: "80"},
  { id: "node-5", host: "172.23.0.15", port: 8005,  difficulty: 5 ,httpPort: "80"},
  { id: "node-6", host: "172.23.0.16", port: 8006, difficulty: 5 ,httpPort: "80"},
  { id: "node-7", host: "172.23.0.17", port: 8007,  difficulty: 5 ,httpPort: "80"},
  { id: "node-8", host: "172.23.0.18", port: 8008, difficulty: 6 ,httpPort: "80"},
  { id: "node-9", host: "172.23.0.19", port: 8009, difficulty: 6 ,httpPort: "80"},
  { id: "node-10", host: "172.23.0.20", port: 8010,  difficulty: 6 ,httpPort: "80"},
];