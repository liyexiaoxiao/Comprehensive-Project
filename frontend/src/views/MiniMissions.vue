<template>
  <div class="mini-missions-page">
    <div class="blur-orb orb-coral"></div>
    <div class="blur-orb orb-leaf"></div>
    <div class="blur-orb orb-sky"></div>

    <div class="page-shell">
      <div class="top-bar">
        <button class="back-btn" @click="goBack">
          <font-awesome-icon icon="angle-left" />
          <span>返回</span>
        </button>
        <h1 class="page-title">行为激活与微任务</h1>
        <div style="width: 80px"></div>
      </div>

      <div class="content-grid">
        <section class="glass-panel mission-panel">
          <div class="panel-header">
            <div>
              <h2>今天先做一件很小的事</h2>
              <p>适合低落：超小行为目标；适合焦虑：暴露阶梯/延期担忧。</p>
            </div>
            <div class="panel-actions">
              <button class="secondary-btn" type="button" :disabled="loadingLogs" @click="refreshAll">
                {{ loadingLogs ? '刷新中...' : '刷新' }}
              </button>
            </div>
          </div>

          <div class="segment">
            <button
              v-for="segment in segments"
              :key="segment.id"
              type="button"
              :class="['segment-btn', { active: activeSegment === segment.id }]"
              @click="activeSegment = segment.id"
            >
              <span class="segment-emoji">{{ segment.emoji }}</span>
              <span>{{ segment.name }}</span>
            </button>
          </div>

          <div class="mission-list">
            <div v-for="mission in visibleMissions" :key="mission.key" class="mission-card">
              <div class="mission-head">
                <div class="mission-title">
                  <strong>{{ mission.title }}</strong>
                  <span class="mission-pill">{{ mission.pill }}</span>
                </div>
                <div class="mission-meta">
                  <button
                    class="primary-btn"
                    type="button"
                    :disabled="!!activeMission || startingKey === mission.key || !resolveMissionId(mission.key)"
                    @click="startMission(mission.key)"
                  >
                    {{ startingKey === mission.key ? '启动中...' : activeMission ? '已有进行中' : '开始' }}
                  </button>
                </div>
              </div>

              <p class="mission-desc">{{ mission.description }}</p>

              <div v-if="mission.steps?.length" class="mission-steps">
                <div v-for="(step, idx) in mission.steps" :key="`${mission.key}-${idx}`" class="mission-step">
                  <span class="step-index">{{ idx + 1 }}</span>
                  <span class="step-text">{{ step }}</span>
                </div>
              </div>

              <p v-if="mission.hint" class="mission-hint">{{ mission.hint }}</p>
              <p v-if="!resolveMissionId(mission.key)" class="mission-hint warning">
                当前后端目录里还没有这条任务，请重启 `meditation-service` 让默认微任务写入数据库。
              </p>
            </div>
          </div>
        </section>

        <aside class="glass-panel side-panel">
          <div class="side-block">
            <h3>当前进行中</h3>
            <div v-if="activeMission" class="active-card">
              <div class="active-title">
                <strong>{{ activeMission.title }}</strong>
                <span class="active-id">#{{ activeMission.missionId }}</span>
              </div>
              <p class="active-desc">{{ activeMission.description }}</p>
              <div class="active-actions">
                <button class="primary-btn" type="button" :disabled="completing" @click="completeActiveMission">
                  {{ completing ? '结算中...' : '完成' }}
                </button>
                <button class="danger-btn" type="button" :disabled="aborting" @click="abortActiveMission">
                  {{ aborting ? '处理中...' : '放弃' }}
                </button>
              </div>
              <p class="active-note">
                完成后会发放花园奖励，并把成长计入冥想森林（额外成长分钟数）。
              </p>
            </div>
            <div v-else class="empty-state">
              <p>暂无进行中的微任务。</p>
              <p class="muted">从左侧选择一个任务开始。</p>
            </div>
          </div>

          <div class="side-block">
            <h3>成长进度</h3>
            <div class="growth-card">
              <div class="growth-row">
                <span class="growth-label">森林额外成长</span>
                <strong class="growth-value">{{ forestBonusMinutes }} 分钟</strong>
              </div>
              <p class="muted">
                微任务完成后会累加到森林成长，用于“非冥想时长”的升级路径。
              </p>
            </div>
          </div>

          <div class="side-block">
            <h3>历史记录</h3>
            <div v-if="loadingLogs" class="empty-state">
              <p>正在读取记录...</p>
            </div>
            <div v-else-if="!logs.length" class="empty-state">
              <p>还没有记录。</p>
              <p class="muted">完成一次微任务后，这里会出现日志。</p>
            </div>
            <div v-else class="log-list">
              <div v-for="log in logs" :key="log.id" class="log-row">
                <div class="log-main">
                  <span class="log-title">{{ getLogMissionTitle(log) }}</span>
                  <span :class="['log-status', statusClass(log.status)]">{{ statusLabel(log.status) }}</span>
                </div>
                <div class="log-time">
                  <span>{{ formatApiDateTime(log.updatedAt || log.createdAt) }}</span>
                </div>
              </div>
            </div>
          </div>
        </aside>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  abortMiniMissionApi,
  completeMiniMissionApi,
  getMiniMissionCatalogApi,
  getMyMiniMissionLogsApi,
  rewardGardenItemApi,
  startMiniMissionApi,
} from '@/api/meditation'
import { formatApiDateTime } from '@/utils/dateTime'

