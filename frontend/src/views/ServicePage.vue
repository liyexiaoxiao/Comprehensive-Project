<template>
  <div class="service-page">
    <div class="blur-orb orb-coral"></div>
    <div class="blur-orb orb-leaf"></div>
    <div class="blur-orb orb-sky"></div>

    <div class="service-shell">
      <section class="music-panel surface warm-surface">
        <div class="panel-top-bar">
          <div class="portal-actions">
            <button class="portal-btn" type="button" @click="$router.push({ name: 'meditation-room' })">
              <span class="portal-icon">🌌</span>
              <span>冥想室</span>
            </button>
            <button class="portal-btn" type="button" @click="$router.push({ name: 'personal-space' })">
              <span class="portal-icon">🌿</span>
              <span>个人空间</span>
            </button>
            <button v-if="isAdmin" class="portal-btn" type="button" @click="$router.push({ name: 'admin' })">
              <span class="portal-icon">🛠</span>
              <span>管理员端</span>
            </button>
          </div>
          <div class="top-bar-actions">
            <button class="secondary-link-btn" type="button" :disabled="isLoggingOut" @click="handleLogout">
              {{ isLoggingOut ? '退出中...' : '退出登录' }}
            </button>
            <RouterLink class="back-link" to="/">回首页</RouterLink>
          </div>
        </div>

        <div class="panel-head">
          <div>
            <h1>今晚你想走进哪一种声场？</h1>
            <p class="lead">
              不同音乐区像花园里的不同小径，有的更安睡，有的更专注......
            </p>
          </div>
          <button class="catalog-link-btn" type="button" @click="openAllPlaylists">
            查看所有歌单
          </button>
        </div>

        <div class="top-tools">
          <label class="search-field">
            <span>搜索音乐</span>
            <div class="search-input-wrapper">
              <input v-model.trim="searchText" type="text" placeholder="搜索纯音乐主题或情绪" />
              <button class="search-btn">搜索</button>
            </div>
          </label>
        </div>

        <div class="category-grid" ref="categoryScrollRef" @wheel.prevent="handleWheel">
          <button
            v-for="category in musicCategories"
            :key="category.id"
            class="category-card"
            :class="{ active: category.id === activeCategoryId }"
            type="button"
            @click="activeCategoryId = category.id"
          >
            <strong>{{ category.name }}</strong>
            <span>{{ category.description }}</span>
          </button>
        </div>

        <div class="track-section">
          <div class="section-head">
            <div>
              <h2>{{ activeCategory.name }}</h2>
            </div>
            <span class="count-pill">{{ filteredTracks.length }} 首曲目</span>
          </div>

          <div class="track-list">
            <button
              v-for="track in filteredTracks"
              :key="track.id"
              class="track-card"
              :class="{ current: track.id === currentTrack.id }"
              type="button"
              @click="selectTrack(track)"
            >
              <img :src="track.cover" :alt="track.title" />
              <div class="track-meta">
                <strong>{{ track.title }}</strong>
                <span>{{ track.artist }}</span>
              </div>
              <span class="track-time">{{ formatTime(track.duration) }}</span>
            </button>

            <div v-if="filteredTracks.length === 0" class="empty-state">
              没有找到匹配的音乐，换个关键词试试看。
            </div>
          </div>
        </div>

        <div class="player-shell" role="button" tabindex="0" @click="openPlayerPage" @keyup.enter="openPlayerPage">
          <div class="now-playing">
            <img :src="currentTrack.cover" :alt="currentTrack.title" />
            <div>
              <strong>{{ currentTrack.title }}</strong>
              <span>{{ currentTrack.artist }}</span>
            </div>
          </div>

          <div class="player-center">
            <div class="player-actions">
              <button class="action-button" type="button" @click.stop="playPrevious">
                <span>上一首</span>
              </button>
              <button class="primary-button" type="button" @click.stop="togglePlayback">
                <span>{{ isPlaying ? '暂停播放' : '开始播放' }}</span>
              </button>
              <button class="action-button" type="button" @click.stop="playNext">
                <span>下一首</span>
              </button>
            </div>

            <div class="progress-row">
              <span>{{ formatTime(progressSeconds) }}</span>
              <input
                v-model="progressSeconds"
                type="range"
                min="0"
                :max="currentTrack.duration"
                step="1"
                @click.stop
              />
              <span>{{ formatTime(currentTrack.duration) }}</span>
            </div>
          </div>

          <label class="volume-control" @click.stop>
            <span>音量</span>
            <input v-model="volume" type="range" min="0" max="100" />
          </label>
        </div>
      </section>

      <section class="companion-panel surface cool-surface">
        <div class="panel-head">
          <div>
            <p class="kicker">Voice Companion</p>
            <h2>先说一句，让今天的情绪被看见。</h2>
          </div>
          <div class="status-pill" :class="{ listening: isListening }">
            {{
              isListening
                ? '正在聆听'
                : voiceSupported
                  ? '可语音输入'
                  : '当前浏览器不支持语音输入'
            }}
          </div>
        </div>

        <div class="chat-stream">
          <article
            v-for="message in messages"
            :key="message.id"
            class="message-card"
            :class="message.role"
          >
            <span class="message-role">{{ message.role === 'assistant' ? '陪伴助手' : '你' }}</span>
            <p>{{ message.content }}</p>
            <div v-if="message.emotions" class="emotion-tags">
              <span v-if="message.emotions.speech" class="emotion-tag speech">
                🎤 {{ message.emotions.speech }}
              </span>
              <span v-if="message.emotions.complex" class="emotion-tag complex">
                💭 {{ message.emotions.complex }}
              </span>
            </div>
            <div v-if="message.toolResults?.length" class="tool-results">
              <section
                v-for="(result, resultIndex) in message.toolResults"
                :key="`${message.id}-tool-${resultIndex}`"
                class="music-recommendations"
              >
                <button
                  v-for="track in result.items"
                  :key="track.id"
                  class="recommend-track-card"
                  type="button"
                  @click="selectRecommendedTrack(track)"
                >
                  <img :src="track.cover" :alt="track.title" />
                  <div>
                    <strong>{{ track.title }}</strong>
                    <span>{{ track.reason }}</span>
                  </div>
                </button>
              </section>
            </div>
            <small>{{ message.timestamp }}</small>
          </article>
        </div>

        <div class="voice-box">
          <label class="voice-selector">
            <span>AI 声线</span>
            <select v-model="selectedTtsVoice">
              <option
                v-for="voice in ttsVoiceOptions"
                :key="voice.value"
                :value="voice.value"
              >
                {{ voice.label }}
              </option>
            </select>
          </label>
          <button class="primary-button full" type="button" @click="toggleVoiceInput">
            <span>{{ isListening ? '结束语音输入' : '开始语音输入' }}</span>
          </button>
          <p class="heard-text">
            {{ heardText || '点击后可直接说话；语音会转文字后发送给 AI，识别情绪并给出陪伴回复。' }}
          </p>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { storeToRefs } from 'pinia'
