import http from './http'

export const getMyMeditationLogsApi = () => http.get('/api/meditation/my-meditation-logs')
export const saveMeditationLogApi = (payload) => http.post('/api/meditation/my-meditation-logs', payload)
export const deleteMeditationLogApi = (logId) => http.post('/api/meditation/my-meditation-logs/delete', { logId })
export const getAdminMeditationLogsApi = (userId) => http.get(`/api/meditation/admin/meditation-logs/${userId}`)
export const saveAdminMeditationLogApi = (userId, payload) => http.post(`/api/meditation/admin/meditation-logs/${userId}`, payload)
export const deleteAdminMeditationLogApi = (logId) => http.post('/api/meditation/admin/meditation-logs/delete', { logId })

// --- Garden Gamification ---
export const getMyGardenApi = () => http.get('/api/meditation/garden/me')
export const rewardGardenItemApi = () => http.post('/api/meditation/garden/me/reward')
export const unlockGardenPlantApi = (plantId) => http.post(`/api/meditation/garden/me/unlock-plant/${plantId}`)
