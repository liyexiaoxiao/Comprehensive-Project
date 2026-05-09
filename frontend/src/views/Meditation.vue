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
              @complete="handleTimerComplete"
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
        </div>

        <!-- Right: Playlist and Player -->
        <div class="playlist-panel glass-panel">
          <div class="panel-header">
            <h2>冥想背景音</h2>
            <p>选择符合当下情绪的背景音</p>
          </div>

          <div class="emotion-scroll-container" ref="emotionScrollRef" @wheel.prevent="handleWheel">
            <button 
              v-for="emotion in emotions" 
              :key="emotion.id"
              :class="['emotion-btn', { active: currentTrack?.id === emotion.id }]"
              @click="selectTrack(emotion)"
            >
              <span class="emotion-name">{{ emotion.name }}</span>
              <span class="emotion-desc">{{ emotion.title }}</span>
            </button>
          </div>

          <div class="guide-section">
            <button class="guide-btn" :class="{ active: showGuideText }" @click="toggleGuide">
              <span class="guide-icon">✨</span>
              <span>冥想引导</span>
            </button>
            <transition name="fade-slide">
              <div class="guide-text-box" v-if="showGuideText">
                <p v-if="isGuideLoading">正在生成与你当前情绪匹配的冥想引导...</p>
                <p v-else>{{ guideText }}</p>
              </div>
            </transition>
          </div>

          <div class="player-controls" role="button" tabindex="0" @click="openPlayerPage" @keyup.enter="openPlayerPage">
            <div class="now-playing-info" v-if="currentTrack">
              <strong>{{ currentTrack.name }}</strong>
              <span>{{ formatTime(progressSeconds) }} / {{ formatTime(currentTrack.duration) }}</span>
            </div>
            <div class="now-playing-info" v-else>
              <strong>未选择音乐</strong>
              <span>00:00 / 00:00</span>
            </div>

            <div class="control-buttons">
              <button class="ctrl-btn" @click.stop="playPrevious" :disabled="!currentTrack">
                <font-awesome-icon icon="backward-step" />
              </button>
              <button class="ctrl-btn play-btn" @click.stop="togglePlayback" :disabled="!currentTrack">
                <font-awesome-icon :icon="isPlaying ? 'pause' : 'play'" />
              </button>
              <button class="ctrl-btn" @click.stop="playNext" :disabled="!currentTrack">
                <font-awesome-icon icon="forward-step" />
              </button>
            </div>
          </div>
        </div>

        <!-- Far Right: Plant Growth -->
        <div class="plant-panel glass-panel">
          <div class="panel-header plant-header">
            <div>
              <h2>冥想森林</h2>
              <p>记录你的专注时光</p>
            </div>
            <button class="info-btn" @click="showPlantInfo = true">
              <font-awesome-icon icon="circle-info" />
            </button>
          </div>
          
          <div class="plant-display">
            <div class="plant-stage">
              <span class="plant-emoji">{{ currentPlantEmoji }}</span>
            </div>
            
            <div class="plant-stats">
              <div class="stat-item">
                <span class="stat-label">总冥想时间</span>
                <span class="stat-value"><strong>{{ totalMeditationTime }}</strong> 分钟</span>
              </div>
              <div class="stat-item">
                <span class="stat-label">当前植物</span>
                <span class="stat-value"><strong>{{ currentTreeName }}</strong> ({{ currentStageName }})</span>
              </div>
              <div class="stat-item" v-if="fruits > 0">
                <span class="stat-label">已结果实</span>
                <span class="stat-value fruit-emojis">{{ '🍎'.repeat(fruits) }}</span>
              </div>
            </div>
            
            <div class="progress-section">
              <div class="progress-bar-container">
                <div class="progress-bar" :style="{ width: progressPercentage + '%' }"></div>
              </div>
              <p class="next-stage-text">{{ nextStageText }}</p>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Plant Info Modal -->
    <transition name="fade">
      <div class="modal-overlay" v-if="showPlantInfo" @click.self="showPlantInfo = false">
        <div class="modal-content glass-panel">
          <button class="close-btn" @click="showPlantInfo = false">
            <font-awesome-icon icon="xmark" />
          </button>
          <h3>🌲 种植说明</h3>
          <p class="modal-desc">每次冥想的时间都会转化为植物的生长养分：</p>
          <ul class="growth-rules">
            <li><span class="rule-time">0-10分钟</span><span class="rule-stage">播种 🪴</span></li>
            <li><span class="rule-time">10-30分钟</span><span class="rule-stage">发芽 🌱</span></li>
            <li><span class="rule-time">30-50分钟</span><span class="rule-stage">展叶 🌿</span></li>
            <li><span class="rule-time">50-70分钟</span><span class="rule-stage">小树 🌳</span></li>
            <li><span class="rule-time">70-100分钟</span><span class="rule-stage">大树 🌲</span></li>
            <li><span class="rule-time">100分钟以上</span><span class="rule-stage">每20分钟结一次果 🍎</span></li>
          </ul>
          <div class="modal-footer-text">
            当结满3次果实后，植物将化作养分，并为你更换新的神秘树种！
          </div>
          <button class="modal-confirm-btn" @click="showPlantInfo = false">我知道了</button>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import axios from 'axios'