import { ElMessage } from 'element-plus'
import { analyzeCompanionAudioApi, askCompanionApi, getAiAssetUrl } from '@/api/ai'
import { initialMessages } from '@/data/mockContent'
import { getMusicFileByNameApi } from '@/api/python'
import { useMusicStore } from '@/stores/musicStore'
import { usePlayerStore } from '@/stores/playerStore'
import { useSpeechStore } from '@/stores/speech'
import { buildMusicCategories, getRealMusicCover } from '@/utils/realMusic'
import { getCurrentUserFromStorage, isAdminUser } from '@/api/user'
import { logoutSession } from '@/api/session'

const router = useRouter()
const route = useRoute()
const musicStore = useMusicStore()
const playerStore = usePlayerStore()
const speechStore = useSpeechStore()
const PLAYER_SESSION_KEY = 'emotion-system-active-player'
const isAdmin = computed(() => isAdminUser(getCurrentUserFromStorage()))
const { currentTrack, isLoadingAudio, isPlaying, progressSeconds, volume } = storeToRefs(playerStore)

const emptyTrack = {
  id: '',
  title: '暂无音乐',
  artist: '',
  duration: 0,
  cover: getRealMusicCover('neutral'),
  tags: ['平静'],
  type: '真实音乐',
}

const defaultCategoryMoodMap = {
  recommend: ['平静', '晚间', '真实音乐'],
  guess: ['治愈', '舒缓', '真实音乐'],
  leaderboard: ['专注', '稳定', '真实音乐'],
  new: ['清新', '放松', '真实音乐'],
  favorite: ['喜欢', '珍藏', '真实音乐'],
  collection: ['温柔', '陪伴', '真实音乐'],
}

const activeCategoryId = ref('recommend')
const searchText = ref('')
const messages = ref([...initialMessages])
const heardText = ref('')
const isListening = ref(false)
const selectedTtsVoice = ref('claire')
const mediaRecorder = ref(null)
const audioChunks = ref([])
const audioCaptureActive = ref(false)
const recognition = ref(null)
const interimTranscript = ref('')
const finalTranscript = ref('')
const categoryScrollRef = ref(null)
const isLoggingOut = ref(false)

if (!currentTrack.value) {
  playerStore.setCurrentTrack({ ...emptyTrack })
}

const ttsVoiceOptions = [
  { value: 'claire', label: 'Claire 温柔女声' },
  { value: 'anna', label: 'Anna 沉稳女声' },
  { value: 'bella', label: 'Bella 明亮女声' },
  { value: 'diana', label: 'Diana 欢快女声' },
  { value: 'alex', label: 'Alex 男声' },
  { value: 'benjamin', label: 'Benjamin 男声' },
  { value: 'charles', label: 'Charles 男声' },
  { value: 'david', label: 'David 男声' },
]

const assistantSpeechPlayer = new Audio()

const voiceSupported = typeof window !== 'undefined'
  ? Boolean(navigator.mediaDevices?.getUserMedia)
  : false

const speechRecognitionSupported = typeof window !== 'undefined'
  ? Boolean(window.SpeechRecognition || window.webkitSpeechRecognition)
  : false

const currentEmotionForRecommend = computed(() => {
  const storedEmotion = typeof window !== 'undefined'
    ? window.localStorage.getItem('currentEmotion')
    : ''
  return speechStore.emotion || storedEmotion || 'neutral'
})

const musicCategories = computed(() => {
  const publicCategories = buildMusicCategories(musicStore.publicTracks, {
    recommendedTracks: musicStore.recommendedTracks,
    likedIds: musicStore.likedTrackIds,
    collectedIds: musicStore.collectedTrackIds,
  })
  const publicLibrary = musicStore.publicTracks
  const customCats = musicStore.customPlaylists.map((playlist) => ({
    id: playlist.id,
    name: playlist.name,
    description: playlist.description || '自建歌单',
    tracks: playlist.trackIds?.map(id => musicStore.resolveTrackById(id) || publicLibrary.find(track => track.id === id)).filter(Boolean) || [],
  }))
  return [...publicCategories, ...customCats]
})

const activeCategory = computed(
  () => musicCategories.value.find((category) => category.id === activeCategoryId.value) || musicCategories.value[0] || {
    id: 'empty',
    name: '真实音乐',
    description: '暂无音乐可播放',
    tracks: [],
  },
)

const filteredTracks = computed(() => {
  const keyword = searchText.value.toLowerCase()
  if (!keyword) {
    return activeCategory.value.tracks || []
  }

  return (activeCategory.value.tracks || []).filter((track) => {
    const fields = [track.title, track.artist, track.filename, track.emotion, ...(track.tags || [])]
    return fields.some((field) => String(field || '').toLowerCase().includes(keyword))
  })
})

const formatTime = (seconds) => {
  const safeValue = Number.isFinite(seconds) ? seconds : 0
  const minutes = Math.floor(safeValue / 60)
  const remain = Math.floor(safeValue % 60)
  return `${String(minutes).padStart(2, '0')}:${String(remain).padStart(2, '0')}`
}

const handleWheel = (event) => {
  if (categoryScrollRef.value) {
    const scrollAmount = event.deltaY > 0 ? 300 : -300
    categoryScrollRef.value.scrollBy({
      left: scrollAmount,
      behavior: 'smooth',
    })
  }
}

const resetAudioState = () => {
  playerStore.resetPlayer()
}

