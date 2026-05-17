import http, { CURRENT_USER_STORAGE_KEY } from './http'

const gatewayBaseUrl = (import.meta.env.VITE_GATEWAY_BASE_URL || 'http://localhost:8080').replace(/\/$/, '')

export const getCurrentUserApi = () => http.get('/api/users/me')
export const uploadMyAvatarApi = (formData) =>
  http.post('/api/users/me/avatar', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })

export const getUserSummariesApi = (userIds = []) => {
  const ids = userIds.filter((id) => id !== null && id !== undefined)
  return http.get('/api/users/summaries', { params: { ids: ids.join(',') } })
}

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

export const resolveUserAvatarUrl = (avatarUrl) => {
  const value = typeof avatarUrl === 'string' ? avatarUrl.trim() : ''
  if (!value) return ''
  if (/^(https?:|data:|blob:)/i.test(value)) {
    return value
  }
  return `${gatewayBaseUrl}${value.startsWith('/') ? value : `/${value}`}`
}
