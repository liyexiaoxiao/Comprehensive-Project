<template>
  <div class="all-playlists-page">
    <div class="blur-orb orb-coral"></div>
    <div class="blur-orb orb-leaf"></div>
    <div class="blur-orb orb-sky"></div>

    <div class="page-shell">
      <div class="page-top-bar">
        <RouterLink class="back-link" :to="{ name: 'service' }">返回音乐首页</RouterLink>
      </div>

      <section class="hero-card surface">
        <div>
          <p class="kicker">Playlist Center</p>
          <h1>查看所有歌单</h1>
          <p class="lead">
            这里汇总了主页精选歌单、官方情绪歌单和你的自建歌单，方便一次性浏览与播放。
          </p>
        </div>
        <div class="hero-stats">
          <div class="stat-pill">
            <strong>{{ visiblePlaylists.length }}</strong>
            <span>当前可见歌单</span>
          </div>
          <div class="stat-pill">
            <strong>{{ activePlaylist?.tracks?.length || 0 }}</strong>
            <span>选中歌单曲目</span>
          </div>
        </div>
      </section>

      <section class="filters-card surface">
        <div class="source-tabs">
          <button
            v-for="tab in playlistSourceTabs"
            :key="tab.id"
            class="source-tab"
            :class="{ active: activeSource === tab.id }"
            type="button"
            @click="activeSource = tab.id"
          >
            {{ tab.label }}
          </button>
        </div>

        <div class="search-row">
          <label class="search-field">
            <span>搜索歌单</span>
            <input v-model.trim="playlistSearchText" type="text" placeholder="搜索歌单名称、描述或来源" />
          </label>
          <label class="search-field">
            <span>搜索当前歌单曲目</span>
            <input v-model.trim="trackSearchText" type="text" placeholder="搜索歌名、歌手或情绪标签" />
          </label>
        </div>
      </section>

      <div class="content-layout">
        <section class="playlist-panel surface">
          <div class="section-head">
            <div>
              <h2>歌单目录</h2>
              <p>点击任意歌单，在右侧查看曲目明细。</p>
            </div>
          </div>

          <div v-if="visiblePlaylists.length" class="playlist-grid">
            <button
              v-for="playlist in visiblePlaylists"
              :key="playlist.id"
              class="playlist-card"
              :class="{ active: activePlaylist?.id === playlist.id }"
              type="button"
              @click="selectPlaylist(playlist.id)"
            >
              <img :src="playlist.cover" :alt="playlist.name" />
              <div class="playlist-card-body">
                <div class="playlist-card-top">
                  <span class="playlist-source-badge">{{ playlist.sourceLabel }}</span>
                  <span class="playlist-count">{{ playlist.tracks.length }} 首</span>
                </div>
                <strong>{{ playlist.name }}</strong>
                <p>{{ playlist.description }}</p>
              </div>
            </button>
          </div>

          <div v-else class="empty-state">
            当前条件下还没有可展示的歌单。
          </div>
        </section>

        <section class="detail-panel surface">
          <template v-if="activePlaylist">
            <div class="detail-hero">
              <img :src="activePlaylist.cover" :alt="activePlaylist.name" />
              <div class="detail-copy">
                <span class="playlist-source-badge">{{ activePlaylist.sourceLabel }}</span>
                <h2>{{ activePlaylist.name }}</h2>
                <p>{{ activePlaylist.description }}</p>
                <div class="detail-meta">
                  <span>{{ activePlaylist.tracks.length }} 首音乐</span>
                  <span>{{ playlistTypeLabel(activePlaylist.kind) }}</span>
                </div>
                <div class="detail-actions">
                  <button class="primary-button" type="button" @click="playPlaylist()">
                    播放歌单
                  </button>
                  <button class="secondary-button" type="button" @click="router.push({ name: 'service' })">
                    回到音乐首页
                  </button>
                </div>
              </div>
            </div>

            <div class="track-section">
              <div class="section-head">
                <div>
                  <h3>曲目列表</h3>
                  <p>选择一首音乐进入播放器，也可以直接播放整个歌单。</p>
                </div>
                <span class="count-pill">{{ filteredTracks.length }} 首</span>
              </div>

              <div v-if="filteredTracks.length" class="track-list">
                <button
                  v-for="track in filteredTracks"
                  :key="track.id"
                  class="track-card"
                  type="button"
                  @click="playPlaylist(track)"
                >
                  <img :src="track.cover || fallbackCover(track)" :alt="track.title" />
                  <div class="track-meta">
                    <strong>{{ track.title }}</strong>
                    <span>{{ track.artist || '佚名' }}</span>
                    <small>{{ formatTrackTags(track) }}</small>
                  </div>
                  <span class="track-duration">{{ formatTime(track.duration) }}</span>
                </button>
              </div>

              <div v-else class="empty-state compact">
                当前歌单里还没有匹配的曲目。
              </div>
            </div>
          </template>

          <div v-else class="empty-state">
            正在整理歌单内容，请稍后。
          </div>
        </section>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getOfficialPlaylistConfigsApi } from '@/api/music'
