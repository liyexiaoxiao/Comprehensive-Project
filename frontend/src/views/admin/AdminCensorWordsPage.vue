<template>
  <section class="admin-grid">
    <article class="glass-card panel">
      <div class="panel-head">
        <div>
          <h2>敏感词审查</h2>
          <p>配置社交服务中的敏感词词库，支持批量新增、批量启停、导入导出和运营侧操作追踪。</p>
        </div>
        <div class="panel-head-actions">
          <button class="ghost-btn" type="button" :disabled="isLoading" @click="refreshWords">
            {{ isLoading ? '加载中...' : '刷新列表' }}
          </button>
          <button class="ghost-btn" type="button" :disabled="!words.length" @click="exportWordLibrary('json')">
            导出 JSON
          </button>
          <button class="ghost-btn" type="button" :disabled="!words.length" @click="exportWordLibrary('txt')">
            导出 TXT
          </button>
          <button class="ghost-btn" type="button" :disabled="isImporting" @click="triggerImport">
            {{ isImporting ? '导入中...' : '导入词库' }}
          </button>
          <input
            ref="importInputRef"
            type="file"
            class="hidden-file-input"
            accept=".txt,.json"
            @change="handleImportFile"
          />
        </div>
      </div>

      <div class="stats-grid">
        <div class="stat-card">
          <span>总词数</span>
          <strong>{{ words.length }}</strong>
        </div>
        <div class="stat-card">
          <span>启用中</span>
          <strong>{{ activeCount }}</strong>
        </div>
        <div class="stat-card">
          <span>已停用</span>
          <strong>{{ inactiveCount }}</strong>
        </div>
      </div>

      <div class="toolbar">
        <input
          v-model.trim="keyword"
          type="text"
          class="toolbar-input"
          placeholder="搜索敏感词"
        />
        <div class="filter-group">
          <button
            v-for="item in filterOptions"
            :key="item.value"
            type="button"
            :class="['filter-chip', { active: activeFilter === item.value }]"
            @click="activeFilter = item.value"
          >
            {{ item.label }}
          </button>
        </div>
      </div>

      <div class="bulk-toolbar">
        <label class="select-all-toggle">
          <input
            type="checkbox"
            :checked="allFilteredSelected"
            :indeterminate.prop="someFilteredSelected && !allFilteredSelected"
            @change="toggleSelectAllFiltered"
          />
          <span>本页筛选结果全选</span>
        </label>
        <span class="selection-summary">已选 {{ selectedCount }} 项</span>
        <div class="bulk-actions">
          <button
            class="ghost-btn compact-btn"
            type="button"
            :disabled="!selectedCount || isBatchOperating"
            @click="batchSetWordActivity(true)"
          >
            {{ isBatchOperating ? '处理中...' : '批量启用' }}
          </button>
          <button
            class="ghost-btn compact-btn"
            type="button"
            :disabled="!selectedCount || isBatchOperating"
            @click="batchSetWordActivity(false)"
          >
            {{ isBatchOperating ? '处理中...' : '批量停用' }}
          </button>
          <button
            class="ghost-btn compact-btn"
            type="button"
            :disabled="!selectedCount"
            @click="clearSelection"
          >
            清空选择
          </button>
        </div>
      </div>

      <div class="word-list">
        <div v-for="word in filteredWords" :key="word.id || word.word" class="word-row">
          <label class="word-check">
            <input
              type="checkbox"
              :checked="isWordSelected(word.word)"
              @change="toggleWordSelection(word.word)"
            />
          </label>
          <div class="word-main">
            <strong>{{ word.word }}</strong>
            <span class="word-meta">创建于：{{ formatDateTime(word.createdAt) }}</span>
          </div>
          <div class="word-actions">
            <span :class="['status-badge', word.isActive ? 'active' : 'inactive']">
              {{ word.isActive ? '启用中' : '已停用' }}
            </span>
            <button
              class="ghost-btn compact-btn"
              type="button"
              :disabled="togglingWord === word.word"
              @click="toggleWordActivity(word)"
            >
              {{ togglingWord === word.word ? '处理中...' : word.isActive ? '停用' : '启用' }}
            </button>
            <button
              class="danger-btn compact-btn"
              type="button"
              :disabled="deletingWord === word.word"
              @click="deleteWord(word)"
            >
              {{ deletingWord === word.word ? '删除中...' : '删除' }}
            </button>
          </div>
        </div>
        <div v-if="!filteredWords.length" class="empty-state">
          当前筛选条件下没有敏感词。
        </div>
      </div>
    </article>

    <article class="glass-card panel side-panel">
      <div class="panel-head">
        <div>
          <h2>新增敏感词</h2>
          <p>支持单条新增与批量投放，适合运营同学快速维护词库。</p>
        </div>
      </div>

      <div class="form-card">
        <label>
          <span>敏感词内容</span>
          <input
            v-model.trim="newWord"
            type="text"
            maxlength="100"
            placeholder="例如：辱骂词、攻击性用语"
            @keyup.enter="createWord"
          />
        </label>
        <button class="primary-btn" type="button" :disabled="isAddingWord" @click="createWord">
          {{ isAddingWord ? '提交中...' : '新增敏感词' }}
        </button>
      </div>

      <div class="form-card">
        <label>
          <span>批量新增</span>
          <textarea
            v-model="batchWordText"
            rows="7"
            placeholder="每行一个敏感词，也支持用逗号、分号分隔"
          ></textarea>
        </label>
        <div class="helper-row">
          <span>预计导入 {{ parsedBatchWordCount }} 个唯一词条</span>
          <button class="primary-btn" type="button" :disabled="isBatchAdding" @click="createWordsBatch">
            {{ isBatchAdding ? '批量提交中...' : '批量新增' }}
          </button>
        </div>
      </div>

      <div class="tips-card operation-card">
        <div class="operation-head">
          <h3>最近操作记录</h3>
          <button class="ghost-btn compact-btn" type="button" :disabled="!operationLogs.length" @click="clearOperationLogs">
            清空记录
          </button>
        </div>
        <div v-if="operationLogs.length" class="operation-list">
          <div v-for="log in operationLogs" :key="log.id" class="operation-row">
            <div class="operation-main">
              <strong>{{ log.action }}</strong>
              <span>{{ log.detail }}</span>
            </div>
            <div class="operation-meta">
              <span :class="['operation-tag', log.result]">{{ resultLabel(log.result) }}</span>
              <span>{{ formatDateTime(log.createdAt) }}</span>
            </div>
          </div>
        </div>
        <div v-else class="empty-state operation-empty">
          还没有操作记录。
        </div>
      </div>

      <div class="tips-card">
        <h3>使用说明</h3>
        <ul>
          <li>“停用”会保留该词，但不会参与内容替换。</li>
          <li>“删除”会直接从词库移除，之后不再生效。</li>
          <li>筛选项支持查看全部、仅启用、仅停用词条。</li>
          <li>导入支持 `.txt` 和 `.json`，导出可用于备份或迁移词库。</li>
        </ul>
      </div>
    </article>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  addAdminCensorWordApi,
  deleteAdminCensorWordApi,
  getAdminCensorWordsApi,
  setAdminCensorWordActivityApi,
} from '@/api/social'
import { formatApiDateTime } from '@/utils/dateTime'

