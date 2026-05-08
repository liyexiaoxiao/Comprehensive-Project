import { defineStore } from 'pinia'
import { ref } from 'vue'

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
  // 存储用户上传的音乐
  const uploadedTracks = ref(initialUploadedTracks.map(createTrack)) 
  const isUploadedTrack = (trackId) => uploadedTracks.value.some(track => track.id === trackId)
  
  // 喜欢和收藏的歌曲ID列表
  const likedTrackIds = ref([])
  const collectedTrackIds = ref([])

  // 自建歌单
  const customPlaylists = ref([])

  const toggleLike = (trackId) => {
    if (isUploadedTrack(trackId)) return
    const idx = likedTrackIds.value.indexOf(trackId)
    if (idx > -1) likedTrackIds.value.splice(idx, 1)
    else likedTrackIds.value.push(trackId)
  }

  const toggleCollect = (trackId) => {
    if (isUploadedTrack(trackId)) return
    const idx = collectedTrackIds.value.indexOf(trackId)
    if (idx > -1) collectedTrackIds.value.splice(idx, 1)
    else collectedTrackIds.value.push(trackId)
  }

  const addUploadedTrack = (track) => {
    uploadedTracks.value.push(track)
  }

  const removeUploadedTrack = (trackId) => {
    uploadedTracks.value = uploadedTracks.value.filter(t => t.id !== trackId)
    likedTrackIds.value = likedTrackIds.value.filter(id => id !== trackId)
    collectedTrackIds.value = collectedTrackIds.value.filter(id => id !== trackId)
    customPlaylists.value.forEach(p => {
      p.tracks = p.tracks.filter(t => t.id !== trackId)
    })
  }

  const createPlaylist = (name, description = '自建歌单') => {
    customPlaylists.value.push({
      id: 'playlist_' + Date.now(),
      name,
      description: description || '自建歌单',
      cover: '/images/feature-img-2.jpg', // Default cover
      tracks: []
    })
  }

  const deletePlaylist = (playlistId) => {
    customPlaylists.value = customPlaylists.value.filter(p => p.id !== playlistId)
  }

  const addTrackToPlaylist = (playlistId, track) => {
    const playlist = customPlaylists.value.find(p => p.id === playlistId)
    if (playlist && !playlist.tracks.some(t => t.id === track.id)) {
      playlist.tracks.push(track)
    }
  }
  
  const removeTrackFromPlaylist = (playlistId, trackId) => {
    const playlist = customPlaylists.value.find(p => p.id === playlistId)
    if (playlist) {
      playlist.tracks = playlist.tracks.filter(t => t.id !== trackId)
    }
  }

  return {
    uploadedTracks,
    likedTrackIds,
    collectedTrackIds,
    customPlaylists,
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