import { useMusicStore } from '@/stores/musicStore'
import { buildAllPlaylistCatalog, getDefaultOfficialPlaylists, mergeOfficialPlaylistConfigs, playlistSourceTabs } from '@/utils/playlistCatalog'
import { getRealMusicCover } from '@/utils/realMusic'

const router = useRouter()
const route = useRoute()
const musicStore = useMusicStore()
const PLAYER_SESSION_KEY = 'emotion-system-active-player'

const activeSource = ref('all')
const playlistSearchText = ref('')
const trackSearchText = ref('')
const activePlaylistId = ref(String(route.query.playlist || 'recommend'))
const officialPlaylistConfigs = ref(getDefaultOfficialPlaylists())

const allPlaylists = computed(() => buildAllPlaylistCatalog({
  publicTracks: musicStore.publicTracks,
  recommendedTracks: musicStore.recommendedTracks,
  likedIds: musicStore.likedTrackIds,
  collectedIds: musicStore.collectedTrackIds,
  officialPlaylistConfigs: officialPlaylistConfigs.value,
  customPlaylists: musicStore.customPlaylists,
  resolveTrackById: musicStore.resolveTrackById,
}))

const visiblePlaylists = computed(() => {
  const keyword = playlistSearchText.value.trim().toLowerCase()

  return allPlaylists.value.filter((playlist) => {
    if (activeSource.value !== 'all' && playlist.kind !== activeSource.value) {
      return false
    }

    if (!keyword) {
      return true
    }

    const fields = [
      playlist.name,
      playlist.description,
      playlist.sourceLabel,
      ...(Array.isArray(playlist.tracks) ? playlist.tracks.flatMap((track) => [track.title, track.artist, ...(track.tags || [])]) : []),
    ]
      .map((value) => String(value || '').toLowerCase())
      .filter(Boolean)

    return fields.some((field) => field.includes(keyword))
  })
})

const activePlaylist = computed(() =>
  visiblePlaylists.value.find((playlist) => String(playlist.id) === String(activePlaylistId.value))
  || visiblePlaylists.value[0]
  || null)

const filteredTracks = computed(() => {
  const tracks = Array.isArray(activePlaylist.value?.tracks) ? activePlaylist.value.tracks : []
  const keyword = trackSearchText.value.trim().toLowerCase()

  if (!keyword) {
    return tracks
  }

  return tracks.filter((track) => {
    const fields = [track.title, track.artist, track.emotion, track.filename, ...(track.tags || [])]
      .map((value) => String(value || '').toLowerCase())
      .filter(Boolean)
    return fields.some((field) => field.includes(keyword))
  })
})

const selectPlaylist = (playlistId) => {
  activePlaylistId.value = String(playlistId)
}

const playlistTypeLabel = (kind) => {
  if (kind === 'official') return '官方情绪歌单'
  if (kind === 'custom') return '个人创建歌单'
  return '主页原有歌单'
}

const formatTime = (seconds) => {
  const safeValue = Number.isFinite(seconds) ? seconds : 0
  const minutes = Math.floor(safeValue / 60)
  const remain = Math.floor(safeValue % 60)
  return `${String(minutes).padStart(2, '0')}:${String(remain).padStart(2, '0')}`
}

const fallbackCover = (track) => getRealMusicCover(track?.emotion || track?.tags?.[0] || 'neutral')

const formatTrackTags = (track) => {
  const tags = Array.isArray(track?.tags) ? track.tags.filter(Boolean) : []
  return tags.length ? tags.slice(0, 3).join(' / ') : '真实音乐'
}

const normalizePlayerTrack = (track) => ({
  id: track.id,
  musicResourceId: track.musicResourceId ?? null,
  title: track.title,
  artist: track.artist?.trim() || '佚名',
  duration: Number(track.duration) || 0,
  cover: track.cover || fallbackCover(track),
  tags: Array.isArray(track.tags) && track.tags.length ? track.tags : ['真实音乐'],
  type: track.file ? '本地上传' : '真实音乐',
  emotion: track.emotion || 'neutral',
  filename: track.filename || '',
  fileUrl: track.fileUrl || '',
})

