import http from './http'

export const getMyMeditationLogsApi = () => http.get('/api/meditation/my-meditation-logs')
export const saveMeditationLogApi = (payload) => http.post('/api/meditation/my-meditation-logs', payload)
export const deleteMeditationLogApi = (logId) => http.post('/api/meditation/my-meditation-logs/delete', { logId })
export const startMeditationCountdownApi = (payload) => http.post('/api/meditation/start-countdown', payload)
export const stopMeditationCountdownApi = () => http.post('/api/meditation/stop-countdown')
export const completeMeditationCountdownApi = () => http.post('/api/meditation/complete-countdown')
export const getMeditationCountdownLeftApi = () => http.get('/api/meditation/countdown-left')
export const getAdminMeditationLogsApi = (userId) => http.get(`/api/meditation/admin/meditation-logs/${userId}`)
export const saveAdminMeditationLogApi = (userId, payload) => http.post(`/api/meditation/admin/meditation-logs/${userId}`, payload)
export const deleteAdminMeditationLogApi = (logId) => http.post('/api/meditation/admin/meditation-logs/delete', { logId })

// --- Garden Gamification ---
export const getMyGardenApi = () => http.get('/api/meditation/garden/me')
export const rewardGardenItemApi = () => http.post('/api/meditation/garden/me/reward')
export const unlockGardenPlantApi = (plantId) => http.post(`/api/meditation/garden/me/unlock-plant/${plantId}`)

// --- Mini Missions (Behavior Activation) ---
export const getMiniMissionByIdApi = (missionId) => http.get(`/api/meditation/mini-missions/${missionId}`)
export const getMiniMissionCatalogApi = () => http.get('/api/meditation/mini-missions/catalog')
export const startMiniMissionApi = (missionId) => http.post('/api/meditation/mini-missions/start', { missionId })
export const abortMiniMissionApi = (missionId) => http.post('/api/meditation/mini-missions/abort', { missionId })
export const completeMiniMissionApi = (missionId) => http.post('/api/meditation/mini-missions/complete', { missionId })
export const getMyMiniMissionLogsApi = () => http.get('/api/meditation/mini-missions/my-logs')
