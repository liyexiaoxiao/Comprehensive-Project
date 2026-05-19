<template>
  <div class="admin-page">
    <div class="admin-shell">
      <header class="admin-header glass-card">
        <div>
          <p class="admin-kicker">Admin Console</p>
          <h1>管理员端</h1>
          <p class="admin-subtitle">集中管理用户资料与冥想日志。</p>
        </div>
        <div class="admin-header-actions">
          <span class="admin-badge">{{ currentAdminName }}</span>
          <RouterLink class="header-link" to="/service">返回服务页</RouterLink>
        </div>
      </header>

      <section class="admin-grid">
        <article class="glass-card panel">
          <div class="panel-head">
            <div>
              <h2>用户管理</h2>
              <p>分页查看、编辑资料、重置密码、删除用户</p>
            </div>
            <div class="panel-head-actions">
              <button class="ghost-btn" type="button" @click="refreshUsers" :disabled="isUserListLoading">
                {{ isUserListLoading ? '加载中...' : '刷新列表' }}
              </button>
            </div>
          </div>

          <div class="toolbar">
            <input v-model.trim="userKeyword" type="text" placeholder="筛选当前页用户名 / 昵称 / 邮箱" class="toolbar-input" />
            <div class="pager">
              <button class="ghost-btn" type="button" @click="loadPrevPage" :disabled="pageIndex === 0 || isUserListLoading">上一页</button>
              <span>第 {{ pageIndex + 1 }} 页</span>
              <button class="ghost-btn" type="button" @click="loadNextPage" :disabled="!canLoadNext || isUserListLoading">下一页</button>
            </div>
          </div>

          <div class="user-list">
            <button
              v-for="user in filteredUsers"
              :key="user.userId"
              type="button"
              class="user-row"
              :class="{ active: user.userId === selectedUserId }"
              @click="selectUser(user.userId)"
            >
              <img :src="resolveAvatar(user.avatarUrl)" alt="" class="user-avatar" />
              <div class="user-row-main">
                <strong>{{ user.nickname || user.username }}</strong>
                <span>{{ user.username }}</span>
                <span>{{ user.email || '未设置邮箱' }}</span>
              </div>
              <div class="user-row-tags">
                <span class="tag">{{ normalizeRole(user.role) }}</span>
                <span class="tag muted">{{ normalizeStatus(user.status) }}</span>
              </div>
            </button>

            <div v-if="!filteredUsers.length" class="empty-state">
              当前页没有匹配用户。
            </div>
          </div>
        </article>

        <article class="glass-card panel detail-panel">
          <div class="panel-head">
            <div>
              <h2>用户详情</h2>
              <p v-if="selectedUserId">当前用户 ID：{{ selectedUserId }}</p>
              <p v-else>请先从左侧选择用户</p>
            </div>
          </div>

          <template v-if="selectedUserId">
            <div class="form-grid">
              <label>
                <span>用户名</span>
                <input v-model.trim="userForm.username" type="text" />
              </label>
              <label>
                <span>昵称</span>
                <input v-model.trim="userForm.nickname" type="text" />
              </label>
              <label>
                <span>邮箱</span>
                <input v-model.trim="userForm.email" type="email" />
              </label>
              <label>
                <span>手机号</span>
                <input v-model.trim="userForm.phone" type="text" />
              </label>
              <label>
                <span>角色</span>
                <select v-model="userForm.role">
                  <option value="USER">普通用户</option>
                  <option value="ADMIN">管理员</option>
                </select>
              </label>
              <label>
                <span>状态</span>
                <select v-model="userForm.status">
                  <option value="ACTIVE">ACTIVE</option>
                  <option value="DISABLED">DISABLED</option>
                </select>
              </label>
              <label class="full-width">
                <span>头像地址</span>
                <input v-model.trim="userForm.avatarUrl" type="text" />
              </label>
              <label class="full-width">
                <span>简介</span>
                <textarea v-model="userForm.bio" rows="4"></textarea>
              </label>
            </div>

            <div class="action-row">
              <button class="primary-btn" type="button" @click="saveUser" :disabled="isSavingUser">
                {{ isSavingUser ? '保存中...' : '保存用户资料' }}
              </button>
              <button class="danger-btn" type="button" @click="deleteUser" :disabled="isDeletingUser">
                {{ isDeletingUser ? '删除中...' : '删除用户' }}
              </button>
            </div>

            <div class="reset-box">
              <label>
                <span>重置密码</span>
                <input v-model.trim="resetPasswordForm.newPassword" type="password" placeholder="输入新密码" />
              </label>
              <button class="ghost-btn" type="button" @click="resetPassword" :disabled="isResettingPassword">执行重置</button>
            </div>

            <div class="panel-head sub-head">
              <div>
                <h2>冥想日志管理</h2>
                <p>查看该用户全部冥想日志，并支持管理员新增 / 删除</p>
              </div>
              <button class="ghost-btn" type="button" @click="fetchMeditationLogs" :disabled="isMeditationLoading">
                {{ isMeditationLoading ? '加载中...' : '刷新日志' }}
              </button>
            </div>

            <div class="form-grid meditation-form">
              <label>
                <span>开始时间</span>
                <input v-model="meditationForm.startTime" type="datetime-local" />
              </label>
              <label>
                <span>时长（分钟）</span>
                <input v-model.number="meditationForm.duration" type="number" min="1" />
              </label>
              <label>
                <span>musicId</span>
                <input v-model.trim="meditationForm.musicId" type="text" />
              </label>
              <label>
                <span>imageId</span>
                <input v-model.number="meditationForm.imageId" type="number" min="1" />
              </label>
            </div>
            <div class="action-row">
              <button class="primary-btn" type="button" @click="createMeditationLog" :disabled="isSavingMeditation">
                {{ isSavingMeditation ? '提交中...' : '新增冥想日志' }}
              </button>
            </div>

            <div class="log-list">
              <div v-for="log in meditationLogs" :key="log.id" class="log-row">
                <div>
                  <strong>{{ formatDateTime(log.startTime) }}</strong>
                  <span>时长：{{ log.duration || 0 }} 分钟</span>
                  <span>musicId：{{ log.musicId || '-' }} / imageId：{{ log.imageId || '-' }}</span>
                </div>
                <button class="danger-btn compact-btn" type="button" @click="deleteMeditationLog(log.id)">删除</button>
              </div>
              <div v-if="!meditationLogs.length" class="empty-state">暂无冥想日志。</div>
            </div>
          </template>
        </article>
      </section>

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
                      <img :src="track.cover || fallbackOfficialCover(track)" alt="" class="official-track-cover" />
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
                        <span>标签（用逗号或空格分隔）</span>
                        <textarea v-model.trim="officialTrackForm.tagsText" rows="3"></textarea>
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
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { RouterLink } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useMusicStore } from '@/stores/musicStore'
import {
  deleteAdminUserApi,
  getAdminUserByIdApi,
  getAdminUsersApi,
  getCurrentUserFromStorage,
  isAdminUser,
  resetAdminUserPasswordApi,
  resolveUserAvatarUrl,
  updateAdminUserApi,
} from '@/api/user'
import {
  deleteAdminMeditationLogApi,
  getAdminMeditationLogsApi,
  saveAdminMeditationLogApi,
} from '@/api/meditation'
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
import { getRealMusicCover } from '@/utils/realMusic'

