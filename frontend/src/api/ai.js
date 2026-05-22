import aiHttp from './aiHttp'

export const getMeditationGuideApi = (payload) => aiHttp.post('/api/meditation/guide', payload)
export const getCompanionTtsApi = (payload) => aiHttp.post('/api/companion/tts', payload)
export const analyzeCompanionAudioApi = (formData, config = {}) =>
  aiHttp.post('/api/companion/audio/analyze', formData, config)
export const askCompanionApi = (payload, config = {}) =>
  aiHttp.post('/api/companion/chat', payload, config)
export const getAiAssetUrl = (path = '') => {
  const normalizedPath = String(path || '')
  if (!normalizedPath) return ''
  if (/^https?:\/\//i.test(normalizedPath)) return normalizedPath
  const baseUrl = (import.meta.env.VITE_AI_SERVICE_BASE_URL || 'http://localhost:5001').replace(/\/$/, '')
  const resolvedPath = normalizedPath.startsWith('/') ? normalizedPath : `/${normalizedPath}`
  return `${baseUrl}${resolvedPath}`
}
