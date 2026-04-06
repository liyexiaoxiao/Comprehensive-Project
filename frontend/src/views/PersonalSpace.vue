<template>
  <div class="personal-space-page">
    <!-- Back Button -->
    <RouterLink to="/service" class="back-btn">
      <font-awesome-icon icon="angle-left" />
      <span>返回</span>
    </RouterLink>

    <div class="page-shell">
      <!-- Sidebar -->
      <aside class="sidebar glass-panel">
        <h2 class="sidebar-title">个人空间</h2>
        <nav class="space-nav">
          <button 
            v-for="tab in tabs" 
            :key="tab.id"
            :class="['nav-item', { active: currentTab === tab.id }]"
            @click="currentTab = tab.id"
          >
            <font-awesome-icon :icon="tab.icon" class="nav-icon" />
            <span>{{ tab.name }}</span>
          </button>
        </nav>
      </aside>

      <!-- Main Content -->
      <main class="main-content glass-panel">
        
        <!-- Tab 0: 我的信息 -->
        <div v-if="currentTab === 'profile'" class="tab-content profile-tab">
          <div class="profile-header">
            <h3 class="section-title">个人资料</h3>
            <button class="action-btn" @click="isEditingProfile = !isEditingProfile">
              {{ isEditingProfile ? '保存修改' : '编辑资料' }}
            </button>
          </div>
          
          <div class="profile-content glass-panel-inner">
            <div class="profile-avatar-section">
              <div class="avatar-wrapper">
                <font-awesome-icon icon="user" v-if="!userProfile.avatar" class="default-avatar" />
                <img :src="userProfile.avatar" v-else class="user-avatar" alt="User Avatar" />
                <div v-if="isEditingProfile" class="avatar-edit-overlay" @click="triggerAvatarUpload">
                  <font-awesome-icon icon="camera" />
                  <span>更换头像</span>
                </div>
                <input type="file" ref="avatarInputRef" @change="handleAvatarUpload" accept="image/*" class="hidden-input" />
              </div>
            </div>
            
            <div class="profile-details-section">
              <div class="info-group">
                <label>昵称</label>
                <div v-if="!isEditingProfile" class="info-text">{{ userProfile.name }}</div>
                <input v-else type="text" v-model="userProfile.name" class="info-input" />
              </div>
              
              <div class="info-group">
                <label>用户 ID</label>
                <div class="info-text id-text">{{ userProfile.id }} (不可修改)</div>
              </div>
              
              <div class="info-group">
                <label>性别</label>
                <div v-if="!isEditingProfile" class="info-text">{{ userProfile.gender || '未设置' }}</div>
                <select v-else v-model="userProfile.gender" class="info-input select-input">
                  <option value="">请选择</option>
                  <option value="男">男</option>
                  <option value="女">女</option>
                  <option value="保密">保密</option>
                </select>
              </div>
              
              <div class="info-group">
                <label>生日</label>
                <div v-if="!isEditingProfile" class="info-text">{{ userProfile.birthday || '未设置' }}</div>
                <input v-else type="date" v-model="userProfile.birthday" class="info-input" />
              </div>
              
              <div class="info-group full-width">
                <label>自我描述</label>
                <div v-if="!isEditingProfile" class="info-text description-text">{{ userProfile.description || '这个人很懒，什么都没写...' }}</div>
                <textarea v-else v-model="userProfile.description" class="info-input description-input" placeholder="介绍一下你自己吧..."></textarea>
              </div>
            </div>
          </div>
        </div>

        <!-- Tab 1: 情绪日记 -->
        <div v-else-if="currentTab === 'diary'" class="tab-content diary-tab">
          <div class="diary-layout">
            <div class="calendar-section">
              <el-calendar v-model="currentDate">
                <template #date-cell="{ data }">
                  <div class="calendar-cell">
                    <span>{{ data.day.split('-').slice(2).join('') }}</span>
                    <div v-if="hasDiary(data.day)" class="diary-dot"></div>
                  </div>
                </template>
              </el-calendar>
            </div>
            
            <div class="diary-editor-section">
              <h3 class="section-title">{{ selectedDateStr }} 情绪日记</h3>
              
              <div v-if="selectedDiary" class="diary-view">
                <div class="diary-tags">
                  <span 
                    v-for="emotion in selectedDiary.emotions" 
                    :key="emotion"
                    class="emotion-tag" 
                    :style="{ backgroundColor: getEmotionColor(emotion) }"
                  >
                    {{ getEmotionName(emotion) }}
                  </span>
                </div>
                <p class="diary-text">{{ selectedDiary.content }}</p>
                <button class="action-btn outline" @click="editDiary">编辑日记</button>
              </div>
              
              <div v-else class="diary-edit">
                <div class="emotion-selector">
                  <span class="label">今日情绪：</span>
                  <div class="emotion-options">
                    <button 
                      v-for="(config, emotion) in emotionConfig" 
                      :key="emotion"
                      :class="['emotion-option-btn', { active: newDiary.emotions.includes(emotion) }]"
                      :style="{ borderColor: config.color, backgroundColor: newDiary.emotions.includes(emotion) ? config.color : 'transparent' }"
                      @click="toggleEmotion(emotion)"
                    >
                      {{ config.name }}
                    </button>
                  </div>
                </div>
                <textarea 
                  v-model="newDiary.content" 
                  class="diary-textarea" 
                  placeholder="写下今天的感受吧..."
                ></textarea>
                <button class="action-btn" @click="saveDiary">保存日记</button>
              </div>
            </div>
          </div>
          
          <!-- Weekly Emotion Distribution Chart -->
          <div class="diary-weekly-chart glass-panel-inner">
            <h4 class="chart-title">周情绪分布统计</h4>
            <div class="chart-container" ref="diaryWeeklyChartRef"></div>
          </div>
          
          <!-- Friend Action Modal -->
          <div v-if="selectedFriend" class="modal-overlay" @click.self="selectedFriend = null">
            <div class="modal-content glass-panel">
              <div class="modal-header">
                <h3>好友详情</h3>
                <button class="close-btn" @click="selectedFriend = null">&times;</button>
              </div>
              <div class="modal-body friend-modal">
                <div class="friend-modal-avatar"><font-awesome-icon icon="user" /></div>
                <h4>{{ selectedFriend.name }}</h4>
                <p class="friend-modal-id">ID: {{ selectedFriend.id }}</p>
                <div class="friend-modal-intimacy">
                  <span>亲密度等级：</span>
                  <span v-for="n in 3" :key="n" class="petal large-petal" :class="{ active: n <= selectedFriend.intimacy }">🌸</span>
                </div>
                <div class="friend-modal-actions">
                  <button class="action-btn danger-btn" @click="deleteFriend(selectedFriend.id)">删除好友</button>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Tab 2: 情绪数据 -->
        <div v-else-if="currentTab === 'data'" class="tab-content data-tab">
          <div class="data-header">
            <h3 class="section-title">当周/当月情绪数据</h3>
          </div>
          
          <div class="charts-grid">
            <!-- 情绪趋势图 -->
            <div class="chart-card glass-panel-inner trend-chart-card">
              <h4 class="chart-title">情绪趋势图 (当周)</h4>
              <div class="chart-container" ref="trendChartRef"></div>
              
              <!-- 情绪总结 -->
              <div class="emotion-summary">
                <p>
                  <strong>本周情绪均分：</strong> 
                  <span :class="{'score-positive': weeklyAverageScore > 0, 'score-negative': weeklyAverageScore < 0}">
                    {{ weeklyAverageScore > 0 ? '+' : '' }}{{ weeklyAverageScore }}
                  </span>
                </p>
                <p><strong>评语：</strong> {{ weeklyComment }}</p>
              </div>
            </div>
            
            <!-- 情绪分布图 -->
            <div class="chart-card glass-panel-inner distribution-chart-card">
              <h4 class="chart-title">情绪分布图 (当周)</h4>
              <div class="chart-container" ref="distributionChartRef"></div>
            </div>
            
            <!-- 情绪热力图 -->
            <div class="chart-card glass-panel-inner full-width">
              <h4 class="chart-title">情绪热力图 ({{ currentMonth }}月)</h4>
              <div class="chart-container" ref="heatmapChartRef"></div>
            </div>
          </div>
        </div>

        <!-- Tab 3: 冥想数据 -->
        <div v-else-if="currentTab === 'meditation'" class="tab-content meditation-tab">
          <div class="meditation-header">
            <h3 class="section-title">冥想数据与花园</h3>
          </div>
          
          <div class="meditation-layout">
            <!-- Top Row: Chart (Left) & Calendar (Right) -->
            <div class="meditation-top-row">
              <!-- Bar Chart -->
              <div class="chart-card glass-panel-inner meditation-chart-container">
                <h4 class="chart-title">冥想时长统计 (当周)</h4>
                <div class="chart-container" ref="meditationChartRef"></div>
                <div class="emotion-summary">
                  <p><strong>本周平均冥想时长：</strong> {{ weeklyAverageMeditation }} 分钟/天</p>
                  <p><strong>评语：</strong> {{ meditationComment }}</p>
                </div>
              </div>
              
              <!-- Habit Tracker Calendar -->
              <div class="habit-tracker-container">
                <div class="habit-tracker-card">
                  <div class="tracker-header">
                    <button @click="prevTrackerMonth"><font-awesome-icon icon="angle-left" /></button>
                    <h4>{{ currentTrackerMonthName }}</h4>
                    <button @click="nextTrackerMonth"><font-awesome-icon icon="angle-right" /></button>
                  </div>
                  <div class="tracker-grid">
                    <div class="tracker-day-header" v-for="day in ['M','T','W','T','F','S','S']" :key="day">{{day}}</div>
                    <!-- empty slots for offset -->
                    <div class="tracker-cell" v-for="n in trackerOffset" :key="'empty-'+n"></div>
                    <!-- days -->
                    <div class="tracker-cell" v-for="day in daysInTrackerMonth" :key="'day-'+day.date">
                      <div :class="['tracker-dot', day.status]">
                        <font-awesome-icon v-if="day.status === 'missed'" icon="times" />
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
            
            <!-- Bottom Row: Garden -->
            <div class="meditation-garden glass-panel-inner colorful-garden-bg">
              <h4 class="chart-title garden-title">冥想花园</h4>
              
              <div class="garden-bags">
                <div :class="['bag-item', { active: activeBag === 'seeds' }]" @click="openBag('seeds')">
                  <div class="bag-icon">🎒</div>
                  <div class="bag-label">种子袋</div>
                </div>
                <div :class="['bag-item', { active: activeBag === 'fruits' }]" @click="openBag('fruits')">
                  <div class="bag-icon">🧺</div>
                  <div class="bag-label">果实袋</div>
                </div>
                <div :class="['bag-item', { active: activeBag === 'encyclopedia' }]" @click="openBag('encyclopedia')">
                  <div class="bag-icon">📖</div>
                  <div class="bag-label">花园图鉴</div>
                </div>
              </div>

              <!-- Content Area based on selection -->
              <div class="bag-content">
                <transition name="fade" mode="out-in">
                  <div v-if="activeBag === 'seeds'" key="seeds" class="garden-section">
                    <div class="inventory-grid">
                      <div class="inventory-item" v-for="seed in seedInventory" :key="seed.id">
                        <span class="item-icon">{{ seed.icon }}</span>
                        <span class="item-name">{{ seed.name }}</span>
                        <span class="item-count">x{{ seed.count }}</span>
                      </div>
                    </div>
                  </div>
                  
                  <div v-else-if="activeBag === 'fruits'" key="fruits" class="garden-section">
                    <div class="inventory-grid">
                      <div class="inventory-item" v-for="fruit in fruitInventory" :key="fruit.id">
                        <span class="item-icon">{{ fruit.icon }}</span>
                        <span class="item-name">{{ fruit.name }}</span>
                        <span class="item-count">x{{ fruit.count }}</span>
                      </div>
                    </div>
                  </div>
                  
                  <div v-else-if="activeBag === 'encyclopedia'" key="encyclopedia" class="garden-section">
                    <div class="encyclopedia-grid">
                      <div class="encyclopedia-item" v-for="plant in unlockedPlants" :key="plant.id" :title="plant.description">
                        <div class="plant-image">{{ plant.icon }}</div>
                        <span class="plant-name">{{ plant.name }}</span>
                      </div>
                    </div>
                  </div>
                </transition>
              </div>
            </div>
          </div>
        </div>

        <!-- Tab 4: 音乐库 -->
        <div v-else-if="currentTab === 'music'" class="tab-content placeholder-tab">
          <font-awesome-icon icon="music" class="placeholder-icon" />
          <h3>音乐库</h3>
          <p>功能开发中...</p>
        </div>

        <!-- Tab 5: 朋友圈 -->
        <div v-else-if="currentTab === 'social'" class="tab-content placeholder-tab">
          <font-awesome-icon icon="users" class="placeholder-icon" />
          <h3>朋友圈</h3>
          <p>分享你的心情，查看朋友们的动态（开发中）</p>
        </div>

        <!-- Tab 6: 好友 -->
        <div v-else-if="currentTab === 'friends'" class="tab-content friends-tab">
          <div class="friends-header">
            <h3 class="section-title">我的好友</h3>
            <div class="add-friend-section">
              <input type="text" v-model="newFriendId" placeholder="输入好友名字或ID搜索" class="search-input" />
              <button class="action-btn" @click="addFriend">添加好友</button>
            </div>
          </div>
          
          <div class="friends-list glass-panel-inner">
            <div class="friend-item" v-for="friend in friendsList" :key="friend.id" @click="selectFriend(friend)">
              <div class="friend-info">
                <div class="friend-avatar"><font-awesome-icon icon="user" /></div>
                <div class="friend-details">
                  <span class="friend-name">{{ friend.name }}</span>
                  <span class="friend-id">ID: {{ friend.id }}</span>
                </div>
              </div>
              <div class="friend-intimacy" :title="'亲密度: ' + friend.intimacy">
                <span v-for="n in 3" :key="n" class="petal" :class="{ active: n <= friend.intimacy }">🌸</span>
              </div>
            </div>
            <div v-if="friendsList.length === 0" class="empty-state">
              暂无好友，快去添加一些朋友吧！
            </div>
          </div>
        </div>
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch, nextTick } from 'vue'
import * as echarts from 'echarts'