const router = useRouter()

const goBack = () => {
  router.push('/service')
}

const FOREST_BONUS_MINUTES_STORAGE_KEY = 'forest_bonus_minutes'

const normalizeInteger = (value) => {
  const normalized = Number.parseInt(String(value ?? '').trim(), 10)
  return Number.isFinite(normalized) ? normalized : null
}

const clamp = (value, min, max) => Math.min(max, Math.max(min, value))

const readForestBonusMinutes = () => {
  const value = normalizeInteger(window.localStorage.getItem(FOREST_BONUS_MINUTES_STORAGE_KEY))
  return value && value > 0 ? value : 0
}

const writeForestBonusMinutes = (value) => {
  const safeValue = Math.max(0, Math.floor(Number(value) || 0))
  window.localStorage.setItem(FOREST_BONUS_MINUTES_STORAGE_KEY, String(safeValue))
  forestBonusMinutes.value = safeValue
}

const addForestBonusMinutes = (delta) => {
  const addValue = Math.floor(Number(delta) || 0)
  if (!Number.isFinite(addValue) || addValue <= 0) return
  writeForestBonusMinutes(readForestBonusMinutes() + addValue)
}

const forestBonusMinutes = ref(readForestBonusMinutes())
const missionCatalog = ref([])
const isCatalogLoading = ref(false)

const segments = [
  { id: 'low', name: '低落（超小目标）', emoji: '☀️' },
  { id: 'anxiety', name: '焦虑（暴露/延期担忧）', emoji: '🧩' },
]
const activeSegment = ref('low')

const defaultMissions = [
  {
    key: 'sunlight_5',
    segment: 'low',
    title: '晒太阳 5 分钟',
    pill: '身体激活',
    description: '到窗边/阳台/户外，感受光线与温度，什么都不需要想。',
    steps: ['站到有光的地方', '计时 5 分钟', '结束后喝一口水'],
    hint: '如果今天完全不想动：把目标改成“打开窗帘 10 秒”。',
  },
  {
    key: 'drink_water',
    segment: 'low',
    title: '喝一杯水',
    pill: '超小启动',
    description: '立刻让身体“完成一次小任务”，给大脑一个可完成的证据。',
    steps: ['拿起杯子', '喝到 3 口即可', '放回原处'],
    hint: '不用追求完美，完成比质量重要。',
  },
  {
    key: 'tidy_desk_2',
    segment: 'low',
    title: '整理桌面 2 分钟',
    pill: '环境整理',
    description: '只收拾“一个角落”，不要试图整理整个房间。',
    steps: ['挑 3 个物品归位', '扔掉 1 个垃圾', '停在 2 分钟就结束'],
    hint: '如果被“必须全部整理完”卡住：只做“清出鼠标垫区域”。',
  },
  {
    key: 'message_friend',
    segment: 'low',
    title: '给朋友发一句消息',
    pill: '连接感',
    description: '一句就好，不用展开聊。例：我今天有点累，想跟你说声嗨。',
    steps: ['选一个最安全的人', '复制一句模板发出去', '发完就关掉聊天框'],
    hint: '如果社交恐惧：发给自己/备忘录也算完成。',
  },
  {
    key: 'exposure_step',
    segment: 'anxiety',
    title: '暴露阶梯：做“第一小步”',
    pill: '暴露练习',
    description: '把焦虑情境拆成阶梯，今天只做最小的一步（2-5 分钟）。',
    steps: ['写下一个焦虑情境（越具体越好）', '把它拆成 5 个难度从 1 到 5 的小步', '选择 1 级小步，做 2-5 分钟'],
    hint: '焦虑升高不是失败，目标是“允许它在场”。',
  },
  {
    key: 'worry_postpone',
    segment: 'anxiety',
    title: '延期担忧：把担心推迟到固定时间',
    pill: '认知技巧',
    description: '把“现在要解决”改成“到担忧时间再处理”，为当下腾出空间。',
    steps: ['设定今天的担忧时间（例如 19:30-19:40）', '把担心写成一句话', '对自己说：我会在担忧时间再处理'],
    hint: '担忧时间到了再决定：要不要继续担忧。',
  },
]

