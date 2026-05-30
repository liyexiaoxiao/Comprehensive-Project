<template>
  <section class="admin-grid">
    <article class="glass-card panel">
      <div class="panel-head">
        <div>
          <h2>服务反馈管理</h2>
          <p>查看普通用户对情绪数据、冥想、音乐、社区与 AI 能力的反馈内容。</p>
        </div>
        <button class="ghost-btn" type="button" :disabled="isListLoading" @click="fetchFeedbackList">
          {{ isListLoading ? '加载中...' : '刷新列表' }}
        </button>
      </div>

      <div class="stat-grid">
        <div class="stat-card">
          <strong>{{ feedbackStats.total }}</strong>
          <span>反馈总数</span>
        </div>
        <div class="stat-card">
          <strong>{{ feedbackStats.averageRating }}</strong>
          <span>平均评分</span>
        </div>
        <div class="stat-card">
          <strong>{{ feedbackStats.userCount }}</strong>
          <span>涉及用户</span>
        </div>
        <div class="stat-card">
          <strong>{{ feedbackStats.serviceCount }}</strong>
          <span>服务类型</span>
        </div>
      </div>

      <div class="filter-grid">
        <label>
          <span>服务类型</span>
          <select v-model="filters.service">
            <option v-for="option in feedbackServiceOptions" :key="option.value || 'all'" :value="option.value">
              {{ option.label }}
            </option>
          </select>
        </label>
        <label>
          <span>用户 ID</span>
          <input v-model.trim="filters.userId" type="text" placeholder="按用户筛选" />
        </label>
        <label>
          <span>最低评分</span>
          <select v-model="filters.minRating">
            <option value="">不限</option>
            <option v-for="score in feedbackRatingOptions" :key="`min-${score}`" :value="String(score)">
              {{ score }} 分及以上
            </option>
          </select>
        </label>
        <label>
          <span>最高评分</span>
          <select v-model="filters.maxRating">
            <option value="">不限</option>
            <option v-for="score in feedbackRatingOptions" :key="`max-${score}`" :value="String(score)">
              {{ score }} 分及以下
            </option>
          </select>
        </label>
        <label>
          <span>开始时间</span>
          <input v-model="filters.startDate" type="datetime-local" />
        </label>
        <label>
          <span>结束时间</span>
          <input v-model="filters.endDate" type="datetime-local" />
        </label>
      </div>

      <div class="filter-actions">
        <button class="primary-btn" type="button" :disabled="isListLoading" @click="fetchFeedbackList">
          应用筛选
        </button>
        <button class="ghost-btn" type="button" :disabled="isListLoading" @click="resetFilters">
          重置筛选
        </button>
      </div>

      <div class="feedback-list">
        <button
          v-for="item in feedbackList"
          :key="item.id"
          type="button"
          class="feedback-row"
          :class="{ active: item.id === selectedFeedbackId }"
          @click="selectFeedback(item.id)"
        >
          <div class="feedback-row-top">
            <span class="pill">{{ item.service || '未分类服务' }}</span>
            <span class="pill muted">{{ formatRating(item.rating) }}</span>
          </div>
          <strong>{{ resolveUserDisplayName(item.userId) }}</strong>
          <span class="feedback-meta">用户 ID：{{ item.userId || '-' }}</span>
          <p>{{ summarizeFeedback(item.feedback) }}</p>
          <span class="feedback-meta">{{ formatDateTime(item.createdAt) }}</span>
        </button>

        <div v-if="!feedbackList.length" class="empty-state">
          当前筛选条件下暂无服务反馈。
        </div>
      </div>
    </article>

    <article class="glass-card panel detail-panel">
      <div class="panel-head">
        <div>
          <h2>反馈详情</h2>
          <p v-if="selectedFeedbackDetail">反馈 ID：{{ selectedFeedbackDetail.id }}</p>
          <p v-else>请先从左侧选择一条反馈</p>
        </div>
        <button
          v-if="selectedFeedbackId"
          class="ghost-btn"
          type="button"
          :disabled="isDetailLoading"
          @click="selectFeedback(selectedFeedbackId)"
        >
          {{ isDetailLoading ? '加载中...' : '刷新详情' }}
        </button>
      </div>

      <template v-if="selectedFeedbackDetail">
        <div class="detail-summary-grid">
          <div class="detail-card">
            <span class="detail-label">用户</span>
            <strong>{{ resolveUserDisplayName(selectedFeedbackDetail.userId) }}</strong>
            <span class="detail-value">用户 ID：{{ selectedFeedbackDetail.userId }}</span>
          </div>
          <div class="detail-card">
            <span class="detail-label">服务类型</span>
            <strong>{{ selectedFeedbackDetail.service || '未分类服务' }}</strong>
            <span class="detail-value">评分：{{ formatRating(selectedFeedbackDetail.rating) }}</span>
          </div>
          <div class="detail-card">
            <span class="detail-label">提交时间</span>
            <strong>{{ formatDateTime(selectedFeedbackDetail.createdAt) }}</strong>
            <span class="detail-value">反馈编号：{{ selectedFeedbackDetail.id }}</span>
          </div>
        </div>

        <div class="detail-content-box">
          <div class="detail-section-head">
            <div>
              <h3>反馈正文</h3>
              <p>这里展示管理员通过详情接口读取到的完整反馈内容。</p>
            </div>
          </div>
          <div class="detail-content">
            {{ selectedFeedbackDetail.feedback || '该反馈未填写正文。' }}
          </div>
        </div>

        <div class="detail-content-box">
          <div class="detail-section-head">
            <div>
              <h3>用户偏好画像</h3>
              <p>基于该用户历史反馈聚合出的各服务平均评分。</p>
            </div>
            <button class="ghost-btn compact-btn" type="button" @click="applyUserFilter(selectedFeedbackDetail.userId)">
              查看该用户全部反馈
            </button>
          </div>

          <div v-if="selectedUserPreferenceCards.length" class="preference-grid">
            <article v-for="item in selectedUserPreferenceCards" :key="item.service" class="preference-item">
              <div class="preference-top">
                <strong>{{ item.service }}</strong>
                <span>{{ item.scoreLabel }}</span>
              </div>
              <div class="progress-track">
                <div class="progress-bar" :style="{ width: `${item.percent}%` }"></div>
              </div>
            </article>
          </div>

          <div v-else class="empty-state">
            该用户目前还没有可展示的偏好画像。
          </div>
        </div>
      </template>
    </article>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  getAdminFeedbackApi,
  getAdminFeedbackByIdApi,
  getAdminUserFeedbackPreferencesApi,
  getUserSummariesApi,
} from '@/api/user'
import { formatApiDateTime, parseApiDateTime } from '@/utils/dateTime'