// --- Profile State ---
const userProfile = ref({
  id: '12345678',
  name: '用户名',
  gender: '',
  birthday: '',
  description: '',
  avatar: ''
})
const isEditingProfile = ref(false)
const avatarInputRef = ref(null)

const triggerAvatarUpload = () => {
  avatarInputRef.value.click()
}

const handleAvatarUpload = (event) => {
  const file = event.target.files[0]
  if (file) {
    const reader = new FileReader()
    reader.onload = (e) => {
      userProfile.value.avatar = e.target.result
    }
    reader.readAsDataURL(file)
  }
}

// --- Tabs Configuration ---
const tabs = [
  { id: 'profile', name: '我的信息', icon: 'id-card' },
  { id: 'diary', name: '情绪日记', icon: 'book' },
  { id: 'data', name: '情绪数据', icon: 'chart-line' },
  { id: 'meditation', name: '冥想数据', icon: 'leaf' },
  { id: 'music', name: '音乐库', icon: 'music' },
  { id: 'social', name: '朋友圈', icon: 'users' },
  { id: 'friends', name: '好友', icon: 'user-friends' }
]
const currentTab = ref('profile')
const currentMonth = ref(new Date().getMonth() + 1)

// --- Emotion Configuration ---
const emotionConfig = {
  Happy: { name: '高兴', score: 2, color: '#4CAF50' },
  Calm: { name: '平静', score: 1, color: '#8BC34A' },
  Neutral: { name: '中性', score: 0, color: '#9E9E9E' },
  Surprise: { name: '惊讶', score: 0, color: '#FFC107' },
  Sad: { name: '悲伤', score: -1, color: '#2196F3' },
  Fear: { name: '恐惧', score: -2, color: '#9C27B0' },
  Disgust: { name: '厌恶', score: -2, color: '#FF9800' },
  Angry: { name: '愤怒', score: -2, color: '#F44336' }
}