const pageSize = 20
const ADMIN_OFFICIAL_SOURCE_PREFIX = 'admin-official-upload:'
const pageIndex = ref(0)
const canLoadNext = ref(false)
const users = ref([])
const userKeyword = ref('')
const selectedUserId = ref(null)
const meditationLogs = ref([])
const isUserListLoading = ref(false)
const isSavingUser = ref(false)
const isDeletingUser = ref(false)
const isResettingPassword = ref(false)
const isMeditationLoading = ref(false)
const isSavingMeditation = ref(false)
const isOfficialMusicLoading = ref(false)
const isUploadingOfficialTrack = ref(false)
const isSavingOfficialTrack = ref(false)
const isDeletingOfficialTrack = ref(false)
const officialAudioInputRef = ref(null)
const activeOfficialPlaylistId = ref(getDefaultOfficialPlaylists()[0]?.id || '')
const editingOfficialTrackId = ref('')
const musicStore = useMusicStore()
const officialPlaylistConfigs = ref(getDefaultOfficialPlaylists())

const currentUser = computed(() => getCurrentUserFromStorage())
const currentAdminName = computed(() => currentUser.value?.nickname || currentUser.value?.username || '管理员')

const userForm = reactive({
  username: '',
  nickname: '',
  email: '',
  phone: '',
  role: 'ROLE_USER',
  status: 'ACTIVE',
  avatarUrl: '',
  bio: '',
})

