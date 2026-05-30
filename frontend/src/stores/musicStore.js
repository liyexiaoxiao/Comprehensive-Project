import { defineStore } from 'pinia'
import { ref } from 'vue'
import {
  createEmotionTagApi,
  createMusicResourceApi,
  deleteMusicResourceApi,
  getEmotionTagsApi,
  getMusicPreferenceByMusicApi,
  getMyMusicPreferencesApi,
  getMyMusicResourcesApi,
  getMusicResourceTagsApi,
  getMusicResourcesApi,
  previewAiTagMusicFileApi,
  deleteMusicPreferenceApi,
  createPlaylistApi,
  addTrackToPlaylistApi,
  deletePlaylistApi,
  replaceMusicTagsApi,
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
const PREFERENCE_TYPE_LIKE = 1
const PREFERENCE_TYPE_COLLECT = 2
const PREFERENCE_TYPE_BLOCK = -1
const SUPPORTED_PREFERENCE_TYPES = [PREFERENCE_TYPE_LIKE, PREFERENCE_TYPE_COLLECT, PREFERENCE_TYPE_BLOCK]

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

const mergeTagNames = (...groups) => Array.from(new Set(
  groups
    .flat()
    .map(item => String(item || '').trim())
    .filter(Boolean),
))

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
  const blockedTrackIds = ref([])
  const customPlaylists = ref([])
  const trackPreferenceMap = ref({})

  const setExclusivePreferenceState = (trackId, preferenceType = null) => {
    const normalizedTrackId = normalizeId(trackId)
    if (!normalizedTrackId) return

    likedTrackIds.value = likedTrackIds.value.filter(id => id !== normalizedTrackId)
    collectedTrackIds.value = collectedTrackIds.value.filter(id => id !== normalizedTrackId)
    blockedTrackIds.value = blockedTrackIds.value.filter(id => id !== normalizedTrackId)

    if (preferenceType === PREFERENCE_TYPE_LIKE) {
      likedTrackIds.value.push(normalizedTrackId)
    } else if (preferenceType === PREFERENCE_TYPE_COLLECT) {
      collectedTrackIds.value.push(normalizedTrackId)
    } else if (preferenceType === PREFERENCE_TYPE_BLOCK) {
      blockedTrackIds.value.push(normalizedTrackId)
    }

    if (preferenceType == null) {
      const nextMap = { ...trackPreferenceMap.value }
      delete nextMap[normalizedTrackId]
      trackPreferenceMap.value = nextMap
      return
    }

    trackPreferenceMap.value = {
      ...trackPreferenceMap.value,
      [normalizedTrackId]: preferenceType,
    }
  }

  const buildEffectivePreferenceMap = (preferences = []) => {
    const grouped = new Map()
    for (const item of Array.isArray(preferences) ? preferences : []) {
      const musicId = normalizeId(item?.musicId)
      if (!musicId) continue
      const current = grouped.get(musicId)
      const nextType = Number(item?.preferenceType)

      if (nextType === PREFERENCE_TYPE_BLOCK) {
        grouped.set(musicId, PREFERENCE_TYPE_BLOCK)
        continue
      }
      if (nextType === PREFERENCE_TYPE_LIKE) {
        grouped.set(musicId, PREFERENCE_TYPE_LIKE)
        continue
      }
      if (nextType === PREFERENCE_TYPE_COLLECT && current == null) {
        grouped.set(musicId, PREFERENCE_TYPE_COLLECT)
      }
    }
    return Object.fromEntries(grouped.entries())
  }

  const applyPreferenceMap = (nextPreferenceMap = {}) => {
    trackPreferenceMap.value = { ...nextPreferenceMap }
    likedTrackIds.value = Object.keys(nextPreferenceMap).filter(id => nextPreferenceMap[id] === PREFERENCE_TYPE_LIKE)
    collectedTrackIds.value = Object.keys(nextPreferenceMap).filter(id => nextPreferenceMap[id] === PREFERENCE_TYPE_COLLECT)
    blockedTrackIds.value = Object.keys(nextPreferenceMap).filter(id => nextPreferenceMap[id] === PREFERENCE_TYPE_BLOCK)
  }

  const getTrackPreferenceType = (trackId) => {
    const normalizedTrackId = normalizeId(trackId)
    return trackPreferenceMap.value[normalizedTrackId] ?? null
  }

  const removePreferenceTypesFromServer = async (trackId, preferenceTypes = SUPPORTED_PREFERENCE_TYPES) => {
    const normalizedTrackId = normalizeId(trackId)
    await Promise.all(preferenceTypes.map(async (preferenceType) => {
      try {
        await deleteMusicPreferenceApi(normalizedTrackId, preferenceType)
      } catch (error) {
        if (error?.response?.status !== 404) {
          throw error
        }
      }
    }))
  }

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

  const ensureEmotionTagIds = async (tagNames) => {
    const uniqueNames = Array.from(new Set(
      (Array.isArray(tagNames) ? tagNames : [])
        .map(item => String(item || '').trim())
        .filter(Boolean),
    ))
    if (!uniqueNames.length) {
      return []
    }

    if (!emotionTags.value.length) {
      await fetchEmotionTags()
    }

    const ids = []
    let shouldRefreshTags = false
    for (const tagName of uniqueNames) {
      let existing = emotionTags.value.find((tag) => String(tag?.tagName || '').trim() === tagName)
      if (!existing) {
        const response = await createEmotionTagApi({ tagName })
        existing = response.data
        shouldRefreshTags = true
      }
      if (existing?.id != null) {
        ids.push(Number(existing.id))
      }
    }

    if (shouldRefreshTags) {
      await fetchEmotionTags()
    }
    return ids
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
          .filter(track => getTrackPreferenceType(track.id) !== PREFERENCE_TYPE_BLOCK)
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
          .filter(track => getTrackPreferenceType(track.id) !== PREFERENCE_TYPE_BLOCK)
          .slice(0, limit)
      }
    } catch (error) {
      const normalizedEmotion = normalizeEmotion(emotion || 'neutral')
      recommendedTracks.value = publicTracks.value
        .filter(track => normalizeEmotion(track?.emotion || track?.tags?.[0]) === normalizedEmotion)
        .filter(track => getTrackPreferenceType(track.id) !== PREFERENCE_TYPE_BLOCK)
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
      return trackEmotion === normalizedEmotion
        && normalizeId(track.id) !== normalizeId(currentMusicId)
        && getTrackPreferenceType(track.id) !== PREFERENCE_TYPE_BLOCK
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
      const resourceTagPairs = await Promise.all(uploadedResources.map(async (resource) => {
        try {
          const response = await getMusicResourceTagsApi(resource.id)
          const tagNames = Array.isArray(response.data)
            ? response.data.map(item => item?.tagName).filter(Boolean)
            : []
          return [resource.id, tagNames]
        } catch (error) {
          return [resource.id, []]
        }
      }))
      const resourceTagMap = Object.fromEntries(resourceTagPairs)

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
          tags: mergeTagNames(pythonTrack.tags, resourceTagMap[matched.id]),
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
      applyPreferenceMap(buildEffectivePreferenceMap(preferences))

      const playlists = Array.isArray(playlistsRes.data) ? playlistsRes.data : []
      customPlaylists.value = playlists.map(normalizePlaylist)
    } catch (error) {
      console.error('Failed to fetch user music data:', error)
      customPlaylists.value = []
      applyPreferenceMap({})
    }
  }

  const fetchTrackPreference = async (trackId) => {
    const normalizedTrackId = normalizeId(trackId)
    if (!normalizedTrackId) return null

    try {
      const response = await getMusicPreferenceByMusicApi(normalizedTrackId)
      const preferenceType = Number(response.data?.preferenceType)
      if (!SUPPORTED_PREFERENCE_TYPES.includes(preferenceType)) {
        setExclusivePreferenceState(normalizedTrackId, null)
        return null
      }
      setExclusivePreferenceState(normalizedTrackId, preferenceType)
      return preferenceType
    } catch (error) {
      if (error?.response?.status === 404) {
        setExclusivePreferenceState(normalizedTrackId, null)
        return null
      }
      console.error('Failed to fetch track preference:', error)
      return getTrackPreferenceType(normalizedTrackId)
    }
  }

  const setTrackPreference = async (trackId, preferenceType) => {
    const normalizedTrackId = normalizeId(trackId)
    if (!normalizedTrackId) return null

    const nextPreferenceType = getTrackPreferenceType(normalizedTrackId) === preferenceType
      ? null
      : preferenceType

    try {
      await removePreferenceTypesFromServer(
        normalizedTrackId,
        nextPreferenceType == null
          ? SUPPORTED_PREFERENCE_TYPES
          : SUPPORTED_PREFERENCE_TYPES.filter(type => type !== nextPreferenceType),
      )

      if (nextPreferenceType != null) {
        await upsertMusicPreferenceApi({ musicId: normalizedTrackId, preferenceType: nextPreferenceType })
      }

      setExclusivePreferenceState(normalizedTrackId, nextPreferenceType)
      return nextPreferenceType
    } catch (error) {
      console.error('Failed to set track preference:', error)
      ElMessage.error('偏好操作失败')
      await fetchTrackPreference(normalizedTrackId)
      return getTrackPreferenceType(normalizedTrackId)
    }
  }

  const toggleLike = async (trackId) => {
    return setTrackPreference(trackId, PREFERENCE_TYPE_LIKE)
  }

  const toggleCollect = async (trackId) => {
    return setTrackPreference(trackId, PREFERENCE_TYPE_COLLECT)
  }

  const toggleBlock = async (trackId) => {
    return setTrackPreference(trackId, PREFERENCE_TYPE_BLOCK)
  }

  const previewTrackEmotionTags = async (file, options = {}) => {
    if (!file) {
      throw new Error('请选择要识别的音频文件')
    }
    const maxTags = Number(options.maxTags) > 0 ? Number(options.maxTags) : 3

    const formData = new FormData()
    formData.append('file', file)
    formData.append('maxTags', String(maxTags))
    formData.append('title', String(options.title || '').trim())
    formData.append('artist', String(options.artist || '').trim())

    const response = await previewAiTagMusicFileApi(formData)
    return {
      caption: response.data?.caption || '',
      inferredEmotionKeys: Array.isArray(response.data?.inferredEmotionKeys) ? response.data.inferredEmotionKeys : [],
      tagNames: Array.isArray(response.data?.tagNames) ? response.data.tagNames.filter(Boolean) : [],
    }
  }

  const uploadTrack = async (payload) => {
    const confirmedTagNames = mergeTagNames(payload.tags)
    const formData = new FormData()
    formData.append('file', payload.file)
    formData.append('title', payload.title?.trim() || '')
    formData.append('artist', payload.artist?.trim() || '')
    formData.append('duration', String(payload.duration || 0))
    formData.append('tags', JSON.stringify(confirmedTagNames))

    const uploadResponse = await uploadMusicFileApi(formData)
    const uploadedTrack = uploadResponse.data || {}
    let musicResource = null

    try {
      const resourceResponse = await createMusicResourceApi({
        title: uploadedTrack.title?.trim() || payload.title?.trim() || '未命名音乐',
        artist: uploadedTrack.artist?.trim() || payload.artist?.trim() || null,
        duration: Number(uploadedTrack.duration) || Number(payload.duration) || 0,
        fileUrl: uploadedTrack.filename ? getMusicFileUrl(uploadedTrack.filename) : '',
        coverUrl: String(payload.coverUrl || '').trim(),
        source: buildUploadSource(uploadedTrack.id),
      })
      musicResource = resourceResponse.data
      if (musicResource?.id != null) {
        const tagIds = await ensureEmotionTagIds(confirmedTagNames)
        if (tagIds.length) {
          await replaceMusicTagsApi(musicResource.id, {
            tagIds,
            source: 'manual',
          })
        }
      }
    } catch (error) {
      console.error('Sync uploaded track to music-service failed:', error)
      if (musicResource?.id != null) {
        ElMessage.warning('音乐已上传，但同步情绪标签到曲库失败，请稍后重试')
      }
    }

    const nextTrack = musicResource
      ? normalizeMusicResourceTrack(musicResource, {
          ...uploadedTrack,
          pythonTrackId: uploadedTrack.id,
          filename: uploadedTrack.filename,
          tags: mergeTagNames(uploadedTrack.tags, confirmedTagNames),
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
    setExclusivePreferenceState(normalizedTrackId, null)
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
    blockedTrackIds,
    customPlaylists,
    trackPreferenceMap,
    fetchPublicTracks,
    fetchUploadedTracks,
    fetchEmotionTags,
    fetchRecommendedTracks,
    fetchNextRecommendedTrack,
    resolveEmotionTagId,
    resolveTrackById,
    fetchUserData,
    fetchTrackPreference,
    getTrackPreferenceType,
    toggleLike,
    toggleCollect,
    toggleBlock,
    previewTrackEmotionTags,
    uploadTrack,
    removeUploadedTrack,
    createPlaylist,
    deletePlaylist,
    addTrackToPlaylist,
    removeTrackFromPlaylist
  }
})