const getEmotionColor = (key) => emotionConfig[key]?.color || '#9E9E9E'
const getEmotionName = (key) => emotionConfig[key]?.name || '未知'

// --- Tab 1: Diary State ---
const currentDate = ref(new Date())
const selectedDateStr = computed(() => {
  const d = currentDate.value
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
})

// Mock Diaries
const mockDiaries = ref({
  '2026-04-01': { emotions: ['Happy', 'Calm'], content: '今天天气很好，出去散步了，心情很不错！' },
  '2026-04-03': { emotions: ['Sad', 'Angry'], content: '工作遇到了一些挫折，感觉有点沮丧，还跟同事吵了一架。' },
  '2026-04-05': { emotions: ['Calm'], content: '安静地看了一下午书，内心很平静。' }
})

const hasDiary = (dateStr) => !!mockDiaries.value[dateStr]
const selectedDiary = computed(() => mockDiaries.value[selectedDateStr.value])

const newDiary = ref({ emotions: ['Happy'], content: '' })

const toggleEmotion = (emotion) => {
  const index = newDiary.value.emotions.indexOf(emotion)
  if (index > -1) {
    // If it's the last emotion, don't remove it (must select at least one)
    if (newDiary.value.emotions.length > 1) {
      newDiary.value.emotions.splice(index, 1)
    }
  } else {
    newDiary.value.emotions.push(emotion)
  }
}

const editDiary = () => {
  if (selectedDiary.value) {
    newDiary.value = { ...selectedDiary.value, emotions: [...selectedDiary.value.emotions] }
    // Temporarily remove to show edit form
    delete mockDiaries.value[selectedDateStr.value]
  }
}

const saveDiary = () => {
  if (!newDiary.value.content.trim()) return
  mockDiaries.value[selectedDateStr.value] = { ...newDiary.value, emotions: [...newDiary.value.emotions] }
  newDiary.value = { emotions: ['Happy'], content: '' }
}

// --- Tab 2: Data State & Charts ---
const trendChartRef = ref(null)
const distributionChartRef = ref(null)
const heatmapChartRef = ref(null)

const weeklyAverageScore = ref(1.2) // Mock average score
const weeklyComment = computed(() => {
  const score = weeklyAverageScore.value
  if (score > 10) return '本周你的情绪非常积极，继续保持这份好心情！'
  if (score > 0) return '本周情绪比较平稳，偶尔有小波澜，整体状态不错。'
  if (score > -10) return '本周情绪略显低落，建议多做一些让自己放松的事情，比如听音乐或冥想。'
  return '本周似乎经历了一些困难，情绪较为负面。请记得深呼吸，必要时可以寻求朋友的倾听或专业的帮助。'
})

// --- Meditation Data Logic ---
const weeklyMeditationData = [15, 30, 0, 45, 20, 60, 10]
const weeklyAverageMeditation = computed(() => {
  const sum = weeklyMeditationData.reduce((a, b) => a + b, 0)
  return (sum / weeklyMeditationData.length).toFixed(1)
})
const meditationComment = computed(() => {
  const avg = parseFloat(weeklyAverageMeditation.value)
  if (avg >= 30) return "太棒了！你保持了极佳的冥想习惯，内心一定非常平静吧。"
  if (avg >= 15) return "做得很好！继续保持，每天给自己一点专属的放松时间。"
  return "本周冥想时间较少哦，试着每天抽出几分钟放松一下身心吧。"
})

