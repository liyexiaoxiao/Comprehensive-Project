import http from './http'

// --- Data Logging ---
export const appendAiChatLogApi = (payload) => http.post('/api/data/v1/logs/ai-chat', payload)
export const appendEmotionSnapshotApi = (payload) => http.post('/api/data/v1/logs/emotion-snapshot', payload)
export const appendUserBehaviorLogApi = (payload) => http.post('/api/data/v1/logs/user-behavior', payload)

// --- Data Queries ---
export const getMyAiChatLogsApi = (params) => http.get('/api/data/v1/me/ai-chat-logs', { params })
export const getMyEmotionSnapshotsApi = (params) => http.get('/api/data/v1/me/emotion-snapshots', { params })
export const getMyUserBehaviorLogsApi = (params) => http.get('/api/data/v1/me/user-behavior-logs', { params })