const OPERATION_LOG_STORAGE_KEY = 'admin_censor_operation_logs_v1'
const MAX_OPERATION_LOGS = 20

const words = ref([])
const keyword = ref('')
const activeFilter = ref('all')
const newWord = ref('')
const batchWordText = ref('')
const isLoading = ref(false)
const isAddingWord = ref(false)
const isBatchAdding = ref(false)
const isBatchOperating = ref(false)
const isImporting = ref(false)
const togglingWord = ref('')
const deletingWord = ref('')
const selectedWords = ref([])
const importInputRef = ref(null)
const operationLogs = ref(readOperationLogs())

const filterOptions = [
  { label: '全部', value: 'all' },
  { label: '仅启用', value: 'active' },
  { label: '仅停用', value: 'inactive' },
]

const formatDateTime = (value) => {
  if (!value) return '-'
  return formatApiDateTime(value, {}, String(value))
}

const normalizeCensorWord = (item) => ({
  ...item,
  word: String(item?.word || '').trim(),
  isActive: item?.isActive ?? item?.active ?? false,
})

function readOperationLogs() {
  try {
    const raw = window.localStorage.getItem(OPERATION_LOG_STORAGE_KEY)
    const parsed = raw ? JSON.parse(raw) : []
    return Array.isArray(parsed) ? parsed : []
  } catch {
    return []
  }
}

