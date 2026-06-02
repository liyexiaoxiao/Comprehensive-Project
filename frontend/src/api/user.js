import http, { CURRENT_USER_STORAGE_KEY } from './http'

const gatewayBaseUrl = (import.meta.env.VITE_GATEWAY_BASE_URL || 'http://localhost:8080').replace(/\/$/, '')

export const getCurrentUserApi = () => http.get('/api/users/me')
export const uploadMyAvatarApi = (formData) =>
  http.post('/api/users/me/avatar', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
export const getAdminUsersApi = (params) => http.get('/api/users/get-users', { params })
export const getAdminUserByIdApi = (userId) => http.get(`/api/users/${userId}`)
export const updateAdminUserApi = (userId, payload) => http.patch(`/api/users/${userId}`, payload)
export const resetAdminUserPasswordApi = (userId, payload) => http.post(`/api/users/${userId}/reset-password`, payload)
export const deleteAdminUserApi = (userId) => http.delete(`/api/users/${userId}`)
export const getAdminFeedbackApi = (params) => http.get('/api/feedback/all', { params })
export const getAdminFeedbackByIdApi = (feedbackId) => http.get(`/api/feedback/${feedbackId}`)
export const getAdminUserFeedbackPreferencesApi = (userId) => http.get(`/api/feedback/preferences/${userId}`)

export const getUserSummariesApi = (userIds = []) => {
  const ids = userIds.filter((id) => id !== null && id !== undefined)
  return http.get('/api/users/summaries', { params: { ids: ids.join(',') } })
}

export const updateMyProfileApi = (payload) => http.patch('/api/users/update-info', payload)
export const submitUserFeedbackApi = (payload) => http.post('/api/feedback/mine', payload)
export const getMyFeedbackApi = (params) => http.get('/api/feedback/mine', { params })
export const getMyFeedbackPreferencesApi = () => http.get('/api/feedback/preferences')

export const saveCurrentUserToStorage = (user) => {
  window.localStorage.setItem(CURRENT_USER_STORAGE_KEY, JSON.stringify(user))
}

export const clearCurrentUserFromStorage = () => {
  window.localStorage.removeItem(CURRENT_USER_STORAGE_KEY)
}

export const getCurrentUserFromStorage = () => {
  try {
    const raw = window.localStorage.getItem(CURRENT_USER_STORAGE_KEY)
    return raw ? JSON.parse(raw) : null
  } catch {
    return null
  }
}

export const isAdminUser = (user) => {
  const role = String(user?.role || '').trim().toUpperCase()
  return role === 'ADMIN' || role === 'ROLE_ADMIN'
}

export const resolveUserAvatarUrl = (avatarUrl) => {
  const value = typeof avatarUrl === 'string' ? avatarUrl.trim() : ''
  if (!value) return ''
  if (!value.startsWith('/api/users/avatars/')) {
    return ''
  }
  return `${gatewayBaseUrl}${value}`
}