const ensureCurrentTrack = () => {
  const availableTracks = filteredTracks.value.length ? filteredTracks.value : activeCategory.value.tracks || []
  if (!availableTracks.length) {
    if (!currentTrack.value?.id) {
      playerStore.setCurrentTrack({ ...emptyTrack })
      resetAudioState()
    }
    return
  }

  if (!currentTrack.value?.id) {
    playerStore.setCurrentTrack({ ...availableTracks[0] })
    return
  }

  const matchedTrack = availableTracks.find((track) => track.id === currentTrack.value.id)
  if (matchedTrack) {
    playerStore.setCurrentTrack({
      ...matchedTrack,
      duration: currentTrack.value.duration || matchedTrack.duration || 0,
    })
  }
}

const loadMusicLibrary = async () => {
  try {
    await Promise.all([
      musicStore.fetchEmotionTags(),
      musicStore.fetchPublicTracks(),
    ])
    await musicStore.fetchRecommendedTracks(currentEmotionForRecommend.value)
    if (!musicCategories.value.some((category) => category.id === activeCategoryId.value)) {
      activeCategoryId.value = musicCategories.value[0]?.id || 'recommend'
    }
    ensureCurrentTrack()
  } catch (error) {
    currentTrack.value = { ...emptyTrack }
    ElMessage.error('音乐目录加载失败，请确认 music-service 已启动。')
    console.error('Load music library failed:', error)
  }
}

const syncCurrentTrackSnapshot = () => {
  if (!currentTrack.value?.id) return
  const sourceTracks = musicCategories.value.flatMap((category) => category.tracks || [])
  const matchedTrack = sourceTracks.find((track) => track.id === currentTrack.value.id)
  if (!matchedTrack) return
  currentTrack.value = {
    ...matchedTrack,
    duration: progressSeconds.value > 0
      ? Math.max(matchedTrack.duration || 0, currentTrack.value.duration || 0)
      : matchedTrack.duration || currentTrack.value.duration || 0,
  }
}

const loadAudioForTrack = async (track, autoplay = true) => {
  if (!track?.id || isLoadingAudio.value) return

  try {
    if (track.file) {
      await playerStore.loadSourceFromBlob(track, track.file, {
        startAt: progressSeconds.value || 0,
        autoplay,
      })
      return
    } else if (track.fileUrl) {
      await playerStore.loadSourceFromUrl(track, track.fileUrl, {
        startAt: progressSeconds.value || 0,
        autoplay,
      })
      return
    } else if (track.filename) {
      const response = await getMusicFileByNameApi(track.filename)
      await playerStore.loadSourceFromBlob(track, response.data, {
        startAt: progressSeconds.value || 0,
        autoplay,
      })
      return
    }
    throw new Error('missing audio source')
  } catch (error) {
    ElMessage.error('音乐播放失败，请确认 Python backend 已启动。')
    console.error('Service audio playback failed:', error)
  }
}

const selectTrack = async (track) => {
  if (!track?.id) return

  if (currentTrack.value.id === track.id && playerStore.isTrackLoaded(track.id)) {
    await togglePlayback()
    return
  }

  playerStore.setCurrentTrack({ ...track })
  progressSeconds.value = 0
  await loadAudioForTrack(currentTrack.value, true)
}

const selectRecommendedTrack = async (track) => {
  if (!track?.filename) return
  await selectTrack({
    ...track,
    type: track.type || 'AI推荐',
    tags: Array.isArray(track.tags) ? track.tags : [track.emotion || 'neutral'],
  })
}

const togglePlayback = async () => {
  if (!currentTrack.value?.id) return

  if (!playerStore.isTrackLoaded(currentTrack.value.id)) {
    progressSeconds.value = 0
    await loadAudioForTrack(currentTrack.value, true)
    return
  }

  if (isPlaying.value) {
    playerStore.pause()
    return
  }

  try {
    await playerStore.play()
  } catch (error) {
    ElMessage.error('无法继续播放当前音频。')
    console.error('Resume service playback failed:', error)
  }
}

const playNext = async () => {
  const trackPool = filteredTracks.value.length ? filteredTracks.value : activeCategory.value.tracks || []
  if (!trackPool.length) return
  const currentIndex = trackPool.findIndex((track) => track.id === currentTrack.value.id)
  const nextIndex = currentIndex >= 0 ? (currentIndex + 1) % trackPool.length : 0
  await selectTrack(trackPool[nextIndex])
}

const playPrevious = async () => {
  const trackPool = filteredTracks.value.length ? filteredTracks.value : activeCategory.value.tracks || []
  if (!trackPool.length) return
  const currentIndex = trackPool.findIndex((track) => track.id === currentTrack.value.id)
  const prevIndex = currentIndex > 0 ? currentIndex - 1 : trackPool.length - 1
  await selectTrack(trackPool[prevIndex])
}

const getTrackTags = (track, categoryId = activeCategory.value?.id) => {
  if (Array.isArray(track?.tags) && track.tags.length) {
    return track.tags
  }

  if (categoryId && defaultCategoryMoodMap[categoryId]) {
    return defaultCategoryMoodMap[categoryId]
  }

  return ['真实音乐', '舒缓']
}

const normalizePlayerTrack = (track) => ({
  id: track.id,
  musicResourceId: track.musicResourceId ?? null,
  title: track.title,
  artist: track.artist?.trim() || '佚名',
  duration: track.duration || 0,
  cover: track.cover || getRealMusicCover(track.emotion || 'neutral'),
  tags: getTrackTags(track),
  type: track.file ? '本地上传' : '真实音乐',
  emotion: track.emotion || 'neutral',
  filename: track.filename,
  fileUrl: track.fileUrl || '',
})

const openPlayerPage = () => {
  if (!currentTrack.value?.id) return

  const trackPool = filteredTracks.value.length ? filteredTracks.value : activeCategory.value.tracks || []
  const normalizedCurrentTrack = normalizePlayerTrack(currentTrack.value)
  const queuePayload = trackPool.map((item) => normalizePlayerTrack(item))
  if (!queuePayload.some((item) => item.id === normalizedCurrentTrack.id)) {
    queuePayload.unshift(normalizedCurrentTrack)
  }

  playerStore.setCurrentTrack(normalizedCurrentTrack)
  playerStore.setQueue(queuePayload)
  playerStore.setPlaybackContext({
    source: 'service',
    returnTo: '/service',
    categoryId: activeCategory.value?.id || 'recommend',
    categoryName: activeCategory.value?.name || '音乐空间',
  })

  window.sessionStorage.setItem(PLAYER_SESSION_KEY, JSON.stringify({
    source: 'service',
    returnTo: '/service',
    categoryId: activeCategory.value?.id || 'recommend',
    categoryName: activeCategory.value?.name || '音乐空间',
    track: normalizedCurrentTrack,
    queue: queuePayload,
  }))

  router.push({ name: 'music-player' })
}

