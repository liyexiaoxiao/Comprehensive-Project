<template>
  <section class="glass-card panel music-admin-panel">
    <div class="panel-head">
      <div>
        <h2>官方情绪歌单管理</h2>
        <p>编辑官方八个情绪歌单信息，上传官方音乐，并维护歌单曲目。</p>
      </div>
      <div class="panel-head-actions">
        <button class="ghost-btn" type="button" @click="refreshOfficialMusic" :disabled="isOfficialMusicLoading">
          {{ isOfficialMusicLoading ? '加载中...' : '刷新音乐库' }}
        </button>
      </div>
    </div>

    <div class="official-layout">
      <aside class="official-playlist-nav">
        <button
          v-for="playlist in officialPlaylists"
          :key="playlist.id"
          type="button"
          class="official-playlist-chip"
          :class="{ active: playlist.id === activeOfficialPlaylistId }"
          @click="selectOfficialPlaylist(playlist.id)"
        >
          <strong>{{ playlist.name }}</strong>
          <span>{{ playlist.tracks.length }} 首</span>
        </button>
      </aside>

      <div class="official-main">
        <template v-if="activeOfficialPlaylist">
          <div class="official-grid">
            <section class="official-card">
              <div class="panel-head compact-head">
                <div>
                  <h2>歌单信息</h2>
                  <p>可编辑官方歌单名称与简介，保存后会同步影响歌单总览页展示。</p>
                </div>
              </div>

              <div class="form-grid">
                <label>
                  <span>歌单名称</span>
                  <input v-model.trim="officialPlaylistForm.name" type="text" />
                </label>
                <label>
                  <span>封面情绪</span>
                  <select v-model="officialPlaylistForm.coverEmotion">
                    <option value="surprise">surprise</option>
                    <option value="sadness">sadness</option>
                    <option value="neutral">neutral</option>
                    <option value="joy">joy</option>
                    <option value="fear">fear</option>
                    <option value="disgust">disgust</option>
                    <option value="calm">calm</option>
                    <option value="anger">anger</option>
                  </select>
                </label>
                <label class="full-width">
                  <span>歌单简介</span>
                  <textarea v-model.trim="officialPlaylistForm.description" rows="4"></textarea>
                </label>
              </div>

              <div class="action-row">
                <button class="primary-btn" type="button" @click="saveOfficialPlaylistConfig">
                  保存歌单配置
                </button>
                <button class="ghost-btn" type="button" @click="resetOfficialPlaylistConfig">
                  恢复默认配置
                </button>
              </div>
            </section>

            <section class="official-card">
              <div class="panel-head compact-head">
                <div>
                  <h2>上传官方音乐</h2>
                  <p>上传流程与用户端一致，但会直接绑定到当前官方情绪歌单。</p>
                </div>
              </div>

              <div class="form-grid">
                <label class="full-width">
                  <span>音频文件</span>
                  <div class="file-row">
                    <input ref="officialAudioInputRef" type="file" accept=".mp3,audio/*" class="hidden-file-input" @change="handleOfficialAudioSelect" />
                    <button class="ghost-btn" type="button" @click="triggerOfficialAudioSelect">选择 MP3 文件</button>
                    <span class="file-name">{{ officialUploadForm.file ? officialUploadForm.file.name : '未选择文件' }}</span>
                  </div>
                </label>
                <label>
                  <span>歌曲名</span>
                  <input v-model.trim="officialUploadForm.title" type="text" />
                </label>
                <label>
                  <span>作者</span>
                  <input v-model.trim="officialUploadForm.artist" type="text" />
                </label>
                <label>
                  <span>时长（秒）</span>
                  <input v-model.number="officialUploadForm.duration" type="number" min="0" />
                </label>
                <label>
                  <span>附加标签</span>
                  <input v-model.trim="officialUploadForm.tagInput" type="text" placeholder="输入后点添加" @keyup.enter.prevent="addOfficialUploadTag" />
                </label>
                <label class="full-width">
                  <span>自动识别封面</span>
                  <div class="upload-cover-preview">
                    <img
                      v-if="officialUploadForm.coverUrl"
                      :src="officialUploadForm.coverUrl"
                      alt="识别到的歌曲封面"
                      class="upload-cover-image"
                    />
                    <div v-else class="upload-cover-placeholder">
                      未识别到内嵌封面，上传后将使用默认情绪封面展示
                    </div>
                  </div>
                </label>
                <label class="full-width">
                  <span>当前标签</span>
                  <div class="tag-editor">
                    <button class="ghost-btn compact-btn" type="button" @click="addOfficialUploadTag">添加标签</button>
                    <span class="small-tag" v-for="tag in officialUploadTags" :key="tag">
                      {{ tag }}
                      <button type="button" class="tag-remove-btn" @click="removeOfficialUploadTag(tag)">&times;</button>
                    </span>
                  </div>
                </label>
              </div>

              <div class="action-row">
                <button class="primary-btn" type="button" @click="submitOfficialUpload" :disabled="!officialUploadForm.file || !officialUploadForm.title || isUploadingOfficialTrack">
                  {{ isUploadingOfficialTrack ? '上传中...' : '上传到当前歌单' }}
                </button>
              </div>
            </section>
          </div>

          <section class="official-card">
            <div class="panel-head compact-head">
              <div>
                <h2>歌单曲目</h2>
                <p>点击某首曲目进入编辑，支持修改元数据、调整标签或删除曲目。</p>
              </div>
              <span class="tag">{{ activeOfficialPlaylist.tracks.length }} 首</span>
            </div>

            <div class="official-track-layout">
              <div class="official-track-list">
                <button
                  v-for="track in activeOfficialPlaylist.tracks"
                  :key="track.id"
                  type="button"
                  class="official-track-row"
                  :class="{ active: track.id === editingOfficialTrackId }"
                  @click="selectOfficialTrack(track)"
                >
                  <img loading="lazy" :src="track.cover || fallbackOfficialCover(track)" alt="" class="official-track-cover" />
                  <div class="official-track-copy">
                    <strong>{{ track.title }}</strong>
                    <span>{{ track.artist || '佚名' }}</span>
                    <small>{{ (track.tags || []).join(' / ') || '真实音乐' }}</small>
                  </div>
                </button>

                <div v-if="!activeOfficialPlaylist.tracks.length" class="empty-state">当前官方歌单暂无曲目。</div>
              </div>

              <div class="official-track-editor" v-if="editingOfficialTrack">
                <div class="form-grid">
                  <label>
                    <span>歌曲名</span>
                    <input v-model.trim="officialTrackForm.title" type="text" />
                  </label>
                  <label>
                    <span>作者</span>
                    <input v-model.trim="officialTrackForm.artist" type="text" />
                  </label>
                  <label>
                    <span>时长（秒）</span>
                    <input v-model.number="officialTrackForm.duration" type="number" min="0" />
                  </label>
                  <label>
                    <span>封面地址</span>
                    <input v-model.trim="officialTrackForm.coverUrl" type="text" />
                  </label>
                  <label class="full-width">
                    <span>情绪标签</span>
                    <textarea v-model.trim="officialTrackForm.tagsText" rows="3" disabled></textarea>
                  </label>
                </div>

                <div class="action-row">
                  <button class="primary-btn" type="button" @click="saveOfficialTrack" :disabled="isSavingOfficialTrack">
                    {{ isSavingOfficialTrack ? '保存中...' : '保存曲目' }}
                  </button>
                  <button class="danger-btn" type="button" @click="deleteOfficialTrack" :disabled="isDeletingOfficialTrack">
                    {{ isDeletingOfficialTrack ? '删除中...' : '删除曲目' }}
                  </button>
                </div>
              </div>

              <div v-else class="empty-state">请选择左侧一首曲目开始编辑。</div>
            </div>
          </section>
        </template>
      </div>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useMusicStore } from '@/stores/musicStore'
