const createApp = require("./src/app");
const env = require("./src/config/dotenv");

const { server } = createApp();

const PORT = env.PORT || 4000;
server.listen(PORT, () => {
  console.log(`Server is running on port ${PORT}`);
});

function graceful() {
  console.log("Shutdown signal received, closing HTTP server...");
  server.close(() => {
    console.log("Server closed gracefully.");
    process.exit(0);
  });
}

process.on("SIGINT", graceful);
process.on("SIGTERM", graceful);
