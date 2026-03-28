<template>
  <div class="service-page">
    <div class="blur-orb orb-coral"></div>
    <div class="blur-orb orb-leaf"></div>
    <div class="blur-orb orb-sky"></div>

    <div class="service-shell">
      <section class="music-panel surface warm-surface">
        <div class="panel-head">
          <div>
            <p class="kicker">Sound Garden</p>
            <h1>今晚你想走进哪一种声场？</h1>
            <p class="lead">
              不同音乐区像花园里的不同小径，有的更安睡，有的更专注，有的更适合轻微整理情绪。
            </p>
          </div>
          <RouterLink class="back-link" to="/">回首页</RouterLink>
        </div>

        <div class="top-tools">
          <label class="search-field">
            <span>搜索音乐</span>
            <input v-model.trim="searchText" type="text" placeholder="搜索歌名或创作者" />
          </label>
          <div class="mood-chip">{{ activeCategory.name }}</div>
        </div>

        <div class="category-grid">
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

        <div class="player-shell">
          <div class="now-playing">
            <img :src="currentTrack.cover" :alt="currentTrack.title" />
            <div>
              <strong>{{ currentTrack.title }}</strong>
              <span>{{ currentTrack.artist }}</span>
            </div>
          </div>

          <div class="player-center">
            <div class="player-actions">
              <button class="action-button" type="button" @click="playPrevious">
                <span>上一首</span>
              </button>
              <button class="primary-button" type="button" @click="togglePlayback">
                <span>{{ isPlaying ? '暂停播放' : '开始播放' }}</span>
              </button>
              <button class="action-button" type="button" @click="playNext">
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
              />
              <span>{{ formatTime(currentTrack.duration) }}</span>
            </div>
          </div>

          <label class="volume-control">
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
            {{ heardText || '点击后可直接说话；当前使用浏览器语音识别与前端模拟回复。' }}
          </p>
        </div>

        <div class="portal-grid">
          <button class="portal-card meditation" type="button" @click="goToPlaceholder('meditation-room')">
            <strong>冥想室</strong>
            <span>进入更深一层的沉静空间，继续往内走。</span>
          </button>
          <button class="portal-card personal" type="button" @click="goToPlaceholder('personal-space')">
            <strong>个人空间</strong>
            <span>回看记录、偏好与属于自己的情绪地图。</span>
          </button>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { initialMessages, mockReplies, musicCategories } from '@/data/mockContent'

const router = useRouter()
const route = useRoute()

const activeCategoryId = ref(musicCategories[0].id)
const searchText = ref('')
const volume = ref(68)
const isPlaying = ref(false)
const progressSeconds = ref(0)
const messages = ref([...initialMessages])
const heardText = ref('')
const isListening = ref(false)
const recognition = ref(null)

const activeCategory = computed(
  () => musicCategories.find((category) => category.id === activeCategoryId.value) || musicCategories[0],
)

const filteredTracks = computed(() => {
  const keyword = searchText.value.toLowerCase()
  if (!keyword) {
    return activeCategory.value.tracks
  }

  return activeCategory.value.tracks.filter((track) => {
    return track.title.toLowerCase().includes(keyword) || track.artist.toLowerCase().includes(keyword)
  })
})

const currentTrack = ref(activeCategory.value.tracks[0])
let playTimer = null

const voiceSupported = typeof window !== 'undefined'
  ? Boolean(window.SpeechRecognition || window.webkitSpeechRecognition)
  : false

const formatTime = (seconds) => {
  const safeValue = Number.isFinite(seconds) ? seconds : 0
  const minutes = Math.floor(safeValue / 60)
  const remain = Math.floor(safeValue % 60)
  return `${String(minutes).padStart(2, '0')}:${String(remain).padStart(2, '0')}`
}

const stopTimer = () => {
  if (playTimer) {
    clearInterval(playTimer)
    playTimer = null
  }
}

const startTimer = () => {
  stopTimer()
  playTimer = setInterval(() => {
    if (!isPlaying.value) {
      return
    }

    if (progressSeconds.value >= currentTrack.value.duration) {
      playNext()
      return
    }

    progressSeconds.value += 1
  }, 1000)
}

const selectTrack = (track) => {
  currentTrack.value = track
  progressSeconds.value = 0
  isPlaying.value = true
}

const togglePlayback = () => {
  isPlaying.value = !isPlaying.value
}

const playNext = () => {
  const trackPool = filteredTracks.value.length ? filteredTracks.value : activeCategory.value.tracks
  const currentIndex = trackPool.findIndex((track) => track.id === currentTrack.value.id)
  const nextIndex = currentIndex >= 0 ? (currentIndex + 1) % trackPool.length : 0
  currentTrack.value = trackPool[nextIndex]
  progressSeconds.value = 0
  isPlaying.value = true
}