import CircleTimer from '@/components/CircleTimer.vue'

const PLAYER_SESSION_KEY = 'emotion-system-active-player'

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

// Plant Growth Logic Mock
const totalMeditationTime = ref(45) // mock: 45 minutes

const TREE_CYCLE_TIME = 160 // 100 mins to mature + 3 * 20 mins for 3 fruits

const currentTreeTime = computed(() => totalMeditationTime.value % TREE_CYCLE_TIME)
const completedTrees = computed(() => Math.floor(totalMeditationTime.value / TREE_CYCLE_TIME))

const treeTypes = ['橡树', '枫树', '银杏', '樱花树', '松树']
const currentTreeName = computed(() => treeTypes[completedTrees.value % treeTypes.length])
const showPlantInfo = ref(false)

const plantStages = [
  { time: 0, name: '播种', emoji: '🪴' },
  { time: 10, name: '发芽', emoji: '🌱' },
  { time: 30, name: '展叶', emoji: '🌿' },
  { time: 50, name: '小树', emoji: '🌳' },
  { time: 70, name: '大树', emoji: '🌲' },
  { time: 100, name: '成熟', emoji: '🌳✨' }
]

const currentStage = computed(() => {
  let stage = plantStages[0]
  for (const s of plantStages) {
    if (currentTreeTime.value >= s.time) {
      stage = s
    }
  }
  return stage
})

const currentPlantEmoji = computed(() => currentStage.value.emoji)
const currentStageName = computed(() => currentStage.value.name)

const fruits = computed(() => {
  if (currentTreeTime.value < 100) return 0
  const extraTime = currentTreeTime.value - 100
  const fruitCount = Math.floor(extraTime / 20)
  return Math.min(fruitCount, 3)
})

const progressPercentage = computed(() => {
  if (currentTreeTime.value >= 160) return 100
  if (currentTreeTime.value >= 100) {
     const extraTime = currentTreeTime.value - 100
     const timeInCurrentStage = extraTime % 20
     return (timeInCurrentStage / 20) * 100
  }
  let current = plantStages[0]
  let next = plantStages[1]
  for (let i = 0; i < plantStages.length - 1; i++) {
    if (currentTreeTime.value >= plantStages[i].time && currentTreeTime.value < plantStages[i+1].time) {
      current = plantStages[i]
      next = plantStages[i+1]
      break
    }
  }
  const stageDuration = next.time - current.time
  const timeInStage = currentTreeTime.value - current.time
  return (timeInStage / stageDuration) * 100
})