import {
  createEmotionTagApi,
  createMusicResourceApi,
  deleteMusicResourceApi,
  getOfficialPlaylistConfigsApi,
  replaceMusicTagsApi,
  updateOfficialPlaylistConfigApi,
  updateMusicResourceApi,
} from '@/api/music'
import { deleteUploadedMusicApi, getMusicFileUrl, uploadMusicFileApi } from '@/api/python'
import {
  buildOfficialEmotionPlaylists,
  getDefaultOfficialPlaylists,
  mergeOfficialPlaylistConfigs,
} from '@/utils/playlistCatalog'
import { readAudioTagMetadata } from '@/utils/audioMetadata'
import { getRealMusicCover, readRemoteAudioDuration } from '@/utils/realMusic'

const ADMIN_OFFICIAL_SOURCE_PREFIX = 'admin-official-upload:'
const isOfficialMusicLoading = ref(false)
const isUploadingOfficialTrack = ref(false)
const isSavingOfficialTrack = ref(false)
const isDeletingOfficialTrack = ref(false)
const officialAudioInputRef = ref(null)
const activeOfficialPlaylistId = ref(getDefaultOfficialPlaylists()[0]?.id || '')
const editingOfficialTrackId = ref('')
const musicStore = useMusicStore()
const officialPlaylistConfigs = ref(getDefaultOfficialPlaylists())

