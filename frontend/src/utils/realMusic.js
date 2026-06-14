const EMOTION_COLOR_MAP = {
  anger: '#F44336',
  calm: '#26A69A',
  disgust: '#7CB342',
  fear: '#9C27B0',
  joy: '#4CAF50',
  love: '#E91E63',
  neutral: '#9E9E9E',
  sadness: '#2196F3',
  surprise: '#FFC107',
}

const EMOTION_LABEL_MAP = {
  anger: '愤怒',
  calm: '平静',
  disgust: '厌恶',
  fear: '恐惧',
  joy: '高兴',
  love: '爱',
  neutral: '中性',
  sadness: '悲伤',
  surprise: '惊讶',
}

const REAL_CATEGORY_CONFIG = [
  { id: 'recommend', name: '今日推荐20首', description: '为您定制的推荐列表', emotions: ['neutral', 'joy'] },
  { id: 'guess', name: '猜你喜欢', description: '根据近期情绪推荐', emotions: ['love', 'surprise'] },
  { id: 'leaderboard', name: '排行榜', description: '大家都爱听', emotions: ['joy', 'anger'] },
  { id: 'new', name: '新音乐', description: '系统上新音乐', emotions: ['fear', 'surprise'] },
  { id: 'favorite', name: '我喜欢', description: '加入喜欢列表的音乐', emotions: ['joy', 'love'] },
  { id: 'collection', name: '我收藏', description: '加入收藏列表的音乐', emotions: ['neutral', 'sadness'] },
]

const EMOTION_ALIAS_MAP = {
  joy: 'joy',
  joyful: 'joy',
  happy: 'joy',
  love: 'love',
  surprise: 'surprise',
  sadness: 'sadness',
  sad: 'sadness',
  lonely: 'sadness',
  anger: 'anger',
  angry: 'anger',
  fear: 'fear',
  anxiety: 'fear',
  anxious: 'fear',
  neutral: 'neutral',
  calm: 'calm',
  relax: 'calm',
  focus: 'calm',
  tired: 'calm',
  hopeful: 'calm',
  disgust: 'disgust',
  '喜悦': 'joy',
  '开心': 'joy',
  '快乐': 'joy',
  '高兴': 'joy',
  '平静': 'calm',
  '放松': 'calm',
  '专注': 'calm',
  '疲惫': 'calm',
  '焦虑': 'fear',
  '悲伤': 'sadness',
  '孤独': 'sadness',
  '愤怒': 'anger',
  '希望': 'calm',
  '爱': 'love',
  '惊喜': 'surprise',
  '惊讶': 'surprise',
  '中性': 'neutral',
  '厌恶': 'disgust',
}

export const normalizeEmotion = (value) => {
  const lowerValue = String(value || '').toLowerCase()
  for (const [alias, target] of Object.entries(EMOTION_ALIAS_MAP)) {
    if (lowerValue === alias || lowerValue.includes(alias)) {
      return target
    }
  }
  if (lowerValue.startsWith('sad')) return 'sadness'
  if (lowerValue.startsWith('ang')) return 'anger'
  return lowerValue
}

export const getEmotionColor = (emotion) => EMOTION_COLOR_MAP[normalizeEmotion(emotion)] || '#9E9E9E'

export const getEmotionLabel = (emotion) => EMOTION_LABEL_MAP[normalizeEmotion(emotion)] || '未知'

export const getRealMusicCover = (emotion) => {
  const color = getEmotionColor(emotion)
  const label = getEmotionLabel(emotion)
  const svg = `
    <svg xmlns="http://www.w3.org/2000/svg" width="320" height="320" viewBox="0 0 320 320">
      <defs>
        <linearGradient id="g" x1="0%" y1="0%" x2="100%" y2="100%">
          <stop offset="0%" stop-color="${color}" stop-opacity="0.95" />
          <stop offset="100%" stop-color="#111827" stop-opacity="0.88" />
        </linearGradient>
      </defs>
      <rect width="320" height="320" rx="36" fill="url(#g)" />
      <circle cx="160" cy="160" r="86" fill="rgba(255,255,255,0.12)" />
      <circle cx="160" cy="160" r="28" fill="rgba(255,255,255,0.88)" />
      <text x="160" y="266" text-anchor="middle" font-family="Arial, sans-serif" font-size="28" fill="#FFFFFF">${label}</text>
    </svg>
  `

  return `data:image/svg+xml;utf8,${encodeURIComponent(svg)}`
}

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