const resolveCatalogMission = (missionKey) => {
  const localMission = defaultMissions.find((mission) => mission.key === missionKey)
  if (!localMission) return null

  return missionCatalog.value.find((mission) => mission.title === localMission.title) || null
}

const resolveMissionId = (missionKey) => {
  const mission = resolveCatalogMission(missionKey)
  const missionId = normalizeInteger(mission?.id)
  return missionId && missionId > 0 ? missionId : null
}

const visibleMissions = computed(() =>
  defaultMissions.filter((mission) => mission.segment === activeSegment.value))

const logs = ref([])
const loadingLogs = ref(false)

const activeMission = ref(null)
const startingKey = ref('')
const completing = ref(false)
const aborting = ref(false)

const statusLabel = (status) => {
  if (status === 'IN_PROGRESS') return '进行中'
  if (status === 'COMPLETED') return '已完成'
  if (status === 'FAILED') return '已放弃'
  return status || '未知'
}

const statusClass = (status) => {
  if (status === 'IN_PROGRESS') return 'progress'
  if (status === 'COMPLETED') return 'completed'
  if (status === 'FAILED') return 'failed'
  return 'unknown'
}

const resolveLocalMissionMeta = (missionId) => {
  const targetId = normalizeInteger(missionId)
  if (!targetId) return null

  const remoteMission = missionCatalog.value.find((mission) => normalizeInteger(mission.id) === targetId)
  if (remoteMission) {
    return defaultMissions.find((mission) => mission.title === remoteMission.title) || null
  }

  return null
}

const getLogMissionTitle = (log) => {
  const localMission = resolveLocalMissionMeta(log?.miniMissionId)
  if (localMission?.title) {
    return localMission.title
  }
  return `任务 #${log?.miniMissionId ?? '-'}`
}

const fetchMissionCatalog = async () => {
  isCatalogLoading.value = true
  try {
    const res = await getMiniMissionCatalogApi()
    missionCatalog.value = Array.isArray(res.data) ? res.data : []
  } catch (error) {
    console.error('Failed to fetch mini mission catalog', error)
    missionCatalog.value = []
  } finally {
    isCatalogLoading.value = false
  }
}

const loadActiveMissionFromLogs = async (nextLogs) => {
  const activeLog = nextLogs.find((log) => log?.status === 'IN_PROGRESS')
  if (!activeLog?.miniMissionId) {
    activeMission.value = null
    return
  }
  const missionId = Number(activeLog.miniMissionId)
  if (!Number.isFinite(missionId) || missionId <= 0) {
    activeMission.value = null
    return
  }
  const localMission = resolveLocalMissionMeta(missionId)
  activeMission.value = {
    missionId,
    title: localMission?.title || `任务 #${missionId}`,
    description: localMission?.description || '该进行中任务来自历史记录，原任务详情暂不可用。',
    rewardValue:  Number(activeLog?.earnedValue) > 0 ? Number(activeLog.earnedValue) : 1,
  }
}