const officialPlaylistForm = reactive({
  name: '',
  description: '',
  coverEmotion: 'neutral',
})

const officialUploadForm = reactive({
  file: null,
  title: '',
  artist: '',
  duration: 0,
  coverUrl: '',
  tagInput: '',
  tags: [],
})

const officialTrackForm = reactive({
  title: '',
  artist: '',
  duration: 0,
  coverUrl: '',
  tagsText: '',
})

const fallbackOfficialCover = (track) => getRealMusicCover(track?.emotion || track?.tags?.[0] || activeOfficialPlaylist.value?.coverEmotion || 'neutral')

const officialPlaylists = computed(() => buildOfficialEmotionPlaylists(
  musicStore.publicTracks,
  officialPlaylistConfigs.value,
))

const activeOfficialPlaylist = computed(() =>
  officialPlaylists.value.find((playlist) => playlist.id === activeOfficialPlaylistId.value)
  || officialPlaylists.value[0]
  || null)

const editingOfficialTrack = computed(() =>
  activeOfficialPlaylist.value?.tracks?.find((track) => track.id === editingOfficialTrackId.value) || null)

const officialUploadTags = computed(() => {
  const playlistTag = activeOfficialPlaylist.value?.tagName
  const tagSet = new Set([playlistTag, ...(officialUploadForm.tags || [])].filter(Boolean))
  return Array.from(tagSet)
})

const applyOfficialPlaylistForm = (playlist) => {
  officialPlaylistForm.name = playlist?.name || ''
  officialPlaylistForm.description = playlist?.description || ''
  officialPlaylistForm.coverEmotion = playlist?.coverEmotion || 'neutral'
}

const buildOfficialPlaylistPayload = (playlist) => ({
  name: playlist?.name || '',
  description: playlist?.description || '',
  tagName: playlist?.tagName || '',
  coverEmotion: playlist?.coverEmotion || 'neutral',
  emotions: Array.isArray(playlist?.emotions) ? [...playlist.emotions] : [],
  keywords: Array.isArray(playlist?.keywords) ? [...playlist.keywords] : [],
  sortOrder: Number.isFinite(Number(playlist?.sortOrder)) ? Number(playlist.sortOrder) : 0,
})

const applyOfficialTrackForm = async (track) => {
  officialTrackForm.title = track?.title || ''
  officialTrackForm.artist = track?.artist || ''
  officialTrackForm.duration = Number(track?.duration) || 0
  officialTrackForm.coverUrl = track?.coverUrl || ''
  // 官方歌单曲目的情绪标签由所属歌单决定，不从 track 历史数据读取
  officialTrackForm.tagsText = activeOfficialPlaylist.value?.tagName || ''

  // 兜底：如果 duration 为 0 但有音频地址，从远程音频文件异步读取真实时长
  if (!officialTrackForm.duration && track?.fileUrl) {
    try {
      const secs = await readRemoteAudioDuration(track.fileUrl)
      if (secs > 0) {
        officialTrackForm.duration = secs
      }
    } catch {
      // 读取失败保持表单当前值
    }
  }
}

const parseTagNames = (value) => Array.from(new Set(String(value || '')
  .split(/[\s,，]+/)
  .map((item) => item.trim())
  .filter(Boolean)))

