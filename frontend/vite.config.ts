import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    host: '127.0.0.1',
    port: 5173,
    strictPort: true,
    proxy: {
      '/reservations': {
        target: 'http://localhost:8080',
        // The customer route shares a name with the API. Full-page navigation
        // serves the SPA; fetch requests explicitly accept JSON.
        bypass(req) { if (req.method === 'GET' && req.headers.accept?.includes('text/html')) return '/index.html' },
      },
      '/screenings': 'http://localhost:8080',
    },
  },
})
