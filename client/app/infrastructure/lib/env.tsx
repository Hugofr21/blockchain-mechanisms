export const env = {
  nodeEnv: import.meta.env.VITE_NODE_ENV,

  network: {
    bootstrap: import.meta.env.VITE_BOOTSTRAP_URL,

    peers: [
      import.meta.env.VITE_PEER_1_URL,
      import.meta.env.VITE_PEER_2_URL,
      import.meta.env.VITE_PEER_3_URL,
      import.meta.env.VITE_PEER_4_URL,
      import.meta.env.VITE_PEER_5_URL,
      import.meta.env.VITE_PEER_6_URL,
      import.meta.env.VITE_PEER_7_URL,
      import.meta.env.VITE_PEER_8_URL,
      import.meta.env.VITE_PEER_9_URL,
      import.meta.env.VITE_PEER_10_URL,
    ].filter(Boolean),
  },
} as const;