const openAllPlaylists = () => {
  router.push({
    name: 'all-playlists',
    query: {
      playlist: activeCategory.value?.id || 'recommend',
    },
  })
}

const handleLogout = async () => {
  if (isLoggingOut.value) return

  try {
    isLoggingOut.value = true
    await logoutSession()
    ElMessage.success('已退出登录')
    router.push({ name: 'login' })
  } catch (error) {
    console.error('Logout failed:', error)
    ElMessage.error('退出登录失败，请稍后重试')
  } finally {
    isLoggingOut.value = false
  }
}

const timeStamp = () =>
  new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })

const pushAssistantMessage = (content, emotions = null, toolResults = null) => {
  messages.value.push({
    id: `assistant-${Date.now()}-${Math.random()}`,
    role: 'assistant',
    content,
    timestamp: timeStamp(),
    emotions,
    toolResults,
  })
}

const pushUserMessage = (content) => {
  const msgId = `user-${Date.now()}-${Math.random()}`
  messages.value.push({
    id: msgId,
    role: 'user',
    content,
    timestamp: timeStamp(),
  })
  return msgId
}

const updateUserMessage = (msgId, content) => {
  const msg = messages.value.find(m => m.id === msgId)
  if (msg) {
    msg.content = content
  }
}

const playAssistantSpeech = async (audioUrl) => {
  if (!audioUrl) return

  const sourceUrl = getAiAssetUrl(audioUrl)

  try {
    assistantSpeechPlayer.pause()
    assistantSpeechPlayer.src = sourceUrl
    assistantSpeechPlayer.currentTime = 0
    await assistantSpeechPlayer.play()
  } catch (error) {
    console.error('AI speech playback failed:', error)
  }
}

const getCompanionSessionId = () => {
  const storageKey = 'companion_session_id'
  let sid = localStorage.getItem(storageKey)
  if (!sid) {
    sid = `sess_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`
    localStorage.setItem(storageKey, sid)
  }
  return sid
}

const getCurrentUserId = () => {
  const candidates = [
    localStorage.getItem('userId'),
    localStorage.getItem('user_id'),
    localStorage.getItem('uid'),
  ]
  for (const item of candidates) {
    const n = Number(item)
    if (Number.isInteger(n) && n > 0) {
      return n
    }
  }
  return 10001
}

const createAssistantPlaceholder = () => {
  const id = `assistant-${Date.now()}-${Math.random()}`
  const msg = {
    id,
    role: 'assistant',
    content: '正在思考中...',
    timestamp: timeStamp(),
    emotions: null,
    toolResults: null,
  }
  messages.value.push(msg)
  return id
}

const updateAssistantMessage = (msgId, content, emotions = null, toolResults = undefined) => {
  const msg = messages.value.find(m => m.id === msgId)
  if (!msg) return
  msg.content = content
  if (emotions) {
    msg.emotions = emotions
  }
  if (toolResults !== undefined) {
    msg.toolResults = toolResults
  }
}

const readStreamAsText = async (streamResponse, assistantMsgId) => {
  const reader = streamResponse.body.getReader()
  const decoder = new TextDecoder('utf-8')
  let buffer = ''
  let collectedText = ''
  let finalDone = null

  while (true) {
    const { done, value } = await reader.read()
    if (done) break

    buffer += decoder.decode(value, { stream: true })
    const chunks = buffer.split('\n\n')
    buffer = chunks.pop() || ''

    for (const chunk of chunks) {
      const line = chunk.split('\n').find(l => l.startsWith('data: '))
      if (!line) continue

      const raw = line.slice(6)
      if (!raw) continue

      let payload
      try {
        payload = JSON.parse(raw)
      } catch {
        continue
      }

      if (payload.type === 'delta') {
        collectedText += payload.text || ''
        updateAssistantMessage(assistantMsgId, collectedText || '正在思考中...')
      } else if (payload.type === 'done') {
        finalDone = payload
      } else if (payload.type === 'error') {
        throw new Error(payload.error || 'stream error')
      }
    }
  }

  return { collectedText, finalDone }
}

const askCompanion = async (transcript, audioBlob = null, userMsgId = null) => {
  try {
    const userId = getCurrentUserId()
    const sessionId = getCompanionSessionId()

    let finalTranscript = (transcript || '').trim()
    let detectedEmotion = null
    let emotionDetails = null

    // 阶段1：语音先做识别与情绪分析
    if (audioBlob) {
      const formData = new FormData()
      formData.append('audio', audioBlob, 'recording.wav')
      if (finalTranscript) {
        formData.append('transcript', finalTranscript)
      }

      const analyzeResp = await analyzeCompanionAudioApi(formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
        timeout: 180000,
      })

      const analyzeData = analyzeResp.data || {}
      finalTranscript = (analyzeData.transcript || finalTranscript).trim()
      detectedEmotion = analyzeData.detected_emotion || null
      emotionDetails = analyzeData.emotion_details || null

      if (!finalTranscript) {
        throw new Error('No transcript from audio analyze')
      }

      if (userMsgId) {
        updateUserMessage(userMsgId, finalTranscript)
      } else {
        pushUserMessage(finalTranscript)
      }
      heardText.value = `已识别：${finalTranscript}`
    }

    // 阶段2：普通聊天，支持后端 LLM tool calling
    const assistantMsgId = createAssistantPlaceholder()
    const chatResp = await askCompanionApi({
      userId,
      sessionId,
      text: finalTranscript,
      detected_emotion: detectedEmotion,
      tts_voice: selectedTtsVoice.value,
    }, { timeout: 180000 })

    const chatData = chatResp.data || {}
    const replyText = chatData.reply || '我在这里。'
    const llmEmotion = chatData.emotion || null
    const toolResults = chatData.tool_results || []

    const emotions = {
      speech: detectedEmotion,
      llm: llmEmotion,
      complex: emotionDetails?.complex_emotion || null,
    }
    updateAssistantMessage(assistantMsgId, replyText, emotions, toolResults)
    await playAssistantSpeech(chatData.audio_url)
  } catch (error) {
    console.error(error)
    pushAssistantMessage('我刚刚有点走神了，暂时没接到你的情绪信号。你可以再说一遍，我会认真听。')
    ElMessage.error('陪伴接口调用失败，请检查后端服务与 API Key。')
  }
}