const nextStageText = computed(() => {
  if (currentTreeTime.value >= 100) {
    const extraTime = currentTreeTime.value - 100
    if (extraTime >= 60) return '即将化作养分，孕育新的生命！'
    const timeToNextFruit = 20 - (extraTime % 20)
    return `距离结出下一颗果实还需 ${timeToNextFruit} 分钟`
  }
  let next = plantStages[1]
  for (let i = 0; i < plantStages.length; i++) {
    if (currentTreeTime.value < plantStages[i].time) {
      next = plantStages[i]
      break
    }
  }
  const timeToNext = next.time - currentTreeTime.value
  return `距离下一阶段【${next.name}】还需 ${timeToNext} 分钟`
})

// Timer Logic
const timeOptions = [180, 300, 600, 1200] // 3min, 5min, 10min, 20min
const selectedTime = ref(300)

const selectTime = (option) => {
  selectedTime.value = option
}

const handleTimerComplete = () => {
  const meditatedMins = Math.floor(selectedTime.value / 60)
  totalMeditationTime.value += meditatedMins
}

const showGuideText = ref(false)
const isGuideLoading = ref(false)
const guideText = ref('请选择一种冥想背景音，我会为你生成对应的冥想引导。')

const fetchMeditationGuide = async (emotionName) => {
  isGuideLoading.value = true
  try {
    const { data } = await axios.post('http://127.0.0.1:5000/api/meditation/guide', {
      emotion: emotionName,
    })
    guideText.value = data?.guide || '当前引导词生成失败，请稍后重试。'
  } catch (error) {
    console.error(error)
    guideText.value = '冥想引导服务暂时不可用，请稍后再试。'
    ElMessage.error('冥想引导生成失败，请检查后端服务与 Kimi API Key。')
  } finally {
    isGuideLoading.value = false
  }
}

const toggleGuide = () => {
  showGuideText.value = !showGuideText.value
  if (showGuideText.value && currentTrack.value) {
    fetchMeditationGuide(currentTrack.value.name)
  }
}

// Emotion Scroll Logic
const emotionScrollRef = ref(null)

const handleWheel = (e) => {
  if (emotionScrollRef.value) {
    // Translate vertical scroll (deltaY) to horizontal scroll
    const scrollAmount = e.deltaY > 0 ? 300 : -300
    emotionScrollRef.value.scrollBy({
      left: scrollAmount,
      behavior: 'smooth'
    })
  }
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

  if (showGuideText.value) {
    fetchMeditationGuide(track.name)
  }
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

const normalizeMeditationTrack = (track) => ({
  id: track.id,
  title: track.title,
  artist: track.artist?.trim() || 'Emotion Healing',
  duration: track.duration || 0,
  cover: `/images/feature-img-${(emotions.findIndex(item => item.id === track.id) % 4 + 1)}.jpg`,
  tags: [track.name, '冥想', '背景音'],
  type: '冥想背景音'
})

const openPlayerPage = () => {
  if (!currentTrack.value) return

  window.sessionStorage.setItem(PLAYER_SESSION_KEY, JSON.stringify({
    source: 'meditation',
    returnTo: '/meditation-room',
    categoryName: '冥想背景音',
    track: normalizeMeditationTrack(currentTrack.value),
    queue: emotions.map(item => normalizeMeditationTrack(item))
  }))

  router.push({ name: 'music-player' })
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
  width: 100%;
  overflow: hidden;
  background-color: var(--color-bg-primary);
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 20px;
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
  height: 100%;
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
  grid-template-columns: 1.1fr 1.1fr 0.8fr;
  gap: 30px;
  overflow: hidden;
}

.glass-panel {
  background: rgba(255, 255, 255, 0.6);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.6);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-float);
  padding: 30px;
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
  margin-bottom: 20px;
  width: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
  flex: 1;
  min-height: 0;
}

.timer-wrapper :deep(.circle-timer) {
  width: 100%;
  height: auto;
  max-width: 440px;
  max-height: 440px;
  aspect-ratio: 1 / 1;
}

.time-options {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
  justify-content: center;
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

.guide-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 16px;
  flex: 1;
}

