<template>
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
          <label class="full-width avatar-readonly-field">
            <span>头像</span>
            <div class="avatar-readonly-row">
              <img :src="resolveAvatar(userForm.avatarUrl)" alt="" class="user-avatar preview-avatar" />
              <p class="avatar-readonly-hint">头像由用户通过上传接口设置，管理员不可填写外部链接。</p>
            </div>
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
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
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
import { formatApiDateTime } from '@/utils/dateTime'

const pageSize = 20
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

const currentUser = computed(() => getCurrentUserFromStorage())

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
const formatDateTime = (value) => {
  if (!value) return '-'
  return formatApiDateTime(value, {}, String(value))
}

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

const PASSWORD_PATTERN = /^(?=.*[A-Za-z])(?=.*\d).{8,}$/

const saveUser = async () => {
  if (!selectedUserId.value) return
  isSavingUser.value = true
  try {
    const { avatarUrl: _avatarUrl, ...profilePayload } = userForm
    await updateAdminUserApi(selectedUserId.value, {
      ...profilePayload,
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
  if (!PASSWORD_PATTERN.test(resetPasswordForm.newPassword)) {
    ElMessage.warning('密码至少 8 位，且需同时包含字母和数字')
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
})
</script>

<style scoped>
.admin-grid {
  display: grid;
  grid-template-columns: 420px 1fr;
  gap: 20px;
}

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

.panel-head,
.toolbar {
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

.toolbar {
  flex-wrap: wrap;
  row-gap: 12px;
  column-gap: 16px;
  align-items: flex-end;
}

.toolbar,
.reset-box,
.sub-head,
.log-list,
.form-grid {
  margin-top: 18px;
}

.toolbar-input {
  min-width: 220px;
  max-width: 380px;
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
  padding: 16px;
  border-radius: 18px;
  background: rgba(244, 248, 246, 0.92);
}

.reset-box label {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.reset-box .ghost-btn {
  min-width: 120px;
  white-space: nowrap;
  height: 44px;
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

.pager span,
.tag {
  display: inline-flex;
  align-items: center;
  justify-content: center;
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

.avatar-readonly-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.preview-avatar {
  flex-shrink: 0;
}

.avatar-readonly-hint {
  margin: 0;
  color: #6b7d77;
  font-size: 13px;
  line-height: 1.5;
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
}

@media (max-width: 720px) {
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