const sanitizePersistedCoverUrl = (value) => {
  const raw = String(value || '').trim()
  return raw.startsWith('data:image/svg+xml;') ? '' : raw
}

const resetOfficialUploadForm = () => {
  officialUploadForm.file = null
  officialUploadForm.title = ''
  officialUploadForm.artist = ''
  officialUploadForm.duration = 0
  officialUploadForm.coverUrl = ''
  officialUploadForm.tagInput = ''
  officialUploadForm.tags = []
  if (officialAudioInputRef.value) {
    officialAudioInputRef.value.value = ''
  }
}

const refreshOfficialMusic = async () => {
  isOfficialMusicLoading.value = true
  try {
    const [officialConfigResponse] = await Promise.all([
      getOfficialPlaylistConfigsApi(),
      musicStore.fetchEmotionTags(),
      musicStore.fetchPublicTracks(),
    ])
    officialPlaylistConfigs.value = mergeOfficialPlaylistConfigs(officialConfigResponse.data)
  } catch (error) {
    console.error('Failed to refresh official music:', error)
    ElMessage.error('刷新官方歌单失败')
  } finally {
    isOfficialMusicLoading.value = false
  }
}

const selectOfficialPlaylist = (playlistId) => {
  activeOfficialPlaylistId.value = playlistId
}

const saveOfficialPlaylistConfig = async () => {
  if (!activeOfficialPlaylist.value) return

  const nextPlaylist = {
    ...activeOfficialPlaylist.value,
    name: officialPlaylistForm.name || activeOfficialPlaylist.value.name,
    description: officialPlaylistForm.description || activeOfficialPlaylist.value.description,
    coverEmotion: officialPlaylistForm.coverEmotion || activeOfficialPlaylist.value.coverEmotion,
  }

  try {
    const response = await updateOfficialPlaylistConfigApi(activeOfficialPlaylist.value.id, buildOfficialPlaylistPayload(nextPlaylist))
    const merged = mergeOfficialPlaylistConfigs([response.data, ...officialPlaylistConfigs.value.filter((item) => item.id !== activeOfficialPlaylist.value.id)])
    officialPlaylistConfigs.value = merged
    applyOfficialPlaylistForm(merged.find((item) => item.id === activeOfficialPlaylist.value.id) || nextPlaylist)
    ElMessage.success('官方歌单配置已保存到后端')
  } catch (error) {
    console.error('Save official playlist config failed:', error)
    ElMessage.error('保存官方歌单配置失败')
  }
}

const resetOfficialPlaylistConfig = async () => {
  if (!activeOfficialPlaylist.value) return
  const defaults = getDefaultOfficialPlaylists()
  const fallback = defaults.find((playlist) => playlist.id === activeOfficialPlaylist.value.id)
  if (!fallback) return

  try {
    const response = await updateOfficialPlaylistConfigApi(fallback.id, buildOfficialPlaylistPayload(fallback))
    officialPlaylistConfigs.value = mergeOfficialPlaylistConfigs([response.data, ...officialPlaylistConfigs.value.filter((item) => item.id !== fallback.id)])
    applyOfficialPlaylistForm(fallback)
    ElMessage.success('已恢复默认歌单配置')
  } catch (error) {
    console.error('Reset official playlist config failed:', error)
    ElMessage.error('恢复默认歌单配置失败')
  }
}

const triggerOfficialAudioSelect = () => {
  officialAudioInputRef.value?.click()
}