const feedbackServiceOptions = [
  { value: '', label: '全部服务' },
  { value: '情绪数据', label: '情绪数据' },
  { value: '冥想服务', label: '冥想服务' },
  { value: '音乐服务', label: '音乐服务' },
  { value: '朋友圈', label: '朋友圈' },
  { value: '聊天室', label: '聊天室' },
  { value: 'AI陪伴', label: 'AI陪伴' },
]
const feedbackRatingOptions = [1, 2, 3, 4, 5]

const filters = reactive({
  service: '',
  userId: '',
  minRating: '',
  maxRating: '',
  startDate: '',
  endDate: '',
})

const feedbackList = ref([])
const selectedFeedbackId = ref(null)
const selectedFeedbackDetail = ref(null)
const selectedUserPreference = ref(null)
const userSummaries = ref({})
const isListLoading = ref(false)
const isDetailLoading = ref(false)

const normalizeDateTimeParam = (value) => {
  const normalized = String(value || '').trim()
  if (!normalized) return ''
  return /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}$/.test(normalized) ? `${normalized}:00` : normalized
}

const buildQueryParams = () => {
  const params = {}
  const trimmedUserId = String(filters.userId || '').trim()

  if (filters.service) params.service = filters.service
  if (trimmedUserId) {
    if (!/^\d+$/.test(trimmedUserId)) {
      throw new Error('用户 ID 只能输入数字')
    }
    params.userId = Number(trimmedUserId)
  }
  if (filters.minRating) params.minRating = Number(filters.minRating)
  if (filters.maxRating) params.maxRating = Number(filters.maxRating)
  if (filters.startDate) params.startDate = normalizeDateTimeParam(filters.startDate)
  if (filters.endDate) params.endDate = normalizeDateTimeParam(filters.endDate)
  return params
}

const normalizeFeedbackList = (items) => {
  return (Array.isArray(items) ? items : [])
    .map((item) => ({
      id: item?.id ?? null,
      userId: item?.userId ?? null,
      service: item?.service || '',
      feedback: item?.feedback || '',
      rating: Number(item?.rating || 0),
      createdAt: item?.createdAt || '',
    }))
    .sort((left, right) => parseApiDateTime(right.createdAt) - parseApiDateTime(left.createdAt))
}

