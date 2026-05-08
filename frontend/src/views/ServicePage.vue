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
          </div>
          <RouterLink class="back-link" to="/">回首页</RouterLink>
        </div>

        <div class="panel-head">
          <div>
            <h1>今晚你想走进哪一种声场？</h1>
            <p class="lead">
              不同音乐区像花园里的不同小径，有的更安睡，有的更专注......
            </p>
          </div>
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
              <p>{{ activeCategory.description }}</p>
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
            <small>{{ message.timestamp }}</small>
          </article>
        </div>

        <div class="voice-box">
          <button class="primary-button full" type="button" @click="toggleVoiceInput">
            <span>{{ isListening ? '结束语音输入' : '开始语音输入' }}</span>
          </button>
          <p class="heard-text">
            {{ heardText || '点击后可直接说话；当前会先用浏览器识别语音，再调用后端进行情绪分析。' }}
          </p>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { initialMessages, mockReplies } from '@/data/mockContent'
import { analyzeEmotionApi, getMusicFileByNameApi, getMusicFileUrl, getMusicListApi } from '@/api/python'
import { useMusicStore } from '@/stores/musicStore'
import { useSpeechStore } from '@/stores/speech'
import { buildRealMusicCategories, getRealMusicCover, readRemoteAudioDuration } from '@/utils/realMusic'

const router = useRouter()
const route = useRoute()
const musicStore = useMusicStore()
const speechStore = useSpeechStore()
const PLAYER_SESSION_KEY = 'emotion-system-active-player'

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
const volume = ref(68)
const isPlaying = ref(false)
const progressSeconds = ref(0)
const messages = ref([...initialMessages])
const heardText = ref('')
const isListening = ref(false)
const recognition = ref(null)
const categoryScrollRef = ref(null)
const realMusicFiles = ref([])
const realMusicDurations = ref({})
const currentTrack = ref({ ...emptyTrack })
const isLoadingAudio = ref(false)

const audioPlayer = new Audio()
let currentObjectUrl = null
let lastLoadedTrackId = ''

const voiceSupported = typeof window !== 'undefined'
  ? Boolean(window.SpeechRecognition || window.webkitSpeechRecognition)
  : false

