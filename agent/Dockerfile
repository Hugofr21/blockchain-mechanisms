FROM node:18-alpine AS builder
WORKDIR /app

COPY package*.json ./
RUN npm ci --only=production && npm cache clean --force


COPY . .

FROM node:18-alpine AS runtime
WORKDIR /app


COPY --from=builder /app /app

RUN mkdir -p logs

EXPOSE 4000


ENV NODE_ENV=production \
    PORT=4000 \
    LOG_LEVEL=info \
    AUDIT_LOG_PATH=logs/audit.log

HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
    CMD wget -qO- http://localhost:${PORT}/health || exit 1


CMD ["node", "index.js"]