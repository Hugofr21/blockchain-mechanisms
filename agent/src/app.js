const express = require("express");
const http = require("http");
const infraRouter = require("./routes/infra.router");
const cors = require("cors");
const config = require("./config/dotenv");

function createApp() {
  const app = express();
  const server = http.createServer(app);

  app.use(
    cors({
      origin: config.CORS_ORIGIN,
      methods: ["GET", "POST", "PUT", "DELETE"],
      allowedHeaders: ["Content-Type", "x-user-id", "x-api-key"],
    }),
  );

  app.use(cors());
  app.use(express.json({ limit: "100kb" }));

  app.use("/api", infraRouter);
  app.get("/health", (_req, res) => res.json({ status: "ok", ts: new Date() }));

  return { app, server };
}

module.exports = createApp;