const persistOperationLogs = () => {
  window.localStorage.setItem(OPERATION_LOG_STORAGE_KEY, JSON.stringify(operationLogs.value))
}

const pushOperationLog = (action, detail, result = 'success') => {
  operationLogs.value = [
    {
      id: `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`,
      action,
      detail,
      result,
      createdAt: new Date().toISOString(),
    },
    ...operationLogs.value,
  ].slice(0, MAX_OPERATION_LOGS)
  persistOperationLogs()
}

const clearOperationLogs = () => {
  operationLogs.value = []
  persistOperationLogs()
}

const normalizeWordList = (rawText) => {
  return Array.from(new Set(
    String(rawText || '')
      .split(/[\n,，;；]+/)
      .map((item) => item.trim())
      .filter(Boolean),
  ))
}

const fetchWords = async () => {
  isLoading.value = true
  try {
    const response = await getAdminCensorWordsApi(0)
    words.value = Array.isArray(response.data) ? response.data.map(normalizeCensorWord) : []
    selectedWords.value = selectedWords.value.filter((word) =>
      words.value.some((item) => item.word === word))
  } catch (error) {
    console.error('Failed to fetch censor words', error)
    ElMessage.error(typeof error?.response?.data === 'string' ? error.response.data : '加载敏感词失败')
  } finally {
    isLoading.value = false
  }
}

const refreshWords = async () => {
  await fetchWords()
}

const activeCount = computed(() => words.value.filter((item) => item.isActive).length)
const inactiveCount = computed(() => words.value.filter((item) => !item.isActive).length)
const parsedBatchWordCount = computed(() => normalizeWordList(batchWordText.value).length)
const selectedCount = computed(() => selectedWords.value.length)

const filteredWords = computed(() => {
  const q = keyword.value.trim().toLowerCase()
  return words.value.filter((item) => {
    const matchStatus =
      activeFilter.value === 'all'
      || (activeFilter.value === 'active' && item.isActive)
      || (activeFilter.value === 'inactive' && !item.isActive)

    const matchKeyword = !q || String(item.word || '').toLowerCase().includes(q)
    return matchStatus && matchKeyword
  })
})

const allFilteredSelected = computed(() =>
  filteredWords.value.length > 0
  && filteredWords.value.every((item) => selectedWords.value.includes(item.word)))

const someFilteredSelected = computed(() =>
  filteredWords.value.some((item) => selectedWords.value.includes(item.word)))

const isWordSelected = (word) => selectedWords.value.includes(word)

const toggleWordSelection = (word) => {
  if (selectedWords.value.includes(word)) {
    selectedWords.value = selectedWords.value.filter((item) => item !== word)
    return
  }
  selectedWords.value = [...selectedWords.value, word]
}

const toggleSelectAllFiltered = () => {
  if (allFilteredSelected.value) {
    const filteredSet = new Set(filteredWords.value.map((item) => item.word))
    selectedWords.value = selectedWords.value.filter((word) => !filteredSet.has(word))
    return
  }
  const merged = new Set(selectedWords.value)
  filteredWords.value.forEach((item) => merged.add(item.word))
  selectedWords.value = Array.from(merged)
}

const clearSelection = () => {
  selectedWords.value = []
}

const resultLabel = (result) => {
  if (result === 'error') return '失败'
  if (result === 'warning') return '提醒'
  return '成功'
}

const batchCreateWords = async (wordList, actionLabel) => {
  const normalizedWords = Array.from(new Set(wordList.map((item) => item.trim()).filter(Boolean)))
  if (!normalizedWords.length) {
    ElMessage.warning('没有可提交的敏感词')
    return { successCount: 0, failedItems: [] }
  }

  const failedItems = []
  let successCount = 0
  for (const word of normalizedWords) {
    try {
      await addAdminCensorWordApi(word)
      successCount += 1
    } catch (error) {
      failedItems.push({
        word,
        message: typeof error?.response?.data === 'string' ? error.response.data : '新增失败',
      })
    }
  }

  const detail = `成功 ${successCount} 个，失败 ${failedItems.length} 个`
  pushOperationLog(actionLabel, detail, failedItems.length ? 'warning' : 'success')
  await fetchWords()
  return { successCount, failedItems }
}

