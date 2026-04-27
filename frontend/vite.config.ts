import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

/**
 * Vite 配置。
 * 将 /api 请求代理到后端，方便本地联调。
 */
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      "/api": "http://localhost:8081"
    }
  },
  test: {
    environment: "jsdom",
    setupFiles: "./src/test/setupTests.ts"
  }
});