const handleOfficialAudioSelect = async (event) => {
  const file = event.target.files?.[0]
  if (!file) return
  officialUploadForm.file = file
  const fallbackTitle = file.name.replace(/\.[^/.]+$/, '')
  if (!officialUploadForm.title) {
    officialUploadForm.title = fallbackTitle
  }
  officialUploadForm.coverUrl = ''

  // 从音频文件提取时长（优先于元数据读取，确保可靠获取）
  const extractDuration = (file) => new Promise((resolve) => {
    const audio = document.createElement('audio')
    const objectUrl = URL.createObjectURL(file)
    audio.src = objectUrl
    const cleanup = () => {
      URL.revokeObjectURL(objectUrl)
      audio.remove()
    }
    audio.addEventListener('loadedmetadata', () => {
      const secs = Number.isFinite(audio.duration) ? Math.floor(audio.duration) : 0
      cleanup()
      resolve(secs)
    }, { once: true })
    audio.addEventListener('error', () => {
      cleanup()
      resolve(0)
    }, { once: true })
    // 超时保护：部分浏览器可能不触发 loadedmetadata
    setTimeout(() => {
      if (Number.isNaN(audio.duration) || audio.duration === Infinity) {
        cleanup()
        resolve(0)
      }
    }, 5000)
  })

  const [metadata, durationFromAudio] = await Promise.all([
    readAudioTagMetadata(file),
    extractDuration(file),
  ])
  officialUploadForm.duration = durationFromAudio || officialUploadForm.duration
  if (metadata.title && (!officialUploadForm.title || officialUploadForm.title === fallbackTitle)) {
    officialUploadForm.title = metadata.title
  }
  if (metadata.artist && !officialUploadForm.artist) {
    officialUploadForm.artist = metadata.artist
  }
  officialUploadForm.coverUrl = metadata.coverUrl || ''
}

const addOfficialUploadTag = () => {
  const tag = officialUploadForm.tagInput.trim()
  if (tag && !officialUploadForm.tags.includes(tag) && tag !== activeOfficialPlaylist.value?.tagName) {
    officialUploadForm.tags.push(tag)
  }
  officialUploadForm.tagInput = ''
}

const removeOfficialUploadTag = (tag) => {
  officialUploadForm.tags = officialUploadForm.tags.filter((item) => item !== tag)
}

const ensureEmotionTagIds = async (tagNames) => {
  const uniqueNames = Array.from(new Set((Array.isArray(tagNames) ? tagNames : []).map((item) => String(item || '').trim()).filter(Boolean)))
  if (!uniqueNames.length) {
    return []
  }

  if (!musicStore.emotionTags.length) {
    await musicStore.fetchEmotionTags()
  }

  const ids = []
  for (const tagName of uniqueNames) {
    let existing = musicStore.emotionTags.find((tag) => String(tag?.tagName || '').trim() === tagName)
    if (!existing) {
      const response = await createEmotionTagApi({ tagName })
      existing = response.data
      await musicStore.fetchEmotionTags()
    }
    if (existing?.id != null) {
      ids.push(Number(existing.id))
    }
  }
  return ids
}

const submitOfficialUpload = async () => {
  if (!activeOfficialPlaylist.value || !officialUploadForm.file || !officialUploadForm.title) return

  try {
    isUploadingOfficialTrack.value = true
    const formData = new FormData()
    formData.append('file', officialUploadForm.file)
    formData.append('title', officialUploadForm.title.trim())
    formData.append('artist', officialUploadForm.artist.trim() || '')
    formData.append('duration', String(Number(officialUploadForm.duration) || 0))
    formData.append('tags', JSON.stringify(officialUploadTags.value))

    const uploadResponse = await uploadMusicFileApi(formData)
    const uploadedTrack = uploadResponse.data || {}
    const fileUrl = uploadedTrack.filename ? getMusicFileUrl(uploadedTrack.filename) : ''
    const resourceResponse = await createMusicResourceApi({
      title: uploadedTrack.title?.trim() || officialUploadForm.title.trim(),
      artist: uploadedTrack.artist?.trim() || officialUploadForm.artist.trim() || null,
      duration: Number(uploadedTrack.duration) || Number(officialUploadForm.duration) || 0,
      fileUrl,
      coverUrl: sanitizePersistedCoverUrl(officialUploadForm.coverUrl),
      source: `${ADMIN_OFFICIAL_SOURCE_PREFIX}${uploadedTrack.id}`,
    })

    const musicId = resourceResponse.data?.id
    const tagIds = await ensureEmotionTagIds(officialUploadTags.value)
    await replaceMusicTagsApi(musicId, {
      tagIds,
      source: `admin-official:${activeOfficialPlaylist.value.id}`,
    })

    await refreshOfficialMusic()
    resetOfficialUploadForm()
    editingOfficialTrackId.value = String(musicId)
    ElMessage.success('官方音乐上传成功')
  } catch (error) {
    console.error('Upload official track failed:', error)
    ElMessage.error(error?.response?.data?.error || '上传官方音乐失败')
  } finally {
    isUploadingOfficialTrack.value = false
  }
}