const feedbackStats = computed(() => {
  const items = feedbackList.value
  if (!items.length) {
    return {
      total: 0,
      averageRating: '-',
      userCount: 0,
      serviceCount: 0,
    }
  }

  const ratingSum = items.reduce((sum, item) => sum + Number(item.rating || 0), 0)
  return {
    total: items.length,
    averageRating: (ratingSum / items.length).toFixed(1),
    userCount: new Set(items.map((item) => item.userId).filter((id) => id !== null && id !== undefined)).size,
    serviceCount: new Set(items.map((item) => item.service).filter(Boolean)).size,
  }
})

const selectedUserPreferenceCards = computed(() => {
  const scores = selectedUserPreference.value?.serviceScores
  if (!Array.isArray(scores)) return []
  return [...scores]
    .map((item) => {
      const score = Number(item?.score || 0)
      return {
        service: item?.service || '未命名服务',
        score,
        scoreLabel: `${score.toFixed(1)} / 5`,
        percent: Math.max(0, Math.min(100, score * 20)),
      }
    })
    .sort((left, right) => right.score - left.score)
})

const formatDateTime = (value) => formatApiDateTime(value, {}, '未知时间')
const formatRating = (value) => `${Number(value || 0).toFixed(1)} / 5`
const summarizeFeedback = (content) => {
  const text = String(content || '').trim()
  if (!text) return '该反馈未填写具体内容。'
  return text.length > 88 ? `${text.slice(0, 88)}...` : text
}

const resolveUserDisplayName = (userId) => {
  const summary = userSummaries.value[userId]
  if (!summary) {
    return userId ? `用户 #${userId}` : '未知用户'
  }
  return summary.nickname || summary.username || `用户 #${userId}`
}

const ensureUserSummaries = async (userIds = []) => {
  const idsToFetch = [...new Set(userIds)]
    .filter((id) => id !== null && id !== undefined && !userSummaries.value[id])

  if (!idsToFetch.length) return

  try {
    const response = await getUserSummariesApi(idsToFetch)
    const nextMap = { ...userSummaries.value }
    ;(Array.isArray(response.data) ? response.data : []).forEach((item) => {
      if (item?.userId !== null && item?.userId !== undefined) {
        nextMap[item.userId] = item
      }
    })
    userSummaries.value = nextMap
  } catch (error) {
    console.error('Failed to fetch user summaries for feedback admin page:', error)
  }
}

const clearDetail = () => {
  selectedFeedbackId.value = null
  selectedFeedbackDetail.value = null
  selectedUserPreference.value = null
}

const selectFeedback = async (feedbackId) => {
  if (!feedbackId) {
    clearDetail()
    return
  }

  const feedbackSummary = feedbackList.value.find((item) => item.id === feedbackId) || null
  selectedFeedbackId.value = feedbackId
  isDetailLoading.value = true

  try {
    const detailResponse = await getAdminFeedbackByIdApi(feedbackId)
    const detail = detailResponse.data || null
    selectedFeedbackDetail.value = detail
    await ensureUserSummaries([feedbackSummary?.userId, detail?.userId].filter(Boolean))

    if (detail?.userId !== null && detail?.userId !== undefined) {
      const preferenceResponse = await getAdminUserFeedbackPreferencesApi(detail.userId)
      selectedUserPreference.value = preferenceResponse.data || null
    } else {
      selectedUserPreference.value = null
    }
  } catch (error) {
    console.error('Failed to fetch feedback detail:', error)
    ElMessage.error(error?.response?.data || '获取反馈详情失败')
  } finally {
    isDetailLoading.value = false
  }
}

const fetchFeedbackList = async () => {
  let params = {}
  try {
    params = buildQueryParams()
  } catch (error) {
    ElMessage.warning(error.message || '筛选条件格式不正确')
    return
  }

  isListLoading.value = true
  try {
    const response = await getAdminFeedbackApi(params)
    const items = normalizeFeedbackList(response.data)
    feedbackList.value = items
    await ensureUserSummaries(items.map((item) => item.userId))

    if (!items.length) {
      clearDetail()
      return
    }

    const nextFeedbackId = items.some((item) => item.id === selectedFeedbackId.value)
      ? selectedFeedbackId.value
      : items[0].id
    await selectFeedback(nextFeedbackId)
  } catch (error) {
    console.error('Failed to fetch feedback list:', error)
    ElMessage.error(error?.response?.data || '获取反馈列表失败')
  } finally {
    isListLoading.value = false
  }
}