let trendChart = null
let distributionChart = null
let heatmapChart = null
let diaryWeeklyChart = null

const diaryWeeklyChartRef = ref(null)

const initDiaryWeeklyChart = () => {
  if (currentTab.value !== 'diary') return
  nextTick(() => {
    if (diaryWeeklyChartRef.value) {
      if (diaryWeeklyChart) diaryWeeklyChart.dispose()
      diaryWeeklyChart = echarts.init(diaryWeeklyChartRef.value)
      
      const days = ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
      const emotionKeys = ['Angry', 'Calm', 'Disgust', 'Fear', 'Happy', 'Neutral', 'Sad', 'Surprise']
      
      // Generate mock data for the diary chart
      // Format: [x_index, y_index]
      const chartData = [
        [0, 4], [0, 1], // 周一: Happy, Calm
        [1, 5],         // 周二: Neutral
        [2, 6], [2, 7], // 周三: Sad, Surprise
        [3, 4],         // 周四: Happy
        [4, 0], [4, 2], // 周五: Angry, Disgust
        [5, 4], [5, 1], // 周六: Happy, Calm
        [6, 1]          // 周日: Calm
      ]
      
      diaryWeeklyChart.setOption({
        grid: { left: '10%', right: '5%', top: '10%', bottom: '15%' },
        xAxis: {
          type: 'category',
          data: days,
          axisLine: { lineStyle: { color: '#ccc' } },
          splitLine: { show: true, lineStyle: { type: 'dashed', color: '#eee' } }
        },
        yAxis: {
          type: 'category',
          data: emotionKeys,
          axisLine: { show: false },
          axisTick: { show: false },
          splitLine: { show: true, lineStyle: { type: 'dashed', color: '#eee' } },
          axisLabel: {
            formatter: function (value) {
              return '{' + value + '|' + emotionConfig[value].name + '}';
            },
            rich: {
              Angry: { color: emotionConfig.Angry.color, fontWeight: 'bold' },
              Calm: { color: emotionConfig.Calm.color, fontWeight: 'bold' },
              Disgust: { color: emotionConfig.Disgust.color, fontWeight: 'bold' },
              Fear: { color: emotionConfig.Fear.color, fontWeight: 'bold' },
              Happy: { color: emotionConfig.Happy.color, fontWeight: 'bold' },
              Neutral: { color: emotionConfig.Neutral.color, fontWeight: 'bold' },
              Sad: { color: emotionConfig.Sad.color, fontWeight: 'bold' },
              Surprise: { color: emotionConfig.Surprise.color, fontWeight: 'bold' }
            }
          }
        },
        series: [{
          type: 'scatter',
          symbol: 'roundRect',
          symbolSize: [50, 25],
          itemStyle: {
            color: function(params) {
              return emotionConfig[emotionKeys[params.value[1]]].color;
            },
            borderRadius: 5
          },
          data: chartData
        }]
      })
    }
  })
}