const createWord = async () => {
  const word = newWord.value.trim()
  if (!word) {
    ElMessage.warning('请先输入敏感词')
    return
  }
  isAddingWord.value = true
  try {
    const { successCount, failedItems } = await batchCreateWords([word], '单条新增敏感词')
    if (successCount) {
      ElMessage.success('敏感词已新增')
      newWord.value = ''
    } else {
      ElMessage.error(failedItems[0]?.message || '新增敏感词失败')
    }
  } finally {
    isAddingWord.value = false
  }
}

const createWordsBatch = async () => {
  const wordsToCreate = normalizeWordList(batchWordText.value)
  if (!wordsToCreate.length) {
    ElMessage.warning('请先输入批量敏感词')
    return
  }
  isBatchAdding.value = true
  try {
    const { successCount, failedItems } = await batchCreateWords(wordsToCreate, '批量新增敏感词')
    if (successCount) {
      batchWordText.value = ''
    }
    if (!failedItems.length) {
      ElMessage.success(`批量新增完成，共 ${successCount} 个`)
    } else {
      ElMessage.warning(`批量新增完成：成功 ${successCount} 个，失败 ${failedItems.length} 个`)
    }
  } finally {
    isBatchAdding.value = false
  }
}

const toggleWordActivity = async (word) => {
  togglingWord.value = word.word
  try {
    await setAdminCensorWordActivityApi(word.word, !word.isActive)
    ElMessage.success(word.isActive ? '已停用敏感词' : '已启用敏感词')
    pushOperationLog(word.isActive ? '停用敏感词' : '启用敏感词', word.word, 'success')
    await fetchWords()
  } catch (error) {
    pushOperationLog(word.isActive ? '停用敏感词' : '启用敏感词', `${word.word}：失败`, 'error')
    ElMessage.error(typeof error?.response?.data === 'string' ? error.response.data : '更新状态失败')
  } finally {
    togglingWord.value = ''
  }
}

const deleteWord = async (word) => {
  try {
    await ElMessageBox.confirm(`确认删除敏感词“${word.word}”吗？`, '删除确认', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning',
    })
  } catch {
    return
  }

  deletingWord.value = word.word
  try {
    await deleteAdminCensorWordApi(word.word)
    ElMessage.success('敏感词已删除')
    pushOperationLog('删除敏感词', word.word, 'success')
    await fetchWords()
  } catch (error) {
    pushOperationLog('删除敏感词', `${word.word}：失败`, 'error')
    ElMessage.error(typeof error?.response?.data === 'string' ? error.response.data : '删除敏感词失败')
  } finally {
    deletingWord.value = ''
  }
}

const batchSetWordActivity = async (nextActive) => {
  const targets = selectedWords.value.slice()
  if (!targets.length) {
    ElMessage.warning('请先选择敏感词')
    return
  }

  isBatchOperating.value = true
  let successCount = 0
  const failedItems = []
  try {
    for (const word of targets) {
      try {
        await setAdminCensorWordActivityApi(word, nextActive)
        successCount += 1
      } catch (error) {
        failedItems.push({
          word,
          message: typeof error?.response?.data === 'string' ? error.response.data : '状态更新失败',
        })
      }
    }
    pushOperationLog(
      nextActive ? '批量启用敏感词' : '批量停用敏感词',
      `成功 ${successCount} 个，失败 ${failedItems.length} 个`,
      failedItems.length ? 'warning' : 'success',
    )
    await fetchWords()
    clearSelection()
    if (!failedItems.length) {
      ElMessage.success(`${nextActive ? '批量启用' : '批量停用'}完成，共 ${successCount} 个`)
    } else {
      ElMessage.warning(`${nextActive ? '批量启用' : '批量停用'}完成：成功 ${successCount} 个，失败 ${failedItems.length} 个`)
    }
  } finally {
    isBatchOperating.value = false
  }
}