const resetPasswordForm = reactive({
  newPassword: '',
})

const meditationForm = reactive({
  startTime: '',
  duration: 20,
  musicId: '',
  imageId: null,
})

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

const filteredUsers = computed(() => {
  const keyword = userKeyword.value.trim().toLowerCase()
  if (!keyword) return users.value
  return users.value.filter((user) => {
    const fields = [user.username, user.nickname, user.email, user.phone, user.userId]
    return fields.some((field) => String(field || '').toLowerCase().includes(keyword))
  })
})

const normalizeRoleValue = (role) => {
  const raw = String(role || '').trim().toUpperCase()
  return raw.startsWith('ROLE_') ? raw.slice(5) : raw
}
const normalizeRole = (role) => (normalizeRoleValue(role) === 'ADMIN' ? '管理员' : '普通用户')
const normalizeStatus = (status) => String(status || 'ACTIVE').toUpperCase()
const resolveAvatar = (avatarUrl) => resolveUserAvatarUrl(avatarUrl) || '/images/feature-img-2.jpg'
const fallbackOfficialCover = (track) => getRealMusicCover(track?.emotion || track?.tags?.[0] || activeOfficialPlaylist.value?.coverEmotion || 'neutral')
const formatDateTime = (value) => {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return String(value)
  return date.toLocaleString('zh-CN', { hour12: false })
}

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

const applyUserForm = (user) => {
  userForm.username = user?.username || ''
  userForm.nickname = user?.nickname || ''
  userForm.email = user?.email || ''
  userForm.phone = user?.phone || ''
  userForm.role = normalizeRoleValue(user?.role) || 'USER'
  userForm.status = user?.status || 'ACTIVE'
  userForm.avatarUrl = user?.avatarUrl || ''
  userForm.bio = user?.bio || ''
}

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