const getDailyShuffled = (array, seedStr) => {
  let seed = 0
  for (let i = 0; i < seedStr.length; i++) {
    seed += seedStr.charCodeAt(i)
  }
  const random = () => {
    let x = Math.sin(seed++) * 10000
    return x - Math.floor(x)
  }
  const shuffled = [...array]
  for (let i = shuffled.length - 1; i > 0; i--) {
    const j = Math.floor(random() * (i + 1))
    ;[shuffled[i], shuffled[j]] = [shuffled[j], shuffled[i]]
  }
  return shuffled
}

const createFallbackTags = (emotion) => {
  const normalizedEmotion = normalizeEmotion(emotion)
  const label = getEmotionLabel(normalizedEmotion)
  return label === '未知' ? ['轻音乐'] : [label, normalizedEmotion]
}

export const normalizeMusicResourceTrack = (resource, tagNames = [], options = {}) => {
  const normalizedTags = [...new Set((Array.isArray(tagNames) ? tagNames : []).filter(Boolean))]
  const emotion = normalizeEmotion(normalizedTags[0] || options.emotion || 'neutral')

  return {
    ...resource,
    id: String(resource?.id ?? options.id ?? ''),
    musicResourceId: Number(resource?.id ?? options.musicResourceId ?? 0) || null,
    title: resource?.title || options.title || '未命名音乐',
    artist: resource?.artist?.trim() || options.artist || getEmotionLabel(emotion),
    duration: Number(resource?.duration ?? options.duration ?? 0) || 0,
    cover: options.cover || resource?.coverUrl || getRealMusicCover(emotion),
    coverUrl: resource?.coverUrl || options.coverUrl || '',
    fileUrl: resource?.fileUrl || options.fileUrl || '',
    filename: options.filename || extractFilenameFromUrl(resource?.fileUrl),
    tags: normalizedTags.length ? normalizedTags : createFallbackTags(emotion),
    emotion,
    type: options.type || '真实音乐',
    source: resource?.source || options.source || '',
  }
}

export const normalizeRealMusicTrack = (filename, index = 0, durationMap = {}) => {
  const safeFilename = String(filename || '')
  const baseName = safeFilename.replace(/\.mp3$/i, '')
  const emotionKey = normalizeEmotion(baseName.split('_')[0])

  return {
    id: `real-${baseName}`,
    title: baseName,
    artist: getEmotionLabel(emotionKey),
    duration: durationMap[safeFilename] || 0,
    cover: getRealMusicCover(emotionKey),
    tags: [getEmotionLabel(emotionKey), emotionKey],
    type: '真实音乐',
    emotion: emotionKey,
    filename: safeFilename,
    order: index,
  }
}

export const buildRealMusicCategories = (filenames, options = {}) => {
  const durationMap = options.durationMap || {}
  const tracks = filenames.map((filename, index) => normalizeRealMusicTrack(filename, index, durationMap))
  const tracksByEmotion = tracks.reduce((accumulator, track) => {
    if (!accumulator[track.emotion]) {
      accumulator[track.emotion] = []
    }
    accumulator[track.emotion].push(track)
    return accumulator
  }, {})

  const likedIds = new Set(options.likedIds || [])
  const collectedIds = new Set(options.collectedIds || [])

  const todayStr = new Date().toDateString()

  return REAL_CATEGORY_CONFIG.map((category, categoryIndex) => {
    let categoryTracks = category.emotions.flatMap((emotion) => tracksByEmotion[emotion] || [])

    if (category.id === 'recommend' || category.id === 'leaderboard') {
      // 每日随机获取 20 首
      categoryTracks = getDailyShuffled(tracks, todayStr + category.id).slice(0, 20)
    } else if (category.id === 'favorite') {
      categoryTracks = tracks.filter((track) => likedIds.has(track.id))
    } else if (category.id === 'collection') {
      categoryTracks = tracks.filter((track) => collectedIds.has(track.id))
    } else {
      if (!categoryTracks.length) {
        categoryTracks = tracks.filter((_, index) => index % REAL_CATEGORY_CONFIG.length === categoryIndex)
      }
    }

    return {
      ...category,
      tracks: categoryTracks,
    }
  })
}

