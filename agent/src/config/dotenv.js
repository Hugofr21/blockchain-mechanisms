const fs = require("fs");
const path = require("path");
const dotenv = require("dotenv");
const { z } = require("zod");

if (process.env.NODE_ENV !== "production") {
  const envPath = path.resolve(process.cwd(), ".env");
  if (fs.existsSync(envPath)) dotenv.config({ path: envPath });
}

const schema = z.object({
  NODE_ENV: z
    .enum(["development", "production", "test"])
    .default("development"),
  PORT: z.coerce.number().default(4000),
  CORS_ORIGIN: z.string().optional(),
  LOG_LEVEL: z
    .enum(["fatal", "error", "warn", "info", "debug", "trace"])
    .default("info"),

  AUDIT_LOG_PATH: z.string().default("logs/audit.log"),
  VAULT_SECRET_PASS: z.string().optional(),
});

module.exports = schema.parse(process.env);