const initCharts = () => {
  if (currentTab.value !== 'data') return
  
  nextTick(() => {
    // 1. Trend Chart
    if (trendChartRef.value) {
      trendChart = echarts.init(trendChartRef.value)
      trendChart.setOption({
        tooltip: { trigger: 'axis' },
        grid: { left: '5%', right: '10%', top: '10%', bottom: '10%', containLabel: true },
        xAxis: { 
          type: 'category', 
          data: ['周一', '周二', '周三', '周四', '周五', '周六', '周日'],
          axisLine: { lineStyle: { color: '#666' } }
        },
        yAxis: { 
          type: 'value', 
          min: -20, 
          max: 20,
          interval: 5,
          position: 'right',
          axisLabel: { 
            formatter: function(value) {
              if (value === 20) return 'Extreme';
              if (value === 15) return 'High';
              if (value === 10) return 'Medium';
              if (value === 5) return 'Low';
              if (value === 0) return '';
              if (value === -5) return 'Low';
              if (value === -10) return 'Medium';
              if (value === -15) return 'High';
              if (value === -20) return 'Extreme';
              return '';
            },
            color: function(value) {
              if (value > 0) return '#228B22';
              if (value < 0) return '#DC143C';
              return '#999';
            },
            fontWeight: 'bold'
          },
          splitLine: { lineStyle: { color: 'rgba(0,0,0,0.05)' } }
        },
        visualMap: {
          show: false,
          pieces: [
            { gt: 15, lte: 20, color: '#006400' }, 
            { gt: 10, lte: 15, color: '#228B22' }, 
            { gt: 5, lte: 10, color: '#3CB371' },  
            { gt: 0, lte: 5, color: '#90EE90' },   
            { gt: -5, lte: 0, color: '#FFB6C1' },  
            { gt: -10, lte: -5, color: '#FF69B4' }, 
            { gt: -15, lte: -10, color: '#DC143C' },
            { gt: -20, lte: -15, color: '#8B0000' } 
          ],
          outOfRange: { color: '#999' }
        },
        graphic: [
          {
            type: 'text',
            left: 'center',
            top: '25%',
            style: {
              text: 'POSITIVE',
              fill: 'rgba(34, 139, 34, 0.1)',
              font: 'bold 40px sans-serif'
            }
          },
          {
            type: 'text',
            left: 'center',
            top: '65%',
            style: {
              text: 'NEGATIVE',
              fill: 'rgba(220, 20, 60, 0.1)',
              font: 'bold 40px sans-serif'
            }
          }
        ],
        series: [{
          data: [2, 12, 18, -4, -17, -9, 3], // Mock scores from -20 to 20
          type: 'line',
          smooth: true,
          lineStyle: { width: 4 },
          markLine: {
            silent: true,
            data: [{ yAxis: 0 }],
            lineStyle: { color: '#ccc', width: 2 }
          }
        }]
      })
    }

    // 2. Distribution Chart
    if (distributionChartRef.value) {
      distributionChart = echarts.init(distributionChartRef.value)
      distributionChart.setOption({
        tooltip: { trigger: 'item' },
        legend: { orient: 'vertical', left: 'left', textStyle: { color: '#666' } },
        series: [{
          type: 'pie',
          radius: ['40%', '70%'],
          avoidLabelOverlap: false,
          itemStyle: {
            borderRadius: 10,
            borderColor: '#fff',
            borderWidth: 2
          },
          label: { show: false, position: 'center' },
          emphasis: {
            label: { show: true, fontSize: 16, fontWeight: 'bold' }
          },
          labelLine: { show: false },
          data: [
            { value: 3, name: '高兴', itemStyle: { color: emotionConfig.Happy.color } },
            { value: 2, name: '平静', itemStyle: { color: emotionConfig.Calm.color } },
            { value: 1, name: '中性', itemStyle: { color: emotionConfig.Neutral.color } },
            { value: 1, name: '悲伤', itemStyle: { color: emotionConfig.Sad.color } }
          ]
        }]
      })
    }

    // 3. Heatmap Chart
    if (heatmapChartRef.value) {
      heatmapChart = echarts.init(heatmapChartRef.value)
      
      const year = new Date().getFullYear();
      const month = currentMonth.value; // Use the current month
      const daysInMonth = new Date(year, month, 0).getDate();
      const firstDay = new Date(year, month - 1, 1).getDay();
      const firstDayIndex = firstDay === 0 ? 6 : firstDay - 1; // Convert to Mon=0, Sun=6

      const xData = ['周一', '周二', '周三', '周四', '周五', '周六', '周日'];
      const requiredWeeks = Math.ceil((firstDayIndex + daysInMonth) / 7);
      const yData = ['Week 1', 'Week 2', 'Week 3', 'Week 4', 'Week 5', 'Week 6'].slice(0, requiredWeeks);
      
      const heatData = [];
      for(let i=1; i<=daysInMonth; i++) {
        const currentPos = firstDayIndex + (i - 1);
        const x = currentPos % 7;
        const y = Math.floor(currentPos / 7);
        const score = Math.floor(Math.random() * 41) - 20;
        heatData.push([x, y, score, i]); // Store day number instead of month+day
      }

      heatmapChart.setOption({
        tooltip: { 
          position: 'top',
          formatter: function(params) {
            return `${month}月${params.value[3]}日<br/>情绪评分: ${params.value[2]}`;
          }
        },
        grid: {
          top: 40,
          bottom: 20,
          left: 60,
          right: 20
        },
        xAxis: {
          type: 'category',
          data: xData,
          position: 'top',
          splitArea: { show: true },
          axisTick: { show: false },
          axisLine: { show: false }
        },
        yAxis: {
          type: 'category',
          data: yData,
          inverse: true,
          splitArea: { show: true },
          axisTick: { show: false },
          axisLine: { show: false }
        },
        visualMap: {
          min: -20,
          max: 20,
          show: false,
          dimension: 2, // This fixes the color missing issue (mapping color to score instead of the text label)
          inRange: {
            color: ['#8B0000', '#DC143C', '#FFB6C1', '#f5f5f5', '#90EE90', '#228B22', '#006400'] 
          }
        },
        series: [{
          type: 'heatmap',
          data: heatData,
          label: {
            show: true,
            formatter: function(params) {
              return params.value[3]; // Display day number only
            },
            color: '#333',
            fontSize: 12,
            fontWeight: 'bold'
          },
          itemStyle: {
            borderWidth: 4,
            borderColor: '#fff',
            borderRadius: 4
          },
          emphasis: {
            itemStyle: {
              shadowBlur: 10,
              shadowColor: 'rgba(0, 0, 0, 0.5)'
            }
          }
        }]
      })
    }
  })
}

watch(currentTab, (newTab) => {
  if (newTab === 'diary') {
    initDiaryWeeklyChart()
  } else if (newTab === 'data') {
    initCharts()
  } else if (newTab === 'meditation') {
    initMeditationCharts()
  }
})

onMounted(() => {
  if (currentTab.value === 'diary') {
    initDiaryWeeklyChart()
  } else if (currentTab.value === 'data') {
    initCharts()
  } else if (currentTab.value === 'meditation') {
    initMeditationCharts()
  }
})

// --- Tab 6: Friends State ---
const friendsList = ref([
  { id: '1001', name: '小明', intimacy: 3 },
  { id: '1002', name: 'Alice', intimacy: 2 },
  { id: '1003', name: '李华', intimacy: 1 }
])
const newFriendId = ref('')
const selectedFriend = ref(null)

const addFriend = () => {
  if (!newFriendId.value.trim()) {
    alert('请输入好友名字或ID')
    return
  }
  const id = Math.floor(Math.random() * 9000 + 1000).toString()
  friendsList.value.push({
    id: id,
    name: newFriendId.value,
    intimacy: 1
  })
  newFriendId.value = ''
  alert('添加成功！')
}

const selectFriend = (friend) => {
  selectedFriend.value = friend
}

const deleteFriend = (id) => {
  if (confirm('确定要删除这位好友吗？')) {
    friendsList.value = friendsList.value.filter(f => f.id !== id)
    selectedFriend.value = null
  }
}

// --- Tab 3: Meditation State ---
const meditationChartRef = ref(null)
let meditationChart = null

const initMeditationCharts = () => {
  nextTick(() => {
    if (meditationChartRef.value) {
      if (meditationChart) meditationChart.dispose();
      meditationChart = echarts.init(meditationChartRef.value)
      meditationChart.setOption({
        tooltip: { trigger: 'axis', formatter: '{b} <br/>时长: {c} 分钟' },
        grid: { left: '5%', right: '5%', top: '15%', bottom: '15%', containLabel: true },
        xAxis: {
          type: 'category',
          data: ['周一', '周二', '周三', '周四', '周五', '周六', '周日'],
          axisLine: { lineStyle: { color: '#666' } }
        },
        yAxis: {
          type: 'value',
          name: '时长 (分钟)',
          splitLine: { lineStyle: { color: 'rgba(0,0,0,0.05)' } }
        },
        series: [{
          data: weeklyMeditationData,
          type: 'bar',
          barWidth: '40%',
          itemStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: '#8BC34A' },
              { offset: 1, color: '#4CAF50' }
            ]),
            borderRadius: [5, 5, 0, 0]
          }
        }]
      })
    }
  })
}

// Mock inventory
const seedInventory = ref([
  { id: 1, name: '向日葵种子', icon: '🌻', count: 5 },
  { id: 2, name: '玫瑰种子', icon: '🌹', count: 2 },
  { id: 3, name: '仙人掌种子', icon: '🌵', count: 12 },
])

const fruitInventory = ref([
  { id: 1, name: '向日葵籽', icon: '🌰', count: 20 },
  { id: 2, name: '玫瑰花瓣', icon: '🥀', count: 8 },
])

