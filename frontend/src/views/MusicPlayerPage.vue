<template>
  <div class="music-player-page">
    <div class="player-glow player-glow-left"></div>
    <div class="player-glow player-glow-right"></div>

    <header class="player-topbar">
      <button class="back-btn" type="button" @click="goBack">
        <font-awesome-icon icon="angle-left" />
        <span>返回</span>
      </button>
      <div class="topbar-note">沉浸播放页</div>
    </header>

    <main class="player-stage glass-shell" v-if="currentTrack">
      <section class="cover-zone">
        <div class="cover-shell">
          <div class="record-disc" :class="{ spinning: isPlaying }">
            <img :src="currentTrack.cover" :alt="currentTrack.title" class="vinyl-cover" />
            <div class="record-center"></div>
          </div>
        </div>

        <div class="track-brief">
          <span class="brief-label">正在播放</span>
          <h1>{{ currentTrack.title }}</h1>
          <p>{{ currentTrack.artist }}</p>
          
          <div class="track-actions">
            <button class="action-icon-btn" :class="{ active: isLiked }" @click="handleLike" title="喜欢">
              <font-awesome-icon icon="heart" />
            </button>
            <button class="action-icon-btn" :class="{ active: isCollected }" @click="handleCollect" title="收藏">
              <font-awesome-icon icon="star" />
            </button>
            
            <el-dropdown trigger="click" @command="handleAddToPlaylist" placement="top">
              <button class="action-icon-btn" title="添加到歌单">
                <font-awesome-icon icon="folder-plus" />
              </button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item v-if="!musicStore.customPlaylists.length" disabled>暂无自建歌单</el-dropdown-item>
                  <el-dropdown-item 
                    v-for="playlist in musicStore.customPlaylists" 
                    :key="playlist.id" 
                    :command="playlist.id"
                  >
                    {{ playlist.name }}
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </div>
      </section>

      <section class="meta-zone">
        <div class="meta-card mood-card">
          <span class="meta-kicker">Current Mood</span>
          <h2>这首歌想陪你停留在 {{ primaryMood }}</h2>
          <p class="meta-desc">
            {{
              currentTrack.type === '冥想背景音'
                ? '适合冥想、放松和稳定呼吸节奏的背景声场。'
                : '这是一首适合慢下来、安静感受当下的轻音乐。'
            }}
          </p>

          <div class="tag-row">
            <span class="type-chip">{{ currentTrack.type }}</span>
            <span v-for="tag in displayTags" :key="tag" class="mood-chip">
              #{{ tag }}
            </span>
          </div>
        </div>

        <div class="meta-card queue-card">
          <div class="queue-head">
            <div>
              <span class="meta-kicker">Queue</span>
              <h3>接下来还可以听</h3>
            </div>
            <span class="queue-count">{{ queue.length }} 首</span>
          </div>

          <div class="queue-list">
            <button
              v-for="track in queue"
              :key="track.id"
              class="queue-item"
              :class="{ active: track.id === currentTrack.id }"
              type="button"
              @click="selectTrack(track)"
            >
              <img :src="track.cover" :alt="track.title" />
              <div class="queue-item-meta">
                <strong>{{ track.title }}</strong>
                <span>{{ track.artist }}</span>
              </div>
              <small>{{ formatTime(track.duration) }}</small>
            </button>
          </div>
        </div>
      </section>

      <section class="bottom-controls">
        <div class="progress-block">
          <span>{{ formatTime(progressSeconds) }}</span>
          <input v-model.number="progressSeconds" type="range" min="0" :max="currentTrack.duration || 0" step="1" />
          <span>{{ formatTime(currentTrack.duration) }}</span>
        </div>

        <div class="control-row">
          <button class="secondary-btn" type="button" @click="playPrevious">上一首</button>
          <button class="play-btn" type="button" @click="togglePlayback">
            {{ isPlaying ? '暂停播放' : '开始播放' }}
          </button>
          <button class="secondary-btn" type="button" @click="playNext">下一首</button>

          <label class="volume-box">
            <span>音量</span>
            <input v-model.number="volume" type="range" min="0" max="100" />
          </label>
        </div>
      </section>
    </main>

    <section v-else class="empty-shell glass-shell">
      <h2>还没有可播放的音乐</h2>
      <p>请先从主页面或冥想室点击底部播放条进入这里。</p>
      <button class="play-btn" type="button" @click="goBack">返回上一页</button>
    </section>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useMusicStore } from '@/stores/musicStore'