const playPlaylist = (selectedTrack = null) => {
  const playlist = activePlaylist.value
  const queueTracks = filteredTracks.value.length ? filteredTracks.value : (playlist?.tracks || [])
  if (!playlist || !queueTracks.length) {
    ElMessage.warning('当前歌单暂无可播放的音乐。')
    return
  }

  const track = selectedTrack || queueTracks[0]
  window.sessionStorage.setItem(PLAYER_SESSION_KEY, JSON.stringify({
    source: 'all-playlists',
    returnTo: '/playlists',
    categoryId: playlist.id,
    categoryName: playlist.name,
    track: normalizePlayerTrack(track),
    queue: queueTracks.map((item) => normalizePlayerTrack(item)),
  }))

  router.push({ name: 'music-player' })
}

const loadData = async () => {
  try {
    const [officialConfigResponse] = await Promise.all([
      getOfficialPlaylistConfigsApi(),
      musicStore.fetchUserData(),
      musicStore.fetchEmotionTags(),
      musicStore.fetchPublicTracks(),
    ])
    officialPlaylistConfigs.value = mergeOfficialPlaylistConfigs(officialConfigResponse.data)
    const currentEmotion = window.localStorage.getItem('currentEmotion') || 'neutral'
    await musicStore.fetchRecommendedTracks(currentEmotion)
  } catch (error) {
    ElMessage.error('歌单目录加载失败，请确认 music-service 已启动。')
    console.error('Load all playlists failed:', error)
  }
}

watch(visiblePlaylists, (playlists) => {
  if (!playlists.some((playlist) => String(playlist.id) === String(activePlaylistId.value))) {
    activePlaylistId.value = playlists[0] ? String(playlists[0].id) : ''
  }
}, { immediate: true })

watch(activePlaylistId, (playlistId) => {
  const nextQuery = { ...route.query }
  if (playlistId) {
    nextQuery.playlist = String(playlistId)
  } else {
    delete nextQuery.playlist
  }
  router.replace({ query: nextQuery })
})

onMounted(async () => {
  await loadData()
})
</script>

<style scoped>
.all-playlists-page {
  position: relative;
  height: calc(100vh + 72px);
  padding: 20px;
  overflow: hidden;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
}

.blur-orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(90px);
  pointer-events: none;
  z-index: 0;
  opacity: 0.5;
}

.orb-coral {
  top: -12%;
  right: -8%;
  width: 460px;
  height: 460px;
  background: var(--color-accent-blush);
}

.orb-leaf {
  left: -10%;
  bottom: -8%;
  width: 560px;
  height: 560px;
  background: var(--color-accent-sage);
}

.orb-sky {
  left: 38%;
  top: 24%;
  width: 360px;
  height: 360px;
  background: var(--color-accent-sky);
}

.page-shell {
  position: relative;
  z-index: 1;
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 18px;
  max-width: 1440px;
  margin: 0 auto;
  min-height: 0;
}

.surface {
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid rgba(255, 255, 255, 0.68);
  box-shadow: 0 30px 60px rgba(31, 41, 55, 0.12);
  backdrop-filter: blur(18px);
  border-radius: 28px;
}

.page-top-bar {
  display: flex;
  justify-content: flex-end;
}

.back-link {
  color: var(--color-text-primary);
  text-decoration: none;
  font-weight: 600;
}

.hero-card {
  padding: 26px 28px;
  display: flex;
  justify-content: space-between;
  gap: 20px;
  align-items: center;
}

.kicker {
  margin: 0 0 6px;
  font-size: 0.86rem;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: var(--color-text-secondary);
}

.hero-card h1,
.section-head h2,
.section-head h3,
.detail-copy h2 {
  margin: 0;
  color: var(--color-text-primary);
}

.lead {
  margin: 10px 0 0;
  max-width: 720px;
  color: var(--color-text-secondary);
  line-height: 1.7;
}

.hero-stats {
  display: flex;
  gap: 14px;
  flex-wrap: wrap;
}

