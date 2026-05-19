import { defineStore } from 'pinia'
import { ref } from 'vue'
import {
  createMusicResourceApi,
  deleteMusicResourceApi,
  getEmotionTagsApi,
  getMyMusicPreferencesApi,
  getMyMusicResourcesApi,
  getMusicResourceTagsApi,
  getMusicResourcesApi,
  deleteMusicPreferenceApi,
  createPlaylistApi,
  addTrackToPlaylistApi,
  deletePlaylistApi,
  getMyPlaylistsApi,
  recommendMusicByEmotionApi,
  recommendNextMusicApi,
  removeTrackFromPlaylistApi,
  upsertMusicPreferenceApi,
} from '@/api/music'
import {
  deleteUploadedMusicApi,
  getMusicListApi,
  getUploadedMusicApi,
  getMusicFileUrl,
  uploadMusicFileApi,
} from '@/api/python'
import {
  getRealMusicCover,
  normalizeEmotion,
  normalizeMusicResourceTrack as normalizePublicMusicTrack,
  normalizeRealMusicTrack,
  readRemoteAudioDuration,
} from '@/utils/realMusic'
import { ElMessage } from 'element-plus'

const DEFAULT_PLAYLIST_COVER = '/images/feature-img-2.jpg'
const PYTHON_UPLOAD_SOURCE_PREFIX = 'python-upload:'
const PYTHON_PUBLIC_SOURCE_PREFIX = 'python-public:'
const ENABLE_PYTHON_PUBLIC_FALLBACK = import.meta.env.VITE_ENABLE_PYTHON_PUBLIC_FALLBACK === 'true'

const normalizeId = (value) => String(value ?? '')

const extractFilenameFromUrl = (fileUrl = '') => {
  if (!fileUrl) return ''
  try {
    const parsed = new URL(fileUrl)
    return decodeURIComponent(parsed.pathname.split('/').pop() || '')
  } catch {
    const sanitized = String(fileUrl).split('?')[0]
    return decodeURIComponent(sanitized.split('/').pop() || '')
  }
}

const parsePythonTrackId = (source = '') =>
  String(source || '').startsWith(PYTHON_UPLOAD_SOURCE_PREFIX)
    ? String(source).slice(PYTHON_UPLOAD_SOURCE_PREFIX.length)
    : ''

const createTrack = (track) => ({
  ...track,
  id: normalizeId(track.id),
  musicResourceId: track.musicResourceId == null ? null : Number(track.musicResourceId),
  artist: track.artist || '',
  duration: Number(track.duration) || 0,
  tags: Array.isArray(track.tags) ? [...track.tags] : [],
  cover: track.cover || track.coverUrl || getRealMusicCover((Array.isArray(track.tags) && track.tags[0]) || 'neutral'),
  coverUrl: track.coverUrl || '',
  fileUrl: track.fileUrl || '',
  filename: track.filename || '',
  pythonTrackId: track.pythonTrackId || '',
  source: track.source || '',
  type: track.type || '真实音乐',
})

const normalizeMusicResourceTrack = (resource, extra = {}) => {
  const tags = Array.isArray(extra.tags) ? extra.tags : []
  const filename = extra.filename || extractFilenameFromUrl(resource?.fileUrl)
  const pythonTrackId = extra.pythonTrackId || parsePythonTrackId(resource?.source)

  return createTrack({
    id: resource?.id ?? extra.id,
    musicResourceId: resource?.id ?? extra.musicResourceId ?? null,
    title: resource?.title || extra.title || '未命名音乐',
    artist: resource?.artist || extra.artist || '',
    duration: resource?.duration ?? extra.duration ?? 0,
    tags,
    coverUrl: resource?.coverUrl || extra.coverUrl || '',
    cover: extra.cover || resource?.coverUrl || getRealMusicCover(tags[0] || 'neutral'),
    fileUrl: resource?.fileUrl || extra.fileUrl || (filename ? getMusicFileUrl(filename) : ''),
    filename,
    pythonTrackId,
    source: resource?.source || extra.source || '',
    type: extra.type || '本地上传',
  })
}