const selectOfficialTrack = async (track) => {
  editingOfficialTrackId.value = String(track?.id || '')
  await applyOfficialTrackForm(track)
}

const saveOfficialTrack = async () => {
  const track = editingOfficialTrack.value
  const playlist = activeOfficialPlaylist.value
  if (!track || !playlist) return

  const musicId = Number(track.musicResourceId || track.id)
  if (!musicId) {
    ElMessage.warning('当前曲目缺少有效 musicId')
    return
  }

  try {
    isSavingOfficialTrack.value = true
    const nextTagNames = [playlist.tagName]
    const tagIds = await ensureEmotionTagIds(nextTagNames)

    await updateMusicResourceApi(musicId, {
      title: officialTrackForm.title.trim(),
      artist: officialTrackForm.artist.trim() || null,
      duration: Number(officialTrackForm.duration) || 0,
      fileUrl: track.fileUrl || '',
      coverUrl: sanitizePersistedCoverUrl(officialTrackForm.coverUrl) || sanitizePersistedCoverUrl(track.coverUrl),
      source: track.source || '',
    })
    await replaceMusicTagsApi(musicId, {
      tagIds,
      source: `admin-official:${playlist.id}`,
    })

    await refreshOfficialMusic()
    const refreshed = activeOfficialPlaylist.value?.tracks?.find((item) => String(item.id) === String(track.id))
    if (refreshed) {
      await selectOfficialTrack(refreshed)
    }
    ElMessage.success('官方曲目已更新')
  } catch (error) {
    console.error('Save official track failed:', error)
    ElMessage.error('保存官方曲目失败')
  } finally {
    isSavingOfficialTrack.value = false
  }
}

const extractAdminUploadTrackId = (source = '') =>
  String(source || '').startsWith(ADMIN_OFFICIAL_SOURCE_PREFIX)
    ? String(source).slice(ADMIN_OFFICIAL_SOURCE_PREFIX.length)
    : ''

const deleteOfficialTrack = async () => {
  const track = editingOfficialTrack.value
  if (!track) return
  if (!window.confirm(`确定删除曲目《${track.title}》吗？`)) return

  const musicId = Number(track.musicResourceId || track.id)
  const uploadTrackId = extractAdminUploadTrackId(track.source)

  try {
    isDeletingOfficialTrack.value = true
    if (uploadTrackId) {
      await deleteUploadedMusicApi(uploadTrackId)
    }
    await deleteMusicResourceApi(musicId)
    editingOfficialTrackId.value = ''
    await refreshOfficialMusic()
    ElMessage.success('官方曲目已删除')
  } catch (error) {
    console.error('Delete official track failed:', error)
    ElMessage.error(error?.response?.data?.error || '删除官方曲目失败')
  } finally {
    isDeletingOfficialTrack.value = false
  }
}

onMounted(async () => {
  await refreshOfficialMusic()
})

watch(activeOfficialPlaylist, (playlist) => {
  if (!playlist) return
  activeOfficialPlaylistId.value = playlist.id
  applyOfficialPlaylistForm(playlist)
  const nextTrack = playlist.tracks?.find((track) => String(track.id) === String(editingOfficialTrackId.value)) || playlist.tracks?.[0]
  if (nextTrack) {
    selectOfficialTrack(nextTrack)
  } else {
    editingOfficialTrackId.value = ''
  }
}, { immediate: true })
</script>

<style scoped>
.glass-card {
  background: rgba(255, 255, 255, 0.78);
  border: 1px solid rgba(255, 255, 255, 0.7);
  box-shadow: 0 18px 40px rgba(91, 115, 105, 0.12);
  backdrop-filter: blur(18px);
  border-radius: 24px;
}

.panel {
  padding: 22px;
}

.music-admin-panel {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.panel-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
}

.panel-head h2 {
  margin: 0;
}

.panel-head p {
  margin: 6px 0 0;
  color: #6a7c76;
}

.panel-head-actions {
  display: flex;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
}

.official-layout {
  display: grid;
  grid-template-columns: 280px minmax(0, 1fr);
  gap: 18px;
  min-height: 0;
}

