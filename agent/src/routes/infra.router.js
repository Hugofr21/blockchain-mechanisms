const express = require("express");
const Docker = require("dockerode");
const config = require("../config/dotenv");

const router = express.Router();
const docker = new Docker({ socketPath: "/var/run/docker.sock" });

router.get("/infra/peers", async (req, res) => {
  try {
    const containers = await docker.listContainers({
      all: false,
      filters: { name: ["peer-"] },
    });

    const ports = containers
      .map((c) => parseInt(c.Names[0].replace(/^\/peer-/, ""), 10))
      .filter((n) => !Number.isNaN(n));

    const activePeers = ["bootstrap", ...ports.map((p) => p.toString())].sort();

    res.status(200).json({ targets: activePeers });
  } catch (err) {
    res.status(500).json({ error: "Failed to read Docker infrastructure" });
  }
});

router.post("/infra/scale", async (req, res) => {
  const userId = req.headers["x-user-id"] || null;

  try {
    const containers = await docker.listContainers({
      all: true,
      filters: { name: ["peer-"] },
    });

    const indices = containers
      .map((c) => parseInt(c.Names[0].replace(/^\/peer-/, ""), 10))
      .filter((n) => !Number.isNaN(n));

    const lastIndex = indices.length ? Math.max(...indices) : 8000;

    const nextIdx = lastIndex + 1;
    const offset = nextIdx - 8000;
    const containerName = `peer-${nextIdx}`;
    const peerIp = `172.23.0.${10 + offset}`;
    const rpcPort = 9001 + offset;
    const syncPort = 10000 + offset;
    const httpPort = nextIdx;
    const bootstrapIp = "172.23.0.100";

    const opts = {
      Image: "observability-stack-peer-base",
      name: containerName,
      Env: [`VAULT_SECRET_PASS=${config.VAULT_SECRET_PASS || ""}`],
      HostConfig: {
        NetworkMode: "observability-stack_monitoring",
        CapDrop: ["ALL"],
      },
      NetworkingConfig: {
        EndpointsConfig: {
          "observability-stack_monitoring": {
            IPAMConfig: { IPv4Address: peerIp },
          },
        },
      },
      Cmd: [
        "sh",
        "-c",
        `sleep 2 && java -cp /app/p2p-node.jar org.graph.server.Launcher ${peerIp} ${rpcPort} ${bootstrapIp} ${syncPort} ${httpPort}`,
      ],
    };

    const container = await docker.createContainer(opts);
    await container.start();

    res.status(202).json({
      status: "DEPLOYING",
      message: `Replica ${containerName} created – Kademlia will start the JoinNetwork`,
      metadata: {
        container: containerName,
        ip: peerIp,
        rpcPort,
        syncPort,
        httpPort,
      },
    });
  } catch (err) {
    console.error("[INFRA SCALE] ", err);
    res.status(500).json({
      status: "ERROR",
      message: "Failed to scale the infrastructure",
      error: err.message,
    });
  }
});

module.exports = router;