.guide-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 10px 28px;
  background: linear-gradient(135deg, var(--color-accent-sage), #8ca595);
  color: #fff;
  border: none;
  border-radius: var(--radius-pill);
  font-size: 1rem;
  font-weight: 600;
  box-shadow: 0 6px 16px rgba(124, 152, 133, 0.3);
  transition: all var(--transition-fast);
  cursor: pointer;
  z-index: 2;
}

.guide-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(124, 152, 133, 0.4);
}

.guide-btn.active {
  background: linear-gradient(135deg, var(--color-accent-terracotta), #c88a75);
  box-shadow: 0 6px 16px rgba(200, 138, 117, 0.3);
}

.guide-icon {
  font-size: 1.1rem;
}

.guide-text-box {
  margin-top: 12px;
  padding: 16px 20px;
  background: rgba(255, 255, 255, 0.7);
  border: 1px solid rgba(255, 255, 255, 0.8);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-soft);
  backdrop-filter: blur(10px);
  text-align: justify;
  line-height: 1.6;
  color: var(--color-text-primary);
  font-size: 0.9rem;
  width: 100%;
  max-width: 100%;
  flex: 1;
  overflow-y: auto;
}

.fade-slide-enter-active,
.fade-slide-leave-active {
  transition: all 0.4s cubic-bezier(0.175, 0.885, 0.32, 1.275);
}

.fade-slide-enter-from,
.fade-slide-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}