const stopVoiceRecognition = () => {
  console.log('停止录音和语音识别')

  // 停止语音识别
  if (recognition.value) {
    try {
      recognition.value.stop()
    } catch (e) {
      console.warn('停止语音识别失败:', e)
    }
  }

  // 停止录音
  if (mediaRecorder.value && mediaRecorder.value.state !== 'inactive') {
    console.log('停止 MediaRecorder，状态:', mediaRecorder.value.state)
    mediaRecorder.value.stop()
  }
  // 注意：isListening 会在 mediaRecorder.onstop 中设置为 false
}

const setupMediaRecorder = async () => {
  try {
    const stream = await navigator.mediaDevices.getUserMedia({ audio: true })
    const recorder = new MediaRecorder(stream)

    recorder.ondataavailable = (event) => {
      if (event.data.size > 0) {
        audioChunks.value.push(event.data)
      }
    }

    recorder.onstop = async () => {
      console.log('录音停止，开始处理音频...')
      // 合并音频块
      const audioBlob = new Blob(audioChunks.value, { type: recorder.mimeType })
      audioChunks.value = []
      console.log('音频块大小:', audioBlob.size)

      // 停止所有音频轨道
      stream.getTracks().forEach(track => track.stop())

      // 转换为 WAV 格式
      if (audioBlob.size > 0) {
        heardText.value = '正在处理音频...'

        // 先添加用户消息占位符
        const userText = finalTranscript.value.trim() || '正在识别中...'
        const userMsgId = pushUserMessage(userText)

        try {
          const wavBlob = await convertToWav(audioBlob)
          console.log('WAV 转换完成，大小:', wavBlob.size)

          console.log('准备调用 askCompanion...')
          // 传递浏览器识别的文本和用户消息ID
          await askCompanion(finalTranscript.value.trim(), wavBlob, userMsgId)
          console.log('askCompanion 调用完成')
        } catch (error) {
          console.error('音频转换失败:', error)
          ElMessage.error('音频处理失败，请重试')
        }
      } else {
        console.log('音频块为空，跳过处理')
      }

      // 重置状态
      audioCaptureActive.value = false
      isListening.value = false
      finalTranscript.value = ''
      interimTranscript.value = ''
    }

    mediaRecorder.value = recorder
    return true
  } catch (error) {
    console.error('无法访问麦克风:', error)
    ElMessage.error('无法访问麦克风，请检查浏览器权限。')
    return false
  }
}

const convertToWav = async (blob) => {
  const audioContext = new (window.AudioContext || window.webkitAudioContext)()
  const arrayBuffer = await blob.arrayBuffer()
  const audioBuffer = await audioContext.decodeAudioData(arrayBuffer)

  // 转换为 WAV 格式
  const wavBuffer = audioBufferToWav(audioBuffer)
  return new Blob([wavBuffer], { type: 'audio/wav' })
}

const audioBufferToWav = (buffer) => {
  const numOfChan = buffer.numberOfChannels
  const length = buffer.length * numOfChan * 2 + 44
  const bufferArray = new ArrayBuffer(length)
  const view = new DataView(bufferArray)
  const channels = []
  let pos = 0

  // 写入 WAV 文件头
  setUint32(0x46464952) // "RIFF"
  setUint32(length - 8) // file length - 8
  setUint32(0x45564157) // "WAVE"

  setUint32(0x20746d66) // "fmt " chunk
  setUint32(16) // length = 16
  setUint16(1) // PCM (uncompressed)
  setUint16(numOfChan)
  setUint32(buffer.sampleRate)
  setUint32(buffer.sampleRate * 2 * numOfChan) // avg. bytes/sec
  setUint16(numOfChan * 2) // block-align
  setUint16(16) // 16-bit

  setUint32(0x61746164) // "data" - chunk
  setUint32(length - pos - 4) // chunk length

  // 写入音频数据
  for (let i = 0; i < buffer.numberOfChannels; i++) {
    channels.push(buffer.getChannelData(i))
  }

  // 交错写入所有声道的样本
  for (let i = 0; i < buffer.length; i++) {
    for (let channel = 0; channel < numOfChan; channel++) {
      const sample = Math.max(-1, Math.min(1, channels[channel][i]))
      view.setInt16(pos, sample < 0 ? sample * 0x8000 : sample * 0x7FFF, true)
      pos += 2
    }
  }

  return bufferArray

  function setUint16(data) {
    view.setUint16(pos, data, true)
    pos += 2
  }

  function setUint32(data) {
    view.setUint32(pos, data, true)
    pos += 4
  }
}

const setupSpeechRecognition = () => {
  if (!speechRecognitionSupported) {
    console.log('浏览器不支持语音识别')
    return false
  }

  try {
    const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition
    const recognitionInstance = new SpeechRecognition()

    recognitionInstance.continuous = true
    recognitionInstance.interimResults = true
    recognitionInstance.lang = 'zh-CN'

    recognitionInstance.onstart = () => {
      console.log('语音识别已启动')
      finalTranscript.value = ''
      interimTranscript.value = ''
    }

    recognitionInstance.onresult = (event) => {
      let interim = ''
      let final = ''

      for (let i = event.resultIndex; i < event.results.length; i++) {
        const transcript = event.results[i][0].transcript
        if (event.results[i].isFinal) {
          final += transcript
        } else {
          interim += transcript
        }
      }

      if (final) {
        finalTranscript.value += final
        console.log('最终识别结果:', finalTranscript.value)
      }

      interimTranscript.value = interim

      // 更新显示文本
      const displayText = finalTranscript.value + (interim ? ` ${interim}` : '')
      if (displayText) {
        heardText.value = `正在识别：${displayText}`
      }
    }

    recognitionInstance.onerror = (event) => {
      console.error('语音识别错误:', event.error)

      // 处理不同类型的错误
      if (event.error === 'network') {
        console.warn('语音识别网络错误（这是正常的，Chrome的语音识别需要网络）')
        // 不显示错误消息，因为录音仍然可以工作
      } else if (event.error === 'no-speech') {
        console.log('未检测到语音')
      } else if (event.error === 'aborted') {
        console.log('语音识别被中止')
      } else {
        console.error('其他语音识别错误:', event.error)
      }
    }

    recognitionInstance.onend = () => {
      console.log('语音识别已结束')
    }

    recognition.value = recognitionInstance
    return true
  } catch (error) {
    console.error('初始化语音识别失败:', error)
    return false
  }
}