const normalizePlaylist = (playlist) => {
  const tracks = Array.isArray(playlist?.tracks) ? playlist.tracks : []
  return {
    id: playlist?.playlistId ?? playlist?.id,
    name: playlist?.name || '未命名歌单',
    description: playlist?.description || '自建歌单',
    cover: playlist?.coverUrl || DEFAULT_PLAYLIST_COVER,
    trackIds: tracks.map((track) => normalizeId(track.musicId)),
    tracks: tracks
      .map((track) => (track.music ? normalizeMusicResourceTrack(track.music, { type: '歌单曲目' }) : null))
      .filter(Boolean),
  }
}

export const useMusicStore = defineStore('music', () => {
  const publicTracks = ref([])
  const uploadedTracks = ref([])
  const recommendedTracks = ref([])
  const emotionTags = ref([])
  const likedTrackIds = ref([])
  const collectedTrackIds = ref([])
  const customPlaylists = ref([])

  const findUploadedTrack = (trackId) => {
    const normalizedTrackId = normalizeId(trackId)
    return uploadedTracks.value.find(track => track.id === normalizedTrackId)
  }

  const buildUploadSource = (pythonTrackId) => `${PYTHON_UPLOAD_SOURCE_PREFIX}${pythonTrackId}`

  const resolveTrackById = (trackId) => {
    const normalizedTrackId = normalizeId(trackId)
    return uploadedTracks.value.find(track => track.id === normalizedTrackId)
      || publicTracks.value.find(track => track.id === normalizedTrackId)
      || recommendedTracks.value.find(track => track.id === normalizedTrackId)
      || null
  }

  const hydrateMusicResources = async (resources, options = {}) => {
    const rows = Array.isArray(resources) ? resources : []
    if (!rows.length) return []

    const tagPairs = await Promise.all(rows.map(async (resource) => {
      try {
        if (String(resource?.source || '').startsWith(PYTHON_PUBLIC_SOURCE_PREFIX) && resource?.artist) {
          return [resource.id, [resource.artist]]
        }

        const response = await getMusicResourceTagsApi(resource.id)
        const tags = Array.isArray(response.data)
          ? response.data.map(item => item?.tagName).filter(Boolean)
          : []
        return [resource.id, tags]
      } catch (error) {
        return [resource.id, []]
      }
    }))

    const tagMap = Object.fromEntries(tagPairs)
    return rows.map(resource => normalizePublicMusicTrack(resource, tagMap[resource.id] || [], options))
  }

  const fetchEmotionTags = async () => {
    try {
      const response = await getEmotionTagsApi()
      emotionTags.value = Array.isArray(response.data) ? response.data : []
    } catch (error) {
      emotionTags.value = []
      console.error('Failed to fetch emotion tags:', error)
    }
    return emotionTags.value
  }

  const resolveEmotionTagId = async (emotion) => {
    const normalizedEmotion = normalizeEmotion(emotion || 'neutral')
    if (!emotionTags.value.length) {
      await fetchEmotionTags()
    }

    const matchedTag = emotionTags.value.find((tag) => normalizeEmotion(tag?.tagName) === normalizedEmotion)
    return matchedTag?.id || null
  }

  const fetchPythonFallbackPublicTracks = async () => {
    if (!ENABLE_PYTHON_PUBLIC_FALLBACK) {
      return []
    }
    try {
      const response = await getMusicListApi()
      const filenames = Array.isArray(response?.data?.music_files) ? response.data.music_files : []
      const durationEntries = await Promise.all(
        filenames.map(async (filename) => [filename, await readRemoteAudioDuration(getMusicFileUrl(filename))]),
      )
      const durationMap = Object.fromEntries(durationEntries)
      return filenames.map((filename, index) => createTrack({
        ...normalizeRealMusicTrack(filename, index, durationMap),
        fileUrl: getMusicFileUrl(filename),
      }))
    } catch (error) {
      console.error('Failed to fetch Python fallback tracks:', error)
      return []
    }
  }

  const fetchPublicTracks = async () => {
    try {
      const response = await getMusicResourcesApi()
      const resources = Array.isArray(response.data)
        ? response.data.filter(resource => !String(resource?.source || '').startsWith(PYTHON_UPLOAD_SOURCE_PREFIX))
        : []
      if (resources.length) {
        publicTracks.value = await hydrateMusicResources(resources, { type: '真实音乐' })
      } else {
        publicTracks.value = await fetchPythonFallbackPublicTracks()
      }
    } catch (error) {
      publicTracks.value = await fetchPythonFallbackPublicTracks()
      console.error('Failed to fetch public tracks:', error)
    }
    return publicTracks.value
  }

  const fetchRecommendedTracks = async (emotion, limit = 20) => {
    try {
      const emotionTagId = await resolveEmotionTagId(emotion)
      if (!emotionTagId) {
        const normalizedEmotion = normalizeEmotion(emotion || 'neutral')
        recommendedTracks.value = publicTracks.value
          .filter(track => normalizeEmotion(track?.emotion || track?.tags?.[0]) === normalizedEmotion)
          .slice(0, limit)
        return recommendedTracks.value
      }

      const response = await recommendMusicByEmotionApi({ emotionTagId, limit })
      const resources = Array.isArray(response.data) ? response.data : []
      if (resources.length) {
        recommendedTracks.value = await hydrateMusicResources(resources, { type: '推荐音乐' })
      } else {
        const normalizedEmotion = normalizeEmotion(emotion || 'neutral')
        recommendedTracks.value = publicTracks.value
          .filter(track => normalizeEmotion(track?.emotion || track?.tags?.[0]) === normalizedEmotion)
          .slice(0, limit)
      }
    } catch (error) {
      const normalizedEmotion = normalizeEmotion(emotion || 'neutral')
      recommendedTracks.value = publicTracks.value
        .filter(track => normalizeEmotion(track?.emotion || track?.tags?.[0]) === normalizedEmotion)
        .slice(0, limit)
      console.error('Failed to fetch recommended tracks:', error)
    }
    return recommendedTracks.value
  }

  const fetchNextRecommendedTrack = async (currentMusicId, emotion, limit = 1) => {
    const numericMusicId = Number(currentMusicId)
    if (!Number.isFinite(numericMusicId) || numericMusicId <= 0) {
      return null
    }

    try {
      const emotionTagId = await resolveEmotionTagId(emotion)
      const response = await recommendNextMusicApi({
        currentMusicId: numericMusicId,
        emotionTagId,
        limit,
      })
      const resources = Array.isArray(response.data) ? response.data : []
      const tracks = await hydrateMusicResources(resources, { type: '推荐音乐' })
      if (tracks[0]) {
        return tracks[0]
      }
    } catch (error) {
      console.error('Failed to fetch next recommended track:', error)
    }

    const normalizedEmotion = normalizeEmotion(emotion || 'neutral')
    const candidates = publicTracks.value.filter((track) => {
      const trackEmotion = normalizeEmotion(track?.emotion || track?.tags?.[0])
      return trackEmotion === normalizedEmotion && normalizeId(track.id) !== normalizeId(currentMusicId)
    })
    return candidates[0] || null
  }

  const findMatchingUploadedResource = (resources, pythonTrack) => {
    const expectedSource = buildUploadSource(pythonTrack.id)
    const expectedFileUrl = pythonTrack.filename ? getMusicFileUrl(pythonTrack.filename) : ''

    return resources.find((resource) =>
      resource?.source === expectedSource ||
      (expectedFileUrl && resource?.fileUrl === expectedFileUrl) ||
      (pythonTrack.filename && extractFilenameFromUrl(resource?.fileUrl) === pythonTrack.filename),
    )
  }

  const fetchUploadedTracks = async () => {
    try {
      const [resourceResponse, pythonResponse] = await Promise.all([
        getMyMusicResourcesApi(),
        getUploadedMusicApi(),
      ])
      let uploadedResources = Array.isArray(resourceResponse.data)
        ? resourceResponse.data.filter(resource => String(resource?.source || '').startsWith(PYTHON_UPLOAD_SOURCE_PREFIX))
        : []
      const pythonTracks = Array.isArray(pythonResponse.data?.tracks) ? pythonResponse.data.tracks : []

      uploadedTracks.value = pythonTracks.map((pythonTrack) => {
        const matched = findMatchingUploadedResource(uploadedResources, pythonTrack)
        if (!matched) {
          return createTrack({
            ...pythonTrack,
            id: pythonTrack.id,
            filename: pythonTrack.filename,
            fileUrl: pythonTrack.filename ? getMusicFileUrl(pythonTrack.filename) : '',
            pythonTrackId: pythonTrack.id,
            source: buildUploadSource(pythonTrack.id),
            type: '本地上传',
          })
        }

        return normalizeMusicResourceTrack(matched, {
          ...pythonTrack,
          pythonTrackId: pythonTrack.id,
          filename: pythonTrack.filename,
          tags: pythonTrack.tags,
          type: '本地上传',
        })
      })
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
        getMyPlaylistsApi(),
        fetchUploadedTracks(),
      ])

      const preferences = prefRes.data || []
      likedTrackIds.value = preferences.filter(p => p.preferenceType === 1).map(p => normalizeId(p.musicId))
      collectedTrackIds.value = preferences.filter(p => p.preferenceType === 2).map(p => normalizeId(p.musicId))

      const playlists = Array.isArray(playlistsRes.data) ? playlistsRes.data : []
      customPlaylists.value = playlists.map(normalizePlaylist)
    } catch (error) {
      console.error('Failed to fetch user music data:', error)
      customPlaylists.value = []
      likedTrackIds.value = []
      collectedTrackIds.value = []
    }
  }

  const toggleLike = async (trackId) => {
    const normalizedTrackId = normalizeId(trackId)
    const idx = likedTrackIds.value.indexOf(normalizedTrackId)
    try {
      if (idx > -1) {
        await deleteMusicPreferenceApi(normalizedTrackId, 1)
        likedTrackIds.value.splice(idx, 1)
      } else {
        await upsertMusicPreferenceApi({ musicId: normalizedTrackId, preferenceType: 1 })
        likedTrackIds.value.push(normalizedTrackId)
      }
    } catch (e) {
      ElMessage.error('操作失败')
    }
  }

  const toggleCollect = async (trackId) => {
    const normalizedTrackId = normalizeId(trackId)
    const idx = collectedTrackIds.value.indexOf(normalizedTrackId)
    try {
      if (idx > -1) {
        await deleteMusicPreferenceApi(normalizedTrackId, 2)
        collectedTrackIds.value.splice(idx, 1)
      } else {
        await upsertMusicPreferenceApi({ musicId: normalizedTrackId, preferenceType: 2 })
        collectedTrackIds.value.push(normalizedTrackId)
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

    const uploadResponse = await uploadMusicFileApi(formData)
    const uploadedTrack = uploadResponse.data || {}
    let musicResource = null

    try {
      const resourceResponse = await createMusicResourceApi({
        title: uploadedTrack.title?.trim() || payload.title?.trim() || '未命名音乐',
        artist: uploadedTrack.artist?.trim() || payload.artist?.trim() || null,
        duration: Number(uploadedTrack.duration) || Number(payload.duration) || 0,
        fileUrl: uploadedTrack.filename ? getMusicFileUrl(uploadedTrack.filename) : '',
        coverUrl: '',
        source: buildUploadSource(uploadedTrack.id),
      })
      musicResource = resourceResponse.data
    } catch (error) {
      console.error('Sync uploaded track to music-service failed:', error)
    }

    const nextTrack = musicResource
      ? normalizeMusicResourceTrack(musicResource, {
          ...uploadedTrack,
          pythonTrackId: uploadedTrack.id,
          filename: uploadedTrack.filename,
          tags: uploadedTrack.tags,
          type: '本地上传',
        })
      : createTrack({
          ...uploadedTrack,
          id: uploadedTrack.id,
          filename: uploadedTrack.filename,
          fileUrl: uploadedTrack.filename ? getMusicFileUrl(uploadedTrack.filename) : '',
          pythonTrackId: uploadedTrack.id,
          source: buildUploadSource(uploadedTrack.id),
          type: '本地上传',
        })
    uploadedTracks.value = [nextTrack, ...uploadedTracks.value.filter(track => track.id !== nextTrack.id)]
    if (!musicResource) {
      ElMessage.warning('音乐文件已上传，但暂未同步到新曲库接口')
    }
    return nextTrack
  }

  const removeUploadedTrack = async (trackId) => {
    const normalizedTrackId = normalizeId(trackId)
    const targetTrack = findUploadedTrack(normalizedTrackId)
    const pythonTrackId = targetTrack?.pythonTrackId || parsePythonTrackId(targetTrack?.source)

    if (pythonTrackId) {
      await deleteUploadedMusicApi(pythonTrackId)
    }
    if (targetTrack?.musicResourceId) {
      await deleteMusicResourceApi(targetTrack.musicResourceId)
    }

    uploadedTracks.value = uploadedTracks.value.filter(t => t.id !== normalizedTrackId)
    likedTrackIds.value = likedTrackIds.value.filter(id => id !== normalizedTrackId)
    collectedTrackIds.value = collectedTrackIds.value.filter(id => id !== normalizedTrackId)
    customPlaylists.value = customPlaylists.value.map((playlist) => ({
      ...playlist,
      trackIds: playlist.trackIds.filter(id => id !== normalizedTrackId),
      tracks: playlist.tracks.filter(track => track.id !== normalizedTrackId),
    }))
    ElMessage.success('已删除上传音乐')
  }

  const createPlaylist = async (name, description = '自建歌单') => {
    try {
      const res = await createPlaylistApi({ name, description, coverUrl: DEFAULT_PLAYLIST_COVER })
      customPlaylists.value.unshift(normalizePlaylist(res.data))
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
    const trackId = normalizeId(track?.musicResourceId ?? track?.id)

    if (playlist && trackId && !playlist.trackIds.includes(trackId)) {
      try {
        await addTrackToPlaylistApi(playlistId, { musicId: trackId })
        playlist.trackIds.push(trackId)
        playlist.tracks.push(createTrack(track))
        ElMessage.success('已添加到歌单')
      } catch (e) {
        ElMessage.error('添加失败')
      }
    }
  }
  
  const removeTrackFromPlaylist = async (playlistId, trackId) => {
    const playlist = customPlaylists.value.find(p => p.id === playlistId)
    const normalizedTrackId = normalizeId(trackId)
    if (playlist) {
      try {
        await removeTrackFromPlaylistApi(playlistId, normalizedTrackId)
        playlist.trackIds = playlist.trackIds.filter(id => id !== normalizedTrackId)
        playlist.tracks = playlist.tracks.filter(t => t.id !== normalizedTrackId)
        ElMessage.success('已移出歌单')
      } catch (e) {
        ElMessage.error('移除失败')
      }
    }
  }

  return {
    publicTracks,
    uploadedTracks,
    recommendedTracks,
    emotionTags,
    likedTrackIds,
    collectedTrackIds,
    customPlaylists,
    fetchPublicTracks,
    fetchUploadedTracks,
    fetchEmotionTags,
    fetchRecommendedTracks,
    fetchNextRecommendedTrack,
    resolveEmotionTagId,
    resolveTrackById,
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
