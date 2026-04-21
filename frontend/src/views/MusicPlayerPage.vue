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
            <div class="record-center"></div>
          </div>
          <div class="cover-card">
            <img :src="currentTrack.cover" :alt="currentTrack.title" />
          </div>
        </div>

        <div class="track-brief">
          <span class="brief-label">正在播放</span>
          <h1>{{ currentTrack.title }}</h1>
          <p>{{ currentTrack.artist }}</p>
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
          <input v-model="progressSeconds" type="range" min="0" :max="currentTrack.duration" step="1" />
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
            <input v-model="volume" type="range" min="0" max="100" />
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

const router = useRouter()

const PLAYER_SESSION_KEY = 'emotion-system-active-player'

const payload = ref(null)
const queue = ref([])
const currentTrack = ref(null)
const isPlaying = ref(false)
const progressSeconds = ref(0)
const volume = ref(72)

let playTimer = null

const displayTags = computed(() => {
  const tags = currentTrack.value?.tags || []
  return tags.length ? tags : ['轻音乐']
})

const primaryMood = computed(() => displayTags.value[0] || '平静')

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
    if (!isPlaying.value || !currentTrack.value) return

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
  startTimer()
}

const togglePlayback = () => {
  if (!currentTrack.value) return
  isPlaying.value = !isPlaying.value
}

const playNext = () => {
  if (!queue.value.length || !currentTrack.value) return
  const index = queue.value.findIndex((track) => track.id === currentTrack.value.id)
  const nextIndex = index >= 0 ? (index + 1) % queue.value.length : 0
  selectTrack(queue.value[nextIndex])
}

const playPrevious = () => {
  if (!queue.value.length || !currentTrack.value) return
  const index = queue.value.findIndex((track) => track.id === currentTrack.value.id)
  const prevIndex = index > 0 ? index - 1 : queue.value.length - 1
  selectTrack(queue.value[prevIndex])
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
    isPlaying.value = Boolean(currentTrack.value)
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
  if (!currentTrack.value) return
  if (value > currentTrack.value.duration) {
    progressSeconds.value = currentTrack.value.duration
  }
})

onMounted(() => {
  loadPayload()
  if (isPlaying.value) {
    startTimer()
  }
})

onBeforeUnmount(() => {
  stopTimer()
})
</script>

<style scoped>
.music-player-page {
  min-height: 100vh;
  padding: 24px;
  position: relative;
  overflow: hidden;
  background:
    linear-gradient(90deg, #dfe4ff 0%, #e5e2ff 42%, #f6dce9 100%);
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
  min-height: calc(100vh - 110px);
  padding: 36px;
  display: grid;
  grid-template-columns: 1.05fr 0.95fr;
  grid-template-rows: minmax(0, 1fr) auto;
  gap: 26px 32px;
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
  width: min(100%, 520px);
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
    radial-gradient(circle at center, #3b3340 0 8%, #ececf1 8.5% 16%, #5c5761 17% 23%, #cfd2da 24% 54%, #8e9098 55% 63%, #d8dae0 64% 100%);
  box-shadow: inset 0 0 30px rgba(0, 0, 0, 0.22), 0 22px 42px rgba(82, 84, 117, 0.18);
}

.record-disc.spinning {
  animation: spinRecord 10s linear infinite;
}

.record-center {
  position: absolute;
  inset: 42%;
  border-radius: 50%;
  background: #f7f7f9;
  box-shadow: 0 0 0 8px rgba(255, 255, 255, 0.32);
}

.cover-card {
  position: relative;
  width: 44%;
  aspect-ratio: 1 / 1;
  border-radius: 20px;
  overflow: hidden;
  box-shadow: 0 16px 30px rgba(61, 61, 86, 0.18);
}

.cover-card img,
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
  padding: 22px 24px;
  border-radius: 24px;
  background: rgba(33, 37, 49, 0.86);
  color: #fff;
}

.progress-block {
  display: grid;
  grid-template-columns: auto 1fr auto;
  align-items: center;
  gap: 14px;
}

.progress-block span,
.volume-box span {
  color: rgba(255, 255, 255, 0.82);
}

.control-row {
  margin-top: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  flex-wrap: wrap;
}

.secondary-btn,
.play-btn {
  border: none;
  border-radius: 999px;
  padding: 12px 22px;
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
  gap: 12px;
  margin-left: 10px;
}

input[type='range'] {
  width: 100%;
  accent-color: #7a88ff;
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
