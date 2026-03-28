export const env = {
  nodeEnv: import.meta.env.VITE_NODE_ENV,
  apiGatewayUrl: import.meta.env.VITE_API_GATEWAY_URL,
} as const;