const unlockedPlants = ref([
  { id: 1, name: '向日葵', icon: '🌻', description: '充满阳光的植物' },
  { id: 2, name: '玫瑰', icon: '🌹', description: '代表热情的植物' },
  { id: 3, name: '小雏菊', icon: '🌼', description: '清新淡雅' },
  { id: 4, name: '仙人掌', icon: '🌵', description: '坚韧不拔' },
])

// Tracker Logic
const trackerDate = ref(new Date())
const currentTrackerMonthName = computed(() => {
  const months = ['JANUARY', 'FEBRUARY', 'MARCH', 'APRIL', 'MAY', 'JUNE', 'JULY', 'AUGUST', 'SEPTEMBER', 'OCTOBER', 'NOVEMBER', 'DECEMBER']
  return months[trackerDate.value.getMonth()]
})

const trackerOffset = computed(() => {
  const year = trackerDate.value.getFullYear()
  const month = trackerDate.value.getMonth()
  const firstDay = new Date(year, month, 1).getDay()
  return firstDay === 0 ? 6 : firstDay - 1 // Mon=0, Sun=6
})

const daysInTrackerMonth = computed(() => {
  const year = trackerDate.value.getFullYear()
  const month = trackerDate.value.getMonth()
  const daysInMonth = new Date(year, month + 1, 0).getDate()
  
  const days = []
  for(let i=1; i<=daysInMonth; i++) {
    let status = 'future'
    if (i < 15) status = Math.random() > 0.2 ? 'completed' : 'missed'
    else if (i === 15) status = 'partial'
    
    days.push({ date: i, status })
  }
  return days
})

const prevTrackerMonth = () => {
  trackerDate.value = new Date(trackerDate.value.getFullYear(), trackerDate.value.getMonth() - 1, 1)
}
const nextTrackerMonth = () => {
  trackerDate.value = new Date(trackerDate.value.getFullYear(), trackerDate.value.getMonth() + 1, 1)
}

// Garden Bags Logic
const activeBag = ref('seeds')
const openBag = (bag) => {
  activeBag.value = bag
}

// Resize charts on window resize
window.addEventListener('resize', () => {
  trendChart?.resize()
  distributionChart?.resize()
  heatmapChart?.resize()
  meditationChart?.resize()
})
</script>

<style scoped>
.personal-space-page {
  min-height: 100vh;
  padding: 40px 20px;
  display: flex;
  flex-direction: column;
  align-items: center;
  position: relative;
  overflow: hidden;
  /* Static Light Green Theme Background */
  background: linear-gradient(135deg, #f4fdf6, #e8f5e9, #f4fdf6);
}

.back-btn {
  position: absolute;
  top: 40px;
  left: 40px;
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--color-text-primary);
  text-decoration: none;
  font-size: 1.1rem;
  font-weight: 500;
  transition: all var(--transition-fast);
  z-index: 10;
}

.back-btn:hover {
  transform: translateX(-5px);
  color: var(--color-accent-sage);
}

.page-shell {
  width: 100%;
  max-width: 1400px;
  display: flex;
  gap: 20px;
  margin-top: 40px;
  height: calc(100vh - 120px);
}

.glass-panel {
  background: rgba(255, 255, 255, 0.6);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.6);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-float);
  overflow: hidden;
}

.glass-panel-inner {
  background: rgba(255, 255, 255, 0.4);
  border: 1px solid rgba(255, 255, 255, 0.5);
  border-radius: var(--radius-lg);
  padding: 20px;
}

/* Sidebar */
.sidebar {
  width: 250px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  padding: 30px 0;
}

.sidebar-title {
  padding: 0 30px;
  margin-bottom: 30px;
  font-family: var(--font-serif);
  color: var(--color-text-primary);
  font-size: 1.5rem;
}

.space-nav {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 0 15px;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 15px 20px;
  background: transparent;
  border: none;
  border-radius: var(--radius-lg);
  color: var(--color-text-secondary);
  font-size: 1.1rem;
  cursor: pointer;
  transition: all var(--transition-fast);
  text-align: left;
}

.nav-item:hover {
  background: rgba(255, 255, 255, 0.5);
  color: var(--color-text-primary);
}

.nav-item.active {
  background: var(--color-accent-sage);
  color: #fff;
  box-shadow: 0 4px 12px rgba(124, 152, 133, 0.3);
}

.nav-icon {
  width: 20px;
}

/* Main Content */
.main-content {
  flex: 1;
  padding: 30px;
  display: flex;
  flex-direction: column;
  overflow-y: auto;
}

.section-title {
  font-size: 1.4rem;
  color: var(--color-text-primary);
  margin-bottom: 20px;
  font-weight: 600;
}

/* Tab 1: Diary */
.diary-layout {
  display: flex;
  gap: 20px;
  margin-bottom: 20px;
}

.diary-weekly-chart {
  height: 350px;
  display: flex;
  flex-direction: column;
}

.diary-weekly-chart .chart-container {
  flex: 1;
}

.calendar-section {
  flex: 1;
  background: rgba(255, 255, 255, 0.5);
  border-radius: var(--radius-lg);
  padding: 10px;
  overflow: hidden;
}

.calendar-cell {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  position: relative;
}

.diary-dot {
  width: 6px;
  height: 6px;
  background-color: #F44336;
  border-radius: 50%;
  margin-top: 4px;
}

/* Override Element Plus Calendar Styles */
:deep(.el-calendar) {
  background: transparent;
}
:deep(.el-calendar__header) {
  border-bottom: none;
}
:deep(.el-calendar-table td.is-selected) {
  background-color: rgba(140, 165, 149, 0.2);
}

.diary-editor-section {
  width: 400px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
}

.emotion-selector {
  margin-bottom: 20px;
}

.emotion-selector .label {
  display: block;
  margin-bottom: 10px;
  color: var(--color-text-secondary);
}