export const buildMusicCategories = (tracks, options = {}) => {
  const normalizedTracks = uniqueTracks(Array.isArray(tracks) ? tracks : [])
  const recommendedTracks = uniqueTracks(Array.isArray(options.recommendedTracks) ? options.recommendedTracks : [])
  const likedIds = new Set(options.likedIds || [])
  const collectedIds = new Set(options.collectedIds || [])
  const todayStr = new Date().toDateString()

  return REAL_CATEGORY_CONFIG.map((category, categoryIndex) => {
    let categoryTracks = []

    if (category.id === 'recommend') {
      categoryTracks = recommendedTracks.length
        ? recommendedTracks.slice(0, 20)
        : getDailyShuffled(normalizedTracks, `${todayStr}-${category.id}`).slice(0, 20)
    } else if (category.id === 'guess') {
      const source = recommendedTracks.length > 1
        ? getDailyShuffled(recommendedTracks, `${todayStr}-${category.id}`)
        : normalizedTracks.filter((track) => ['love', 'surprise'].includes(normalizeEmotion(track.emotion || track.tags?.[0])))
      categoryTracks = source.slice(0, 20)
    } else if (category.id === 'leaderboard') {
      categoryTracks = getDailyShuffled(normalizedTracks, `${todayStr}-${category.id}`).slice(0, 20)
    } else if (category.id === 'new') {
      categoryTracks = [...normalizedTracks]
        .sort((a, b) => Number(b.musicResourceId || 0) - Number(a.musicResourceId || 0))
        .slice(0, 20)
    } else if (category.id === 'favorite') {
      categoryTracks = normalizedTracks.filter((track) => likedIds.has(String(track.id)))
    } else if (category.id === 'collection') {
      categoryTracks = normalizedTracks.filter((track) => collectedIds.has(String(track.id)))
    }

    if (['recommend', 'guess', 'leaderboard', 'new'].includes(category.id) && categoryTracks.length > 0 && categoryTracks.length < 20) {
      const existingIds = new Set(categoryTracks.map((track) => String(track?.id || '')))
      const fillSource = getDailyShuffled(normalizedTracks, `${todayStr}-${category.id}-fill`)
      for (const candidate of fillSource) {
        if (categoryTracks.length >= 20) break
        const candidateId = String(candidate?.id || '')
        if (!candidateId || existingIds.has(candidateId)) continue
        existingIds.add(candidateId)
        categoryTracks.push(candidate)
      }
    }

    if (!categoryTracks.length && category.id !== 'favorite' && category.id !== 'collection') {
      categoryTracks = normalizedTracks.filter((_, index) => index % REAL_CATEGORY_CONFIG.length === categoryIndex)
    }

    return {
      ...category,
      tracks: uniqueTracks(categoryTracks),
    }
  })
}

export const readRemoteAudioDuration = (url) =>
  new Promise((resolve) => {
    const audio = document.createElement('audio')
    const cleanup = () => {
      audio.onloadedmetadata = null
      audio.onerror = null
      audio.src = ''
    }

    audio.preload = 'metadata'
    audio.onloadedmetadata = () => {
      const duration = Number.isFinite(audio.duration) ? Math.floor(audio.duration) : 0
      cleanup()
      resolve(duration)
    }
    audio.onerror = () => {
      cleanup()
      resolve(0)
    }
    audio.src = url
  })
