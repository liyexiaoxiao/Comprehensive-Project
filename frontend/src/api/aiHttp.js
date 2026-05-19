import axios from 'axios'

const aiHttp = axios.create({
  baseURL: import.meta.env.VITE_AI_SERVICE_BASE_URL || 'http://localhost:5001',
  timeout: 60000,
})

export default aiHttp