const resetFilters = async () => {
  filters.service = ''
  filters.userId = ''
  filters.minRating = ''
  filters.maxRating = ''
  filters.startDate = ''
  filters.endDate = ''
  await fetchFeedbackList()
}

const applyUserFilter = async (userId) => {
  if (userId === null || userId === undefined) return
  filters.userId = String(userId)
  await fetchFeedbackList()
}

onMounted(async () => {
  await fetchFeedbackList()
})
</script>

<style scoped>
.admin-grid {
  display: grid;
  grid-template-columns: minmax(340px, 460px) minmax(0, 1fr);
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
  min-width: 0;
}

.panel-head,
.detail-section-head,
.feedback-row-top,
.preference-top,
.filter-actions {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
}

.panel-head h2,
.detail-section-head h3 {
  margin: 0;
}

.panel-head p,
.detail-section-head p,
.detail-label,
.detail-value,
.feedback-meta,
.feedback-row p,
.stat-card span {
  color: #6a7c76;
}

.stat-grid,
.filter-grid,
.detail-summary-grid,
.preference-grid {
  display: grid;
  gap: 14px;
}

.stat-grid,
.filter-grid,
.detail-summary-grid,
.feedback-list,
.detail-content-box {
  margin-top: 18px;
}

.stat-grid {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.stat-card,
.detail-card,
.preference-item,
.feedback-row,
.detail-content-box {
  border: 1px solid rgba(125, 159, 143, 0.18);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.74);
}

.stat-card,
.detail-card,
.preference-item,
.detail-content-box {
  padding: 16px;
}

.stat-card strong,
.detail-card strong {
  font-size: 1.2rem;
  color: #2c5544;
}

.stat-card {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.filter-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.filter-grid label {
  display: flex;
  flex-direction: column;
  gap: 8px;
  color: #405752;
}

.filter-grid input,
.filter-grid select {
  width: 100%;
  border: 1px solid rgba(125, 159, 143, 0.24);
  border-radius: 14px;
  padding: 12px 14px;
  background: rgba(255, 255, 255, 0.88);
  font: inherit;
}

.feedback-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  max-height: 920px;
  overflow-y: auto;
  padding-right: 4px;
  min-width: 0;
}

.feedback-row {
  width: 100%;
  padding: 16px;
  text-align: left;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.feedback-row.active {
  border-color: rgba(75, 126, 102, 0.42);
  box-shadow: 0 10px 24px rgba(94, 136, 116, 0.14);
}

.feedback-row p {
  margin: 0;
  line-height: 1.6;
}

.pill {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: fit-content;
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(54, 95, 77, 0.08);
  color: #365f4d;
}

.pill.muted {
  background: rgba(120, 126, 155, 0.12);
  color: #56607f;
}

.detail-panel {
  min-height: 860px;
  min-width: 0;
}

.detail-summary-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.detail-card {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.detail-label {
  font-size: 0.85rem;
}

.detail-content {
  margin-top: 14px;
  padding: 16px;
  border-radius: 16px;
  background: rgba(244, 248, 246, 0.9);
  min-height: 140px;
  white-space: pre-wrap;
  line-height: 1.75;
  color: #314640;
}

.preference-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
  margin-top: 14px;
}

.preference-item {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.progress-track {
  position: relative;
  height: 10px;
  border-radius: 999px;
  background: rgba(54, 95, 77, 0.12);
  overflow: hidden;
}

.progress-bar {
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #7ebf9f 0%, #4c8a6b 100%);
}

.primary-btn,
.ghost-btn {
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

.compact-btn {
  padding: 10px 14px;
}

.empty-state {
  padding: 24px;
  border-radius: 18px;
  background: rgba(244, 248, 246, 0.92);
  color: #6b7d77;
  text-align: center;
}

@media (max-width: 1120px) {
  .admin-grid {
    grid-template-columns: 1fr;
  }

  .detail-panel {
    min-height: unset;
  }

  .stat-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 960px) {
  .detail-summary-grid,
  .preference-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 760px) {
  .panel-head,
  .detail-section-head,
  .filter-actions,
  .feedback-row-top,
  .preference-top {
    flex-direction: column;
    align-items: stretch;
  }

  .stat-grid,
  .filter-grid,
  .detail-summary-grid,
  .preference-grid {
    grid-template-columns: 1fr;
  }
}
</style>
