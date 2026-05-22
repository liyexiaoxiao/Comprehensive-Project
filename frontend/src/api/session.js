import { getMeApi, logoutApi } from './auth'
import { clearAuthToken, getStoredAuthToken } from './http'
import {
  clearCurrentUserFromStorage,
  getCurrentUserFromStorage,
  isAdminUser,
  saveCurrentUserToStorage,
} from './user'

let validatedToken = null

export const clearSession = () => {
  validatedToken = null
  clearAuthToken()
  clearCurrentUserFromStorage()
}

export const resolveAuthenticatedRoute = (user) => (isAdminUser(user) ? '/admin' : '/service')

export const restoreSession = async ({ force = false } = {}) => {
  const token = getStoredAuthToken()
  if (!token) {
    clearSession()
    return null
  }

  if (!force && validatedToken === token) {
    const cachedUser = getCurrentUserFromStorage()
    if (cachedUser) {
      return cachedUser
    }
  }

  try {
    const response = await getMeApi()
    const currentUser = response.data
    if (getStoredAuthToken() !== token) {
      return getCurrentUserFromStorage()
    }
    saveCurrentUserToStorage(currentUser)
    validatedToken = token
    return currentUser
  } catch (error) {
    if (getStoredAuthToken() === token) {
      clearSession()
    }
    throw error
  }
}

export const logoutSession = async () => {
  try {
    if (getStoredAuthToken()) {
      await logoutApi()
    }
  } finally {
    clearSession()
  }
}