import {
  getEmotionMusicApi,
  getMusicFileByNameApi,
  getNextEmotionMusicApi,
  getPreviousEmotionMusicApi,
} from '@/api/python'
import { appendUserBehaviorLogApi } from '@/api/data'

const router = useRouter()
const musicStore = useMusicStore()

const PLAYER_SESSION_KEY = 'emotion-system-active-player'

const payload = ref(null)
const queue = ref([])
const currentTrack = ref(null)
const isPlaying = ref(false)
const progressSeconds = ref(0)
const volume = ref(72)
const currentEmotion = ref('neutral')
const isLoadingAudio = ref(false)

const isLiked = computed(() => currentTrack.value && musicStore.likedTrackIds.includes(currentTrack.value.id))
const isCollected = computed(() => currentTrack.value && musicStore.collectedTrackIds.includes(currentTrack.value.id))

const handleLike = async () => {
  if (currentTrack.value) {
    await musicStore.toggleLike(currentTrack.value.id)
  }
}

const handleCollect = async () => {
  if (currentTrack.value) {
    await musicStore.toggleCollect(currentTrack.value.id)
  }
}

const handleAddToPlaylist = async (playlistId) => {
  if (currentTrack.value) {
    await musicStore.addTrackToPlaylist(playlistId, currentTrack.value)
  }
}

let playTimer = null
const audioPlayer = new Audio()
let currentObjectUrl = null

