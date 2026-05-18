import { buildMusicCategories, getRealMusicCover, normalizeEmotion } from '@/utils/realMusic'

const DEFAULT_OFFICIAL_PLAYLISTS = [
  {
    id: 'official-neutral',
    name: '官方·中性歌单',
    description: '适合情绪平稳、不过度起伏，想安静陪伴自己的时刻。',
    tagName: '中性',
    coverEmotion: 'neutral',
    emotions: ['neutral'],
    keywords: ['中性', 'neutral'],
  },
  {
    id: 'official-joy',
    name: '官方·高兴歌单',
    description: '适合开心、轻快、想把好心情继续延长的时刻。',
    tagName: '高兴',
    coverEmotion: 'joy',
    emotions: ['joy'],
    keywords: ['高兴', '喜悦', '快乐', '开心', 'joy', 'happy'],
  },
  {
    id: 'official-sadness',
    name: '官方·悲伤歌单',
    description: '允许情绪被温柔接住，先陪你慢慢消化低落。',
    tagName: '悲伤',
    coverEmotion: 'sadness',
    emotions: ['sadness'],
    keywords: ['悲伤', 'sad', 'sadness', '孤独'],
  },
  {
    id: 'official-fear',
    name: '官方·恐惧歌单',
    description: '给焦虑和不安留出缓冲区，慢慢找回安全感。',
    tagName: '恐惧',
    coverEmotion: 'fear',
    emotions: ['fear'],
    keywords: ['恐惧', 'fear', '焦虑', 'anxious', 'anxiety'],
  },
  {
    id: 'official-disgust',
    name: '官方·厌恶歌单',
    description: '适合需要与排斥感、烦躁感拉开距离，慢慢整理情绪的时刻。',
    tagName: '厌恶',
    coverEmotion: 'disgust',
    emotions: ['disgust'],
    keywords: ['厌恶', 'disgust'],
  },
  {
    id: 'official-surprise',
    name: '官方·惊讶歌单',
    description: '适合感受新鲜、意外和被触动时的跳跃心情。',
    tagName: '惊讶',
    coverEmotion: 'surprise',
    emotions: ['surprise'],
    keywords: ['惊讶', '惊喜', 'surprise'],
  },
  {
    id: 'official-calm',
    name: '官方·平静歌单',
    description: '适合放慢呼吸、慢慢松下来，让身心重新沉静的时刻。',
    tagName: '平静',
    coverEmotion: 'calm',
    emotions: ['calm'],
    keywords: ['平静', 'calm', '放松', '舒缓'],
  },
  {
    id: 'official-anger',
    name: '官方·愤怒歌单',
    description: '帮助释放紧绷与躁动，让身体和思绪重新稳定。',
    tagName: '愤怒',
    coverEmotion: 'anger',
    emotions: ['anger'],
    keywords: ['愤怒', 'anger', 'angry'],
  },
]

const clonePlaylist = (playlist) => ({
  ...playlist,
  emotions: Array.isArray(playlist?.emotions) ? [...playlist.emotions] : [],
  keywords: Array.isArray(playlist?.keywords) ? [...playlist.keywords] : [],
})

export const getDefaultOfficialPlaylists = () => DEFAULT_OFFICIAL_PLAYLISTS.map(clonePlaylist)

export const mergeOfficialPlaylistConfigs = (configs = []) => {
  const defaults = getDefaultOfficialPlaylists()
  const overrideMap = new Map((Array.isArray(configs) ? configs : []).map((item) => [String(item?.playlistKey || item?.id || ''), item]))
  return defaults.map((playlist) => {
    const override = overrideMap.get(String(playlist.id))
    if (!override) {
      return playlist
    }
    return clonePlaylist({
      ...playlist,
      ...override,
      id: override.id || override.playlistKey || playlist.id,
    })
  })
}

const uniqueTracks = (tracks = []) => {
  const seen = new Set()
  return tracks.filter((track) => {
    const id = String(track?.id || '')
    if (!id || seen.has(id)) {
      return false
    }
    seen.add(id)
    return true
  })
}

const normalizeText = (value) => String(value || '').trim().toLowerCase()

const getTrackSearchFields = (track) => [
  track?.title,
  track?.artist,
  track?.emotion,
  track?.filename,
  ...(Array.isArray(track?.tags) ? track.tags : []),
]
  .map(normalizeText)
  .filter(Boolean)

const matchOfficialTrack = (track, playlist) => {
  const trackEmotion = normalizeEmotion(track?.emotion || track?.tags?.[0] || '')
  const emotionMatched = playlist.emotions.some((emotion) => normalizeEmotion(emotion) === trackEmotion)
  if (emotionMatched) {
    return true
  }

  const fields = getTrackSearchFields(track)
  return playlist.keywords.some((keyword) => {
    const normalizedKeyword = normalizeText(keyword)
    return fields.some((field) => field.includes(normalizedKeyword))
  })
}

const buildFeaturedPlaylists = ({ publicTracks, recommendedTracks, likedIds, collectedIds }) =>
  buildMusicCategories(publicTracks, {
    recommendedTracks,
    likedIds,
    collectedIds,
  }).map((playlist) => ({
    ...playlist,
    kind: 'featured',
    sourceLabel: '主页精选',
    cover: playlist.tracks?.[0]?.cover || getRealMusicCover(playlist.tracks?.[0]?.emotion || 'neutral'),
  }))

export const buildOfficialEmotionPlaylists = (publicTracks, playlistConfigs = getDefaultOfficialPlaylists()) =>
  playlistConfigs.map((playlist) => {
    const tracks = uniqueTracks((Array.isArray(publicTracks) ? publicTracks : []).filter((track) => matchOfficialTrack(track, playlist)))

    return {
      ...playlist,
      kind: 'official',
      sourceLabel: '官方情绪',
      tracks,
      cover: tracks[0]?.cover || getRealMusicCover(playlist.coverEmotion),
    }
  })

const buildCustomPlaylists = ({ customPlaylists, resolveTrackById, fallbackTracks }) =>
  (Array.isArray(customPlaylists) ? customPlaylists : []).map((playlist) => {
    const tracks = uniqueTracks((playlist.trackIds || [])
      .map((trackId) => resolveTrackById(trackId) || fallbackTracks.find((track) => String(track?.id || '') === String(trackId)))
      .filter(Boolean))

    return {
      ...playlist,
      kind: 'custom',
      sourceLabel: '我的歌单',
      tracks,
      cover: playlist.cover || tracks[0]?.cover || getRealMusicCover(tracks[0]?.emotion || 'neutral'),
    }
  })

export const buildAllPlaylistCatalog = ({
  publicTracks = [],
  recommendedTracks = [],
  likedIds = [],
  collectedIds = [],
  officialPlaylistConfigs = getDefaultOfficialPlaylists(),
  customPlaylists = [],
  resolveTrackById = () => null,
}) => {
  const fallbackTracks = uniqueTracks([...publicTracks, ...recommendedTracks])

  return [
    ...buildFeaturedPlaylists({
      publicTracks,
      recommendedTracks,
      likedIds,
      collectedIds,
    }),
    ...buildOfficialEmotionPlaylists(publicTracks, officialPlaylistConfigs),
    ...buildCustomPlaylists({
      customPlaylists,
      resolveTrackById,
      fallbackTracks,
    }),
  ]
}

export const playlistSourceTabs = [
  { id: 'all', label: '全部歌单' },
  { id: 'featured', label: '主页精选' },
  { id: 'official', label: '官方情绪' },
  { id: 'custom', label: '我的歌单' },
]
