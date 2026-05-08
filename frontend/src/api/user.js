import http, { CURRENT_USER_STORAGE_KEY } from './http'

export const getCurrentUserApi = () => http.get('/api/users/me')

export const updateMyProfileApi = (payload) => http.patch('/api/users/update-info', payload)

export const saveCurrentUserToStorage = (user) => {
  window.localStorage.setItem(CURRENT_USER_STORAGE_KEY, JSON.stringify(user))
}

export const getCurrentUserFromStorage = () => {
  try {
    const raw = window.localStorage.getItem(CURRENT_USER_STORAGE_KEY)
    return raw ? JSON.parse(raw) : null
  } catch {
    return null
  }
}
