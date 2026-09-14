import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'
import { defineConfig } from 'vitest/config'

export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    port: 5173,
    proxy: {
      '/api': 'http://backend:8080',
      '/actuator': 'http://backend:8080',
    },
  },
  test: {
    environment: 'jsdom',
    globals: true,
  },
})
