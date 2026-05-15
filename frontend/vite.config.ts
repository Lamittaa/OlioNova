import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

export default defineConfig({
  plugins: [react()],
  server: {
    host: "0.0.0.0",
    port: 5173,
    proxy: {
      "/api/production": {
        target: "http://localhost:9030",
        changeOrigin: true
      },
      "/api/public/tracking": {
        target: "http://localhost:9050",
        changeOrigin: true
      },
      "/api/tracking": {
        target: "http://localhost:9050",
        changeOrigin: true
      },
      "/api/public/queues": {
        target: "http://localhost:9040",
        changeOrigin: true
      },
      "/api/queues": {
        target: "http://localhost:9040",
        changeOrigin: true
      },
      "/api/payments": {
        target: "http://localhost:9020",
        changeOrigin: true
      },
      "/api/v1": {
        target: "http://localhost:9082",
        changeOrigin: true
      },
      "/api": {
        target: "http://localhost:8080",
        changeOrigin: true
      },
      "/actuator": {
        target: "http://localhost:8080",
        changeOrigin: true
      },
      "/ws": {
        target: "ws://localhost:8080",
        ws: true,
        changeOrigin: true
      }
    }
  },
  preview: {
    host: "0.0.0.0",
    port: 4173
  }
});