/* Playlist Panel */
.playlist-panel {
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.panel-header {
  flex-shrink: 0;
}

.panel-header h2 {
  font-family: var(--font-serif);
  font-size: 1.6rem;
  margin: 0 0 8px 0;
  color: var(--color-text-primary);
}

.panel-header p {
  color: var(--color-text-secondary);
  margin: 0 0 16px 0;
}

.emotion-scroll-container {
  flex: 0 0 auto;
  display: flex;
  gap: 16px;
  align-items: center;
  overflow-x: auto;
  padding: 12px 8px;
  margin-bottom: 8px;
  -ms-overflow-style: none;
  scrollbar-width: none;
  scroll-snap-type: x mandatory;
}

.emotion-scroll-container::-webkit-scrollbar {
  display: none;
}

.emotion-btn {
  flex: 0 0 auto;
  width: 140px;
  height: 120px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  gap: 12px;
  background: rgba(255, 255, 255, 0.4);
  border: 2px solid rgba(255, 255, 255, 0.5);
  border-radius: var(--radius-xl);
  color: var(--color-text-primary);
  cursor: pointer;
  transition: all 0.4s cubic-bezier(0.175, 0.885, 0.32, 1.275);
  scroll-snap-align: center;
  backdrop-filter: blur(10px);
}

.emotion-name {
  font-size: 1.2rem;
  font-weight: 600;
  font-family: var(--font-serif);
}

.emotion-desc {
  font-size: 0.85rem;
  color: var(--color-text-secondary);
  opacity: 0.8;
  transition: all 0.3s;
}

.emotion-btn:hover {
  background: rgba(255, 255, 255, 0.8);
  transform: translateY(-5px) scale(1.02);
  box-shadow: 0 10px 20px rgba(44, 48, 46, 0.1);
  border-color: rgba(255, 255, 255, 0.8);
}

.emotion-btn.active {
  background: rgba(255, 255, 255, 0.9);
  border-color: var(--color-accent-terracotta);
  transform: translateY(-5px) scale(1.02);
  box-shadow: 0 10px 20px rgba(217, 122, 108, 0.2);
}

.emotion-btn.active .emotion-name {
  color: var(--color-accent-terracotta);
}

.emotion-btn.active .emotion-desc {
  opacity: 1;
  color: var(--color-accent-terracotta);
}

.player-controls {
  margin-top: auto;
  background: rgba(44, 48, 46, 0.85);
  backdrop-filter: blur(10px);
  border-radius: var(--radius-lg);
  padding: 16px 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  color: #fff;
  flex-shrink: 0;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.player-controls:hover {
  transform: translateY(-2px);
  box-shadow: 0 18px 34px rgba(44, 48, 46, 0.16);
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

/* Plant Panel */
.plant-panel {
  display: flex;
  flex-direction: column;
}

.plant-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.info-btn {
  background: transparent;
  border: none;
  color: var(--color-text-secondary);
  font-size: 1.4rem;
  cursor: pointer;
  transition: all 0.3s ease;
  padding: 4px;
}

.info-btn:hover {
  color: var(--color-accent-terracotta);
  transform: scale(1.1);
}

.plant-display {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding-top: 10px;
}

.plant-stage {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 180px;
  background: rgba(255, 255, 255, 0.3);
  border-radius: var(--radius-xl);
  margin-bottom: 20px;
  border: 1px dashed rgba(255, 255, 255, 0.5);
  box-shadow: inset 0 0 20px rgba(255, 255, 255, 0.2);
}

.plant-emoji {
  font-size: 6rem;
  filter: drop-shadow(0 10px 10px rgba(0,0,0,0.1));
  animation: floatGentle 4s ease-in-out infinite alternate;
}

.plant-stats {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 20px;
}

.stat-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: rgba(255, 255, 255, 0.5);
  padding: 12px 16px;
  border-radius: var(--radius-lg);
  font-size: 0.95rem;
}

.stat-label {
  color: var(--color-text-secondary);
}

.stat-value {
  color: var(--color-text-primary);
}

.fruit-emojis {
  font-size: 1.2rem;
  letter-spacing: 2px;
}

.progress-section {
  margin-top: auto;
}

.progress-bar-container {
  height: 8px;
  background: rgba(44, 48, 46, 0.1);
  border-radius: var(--radius-pill);
  overflow: hidden;
  margin-bottom: 8px;
}

.progress-bar {
  height: 100%;
  background: linear-gradient(90deg, var(--color-accent-sage), var(--color-accent-terracotta));
  border-radius: var(--radius-pill);
  transition: width 0.5s ease-in-out;
}

.next-stage-text {
  font-size: 0.85rem;
  color: var(--color-text-secondary);
  text-align: center;
  margin: 0;
}

/* Modal */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background: rgba(0, 0, 0, 0.3);
  backdrop-filter: blur(5px);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.modal-content {
  width: 90%;
  max-width: 450px;
  padding: 30px;
  position: relative;
}

.close-btn {
  position: absolute;
  top: 20px;
  right: 20px;
  background: transparent;
  border: none;
  font-size: 1.2rem;
  color: var(--color-text-secondary);
  cursor: pointer;
  transition: color 0.3s;
}

.close-btn:hover {
  color: var(--color-text-primary);
}

.modal-content h3 {
  font-family: var(--font-serif);
  font-size: 1.4rem;
  margin-top: 0;
  margin-bottom: 16px;
  color: var(--color-text-primary);
}

.modal-desc {
  color: var(--color-text-secondary);
  font-size: 0.95rem;
  margin-bottom: 20px;
}

.growth-rules {
  list-style: none;
  padding: 0;
  margin: 0 0 20px 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.growth-rules li {
  display: flex;
  justify-content: space-between;
  background: rgba(255, 255, 255, 0.4);
  padding: 10px 16px;
  border-radius: var(--radius-lg);
  font-size: 0.95rem;
}

.rule-time {
  color: var(--color-text-secondary);
}

.rule-stage {
  font-weight: bold;
  color: var(--color-text-primary);
}

.modal-footer-text {
  font-size: 0.9rem;
  color: var(--color-accent-terracotta);
  text-align: center;
  margin-bottom: 24px;
  font-weight: 500;
}

.modal-confirm-btn {
  width: 100%;
  padding: 12px;
  background: var(--color-text-primary);
  color: #fff;
  border: none;
  border-radius: var(--radius-pill);
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;
}

.modal-confirm-btn:hover {
  background: var(--color-accent-sage);
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
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
