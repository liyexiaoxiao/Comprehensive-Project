import axios from 'axios'
import { AUTH_TOKEN_STORAGE_KEY, CURRENT_USER_STORAGE_KEY } from './http'

const pythonHttp = axios.create({
  baseURL: import.meta.env.VITE_PYTHON_BASE_URL || 'http://localhost:5000',
  timeout: 60000,
})

pythonHttp.interceptors.request.use((config) => {
  const token = window.localStorage.getItem(AUTH_TOKEN_STORAGE_KEY)
  if (token) {
    config.headers.Authorization = token
  }

  try {
    const rawUser = window.localStorage.getItem(CURRENT_USER_STORAGE_KEY)
    const currentUser = rawUser ? JSON.parse(rawUser) : null
    if (currentUser?.userId) {
      config.headers['X-User-Id'] = currentUser.userId
    }
  } catch {
    // ignore malformed local storage values
  }

  return config
})

export default pythonHttp
