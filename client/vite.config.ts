import { reactRouter } from "@react-router/dev/vite";
import tailwindcss from "@tailwindcss/vite";
import { defineConfig, loadEnv } from "vite";
import tsconfigPaths from "vite-tsconfig-paths";

export default defineConfig(({ mode }) => {
    const env = loadEnv(mode, process.cwd(), '');
    const serverPort = parseInt(env.VITE_PORT || '3001', 10);

    return {
        plugins: [tailwindcss(), reactRouter(), tsconfigPaths()],
        server: {
            port: serverPort,
            strictPort: true,
            host: true
        }
    };
});