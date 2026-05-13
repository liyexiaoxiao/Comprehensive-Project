import http from './http'

export const getMyMeditationLogsApi = () => http.get('/api/meditation/api/my-meditation-logs')
export const saveMeditationLogApi = (payload) => http.post('/api/meditation/api/my-meditation-logs', payload)
export const deleteMeditationLogApi = (logId) => http.post('/api/meditation/api/my-meditation-logs/delete', { logId })