.emotion-options {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.emotion-option-btn {
  padding: 6px 12px;
  border-radius: var(--radius-pill);
  border: 1px solid;
  cursor: pointer;
  font-size: 0.9rem;
  transition: all 0.2s;
  color: var(--color-text-primary);
}

.emotion-option-btn.active {
  color: #fff;
  border-color: transparent !important;
}

.diary-textarea {
  flex: 1;
  width: 100%;
  min-height: 200px;
  padding: 15px;
  border: 1px solid rgba(0,0,0,0.1);
  border-radius: var(--radius-md);
  background: rgba(255, 255, 255, 0.5);
  resize: none;
  font-family: inherit;
  font-size: 1rem;
  color: var(--color-text-primary);
  margin-bottom: 20px;
}

.diary-textarea:focus {
  outline: none;
  border-color: var(--color-accent-sage);
  background: #fff;
}

.action-btn {
  width: 100%;
  padding: 12px;
  background: var(--color-text-primary);
  color: #fff;
  border: none;
  border-radius: var(--radius-pill);
  font-size: 1.1rem;
  cursor: pointer;
  transition: all 0.2s;
}

.action-btn:hover {
  opacity: 0.9;
  transform: translateY(-2px);
}

.action-btn.outline {
  background: transparent;
  border: 1px solid var(--color-text-primary);
  color: var(--color-text-primary);
}

.action-btn.outline:hover {
  background: var(--color-text-primary);
  color: #fff;
}

.diary-view {
  background: rgba(255, 255, 255, 0.5);
  border-radius: var(--radius-lg);
  padding: 20px;
  flex: 1;
  display: flex;
  flex-direction: column;
}

.diary-tags {
  margin-bottom: 15px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.emotion-tag {
  display: inline-block;
  padding: 4px 12px;
  border-radius: var(--radius-pill);
  color: #fff;
  font-size: 0.85rem;
}

.diary-text {
  flex: 1;
  line-height: 1.6;
  color: var(--color-text-primary);
  white-space: pre-wrap;
  margin-bottom: 20px;
}

/* Tab 2: Data */
.data-tab {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.charts-grid {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 20px;
  flex: 1;
}

.chart-card {
  display: flex;
  flex-direction: column;
}

.chart-card.full-width {
  grid-column: 1 / -1;
  min-height: 250px;
}

.chart-title {
  font-size: 1.1rem;
  color: var(--color-text-secondary);
  margin-bottom: 15px;
}

.chart-container {
  flex: 1;
  width: 100%;
  min-height: 200px;
}

/* Placeholder Tabs */
.placeholder-tab {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: var(--color-text-secondary);
}

.placeholder-icon {
  font-size: 4rem;
  color: rgba(0,0,0,0.1);
  margin-bottom: 20px;
}

.emotion-summary {
  margin-top: 20px;
  padding: 20px 25px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  font-size: 1.05rem;
  color: #555;
  background: rgba(255, 255, 255, 0.4);
  border-radius: 16px;
}

.emotion-summary strong {
  color: #333;
  font-weight: 600;
}

.score-positive {
  color: #228B22;
  font-weight: bold;
}

.score-negative {
  color: #DC143C;
  font-weight: bold;
}

/* --- Meditation Tab Styles --- */
.meditation-tab {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.meditation-header {
  margin-bottom: 20px;
}

.meditation-layout {
  display: flex;
  flex-direction: column;
  gap: 20px;
  flex: 1;
  overflow-y: auto;
}

.meditation-top-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}

.meditation-chart-container {
  min-height: 380px;
  display: flex;
  flex-direction: column;
}

.habit-tracker-container {
  display: flex;
  justify-content: center;
  align-items: flex-start; /* Prevent top clipping */
  padding-top: 10px;
  min-height: 380px; 
}

.habit-tracker-card {
  background: #66BB6A;
  color: white;
  padding: 20px;
  border-radius: var(--radius-lg);
  display: flex;
  flex-direction: column;
  align-items: center;
  box-shadow: var(--shadow-float);
  width: 100%;
  max-width: 320px;
}

.tracker-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  margin-bottom: 15px;
}

.tracker-header h4 {
  margin: 0;
  font-size: 1.1rem;
  letter-spacing: 2px;
}

.tracker-header button {
  background: none;
  border: none;
  color: white;
  font-size: 1.1rem;
  cursor: pointer;
  padding: 5px;
  transition: transform 0.2s;
}
.tracker-header button:hover {
  transform: scale(1.2);
}

.tracker-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 8px; /* Smaller gaps */
  width: 100%;
}

.tracker-day-header {
  text-align: center;
  font-weight: bold;
  font-size: 0.9rem;
  margin-bottom: 5px;
  color: rgba(255,255,255,0.9);
}

.tracker-cell {
  display: flex;
  justify-content: center;
  align-items: center;
  aspect-ratio: 1;
}

.tracker-dot {
  width: 24px; /* Smaller dots */
  height: 24px;
  border-radius: 50%;
  display: flex;
  justify-content: center;
  align-items: center;
}

.tracker-dot.completed {
  background-color: white;
}

.tracker-dot.missed {
  background-color: transparent;
  color: rgba(255,255,255,0.6);
  font-size: 1.2rem;
  font-weight: bold;
}

.tracker-dot.partial {
  border: 3px solid white;
  background-color: transparent;
}

.tracker-dot.future {
  background-color: rgba(255,255,255,0.2);
}

/* Garden Styles */
.colorful-garden-bg {
  background: 
    linear-gradient(135deg, rgba(255, 230, 240, 0.8), rgba(220, 245, 230, 0.8)),
    url('data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="100" height="100" viewBox="0 0 100 100"><text x="10" y="40" font-size="20" opacity="0.15">🌸</text><text x="60" y="20" font-size="15" opacity="0.15">🌺</text><text x="40" y="80" font-size="25" opacity="0.15">🌼</text><text x="80" y="70" font-size="18" opacity="0.15">🌻</text><text x="20" y="90" font-size="12" opacity="0.15">🌷</text></svg>');
  border: 1px solid rgba(255, 255, 255, 0.7);
  box-shadow: inset 0 0 30px rgba(255, 255, 255, 0.5), var(--shadow-float);
  border-radius: var(--radius-xl);
  padding: 30px;
}

.garden-title {
  text-align: center;
  font-size: 1.5rem;
  color: #2c5e3b;
  text-shadow: 1px 1px 2px rgba(255,255,255,0.8);
  margin-bottom: 25px;
}

.meditation-garden {
  display: flex;
  flex-direction: column;
  gap: 20px;
  flex: 1;
}

.garden-bags {
  display: flex;
  justify-content: space-around;
  margin-bottom: 10px;
}

.bag-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 10px 20px;
  background: rgba(255, 255, 255, 0.4);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all 0.3s;
  border: 2px solid transparent;
  min-width: 100px;
}

.bag-item:hover {
  transform: translateY(-5px);
  background: rgba(255, 255, 255, 0.6);
  box-shadow: var(--shadow-md);
}