const triggerImport = () => {
  importInputRef.value?.click()
}

const parseImportContent = (fileName, text) => {
  if (fileName.toLowerCase().endsWith('.json')) {
    const parsed = JSON.parse(text)
    if (Array.isArray(parsed)) {
      return Array.from(new Set(parsed
        .map((item) => (typeof item === 'string' ? item : item?.word))
        .map((item) => String(item || '').trim())
        .filter(Boolean)))
    }
  }
  return normalizeWordList(text)
}

const handleImportFile = async (event) => {
  const file = event.target.files?.[0]
  if (!file) return

  isImporting.value = true
  try {
    const text = await file.text()
    const importedWords = parseImportContent(file.name, text)
    if (!importedWords.length) {
      ElMessage.warning('导入文件里没有可用敏感词')
      return
    }
    const { successCount, failedItems } = await batchCreateWords(importedWords, '导入敏感词词库')
    if (!failedItems.length) {
      ElMessage.success(`导入完成，共新增 ${successCount} 个词条`)
    } else {
      ElMessage.warning(`导入完成：成功 ${successCount} 个，失败 ${failedItems.length} 个`)
    }
  } catch (error) {
    console.error('Import word library failed', error)
    pushOperationLog('导入敏感词词库', `${file.name}：解析失败`, 'error')
    ElMessage.error('导入失败，请检查文件格式（支持 txt / json）')
  } finally {
    isImporting.value = false
    event.target.value = ''
  }
}

const exportWordLibrary = (format) => {
  const payload = format === 'json'
    ? JSON.stringify(words.value, null, 2)
    : words.value.map((item) => item.word).join('\n')
  const mimeType = format === 'json' ? 'application/json;charset=utf-8' : 'text/plain;charset=utf-8'
  const fileName = `censor-words-${new Date().toISOString().slice(0, 10)}.${format}`
  const blob = new Blob([payload], { type: mimeType })
  const url = URL.createObjectURL(blob)
  const anchor = document.createElement('a')
  anchor.href = url
  anchor.download = fileName
  anchor.click()
  URL.revokeObjectURL(url)
  pushOperationLog('导出敏感词词库', fileName, 'success')
}

onMounted(async () => {
  await fetchWords()
})
</script>

<style scoped>
.admin-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.7fr) minmax(320px, 0.9fr);
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
  padding: 24px;
}

.panel-head,
.toolbar {
  display: flex;
  justify-content: space-between;
  gap: 16px;
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

.stats-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin-top: 18px;
}

.stat-card {
  padding: 16px 18px;
  border-radius: 18px;
  background: rgba(54, 95, 77, 0.08);
  display: grid;
  gap: 8px;
}

.stat-card span {
  color: #6a7c76;
  font-size: 0.9rem;
}

.stat-card strong {
  font-size: 1.8rem;
  color: #365f4d;
}

.toolbar {
  margin-top: 18px;
  align-items: stretch;
}

.bulk-toolbar {
  margin-top: 14px;
  padding: 14px 16px;
  border-radius: 18px;
  background: rgba(54, 95, 77, 0.06);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  flex-wrap: wrap;
}

.select-all-toggle {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  color: #48655a;
}

.selection-summary {
  color: #6a7c76;
  font-size: 0.92rem;
}

.bulk-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.toolbar-input {
  flex: 1;
}

.toolbar-input,
.form-card input,
.form-card textarea {
  border: 1px solid rgba(92, 120, 110, 0.18);
  border-radius: 16px;
  padding: 12px 14px;
  font: inherit;
  background: rgba(255, 255, 255, 0.85);
}

.form-card textarea {
  resize: vertical;
  min-height: 132px;
}

.filter-group {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.filter-chip {
  border: none;
  cursor: pointer;
  padding: 10px 14px;
  border-radius: 999px;
  background: rgba(54, 95, 77, 0.08);
  color: #365f4d;
  font: inherit;
}

.filter-chip.active {
  background: #365f4d;
  color: #fff;
}

.word-list {
  margin-top: 18px;
  display: grid;
  gap: 12px;
}

.word-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 16px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.6);
  border: 1px solid rgba(92, 120, 110, 0.14);
}

