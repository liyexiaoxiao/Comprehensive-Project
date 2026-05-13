const EMOTION_COLOR_MAP = {
  anger: '#F44336',
  fear: '#9C27B0',
  joy: '#4CAF50',
  love: '#E91E63',
  neutral: '#9E9E9E',
  sadness: '#2196F3',
  surprise: '#FFC107',
}

const EMOTION_LABEL_MAP = {
  anger: '愤怒',
  fear: '恐惧',
  joy: '喜悦',
  love: '爱',
  neutral: '平静',
  sadness: '悲伤',
  surprise: '惊喜',
}

const REAL_CATEGORY_CONFIG = [
  { id: 'recommend', name: '今日推荐20首', description: '为您定制的推荐列表', emotions: ['neutral', 'joy'] },
  { id: 'guess', name: '猜你喜欢', description: '根据近期情绪推荐', emotions: ['love', 'surprise'] },
  { id: 'leaderboard', name: '排行榜', description: '大家都爱听', emotions: ['joy', 'anger'] },
  { id: 'new', name: '新音乐', description: '系统上新音乐', emotions: ['fear', 'surprise'] },
  { id: 'favorite', name: '我喜欢', description: '加入喜欢列表的音乐', emotions: ['joy', 'love'] },
  { id: 'collection', name: '我收藏', description: '加入收藏列表的音乐', emotions: ['neutral', 'sadness'] },
]

const normalizeEmotion = (value) => {
  const lowerValue = String(value || '').toLowerCase()
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

  // 简单的伪随机打乱函数（根据当天的日期字符串生成固定的随机序列）
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
      const j = Math.floor(random() * (i + 1));
      [shuffled[i], shuffled[j]] = [shuffled[j], shuffled[i]]
    }
    return shuffled
  }

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