const playPrevious = () => {
  const trackPool = filteredTracks.value.length ? filteredTracks.value : activeCategory.value.tracks
  const currentIndex = trackPool.findIndex((track) => track.id === currentTrack.value.id)
  const prevIndex = currentIndex > 0 ? currentIndex - 1 : trackPool.length - 1
  currentTrack.value = trackPool[prevIndex]
  progressSeconds.value = 0
  isPlaying.value = true
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

const handleTranscript = (transcript) => {
  heardText.value = `已识别：${transcript}`
  messages.value.push({
    id: `user-${Date.now()}`,
    role: 'user',
    content: transcript,
    timestamp: timeStamp(),
  })

  const reply = mockReplies[Math.floor(Math.random() * mockReplies.length)]
  window.setTimeout(() => {
    pushAssistantMessage(reply)
  }, 500)
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

const goToPlaceholder = (panel) => {
  const target = panel === 'meditation-room' ? '/meditation-room' : '/personal-space'
  router.push(target)
}

watch(activeCategoryId, () => {
  const availableTracks = filteredTracks.value.length ? filteredTracks.value : activeCategory.value.tracks
  currentTrack.value = availableTracks[0]
  progressSeconds.value = 0
  isPlaying.value = false
})

watch(searchText, () => {
  const availableTracks = filteredTracks.value
  if (!availableTracks.length) {
    isPlaying.value = false
    progressSeconds.value = 0
    return
  }

  const currentExists = availableTracks.some((track) => track.id === currentTrack.value.id)
  if (!currentExists) {
    currentTrack.value = availableTracks[0]
    progressSeconds.value = 0
    isPlaying.value = false
  }
})

watch(isPlaying, (playing) => {
  if (playing) {
    startTimer()
  } else {
    stopTimer()
  }
})

watch(progressSeconds, (value) => {
  if (value > currentTrack.value.duration) {
    progressSeconds.value = currentTrack.value.duration
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

onMounted(() => {
  setupVoiceRecognition()
})

onBeforeUnmount(() => {
  stopTimer()
  stopVoiceRecognition()
})
</script>

<style scoped>
.service-page {
  position: relative;
  min-height: 100vh;
  overflow: hidden;
  padding: 40px 24px;
}

.blur-orb {
  position: fixed;
  border-radius: 50%;
  filter: blur(80px);
  pointer-events: none;
  animation: floatGentle 15s ease-in-out infinite alternate;
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
  max-width: 1440px;
  margin: 0 auto;
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) minmax(400px, 0.8fr);
  gap: 30px;
}

.surface {
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-float);
  border: 1px solid rgba(255, 255, 255, 0.6);
  backdrop-filter: blur(20px);
}

.warm-surface {
  padding: 40px;
  background: rgba(249, 248, 246, 0.75);
}

.cool-surface {
  padding: 40px;
  background: rgba(240, 239, 234, 0.75);
  display: flex;
  flex-direction: column;
}

.panel-head,
.top-tools,
.section-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
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
  margin-top: 40px;
  align-items: flex-end;
}

.search-field {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.search-field span,
.volume-control span {
  font-weight: 500;
  color: var(--color-text-primary);
}

.search-field input {
  height: 52px;
  padding: 0 20px;
  border-radius: var(--radius-pill);
  border: 1px solid rgba(44, 48, 46, 0.1);
  background: rgba(255, 255, 255, 0.6);
  outline: none;
  font-size: 1rem;
  transition: all var(--transition-medium);
}

.search-field input:focus {
  background: #fff;
  border-color: var(--color-accent-terracotta);
  box-shadow: 0 0 0 4px rgba(200, 138, 117, 0.1);
}

.category-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 16px;
  margin-top: 30px;
}

.category-card {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  padding: 24px;
  background: rgba(255, 255, 255, 0.5);
  border: 1px solid rgba(44, 48, 46, 0.05);
  border-radius: var(--radius-lg);
  text-align: left;
  transition: all var(--transition-medium);
}

.category-card:hover {
  background: rgba(255, 255, 255, 0.8);
  transform: translateY(-2px);
  box-shadow: var(--shadow-soft);
}

.category-card.active {
  background: var(--color-text-primary);
  color: var(--color-bg-primary);
  border-color: var(--color-text-primary);
}

.category-card strong {
  font-size: 1.1rem;
  font-weight: 600;
}

.category-card span {
  font-size: 0.85rem;
  margin-top: 8px;
  opacity: 0.8;
}

.track-section {
  margin-top: 60px;
  background: rgba(255, 255, 255, 0.6);
  border-radius: var(--radius-xl);
  padding: 30px;
}

.track-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 24px;
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
  margin-top: 40px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding: 24px 30px;
  background: var(--color-text-primary);
  color: var(--color-bg-primary);
  border-radius: var(--radius-pill);
}

.now-playing {
  display: flex;
  align-items: center;
  gap: 16px;
  min-width: 200px;
}

.now-playing img {
  width: 56px;
  height: 56px;
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
}

.now-playing span {
  font-size: 0.85rem;
  color: rgba(255,255,255,0.7);
}

.player-center {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.player-actions {
  display: flex;
  align-items: center;
  gap: 16px;
}

.action-button,
.primary-button {
  border-radius: var(--radius-pill);
  font-weight: 500;
  transition: all var(--transition-fast);
}

.action-button {
  padding: 8px 16px;
  color: rgba(255,255,255,0.8);
}
.action-button:hover {
  color: #fff;
  background: rgba(255,255,255,0.1);
}

.primary-button {
  padding: 12px 32px;
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
}

.voice-box .primary-button {
  background: var(--color-text-primary);
  color: var(--color-bg-primary);
  padding: 16px;
  font-size: 1.1rem;
}

.portal-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  margin-top: 30px;
  padding-top: 30px;
  border-top: 1px solid rgba(44, 48, 46, 0.1);
}

.portal-card {
  display: flex;
  flex-direction: column;
  padding: 20px;
  background: rgba(255, 255, 255, 0.6);
  border-radius: var(--radius-lg);
  text-align: left;
  transition: all var(--transition-fast);
}

.portal-card:hover {
  background: #fff;
  transform: translateY(-2px);
  box-shadow: var(--shadow-soft);
}

.portal-card strong {
  font-size: 1.1rem;
  color: var(--color-text-primary);
  margin-bottom: 8px;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

@media (max-width: 1024px) {
  .service-shell {
    grid-template-columns: 1fr;
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
