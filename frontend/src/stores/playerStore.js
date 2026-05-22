import { defineStore } from 'pinia'
import { ref } from 'vue'

const audioPlayer = new Audio()
const currentTrack = ref(null)
const queue = ref([])
const playbackContext = ref({
  source: '',
  returnTo: '/service',
  categoryId: 'recommend',
  categoryName: '音乐空间',
})
const isPlaying = ref(false)
const progressSeconds = ref(0)
const volume = ref(68)
const isLoadingAudio = ref(false)
const lastLoadedTrackId = ref('')

let currentObjectUrl = null
let endedHandler = null

const normalizeTrack = (track) => (track ? { ...track } : null)

const cleanupObjectUrl = () => {
  if (currentObjectUrl) {
    URL.revokeObjectURL(currentObjectUrl)
    currentObjectUrl = null
  }
}

audioPlayer.preload = 'auto'
audioPlayer.volume = volume.value / 100

audioPlayer.onplay = () => {
  isPlaying.value = true
}

audioPlayer.onpause = () => {
  isPlaying.value = false
}

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

audioPlayer.onended = () => {
  isPlaying.value = false
  if (typeof endedHandler === 'function') {
    endedHandler()
  }
}

const setCurrentTrack = (track) => {
  currentTrack.value = normalizeTrack(track)
}

const setQueue = (tracks = []) => {
  queue.value = Array.isArray(tracks) ? tracks.map(normalizeTrack).filter(Boolean) : []
}

const setPlaybackContext = (nextContext = {}) => {
  playbackContext.value = {
    ...playbackContext.value,
    ...nextContext,
  }
}

const setVolumeValue = (nextVolume) => {
  const numericValue = Number(nextVolume)
  const safeVolume = Number.isFinite(numericValue)
    ? Math.min(100, Math.max(0, numericValue))
    : volume.value
  volume.value = safeVolume
  audioPlayer.volume = safeVolume / 100
}

const seekTo = (nextProgress) => {
  const numericValue = Number(nextProgress)
  const safeProgress = Number.isFinite(numericValue) ? Math.max(0, numericValue) : 0
  progressSeconds.value = safeProgress
  if (audioPlayer.src && Math.abs(audioPlayer.currentTime - safeProgress) > 1) {
    audioPlayer.currentTime = safeProgress
  }
}

const hasActiveSource = () => Boolean(audioPlayer.src)

const isTrackLoaded = (trackId) => {
  const normalizedTrackId = String(trackId ?? '')
  return Boolean(normalizedTrackId) && hasActiveSource() && lastLoadedTrackId.value === normalizedTrackId
}

const play = async () => {
  if (!audioPlayer.src) return
  await audioPlayer.play()
  isPlaying.value = true
}

const pause = () => {
  audioPlayer.pause()
  isPlaying.value = false
}

const applySource = async (track, src, options = {}) => {
  const { startAt = 0, autoplay = true, cleanupExistingObjectUrl = true } = options

  if (!src || isLoadingAudio.value) {
    return false
  }

  try {
    isLoadingAudio.value = true
    if (cleanupExistingObjectUrl) {
      cleanupObjectUrl()
    }
    currentTrack.value = normalizeTrack(track)
    lastLoadedTrackId.value = String(track?.id ?? '')
    audioPlayer.src = src
    audioPlayer.currentTime = Number.isFinite(startAt) ? Math.max(0, startAt) : 0
    progressSeconds.value = audioPlayer.currentTime
    audioPlayer.volume = volume.value / 100

    if (autoplay) {
      await audioPlayer.play()
      isPlaying.value = true
    } else {
      pause()
    }

    return true
  } finally {
    isLoadingAudio.value = false
  }
}

const loadSourceFromUrl = async (track, url, options = {}) => {
  return applySource(track, url, options)
}

const loadSourceFromBlob = async (track, blob, options = {}) => {
  if (!blob) return false

  cleanupObjectUrl()
  currentObjectUrl = URL.createObjectURL(blob)
  try {
    return await applySource(track, currentObjectUrl, {
      ...options,
      cleanupExistingObjectUrl: false,
    })
  } catch (error) {
    cleanupObjectUrl()
    throw error
  }
}

const resetPlayer = (options = {}) => {
  const { clearTrack = true, clearQueue = false } = options

  pause()
  audioPlayer.src = ''
  progressSeconds.value = 0
  lastLoadedTrackId.value = ''
  cleanupObjectUrl()

  if (clearTrack) {
    currentTrack.value = null
  }
  if (clearQueue) {
    queue.value = []
  }
}

const setEndedHandler = (handler) => {
  endedHandler = typeof handler === 'function' ? handler : null
}

export const usePlayerStore = defineStore('player', () => {
  return {
    currentTrack,
    queue,
    playbackContext,
    isPlaying,
    progressSeconds,
    volume,
    isLoadingAudio,
    lastLoadedTrackId,
    setCurrentTrack,
    setQueue,
    setPlaybackContext,
    setVolumeValue,
    seekTo,
    hasActiveSource,
    isTrackLoaded,
    play,
    pause,
    loadSourceFromUrl,
    loadSourceFromBlob,
    resetPlayer,
    setEndedHandler,
  }
})
