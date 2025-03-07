import {defineConfig} from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
    server: {
        host: true,
        port: 5173,
        proxy: {
            '/api': 'http://backend:8080'
            //'/api': 'http://localhost:8080'
        }
    },
    plugins: [react()],
    define: {
        global: {},
    },
})