const toggleVoiceInput = async () => {
  if (!voiceSupported) {
    ElMessage.warning('当前浏览器暂不支持录音。')
    pushAssistantMessage('这个浏览器暂时不能录音，可以换用 Chrome 或 Edge 再试一次。')
    return
  }

  if (isListening.value) {
    stopVoiceRecognition()
    return
  }

  // 每次录音前重新初始化 MediaRecorder（因为 stream 在上次录音结束时已关闭）
  const success = await setupMediaRecorder()
  if (!success) {
    return
  }

  // 尝试启动语音识别（可选功能，失败不影响录音）
  if (speechRecognitionSupported) {
    const recognitionReady = setupSpeechRecognition()
    if (recognitionReady && recognition.value) {
      try {
        recognition.value.start()
        console.log('语音识别已启动')
      } catch (error) {
        console.warn('启动语音识别失败（不影响录音）:', error)
      }
    }
  }

  // 开始录音
  audioChunks.value = []
  mediaRecorder.value.start()
  audioCaptureActive.value = true
  isListening.value = true
  heardText.value = '正在录音，请开始说话...'
}

watch(activeCategoryId, () => {
  ensureCurrentTrack()
})

watch(searchText, () => {
  ensureCurrentTrack()
})

watch(
  () => musicStore.customPlaylists,
  () => {
    if (!musicCategories.value.some((category) => category.id === activeCategoryId.value)) {
      activeCategoryId.value = musicCategories.value[0]?.id || 'recommend'
    }
    ensureCurrentTrack()
  },
  { deep: true },
)

watch(
  () => [musicStore.likedTrackIds, musicStore.collectedTrackIds],
  () => {
    ensureCurrentTrack()
  },
  { deep: true },
)

watch(
  () => speechStore.emotion,
  async (emotion) => {
    const nextEmotion = emotion || currentEmotionForRecommend.value
    await musicStore.fetchRecommendedTracks(nextEmotion)
    if (activeCategoryId.value === 'recommend' || activeCategoryId.value === 'guess') {
      ensureCurrentTrack()
    }
  },
)

watch(
  musicCategories,
  () => {
    syncCurrentTrackSnapshot()
  },
  { deep: true },
)

watch(volume, (newVol) => {
  playerStore.setVolumeValue(newVol)
})

watch(progressSeconds, (value) => {
  if (!playerStore.hasActiveSource()) return
  const safeMax = currentTrack.value.duration || 0
  if (value > safeMax) {
    progressSeconds.value = safeMax
    return
  }
  playerStore.seekTo(value)
})

watch(
  () => route.query.panel,
  (panel) => {
    if (panel === 'meditation-room') {
      pushAssistantMessage('冥想室页面会在下一阶段展开，这里先为你保留了入口。')
    }
    if (panel === 'personal-space') {
      pushAssistantMessage('个人空间页面会在下一阶段展开，这里先为你保留了入口。')
    }
  },
  { immediate: true },
)

onMounted(async () => {
  setupSpeechRecognition()
  playerStore.setEndedHandler(playNext)
  await musicStore.fetchUserData()
  await loadMusicLibrary()
})

onBeforeUnmount(() => {
  playerStore.setEndedHandler(null)
  stopVoiceRecognition()
  assistantSpeechPlayer.pause()
  assistantSpeechPlayer.src = ''
})
</script>

<style scoped>
.service-page {
  position: relative;
  height: 100vh;
  overflow: hidden;
  padding: 20px;
  display: flex;
  flex-direction: column;
}

.blur-orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  pointer-events: none;
  animation: floatGentle 15s ease-in-out infinite alternate;
  z-index: 0;
}

.orb-coral {
  top: -10%;
  right: -5%;
  width: 500px;
  height: 500px;
  background: var(--color-accent-blush);
  opacity: 0.6;
}

.orb-leaf {
  left: -10%;
  bottom: -10%;
  width: 600px;
  height: 600px;
  background: var(--color-accent-sage);
  opacity: 0.5;
  animation-delay: -5s;
}

.orb-sky {
  left: 40%;
  top: 30%;
  width: 400px;
  height: 400px;
  background: var(--color-accent-sky);
  opacity: 0.4;
  animation-delay: -2s;
}

.service-shell {
  position: relative;
  z-index: 1;
  width: 100%;
  height: 100%;
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) minmax(400px, 0.8fr);
  gap: 20px;
}

.surface {
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-float);
  border: 1px solid rgba(255, 255, 255, 0.6);
  backdrop-filter: blur(20px);
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.warm-surface {
  padding: 30px;
  background: rgba(249, 248, 246, 0.75);
}

.cool-surface {
  padding: 30px;
  background: rgba(240, 239, 234, 0.75);
}

.panel-top-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  flex-shrink: 0;
}

.top-bar-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.portal-actions {
  display: flex;
  gap: 16px;
}

.portal-btn {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 24px;
  background: rgba(255, 255, 255, 0.6);
  border: 1px solid rgba(44, 48, 46, 0.1);
  border-radius: var(--radius-pill);
  font-weight: 600;
  font-size: 1.1rem;
  color: var(--color-text-primary);
  transition: all var(--transition-fast);
}

.portal-btn:hover {
  background: #fff;
  transform: translateY(-2px);
  box-shadow: var(--shadow-soft);
}

.portal-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  background: var(--color-bg-primary);
  border-radius: 50%;
  font-size: 16px;
  box-shadow: inset 0 2px 4px rgba(0,0,0,0.05);
}