.bag-item.active {
  background: rgba(255, 255, 255, 0.8);
  border-color: var(--color-accent-sage);
  transform: scale(1.05);
  box-shadow: 0 8px 20px rgba(0,0,0,0.1);
}

.bag-icon {
  font-size: 2rem;
  margin-bottom: 5px;
  transition: transform 0.3s;
}

.bag-item:hover .bag-icon {
  transform: scale(1.1) rotate(5deg);
}

.bag-label {
  font-size: 0.9rem;
  font-weight: bold;
  color: #555;
}

.bag-content {
  position: relative;
  min-height: 200px;
}

.garden-section {
  background: rgba(255,255,255,0.4);
  padding: 20px;
  border-radius: var(--radius-md);
  width: 100%;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease, transform 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
  transform: translateY(10px);
}

.inventory-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(90px, 1fr));
  gap: 15px;
}

.inventory-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  background: rgba(255,255,255,0.6);
  padding: 15px 10px;
  border-radius: var(--radius-sm);
  box-shadow: var(--shadow-sm);
  transition: transform 0.2s;
}

.inventory-item:hover {
  transform: translateY(-3px);
}

.inventory-item .item-icon {
  font-size: 2rem;
  margin-bottom: 8px;
}

.inventory-item .item-name {
  font-size: 0.85rem;
  color: #555;
  text-align: center;
}

.inventory-item .item-count {
  font-size: 0.9rem;
  font-weight: bold;
  color: #333;
  margin-top: 4px;
}

.encyclopedia-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(80px, 1fr));
  gap: 15px;
}

.encyclopedia-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  background: rgba(255,255,255,0.6);
  padding: 15px 10px;
  border-radius: var(--radius-sm);
  box-shadow: var(--shadow-sm);
  cursor: help;
  transition: all 0.2s;
}

.encyclopedia-item:hover {
  transform: scale(1.05);
  background: rgba(255,255,255,0.8);
}

.plant-image {
  font-size: 2.5rem;
  margin-bottom: 8px;
}

.plant-name {
  font-size: 0.85rem;
  color: #555;
  text-align: center;
  font-weight: 500;
}
/* Profile Tab Styles */
.profile-tab {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.profile-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.profile-header .action-btn {
  width: auto;
  padding: 8px 20px;
  font-size: 0.95rem;
}

.profile-content {
  display: flex;
  flex-direction: column;
  gap: 30px;
  padding: 40px;
}

.profile-avatar-section {
  display: flex;
  justify-content: center;
  margin-bottom: 10px;
}

.avatar-wrapper {
  position: relative;
  width: 120px;
  height: 120px;
  border-radius: 50%;
  background: var(--color-accent-sage);
  display: flex;
  justify-content: center;
  align-items: center;
  overflow: hidden;
  box-shadow: var(--shadow-md);
}

.default-avatar {
  font-size: 4rem;
  color: white;
}

.user-avatar {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-edit-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.5);
  color: white;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  cursor: pointer;
  opacity: 0;
  transition: opacity 0.3s;
  font-size: 0.9rem;
  gap: 5px;
}

.avatar-wrapper:hover .avatar-edit-overlay {
  opacity: 1;
}

.hidden-input {
  display: none;
}

.profile-details-section {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
}

.info-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.info-group.full-width {
  grid-column: 1 / -1;
}

.info-group label {
  font-size: 0.9rem;
  color: #666;
  font-weight: bold;
}

.info-text {
  padding: 10px 15px;
  background: rgba(0, 0, 0, 0.04);
  border-radius: var(--radius-md);
  min-height: 42px;
  display: flex;
  align-items: center;
  color: var(--color-text-primary);
}

.id-text {
  color: #888;
  background: rgba(0, 0, 0, 0.08);
}

.info-input {
  padding: 10px 15px;
  border: 1px solid rgba(0, 0, 0, 0.1);
  border-radius: var(--radius-md);
  background: rgba(255, 255, 255, 0.8);
  font-family: inherit;
  font-size: 1rem;
  color: var(--color-text-primary);
  outline: none;
  transition: border-color 0.3s;
}

.info-input:focus {
  border-color: var(--color-accent-sage);
}

.select-input {
  cursor: pointer;
}

.description-text {
  min-height: 100px;
  align-items: flex-start;
  white-space: pre-wrap;
}

.description-input {
  min-height: 100px;
  resize: vertical;
}

/* Friends Tab Styles */
.friends-tab {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.friends-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.add-friend-section {
  display: flex;
  gap: 10px;
}

.search-input {
  padding: 8px 15px;
  border-radius: var(--radius-full);
  border: 1px solid rgba(0,0,0,0.1);
  background: rgba(255,255,255,0.6);
  outline: none;
  min-width: 200px;
}

.friends-list {
  flex: 1;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 20px;
}

.friend-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px 20px;
  background: rgba(255,255,255,0.5);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all 0.3s;
}

.friend-item:hover {
  background: rgba(255,255,255,0.8);
  transform: translateY(-2px);
  box-shadow: var(--shadow-sm);
}

.friend-info {
  display: flex;
  align-items: center;
  gap: 15px;
}

.friend-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: var(--color-accent-sage);
  color: white;
  display: flex;
  justify-content: center;
  align-items: center;
  font-size: 1.2rem;
}

.friend-details {
  display: flex;
  flex-direction: column;
}

.friend-name {
  font-weight: bold;
  color: var(--color-text-primary);
}

.friend-id {
  font-size: 0.8rem;
  color: #777;
}

.friend-intimacy {
  display: flex;
  gap: 5px;
}

.petal {
  opacity: 0.2;
  filter: grayscale(100%);
  transition: all 0.3s;
}

.petal.active {
  opacity: 1;
  filter: grayscale(0%);
}

.friend-modal {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 20px 0;
  text-align: center;
}

.friend-modal-avatar {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: var(--color-accent-sage);
  color: white;
  display: flex;
  justify-content: center;
  align-items: center;
  font-size: 2.5rem;
  margin-bottom: 15px;
}

.friend-modal h4 {
  font-size: 1.5rem;
  margin-bottom: 5px;
}

.friend-modal-id {
  color: #666;
  margin-bottom: 20px;
}

.friend-modal-intimacy {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 30px;
  font-size: 1.1rem;
}

.large-petal {
  font-size: 1.5rem;
}

.friend-modal-actions {
  width: 100%;
}

.danger-btn {
  background: #ff5252;
  color: white;
  width: 100%;
}

.danger-btn:hover {
  background: #ff1744;
}

</style>
