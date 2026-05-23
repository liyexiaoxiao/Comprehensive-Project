import axios from 'axios'
import { AUTH_TOKEN_STORAGE_KEY, CURRENT_USER_STORAGE_KEY } from './http'

const aiHttp = axios.create({
  baseURL: import.meta.env.VITE_AI_SERVICE_BASE_URL || 'http://localhost:5001',
  timeout: 60000,
})

aiHttp.interceptors.request.use((config) => {
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

export default aiHttp