const emotionAliasMap = {
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

const displayTags = computed(() => {
  const tags = currentTrack.value?.tags || []
  return tags.length ? tags : ['轻音乐']
})

const primaryMood = computed(() => displayTags.value[0] || '平静')
const preferRecommendedNext = computed(() => ['recommend', 'guess'].includes(payload.value?.categoryId))

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

const cleanupObjectUrl = () => {
  if (currentObjectUrl) {
    URL.revokeObjectURL(currentObjectUrl)
    currentObjectUrl = null
  }
}

const normalizeEmotionValue = (value) => String(value || '').trim().toLowerCase()

const inferEmotionFromText = (value) => {
  const normalized = normalizeEmotionValue(value)
  if (!normalized) return null

  for (const [alias, target] of Object.entries(emotionAliasMap)) {
    if (normalized === alias || normalized.includes(alias)) {
      return target
    }
  }

  return null
}

const resolveEmotionForTrack = (track) => {
  const candidates = [
    window.localStorage.getItem('currentEmotion'),
    track?.emotion,
    track?.id,
    track?.title,
    track?.artist,
    track?.type,
    ...(Array.isArray(track?.tags) ? track.tags : []),
  ]

  for (const candidate of candidates) {
    const matchedEmotion = inferEmotionFromText(candidate)
    if (matchedEmotion) {
      return matchedEmotion
    }
  }

  return 'neutral'
}

const persistEmotion = (emotion) => {
  currentEmotion.value = emotion
  window.localStorage.setItem('currentEmotion', emotion)
}

const syncTrackWithQueue = (step) => {
  if (!queue.value.length || !currentTrack.value) return

  const index = queue.value.findIndex((track) => track.id === currentTrack.value.id)
  const nextIndex =
    step > 0
      ? (index >= 0 ? (index + 1) % queue.value.length : 0)
      : (index > 0 ? index - 1 : queue.value.length - 1)

  currentTrack.value = queue.value[nextIndex]
}

const loadAudioBlob = async (requestFactory) => {
  if (isLoadingAudio.value) return

  try {
    isLoadingAudio.value = true
    const response = await requestFactory()
    cleanupObjectUrl()
    currentObjectUrl = URL.createObjectURL(response.data)
    audioPlayer.src = currentObjectUrl
    audioPlayer.currentTime = 0
    progressSeconds.value = 0
    audioPlayer.volume = volume.value / 100
    await audioPlayer.play()
    isPlaying.value = true
    
    // User Behavior Log
    appendUserBehaviorLogApi({
      actionType: 'play_music',
      targetType: 'music',
      targetId: currentTrack.value?.id || 0,
      metadata: {
        title: currentTrack.value?.title,
        emotion: currentEmotion.value
      }
    }).catch(e => console.warn('Log user behavior failed', e))
    
  } catch (error) {
    isPlaying.value = false
    ElMessage.error('音乐播放失败，请确认 Python 音乐服务已启动。')
    console.error('Audio playback failed:', error)
  } finally {
    isLoadingAudio.value = false
  }
}

const loadAudioUrl = async (sourceUrl) => {
  if (!sourceUrl || isLoadingAudio.value) return

  try {
    isLoadingAudio.value = true
    cleanupObjectUrl()
    audioPlayer.src = sourceUrl
    audioPlayer.currentTime = 0
    progressSeconds.value = 0
    audioPlayer.volume = volume.value / 100
    await audioPlayer.play()
    isPlaying.value = true

    appendUserBehaviorLogApi({
      actionType: 'play_music',
      targetType: 'music',
      targetId: currentTrack.value?.id || 0,
      metadata: {
        title: currentTrack.value?.title,
        emotion: currentEmotion.value
      }
    }).catch(e => console.warn('Log user behavior failed', e))
  } catch (error) {
    isPlaying.value = false
    ElMessage.error('音乐播放失败，请确认音乐服务已启动。')
    console.error('Audio playback failed:', error)
  } finally {
    isLoadingAudio.value = false
  }
}

const startTimer = () => {
  stopTimer()
  playTimer = setInterval(() => {
    if (!isPlaying.value || !currentTrack.value) return

    progressSeconds.value = audioPlayer.currentTime || progressSeconds.value
  }, 1000)
}

const playCurrentTrack = async () => {
  if (currentTrack.value?.fileUrl) {
    await loadAudioUrl(currentTrack.value.fileUrl)
    return
  }

  if (currentTrack.value?.filename) {
    await loadAudioBlob(() => getMusicFileByNameApi(currentTrack.value.filename))
    return
  }

  const emotion = resolveEmotionForTrack(currentTrack.value)
  persistEmotion(emotion)
  await loadAudioBlob(() => getEmotionMusicApi(emotion))
}

const selectTrack = async (track) => {
  currentTrack.value = track
  progressSeconds.value = 0
  await playCurrentTrack()
}

const togglePlayback = async () => {
  if (!currentTrack.value) return

  if (!audioPlayer.src) {
    await playCurrentTrack()
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
    console.error('Resume playback failed:', error)
  }
}

const playNext = async () => {
  if (!currentTrack.value) return

  if (preferRecommendedNext.value && currentTrack.value?.musicResourceId) {
    const nextTrack = await musicStore.fetchNextRecommendedTrack(currentTrack.value.musicResourceId, currentEmotion.value)
    if (nextTrack) {
      const queueIndex = queue.value.findIndex((track) => track.id === nextTrack.id)
      if (queueIndex === -1) {
        queue.value = [...queue.value, nextTrack]
      }
      currentTrack.value = nextTrack
      await playCurrentTrack()
      return
    }
  }

  syncTrackWithQueue(1)
  if (currentTrack.value?.filename) {
    await playCurrentTrack()
    return
  }
  await loadAudioBlob(() => getNextEmotionMusicApi(currentEmotion.value))
}

const playPrevious = async () => {
  if (!currentTrack.value) return
  syncTrackWithQueue(-1)
  if (currentTrack.value?.filename) {
    await playCurrentTrack()
    return
  }
  await loadAudioBlob(() => getPreviousEmotionMusicApi(currentEmotion.value))
}

const goBack = () => {
  router.push(payload.value?.returnTo || '/service')
}

const loadPayload = () => {
  const raw = window.sessionStorage.getItem(PLAYER_SESSION_KEY)
  if (!raw) return

  try {
    const parsed = JSON.parse(raw)
    payload.value = parsed
    queue.value = Array.isArray(parsed.queue) ? parsed.queue : []
    currentTrack.value = parsed.track || parsed.queue?.[0] || null
    progressSeconds.value = 0
    if (currentTrack.value) {
      persistEmotion(resolveEmotionForTrack(currentTrack.value))
    }
  } catch (error) {
    console.error('读取播放器数据失败', error)
  }
}

watch(isPlaying, (playing) => {
  if (playing) {
    startTimer()
  } else {
    stopTimer()
  }
})

watch(progressSeconds, (value) => {
  if (!currentTrack.value || !audioPlayer.src) return
  if (Math.abs(audioPlayer.currentTime - value) > 1) {
    audioPlayer.currentTime = value
  }
})

watch(volume, (value) => {
  audioPlayer.volume = value / 100
})

audioPlayer.onloadedmetadata = () => {
  const measuredDuration = Number.isFinite(audioPlayer.duration) ? Math.floor(audioPlayer.duration) : 0
  if (currentTrack.value && measuredDuration > 0) {
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

onMounted(() => {
  loadPayload()
  if (currentTrack.value) {
    playCurrentTrack()
  }
})

onBeforeUnmount(() => {
  stopTimer()
  audioPlayer.pause()
  audioPlayer.src = ''
  cleanupObjectUrl()
})
</script>

<style scoped>
.music-player-page {
  height: 100vh;
  padding: 24px;
  position: relative;
  overflow: hidden;
  background:
    linear-gradient(90deg, #dfe4ff 0%, #e5e2ff 42%, #f6dce9 100%);
  display: flex;
  flex-direction: column;
  box-sizing: border-box;
}

.player-glow {
  position: absolute;
  border-radius: 50%;
  filter: blur(90px);
  opacity: 0.5;
  pointer-events: none;
}

.player-glow-left {
  width: 320px;
  height: 320px;
  left: -80px;
  top: 18%;
  background: rgba(146, 165, 255, 0.6);
}

.player-glow-right {
  width: 360px;
  height: 360px;
  right: -120px;
  top: 12%;
  background: rgba(255, 180, 209, 0.68);
}

.player-topbar {
  position: relative;
  z-index: 1;
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  flex-shrink: 0;
}

.back-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
  border: none;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.75);
  color: #39445f;
  font-weight: 600;
  cursor: pointer;
  box-shadow: 0 12px 28px rgba(92, 99, 141, 0.12);
}

.topbar-note {
  padding: 9px 14px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.52);
  color: #55627e;
  font-size: 0.9rem;
  letter-spacing: 0.06em;
}

.glass-shell {
  position: relative;
  z-index: 1;
  background: rgba(255, 255, 255, 0.26);
  border: 1px solid rgba(255, 255, 255, 0.5);
  border-radius: 32px;
  backdrop-filter: blur(18px);
  box-shadow: 0 30px 60px rgba(89, 91, 124, 0.12);
}

.player-stage {
  flex: 1;
  min-height: 0;
  padding: 36px;
  display: grid;
  grid-template-columns: 1fr 1.6fr;
  grid-template-rows: minmax(0, 1fr) auto;
  gap: 26px 48px;
  box-sizing: border-box;
}

.cover-zone,
.meta-zone {
  min-height: 0;
}

.cover-zone {
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 28px;
}

.cover-shell {
  width: min(100%, 400px);
  aspect-ratio: 1 / 1;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto;
}

.record-disc {
  position: absolute;
  inset: 9%;
  border-radius: 50%;
  background:
    radial-gradient(circle at center, #111 0 35%, #222 36% 40%, #111 41% 45%, #222 46% 50%, #111 51% 100%);
  box-shadow: inset 0 0 30px rgba(0, 0, 0, 0.5), 0 22px 42px rgba(82, 84, 117, 0.18);
  display: flex;
  align-items: center;
  justify-content: center;
}

.record-disc::after {
  content: '';
  position: absolute;
  inset: 0;
  border-radius: 50%;
  background: conic-gradient(
    from 45deg,
    rgba(255, 255, 255, 0.1) 0deg,
    rgba(255, 255, 255, 0) 45deg,
    rgba(255, 255, 255, 0) 135deg,
    rgba(255, 255, 255, 0.1) 180deg,
    rgba(255, 255, 255, 0) 225deg,
    rgba(255, 255, 255, 0) 315deg,
    rgba(255, 255, 255, 0.1) 360deg
  );
  pointer-events: none;
}

.record-disc.spinning {
  animation: spinRecord 10s linear infinite;
}

.vinyl-cover {
  width: 50%;
  height: 50%;
  border-radius: 50%;
  object-fit: cover;
  box-shadow: 0 0 0 4px #1a1a1a;
}

.record-center {
  position: absolute;
  width: 10%;
  height: 10%;
  border-radius: 50%;
  background: #f7f7f9;
  box-shadow: inset 0 0 4px rgba(0,0,0,0.4);
}

@keyframes spinRecord {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}
.queue-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.track-brief {
  text-align: center;
}

.brief-label,
.meta-kicker {
  display: inline-flex;
  align-items: center;
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.44);
  color: #687395;
  font-size: 0.82rem;
  letter-spacing: 0.06em;
  text-transform: uppercase;
}

.track-brief h1,
.meta-card h2,
.meta-card h3,
.empty-shell h2 {
  margin: 14px 0 10px;
  color: #2f3550;
}

.track-brief h1 {
  font-size: clamp(2rem, 3vw, 3rem);
}

.track-brief p,
.meta-desc,
.queue-item-meta span,
.empty-shell p {
  margin: 0;
  color: #69748f;
  line-height: 1.7;
}

.track-actions {
  display: flex;
  justify-content: center;
  gap: 16px;
  margin-top: 20px;
}

.action-icon-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  border-radius: 50%;
  border: 1px solid rgba(255, 255, 255, 0.4);
  background: rgba(255, 255, 255, 0.2);
  color: #69748f;
  font-size: 1.1rem;
  cursor: pointer;
  transition: all 0.2s ease;
  backdrop-filter: blur(10px);
}

