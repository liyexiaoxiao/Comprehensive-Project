import axios from 'axios'

export const AUTH_TOKEN_STORAGE_KEY = 'auth_token'
export const CURRENT_USER_STORAGE_KEY = 'current_user'

const http = axios.create({
  baseURL: import.meta.env.VITE_GATEWAY_BASE_URL || 'http://localhost:8080',
  timeout: 10000,
})

http.interceptors.request.use((config) => {
  const token = window.localStorage.getItem(AUTH_TOKEN_STORAGE_KEY)
  if (token) {
    config.headers.Authorization = token
  }
  return config
})

export default http