const fetchLogs = async () => {
  loadingLogs.value = true
  try {
    const res = await getMyMiniMissionLogsApi()
    const list = Array.isArray(res.data) ? res.data : []
    logs.value = list.slice().sort((a, b) => {
      const at = new Date(a?.updatedAt || a?.createdAt || 0).getTime()
      const bt = new Date(b?.updatedAt || b?.createdAt || 0).getTime()
      return bt - at
    })
    await loadActiveMissionFromLogs(logs.value)
  } catch (error) {
    console.error('Failed to fetch mini mission logs', error)
    logs.value = []
    activeMission.value = null
  } finally {
    loadingLogs.value = false
  }
}

const refreshAll = async () => {
  await Promise.all([fetchMissionCatalog(), fetchLogs()])
  forestBonusMinutes.value = readForestBonusMinutes()
}

const startMission = async (missionKey) => {
  if (activeMission.value) return
  startingKey.value = missionKey
  try {
    const missionId = resolveMissionId(missionKey)
    if (!missionId) {
      ElMessage.warning('后端还没有这条微任务，请先重启 meditation-service 初始化默认任务。')
      return
    }
    const res = await startMiniMissionApi(missionId)
    activeMission.value = {
      missionId,
      title: res.data?.title || `任务 #${missionId}`,
      description: res.data?.description || '',
      rewardValue: Number(res.data?.rewardValue) || 1,
    }
    ElMessage.success('微任务已开始')
    await fetchLogs()
  } catch (error) {
    const message = typeof error?.response?.data === 'string'
      ? error.response.data
      : error?.response?.data?.message || '启动失败'
    if (message.includes('User already has an active mini mission')) {
      await fetchLogs()
      ElMessage.warning('你当前已有一个进行中的微任务，已为你恢复到右侧面板。')
    } else if (message.includes('Mini mission not found')) {
      await fetchMissionCatalog()
      ElMessage.error('后端任务目录尚未初始化完成，请重启 meditation-service 后再试。')
    } else {
      ElMessage.error(message)
    }
  } finally {
    startingKey.value = ''
  }
}

const completeActiveMission = async () => {
  if (!activeMission.value?.missionId) return
  completing.value = true
  const missionSnapshot = { ...activeMission.value }
  try {
    const missionId = activeMission.value.missionId
    const res = await completeMiniMissionApi(missionId)
    const rewardValue = clamp(Number(res.data?.rewardValue) || 1, 1, 10)

    let rewarded = 0
    for (let i = 0; i < rewardValue; i += 1) {
      try {
        await rewardGardenItemApi()
        rewarded += 1
      } catch {
        break
      }
    }

    addForestBonusMinutes(rewarded || rewardValue)
    ElMessage.success(`已完成：花园奖励 ${rewarded || 0} 次，森林成长 +${rewarded || rewardValue} 分钟`)
    activeMission.value = null
    await refreshAll()
  } catch (error) {
    const message = typeof error?.response?.data === 'string'
      ? error.response.data
      : error?.response?.data?.message || '完成失败'
    if (message.includes('Mini mission not found')) {
      await fetchLogs()
      if (!activeMission.value) {
        const rewardValue = clamp(Number(missionSnapshot.rewardValue) || 1, 1, 10)
        let rewarded = 0
        for (let i = 0; i < rewardValue; i += 1) {
          try {
            await rewardGardenItemApi()
            rewarded += 1
          } catch {
            break
          }
        }
        addForestBonusMinutes(rewarded || rewardValue)
        ElMessage.success(`任务已完成，已补发花园奖励并增加森林成长 +${rewarded || rewardValue} 分钟`)
        await refreshAll()
        return
      }
    }
    ElMessage.error(message)
  } finally {
    completing.value = false
  }
}

const abortActiveMission = async () => {
  if (!activeMission.value?.missionId) return
  aborting.value = true
  const missionId = activeMission.value.missionId
  try {
    await abortMiniMissionApi(missionId)
    ElMessage.info('已放弃当前微任务')
    activeMission.value = null
    await fetchLogs()
  } catch (error) {
    const message = typeof error?.response?.data === 'string'
      ? error.response.data
      : error?.response?.data?.message || '放弃失败'
    if (message.includes('Mini mission not found')) {
      await fetchLogs()
      if (!activeMission.value) {
        ElMessage.info('已放弃当前微任务')
        return
      }
    }
    ElMessage.error(message)
  } finally {
    aborting.value = false
  }
}