.action-icon-btn:hover {
  background: rgba(255, 255, 255, 0.5);
  transform: translateY(-2px);
  color: #39445f;
}

.action-icon-btn.active {
  color: #ff5b77;
  background: rgba(255, 255, 255, 0.6);
  border-color: rgba(255, 91, 119, 0.3);
}

.action-icon-btn.active[title="收藏"] {
  color: #f5a623;
  border-color: rgba(245, 166, 35, 0.3);
}

.meta-zone {
  display: flex;
  flex-direction: column;
  gap: 18px;
  min-height: 0;
}

.meta-card {
  min-height: 0;
  padding: 24px;
  border-radius: 26px;
  background: rgba(255, 255, 255, 0.34);
  border: 1px solid rgba(255, 255, 255, 0.52);
  box-sizing: border-box;
}

.mood-card {
  flex: 0 0 auto;
  padding: 20px 22px;
}

.queue-card {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.tag-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 14px;
}

.type-chip,
.mood-chip,
.queue-count {
  display: inline-flex;
  align-items: center;
  border-radius: 999px;
  padding: 8px 14px;
  font-size: 0.88rem;
}

.type-chip {
  background: rgba(93, 110, 255, 0.12);
  color: #5565cf;
}

.mood-chip {
  background: rgba(255, 255, 255, 0.62);
  color: #495673;
}

