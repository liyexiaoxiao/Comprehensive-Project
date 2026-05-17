import http from './http'

export const getMusicResourcesApi = () => http.get('/api/music/v1/music-resources')
export const createMusicResourceApi = (payload) => http.post('/api/music/v1/music-resources', payload)

export const getMyMusicPreferencesApi = () => http.get('/api/music/v1/me/music-preferences')
export const upsertMusicPreferenceApi = (payload) => http.post('/api/music/v1/me/music-preferences', payload)
export const deleteMusicPreferenceApi = (musicId, preferenceType) => http.delete(`/api/music/v1/me/music-preferences/${musicId}/${preferenceType}`)

export const getMyPlaylistsApi = () => http.get('/api/music/v1/playlists')
export const createPlaylistApi = (payload) => http.post('/api/music/v1/playlists', payload)
export const deletePlaylistApi = (id) => http.delete(`/api/music/v1/playlists/${id}`)

export const getPlaylistTracksApi = (id) => http.get(`/api/music/v1/playlists/${id}/tracks`)
export const addTrackToPlaylistApi = (id, payload) => http.post(`/api/music/v1/playlists/${id}/tracks`, payload)
export const removeTrackFromPlaylistApi = (id, musicId) => http.delete(`/api/music/v1/playlists/${id}/tracks/${musicId}`)
