import axios from 'axios'

export const AUTH_TOKEN_STORAGE_KEY = 'auth_token'
export const CURRENT_USER_STORAGE_KEY = 'current_user'

export const getStoredAuthToken = () => window.localStorage.getItem(AUTH_TOKEN_STORAGE_KEY)

export const saveAuthToken = (token) => {
  window.localStorage.setItem(AUTH_TOKEN_STORAGE_KEY, token)
}

export const clearAuthToken = () => {
  window.localStorage.removeItem(AUTH_TOKEN_STORAGE_KEY)
}

const http = axios.create({
  baseURL: import.meta.env.VITE_GATEWAY_BASE_URL || 'http://localhost:8080',
  timeout: 30000,
})

http.interceptors.request.use((config) => {
  const token = getStoredAuthToken()
  config.__authToken = token || null
  if (token) {
    config.headers.Authorization = token
  }
  return config
})

http.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error?.response?.status === 401) {
      const shouldSuppressAuthRedirect = Boolean(error?.config?.suppressAuthRedirect)
      const requestToken = error?.config?.__authToken || null
      const currentToken = getStoredAuthToken()
      const shouldClearActiveSession = !shouldSuppressAuthRedirect && requestToken && requestToken === currentToken

      if (shouldClearActiveSession) {
        clearAuthToken()
        window.localStorage.removeItem(CURRENT_USER_STORAGE_KEY)

        if (window.location.pathname !== '/login') {
          const redirect = `${window.location.pathname}${window.location.search}${window.location.hash}`
          const loginUrl = redirect
            ? `/login?redirect=${encodeURIComponent(redirect)}`
            : '/login'
          window.location.replace(loginUrl)
        }
      }
    }

    return Promise.reject(error)
  },
)

export default http
