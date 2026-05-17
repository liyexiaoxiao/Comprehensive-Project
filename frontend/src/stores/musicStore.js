import { defineStore } from 'pinia'
import { ref } from 'vue'
import { 
  getMyMusicPreferencesApi, 
  upsertMusicPreferenceApi, 
  deleteMusicPreferenceApi,
  getMyPlaylistsApi,
  createPlaylistApi,
  deletePlaylistApi,
  getPlaylistTracksApi,
  addTrackToPlaylistApi,
  removeTrackFromPlaylistApi
} from '@/api/music'
import {
  deleteUploadedMusicApi,
  getUploadedMusicApi,
  uploadMusicFileApi,
} from '@/api/python'
import { getRealMusicCover } from '@/utils/realMusic'
import { ElMessage } from 'element-plus'

const createTrack = (track) => ({
  ...track,
  artist: track.artist || '',
  duration: Number(track.duration) || 0,
  tags: Array.isArray(track.tags) ? [...track.tags] : [],
  cover: track.cover || getRealMusicCover((Array.isArray(track.tags) && track.tags[0]) || 'neutral')
})

export const useMusicStore = defineStore('music', () => {
  const uploadedTracks = ref([])
  const isUploadedTrack = (trackId) => uploadedTracks.value.some(track => track.id === trackId)
  
  const likedTrackIds = ref([])
  const collectedTrackIds = ref([])
  const customPlaylists = ref([])

  const fetchUploadedTracks = async () => {
    try {
      const response = await getUploadedMusicApi()
      uploadedTracks.value = (response.data?.tracks || []).map(createTrack)
    } catch (error) {
      uploadedTracks.value = []
      console.error('Failed to fetch uploaded tracks:', error)
    }
  }

  // 从后端获取用户的偏好和歌单
  const fetchUserData = async () => {
    try {
      const [prefRes, playlistsRes] = await Promise.all([
        getMyMusicPreferencesApi(),
        getMyPlaylistsApi()
      ])

      const preferences = prefRes.data || []
      likedTrackIds.value = preferences.filter(p => p.preferenceType === 1).map(p => p.musicId)
      collectedTrackIds.value = preferences.filter(p => p.preferenceType === 2).map(p => p.musicId)

      const playlists = playlistsRes.data || []
      
      // Fetch tracks for each playlist
      const playlistsWithTracks = await Promise.all(playlists.map(async (pl) => {
        try {
          const tracksRes = await getPlaylistTracksApi(pl.id)
          return {
            id: pl.id,
            name: pl.name,
            description: pl.description || '自建歌单',
            cover: pl.coverUrl || '/images/feature-img-2.jpg',
            // Backend returns string IDs, we store them directly, but frontend needs full track objects
            // In PersonalSpace, we will map them
            trackIds: tracksRes.data || [],
            tracks: [] // This will be populated dynamically if needed, or we just store IDs
          }
        } catch (e) {
          return { ...pl, trackIds: [], tracks: [] }
        }
      }))
      
      customPlaylists.value = playlistsWithTracks
    } catch (error) {
      console.error('Failed to fetch user music data:', error)
    }

    await fetchUploadedTracks()
  }

  const toggleLike = async (trackId) => {
    if (isUploadedTrack(trackId)) return
    const idx = likedTrackIds.value.indexOf(trackId)
    try {
      if (idx > -1) {
        await deleteMusicPreferenceApi(trackId, 1)
        likedTrackIds.value.splice(idx, 1)
      } else {
        await upsertMusicPreferenceApi({ musicId: trackId, preferenceType: 1 })
        likedTrackIds.value.push(trackId)
      }
    } catch (e) {
      ElMessage.error('操作失败')
    }
  }

  const toggleCollect = async (trackId) => {
    if (isUploadedTrack(trackId)) return
    const idx = collectedTrackIds.value.indexOf(trackId)
    try {
      if (idx > -1) {
        await deleteMusicPreferenceApi(trackId, 2)
        collectedTrackIds.value.splice(idx, 1)
      } else {
        await upsertMusicPreferenceApi({ musicId: trackId, preferenceType: 2 })
        collectedTrackIds.value.push(trackId)
      }
    } catch (e) {
      ElMessage.error('操作失败')
    }
  }

  const uploadTrack = async (payload) => {
    const formData = new FormData()
    formData.append('file', payload.file)
    formData.append('title', payload.title?.trim() || '')
    formData.append('artist', payload.artist?.trim() || '')
    formData.append('duration', String(payload.duration || 0))
    formData.append('tags', JSON.stringify(Array.isArray(payload.tags) ? payload.tags : []))

    const response = await uploadMusicFileApi(formData)
    const nextTrack = createTrack(response.data || {})
    uploadedTracks.value = [nextTrack, ...uploadedTracks.value.filter(track => track.id !== nextTrack.id)]
    return nextTrack
  }

  const removeUploadedTrack = async (trackId) => {
    await deleteUploadedMusicApi(trackId)
    uploadedTracks.value = uploadedTracks.value.filter(t => t.id !== trackId)
    likedTrackIds.value = likedTrackIds.value.filter(id => id !== trackId)
    collectedTrackIds.value = collectedTrackIds.value.filter(id => id !== trackId)
    customPlaylists.value = customPlaylists.value.map((playlist) => ({
      ...playlist,
      trackIds: playlist.trackIds.filter(id => id !== trackId),
      tracks: playlist.tracks.filter(track => track.id !== trackId),
    }))
    ElMessage.success('已删除上传音乐')
  }

  const createPlaylist = async (name, description = '自建歌单') => {
    try {
      const res = await createPlaylistApi({ name, description, coverUrl: '/images/feature-img-2.jpg' })
      customPlaylists.value.push({
        id: res.data.id,
        name: res.data.name,
        description: res.data.description || '自建歌单',
        cover: res.data.coverUrl || '/images/feature-img-2.jpg',
        trackIds: [],
        tracks: []
      })
      ElMessage.success('歌单创建成功')
    } catch (e) {
      ElMessage.error('歌单创建失败')
    }
  }

  const deletePlaylist = async (playlistId) => {
    try {
      await deletePlaylistApi(playlistId)
      customPlaylists.value = customPlaylists.value.filter(p => p.id !== playlistId)
      ElMessage.success('删除成功')
    } catch (e) {
      ElMessage.error('删除失败')
    }
  }

  const addTrackToPlaylist = async (playlistId, track) => {
    const playlist = customPlaylists.value.find(p => p.id === playlistId)
    if (playlist && !playlist.trackIds.includes(track.id)) {
      try {
        await addTrackToPlaylistApi(playlistId, { musicId: track.id })
        playlist.trackIds.push(track.id)
        playlist.tracks.push(track) // Keep local tracks array synced for UI
        ElMessage.success('已添加到歌单')
      } catch (e) {
        ElMessage.error('添加失败')
      }
    }
  }
  
  const removeTrackFromPlaylist = async (playlistId, trackId) => {
    const playlist = customPlaylists.value.find(p => p.id === playlistId)
    if (playlist) {
      try {
        await removeTrackFromPlaylistApi(playlistId, trackId)
        playlist.trackIds = playlist.trackIds.filter(id => id !== trackId)
        playlist.tracks = playlist.tracks.filter(t => t.id !== trackId)
        ElMessage.success('已移出歌单')
      } catch (e) {
        ElMessage.error('移除失败')
      }
    }
  }

  return {
    uploadedTracks,
    likedTrackIds,
    collectedTrackIds,
    customPlaylists,
    fetchUploadedTracks,
    fetchUserData,
    toggleLike,
    toggleCollect,
    uploadTrack,
    removeUploadedTrack,
    createPlaylist,
    deletePlaylist,
    addTrackToPlaylist,
    removeTrackFromPlaylist
  }
})
