import axios from 'axios'

const pythonHttp = axios.create({
  baseURL: import.meta.env.VITE_PYTHON_BASE_URL || 'http://localhost:5000',
  timeout: 10000,
})

export default pythonHttp