const musicCategories = computed(() => {
  const realCategories = buildRealMusicCategories(realMusicFiles.value, {
    likedIds: musicStore.likedTrackIds,
    collectedIds: musicStore.collectedTrackIds,
    durationMap: realMusicDurations.value,
  })
  const customCats = musicStore.customPlaylists.map((playlist) => ({
    id: playlist.id,
    name: playlist.name,
    description: playlist.description || '自建歌单',
    tracks: playlist.tracks,
  }))
  return [...realCategories, ...customCats]
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

const cleanupObjectUrl = () => {
  if (currentObjectUrl) {
    URL.revokeObjectURL(currentObjectUrl)
    currentObjectUrl = null
  }
}

const resetAudioState = () => {
  audioPlayer.pause()
  audioPlayer.src = ''
  progressSeconds.value = 0
  isPlaying.value = false
  lastLoadedTrackId = ''
  cleanupObjectUrl()
}

const ensureCurrentTrack = () => {
  const availableTracks = filteredTracks.value.length ? filteredTracks.value : activeCategory.value.tracks || []
  if (!availableTracks.length) {
    currentTrack.value = { ...emptyTrack }
    resetAudioState()
    return
  }

  const currentExists = availableTracks.some((track) => track.id === currentTrack.value.id)
  if (!currentExists) {
    currentTrack.value = { ...availableTracks[0] }
    progressSeconds.value = 0
    resetAudioState()
  }
}

const loadRealMusicLibrary = async () => {
  try {
    const response = await getMusicListApi()
    realMusicFiles.value = Array.isArray(response?.data?.music_files) ? response.data.music_files : []
    const durationEntries = await Promise.all(
      realMusicFiles.value.map(async (filename) => [filename, await readRemoteAudioDuration(getMusicFileUrl(filename))]),
    )
    realMusicDurations.value = Object.fromEntries(durationEntries)
    if (!musicCategories.value.some((category) => category.id === activeCategoryId.value)) {
      activeCategoryId.value = musicCategories.value[0]?.id || 'recommend'
    }
    ensureCurrentTrack()
  } catch (error) {
    realMusicFiles.value = []
    realMusicDurations.value = {}
    currentTrack.value = { ...emptyTrack }
    ElMessage.error('真实音乐目录加载失败，请确认 Python backend 已启动。')
    console.error('Load real music list failed:', error)
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
    isLoadingAudio.value = true
    let sourceBlob = null

    if (track.file) {
      sourceBlob = track.file
    } else if (track.filename) {
      const response = await getMusicFileByNameApi(track.filename)
      sourceBlob = response.data
    }

    if (!sourceBlob) {
      throw new Error('missing audio source')
    }

    cleanupObjectUrl()
    currentObjectUrl = URL.createObjectURL(sourceBlob)
    audioPlayer.src = currentObjectUrl
    audioPlayer.currentTime = progressSeconds.value || 0
    audioPlayer.volume = volume.value / 100
    lastLoadedTrackId = track.id

    if (autoplay) {
      await audioPlayer.play()
      isPlaying.value = true
    }
  } catch (error) {
    isPlaying.value = false
    ElMessage.error('音乐播放失败，请确认 Python backend 已启动。')
    console.error('Service audio playback failed:', error)
  } finally {
    isLoadingAudio.value = false
  }
}

const selectTrack = async (track) => {
  if (!track?.id) return

  if (currentTrack.value.id === track.id && audioPlayer.src) {
    await togglePlayback()
    return
  }

  currentTrack.value = { ...track }
  progressSeconds.value = 0
  await loadAudioForTrack(currentTrack.value, true)
}

const togglePlayback = async () => {
  if (!currentTrack.value?.id) return

  if (!audioPlayer.src || lastLoadedTrackId !== currentTrack.value.id) {
    progressSeconds.value = 0
    await loadAudioForTrack(currentTrack.value, true)
    return
  }

  if (isPlaying.value) {
    audioPlayer.pause()
    isPlaying.value = false
    return
  }

  try {
    await audioPlayer.play()
    isPlaying.value = true
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
  title: track.title,
  artist: track.artist?.trim() || '佚名',
  duration: track.duration || 0,
  cover: track.cover || getRealMusicCover(track.emotion || 'neutral'),
  tags: getTrackTags(track),
  type: track.file ? '本地上传' : '真实音乐',
  emotion: track.emotion || 'neutral',
  filename: track.filename,
})

const openPlayerPage = () => {
  if (!currentTrack.value?.id) return

  const trackPool = filteredTracks.value.length ? filteredTracks.value : activeCategory.value.tracks || []
  const queuePayload = trackPool.map((item) => normalizePlayerTrack(item))

  window.sessionStorage.setItem(PLAYER_SESSION_KEY, JSON.stringify({
    source: 'service',
    returnTo: '/service',
    categoryName: activeCategory.value?.name || '音乐空间',
    track: normalizePlayerTrack(currentTrack.value),
    queue: queuePayload,
  }))

  router.push({ name: 'music-player' })
}

const timeStamp = () =>
  new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })

const pushAssistantMessage = (content) => {
  messages.value.push({
    id: `assistant-${Date.now()}-${Math.random()}`,
    role: 'assistant',
    content,
    timestamp: timeStamp(),
  })
}

const buildEmotionReply = (emotion) => {
  const normalizedEmotion = String(emotion || '').toLowerCase()

  if (normalizedEmotion.includes('joy') || normalizedEmotion.includes('happy')) {
    return `我感受到你现在更接近“${emotion}”。可以顺着这份轻盈感，去听一些更明亮的旋律。`
  }

  if (normalizedEmotion.includes('sad')) {
    return `我感受到你现在更接近“${emotion}”。先不用急着振作，我们可以先去冥想室，让情绪慢慢落下来。`
  }

  if (normalizedEmotion.includes('anger')) {
    return `我感受到你现在更接近“${emotion}”。先放慢呼吸，再选择一些舒缓的声音，通常会更容易稳下来。`
  }

  if (normalizedEmotion.includes('fear') || normalizedEmotion.includes('anxiety')) {
    return `我感受到你现在更接近“${emotion}”。先给自己一点安全感，去冥想室或轻音乐区会更合适。`
  }

  return `我感受到你现在更接近“${emotion}”。如果你愿意，可以继续说下去，我会根据你的状态继续陪你。`
}