.stat-pill {
  min-width: 132px;
  padding: 16px 18px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid rgba(255, 255, 255, 0.7);
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.stat-pill strong {
  font-size: 1.35rem;
  color: var(--color-text-primary);
}

.stat-pill span,
.section-head p,
.detail-copy p,
.detail-meta,
.track-meta span,
.track-meta small {
  color: var(--color-text-secondary);
}

.filters-card {
  padding: 20px 24px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.source-tabs {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.source-tab,
.secondary-button,
.primary-button,
.playlist-card,
.track-card {
  border: none;
  cursor: pointer;
}

.source-tab {
  padding: 10px 16px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.72);
  color: var(--color-text-secondary);
  transition: all 0.2s ease;
}

.source-tab.active {
  background: rgba(93, 111, 98, 0.16);
  color: var(--color-text-primary);
  box-shadow: inset 0 0 0 1px rgba(93, 111, 98, 0.24);
}

.search-row {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.search-field {
  display: flex;
  flex-direction: column;
  gap: 8px;
  color: var(--color-text-primary);
  font-weight: 600;
}

.search-field input {
  height: 46px;
  border-radius: 16px;
  border: 1px solid rgba(148, 163, 184, 0.3);
  padding: 0 14px;
  background: rgba(255, 255, 255, 0.84);
  color: var(--color-text-primary);
  outline: none;
}

.content-layout {
  display: grid;
  grid-template-columns: minmax(340px, 0.95fr) minmax(0, 1.35fr);
  gap: 18px;
  flex: 1;
  min-height: 0;
  align-items: stretch;
}

.playlist-panel,
.detail-panel {
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 18px;
  min-height: 0;
  height: 100%;
  overflow: hidden;
}

.section-head {
  display: flex;
  justify-content: space-between;
  gap: 14px;
  align-items: flex-start;
}

.section-head p {
  margin: 8px 0 0;
  line-height: 1.6;
}

.playlist-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
  align-content: flex-start;
  overflow: auto;
  min-height: 0;
  flex: 1;
  padding-right: 4px;
}

.playlist-card {
  text-align: left;
  background: rgba(255, 255, 255, 0.76);
  border-radius: 22px;
  border: 1px solid transparent;
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  transition: all 0.2s ease;
}

.playlist-card:hover,
.playlist-card.active {
  transform: translateY(-2px);
  border-color: rgba(93, 111, 98, 0.24);
  box-shadow: 0 16px 34px rgba(31, 41, 55, 0.12);
}

.playlist-card img,
.detail-hero img,
.track-card img {
  border-radius: 18px;
  object-fit: cover;
}

.playlist-card img {
  width: 100%;
  aspect-ratio: 1 / 1;
}

.playlist-card-body {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.playlist-card-top,
.detail-meta {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  flex-wrap: wrap;
}

.playlist-card strong,
.track-meta strong {
  color: var(--color-text-primary);
}

.playlist-card p {
  margin: 0;
  color: var(--color-text-secondary);
  line-height: 1.6;
  font-size: 0.95rem;
}

.playlist-source-badge,
.playlist-count,
.count-pill {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  padding: 6px 12px;
  font-size: 0.82rem;
}

.playlist-source-badge {
  background: rgba(93, 111, 98, 0.12);
  color: var(--color-text-primary);
}

.playlist-count,
.count-pill {
  background: rgba(255, 255, 255, 0.86);
  color: var(--color-text-secondary);
}

.detail-hero {
  display: grid;
  grid-template-columns: 220px minmax(0, 1fr);
  gap: 20px;
  align-items: center;
}

.detail-hero img {
  width: 220px;
  height: 220px;
}

.detail-copy {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.detail-copy p {
  margin: 0;
  line-height: 1.7;
}

.detail-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.primary-button,
.secondary-button {
  min-height: 44px;
  padding: 0 18px;
  border-radius: 14px;
  font-weight: 600;
}

.primary-button {
  background: #4f6f61;
  color: #fff;
}

.secondary-button {
  background: rgba(255, 255, 255, 0.82);
  color: var(--color-text-primary);
}

.track-section {
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-height: 0;
  flex: 1;
}

.track-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  overflow: auto;
  min-height: 0;
  flex: 1;
  padding-right: 4px;
}

.track-card {
  width: 100%;
  padding: 12px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.78);
  display: grid;
  grid-template-columns: 72px minmax(0, 1fr) auto;
  gap: 14px;
  align-items: center;
  text-align: left;
}

.track-card img {
  width: 72px;
  height: 72px;
}

.track-meta {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
}

.track-meta strong,
.track-meta span,
.track-meta small {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.track-duration {
  color: var(--color-text-secondary);
  font-weight: 600;
}

.empty-state {
  min-height: 220px;
  display: flex;
  align-items: center;
  justify-content: center;
  text-align: center;
  padding: 20px;
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.56);
  color: var(--color-text-secondary);
  line-height: 1.7;
}

.empty-state.compact {
  min-height: 160px;
}

@media (max-width: 1180px) {
  .all-playlists-page {
    height: auto;
    min-height: 100vh;
    overflow: auto;
  }

  .page-shell {
    height: auto;
  }

  .content-layout {
    flex: none;
  }

  .content-layout {
    grid-template-columns: 1fr;
  }

  .detail-hero {
    grid-template-columns: 180px minmax(0, 1fr);
  }

  .detail-hero img {
    width: 180px;
    height: 180px;
  }
}

@media (max-width: 860px) {
  .hero-card {
    flex-direction: column;
    align-items: flex-start;
  }

  .search-row,
  .playlist-grid,
  .detail-hero {
    grid-template-columns: 1fr;
  }

  .detail-hero img {
    width: 100%;
    height: auto;
    aspect-ratio: 1 / 1;
  }

  .track-card {
    grid-template-columns: 64px minmax(0, 1fr);
  }

  .track-duration {
    grid-column: 2;
  }
}
</style>