.queue-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 14px;
  flex-shrink: 0;
}

.queue-count {
  background: rgba(255, 255, 255, 0.48);
  color: #5b6680;
}

.queue-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  overflow-x: hidden;
  padding-right: 6px;
  scrollbar-gutter: stable;
}

.queue-item {
  display: grid;
  grid-template-columns: 58px minmax(0, 1fr) 48px;
  gap: 14px;
  align-items: center;
  padding: 10px;
  border: 1px solid transparent;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.44);
  cursor: pointer;
  transition: transform 0.2s ease, border-color 0.2s ease, background 0.2s ease;
  width: 100%;
  box-sizing: border-box;
  min-width: 0;
}

.queue-item:hover,
.queue-item.active {
  transform: translateY(-1px);
  border-color: rgba(153, 164, 229, 0.5);
  background: rgba(255, 255, 255, 0.72);
}

.queue-item img {
  width: 58px;
  height: 58px;
  border-radius: 14px;
}

.queue-item-meta {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
  text-align: left;
}

.queue-item-meta strong {
  color: #303856;
  line-height: 1.35;
}

.queue-item small {
  color: #77819a;
  text-align: right;
  white-space: nowrap;
}

.meta-card h2 {
  margin: 12px 0 8px;
  font-size: 1.35rem;
  line-height: 1.4;
}