const handleTranscript = async (transcript) => {
  heardText.value = `已识别：${transcript}`
  messages.value.push({
    id: `user-${Date.now()}`,
    role: 'user',
    content: transcript,
    timestamp: timeStamp(),
  })

  try {
    const response = await analyzeEmotionApi({ text: transcript })
    const emotion = response?.data?.emotion?.trim()

    if (!emotion) {
      throw new Error('empty emotion')
    }

    speechStore.setEmotion(emotion)
    window.localStorage.setItem('currentEmotion', emotion)
    heardText.value = `已识别：${transcript} | 当前情绪：${emotion}`
    pushAssistantMessage(buildEmotionReply(emotion))
    return
  } catch (error) {
    const reply = mockReplies[Math.floor(Math.random() * mockReplies.length)]
    pushAssistantMessage(reply)
    ElMessage.warning('情绪分析暂时不可用，已切换为本地陪伴回复。')
    console.error('Emotion analysis failed:', error)
  }
}

const stopVoiceRecognition = () => {
  if (recognition.value && isListening.value) {
    recognition.value.stop()
  }
  isListening.value = false
}

const setupVoiceRecognition = () => {
  if (!voiceSupported) {
    return
  }

  const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition
  const instance = new SpeechRecognition()
  instance.lang = 'zh-CN'
  instance.interimResults = false
  instance.maxAlternatives = 1

  instance.onstart = () => {
    isListening.value = true
    heardText.value = '正在聆听，请开始说话。'
  }

  instance.onresult = (event) => {
    const transcript = event.results?.[0]?.[0]?.transcript?.trim()
    if (transcript) {
      handleTranscript(transcript)
    }
  }

  instance.onerror = () => {
    isListening.value = false
    heardText.value = '语音识别暂时失败了，可以稍后再试。'
    pushAssistantMessage('刚才的语音没有顺利识别，我们也可以先听会儿音乐，再重新开始。')
  }

  instance.onend = () => {
    isListening.value = false
  }

  recognition.value = instance
}

const toggleVoiceInput = () => {
  if (!voiceSupported) {
    ElMessage.warning('当前浏览器暂不支持语音识别。')
    pushAssistantMessage('这个浏览器还不能直接语音输入，但页面结构已经为后续接口接入预留好了。')
    return
  }

  if (isListening.value) {
    stopVoiceRecognition()
    return
  }

  recognition.value?.start()
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
  musicCategories,
  () => {
    syncCurrentTrackSnapshot()
  },
  { deep: true },
)

watch(volume, (newVol) => {
  audioPlayer.volume = newVol / 100
})

watch(progressSeconds, (value) => {
  if (!audioPlayer.src) return
  const safeMax = Number.isFinite(audioPlayer.duration) ? audioPlayer.duration : currentTrack.value.duration
  if (value > safeMax) {
    progressSeconds.value = safeMax
    return
  }

  if (Math.abs(audioPlayer.currentTime - value) > 1) {
    audioPlayer.currentTime = value
  }
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

audioPlayer.onloadedmetadata = () => {
  const measuredDuration = Number.isFinite(audioPlayer.duration) ? Math.floor(audioPlayer.duration) : 0
  if (measuredDuration > 0) {
    currentTrack.value = {
      ...currentTrack.value,
      duration: measuredDuration,
    }
  }
}

audioPlayer.ontimeupdate = () => {
  progressSeconds.value = audioPlayer.currentTime
}

audioPlayer.onended = () => {
  playNext()
}

onMounted(async () => {
  setupVoiceRecognition()
  await loadRealMusicLibrary()
})

onBeforeUnmount(() => {
  stopVoiceRecognition()
  audioPlayer.pause()
  audioPlayer.src = ''
  cleanupObjectUrl()
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
  font-size: 3rem;
}

.panel-head h2 {
  font-size: 2.2rem;
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

.lead {
  max-width: 620px;
  margin: 16px 0 0;
  font-size: 1.1rem;
}

.back-link,
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
  width: 240px;
  height: 120px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: flex-start;
  padding: 24px;
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
  transform: translateY(-8px) scale(1.02);
  box-shadow: 0 15px 30px rgba(44, 48, 46, 0.1);
  border-color: rgba(255, 255, 255, 0.8);
}

.category-card.active {
  background: #fff;
  border-color: var(--color-accent-terracotta);
  color: var(--color-text-primary);
  box-shadow: 0 10px 25px rgba(200, 138, 117, 0.2);
  transform: translateY(-4px) scale(1.01);
}

.category-card strong {
  font-size: 1.2rem;
  font-weight: 600;
  font-family: var(--font-serif);
}

.category-card span {
  font-size: 0.85rem;
  margin-top: 8px;
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
  margin-top: 30px;
  background: rgba(255, 255, 255, 0.6);
  border-radius: var(--radius-xl);
  padding: 24px;
  flex: 1;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
}

.track-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 24px;
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
}
</style>