onMounted(async () => {
  await refreshAll()
})
</script>

<style scoped>
.mini-missions-page {
  height: 100vh;
  background: linear-gradient(180deg, #dff7ef 0%, #eaf8ff 52%, #f2fff7 100%);
  color: #f2f6f4;
  position: relative;
  overflow: hidden;
}

.blur-orb {
  position: absolute;
  width: 380px;
  height: 380px;
  filter: blur(70px);
  opacity: 0.45;
  pointer-events: none;
}

.orb-coral {
  background: rgba(160, 230, 209, 0.9);
  top: -120px;
  left: -80px;
}

.orb-leaf {
  background: rgba(157, 221, 181, 0.9);
  top: 80px;
  right: -120px;
}

.orb-sky {
  background: rgba(163, 213, 244, 0.95);
  bottom: -150px;
  left: 18%;
}

.page-shell {
  width: 100%;
  max-width: 1320px;
  margin: 0 auto;
  height: 100vh;
  box-sizing: border-box;
  padding: 24px 36px;
  position: relative;
  z-index: 2;
  display: flex;
  flex-direction: column;
}

.top-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 18px;
}

.back-btn {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  border-radius: 999px;
  border: 1px solid rgba(255, 255, 255, 0.14);
  background: rgba(20, 28, 30, 0.6);
  color: #f2f6f4;
  cursor: pointer;
}

.page-title {
  font-size: 22px;
  font-weight: 700;
  letter-spacing: 0.5px;
}

.content-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.65fr) minmax(320px, 0.95fr);
  gap: 22px;
  flex: 1;
  min-height: 0;
  width: 100%;
}

.glass-panel {
  background: rgba(25, 58, 63, 0.66);
  border: 1px solid rgba(255, 255, 255, 0.18);
  border-radius: 18px;
  backdrop-filter: blur(14px);
  box-shadow: 0 18px 44px rgba(62, 115, 120, 0.18);
}

.mission-panel {
  padding: 18px;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.panel-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.panel-header h2 {
  margin: 0 0 6px;
  font-size: 18px;
  color: #ffffff;
}

.panel-header p {
  margin: 0;
  color: rgba(242, 246, 244, 0.74);
  font-size: 13px;
  line-height: 1.5;
}

.panel-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.segment {
  margin-top: 16px;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.segment-btn {
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 14px;
  background: rgba(12, 18, 20, 0.45);
  color: rgba(242, 246, 244, 0.9);
  padding: 12px 14px;
  display: inline-flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  transition: transform 0.15s ease, border-color 0.15s ease, background 0.15s ease;
}

.segment-btn:hover {
  transform: translateY(-1px);
}

.segment-btn.active {
  background: rgba(217, 122, 108, 0.12);
  border-color: rgba(217, 122, 108, 0.35);
}

.segment-emoji {
  font-size: 16px;
}

.mission-list {
  margin-top: 14px;
  display: grid;
  gap: 12px;
  min-height: 0;
  overflow-y: auto;
  padding-right: 4px;
}

.mission-card {
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.10);
  background: rgba(10, 14, 16, 0.55);
  padding: 14px;
}

.mission-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
}

