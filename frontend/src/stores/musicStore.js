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
import { ElMessage } from 'element-plus'

const createTrack = (track) => ({
  ...track,
  tags: Array.isArray(track.tags) ? [...track.tags] : []
})

const initialUploadedTracks = [
  {
    id: 'upload-1',
    title: '凌晨四点的海风',
    artist: '林雾',
    duration: 218,
    tags: ['平静', '夜晚'],
    cover: '/images/feature-img-1.jpg'
  },
  {
    id: 'upload-2',
    title: '给自己留一盏灯',
    artist: '沈青',
    duration: 204,
    tags: ['治愈', '独处'],
    cover: '/images/feature-img-2.jpg'
  },
  {
    id: 'upload-3',
    title: '窗台上的云',
    artist: '',
    duration: 231,
    tags: ['轻盈', '午后'],
    cover: '/images/feature-img-3.jpg'
  },
  {
    id: 'upload-4',
    title: '雨后慢行',
    artist: '陈眠',
    duration: 247,
    tags: ['放松', '雨天'],
    cover: '/images/feature-img-4.jpg'
  }
]

export const useMusicStore = defineStore('music', () => {
  const uploadedTracks = ref(initialUploadedTracks.map(createTrack)) 
  const isUploadedTrack = (trackId) => uploadedTracks.value.some(track => track.id === trackId)
  
  const likedTrackIds = ref([])
  const collectedTrackIds = ref([])
  const customPlaylists = ref([])

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

  const addUploadedTrack = (track) => {
    uploadedTracks.value.push(track)
  }

  const removeUploadedTrack = (trackId) => {
    uploadedTracks.value = uploadedTracks.value.filter(t => t.id !== trackId)
    likedTrackIds.value = likedTrackIds.value.filter(id => id !== trackId)
    collectedTrackIds.value = collectedTrackIds.value.filter(id => id !== trackId)
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
    fetchUserData,
    toggleLike,
    toggleCollect,
    addUploadedTrack,
    removeUploadedTrack,
    createPlaylist,
    deletePlaylist,
    addTrackToPlaylist,
    removeTrackFromPlaylist
  }
})
