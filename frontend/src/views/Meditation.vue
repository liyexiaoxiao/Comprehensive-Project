<template>
  <div class="meditation-page">
    <div class="blur-orb orb-coral"></div>
    <div class="blur-orb orb-leaf"></div>
    <div class="blur-orb orb-sky"></div>

    <div class="page-shell">
      <div class="top-bar">
        <button class="back-btn" @click="goBack">
          <font-awesome-icon icon="angle-left" />
          <span>返回</span>
        </button>
        <h1 class="page-title">冥想室</h1>
        <div style="width: 80px"></div> <!-- placeholder for balance -->
      </div>

      <div class="main-content">
        <!-- Left: Timer and Controls -->
        <div class="timer-panel glass-panel">
          <div class="timer-wrapper">
            <CircleTimer 
              :total-time="selectedTime" 
              :colors="['#D97A6C', '#8ca595']" 
              key="circle-timer" 
            />
          </div>
          
          <div class="time-options">
            <button 
              v-for="option in timeOptions" 
              :key="option" 
              :class="['time-btn', { active: selectedTime === option }]"
              @click="selectTime(option)"
            >
              {{ option / 60 }}分钟
            </button>
          </div>

          <button class="guide-btn" @click="startGuide">
            <span class="guide-icon">✨</span>
            <span>冥想引导</span>
          </button>
        </div>

        <!-- Right: Playlist and Player -->
        <div class="playlist-panel glass-panel">
          <div class="panel-header">
            <h2>冥想背景音</h2>
            <p>选择符合当下情绪的背景音</p>
          </div>

          <div class="emotion-grid">
            <button 
              v-for="emotion in emotions" 
              :key="emotion.id"
              :class="['emotion-btn', { active: currentTrack?.id === emotion.id }]"
              @click="selectTrack(emotion)"
            >
              {{ emotion.name }}
            </button>
          </div>

          <div class="player-controls">
            <div class="now-playing-info" v-if="currentTrack">
              <strong>{{ currentTrack.name }}</strong>
              <span>{{ formatTime(progressSeconds) }} / {{ formatTime(currentTrack.duration) }}</span>
            </div>
            <div class="now-playing-info" v-else>
              <strong>未选择音乐</strong>
              <span>00:00 / 00:00</span>
            </div>

            <div class="control-buttons">
              <button class="ctrl-btn" @click="playPrevious" :disabled="!currentTrack">
                <font-awesome-icon icon="backward-step" />
              </button>
              <button class="ctrl-btn play-btn" @click="togglePlayback" :disabled="!currentTrack">
                <font-awesome-icon :icon="isPlaying ? 'pause' : 'play'" />
              </button>
              <button class="ctrl-btn" @click="playNext" :disabled="!currentTrack">
                <font-awesome-icon icon="forward-step" />
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import CircleTimer from '@/components/CircleTimer.vue'

const emotions = [
  { id: 'calm', name: '平静', title: '平静之声', artist: 'Emotion Healing', duration: 1800 },
  { id: 'relax', name: '放松', title: '放松频率', artist: 'Emotion Healing', duration: 1800 },
  { id: 'focus', name: '专注', title: '深度专注', artist: 'Emotion Healing', duration: 1800 },
  { id: 'tired', name: '疲惫', title: '缓解疲惫', artist: 'Emotion Healing', duration: 1800 },
  { id: 'anxious', name: '焦虑', title: '抚平焦虑', artist: 'Emotion Healing', duration: 1800 },
  { id: 'sad', name: '悲伤', title: '拥抱悲伤', artist: 'Emotion Healing', duration: 1800 },
  { id: 'lonely', name: '孤独', title: '陪伴孤独', artist: 'Emotion Healing', duration: 1800 },
  { id: 'joyful', name: '喜悦', title: '喜悦共振', artist: 'Emotion Healing', duration: 1800 },
  { id: 'angry', name: '愤怒', title: '释放愤怒', artist: 'Emotion Healing', duration: 1800 },
  { id: 'hopeful', name: '充满希望', title: '希望之光', artist: 'Emotion Healing', duration: 1800 }
]

const router = useRouter()

const goBack = () => {
  router.push('/service')
}

// Timer Logic
const timeOptions = [180, 300, 600, 1200] // 3min, 5min, 10min, 20min
const selectedTime = ref(300)

const selectTime = (option) => {
  selectedTime.value = option
}

const startGuide = () => {
  ElMessage.info('冥想引导功能即将上线，敬请期待。')
}

// Player Logic
const currentTrack = ref(null)
const isPlaying = ref(false)
const progressSeconds = ref(0)
let playTimer = null

const formatTime = (seconds) => {
  const safeValue = Number.isFinite(seconds) ? seconds : 0
  const m = Math.floor(safeValue / 60)
  const s = Math.floor(safeValue % 60)
  return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
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
  if (isPlaying.value) {
    startTimer()
  } else {
    stopTimer()
  }
}

const playNext = () => {
  if (!currentTrack.value) return
  const idx = emotions.findIndex(t => t.id === currentTrack.value.id)
  const nextIdx = (idx + 1) % emotions.length
  selectTrack(emotions[nextIdx])
}

const playPrevious = () => {
  if (!currentTrack.value) return
  const idx = emotions.findIndex(t => t.id === currentTrack.value.id)
  const prevIdx = idx > 0 ? idx - 1 : emotions.length - 1
  selectTrack(emotions[prevIdx])
}

onMounted(() => {
  // Optionally select the first track automatically
  // selectTrack(activeCategory.value.tracks[0])
})

onBeforeUnmount(() => {
  stopTimer()
})
</script>