.official-playlist-nav {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.official-playlist-chip {
  width: 100%;
  border: 1px solid rgba(125, 159, 143, 0.18);
  background: rgba(255, 255, 255, 0.8);
  border-radius: 18px;
  padding: 14px;
  text-align: left;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.official-playlist-chip.active {
  border-color: rgba(75, 126, 102, 0.42);
  box-shadow: 0 10px 24px rgba(94, 136, 116, 0.14);
}

.official-playlist-chip strong {
  color: #29453b;
}

.official-playlist-chip span {
  color: #6b7d77;
}

.official-main,
.official-grid,
.official-track-layout,
.official-track-list {
  min-height: 0;
}

.official-main {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.official-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}

.official-card {
  padding: 18px;
  border-radius: 20px;
  background: rgba(244, 248, 246, 0.82);
  border: 1px solid rgba(125, 159, 143, 0.14);
}

.compact-head {
  margin-bottom: 10px;
}

.form-grid,
.official-track-layout,
.official-track-list {
  margin-top: 18px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.form-grid label {
  display: flex;
  flex-direction: column;
  gap: 8px;
  color: #405752;
}

.form-grid input,
.form-grid select,
.form-grid textarea {
  width: 100%;
  border: 1px solid rgba(125, 159, 143, 0.24);
  border-radius: 14px;
  padding: 12px 14px;
  background: rgba(255, 255, 255, 0.88);
  font: inherit;
}

.full-width {
  grid-column: 1 / -1;
}

.action-row {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
  margin-top: 14px;
}

.file-row,
.tag-editor {
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
}

.hidden-file-input {
  display: none;
}

.file-name,
.empty-state,
.official-track-copy span,
.official-track-copy small {
  color: #6b7d77;
}

.upload-cover-preview {
  min-height: 112px;
  border: 1px dashed rgba(125, 159, 143, 0.28);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.72);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 12px;
}

.upload-cover-image {
  width: 96px;
  height: 96px;
  border-radius: 16px;
  object-fit: cover;
}

.upload-cover-placeholder {
  color: #6b7d77;
  font-size: 0.92rem;
  text-align: center;
}

.small-tag,
.tag {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(54, 95, 77, 0.08);
  color: #365f4d;
}

.tag-remove-btn {
  border: none;
  background: transparent;
  color: inherit;
  cursor: pointer;
  padding: 0;
  line-height: 1;
}

.official-track-layout {
  display: grid;
  grid-template-columns: minmax(320px, 0.95fr) minmax(0, 1.05fr);
  gap: 18px;
}

.official-track-list {
  max-height: 540px;
  overflow: auto;
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding-right: 4px;
}

.official-track-row {
  width: 100%;
  display: grid;
  grid-template-columns: 64px minmax(0, 1fr);
  gap: 12px;
  border: 1px solid rgba(125, 159, 143, 0.18);
  background: rgba(255, 255, 255, 0.78);
  border-radius: 18px;
  padding: 12px;
  text-align: left;
  cursor: pointer;
}

.official-track-row.active {
  border-color: rgba(75, 126, 102, 0.42);
  box-shadow: 0 10px 24px rgba(94, 136, 116, 0.14);
}

.official-track-cover {
  width: 64px;
  height: 64px;
  border-radius: 14px;
  object-fit: cover;
}

.official-track-copy {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.official-track-copy strong,
.official-track-copy span,
.official-track-copy small {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.official-track-editor {
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.76);
  padding: 16px;
}

.primary-btn,
.ghost-btn,
.danger-btn {
  border: none;
  border-radius: 14px;
  padding: 12px 16px;
  font: inherit;
  cursor: pointer;
}

.primary-btn {
  background: #365f4d;
  color: #fff;
}

.ghost-btn {
  background: rgba(54, 95, 77, 0.08);
  color: #365f4d;
}

.danger-btn {
  background: #cb4d58;
  color: #fff;
}

.compact-btn {
  padding: 8px 12px;
}

@media (max-width: 1100px) {
  .official-layout,
  .official-grid,
  .official-track-layout {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .panel-head,
  .action-row {
    flex-direction: column;
    align-items: stretch;
  }

  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