.panel-head,
.top-tools,
.section-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  flex-shrink: 0;
}

.kicker {
  margin: 0 0 10px 0;
  font-size: 0.85rem;
  letter-spacing: 0.15em;
  text-transform: uppercase;
  color: var(--color-accent-terracotta);
  font-weight: 600;
}

.panel-head h1,
.panel-head h2,
.section-head h2 {
  margin: 0;
  font-family: var(--font-serif);
  font-weight: 500;
  line-height: 1.2;
  color: var(--color-text-primary);
}

.panel-head h1 {
  font-size: 2.2rem;
}

.panel-head h2 {
  font-size: 1.8rem;
}

.lead {
  max-width: 620px;
  margin: 12px 0 0;
  font-size: 1rem;
}

.lead,
.section-head p,
.track-meta span,
.now-playing span,
.track-time,
.heard-text,
.message small,
.message-role,
.portal-card span {
  color: var(--color-text-secondary);
}

.back-link,
.secondary-link-btn,
.catalog-link-btn,
.mood-chip,
.count-pill,
.status-pill {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 40px;
  padding: 0 20px;
  border-radius: var(--radius-pill);
  font-size: 0.9rem;
}

.back-link {
  background: var(--color-text-primary);
  color: var(--color-bg-primary);
  font-weight: 500;
  transition: all var(--transition-fast);
}
.back-link:hover {
  background: var(--color-text-secondary);
}

.secondary-link-btn {
  border: 1px solid rgba(44, 48, 46, 0.12);
  background: rgba(255, 255, 255, 0.72);
  color: var(--color-text-primary);
  font-weight: 600;
  transition: all var(--transition-fast);
}

.secondary-link-btn:hover:not(:disabled) {
  background: #fff;
  transform: translateY(-1px);
}

.secondary-link-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.catalog-link-btn {
  background: rgba(255, 255, 255, 0.78);
  color: var(--color-text-primary);
  border: 1px solid rgba(44, 48, 46, 0.1);
  font-weight: 600;
  transition: all var(--transition-fast);
}

.catalog-link-btn:hover {
  background: #fff;
  transform: translateY(-2px);
  box-shadow: var(--shadow-soft);
}

.mood-chip,
.count-pill {
  background: rgba(255, 255, 255, 0.6);
  color: var(--color-text-primary);
  border: 1px solid rgba(44, 48, 46, 0.1);
}

.status-pill {
  background: var(--color-accent-sage);
  color: #fff;
  font-weight: 500;
}

.status-pill.listening {
  background: var(--color-accent-terracotta);
}

.top-tools {
  margin-top: 24px;
  align-items: flex-end;
  flex-shrink: 0;
}

