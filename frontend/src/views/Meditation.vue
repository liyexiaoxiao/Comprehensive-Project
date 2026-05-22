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
              :is-running="isMeditationRunning"
              :reset-token="timerResetToken"
              :colors="['#D97A6C', '#8ca595']" 
              @tick="handleTimerTick"
              @complete="handleTimerComplete"
            />
          </div>

          <p class="timer-status-text">{{ timerStatusText }}</p>
          
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

          <div class="custom-time-row">
            <input
              v-model.number="customDurationMinutes"
              type="number"
              min="1"
              max="180"
              step="1"
              class="custom-time-input"
              placeholder="自定义分钟数"
            />
            <button class="apply-time-btn" @click="applyCustomDuration">应用时长</button>
          </div>

          <div class="timer-action-row">
            <button
              class="timer-action-btn primary"
              @click="startMeditation"
              :disabled="isMeditationRunning || isRoundCompleted"
            >
              开始
            </button>
            <button
              class="timer-action-btn"
              @click="pauseMeditation"
              :disabled="!isMeditationRunning"
            >
              暂停
            </button>
            <button
              class="timer-action-btn"
              @click="restartMeditation"
            >
              重新开始
            </button>
          </div>
        </div>

        <div class="right-panels-shell">
          <div class="right-panels-grid">
            <!-- Right: Playlist and Player -->
            <div class="playlist-panel glass-panel">
              <div class="panel-header">
                <h2>冥想背景音</h2>
                <p>选择系统官方情绪，循环播放对应的官方歌单</p>
              </div>

              <div class="emotion-scroll-container" ref="emotionScrollRef" @wheel.prevent="handleWheel">
                <button 
                  v-for="emotion in emotions" 
                  :key="emotion.id"
                  :class="['emotion-btn', { active: activeEmotionPlaylistId === emotion.id }]"
                  @click="selectEmotionPlaylist(emotion)"
                >
                  <span class="emotion-name">{{ emotion.tagName }}</span>
                  <span class="emotion-desc">{{ emotion.tracks.length ? `${emotion.tracks.length} 首官方歌曲` : '歌单待补充' }}</span>
                </button>
              </div>

              <div class="guide-section">
                <button class="guide-btn" :class="{ active: guideModalVisible }" @click="openGuideModal">
                  <span class="guide-icon">✨</span>
                  <span>{{ guideModalVisible ? '重新生成引导' : '冥想引导' }}</span>
                </button>
                <p class="guide-hint">生成后会以逐句字幕和语音播报的方式播放</p>
              </div>

              <div class="player-controls" role="button" tabindex="0" @click="openPlayerPage" @keyup.enter="openPlayerPage">
                <div class="now-playing-info" v-if="currentTrack">
                  <strong>{{ currentTrack.title }}</strong>
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

          <transition name="guide-overlay-fade">
            <div class="guide-overlay" v-if="guideModalVisible">
              <div class="guide-modal-card" @click.stop>
                <div class="guide-modal-header">
                  <div>
                    <p class="guide-modal-kicker">冥想引导</p>
                    <h3 class="guide-modal-title">{{ getGuideEmotionName() }} 情绪引导</h3>
                  </div>
                  <button class="close-btn guide-modal-close" @click="closeGuideModal">
                    <font-awesome-icon icon="xmark" />
                  </button>
                </div>

                <div class="guide-modal-toolbar">
                  <span :class="['guide-status-pill', guideStatusClass]">{{ guideStatusText }}</span>
                  <span class="guide-progress-text" v-if="guideLines.length">
                    第 {{ currentGuideLineDisplay }} / {{ guideLines.length }} 句
                  </span>
                </div>

                <div class="guide-lyrics-area">
                  <div class="guide-placeholder" v-if="isGuideLoading">
                    正在生成与你当前情绪匹配的冥想引导...
                  </div>
                  <div class="guide-placeholder" v-else-if="!guideLines.length">
                    {{ guideText }}
                  </div>
                  <div class="guide-lines" v-else ref="guideLinesContainerRef">
                    <p
                      v-for="(line, index) in guideLines"
                      :key="`${index}-${line}`"
                      :ref="(el) => setGuideLineRef(el, index)"
                      :class="[
                        'guide-line',
                        {
                          active: index === currentGuideLineIndex,
                          passed: index < currentGuideLineIndex,
                          upcoming: index > currentGuideLineIndex,
                        },
                      ]"
                    >
                      {{ line }}
                    </p>
                  </div>
                </div>

                <div class="guide-modal-actions">
                  <button class="guide-modal-btn" @click="regenerateGuide" :disabled="isGuideLoading">
                    重新生成
                  </button>
                  <button
                    class="guide-modal-btn primary"
                    @click="replayGuideNarration"
                    :disabled="isGuideLoading || !guideLines.length"
                  >
                    重播引导
                  </button>
                  <button class="guide-modal-btn" @click="closeGuideModal">
                    关闭
                  </button>
                </div>
              </div>
            </div>
          </transition>
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
import { ref, computed, onMounted, onBeforeUnmount, watch, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import CircleTimer from '@/components/CircleTimer.vue'
import { getCompanionTtsApi, getMeditationGuideApi, getAiAssetUrl } from '@/api/ai'
import { getOfficialPlaylistConfigsApi } from '@/api/music'
import { getMusicFileUrl } from '@/api/python'
import {
  completeMeditationCountdownApi,
  getMyMeditationLogsApi,
  rewardGardenItemApi,
  saveMeditationLogApi,
  startMeditationCountdownApi,
  stopMeditationCountdownApi,
} from '@/api/meditation'
import { useMusicStore } from '@/stores/musicStore'
import {
  buildOfficialEmotionPlaylists,
  getDefaultOfficialPlaylists,
  mergeOfficialPlaylistConfigs,
} from '@/utils/playlistCatalog'
import { getRealMusicCover, normalizeEmotion } from '@/utils/realMusic'

const PLAYER_SESSION_KEY = 'emotion-system-active-player'
const GUIDE_TTS_VOICE = 'claire'
const MEDITATION_BG_VOLUME = 0.72
const GUIDE_ACTIVE_BG_VOLUME = 0.2
const GUIDE_VOICE_VOLUME = 1
const GUIDE_EMOTION_REQUEST_MAP = {
  anger: '愤怒',
  calm: '平静',
  disgust: '愤怒',
  fear: '焦虑',
  joy: '喜悦',
  love: '喜悦',
  neutral: '平静',
  sadness: '悲伤',
  surprise: '充满希望',
}

const router = useRouter()
const musicStore = useMusicStore()
const officialPlaylistConfigs = ref(getDefaultOfficialPlaylists())
const activeEmotionPlaylistId = ref(officialPlaylistConfigs.value[0]?.id || '')

const emotions = computed(() => buildOfficialEmotionPlaylists(
  musicStore.publicTracks,
  officialPlaylistConfigs.value,
))

const activeEmotionPlaylist = computed(() =>
  emotions.value.find((playlist) => playlist.id === activeEmotionPlaylistId.value)
  || emotions.value[0]
  || null)

const currentPlaylistTracks = computed(() =>
  Array.isArray(activeEmotionPlaylist.value?.tracks) ? activeEmotionPlaylist.value.tracks : [])

const goBack = () => {
  router.push('/service')
}

// Plant Growth Logic Mock
const totalMeditationTime = ref(0) 

const fetchLogs = async () => {
  try {
    const res = await getMyMeditationLogsApi()
    if (res.data) {
      const total = res.data.reduce((sum, log) => sum + (log.duration || 0), 0)
      totalMeditationTime.value = total
    }
  } catch (e) {
    console.error('Failed to fetch meditation logs:', e)
  }
}

onMounted(() => {
  fetchLogs()
})

const loadOfficialMeditationPlaylists = async () => {
  try {
    const [officialConfigResponse] = await Promise.all([
      getOfficialPlaylistConfigsApi(),
      musicStore.fetchPublicTracks(),
    ])
    officialPlaylistConfigs.value = mergeOfficialPlaylistConfigs(officialConfigResponse.data)
    if (!emotions.value.some((playlist) => playlist.id === activeEmotionPlaylistId.value)) {
      activeEmotionPlaylistId.value = emotions.value[0]?.id || ''
    }
  } catch (error) {
    console.error('Failed to load official meditation playlists:', error)
    ElMessage.error('加载官方冥想歌单失败，请确认 music-service 已启动。')
  }
}

onMounted(() => {
  loadOfficialMeditationPlaylists()
})

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
const customDurationMinutes = ref(selectedTime.value / 60)
const isMeditationRunning = ref(false)
const isRoundCompleted = ref(false)
const timerResetToken = ref(0)
const roundStartedAt = ref(null)
const remainingSeconds = ref(selectedTime.value)

const handleTimerTick = (timeLeft) => {
  remainingSeconds.value = Math.max(0, Number(timeLeft) || 0)
}

const selectTime = (option) => {
  selectedTime.value = option
  customDurationMinutes.value = option / 60
  restartMeditation()
}

const timerStatusText = computed(() => {
  if (isMeditationRunning.value) {
    return '冥想进行中，倒计时结束后会自动保存记录。'
  }
  if (isRoundCompleted.value) {
    return '本轮已完成，当前停留在 00:00，请手动开始下一轮。'
  }
  return '点击“开始”后才会真正开始倒计时。'
})

const restartMeditation = async () => {
  if (isMeditationRunning.value) {
    try {
      await stopMeditationCountdownApi()
    } catch (e) {
      console.warn('Failed to stop countdown before restart', e)
    }
  }
  isMeditationRunning.value = false
  isRoundCompleted.value = false
  roundStartedAt.value = null
  remainingSeconds.value = selectedTime.value
  timerResetToken.value += 1
}

const clampCustomMinutes = (value) => {
  const normalized = Math.floor(Number(value) || 0)
  return Math.min(180, Math.max(1, normalized))
}

const applyCustomDuration = () => {
  const minutes = clampCustomMinutes(customDurationMinutes.value)
  customDurationMinutes.value = minutes
  selectedTime.value = minutes * 60
  restartMeditation()
}

const startMeditation = async () => {
  if (isMeditationRunning.value || isRoundCompleted.value) return
  if (!roundStartedAt.value) {
    roundStartedAt.value = new Date().toISOString()
  }
  try {
    await startMeditationCountdownApi({
      duration: remainingSeconds.value || selectedTime.value,
      musicId: currentTrack.value?.musicResourceId || Number(currentTrack.value?.id) || null,
      imageId: null,
    })
    isMeditationRunning.value = true
  } catch (e) {
    ElMessage.error('启动倒计时失败')
  }
}

const pauseMeditation = async () => {
  try {
    await stopMeditationCountdownApi()
  } catch (e) {
    console.warn('Failed to pause meditation countdown', e)
  }
  isMeditationRunning.value = false
}

const handleTimerComplete = async () => {
  isMeditationRunning.value = false
  isRoundCompleted.value = true
  const meditatedMins = Math.floor(selectedTime.value / 60)
  try {
    await completeMeditationCountdownApi()
    await saveMeditationLogApi({
      startTime: roundStartedAt.value || new Date().toISOString(),
      duration: meditatedMins,
      musicId: currentTrack.value?.id || null
    })
    totalMeditationTime.value += meditatedMins
    ElMessage.success('冥想记录已保存')
    
    // Reward a random garden item
    try {
      await rewardGardenItemApi()
      ElMessage.success('获得了一个冥想花园奖励！')
    } catch (e) {
      console.warn('Failed to reward garden item', e)
    }
  } catch (e) {
    ElMessage.error('保存记录失败')
  } finally {
    roundStartedAt.value = null
    remainingSeconds.value = 0
  }
}

const guideModalVisible = ref(false)
const isGuideLoading = ref(false)
const guideText = ref('请选择一种冥想背景音，我会为你生成对应的冥想引导。')
const guideLines = ref([])
const currentGuideLineIndex = ref(-1)
const isGuideSpeaking = ref(false)
const isGuidePreparingAudio = ref(false)
const guideSpeechError = ref('')
const guideLineRefs = ref([])
const guideLinesContainerRef = ref(null)
const guideSpeechPlayer = new Audio()
let guidePlaybackSession = 0
let guideRequestSerial = 0
let resolveGuideSpeechWait = null

const currentGuideLineDisplay = computed(() => {
  if (!guideLines.value.length || currentGuideLineIndex.value < 0) {
    return 0
  }
  return Math.min(currentGuideLineIndex.value + 1, guideLines.value.length)
})

const guideStatusText = computed(() => {
  if (isGuideLoading.value) return '引导生成中'
  if (guideSpeechError.value) return '语音播报异常'
  if (isGuidePreparingAudio.value) return '正在准备语音'
  if (isGuideSpeaking.value) return '语音播报中'
  if (guideLines.value.length && currentGuideLineIndex.value === guideLines.value.length - 1) {
    return '本轮播报完成'
  }
  return '等待开始'
})

const guideStatusClass = computed(() => {
  if (isGuideLoading.value || isGuidePreparingAudio.value) return 'loading'
  if (guideSpeechError.value) return 'error'
  if (isGuideSpeaking.value) return 'speaking'
  return 'idle'
})

const getGuideEmotionName = () =>
  activeEmotionPlaylist.value?.tagName
  || currentTrack.value?.tags?.[0]
  || currentTrack.value?.emotion
  || '平静'

const getGuideRequestEmotion = () => {
  const rawEmotion = getGuideEmotionName()
  const normalizedEmotion = normalizeEmotion(rawEmotion)
  return GUIDE_EMOTION_REQUEST_MAP[normalizedEmotion] || rawEmotion || '平静'
}

const splitGuideIntoLines = (text) => {
  const normalized = String(text || '').replace(/\r/g, '\n')
  const lines = normalized
    .split('\n')
    .flatMap((paragraph) => paragraph.split(/(?<=[。！？!?；;])/))
    .map((line) => line.trim())
    .filter(Boolean)

  return lines.length ? lines : [String(text || '').trim()].filter(Boolean)
}

const setGuideLineRef = (element, index) => {
  if (element) {
    guideLineRefs.value[index] = element
  }
}

const clearGuideSpeechWaiter = () => {
  guideSpeechPlayer.onended = null
  guideSpeechPlayer.onerror = null
  resolveGuideSpeechWait = null
}

const syncMeditationAudioMix = () => {
  guideSpeechPlayer.volume = GUIDE_VOICE_VOLUME
  audioPlayer.volume = (guideModalVisible.value && (isGuideSpeaking.value || isGuidePreparingAudio.value))
    ? GUIDE_ACTIVE_BG_VOLUME
    : MEDITATION_BG_VOLUME
}

const scrollGuideLineIntoView = (index, behavior = 'smooth') => {
  const container = guideLinesContainerRef.value
  const line = guideLineRefs.value[index]
  if (!container || !line) return

  const targetTop = line.offsetTop - ((container.clientHeight - line.offsetHeight) / 2)
  const maxScrollTop = Math.max(container.scrollHeight - container.clientHeight, 0)
  container.scrollTo({
    top: Math.min(Math.max(targetTop, 0), maxScrollTop),
    behavior,
  })
}

const waitForGuideSpeechToFinish = () => new Promise((resolve, reject) => {
  clearGuideSpeechWaiter()
  resolveGuideSpeechWait = resolve
  guideSpeechPlayer.onended = () => {
    clearGuideSpeechWaiter()
    resolve()
  }
  guideSpeechPlayer.onerror = () => {
    clearGuideSpeechWaiter()
    reject(new Error('guide speech playback failed'))
  }
})

const stopGuidePlayback = (resetLine = false) => {
  guidePlaybackSession += 1
  if (resolveGuideSpeechWait) {
    const resolve = resolveGuideSpeechWait
    clearGuideSpeechWaiter()
    resolve()
  }
  guideSpeechPlayer.pause()
  guideSpeechPlayer.src = ''
  isGuideSpeaking.value = false
  isGuidePreparingAudio.value = false
  syncMeditationAudioMix()
  if (resetLine) {
    currentGuideLineIndex.value = -1
  }
}

const playGuideLineAudio = async (line, playbackSession) => {
  const { data } = await getCompanionTtsApi({
    text: line,
    tts_voice: GUIDE_TTS_VOICE,
  })

  if (playbackSession !== guidePlaybackSession || !guideModalVisible.value) {
    return
  }

  if (data?.tts_error) {
    throw new Error(data.tts_error)
  }

  const audioUrl = getAiAssetUrl(data?.audio_url)
  if (!audioUrl) {
    throw new Error('TTS 未返回可播放音频')
  }

  guideSpeechPlayer.pause()
  guideSpeechPlayer.src = audioUrl
  guideSpeechPlayer.currentTime = 0
  syncMeditationAudioMix()
  await guideSpeechPlayer.play()
  await waitForGuideSpeechToFinish()
}

const startGuideNarration = async (startIndex = 0) => {
  if (!guideLines.value.length || !guideModalVisible.value) return

  stopGuidePlayback(false)
  guideSpeechError.value = ''
  const playbackSession = guidePlaybackSession
  isGuideSpeaking.value = true

  if (startIndex <= 0) {
    await nextTick()
    if (guideLinesContainerRef.value) {
      guideLinesContainerRef.value.scrollTop = 0
    }
  }

  for (let index = Math.max(0, startIndex); index < guideLines.value.length; index += 1) {
    if (playbackSession !== guidePlaybackSession || !guideModalVisible.value) {
      if (playbackSession === guidePlaybackSession) {
        isGuideSpeaking.value = false
        isGuidePreparingAudio.value = false
      }
      return
    }

    currentGuideLineIndex.value = index
    isGuidePreparingAudio.value = true

    try {
      await playGuideLineAudio(guideLines.value[index], playbackSession)
    } catch (error) {
      if (playbackSession !== guidePlaybackSession) {
        return
      }
      console.error('Failed to play guide narration:', error)
      guideSpeechError.value = '语音播报失败'
      isGuideSpeaking.value = false
      isGuidePreparingAudio.value = false
      ElMessage.error('冥想引导语音播放失败，请检查 AI 服务 TTS 配置。')
      return
    } finally {
      if (playbackSession === guidePlaybackSession) {
        isGuidePreparingAudio.value = false
      }
    }
  }

  if (playbackSession === guidePlaybackSession) {
    isGuideSpeaking.value = false
    isGuidePreparingAudio.value = false
  }
}

const fetchMeditationGuide = async (emotionName) => {
  const requestSerial = ++guideRequestSerial
  guideModalVisible.value = true
  isGuideLoading.value = true
  guideSpeechError.value = ''
  guideLines.value = []
  stopGuidePlayback(true)
  try {
    const { data } = await getMeditationGuideApi({
      emotion: emotionName,
    })
    if (requestSerial !== guideRequestSerial) return
    guideText.value = data?.guide || '当前引导词生成失败，请稍后重试。'
    guideLines.value = splitGuideIntoLines(guideText.value)
    currentGuideLineIndex.value = guideLines.value.length ? 0 : -1
    if (guideLines.value.length) {
      isGuideLoading.value = false
      await nextTick()
      await startGuideNarration(0)
    }
  } catch (error) {
    if (requestSerial !== guideRequestSerial) return
    console.error(error)
    guideLines.value = []
    guideText.value = '冥想引导服务暂时不可用，请稍后再试。'
    ElMessage.error(error?.response?.data?.error || '冥想引导生成失败，请检查 AI 服务与 Kimi API Key。')
  } finally {
    if (requestSerial === guideRequestSerial) {
      isGuideLoading.value = false
    }
  }
}

const openGuideModal = () => {
  fetchMeditationGuide(getGuideRequestEmotion())
}

const closeGuideModal = () => {
  guideRequestSerial += 1
  guideModalVisible.value = false
  stopGuidePlayback(false)
}

const regenerateGuide = () => {
  fetchMeditationGuide(getGuideRequestEmotion())
}

const replayGuideNarration = () => {
  guideSpeechError.value = ''
  startGuideNarration(0)
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
const audioPlayer = new Audio()

const createMeditationPlayerTrack = (track, playlist = activeEmotionPlaylist.value) => ({
  id: track.id,
  musicResourceId: track.musicResourceId ?? null,
  title: track.title,
  artist: track.artist?.trim() || playlist?.tagName || 'Emotion Healing',
  duration: Number(track.duration) || 0,
  cover: track.cover || getRealMusicCover(track.emotion || playlist?.coverEmotion || 'neutral'),
  tags: Array.isArray(track.tags) && track.tags.length
    ? [...track.tags]
    : [playlist?.tagName || '冥想', '官方歌单'],
  type: '冥想背景音',
  emotion: track.emotion || playlist?.coverEmotion || 'neutral',
  filename: track.filename || '',
  fileUrl: track.fileUrl || '',
  source: track.source || '',
})

const formatTime = (seconds) => {
  const safeValue = Number.isFinite(seconds) ? seconds : 0
  const m = Math.floor(safeValue / 60)
  const s = Math.floor(safeValue % 60)
  return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
}

const stopAudioPlayback = () => {
  audioPlayer.pause()
  audioPlayer.currentTime = 0
  progressSeconds.value = 0
  isPlaying.value = false
}

const resolveTrackSource = (track) => {
  if (track?.fileUrl) {
    return track.fileUrl
  }
  if (track?.filename) {
    return getMusicFileUrl(track.filename)
  }
  return ''
}

const playAudioForTrack = async (track) => {
  const sourceUrl = resolveTrackSource(track)
  if (!sourceUrl) {
    isPlaying.value = false
    ElMessage.warning('当前曲目缺少可播放的音频地址')
    return
  }

  try {
    audioPlayer.pause()
    audioPlayer.src = sourceUrl
    audioPlayer.currentTime = 0
    progressSeconds.value = 0
    syncMeditationAudioMix()
    await audioPlayer.play()
    isPlaying.value = true
  } catch (error) {
    isPlaying.value = false
    console.error('Failed to play meditation track:', error)
    ElMessage.error('背景音播放失败，请确认音乐服务已启动且资源可访问。')
  }
}

const selectTrack = async (track, playlist = activeEmotionPlaylist.value) => {
  currentTrack.value = createMeditationPlayerTrack(track, playlist)
  progressSeconds.value = 0
  await playAudioForTrack(currentTrack.value)

  if (guideModalVisible.value && playlist) {
    fetchMeditationGuide(
      GUIDE_EMOTION_REQUEST_MAP[normalizeEmotion(playlist.tagName || track.tags?.[0] || track.emotion || '平静')]
      || playlist.tagName
      || track.tags?.[0]
      || track.emotion
      || '平静',
    )
  }
}

const clearCurrentTrack = () => {
  currentTrack.value = null
  stopAudioPlayback()
}

const selectEmotionPlaylist = async (playlist) => {
  activeEmotionPlaylistId.value = playlist.id
  if (!playlist?.tracks?.length) {
    clearCurrentTrack()
    if (guideModalVisible.value) {
      fetchMeditationGuide(
        GUIDE_EMOTION_REQUEST_MAP[normalizeEmotion(playlist?.tagName || '平静')]
        || playlist?.tagName
        || '平静',
      )
    }
    ElMessage.info('当前官方情绪歌单暂无曲目')
    return
  }
  await selectTrack(playlist.tracks[0], playlist)
}

const togglePlayback = async () => {
  if (!currentTrack.value) return

  if (!audioPlayer.src) {
    await playAudioForTrack(currentTrack.value)
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
    isPlaying.value = false
    console.error('Failed to resume meditation track:', error)
    ElMessage.error('无法继续播放当前背景音。')
  }
}

const playNext = async () => {
  const tracks = currentPlaylistTracks.value
  if (!currentTrack.value || !tracks.length) return
  const idx = tracks.findIndex((track) => String(track.id) === String(currentTrack.value.id))
  const nextIdx = idx >= 0 ? (idx + 1) % tracks.length : 0
  await selectTrack(tracks[nextIdx], activeEmotionPlaylist.value)
}

const playPrevious = async () => {
  const tracks = currentPlaylistTracks.value
  if (!currentTrack.value || !tracks.length) return
  const idx = tracks.findIndex((track) => String(track.id) === String(currentTrack.value.id))
  const prevIdx = idx > 0 ? idx - 1 : tracks.length - 1
  await selectTrack(tracks[prevIdx], activeEmotionPlaylist.value)
}

const openPlayerPage = () => {
  const playlist = activeEmotionPlaylist.value
  const queueTracks = Array.isArray(playlist?.tracks) ? playlist.tracks : []
  if (!currentTrack.value || !playlist || !queueTracks.length) return

  window.sessionStorage.setItem(PLAYER_SESSION_KEY, JSON.stringify({
    source: 'meditation',
    returnTo: '/meditation-room',
    categoryId: playlist.id,
    categoryName: playlist.name,
    track: currentTrack.value,
    queue: queueTracks.map((item) => createMeditationPlayerTrack(item, playlist)),
  }))

  router.push({ name: 'music-player' })
}

onMounted(() => {
  // Optionally select the first track automatically
  // selectTrack(activeCategory.value.tracks[0])
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
  progressSeconds.value = audioPlayer.currentTime || 0
}

audioPlayer.onpause = () => {
  if (!audioPlayer.ended) {
    isPlaying.value = false
  }
}

audioPlayer.onplay = () => {
  isPlaying.value = true
}

audioPlayer.onended = () => {
  playNext()
}

onBeforeUnmount(() => {
  stopGuidePlayback(false)
  clearGuideSpeechWaiter()
  guideSpeechPlayer.pause()
  guideSpeechPlayer.src = ''
  audioPlayer.pause()
  audioPlayer.src = ''
})

watch(guideLines, () => {
  guideLineRefs.value = []
  if (guideLinesContainerRef.value) {
    guideLinesContainerRef.value.scrollTop = 0
  }
})

watch(currentGuideLineIndex, async (index) => {
  if (index < 0) return
  await nextTick()
  scrollGuideLineIntoView(index, index <= 1 ? 'auto' : 'smooth')
})

watch([guideModalVisible, isGuideSpeaking, isGuidePreparingAudio], () => {
  syncMeditationAudioMix()
}, { immediate: true })
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
  grid-template-columns: 1.05fr 1.95fr;
  gap: 30px;
  overflow: hidden;
}

.right-panels-shell {
  position: relative;
  min-width: 0;
}

.right-panels-grid {
  height: 100%;
  display: grid;
  grid-template-columns: 1.1fr 0.8fr;
  gap: 30px;
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

.timer-status-text {
  margin: -8px 0 0;
  text-align: center;
  color: var(--color-text-secondary);
  font-size: 0.95rem;
  line-height: 1.6;
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

.custom-time-row,
.timer-action-row {
  width: 100%;
  display: flex;
  gap: 14px;
  justify-content: center;
  flex-wrap: wrap;
}

.custom-time-input {
  width: 160px;
  padding: 10px 14px;
  border: 1px solid rgba(44, 48, 46, 0.12);
  border-radius: var(--radius-pill);
  background: rgba(255, 255, 255, 0.78);
  color: var(--color-text-primary);
  text-align: center;
  font-size: 1rem;
}

.custom-time-input:focus {
  outline: none;
  border-color: var(--color-accent-sage);
  background: #fff;
}

.apply-time-btn,
.timer-action-btn {
  padding: 10px 22px;
  border: 1px solid rgba(44, 48, 46, 0.12);
  border-radius: var(--radius-pill);
  background: rgba(255, 255, 255, 0.7);
  color: var(--color-text-primary);
  font-weight: 600;
  transition: all var(--transition-fast);
}

.apply-time-btn:hover,
.timer-action-btn:hover:not(:disabled) {
  background: #fff;
  transform: translateY(-1px);
}

.timer-action-btn.primary {
  background: linear-gradient(135deg, var(--color-accent-sage), #8ca595);
  color: #fff;
  border-color: transparent;
}

.timer-action-btn.primary:hover:not(:disabled) {
  background: linear-gradient(135deg, #789580, #8ca595);
}

.apply-time-btn:disabled,
.timer-action-btn:disabled {
  opacity: 0.45;
  cursor: not-allowed;
  transform: none;
}

.guide-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
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

.guide-hint {
  margin: 0;
  text-align: center;
  color: var(--color-text-secondary);
  font-size: 0.84rem;
  line-height: 1.6;
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

.guide-overlay-fade-enter-active,
.guide-overlay-fade-leave-active {
  transition: opacity 0.25s ease;
}

.guide-overlay-fade-enter-from,
.guide-overlay-fade-leave-to {
  opacity: 0;
}

.guide-overlay {
  position: absolute;
  inset: 0;
  z-index: 50;
  padding: 18px;
  border-radius: var(--radius-xl);
  background: rgba(33, 38, 36, 0.18);
  backdrop-filter: blur(10px);
}

.guide-modal-card {
  width: 100%;
  height: 100%;
  background: rgba(255, 255, 255, 0.78);
  border: 1px solid rgba(255, 255, 255, 0.68);
  border-radius: calc(var(--radius-xl) - 2px);
  box-shadow: var(--shadow-float);
  backdrop-filter: blur(24px);
  padding: 30px 32px;
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.guide-modal-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 20px;
}

.guide-modal-kicker {
  margin: 0 0 10px;
  font-size: 0.84rem;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: var(--color-accent-terracotta);
  font-weight: 700;
}

.guide-modal-title {
  margin: 0 0 10px;
  font-family: var(--font-serif);
  font-size: 1.8rem;
  color: var(--color-text-primary);
}

.guide-modal-desc {
  margin: 0;
  color: var(--color-text-secondary);
  line-height: 1.6;
}

.guide-modal-close {
  position: static;
  width: 42px;
  height: 42px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.78);
  border: 1px solid rgba(44, 48, 46, 0.1);
  flex-shrink: 0;
}

.guide-modal-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
}

.guide-status-pill {
  display: inline-flex;
  align-items: center;
  padding: 7px 14px;
  border-radius: var(--radius-pill);
  font-size: 0.86rem;
  font-weight: 700;
}

.guide-status-pill.loading {
  background: rgba(140, 165, 149, 0.18);
  color: #587466;
}

.guide-status-pill.speaking {
  background: rgba(217, 122, 108, 0.18);
  color: #b06054;
}

.guide-status-pill.error {
  background: rgba(210, 71, 87, 0.12);
  color: #b24150;
}

.guide-status-pill.idle {
  background: rgba(44, 48, 46, 0.08);
  color: var(--color-text-secondary);
}

.guide-progress-text {
  font-size: 0.92rem;
  color: var(--color-text-secondary);
}

.guide-lyrics-area {
  flex: 1;
  min-height: 0;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.62), rgba(255, 255, 255, 0.38));
  border: 1px solid rgba(255, 255, 255, 0.72);
  border-radius: var(--radius-xl);
  overflow: hidden;
  padding: 18px 22px;
}

.guide-placeholder {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  text-align: center;
  color: var(--color-text-secondary);
  font-size: 1rem;
  line-height: 1.9;
}

.guide-lines {
  height: 100%;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  justify-content: flex-start;
  align-items: stretch;
  gap: 14px;
  padding: 8px 8px 24px;
  scroll-behavior: smooth;
}

.guide-line {
  margin: 0;
  padding: 10px 16px;
  text-align: center;
  line-height: 1.9;
  font-size: 1.06rem;
  color: rgba(44, 48, 46, 0.68);
  border-radius: var(--radius-lg);
  transition: all 0.24s ease;
}

.guide-line.passed {
  color: rgba(44, 48, 46, 0.4);
}

.guide-line.upcoming {
  color: rgba(44, 48, 46, 0.82);
}

.guide-line.active {
  background: rgba(217, 122, 108, 0.14);
  box-shadow: 0 12px 28px rgba(217, 122, 108, 0.12);
  color: var(--color-accent-terracotta);
  font-size: 1.34rem;
  font-weight: 700;
  transform: scale(1.01);
}

.guide-modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 14px;
  flex-wrap: wrap;
}

.guide-modal-btn {
  padding: 10px 22px;
  border-radius: var(--radius-pill);
  border: 1px solid rgba(44, 48, 46, 0.12);
  background: rgba(255, 255, 255, 0.82);
  color: var(--color-text-primary);
  font-weight: 600;
  transition: all var(--transition-fast);
}

.guide-modal-btn.primary {
  background: linear-gradient(135deg, var(--color-accent-terracotta), #c88a75);
  border-color: transparent;
  color: #fff;
}

.guide-modal-btn:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 12px 24px rgba(44, 48, 46, 0.08);
}

.guide-modal-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
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

  .right-panels-grid {
    grid-template-columns: 1fr;
  }
  
  .page-shell {
    height: auto;
    min-height: 100vh;
  }
  
  .meditation-page {
    height: auto;
    overflow: auto;
  }

  .guide-overlay {
    position: static;
    padding: 0;
    background: transparent;
    backdrop-filter: none;
    margin-top: 20px;
  }

  .guide-modal-card {
    min-height: 640px;
    padding: 24px 20px;
  }
}
</style>