.word-check {
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.word-main {
  display: grid;
  gap: 6px;
  flex: 1;
}

.word-main strong {
  font-size: 1rem;
  color: #27493d;
}

.word-meta {
  color: #7a8c86;
  font-size: 0.9rem;
}

.word-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.status-badge {
  padding: 6px 10px;
  border-radius: 999px;
  font-size: 0.86rem;
}

.status-badge.active {
  background: rgba(104, 192, 138, 0.14);
  color: #2b7a47;
}

.status-badge.inactive {
  background: rgba(160, 166, 173, 0.16);
  color: #697780;
}

.side-panel {
  display: grid;
  gap: 18px;
  align-content: start;
}

.form-card,
.tips-card {
  padding: 18px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.58);
  border: 1px solid rgba(92, 120, 110, 0.14);
}

.form-card {
  display: grid;
  gap: 16px;
}

.form-card label {
  display: grid;
  gap: 8px;
}

.helper-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  color: #6a7c76;
  font-size: 0.92rem;
}

.form-card span {
  color: #58716a;
  font-size: 0.92rem;
}

.operation-card {
  display: grid;
  gap: 12px;
}

.operation-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.operation-head h3 {
  margin: 0;
  color: #365f4d;
}

.operation-list {
  display: grid;
  gap: 10px;
  max-height: 300px;
  overflow-y: auto;
}

.operation-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 14px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid rgba(92, 120, 110, 0.12);
}

.operation-main {
  display: grid;
  gap: 4px;
}

.operation-main strong {
  color: #2d5144;
}

.operation-main span {
  color: #72837d;
  font-size: 0.9rem;
}

.operation-meta {
  display: grid;
  justify-items: end;
  gap: 6px;
  color: #7a8c86;
  font-size: 0.86rem;
}

.operation-tag {
  padding: 4px 8px;
  border-radius: 999px;
}

.operation-tag.success {
  background: rgba(104, 192, 138, 0.14);
  color: #2b7a47;
}

.operation-tag.warning {
  background: rgba(224, 169, 79, 0.16);
  color: #8d6322;
}

.operation-tag.error {
  background: rgba(214, 85, 93, 0.14);
  color: #a33a41;
}

.operation-empty {
  padding: 14px;
}

.tips-card h3 {
  margin: 0 0 10px;
  color: #365f4d;
}

.tips-card ul {
  margin: 0;
  padding-left: 18px;
  color: #667a72;
  line-height: 1.75;
}

.empty-state {
  padding: 20px;
  text-align: center;
  color: #70837d;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.55);
  border: 1px dashed rgba(92, 120, 110, 0.2);
}

.primary-btn,
.ghost-btn,
.danger-btn {
  border: none;
  border-radius: 14px;
  padding: 12px 16px;
  font: inherit;
  cursor: pointer;
  transition: transform 0.2s ease, opacity 0.2s ease, box-shadow 0.2s ease;
}

.primary-btn {
  background: #365f4d;
  color: #fff;
  box-shadow: 0 10px 24px rgba(54, 95, 77, 0.18);
}

.ghost-btn {
  background: rgba(54, 95, 77, 0.08);
  color: #365f4d;
}

.danger-btn {
  background: rgba(214, 85, 93, 0.14);
  color: #a33a41;
}

.compact-btn {
  padding: 8px 12px;
}

.hidden-file-input {
  display: none;
}

.primary-btn:disabled,
.ghost-btn:disabled,
.danger-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.primary-btn:not(:disabled):hover,
.ghost-btn:not(:disabled):hover,
.danger-btn:not(:disabled):hover {
  transform: translateY(-1px);
}

@media (max-width: 1100px) {
  .admin-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .panel {
    padding: 18px;
  }

  .panel-head,
  .toolbar,
  .word-row,
  .bulk-toolbar,
  .helper-row,
  .operation-row {
    flex-direction: column;
    align-items: stretch;
  }

  .stats-grid {
    grid-template-columns: 1fr;
  }

  .word-actions {
    justify-content: flex-start;
  }

  .operation-meta {
    justify-items: start;
  }
}
</style>