.search-field {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.search-input-wrapper {
  display: flex;
  align-items: center;
  gap: 12px;
}

.search-field span,
.volume-control span {
  font-weight: 500;
  color: var(--color-text-primary);
}

.search-field input {
  flex: 1;
  height: 52px;
  padding: 0 20px;
  border-radius: var(--radius-pill);
  border: 1px solid rgba(44, 48, 46, 0.1);
  background: rgba(255, 255, 255, 0.6);
  outline: none;
  font-size: 1rem;
  transition: all var(--transition-medium);
}

.search-btn {
  height: 52px;
  padding: 0 32px;
  border-radius: var(--radius-pill);
  background: var(--color-text-primary);
  color: #fff;
  font-weight: 500;
  border: none;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.search-btn:hover {
  background: var(--color-text-secondary);
  transform: translateY(-1px);
}

.search-field input:focus {
  background: #fff;
  border-color: var(--color-accent-terracotta);
  box-shadow: 0 0 0 4px rgba(200, 138, 117, 0.1);
}

.category-grid {
  display: flex;
  overflow-x: auto;
  gap: 16px;
  margin-top: 8px; /* Adjusted to balance the top padding */
  flex-shrink: 0;
  padding: 16px 8px 24px 8px; /* Added padding to prevent box-shadow and hover transforms from being clipped */
  -ms-overflow-style: none; /* IE and Edge */
  scrollbar-width: none; /* Firefox */
  scroll-snap-type: x mandatory;
}

.category-grid::-webkit-scrollbar {
  display: none; /* Chrome, Safari and Opera */
}

.category-card {
  flex: 0 0 auto;
  width: 200px;
  height: 100px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: flex-start;
  padding: 18px 20px;
  background: rgba(255, 255, 255, 0.4);
  border: 2px solid rgba(255, 255, 255, 0.5); /* Thicker default border to avoid layout shift when selected */
  border-radius: var(--radius-xl);
  text-align: left;
  transition: all 0.4s cubic-bezier(0.175, 0.885, 0.32, 1.275);
  scroll-snap-align: center;
  backdrop-filter: blur(10px);
  color: var(--color-text-primary);
  cursor: pointer;
}

.category-card:hover {
  background: rgba(255, 255, 255, 0.8);
  transform: translateY(-4px) scale(1.02);
  box-shadow: 0 15px 30px rgba(44, 48, 46, 0.1);
  border-color: rgba(255, 255, 255, 0.8);
}

.category-card.active {
  background: #fff;
  border-color: var(--color-accent-terracotta);
  color: var(--color-text-primary);
  box-shadow: 0 10px 25px rgba(200, 138, 117, 0.2);
  transform: translateY(-2px) scale(1.01);
}

.category-card strong {
  font-size: 1.1rem;
  font-weight: 600;
  font-family: var(--font-serif);
}

.category-card span {
  font-size: 0.8rem;
  margin-top: 6px;
  color: var(--color-text-secondary);
  opacity: 0.8;
  transition: all 0.3s;
}

.category-card.active strong {
  color: var(--color-accent-terracotta);
}

.category-card.active span {
  color: var(--color-accent-terracotta);
  opacity: 1;
}

.track-section {
  margin-top: 10px;
  background: rgba(255, 255, 255, 0.6);
  border-radius: var(--radius-xl);
  padding: 16px 24px;
  flex: 1;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
}

.section-head h2 {
  font-size: 1.4rem;
}

.track-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 16px;
  flex: 1;
}

.track-card {
  display: grid;
  grid-template-columns: 50px 1fr auto;
  align-items: center;
  gap: 16px;
  padding: 16px;
  border-radius: var(--radius-md);
  background: transparent;
  transition: all var(--transition-fast);
}

.track-card:hover {
  background: rgba(255, 255, 255, 0.6);
}

.track-card.current {
  background: #fff;
  box-shadow: var(--shadow-soft);
}

.track-card img {
  width: 50px;
  height: 50px;
  border-radius: 12px;
  object-fit: cover;
}

.track-meta {
  display: flex;
  flex-direction: column;
  text-align: left;
}

.track-meta strong {
  font-weight: 600;
  color: var(--color-text-primary);
}

.player-shell {
  margin-top: 16px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 12px 20px;
  background: var(--color-text-primary);
  color: var(--color-bg-primary);
  border-radius: var(--radius-pill);
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.player-shell:hover {
  transform: translateY(-2px);
  box-shadow: 0 18px 32px rgba(32, 43, 40, 0.22);
}

.now-playing {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 160px;
}

.now-playing img {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  object-fit: cover;
  animation: spin 10s linear infinite;
}

.now-playing div {
  display: flex;
  flex-direction: column;
}

.now-playing strong {
  font-weight: 600;
  font-size: 0.95rem;
}

.now-playing span {
  font-size: 0.8rem;
  color: rgba(255,255,255,0.7);
}

.player-center {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.player-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.action-button,
.primary-button {
  border-radius: var(--radius-pill);
  font-weight: 500;
  transition: all var(--transition-fast);
}

.action-button {
  padding: 6px 12px;
  font-size: 0.9rem;
  color: rgba(255,255,255,0.8);
}
.action-button:hover {
  color: #fff;
  background: rgba(255,255,255,0.1);
}

.primary-button {
  padding: 8px 24px;
  font-size: 0.95rem;
  background: #fff;
  color: var(--color-text-primary);
}
.primary-button:hover {
  transform: scale(1.05);
}

.progress-row {
  display: flex;
  align-items: center;
  gap: 16px;
  width: 100%;
  max-width: 400px;
  font-size: 0.85rem;
  color: rgba(255,255,255,0.7);
}

.progress-row input {
  flex: 1;
  accent-color: #fff;
}

.volume-control {
  display: flex;
  align-items: center;
  gap: 12px;
}

.volume-control span {
  color: rgba(255,255,255,0.8);
}

/* Companion Panel specific */
.chat-stream {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 20px;
  margin: 30px 0;
  overflow-y: auto;
  padding-right: 10px;
}

.message-card {
  display: flex;
  flex-direction: column;
  padding: 20px;
  border-radius: var(--radius-lg);
  max-width: 85%;
  animation: fadeIn 0.4s ease;
}

.message-card.assistant {
  align-self: flex-start;
  background: rgba(255, 255, 255, 0.8);
  border-bottom-left-radius: 4px;
}

.message-card.user {
  align-self: flex-end;
  background: var(--color-accent-sage);
  color: #fff;
  border-bottom-right-radius: 4px;
}

.message-card.user .message-role,
.message-card.user small {
  color: rgba(255,255,255,0.8);
}

.message-role {
  font-size: 0.8rem;
  font-weight: 600;
  margin-bottom: 8px;
}

.message-card p {
  margin: 0;
  font-size: 1.05rem;
}

.message-card small {
  margin-top: 12px;
  font-size: 0.75rem;
  align-self: flex-end;
}

.emotion-tags {
  display: flex;
  gap: 8px;
  margin-top: 12px;
  flex-wrap: wrap;
}

.emotion-tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 12px;
  border-radius: var(--radius-pill);
  font-size: 0.8rem;
  font-weight: 500;
  background: rgba(255, 255, 255, 0.3);
  color: var(--color-text-primary);
}

.emotion-tag.speech {
  background: rgba(200, 138, 117, 0.2);
  color: var(--color-accent-terracotta);
}

.emotion-tag.complex {
  background: rgba(139, 166, 140, 0.2);
  color: var(--color-accent-sage);
}

.tool-results {
  margin-top: 14px;
  display: grid;
  gap: 10px;
}

.music-recommendations {
  display: grid;
  gap: 10px;
}

.recommend-track-card {
  display: grid;
  grid-template-columns: 46px 1fr;
  align-items: center;
  gap: 12px;
  width: 100%;
  padding: 10px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid rgba(44, 48, 46, 0.08);
  text-align: left;
  color: var(--color-text-primary);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.recommend-track-card:hover {
  transform: translateY(-1px);
  box-shadow: var(--shadow-soft);
}

.recommend-track-card img {
  width: 46px;
  height: 46px;
  border-radius: 8px;
  object-fit: cover;
}

.recommend-track-card div {
  min-width: 0;
  display: grid;
  gap: 4px;
}

.recommend-track-card strong,
.recommend-track-card span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.recommend-track-card span {
  color: var(--color-text-secondary);
  font-size: 0.82rem;
}

.voice-box {
  margin-top: auto;
  display: flex;
  flex-direction: column;
  gap: 16px;
  flex-shrink: 0;
}

.voice-box .primary-button {
  background: var(--color-text-primary);
  color: var(--color-bg-primary);
  padding: 16px;
  font-size: 1.1rem;
}

.voice-selector {
  display: grid;
  gap: 8px;
  color: var(--color-text-secondary);
  font-size: 0.9rem;
  font-weight: 600;
}

.voice-selector select {
  width: 100%;
  border: 1px solid rgba(92, 78, 61, 0.16);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.76);
  color: var(--color-text-primary);
  padding: 12px 14px;
  font: inherit;
  outline: none;
}

.voice-selector select:focus {
  border-color: var(--color-accent-sage);
  box-shadow: 0 0 0 3px rgba(139, 166, 140, 0.18);
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

@media (max-width: 1024px) {
  .service-page {
    height: auto;
    min-height: 100vh;
    overflow: auto;
  }
  .service-shell {
    grid-template-columns: 1fr;
    height: auto;
  }
  .surface {
    height: auto;
    overflow: visible;
  }
  .track-section, .chat-stream {
    overflow-y: visible;
  }
  .player-shell {
    flex-direction: column;
    border-radius: var(--radius-xl);
  }
  .volume-control {
    display: none;
  }

  .panel-head {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