const applyOfficialTrackForm = (track) => {
  officialTrackForm.title = track?.title || ''
  officialTrackForm.artist = track?.artist || ''
  officialTrackForm.duration = Number(track?.duration) || 0
  officialTrackForm.coverUrl = track?.coverUrl || ''
  officialTrackForm.tagsText = Array.isArray(track?.tags) ? track.tags.join(', ') : ''
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
  const audio = document.createElement('audio')
  audio.src = URL.createObjectURL(file)
  audio.onloadedmetadata = () => {
    officialUploadForm.duration = Number.isFinite(audio.duration) ? Math.floor(audio.duration) : 0
    URL.revokeObjectURL(audio.src)
  }

  const metadata = await readAudioTagMetadata(file)
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

const selectOfficialTrack = (track) => {
  editingOfficialTrackId.value = String(track?.id || '')
  applyOfficialTrackForm(track)
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
    const nextTagNames = Array.from(new Set([playlist.tagName, ...parseTagNames(officialTrackForm.tagsText)]))
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
      selectOfficialTrack(refreshed)
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

const fetchUsers = async () => {
  isUserListLoading.value = true
  try {
    const response = await getAdminUsersApi({ offset: pageIndex.value * pageSize, limit: pageSize })
    users.value = Array.isArray(response.data) ? response.data : []
    canLoadNext.value = users.value.length === pageSize
    if (!selectedUserId.value && users.value.length) {
      await selectUser(users.value[0].userId)
    } else if (selectedUserId.value && !users.value.some((item) => item.userId === selectedUserId.value)) {
      selectedUserId.value = null
      meditationLogs.value = []
    }
  } catch (error) {
    console.error('Failed to fetch admin users:', error)
    ElMessage.error('获取用户列表失败')
  } finally {
    isUserListLoading.value = false
  }
}

const fetchMeditationLogs = async () => {
  if (!selectedUserId.value) return
  isMeditationLoading.value = true
  try {
    const response = await getAdminMeditationLogsApi(selectedUserId.value)
    meditationLogs.value = Array.isArray(response.data) ? response.data : []
  } catch (error) {
    console.error('Failed to fetch meditation logs:', error)
    ElMessage.error('获取冥想日志失败')
  } finally {
    isMeditationLoading.value = false
  }
}

const selectUser = async (userId) => {
  selectedUserId.value = userId
  try {
    const [userResponse] = await Promise.all([
      getAdminUserByIdApi(userId),
      fetchMeditationLogs(),
    ])
    applyUserForm(userResponse.data || {})
  } catch (error) {
    console.error('Failed to fetch admin user detail:', error)
    ElMessage.error('获取用户详情失败')
  }
}

const saveUser = async () => {
  if (!selectedUserId.value) return
  isSavingUser.value = true
  try {
    await updateAdminUserApi(selectedUserId.value, {
      ...userForm,
      role: normalizeRoleValue(userForm.role),
    })
    ElMessage.success('用户资料已更新')
    await fetchUsers()
    await selectUser(selectedUserId.value)
  } catch (error) {
    ElMessage.error(error?.response?.data || '更新用户失败')
  } finally {
    isSavingUser.value = false
  }
}

const resetPassword = async () => {
  if (!selectedUserId.value) return
  if (!resetPasswordForm.newPassword) {
    ElMessage.warning('请先填写新密码')
    return
  }
  isResettingPassword.value = true
  try {
    await resetAdminUserPasswordApi(selectedUserId.value, { newPassword: resetPasswordForm.newPassword })
    resetPasswordForm.newPassword = ''
    ElMessage.success('密码已重置')
  } catch (error) {
    ElMessage.error(error?.response?.data || '重置密码失败')
  } finally {
    isResettingPassword.value = false
  }
}

const deleteUser = async () => {
  if (!selectedUserId.value) return
  const targetId = selectedUserId.value
  if (currentUser.value?.userId === targetId) {
    ElMessage.warning('暂不支持在管理员端删除当前登录账号')
    return
  }
  if (!window.confirm(`确定删除用户 ${targetId} 吗？该操作不可恢复。`)) return
  isDeletingUser.value = true
  try {
    await deleteAdminUserApi(targetId)
    ElMessage.success('用户已删除')
    selectedUserId.value = null
    meditationLogs.value = []
    await fetchUsers()
  } catch (error) {
    ElMessage.error(error?.response?.data || '删除用户失败')
  } finally {
    isDeletingUser.value = false
  }
}

const createMeditationLog = async () => {
  if (!selectedUserId.value) return
  if (!meditationForm.duration || meditationForm.duration <= 0) {
    ElMessage.warning('请输入有效时长')
    return
  }
  isSavingMeditation.value = true
  try {
    await saveAdminMeditationLogApi(selectedUserId.value, {
      startTime: meditationForm.startTime || null,
      duration: meditationForm.duration,
      musicId: meditationForm.musicId || null,
      imageId: meditationForm.imageId || null,
    })
    ElMessage.success('冥想日志已新增')
    await fetchMeditationLogs()
  } catch (error) {
    ElMessage.error(error?.response?.data || '新增冥想日志失败')
  } finally {
    isSavingMeditation.value = false
  }
}

const deleteMeditationLog = async (logId) => {
  if (!window.confirm(`确定删除冥想日志 ${logId} 吗？`)) return
  try {
    await deleteAdminMeditationLogApi(logId)
    ElMessage.success('冥想日志已删除')
    await fetchMeditationLogs()
  } catch (error) {
    ElMessage.error(error?.response?.data || '删除冥想日志失败')
  }
}

const refreshUsers = async () => {
  await fetchUsers()
}

const loadPrevPage = async () => {
  if (pageIndex.value <= 0) return
  pageIndex.value -= 1
  await fetchUsers()
}

const loadNextPage = async () => {
  if (!canLoadNext.value) return
  pageIndex.value += 1
  await fetchUsers()
}

onMounted(async () => {
  if (!isAdminUser(currentUser.value)) {
    ElMessage.error('当前账号没有管理员权限')
    return
  }
  await fetchUsers()
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
.admin-page {
  min-height: 100vh;
  padding: 24px;
  background:
    radial-gradient(circle at top left, rgba(157, 195, 255, 0.28), transparent 28%),
    radial-gradient(circle at top right, rgba(155, 232, 190, 0.24), transparent 32%),
    linear-gradient(180deg, #f6fbff 0%, #eef5f2 100%);
}

.admin-shell {
  max-width: 1440px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.glass-card {
  background: rgba(255, 255, 255, 0.78);
  border: 1px solid rgba(255, 255, 255, 0.7);
  box-shadow: 0 18px 40px rgba(91, 115, 105, 0.12);
  backdrop-filter: blur(18px);
  border-radius: 24px;
}

.admin-header {
  padding: 24px 28px;
  display: flex;
  justify-content: space-between;
  gap: 24px;
  align-items: center;
  margin-bottom: 20px;
}

.admin-kicker {
  margin: 0 0 6px;
  font-size: 0.85rem;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: #6e8f80;
}

.admin-header h1,
.panel-head h2 {
  margin: 0;
}

.admin-subtitle,
.panel-head p {
  margin: 6px 0 0;
  color: #6a7c76;
}

.admin-header-actions {
  display: flex;
  gap: 12px;
  align-items: center;
}

.admin-badge,
.header-link,
.tag {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 8px 14px;
  border-radius: 999px;
  background: rgba(125, 159, 143, 0.14);
  color: #37564a;
  text-decoration: none;
}

.admin-grid {
  display: grid;
  grid-template-columns: 420px 1fr;
  gap: 20px;
}

.panel {
  padding: 22px;
}

.panel-head,
.toolbar {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
}

.panel-head-actions {
  display: flex;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
}

.toolbar {
  flex-wrap: wrap;
  row-gap: 12px;
  column-gap: 16px;
  align-items: flex-end;
}

.toolbar-input {
  min-width: 220px;
  max-width: 380px;
}

.action-row {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
  margin-top: 14px;
}

.action-row .primary-btn,
.action-row .danger-btn,
.action-row .ghost-btn {
  min-width: 140px;
}

.reset-box {
  display: grid;
  grid-template-columns: minmax(360px, 720px) auto;
  column-gap: 16px;
  row-gap: 12px;
  justify-content: start;
  align-items: end;
}

.music-admin-panel {
  display: flex;
  flex-direction: column;
  gap: 18px;
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

.file-name {
  color: #6b7d77;
  word-break: break-all;
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

.small-tag {
  display: inline-flex;
  align-items: center;
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

.official-track-copy span,
.official-track-copy small {
  color: #6b7d77;
}

.official-track-editor {
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.76);
  padding: 16px;
}

.reset-box .ghost-btn {
  min-width: 120px;
  white-space: nowrap;
  height: 44px;
}

.toolbar,
.reset-box,
.sub-head,
.log-list,
.form-grid {
  margin-top: 18px;
}

.toolbar-input,
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

.full-width {
  grid-column: 1 / -1;
}

.pager,
.user-row,
.user-row-main,
.user-row-tags,
.log-row {
  display: flex;
  gap: 12px;
}

.pager {
  align-items: center;
  flex-shrink: 0;
  white-space: nowrap;
  flex-wrap: nowrap;
}

.pager span {
  padding: 8px 12px;
  border-radius: 999px;
  background: rgba(54, 95, 77, 0.08);
  color: #365f4d;
}

.user-list,
.log-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.user-row,
.log-row {
  width: 100%;
  border: 1px solid rgba(125, 159, 143, 0.18);
  background: rgba(255, 255, 255, 0.72);
  border-radius: 18px;
  padding: 14px;
  align-items: center;
}

.user-row {
  cursor: pointer;
  text-align: left;
}

.user-row.active {
  border-color: rgba(75, 126, 102, 0.4);
  box-shadow: 0 10px 24px rgba(94, 136, 116, 0.14);
}

.user-avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  object-fit: cover;
  background: #eff5f2;
}

.user-row-main {
  flex: 1;
  flex-direction: column;
}

.user-row-main span,
.log-row span,
.empty-state {
  color: #6b7d77;
}

.user-row-tags {
  flex-direction: column;
  align-items: flex-end;
}

.tag.muted {
  background: rgba(120, 126, 155, 0.12);
  color: #56607f;
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

.reset-box {
  padding: 16px;
  border-radius: 18px;
  background: rgba(244, 248, 246, 0.92);
}

.reset-box label {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.log-row {
  justify-content: space-between;
}

.log-row > div {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

@media (max-width: 1100px) {
  .admin-grid {
    grid-template-columns: 1fr;
  }

  .official-layout,
  .official-grid,
  .official-track-layout {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .admin-page {
    padding: 14px;
  }

  .admin-header,
  .panel-head,
  .toolbar,
  .action-row,
  .log-row {
    flex-direction: column;
    align-items: stretch;
  }

  .reset-box {
    grid-template-columns: 1fr;
    align-items: stretch;
    justify-content: stretch;
  }

  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