.mission-title {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.mission-pill {
  display: inline-flex;
  align-items: center;
  padding: 4px 10px;
  border-radius: 999px;
  background: rgba(140, 165, 149, 0.18);
  border: 1px solid rgba(140, 165, 149, 0.3);
  color: rgba(242, 246, 244, 0.9);
  font-size: 12px;
}

.mission-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.mission-desc {
  margin: 10px 0 0;
  color: rgba(242, 246, 244, 0.78);
  font-size: 13px;
  line-height: 1.6;
}

.mission-steps {
  margin-top: 10px;
  display: grid;
  gap: 8px;
}

.mission-step {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  color: rgba(242, 246, 244, 0.85);
  font-size: 13px;
}

.step-index {
  width: 22px;
  height: 22px;
  border-radius: 8px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: rgba(108, 170, 217, 0.14);
  border: 1px solid rgba(108, 170, 217, 0.3);
  flex: 0 0 auto;
  font-size: 12px;
}

.mission-hint {
  margin: 10px 0 0;
  font-size: 12px;
  color: rgba(242, 246, 244, 0.7);
}

.mission-hint.warning {
  color: #ffb7ac;
}

.side-panel {
  padding: 18px;
  display: grid;
  gap: 16px;
  align-content: start;
  min-height: 0;
  overflow: hidden;
}

.side-block h3 {
  margin: 0 0 10px;
  font-size: 15px;
  color: #ffffff;
}

.active-card {
  border-radius: 16px;
  padding: 14px;
  background: rgba(10, 14, 16, 0.55);
  border: 1px solid rgba(255, 255, 255, 0.10);
}

.active-title {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 10px;
}

.active-id {
  color: rgba(242, 246, 244, 0.68);
  font-size: 12px;
}

.active-desc {
  margin: 8px 0 0;
  color: rgba(242, 246, 244, 0.78);
  font-size: 13px;
  line-height: 1.6;
}

.active-actions {
  margin-top: 12px;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.active-note {
  margin: 10px 0 0;
  color: rgba(242, 246, 244, 0.66);
  font-size: 12px;
  line-height: 1.5;
}

.growth-card {
  border-radius: 16px;
  padding: 14px;
  background: rgba(10, 14, 16, 0.55);
  border: 1px solid rgba(255, 255, 255, 0.10);
}

.growth-row {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
}

.growth-label {
  color: rgba(242, 246, 244, 0.75);
  font-size: 13px;
}

.growth-value {
  font-size: 18px;
}

.muted {
  color: rgba(242, 246, 244, 0.65);
  font-size: 12px;
  line-height: 1.5;
  margin: 8px 0 0;
}

.empty-state {
  border-radius: 16px;
  padding: 14px;
  background: rgba(10, 14, 16, 0.45);
  border: 1px dashed rgba(255, 255, 255, 0.16);
}

.log-list {
  display: grid;
  gap: 8px;
  max-height: 320px;
  overflow-y: auto;
  padding-right: 4px;
}

.log-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 14px;
  background: rgba(10, 14, 16, 0.55);
  border: 1px solid rgba(255, 255, 255, 0.10);
}

.log-main {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.log-title {
  font-weight: 700;
  font-size: 13px;
  color: #ffffff;
}

.log-status {
  font-size: 12px;
  padding: 3px 8px;
  border-radius: 999px;
  border: 1px solid rgba(255, 255, 255, 0.14);
  background: rgba(255, 255, 255, 0.06);
  color: rgba(242, 246, 244, 0.85);
}

.log-status.completed {
  border-color: rgba(140, 165, 149, 0.35);
  background: rgba(140, 165, 149, 0.14);
}

.log-status.progress {
  border-color: rgba(108, 170, 217, 0.35);
  background: rgba(108, 170, 217, 0.14);
}

.log-status.failed {
  border-color: rgba(217, 122, 108, 0.35);
  background: rgba(217, 122, 108, 0.14);
}

.log-time {
  color: rgba(242, 246, 244, 0.68);
  font-size: 12px;
}

.primary-btn,
.secondary-btn,
.danger-btn {
  border: 1px solid rgba(255, 255, 255, 0.14);
  border-radius: 12px;
  padding: 10px 12px;
  cursor: pointer;
  color: rgba(242, 246, 244, 0.92);
  background: rgba(255, 255, 255, 0.08);
  transition: transform 0.15s ease, background 0.15s ease;
}

.primary-btn {
  background: rgba(140, 165, 149, 0.18);
  border-color: rgba(140, 165, 149, 0.35);
}

.danger-btn {
  background: rgba(217, 122, 108, 0.14);
  border-color: rgba(217, 122, 108, 0.35);
}

.primary-btn:hover,
.secondary-btn:hover,
.danger-btn:hover {
  transform: translateY(-1px);
}

.primary-btn:disabled,
.secondary-btn:disabled,
.danger-btn:disabled {
  cursor: not-allowed;
  opacity: 0.6;
  transform: none;
}

@media (max-width: 980px) {
  .mini-missions-page {
    height: auto;
    min-height: 100vh;
    overflow: auto;
  }

  .page-shell {
    height: auto;
    min-height: 100vh;
    padding: 20px 14px;
  }

  .content-grid {
    grid-template-columns: 1fr;
  }

  .mission-list,
  .log-list {
    max-height: none;
  }
}
</style>
