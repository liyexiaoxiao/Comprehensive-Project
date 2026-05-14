import pythonHttp from './pythonHttp'

export const analyzeEmotionApi = (payload) => pythonHttp.post('/api/analyze', payload)

export const getMusicListApi = () => pythonHttp.get('/api/music/list')
export const getUploadedMusicApi = () => pythonHttp.get('/api/music/uploads')
export const uploadMusicFileApi = (formData) =>
  pythonHttp.post('/api/music/uploads', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
export const deleteUploadedMusicApi = (trackId) => pythonHttp.delete(`/api/music/uploads/${encodeURIComponent(trackId)}`)

export const getMusicFileUrl = (filename) => {
  const baseUrl = (import.meta.env.VITE_PYTHON_BASE_URL || 'http://localhost:5000').replace(/\/$/, '')
  return `${baseUrl}/api/music/file/${encodeURIComponent(filename)}`
}

export const getEmotionMusicApi = (emotion) =>
  pythonHttp.get(`/api/music/${emotion}`, { responseType: 'blob' })

export const getMusicFileByNameApi = (filename) =>
  pythonHttp.get(`/api/music/file/${encodeURIComponent(filename)}`, { responseType: 'blob' })

export const getPreviousEmotionMusicApi = (emotion) =>
  pythonHttp.get(`/api/music/${emotion}/prev`, { responseType: 'blob' })

export const getNextEmotionMusicApi = (emotion) =>
  pythonHttp.get(`/api/music/${emotion}/next`, { responseType: 'blob' })