.meta-card h3 {
  margin: 10px 0 0;
  font-size: 1.12rem;
}

.meta-desc {
  max-width: 32ch;
}

.queue-list::-webkit-scrollbar {
  width: 8px;
}

.queue-list::-webkit-scrollbar-track {
  background: rgba(255, 255, 255, 0.2);
  border-radius: 999px;
}

.queue-list::-webkit-scrollbar-thumb {
  background: rgba(120, 132, 190, 0.35);
  border-radius: 999px;
}

.bottom-controls {
  grid-column: 1 / -1;
  padding: 16px 24px;
  border-radius: 20px;
  background: rgba(33, 37, 49, 0.86);
  color: #fff;
  display: flex;
  align-items: center;
  gap: 20px;
}

.progress-block {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 12px;
}

.progress-block span,
.volume-box span {
  color: rgba(255, 255, 255, 0.82);
  font-size: 0.9rem;
}

.control-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.secondary-btn,
.play-btn {
  border: none;
  border-radius: 999px;
  padding: 8px 16px;
  font-size: 0.9rem;
  font-weight: 600;
  cursor: pointer;
}

.secondary-btn {
  background: rgba(255, 255, 255, 0.12);
  color: #fff;
}

.play-btn {
  background: #fff;
  color: #2f3550;
  min-width: 132px;
}

.volume-box {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  margin-left: 10px;
  width: 120px;
}

input[type='range'] {
  flex: 1;
  min-width: 0;
  accent-color: #7a88ff;
  height: 4px;
  border-radius: 2px;
}

.empty-shell {
  max-width: 720px;
  margin: 120px auto 0;
  padding: 48px 32px;
  text-align: center;
}

@keyframes spinRecord {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

@media (max-width: 1100px) {
  .player-stage {
    min-height: auto;
    grid-template-columns: 1fr;
  }

  .cover-shell {
    width: min(100%, 420px);
  }

  .queue-list {
    max-height: 360px;
  }
}

@media (max-width: 720px) {
  .music-player-page {
    padding: 14px;
  }

  .player-stage {
    padding: 20px;
    gap: 18px;
  }

  .bottom-controls {
    padding: 18px;
  }

  .progress-block {
    grid-template-columns: 1fr;
  }

  .volume-box {
    width: 100%;
    margin-left: 0;
    justify-content: space-between;
  }

  .queue-item {
    grid-template-columns: 50px minmax(0, 1fr);
  }

  .queue-item small {
    grid-column: 2;
    text-align: left;
  }
}
</style>