<style scoped>
.meditation-page {
  position: relative;
  height: 100vh;
  width: 100vw;
  overflow: hidden;
  background-color: var(--color-bg-primary);
  display: flex;
  justify-content: center;
  align-items: center;
}

.blur-orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  pointer-events: none;
  animation: floatGentle 15s ease-in-out infinite alternate;
  z-index: 0;
}

.orb-coral { top: -10%; right: -5%; width: 500px; height: 500px; background: var(--color-accent-blush); opacity: 0.6; }
.orb-leaf { left: -10%; bottom: -10%; width: 600px; height: 600px; background: var(--color-accent-sage); opacity: 0.5; animation-delay: -5s; }
.orb-sky { left: 40%; top: 30%; width: 400px; height: 400px; background: var(--color-accent-sky); opacity: 0.4; animation-delay: -2s; }

.page-shell {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 1400px;
  height: 100%;
  padding: 40px;
  display: flex;
  flex-direction: column;
}

.top-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
}

.back-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  background: rgba(255, 255, 255, 0.6);
  border: 1px solid rgba(44, 48, 46, 0.1);
  padding: 10px 20px;
  border-radius: var(--radius-pill);
  font-weight: 600;
  color: var(--color-text-primary);
  transition: all var(--transition-fast);
}

.back-btn:hover {
  background: #fff;
  transform: translateY(-2px);
  box-shadow: var(--shadow-soft);
}

.page-title {
  font-family: var(--font-serif);
  font-size: 2rem;
  margin: 0;
  color: var(--color-text-primary);
}

.main-content {
  flex: 1;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 40px;
  overflow: hidden;
}

.glass-panel {
  background: rgba(255, 255, 255, 0.6);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.6);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-float);
  padding: 40px;
  display: flex;
  flex-direction: column;
}

/* Timer Panel */
.timer-panel {
  align-items: center;
  justify-content: center;
  gap: 40px;
}

.timer-wrapper {
  transform: scale(1.2);
  margin-bottom: 20px;
}

.time-options {
  display: flex;
  gap: 16px;
}

.time-btn {
  padding: 8px 20px;
  background: rgba(255, 255, 255, 0.5);
  border: 1px solid rgba(44, 48, 46, 0.1);
  border-radius: var(--radius-pill);
  font-weight: 500;
  color: var(--color-text-secondary);
  transition: all var(--transition-fast);
}

.time-btn.active {
  background: var(--color-text-primary);
  color: #fff;
  border-color: var(--color-text-primary);
}

.time-btn:hover:not(.active) {
  background: #fff;
}

.guide-btn {
  margin-top: 20px;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 40px;
  background: linear-gradient(135deg, var(--color-accent-sage), #8ca595);
  color: #fff;
  border: none;
  border-radius: var(--radius-pill);
  font-size: 1.2rem;
  font-weight: 600;
  box-shadow: 0 8px 20px rgba(124, 152, 133, 0.3);
  transition: all var(--transition-fast);
}

.guide-btn:hover {
  transform: translateY(-3px);
  box-shadow: 0 12px 24px rgba(124, 152, 133, 0.4);
}

.guide-icon {
  font-size: 1.4rem;
}

/* Playlist Panel */
.playlist-panel {
  overflow: hidden;
}

.panel-header h2 {
  font-family: var(--font-serif);
  font-size: 1.6rem;
  margin: 0 0 8px 0;
  color: var(--color-text-primary);
}

.panel-header p {
  color: var(--color-text-secondary);
  margin: 0 0 24px 0;
}

.emotion-grid {
  flex: 1;
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
  align-content: start;
  overflow-y: auto;
  padding-right: 8px;
  margin-bottom: 16px;
}

.emotion-btn {
  padding: 16px 20px;
  background: rgba(255, 255, 255, 0.4);
  border: 1px solid transparent;
  border-radius: var(--radius-lg);
  color: var(--color-text-primary);
  font-size: 1.1rem;
  font-weight: 500;
  cursor: pointer;
  transition: all var(--transition-medium);
  text-align: center;
}

.emotion-btn:hover {
  background: rgba(255, 255, 255, 0.8);
  transform: translateY(-2px);
}

.emotion-btn.active {
  background: #fff;
  border-color: rgba(44, 48, 46, 0.1);
  box-shadow: var(--shadow-soft);
  color: var(--color-accent-terracotta);
  font-weight: 600;
}

.player-controls {
  margin-top: 24px;
  padding: 20px;
  background: var(--color-text-primary);
  color: #fff;
  border-radius: var(--radius-xl);
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.now-playing-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.now-playing-info strong {
  font-size: 1.1rem;
}

.now-playing-info span {
  font-size: 0.9rem;
  color: rgba(255, 255, 255, 0.7);
}

.control-buttons {
  display: flex;
  align-items: center;
  gap: 16px;
}

.ctrl-btn {
  background: transparent;
  border: none;
  color: rgba(255, 255, 255, 0.8);
  font-size: 1.2rem;
  cursor: pointer;
  transition: all 0.2s;
}

.ctrl-btn:hover:not(:disabled) {
  color: #fff;
  transform: scale(1.1);
}

.ctrl-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.play-btn {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: #fff;
  color: var(--color-text-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.2rem;
}

.play-btn:hover:not(:disabled) {
  color: var(--color-text-primary);
  transform: scale(1.05);
}

@keyframes floatGentle {
  0% { transform: translate(0, 0) scale(1); }
  100% { transform: translate(30px, -30px) scale(1.05); }
}

@media (max-width: 1024px) {
  .main-content {
    grid-template-columns: 1fr;
    overflow-y: auto;
  }
  
  .page-shell {
    height: auto;
    min-height: 100vh;
  }
  
  .meditation-page {
    height: auto;
    overflow: auto;
  }
}
</style>
