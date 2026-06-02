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
        <div class="sidebar-brand">
          <span class="sidebar-kicker">My Sanctuary</span>
          <h2 class="sidebar-title">个人空间</h2>
          <p class="sidebar-subtitle">记录、整理与回看你的情绪轨迹</p>
        </div>
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
            <button class="action-btn" @click="handleProfileAction" :disabled="isProfileSubmitting">
              {{ isProfileSubmitting ? '保存中...' : isEditingProfile ? '保存修改' : '编辑资料' }}
            </button>
          </div>
          
          <div class="profile-content glass-panel-inner">
            <div class="profile-avatar-section">
              <div class="avatar-wrapper">
                <font-awesome-icon icon="user" v-if="!userProfile.avatar" class="default-avatar" />
                <img loading="lazy" :src="userProfile.avatar" v-else class="user-avatar" alt="User Avatar" />
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
                <label>用户名</label>
                <div class="info-text id-text">{{ userProfile.username || '未设置' }} (不可修改)</div>
              </div>
              
              <div class="info-group">
                <label>邮箱</label>
                <div v-if="!isEditingProfile" class="info-text">{{ userProfile.email || '未设置' }}</div>
                <input v-else type="email" v-model.trim="userProfile.email" class="info-input" />
              </div>

              <div class="info-group">
                <label>手机号</label>
                <div v-if="!isEditingProfile" class="info-text">{{ userProfile.phone || '未设置' }}</div>
                <input v-else type="text" v-model.trim="userProfile.phone" class="info-input" />
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
              
              <div v-if="isSelectedDateToday" class="diary-edit">
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
                <div class="diary-actions">
                  <button class="action-btn" @click="saveDiary">{{ selectedDiary ? '保存修改' : '保存日记' }}</button>
                  <button v-if="selectedDiary" class="action-btn danger-btn" @click="deleteDiary">删除日记</button>
                </div>
                <p class="diary-hint">今日日记可反复修改；历史日记不可修改，但可删除。</p>
              </div>

              <div v-else-if="selectedDiary" class="diary-view">
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
                <div class="diary-actions">
                  <button class="action-btn danger-btn" @click="deleteDiary">删除日记</button>
                </div>
                <p class="diary-hint">历史日记不可修改，但可删除。</p>
              </div>
              
              <div v-else class="diary-empty-state">
                <p class="diary-hint">只能编写今天的日记；历史日期仅支持查看。</p>
              </div>
            </div>
          </div>
          
          <!-- Weekly Emotion Distribution Chart -->
          <div class="diary-weekly-chart glass-panel-inner">
            <h4 class="chart-title">周情绪分布统计</h4>
            <div class="chart-container" ref="diaryWeeklyChartRef"></div>
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

        <!-- Tab 3: 服务反馈 -->
        <div v-else-if="currentTab === 'feedback'" class="tab-content feedback-tab">
          <div class="feedback-tab-header">
            <div>
              <h3 class="section-title">服务反馈</h3>
              <p class="feedback-header-desc">记录你对疗效数据、社区互动与 AI 能力的主观体验，帮助后端持续优化推荐与服务偏好。</p>
            </div>
            <span class="feedback-summary-pill">{{ myFeedbackList.length }} 条反馈</span>
          </div>

          <div class="feedback-layout">
            <section class="feedback-card glass-panel-inner">
              <div class="feedback-card-head">
                <div>
                  <h4 class="chart-title">提交反馈</h4>
                  <p>告诉我们你更认可哪些模块、哪里还需要优化，便于后端持续打磨服务质量。</p>
                </div>
              </div>

              <div class="feedback-form-grid">
                <label class="feedback-field">
                  <span>反馈服务</span>
                  <select v-model="feedbackForm.service" class="info-input feedback-select">
                    <option
                      v-for="option in feedbackServiceOptions"
                      :key="option.value"
                      :value="option.value"
                    >
                      {{ option.label }}
                    </option>
                  </select>
                </label>

                <label class="feedback-field">
                  <span>满意度评分</span>
                  <div class="feedback-rating-row">
                    <button
                      v-for="score in feedbackRatingOptions"
                      :key="score"
                      type="button"
                      :class="['feedback-rating-btn', { active: Number(feedbackForm.rating) === score }]"
                      @click="feedbackForm.rating = score"
                    >
                      {{ score }}
                    </button>
                  </div>
                </label>

                <label class="feedback-field full-width">
                  <span>反馈内容</span>
                  <textarea
                    v-model="feedbackForm.feedback"
                    class="info-input info-textarea feedback-textarea"
                    rows="4"
                    maxlength="300"
                    placeholder="例如：情绪趋势图很直观，但我更希望看到疗效数据和服务建议联动。"
                  />
                </label>
              </div>

              <div class="feedback-actions">
                <button
                  class="action-btn"
                  :disabled="isFeedbackSubmitting || !feedbackForm.feedback.trim()"
                  @click="submitFeedback"
                >
                  {{ isFeedbackSubmitting ? '提交中...' : '提交反馈' }}
                </button>
                <button class="action-btn outline" type="button" @click="resetFeedbackForm">
                  重置
                </button>
              </div>
            </section>

            <section class="feedback-card glass-panel-inner">
              <div class="feedback-card-head">
                <div>
                  <h4 class="chart-title">反馈偏好画像</h4>
                  <p>基于历史反馈自动聚合，帮助你快速回看自己更认可哪些服务模块。</p>
                </div>
              </div>

              <div v-if="feedbackPreferenceCards.length" class="feedback-preference-grid">
                <article
                  v-for="item in feedbackPreferenceCards"
                  :key="item.service"
                  class="feedback-preference-item"
                >
                  <div class="feedback-preference-top">
                    <strong>{{ item.service }}</strong>
                    <span>{{ item.scoreText }}</span>
                  </div>
                  <div class="feedback-progress-track">
                    <div class="feedback-progress-bar" :style="{ width: `${item.percent}%` }"></div>
                  </div>
                </article>
              </div>
              <div v-else class="empty-state compact-empty-state feedback-empty-state">
                暂无偏好画像，提交第一条反馈后会在这里自动生成。
              </div>

              <div class="feedback-history-head">
                <h5>最近反馈</h5>
                <button class="action-btn outline slim-btn feedback-refresh-btn" type="button" :disabled="isFeedbackLoading" @click="fetchFeedbackCenterData">
                  {{ isFeedbackLoading ? '刷新中...' : '刷新' }}
                </button>
              </div>
              <div v-if="myFeedbackList.length" class="feedback-history-list">
                <article v-for="item in myFeedbackList" :key="item.id" class="feedback-history-item">
                  <div class="feedback-history-top">
                    <strong>{{ item.service }}</strong>
                    <span>{{ item.ratingLabel }}</span>
                  </div>
                  <p>{{ item.feedback }}</p>
                  <span class="feedback-history-time">{{ item.timeLabel }}</span>
                </article>
              </div>
              <div v-else class="empty-state compact-empty-state feedback-empty-state">
                还没有反馈记录，欢迎补充你对当前功能体验的看法。
              </div>
            </section>
          </div>
        </div>

        <!-- Tab 4: 冥想数据 -->
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
        <div v-else-if="currentTab === 'music'" class="tab-content music-tab">
          <div class="music-header">
            <h3 class="section-title">音乐库</h3>
            <div class="music-actions">
              <button class="action-btn music-main-btn" @click="openUploadModal">上传音乐</button>
              <button class="action-btn outline music-main-btn" @click="openCreatePlaylistModal">新建歌单</button>
            </div>
          </div>

          <div class="music-content glass-panel-inner">
            <div class="music-overview-grid">
              <article class="music-overview-card">
                <span class="overview-label">我的上传</span>
                <strong>{{ musicStore.uploadedTracks.length }}</strong>
                <p>首个人上传音乐，形成你的情绪声音档案。</p>
              </article>
              <article class="music-overview-card">
                <span class="overview-label">我喜欢</span>
                <strong>{{ likedTracksList.length }}</strong>
                <p>首喜欢歌曲，沉淀近期最常回访的旋律。</p>
              </article>
              <article class="music-overview-card">
                <span class="overview-label">我收藏</span>
                <strong>{{ collectedTracksList.length }}</strong>
                <p>首收藏内容，方便在不同场景快速切换。</p>
              </article>
              <article class="music-overview-card">
                <span class="overview-label">我的歌单</span>
                <strong>{{ musicStore.customPlaylists.length }}</strong>
                <p>个自建歌单，把常用氛围整理成自己的入口。</p>
              </article>
              <article class="music-overview-card">
                <span class="overview-label">黑名单</span>
                <strong>{{ blockedTracksList.length }}</strong>
                <p>首暂时不想再被推荐的歌曲，会从推荐结果里排除。</p>
              </article>
            </div>

            <div class="music-sections-grid">
              <section class="music-showcase-card uploads-card">
                <div class="music-showcase-header">
                  <div>
                    <span class="music-card-kicker">Personal Archive</span>
                    <h4>我的上传</h4>
                    <p>近期上传的情绪音乐会优先展示在这里，便于继续归类和加入歌单。</p>
                  </div>
                  <span class="section-meta">{{ musicStore.uploadedTracks.length }} 首</span>
                </div>

                <div v-if="musicStore.uploadedTracks.length" class="music-track-scroll-area">
                  <div class="music-track-stack">
                  <article class="music-track-card" v-for="track in musicStore.uploadedTracks" :key="track.id">
                    <div class="track-main">
                      <div class="track-title-row">
                        <strong>{{ track.title }}</strong>
                        <span class="track-duration">{{ formatDuration(track.duration) }}</span>
                      </div>
                      <span class="track-artist-name">{{ getTrackArtist(track) }}</span>
                      <div class="track-tag-row">
                        <span v-for="tag in getTrackTags(track)" :key="tag" class="small-tag">{{ tag }}</span>
                      </div>
                    </div>
                    <div class="track-action-row">
                      <button class="icon-btn" @click="toggleUploadedTrackPreview(track)" :title="previewingUploadedTrackId === track.id ? '暂停试听' : '试听音乐'">
                        {{ previewingUploadedTrackId === track.id ? '⏸' : '▶' }}
                      </button>
                      <button class="icon-btn" @click="openAddToPlaylistModal(track)" title="加入歌单">➕</button>
                      <button class="icon-btn danger" @click="deleteUploadedTrack(track.id)" title="删除">🗑️</button>
                    </div>
                  </article>
                  </div>
                </div>
                <div v-else class="empty-state compact-empty-state">
                  还没上传过音乐，点击上方按钮上传你的第一首歌吧！
                </div>
              </section>

              <section class="music-showcase-card likes-card">
                <div class="music-showcase-header">
                  <div>
                    <span class="music-card-kicker">Favorite Mood</span>
                    <h4>我喜欢</h4>
                    <p>这里保留你最近最想反复回放的歌曲，适合快速进入熟悉的情绪状态。</p>
                  </div>
                  <span class="section-meta">{{ likedTracksList.length }} 首</span>
                </div>

                <div v-if="likedTracksList.length" class="music-track-scroll-area">
                  <div class="music-track-stack">
                  <article class="music-track-card compact-track-card" v-for="track in likedTracksList" :key="track.id">
                    <div class="track-main">
                      <div class="track-title-row">
                        <strong>{{ track.title }}</strong>
                        <span class="track-duration">{{ formatDuration(track.duration) }}</span>
                      </div>
                      <span class="track-artist-name">{{ getTrackArtist(track) }}</span>
                      <div class="track-tag-row">
                        <span v-for="tag in getTrackTags(track)" :key="tag" class="small-tag subtle-tag">{{ tag }}</span>
                      </div>
                    </div>
                    <div class="track-action-row">
                      <button class="icon-btn" @click="musicStore.toggleLike(track.id)" title="取消喜欢">💔</button>
                      <button class="icon-btn" @click="openAddToPlaylistModal(track)" title="加入歌单">➕</button>
                    </div>
                  </article>
                  </div>
                </div>
                <div v-else class="empty-state compact-empty-state">
                  暂无喜欢的音乐。
                </div>
              </section>

              <section class="music-showcase-card collections-card">
                <div class="music-showcase-header">
                  <div>
                    <span class="music-card-kicker">Saved Library</span>
                    <h4>我收藏</h4>
                    <p>收藏区更适合留住一些不同场景的备用旋律，比如夜晚、通勤和专注时刻。</p>
                  </div>
                  <span class="section-meta">{{ collectedTracksList.length }} 首</span>
                </div>

                <div v-if="collectedTracksList.length" class="music-track-scroll-area">
                  <div class="music-track-stack">
                  <article class="music-track-card compact-track-card" v-for="track in collectedTracksList" :key="track.id">
                    <div class="track-main">
                      <div class="track-title-row">
                        <strong>{{ track.title }}</strong>
                        <span class="track-duration">{{ formatDuration(track.duration) }}</span>
                      </div>
                      <span class="track-artist-name">{{ getTrackArtist(track) }}</span>
                      <div class="track-tag-row">
                        <span v-for="tag in getTrackTags(track)" :key="tag" class="small-tag subtle-tag">{{ tag }}</span>
                      </div>
                    </div>
                    <div class="track-action-row">
                      <button class="icon-btn" @click="musicStore.toggleCollect(track.id)" title="取消收藏">❌</button>
                      <button class="icon-btn" @click="openAddToPlaylistModal(track)" title="加入歌单">➕</button>
                    </div>
                  </article>
                  </div>
                </div>
                <div v-else class="empty-state compact-empty-state">
                  暂无收藏的音乐。
                </div>
              </section>

              <section class="music-showcase-card playlists-card">
                <div class="music-showcase-header">
                  <div>
                    <span class="music-card-kicker">Playlist Studio</span>
                    <h4>我的歌单</h4>
                    <p>用歌单把不同时间段和状态拆开管理，左右滑动切换不同歌单，详情通过弹窗查看。</p>
                  </div>
                  <span class="section-meta">{{ musicStore.customPlaylists.length }} 个</span>
                </div>

                <div
                  v-if="musicStore.customPlaylists.length"
                  ref="playlistCarouselRef"
                  class="playlist-carousel-shell"
                  @wheel.prevent="handlePlaylistWheel"
                >
                  <div class="playlist-carousel">
                  <article
                    class="playlist-preview-card"
                    v-for="pl in musicStore.customPlaylists"
                    :key="pl.id"
                    :class="{ active: activePlaylist?.id === pl.id }"
                    @click="selectPlaylist(pl)"
                  >
                    <div class="playlist-info">
                      <h4>{{ pl.name }}</h4>
                      <p>{{ pl.description }}</p>
                      <span>{{ formatPlaylistMeta(pl) }}</span>
                    </div>
                    <div class="playlist-card-actions">
                      <button class="action-btn outline slim-btn" @click.stop="openPlaylistDetailModal(pl)">查看</button>
                      <button class="icon-btn delete-pl-btn always-show" @click.stop="deletePlaylistAndReset(pl.id)" title="删除歌单">🗑️</button>
                    </div>
                  </article>
                  </div>
                </div>
                <div v-else class="empty-state compact-empty-state">
                  暂无歌单，点击上方新建歌单吧！
                </div>
              </section>

              <section class="music-showcase-card blocked-card">
                <div class="music-showcase-header">
                  <div>
                    <span class="music-card-kicker">Blocked Tracks</span>
                    <h4>黑名单</h4>
                    <p>这些歌曲会从推荐结果里排除；如果想重新接收推荐，可以在这里移除。</p>
                  </div>
                  <span class="section-meta">{{ blockedTracksList.length }} 首</span>
                </div>

                <div v-if="blockedTracksList.length" class="music-track-scroll-area">
                  <div class="music-track-stack">
                  <article class="music-track-card compact-track-card" v-for="track in blockedTracksList" :key="track.id">
                    <div class="track-main">
                      <div class="track-title-row">
                        <strong>{{ track.title }}</strong>
                        <span class="track-duration">{{ formatDuration(track.duration) }}</span>
                      </div>
                      <span class="track-artist-name">{{ getTrackArtist(track) }}</span>
                      <div class="track-tag-row">
                        <span class="small-tag blocked-tag">不再推荐</span>
                        <span v-for="tag in getTrackTags(track)" :key="tag" class="small-tag subtle-tag">{{ tag }}</span>
                      </div>
                    </div>
                    <div class="track-action-row">
                      <button class="icon-btn" @click="musicStore.toggleBlock(track.id)" title="移出黑名单">✅</button>
                      <button class="icon-btn" @click="openAddToPlaylistModal(track)" title="加入歌单">➕</button>
                    </div>
                  </article>
                  </div>
                </div>
                <div v-else class="empty-state compact-empty-state">
                  暂无黑名单歌曲。
                </div>
              </section>
            </div>
          </div>
        </div>

        <!-- Tab 5: 朋友圈 -->
        <div v-else-if="currentTab === 'social'" class="tab-content social-tab">
          <div class="social-header">
            <div>
              <h3 class="section-title">朋友圈</h3>
              <p class="social-header-desc">像动态广场一样浏览好友近况，支持点赞、评论与带上“现在的情绪”发布帖子。</p>
            </div>
            <div class="social-header-actions">
              <button class="action-btn social-secondary-btn" @click="openMyPostsSpace">我的帖子</button>
              <button class="action-btn social-publish-btn" @click="openSocialComposer">发布</button>
            </div>
          </div>

          <div class="social-layout">
            <div class="social-feed">
              <div v-if="isSocialPostsLoading" class="empty-state compact-empty-state">
                正在加载动态...
              </div>
              <article v-for="post in socialPosts" :key="post.id" class="social-post-card glass-panel-inner">
                <div class="social-post-top">
                  <div class="social-author-block">
                    <div class="social-avatar">
                      <img loading="lazy" v-if="post.authorAvatarUrl" :src="post.authorAvatarUrl" alt="avatar" />
                      <span v-else>{{ getNameInitial(post.authorName) }}</span>
                    </div>
                    <div>
                      <div class="social-author-row">
                        <strong>{{ post.authorName }}</strong>
                        <span class="social-role-tag">{{ post.authorRole }}</span>
                      </div>
                      <span class="social-post-time">{{ post.timeLabel }}</span>
                    </div>
                  </div>
                  <div class="social-post-top-actions">
                    <span class="social-mood-pill">现在的情绪 · {{ post.mood }}</span>
                    <div class="social-menu-wrap">
                      <button class="social-menu-btn" @click="toggleSocialMenu(post)">...</button>
                      <div v-if="post.isMenuOpen" class="social-menu-dropdown">
                        <button class="social-menu-item" @click="handleSocialMenuLike(post)">
                          {{ post.likedByMe ? '取消点赞' : '点赞' }}
                        </button>
                        <button class="social-menu-item" @click="openSocialCommentEditor(post)">
                          评论
                        </button>
                        <button class="social-menu-item" @click="requestPostAiSuggestion(post)">
                          AI 回帖建议
                        </button>
                        <button
                          v-if="Number(post.authorUserId) === currentUserId"
                          class="social-menu-item danger"
                          @click="deleteSocialPost(post)"
                        >
                          删除帖子
                        </button>
                      </div>
                    </div>
                  </div>
                </div>

                <p class="social-post-content">{{ post.content }}</p>

                <div v-if="post.highlights?.length" class="social-highlight-row">
                  <span v-for="item in post.highlights" :key="item" class="social-highlight-chip"># {{ item }}</span>
                </div>

                <div
                  v-if="post.isAiSuggestionLoading || post.aiSuggestion || post.aiSuggestionError"
                  class="social-ai-suggestion-card"
                >
                  <div class="social-ai-suggestion-head">
                    <strong>AI 回帖建议</strong>
                    <span v-if="post.isAiSuggestionLoading">正在生成...</span>
                    <div v-else class="social-ai-suggestion-tools">
                      <button
                        v-if="post.aiSuggestion"
                        class="social-text-btn"
                        type="button"
                        @click="applyAiSuggestionToComment(post)"
                      >
                        带入评论框
                      </button>
                      <button
                        class="social-text-btn"
                        type="button"
                        @click="requestPostAiSuggestion(post)"
                      >
                        重新生成
                      </button>
                    </div>
                  </div>
                  <p v-if="post.aiSuggestion" class="social-ai-suggestion-text">{{ post.aiSuggestion }}</p>
                  <p v-else-if="post.aiSuggestionError" class="social-ai-suggestion-error">{{ post.aiSuggestionError }}</p>
                  <p v-else class="social-ai-suggestion-placeholder">AI 正在分析帖子情绪并生成一条共情回复。</p>
                </div>

                <div v-if="post.likeUsers.length || post.comments.length" class="social-feedback-card">
                  <div v-if="post.likeUsers.length" class="social-like-line">
                    <span class="social-like-icon">♡</span>
                    <span>{{ formatSocialLikeNames(post.likeUsers) }}</span>
                  </div>

                  <div class="social-comment-list">
                    <div v-for="comment in post.comments" :key="comment.id" class="social-comment-item">
                      <div class="social-comment-main">
                        <span class="social-comment-author">{{ comment.author }}</span>
                        <span v-if="comment.replyToName" class="social-comment-reply-target">回复 {{ comment.replyToName }}</span>
                        <span class="social-comment-text">：{{ comment.content }}</span>
                        <span class="social-comment-time">{{ comment.timeLabel }}</span>
                      </div>
                      <div v-if="comment.likeUsers.length" class="social-comment-like-line">
                        <span class="social-like-icon">♡</span>
                        <span>{{ formatSocialLikeNames(comment.likeUsers) }}</span>
                      </div>
                      <div class="social-comment-actions">
                        <button class="social-comment-action-btn" @click="toggleCommentLike(post, comment)">
                          {{ comment.likedByMe ? '取消点赞' : '点赞' }}
                        </button>
                        <button class="social-comment-action-btn" @click="openCommentReplyEditor(post, comment)">
                          回复
                        </button>
                      </div>
                      <div v-if="comment.isReplyEditorOpen" class="social-comment-reply-editor">
                        <input
                          v-model="comment.replyDraft"
                          type="text"
                          class="info-input"
                          placeholder="回复这条评论..."
                          @keyup.enter="submitCommentReply(post, comment)"
                        />
                        <button class="action-btn social-comment-btn" @click="submitCommentReply(post, comment)">发送</button>
                      </div>
                    </div>
                  </div>
                </div>

                <div v-if="post.isCommentEditorOpen" class="social-comment-editor">
                  <input
                    v-model="post.commentDraft"
                    type="text"
                    class="info-input"
                    placeholder="写下你的评论..."
                    @keyup.enter="submitSocialComment(post)"
                  />
                  <button class="action-btn social-comment-btn" @click="submitSocialComment(post)">评论</button>
                </div>
              </article>

              <div v-if="!isSocialPostsLoading && socialPosts.length === 0" class="empty-state compact-empty-state">
                朋友圈暂时还没有动态，点击右上角发布第一条帖子吧！
              </div>

              <div v-if="socialTotalPages > 1" class="social-pagination">
                <button
                  class="action-btn social-secondary-btn"
                  type="button"
                  :disabled="isSocialPostsLoading || socialPage <= 0"
                  @click="changeSocialPage(-1)"
                >
                  上一页
                </button>
                <span class="social-page-indicator">第 {{ socialPage + 1 }} / {{ socialTotalPages }} 页</span>
                <button
                  class="action-btn social-secondary-btn"
                  type="button"
                  :disabled="isSocialPostsLoading || socialPage >= socialTotalPages - 1"
                  @click="changeSocialPage(1)"
                >
                  下一页
                </button>
              </div>
            </div>

            <aside class="social-side-panel glass-panel-inner">
              <div class="social-side-card">
                <span class="social-side-kicker">Social Snapshot</span>
                <h4>今日互动看板</h4>
                <div class="social-stat-grid">
                  <div class="social-stat-item">
                    <strong>{{ socialSummary.postCount }}</strong>
                    <span>动态</span>
                  </div>
                  <div class="social-stat-item">
                    <strong>{{ socialSummary.commentCount }}</strong>
                    <span>收到评论/回复</span>
                  </div>
                  <div class="social-stat-item">
                    <strong>{{ socialSummary.likeCount }}</strong>
                    <span>收到点赞</span>
                  </div>
                  <div class="social-stat-item">
                    <strong>{{ friendsList.length }}</strong>
                    <span>好友</span>
                  </div>
                </div>
              </div>

              <div class="social-side-card">
                <span class="social-side-kicker">Notifications</span>
                <h4>今日互动提醒</h4>
                <div v-if="socialNotifications.length" class="social-notification-list">
                  <div v-for="item in socialNotifications" :key="item.id" class="social-notification-item">
                    <strong>{{ item.actorName }}</strong>
                    <span>{{ item.message }}</span>
                    <em>{{ item.timeLabel }}</em>
                  </div>
                </div>
                <p v-else>今天暂时还没有新的互动提醒。</p>
              </div>

              <div class="social-side-card muted">
                <span class="social-side-kicker">Posting Tip</span>
                <h4>发布建议</h4>
                <p>发帖时可以写下今天的片段，再补充一句“现在的情绪”，更容易让好友理解你的状态。</p>
                <div class="social-trend-tags">
                  <span v-for="tag in socialMoodSuggestions" :key="tag" class="small-tag subtle-tag">{{ tag }}</span>
                </div>
              </div>
            </aside>
          </div>
        </div>

        <!-- Tab 6: 好友 -->
        <div v-else-if="currentTab === 'friends'" class="tab-content friends-tab">
          <div class="friends-header">
            <h3 class="section-title">我的好友</h3>
            <div class="add-friend-section">
              <input 
                type="text" 
                v-model="newFriendId" 
                placeholder="输入用户名搜索" 
                class="search-input"
                @keyup.enter="searchUser"
              />
              <button class="action-btn" @click="searchUser" :disabled="isSearching">
                {{ isSearching ? '搜索中...' : '搜索' }}
              </button>
            </div>
          </div>
          
          <div class="search-results-panel glass-panel-inner" v-if="searchResults.length > 0">
            <h4 class="sub-section-title">搜索结果</h4>
            <div class="search-results-list">
              <div class="friend-item" v-for="user in searchResults" :key="user.userId">
                <div class="friend-info">
                  <div class="friend-avatar">
                    <img loading="lazy" v-if="user.avatarUrl" :src="user.avatarUrl" alt="avatar" />
                    <font-awesome-icon v-else icon="user" />
                  </div>
                  <div class="friend-details">
                    <span class="friend-name">{{ user.nickname || user.username }}</span>
                    <span class="friend-id">{{ user.username ? `账号：${user.username}` : (user.userId ? `用户ID：${user.userId}` : '账号信息暂不可用') }}</span>
                  </div>
                </div>
                <button class="action-btn outline slim-btn" @click="addFriend(user.userId)">发送申请</button>
              </div>
            </div>
          </div>

          <div class="friend-requests-panel glass-panel-inner" v-if="friendRequests.length > 0">
            <h4 class="sub-section-title">好友申请</h4>
            <div class="friend-requests-list">
              <div class="friend-item" v-for="req in friendRequests" :key="req.id">
                <div class="friend-info">
                  <div class="friend-avatar">
                    <img loading="lazy" v-if="req.senderAvatarUrl" :src="req.senderAvatarUrl" alt="avatar" />
                    <font-awesome-icon v-else icon="user" />
                  </div>
                  <div class="friend-details">
                    <span class="friend-name">{{ req.senderName }}</span>
                    <span class="friend-id">{{ req.senderUsername ? `账号：${req.senderUsername}` : (req.senderId ? `用户ID：${req.senderId}` : '请求添加您为好友') }}</span>
                  </div>
                </div>
                <div class="request-actions">
                  <button class="action-btn slim-btn" @click="handleRequest(req.id, true)">同意</button>
                  <button class="action-btn outline slim-btn" @click="handleRequest(req.id, false)">拒绝</button>
                </div>
              </div>
            </div>
          </div>
          
          <div class="friends-dashboard" :class="{ 'detail-open': !!selectedFriend }">
            <section class="friends-list-panel glass-panel-inner">
              <div class="friends-list-panel-header">
                <div>
                  <h4 class="sub-section-title">好友列表</h4>
                  <p class="friend-panel-hint">点击好友，在右侧查看基础信息和友情度成长。</p>
                </div>
                <span class="friend-count-badge">{{ friendsList.length }} 位好友</span>
              </div>

              <div class="friends-list">
                <div
                  class="friend-item"
                  :class="{ active: selectedFriend?.id === friend.id }"
                  v-for="friend in friendsList"
                  :key="friend.id"
                  @click="selectFriend(friend)"
                >
                  <div class="friend-info">
                    <div class="friend-avatar">
                      <img loading="lazy" v-if="friend.avatarUrl" :src="friend.avatarUrl" alt="avatar" />
                      <font-awesome-icon v-else icon="user" />
                    </div>
                    <div class="friend-details">
                      <span class="friend-name">{{ friend.name }}</span>
                      <span class="friend-id">{{ friend.username ? `账号：${friend.username}` : (friend.userId ? `用户ID：${friend.userId}` : '账号信息暂不可用') }}</span>
                      <span class="friend-desc">{{ friend.friendshipDescription }}</span>
                    </div>
                  </div>
                  <div class="friend-item-side">
                    <div class="friend-intimacy" :title="`友情度：${friend.friendshipScore}`">
                      <span v-for="n in 3" :key="n" class="petal" :class="{ active: n <= friend.intimacy }">🌸</span>
                    </div>
                    <span class="friend-score-label">{{ friend.friendshipScore }} 分</span>
                  </div>
                </div>

                <div v-if="friendsList.length === 0" class="empty-state">
                  暂无好友，快去添加一些朋友吧！
                </div>
              </div>
            </section>

            <aside v-if="selectedFriend" class="friend-detail-panel glass-panel-inner">
              <div class="friend-detail-card">
                <div class="friend-detail-toolbar">
                  <button class="friend-detail-close" @click="closeSelectedFriend">关闭</button>
                </div>
                <div class="friend-detail-top">
                  <div class="friend-detail-avatar">
                    <img loading="lazy" v-if="selectedFriend.avatarUrl" :src="selectedFriend.avatarUrl" alt="avatar" />
                    <font-awesome-icon v-else icon="user" />
                  </div>
                  <div class="friend-detail-main">
                    <div class="friend-detail-heading">
                      <div>
                        <h4>{{ selectedFriend.name }}</h4>
                        <p>{{ selectedFriend.username ? `账号：${selectedFriend.username}` : (selectedFriend.userId ? `用户ID：${selectedFriend.userId}` : '账号信息暂不可用') }}</p>
                      </div>
                      <span class="friendship-badge">{{ selectedFriend.intimacy }}/3 朵花</span>
                    </div>
                    <p class="friend-bio">{{ selectedFriend.bio || '这个朋友还没有留下自我描述。' }}</p>
                  </div>
                </div>

                <div class="friend-detail-stats">
                  <div class="friend-stat-card">
                    <span>友情度</span>
                    <strong>{{ selectedFriend.friendshipScore }}</strong>
                  </div>
                  <div class="friend-stat-card">
                    <span>已点亮花朵</span>
                    <strong>{{ selectedFriend.intimacy }}/3</strong>
                  </div>
                  <div class="friend-stat-card">
                    <span>状态</span>
                    <strong>{{ selectedFriend.intimacy >= 3 ? '满级好友' : '持续升温中' }}</strong>
                  </div>
                </div>

                <div class="friendship-progress-card">
                  <div class="friendship-progress-header">
                    <span>友情花朵</span>
                    <span>{{ selectedFriend.friendshipScore }}/150</span>
                  </div>
                  <div class="friend-intimacy detail-intimacy">
                    <span v-for="n in 3" :key="n" class="petal large-petal" :class="{ active: n <= selectedFriend.intimacy }">🌸</span>
                  </div>
                  <div class="friendship-progress-track">
                    <div class="friendship-progress-bar" :style="{ width: `${selectedFriend.progressPercent}%` }"></div>
                  </div>
                  <p class="friendship-description">{{ selectedFriend.friendshipDescription }}</p>
                  <p class="friendship-rule-tip">互动规则：点赞对方帖子 +3，评论对方帖子 +5。</p>
                </div>

                <div class="friend-basic-grid">
                  <div class="friend-basic-item">
                    <span>最近更新</span>
                    <strong>{{ formatFriendDate(selectedFriend.updatedAt) }}</strong>
                  </div>
                  <div class="friend-basic-item">
                    <span>下一目标</span>
                    <strong>{{ selectedFriend.nextGoalText }}</strong>
                  </div>
                </div>

                <div class="friend-detail-actions">
                  <button class="action-btn outline friend-chat-btn" @click="openChatWithFriend(selectedFriend)">发消息</button>
                  <button class="action-btn danger-btn friend-delete-btn" @click="deleteFriend(selectedFriend.friendshipId)">删除好友</button>
                </div>
              </div>
            </aside>
          </div>
        </div>

        <!-- Tab 7: 聊天室 -->
        <div v-else-if="currentTab === 'chat'" class="tab-content chat-tab">
          <div class="chat-header">
            <div>
              <h3 class="section-title">聊天室</h3>
              <p class="chat-header-desc">从已添加好友中选择一位，打开一对一聊天窗口并查看历史消息。</p>
            </div>
            <span class="friend-count-badge">{{ sortedChatFriends.length }} 位好友可聊天</span>
          </div>

          <div class="chat-dashboard">
            <section class="chat-friend-panel glass-panel-inner">
              <div class="friends-list-panel-header">
                <div>
                  <h4 class="sub-section-title">好友会话</h4>
                  <p class="friend-panel-hint">优先展示最近有消息的好友，也可以直接点开尚未开始聊天的好友。</p>
                </div>
                <span v-if="isChatListLoading" class="chat-status-text">刷新中...</span>
              </div>

              <div v-if="sortedChatFriends.length > 0" class="chat-friend-list">
                <button
                  v-for="friend in sortedChatFriends"
                  :key="friend.id"
                  type="button"
                  class="chat-friend-item"
                  :class="{ active: activeChatFriendId === friend.id }"
                  @click="openChatWithFriend(friend)"
                >
                  <div class="friend-info">
                    <div class="friend-avatar">
                      <img loading="lazy" v-if="friend.avatarUrl" :src="friend.avatarUrl" alt="avatar" />
                      <font-awesome-icon v-else icon="user" />
                    </div>
                    <div class="friend-details">
                      <div class="chat-friend-topline">
                        <span class="friend-name">{{ friend.name }}</span>
                        <span class="chat-list-time">{{ formatChatListTime(friend.lastMessageAt) }}</span>
                      </div>
                      <span class="friend-id">{{ friend.username ? `账号：${friend.username}` : (friend.userId ? `用户ID：${friend.userId}` : '账号信息暂不可用') }}</span>
                      <div class="chat-preview-row">
                        <span class="chat-last-message">{{ friend.lastMessagePreview || '点击开始聊天' }}</span>
                        <span v-if="friend.unreadCount > 0" class="chat-unread-badge">
                          {{ friend.unreadCount > 99 ? '99+' : friend.unreadCount }}
                        </span>
                      </div>
                    </div>
                  </div>
                </button>
              </div>

              <div v-else class="empty-state compact-empty-state">
                暂无可聊天的好友，请先在“好友”页添加朋友。
              </div>
            </section>

            <section class="chat-window-panel glass-panel-inner">
              <template v-if="activeChatFriend">
                <div class="chat-window-header">
                  <div class="friend-info">
                    <div class="friend-avatar large-avatar">
                      <img loading="lazy" v-if="activeChatFriend.avatarUrl" :src="activeChatFriend.avatarUrl" alt="avatar" />
                      <font-awesome-icon v-else icon="user" />
                    </div>
                    <div class="friend-details">
                      <div class="chat-friend-topline">
                        <h4>{{ activeChatFriend.name }}</h4>
                        <span class="friendship-badge">{{ activeChatFriend.intimacy }}/3 朵花</span>
                      </div>
                      <span class="friend-id">{{ activeChatFriend.username ? `账号：${activeChatFriend.username}` : (activeChatFriend.userId ? `用户ID：${activeChatFriend.userId}` : '账号信息暂不可用') }}</span>
                      <span class="friend-desc">{{ activeChatFriend.bio || '和这位朋友聊聊最近的心情吧。' }}</span>
                    </div>
                  </div>
                </div>

                <div ref="chatMessageListRef" class="chat-message-list">
                  <div v-if="isChatLoading" class="empty-state compact-empty-state">
                    正在加载聊天记录...
                  </div>
                  <template v-else-if="chatMessages.length > 0">
                    <div
                      v-for="message in chatMessages"
                      :key="message.messageId"
                      class="chat-message-row"
                      :class="{ own: Number(message.senderId) === currentUserId }"
                    >
                      <div class="chat-message-bubble">
                        <p>{{ message.content }}</p>
                        <span class="chat-message-meta">
                          {{ formatChatMessageTime(message.createdAt) }}
                          <template v-if="Number(message.senderId) === currentUserId">
                            {{ message.readAt ? ' · 已读' : ' · 已发送' }}
                          </template>
                        </span>
                      </div>
                    </div>
                  </template>
                  <div v-else class="empty-state compact-empty-state">
                    还没有聊天记录，发送第一条消息开始对话吧。
                  </div>
                </div>

                <div class="chat-composer">
                  <textarea
                    v-model="chatDraft"
                    ref="chatTextareaRef"
                    class="chat-textarea"
                    placeholder="输入你想发送的内容，按 Enter 发送，Shift + Enter 换行"
                    maxlength="2000"
                    @keydown.enter.exact.prevent="sendCurrentChatMessage"
                  ></textarea>
                  <div class="chat-composer-footer">
                    <div class="chat-composer-left">
                      <button class="chat-suggestion-btn" type="button" @click="openSupportiveReplyModal">
                        支持型回复建议
                      </button>
                      <span class="chat-status-text">按 Enter 发送，Shift + Enter 换行</span>
                    </div>
                    <button
                      class="action-btn friend-chat-send-btn"
                      :disabled="isSendingChat || !chatDraft.trim()"
                      @click="sendCurrentChatMessage"
                    >
                      {{ isSendingChat ? '发送中...' : '发送' }}
                    </button>
                  </div>
                </div>

                <div
                  v-if="supportiveReplyModalVisible"
                  class="modal-overlay"
                  @click.self="closeSupportiveReplyModal"
                >
                  <div class="modal-content glass-panel supportive-replies-modal-card">
                    <div class="modal-header">
                      <h3>支持型回复建议</h3>
                      <button class="close-btn" type="button" @click="closeSupportiveReplyModal">&times;</button>
                    </div>
                    <div class="supportive-modal-body">
                      <div class="supportive-modal-intro">
                        <span class="supportive-kicker">建议模板</span>
                        <p>先选一个回复方向，再点击右侧建议内容即可自动带入输入框。整体会偏温和、克制，适合朋友间日常支持。</p>
                      </div>
                      <div class="supportive-modal-layout">
                        <div class="supportive-categories-panel">
                          <button
                            v-for="category in supportiveReplyCategories"
                            :key="category.key"
                            type="button"
                            :class="['supportive-category', { active: activeSupportiveReplyCategory === category.key }]"
                            @click="activeSupportiveReplyCategory = category.key"
                          >
                            <strong>{{ category.label }}</strong>
                            <span>{{ category.description }}</span>
                          </button>
                        </div>
                        <div class="supportive-suggestions-panel">
                          <div class="supportive-panel-head">
                            <div>
                              <span class="supportive-kicker">当前类型</span>
                              <h4>{{ activeSupportiveReplyCategoryMeta.label }}</h4>
                            </div>
                            <p>{{ activeSupportiveReplyCategoryMeta.description }}</p>
                          </div>
                          <div class="supportive-suggestions">
                            <button
                              v-for="text in activeSupportiveReplies"
                              :key="text"
                              type="button"
                              class="supportive-chip"
                              @click="applySupportiveReply(text)"
                            >
                              <strong>{{ text }}</strong>
                              <span>点击带入聊天输入框</span>
                            </button>
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </template>

              <div v-else class="empty-state compact-empty-state">
                从左侧选择一位好友，打开聊天窗口。
              </div>
            </section>
          </div>
        </div>
      </main>

      <!-- Music Modals -->
      <div v-if="activeMusicModal === 'upload'" class="modal-overlay" @click.self="closeMusicModal">
        <div class="modal-content glass-panel music-modal-card upload-modal-card">
          <div class="modal-header">
            <h3>上传音乐</h3>
            <button class="close-btn" @click="closeMusicModal">&times;</button>
          </div>
          <div class="modal-body music-upload-form">
            <div class="form-group">
              <label>选择音频文件 (MP3)</label>
              <input ref="audioInputRef" type="file" accept=".mp3,audio/*" class="hidden-file-input" @change="handleAudioSelect" />
              <div class="file-select-wrap">
                <button class="modal-btn secondary-btn large-btn" @click="triggerAudioSelect">选择 MP3 文件</button>
                <span class="selected-file-name">{{ uploadForm.file ? uploadForm.file.name : '未选择文件' }}</span>
              </div>
            </div>
            <div class="form-group">
              <label>歌曲名</label>
              <input type="text" v-model="uploadForm.title" placeholder="输入歌曲名称" class="info-input" />
            </div>
            <div class="form-group">
              <label>作者（可选）</label>
              <input type="text" v-model="uploadForm.artist" placeholder="输入作者名称，不填则展示为佚名" class="info-input" />
            </div>
            <div class="form-group">
              <label>自动识别封面</label>
              <div class="music-upload-cover-preview">
                <img
                  v-if="uploadForm.coverUrl"
                  :src="uploadForm.coverUrl"
                  alt="识别到的歌曲封面"
                  class="music-upload-cover-image"
                />
                <div v-else class="music-upload-cover-placeholder">
                  未识别到内嵌封面，上传后将使用默认情绪封面展示
                </div>
              </div>
            </div>
            <div class="form-group">
              <label>情绪标签</label>
              <div class="tag-input-group stack">
                <input type="text" v-model="uploadForm.tagInput" placeholder="输入情绪(如: 安静)" class="info-input" />
                <div class="tag-action-row">
                  <button class="modal-btn secondary-btn" @click="addTag">添加标签</button>
                  <button class="modal-btn ai-btn" @click="autoDetectTags" :disabled="isDetectingUploadTags">
                    {{ isDetectingUploadTags ? '识别中...' : 'AI 识别标签' }}
                  </button>
                </div>
              </div>
              <p v-if="uploadForm.aiCaption" class="music-upload-ai-caption">AI 描述：{{ uploadForm.aiCaption }}</p>
              <div class="current-tags">
                <span v-for="(tag, index) in uploadForm.tags" :key="index" class="small-tag">
                  {{ tag }} <span class="remove-tag" @click="uploadForm.tags.splice(index, 1)">&times;</span>
                </span>
              </div>
            </div>
            <div class="modal-actions">
              <button class="modal-btn secondary-btn" @click="closeMusicModal">取消</button>
              <button class="modal-btn primary-btn" @click="submitUpload" :disabled="!uploadForm.file || !uploadForm.title || isUploadingTrack">
                {{ isUploadingTrack ? '上传中...' : '确认上传' }}
              </button>
            </div>
          </div>
        </div>
      </div>

      <div v-if="activeMusicModal === 'playlist'" class="modal-overlay" @click.self="closeMusicModal">
        <div class="modal-content glass-panel music-modal-card playlist-modal-card">
          <div class="modal-header">
            <h3>新建歌单</h3>
            <button class="close-btn" @click="closeMusicModal">&times;</button>
          </div>
          <div class="modal-body music-create-form">
            <div class="form-group">
              <label>歌单名称</label>
              <input type="text" v-model="newPlaylistName" placeholder="输入歌单名称" class="info-input" />
            </div>
            <div class="form-group">
              <label>歌单简介</label>
              <textarea
                v-model="newPlaylistDescription"
                placeholder="输入歌单简介，比如适合通勤、夜晚或专注时播放"
                class="info-input info-textarea"
                rows="4"
              />
            </div>
            <div class="modal-actions">
              <button class="modal-btn secondary-btn" @click="closeMusicModal">取消</button>
              <button class="modal-btn primary-btn" @click="submitCreatePlaylist" :disabled="!newPlaylistName">创建</button>
            </div>
          </div>
        </div>
      </div>

      <div v-if="showPlaylistDetailModal && activePlaylist" class="modal-overlay" @click.self="closePlaylistDetailModal">
        <div class="modal-content glass-panel music-modal-card playlist-detail-modal-card">
          <div class="modal-header">
            <div class="playlist-detail-modal-title">
              <span class="playlist-focus-label">当前展开</span>
              <h3>{{ activePlaylist.name }}</h3>
              <p>{{ activePlaylist.description }}</p>
            </div>
            <button class="close-btn" @click="closePlaylistDetailModal">&times;</button>
          </div>
          <div class="playlist-detail-modal-meta">
            <span class="playlist-focus-meta">{{ formatPlaylistMeta(activePlaylist) }}</span>
          </div>
          <div v-if="playlistModalTracks.length" class="playlist-track-scroll-area">
            <div class="playlist-track-list">
              <article class="playlist-track-row" v-for="track in playlistModalTracks" :key="track.id">
                <div class="playlist-track-main">
                  <strong>{{ track.title }}</strong>
                  <span>{{ getTrackArtist(track) }}</span>
                </div>
                <div class="playlist-track-side">
                  <span>{{ formatDuration(track.duration) }}</span>
                  <button class="icon-btn" @click="musicStore.removeTrackFromPlaylist(activePlaylist.id, track.id)" title="移出歌单">❌</button>
                </div>
              </article>
            </div>
          </div>
          <div v-else class="empty-state compact-empty-state">
            歌单里还没有音乐哦。
          </div>
        </div>
      </div>

      <div v-if="showAddToPlaylistModal" class="modal-overlay" @click.self="showAddToPlaylistModal = false">
        <div class="modal-content glass-panel music-modal-card playlist-detail-modal-card">
          <div class="modal-header">
            <div class="playlist-detail-modal-title">
              <span class="playlist-focus-label">操作</span>
              <h3>加入歌单</h3>
            </div>
            <button class="close-btn" @click="showAddToPlaylistModal = false">&times;</button>
          </div>
          <div class="modal-body">
            <div v-if="musicStore.customPlaylists.length === 0" class="empty-state compact-empty-state">
              暂无歌单，请先创建歌单。
            </div>
            <div v-else class="playlist-track-scroll-area">
              <div class="playlist-track-list">
                <button
                  v-for="pl in musicStore.customPlaylists" 
                  :key="pl.id" 
                  class="playlist-track-row add-to-pl-row"
                  @click="confirmAddToPlaylist(pl.id)"
                >
                  <div class="playlist-track-main">
                    <strong>{{ pl.name }}</strong>
                    <span>{{ pl.description || '自建歌单' }}</span>
                  </div>
                  <div class="playlist-track-side">
                    <span class="add-icon">➕</span>
                  </div>
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div v-if="showSocialComposer" class="modal-overlay" @click.self="closeSocialComposer">
        <div class="modal-content glass-panel music-modal-card social-modal-card">
          <div class="modal-header">
            <h3>发布帖子</h3>
            <button class="close-btn" @click="closeSocialComposer">&times;</button>
          </div>
          <div class="modal-body social-create-form">
            <div class="form-group">
              <label>帖子内容</label>
              <textarea
                v-model="socialDraft.content"
                class="info-input info-textarea"
                rows="5"
                placeholder="记录一下此刻发生了什么，或者想和朋友分享什么..."
              />
            </div>
            <div class="form-group">
              <label>现在的情绪</label>
              <input
                v-model="socialDraft.mood"
                type="text"
                class="info-input"
                placeholder="例如：轻松 平静，或使用逗号分隔多个情绪"
              />
            </div>
            <div class="social-draft-preview">
              <span class="social-preview-label">预览标签</span>
              <div v-if="parsedSocialMoodTags.length" class="social-preview-tags">
                <span
                  v-for="tag in parsedSocialMoodTags"
                  :key="tag"
                  class="social-mood-pill"
                >
                  {{ tag }}
                </span>
              </div>
              <span v-else class="social-mood-pill">未填写情绪</span>
            </div>
            <div class="modal-actions">
              <button class="modal-btn secondary-btn" @click="closeSocialComposer">取消</button>
              <button
                class="modal-btn primary-btn"
                @click="submitSocialPost"
                :disabled="!socialDraft.content.trim() || parsedSocialMoodTags.length === 0"
              >
                发布动态
              </button>
            </div>
          </div>
        </div>
      </div>

      <div v-if="showMyPostsModal" class="modal-overlay" @click.self="closeMyPostsSpace">
        <div class="modal-content glass-panel music-modal-card my-posts-modal-card">
          <div class="modal-header">
            <div class="playlist-detail-modal-title">
              <span class="playlist-focus-label">MY POSTS</span>
              <h3>我发布的帖子</h3>
              <p>集中查看自己曾发布过的所有朋友圈动态。</p>
            </div>
            <button class="close-btn" @click="closeMyPostsSpace">&times;</button>
          </div>
          <div class="modal-body">
            <div v-if="isMyPostsLoading" class="empty-state compact-empty-state">
              正在加载我的帖子...
            </div>
            <div v-else-if="mySocialPosts.length" class="my-posts-scroll-area">
              <article v-for="post in mySocialPosts" :key="post.id" class="my-post-card">
                <div class="my-post-card-top">
                  <div class="my-post-card-head">
                    <div class="my-post-card-meta">
                      <span class="social-mood-pill">现在的情绪 · {{ post.mood }}</span>
                      <span class="social-post-time">{{ post.timeLabel }}</span>
                    </div>
                    <strong>{{ post.authorName }}</strong>
                  </div>
                  <button
                    class="modal-btn danger-btn my-post-delete-btn"
                    @click="deleteSocialPost(post, { fromMyPosts: true })"
                    :disabled="deletingPostId === post.id"
                  >
                    {{ deletingPostId === post.id ? '删除中...' : '删除帖子' }}
                  </button>
                </div>
                <p class="social-post-content">{{ post.content }}</p>
                <div v-if="post.highlights?.length" class="social-highlight-row">
                  <span v-for="item in post.highlights" :key="`${post.id}-${item}`" class="social-highlight-chip"># {{ item }}</span>
                </div>
                <div v-if="post.likeUsers.length || post.comments.length" class="social-feedback-card my-post-feedback-card">
                  <div v-if="post.likeUsers.length" class="social-like-line">
                    <span class="social-like-icon">♡</span>
                    <span>{{ formatSocialLikeNames(post.likeUsers) }}</span>
                  </div>
                  <div class="social-comment-list">
                    <div v-for="comment in post.comments" :key="`my-${comment.id}`" class="social-comment-item">
                      <div class="social-comment-main">
                        <span class="social-comment-author">{{ comment.author }}</span>
                        <span v-if="comment.replyToName" class="social-comment-reply-target">回复 {{ comment.replyToName }}</span>
                        <span class="social-comment-text">：{{ comment.content }}</span>
                        <span class="social-comment-time">{{ comment.timeLabel }}</span>
                      </div>
                      <div v-if="comment.likeUsers.length" class="social-comment-like-line">
                        <span class="social-like-icon">♡</span>
                        <span>{{ formatSocialLikeNames(comment.likeUsers) }}</span>
                      </div>
                    </div>
                  </div>
                </div>
              </article>
            </div>
            <div v-else class="empty-state compact-empty-state">
              你还没有发布过帖子，去朋友圈记录第一条动态吧！
            </div>
          </div>
        </div>
      </div>

    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, watch, nextTick } from 'vue'
import { loadEcharts } from '@/utils/loadEcharts'
import { useMusicStore } from '@/stores/musicStore'
import { ElMessage } from 'element-plus'
import {
  getCurrentUserApi,
  getUserSummariesApi,
  getCurrentUserFromStorage,
  getMyFeedbackApi,
  getMyFeedbackPreferencesApi,
  resolveUserAvatarUrl,
  saveCurrentUserToStorage,
  submitUserFeedbackApi,
  uploadMyAvatarApi,
  updateMyProfileApi,
} from '@/api/user'
import { getMusicFileUrl } from '@/api/python'
import { 
  getMyMoodDiariesApi, 
  createMoodDiaryApi, 
  updateMoodDiaryApi,
  deleteMoodDiaryApi,
  getPostsApi,
  getMyPostsApi,
  getPostInteractionsApi,
  getPostAiReplySuggestionApi,
  createPostApi,
  deletePostApi,
  likePostApi,
  commentPostApi,
  likeCommentApi,
  replyCommentApi,
  getMySocialNotificationsApi,
  getMyFriendsApi,
  sendFriendRequestApi,
  getReceivedFriendRequestsApi,
  handleFriendRequestApi,
  searchUsersApi,
  deleteFriendshipApi,
  getChatConversationsApi,
  getChatMessagesApi,
  sendChatMessageApi,
  markChatAsReadApi,
} from '@/api/social'
import { formatApiDateTime, parseApiDateTime } from '@/utils/dateTime'

import {
  appendEmotionSnapshotApi,
  appendUserBehaviorLogApi,
  getMyEmotionSnapshotsApi,
} from '@/api/data'

import { getMyMeditationLogsApi, getMyGardenApi } from '@/api/meditation'
import { readAudioTagMetadata } from '@/utils/audioMetadata'

const musicStore = useMusicStore()

// --- Profile State ---
const userProfile = ref({
  id: '',
  name: '未设置昵称',
  username: '',
  email: '',
  phone: '',
  description: '',
  avatar: ''
})
const isEditingProfile = ref(false)
const isProfileSubmitting = ref(false)
const avatarInputRef = ref(null)
const pendingAvatarFile = ref(null)
let avatarPreviewObjectUrl = ''

const mapUserToProfile = (user) => ({
  id: user?.userId ?? '',
  name: user?.nickname?.trim() || user?.username?.trim() || '未设置昵称',
  username: user?.username?.trim() || '',
  email: user?.email?.trim() || '',
  phone: user?.phone?.trim() || '',
  description: user?.bio?.trim() || '',
  avatar: resolveUserAvatarUrl(user?.avatarUrl)
})

const resetPendingAvatarUpload = () => {
  pendingAvatarFile.value = null
  if (avatarPreviewObjectUrl) {
    URL.revokeObjectURL(avatarPreviewObjectUrl)
    avatarPreviewObjectUrl = ''
  }
  if (avatarInputRef.value) {
    avatarInputRef.value.value = ''
  }
}

const loadCurrentUserProfile = async () => {
  const cachedUser = getCurrentUserFromStorage()
  if (cachedUser) {
    userProfile.value = mapUserToProfile(cachedUser)
  }

  try {
    const response = await getCurrentUserApi()
    const user = response.data
    saveCurrentUserToStorage(user)
    userProfile.value = mapUserToProfile(user)
  } catch (error) {
    console.error('读取当前用户信息失败', error)
    if (!cachedUser) {
      ElMessage.error('读取当前用户信息失败')
    }
  }
}

const triggerAvatarUpload = () => {
  avatarInputRef.value?.click()
}

const handleAvatarUpload = (event) => {
  const file = event.target.files[0]
  if (!file) return

  if (!file.type.startsWith('image/')) {
    ElMessage.error('请选择图片文件作为头像')
    event.target.value = ''
    return
  }

  if (file.size > 5 * 1024 * 1024) {
    ElMessage.error('头像图片不能超过 5MB')
    event.target.value = ''
    return
  }

  resetPendingAvatarUpload()
  pendingAvatarFile.value = file
  avatarPreviewObjectUrl = URL.createObjectURL(file)
  userProfile.value.avatar = avatarPreviewObjectUrl
}

const handleProfileAction = async () => {
  if (!isEditingProfile.value) {
    isEditingProfile.value = true
    return
  }

  isProfileSubmitting.value = true
  try {
    let avatarUrl = userProfile.value.avatar || null

    if (pendingAvatarFile.value) {
      const formData = new FormData()
      formData.append('file', pendingAvatarFile.value)
      const uploadResponse = await uploadMyAvatarApi(formData)
      avatarUrl = resolveUserAvatarUrl(uploadResponse.data?.avatarUrl)
      userProfile.value.avatar = avatarUrl
    }

    await updateMyProfileApi({
      nickname: userProfile.value.name.trim(),
      email: userProfile.value.email.trim() || null,
      phone: userProfile.value.phone.trim() || null,
      bio: userProfile.value.description.trim() || null,
    })

    await loadCurrentUserProfile()
    resetPendingAvatarUpload()
    isEditingProfile.value = false
    ElMessage.success('个人资料已更新')
  } catch (error) {
    const message = typeof error?.response?.data === 'string'
      ? error.response.data
      : '保存个人资料失败'
    ElMessage.error(message)
  } finally {
    isProfileSubmitting.value = false
  }
}

// --- Tabs Configuration ---
const tabs = [
  { id: 'profile', name: '我的信息', icon: 'id-card' },
  { id: 'diary', name: '情绪日记', icon: 'book' },
  { id: 'data', name: '情绪数据', icon: 'chart-line' },
  { id: 'feedback', name: '服务反馈', icon: 'star' },
  { id: 'meditation', name: '冥想数据', icon: 'leaf' },
  { id: 'music', name: '音乐库', icon: 'music' },
  { id: 'social', name: '朋友圈', icon: 'users' },
  { id: 'friends', name: '好友', icon: 'user-friends' },
  { id: 'chat', name: '聊天室', icon: 'users' }
]
const currentTab = ref('profile')
const currentMonth = ref(new Date().getMonth() + 1)

const TAB_CACHE_MS = 60 * 1000
const tabLoadedAt = ref({})
const musicTabReady = ref(false)
let echartsLib = null

const isTabCacheFresh = (tab) => {
  const loadedAt = tabLoadedAt.value[tab]
  return Boolean(loadedAt && Date.now() - loadedAt < TAB_CACHE_MS)
}

const markTabLoaded = (tab) => {
  tabLoadedAt.value = { ...tabLoadedAt.value, [tab]: Date.now() }
}

const invalidateTabCache = (tab) => {
  const next = { ...tabLoadedAt.value }
  delete next[tab]
  tabLoadedAt.value = next
}

const recordEmotionSnapshotsFromDiary = async (emotions) => {
  const list = Array.isArray(emotions) ? emotions : []
  if (!list.length) return
  await Promise.all(
    list.map((emotion) => {
      const cfg = emotionConfig[emotion]
      if (!cfg) return Promise.resolve()
      return appendEmotionSnapshotApi({
        source: 'diary',
        emotion,
        score: cfg.score,
      }).catch((e) => console.warn('append emotion snapshot failed', e))
    }),
  )
}

const getEcharts = async () => {
  if (!echartsLib) {
    echartsLib = await loadEcharts()
  }
  return echartsLib
}

const ensureMusicTabData = async () => {
  if (musicTabReady.value) return
  await Promise.all([
    musicStore.fetchUserData(),
    musicStore.fetchEmotionTags(),
    musicStore.fetchPublicTracks(),
  ])
  musicTabReady.value = true
}

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
const todayDateStr = computed(() => {
  const today = new Date()
  return `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`
})
const isSelectedDateToday = computed(() => selectedDateStr.value === todayDateStr.value)

// Mock Diaries
const mockDiaries = ref({})

const fetchDiaries = async () => {
  try {
    const res = await getMyMoodDiariesApi()
    const diariesMap = {}
    if (res.data && Array.isArray(res.data)) {
      res.data.forEach(diary => {
        diariesMap[diary.date] = {
          id: diary.id,
          emotions: diary.dominantEmotion ? diary.dominantEmotion.split(',') : [],
          content: diary.context
        }
      })
    }
    mockDiaries.value = diariesMap
  } catch (e) {
    console.error('Failed to fetch diaries:', e)
  }
}

const hasDiary = (dateStr) => !!mockDiaries.value[dateStr]
const selectedDiary = computed(() => mockDiaries.value[selectedDateStr.value])

const newDiary = ref({ emotions: ['Happy'], content: '' })

const resetDiaryForm = () => {
  newDiary.value = { emotions: ['Happy'], content: '' }
}

const syncDiaryFormFromSelected = () => {
  if (!isSelectedDateToday.value) return
  const diary = selectedDiary.value
  if (!diary) {
    resetDiaryForm()
    return
  }
  const emotions = Array.isArray(diary.emotions) && diary.emotions.length ? [...diary.emotions] : ['Happy']
  newDiary.value = { emotions, content: diary.content || '' }
}

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

const saveDiary = async () => {
  if (!isSelectedDateToday.value) {
    ElMessage.warning('只能编写今天的日记')
    return
  }
  if (!newDiary.value.content.trim()) {
    ElMessage.warning('请先填写日记内容')
    return
  }
  try {
    const payload = {
      date: selectedDateStr.value,
      dominantEmotion: newDiary.value.emotions.join(','),
      context: newDiary.value.content
    }
    const existingDiaryId = selectedDiary.value?.id
    let targetId = 0
    let actionType = 'save_diary'
    if (existingDiaryId) {
      await updateMoodDiaryApi(existingDiaryId, payload)
      targetId = existingDiaryId
      actionType = 'update_diary'
      ElMessage.success('日记已更新')
    } else {
      const res = await createMoodDiaryApi(payload)
      targetId = res.data?.id || 0
      ElMessage.success('日记保存成功')
    }
    await fetchDiaries()
    initDiaryWeeklyChart()
    syncDiaryFormFromSelected()

    await recordEmotionSnapshotsFromDiary(newDiary.value.emotions)
    invalidateTabCache('data')
    if (currentTab.value === 'data') {
      await fetchEmotionData()
    }

    appendUserBehaviorLogApi({
      actionType,
      targetType: 'diary',
      targetId: targetId,
      metadata: { emotions: payload.dominantEmotion }
    }).catch(e => console.warn('Log user behavior failed', e))
    
  } catch (e) {
    if (e.response?.status === 403) {
      ElMessage.error('历史日记不可修改')
    } else if (e.response?.status === 409) {
      ElMessage.error('今日日记已存在，请直接保存修改')
      syncDiaryFormFromSelected()
    } else if (e.response?.status === 404) {
      ElMessage.error('日记不存在或已被删除')
    } else {
      ElMessage.error('保存失败')
    }
  }
}

const deleteDiary = async () => {
  const diary = selectedDiary.value
  if (!diary?.id) return
  const confirmed = window.confirm('确定要删除这篇日记吗？删除后不可恢复。')
  if (!confirmed) return
  try {
    await deleteMoodDiaryApi(diary.id)
    ElMessage.success('日记已删除')
    await fetchDiaries()
    initDiaryWeeklyChart()
    if (isSelectedDateToday.value) {
      resetDiaryForm()
    }
    appendUserBehaviorLogApi({
      actionType: 'delete_diary',
      targetType: 'diary',
      targetId: diary.id,
      metadata: { date: selectedDateStr.value }
    }).catch(e => console.warn('Log user behavior failed', e))
  } catch (e) {
    if (e.response?.status === 404) {
      ElMessage.error('日记不存在或已被删除')
    } else {
      ElMessage.error('删除失败')
    }
  }
}

watch([selectedDateStr, mockDiaries], () => {
  syncDiaryFormFromSelected()
})

// --- Tab 2: Data State & Charts ---
const trendChartRef = ref(null)
const distributionChartRef = ref(null)
const heatmapChartRef = ref(null)

const emotionSnapshots = ref([])
const trendData = ref([0, 0, 0, 0, 0, 0, 0])
const distributionData = ref([])
const heatmapDataList = ref([])
const isEmotionDataLoading = ref(false)

const buildTrendChartOption = () => ({
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
    data: trendData.value,
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

const buildDistributionChartOption = () => ({
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
    data: distributionData.value.length ? distributionData.value : [{ value: 1, name: '暂无数据', itemStyle: { color: '#ccc' } }]
  }]
})

const buildHeatmapChartOption = () => {
  const year = new Date().getFullYear()
  const month = currentMonth.value
  const daysInMonth = new Date(year, month, 0).getDate()
  const firstDay = new Date(year, month - 1, 1).getDay()
  const firstDayIndex = firstDay === 0 ? 6 : firstDay - 1

  const xData = ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
  const requiredWeeks = Math.ceil((firstDayIndex + daysInMonth) / 7)
  const yData = ['Week 1', 'Week 2', 'Week 3', 'Week 4', 'Week 5', 'Week 6'].slice(0, requiredWeeks)

  return {
    tooltip: {
      position: 'top',
      formatter: function(params) {
        return `${month}月${params.value[3]}日<br/>情绪评分: ${params.value[2]}`
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
      dimension: 2,
      inRange: {
        color: ['#8B0000', '#DC143C', '#FFB6C1', '#f5f5f5', '#90EE90', '#228B22', '#006400']
      }
    },
    series: [{
      type: 'heatmap',
      data: heatmapDataList.value,
      label: {
        show: true,
        formatter: function(params) {
          return params.value[3]
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
  }
}

const syncDataCharts = () => {
  if (trendChart) {
    trendChart.setOption(buildTrendChartOption(), true, true)
  }
  if (distributionChart) {
    distributionChart.setOption(buildDistributionChartOption(), true, true)
  }
  if (heatmapChart) {
    heatmapChart.setOption(buildHeatmapChartOption(), true, true)
  }
}

const updateDataChartLoading = (loading) => {
  const charts = [trendChart, distributionChart, heatmapChart].filter(Boolean)
  charts.forEach((chart) => {
    if (loading) {
      chart.showLoading('default', {
        text: '加载中...',
        color: '#86aa8a',
        textColor: '#6c7f78',
        maskColor: 'rgba(255,255,255,0.45)'
      })
    } else {
      chart.hideLoading()
    }
  })
}

const fetchEmotionData = async () => {
  isEmotionDataLoading.value = true
  updateDataChartLoading(true)
  try {
    const res = await getMyEmotionSnapshotsApi({ size: 50 })
    if (res.data && Array.isArray(res.data)) {
      emotionSnapshots.value = res.data
      
      const now = new Date()
      const tData = [0, 0, 0, 0, 0, 0, 0]
      const tCount = [0, 0, 0, 0, 0, 0, 0]
      const distMap = {}
      
      res.data.forEach(snap => {
        const snapDate = new Date(snap.createdAt)
        const diffTime = Math.abs(now - snapDate)
        const diffDays = Math.floor(diffTime / (1000 * 60 * 60 * 24))
        
        // Trend
        if (diffDays < 7) {
          const index = 6 - diffDays
          tData[index] += snap.score || 0
          tCount[index] += 1
        }
        
        // Distribution
        const eName = snap.emotion
        if (eName) {
          distMap[eName] = (distMap[eName] || 0) + 1
        }
      })
      
      // Average trend
      for (let i = 0; i < 7; i++) {
        if (tCount[i] > 0) {
          tData[i] = parseFloat((tData[i] / tCount[i]).toFixed(1))
        }
      }
      trendData.value = tData
      
      // Calculate weekly average for the comment
      let sum = 0, count = 0
      tData.forEach(s => {
        if (s !== 0 || tCount[tData.indexOf(s)] > 0) {
          sum += s
          count++
        }
      })
      weeklyAverageScore.value = count > 0 ? parseFloat((sum / count).toFixed(1)) : 0
      
      // Distribution Data
      distributionData.value = Object.keys(distMap).map(k => {
        // Map backend emotion to frontend name if possible
        const mappedName = emotionConfig[k] ? emotionConfig[k].name : k
        const color = emotionConfig[k] ? emotionConfig[k].color : '#999'
        return {
          value: distMap[k],
          name: mappedName,
          itemStyle: { color }
        }
      })
      
      // Heatmap Data
      const hData = []
      const year = new Date().getFullYear();
      const month = currentMonth.value; 
      const firstDay = new Date(year, month - 1, 1).getDay();
      const firstDayIndex = firstDay === 0 ? 6 : firstDay - 1;
      
      const snapMapByDay = {}
      res.data.forEach(snap => {
        const d = new Date(snap.createdAt)
        if (d.getFullYear() === year && (d.getMonth() + 1) === month) {
          const day = d.getDate()
          if (!snapMapByDay[day]) snapMapByDay[day] = []
          snapMapByDay[day].push(snap.score || 0)
        }
      })
      
      const daysInMonth = new Date(year, month, 0).getDate();
      for(let i=1; i<=daysInMonth; i++) {
        const currentPos = firstDayIndex + (i - 1);
        const x = currentPos % 7;
        const y = Math.floor(currentPos / 7);
        
        let score = 0
        if (snapMapByDay[i]) {
          score = snapMapByDay[i].reduce((a,b) => a+b, 0) / snapMapByDay[i].length
        }
        hData.push([x, y, Math.round(score), i]); 
      }
      heatmapDataList.value = hData
      syncDataCharts()
    }
  } catch (e) {
    console.error('Failed to fetch emotion data:', e)
    emotionSnapshots.value = []
    trendData.value = [0, 0, 0, 0, 0, 0, 0]
    distributionData.value = []
    heatmapDataList.value = []
    weeklyAverageScore.value = 0
    syncDataCharts()
    if (e?.code === 'ECONNABORTED' || String(e?.message || '').includes('timeout')) {
      ElMessage.error('情绪数据服务请求超时，请确认 data-service 与 MongoDB 已启动')
    } else {
      ElMessage.error('获取情绪数据失败，请稍后重试')
    }
  } finally {
    isEmotionDataLoading.value = false
    updateDataChartLoading(false)
  }
}

const weeklyAverageScore = ref(0)
const weeklyComment = computed(() => {
  const score = weeklyAverageScore.value
  if (score > 10) return '本周你的情绪非常积极，继续保持这份好心情！'
  if (score > 0) return '本周情绪比较平稳，偶尔有小波澜，整体状态不错。'
  if (score > -10) return '本周情绪略显低落，建议多做一些让自己放松的事情，比如听音乐或冥想。'
  return '本周似乎经历了一些困难，情绪较为负面。请记得深呼吸，必要时可以寻求朋友的倾听或专业的帮助。'
})

const feedbackServiceOptions = [
  { value: '情绪数据', label: '情绪数据' },
  { value: '冥想服务', label: '冥想服务' },
  { value: '音乐服务', label: '音乐服务' },
  { value: '朋友圈', label: '朋友圈' },
  { value: '聊天室', label: '聊天室' },
  { value: 'AI陪伴', label: 'AI陪伴' },
]
const feedbackRatingOptions = [1, 2, 3, 4, 5]
const feedbackForm = ref({
  service: feedbackServiceOptions[0].value,
  rating: 4,
  feedback: '',
})
const isFeedbackSubmitting = ref(false)
const isFeedbackLoading = ref(false)
const myFeedbackList = ref([])
const feedbackPreferences = ref([])

const formatFeedbackTime = (value) => {
  const parsed = parseApiDateTime(value)
  if (Number.isNaN(parsed.getTime())) {
    return '刚刚'
  }
  return formatApiDateTime(parsed, {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  }, '刚刚')
}

const mapFeedbackItem = (item, index) => {
  const rating = Number(item?.rating) || 0
  return {
    id: `${item?.createdAt || 'feedback'}-${index}`,
    service: item?.service || '未分类',
    feedback: item?.feedback || '未填写反馈内容',
    rating,
    ratingLabel: `${rating.toFixed(1)} / 5`,
    timeLabel: formatFeedbackTime(item?.createdAt),
  }
}

const feedbackPreferenceCards = computed(() => (
  feedbackPreferences.value
    .map((item) => {
      const score = Number(item?.score) || 0
      return {
        service: item?.service || '未分类',
        score,
        scoreText: `${score.toFixed(1)} / 5`,
        percent: Math.max(0, Math.min(100, (score / 5) * 100)),
      }
    })
    .sort((a, b) => b.score - a.score)
))

const resetFeedbackForm = () => {
  feedbackForm.value = {
    service: feedbackServiceOptions[0].value,
    rating: 4,
    feedback: '',
  }
}

const fetchFeedbackCenterData = async () => {
  isFeedbackLoading.value = true
  try {
    const [feedbackRes, preferencesRes] = await Promise.all([
      getMyFeedbackApi(),
      getMyFeedbackPreferencesApi(),
    ])
    myFeedbackList.value = (feedbackRes.data || [])
      .map(mapFeedbackItem)
      .reverse()
    feedbackPreferences.value = preferencesRes.data?.serviceScores || []
  } catch (error) {
    console.error('加载反馈中心失败:', error)
    myFeedbackList.value = []
    feedbackPreferences.value = []
    if (error?.response?.status === 403) {
      ElMessage.error('当前账号暂时无权读取反馈数据，请重新登录后重试')
    } else {
      ElMessage.error('获取反馈数据失败，请确认 user-service 已启动')
    }
  } finally {
    isFeedbackLoading.value = false
  }
}

const submitFeedback = async () => {
  const payload = {
    service: String(feedbackForm.value.service || '').trim(),
    rating: Number(feedbackForm.value.rating) || 0,
    feedback: String(feedbackForm.value.feedback || '').trim(),
  }
  if (!payload.service || !payload.feedback) {
    ElMessage.warning('请完整填写反馈内容')
    return
  }

  isFeedbackSubmitting.value = true
  try {
    await submitUserFeedbackApi(payload)
    ElMessage.success('反馈提交成功')
    resetFeedbackForm()
    await fetchFeedbackCenterData()
    appendUserBehaviorLogApi({
      actionType: 'submit_feedback',
      targetType: 'service_feedback',
      targetId: 0,
      metadata: {
        service: payload.service,
        rating: payload.rating,
      }
    }).catch((error) => console.warn('Log user behavior failed', error))
  } catch (error) {
    console.error('提交反馈失败:', error)
    ElMessage.error('反馈提交失败，请稍后重试')
  } finally {
    isFeedbackSubmitting.value = false
  }
}

// --- Meditation Data Logic ---
const weeklyMeditationData = ref([0, 0, 0, 0, 0, 0, 0])
const meditationLogs = ref([])
const MEDITATION_DAY_TARGET_MINUTES = 20

const formatLocalDateKey = (date) => {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

const toLocalDateStart = (value) => {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return null
  }
  date.setHours(0, 0, 0, 0)
  return date
}

const getWeekDatesMondayStart = (baseDate = new Date()) => {
  const centerDate = new Date(baseDate)
  centerDate.setHours(0, 0, 0, 0)

  const startDate = new Date(centerDate)
  const dayOfWeek = startDate.getDay()
  const offset = dayOfWeek === 0 ? -6 : 1 - dayOfWeek
  startDate.setDate(startDate.getDate() + offset)

  return Array.from({ length: 7 }, (_, index) => {
    const date = new Date(startDate)
    date.setDate(startDate.getDate() + index)
    return date
  })
}

const buildMeditationDurationMap = (logs = []) => {
  const durationMap = {}
  logs.forEach((log) => {
    const logDate = toLocalDateStart(log.startTime)
    if (!logDate) return
    const dateKey = formatLocalDateKey(logDate)
    durationMap[dateKey] = (durationMap[dateKey] || 0) + (Number(log.duration) || 0)
  })
  return durationMap
}

const fetchMeditationLogs = async () => {
  try {
    const res = await getMyMeditationLogsApi()
    if (res.data && Array.isArray(res.data)) {
      meditationLogs.value = res.data
      const durationMap = buildMeditationDurationMap(res.data)
      const currentWeekDates = getWeekDatesMondayStart(new Date())
      const data = currentWeekDates.map((date) => {
        const dateKey = formatLocalDateKey(date)
        return durationMap[dateKey] || 0
      })
      weeklyMeditationData.value = data
    } else {
      meditationLogs.value = []
      weeklyMeditationData.value = [0, 0, 0, 0, 0, 0, 0]
    }
  } catch (e) {
    console.error('Failed to fetch meditation logs:', e)
    meditationLogs.value = []
    weeklyMeditationData.value = [0, 0, 0, 0, 0, 0, 0]
  }
}

const weeklyAverageMeditation = computed(() => {
  const sum = weeklyMeditationData.value.reduce((a, b) => a + b, 0)
  return (sum / weeklyMeditationData.value.length).toFixed(1)
})

const getMeditationYAxisConfig = () => {
  const maxDuration = Math.max(...weeklyMeditationData.value, 0)
  const suggestedMax = Math.max(maxDuration, MEDITATION_DAY_TARGET_MINUTES)

  if (suggestedMax <= 10) {
    return { max: 10, interval: 2 }
  }
  if (suggestedMax <= 30) {
    return { max: Math.ceil(suggestedMax / 5) * 5, interval: 5 }
  }
  if (suggestedMax <= 60) {
    return { max: Math.ceil(suggestedMax / 10) * 10, interval: 10 }
  }

  const interval = suggestedMax <= 120 ? 15 : 30
  return {
    max: Math.ceil(suggestedMax / interval) * interval,
    interval,
  }
}

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

const getWeekDatesForCalendar = (baseDate) => {
  const dates = []
  const centerDate = new Date(baseDate)
  centerDate.setHours(0, 0, 0, 0)

  const startDate = new Date(centerDate)
  startDate.setDate(centerDate.getDate() - centerDate.getDay())

  for (let offset = 0; offset < 7; offset++) {
    const date = new Date(startDate)
    date.setDate(startDate.getDate() + offset)
    dates.push(date)
  }

  return dates
}

const parseDiaryEmotions = (diary) => {
  if (!diary?.emotions || !Array.isArray(diary.emotions)) {
    return []
  }
  return diary.emotions.filter((emotion) => emotionConfig[emotion])
}

const initDiaryWeeklyChart = async () => {
  if (currentTab.value !== 'diary') return
  const echarts = await getEcharts()
  nextTick(() => {
    if (diaryWeeklyChartRef.value) {
      if (diaryWeeklyChart) diaryWeeklyChart.dispose()
      diaryWeeklyChart = echarts.init(diaryWeeklyChartRef.value)
      
      const days = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
      const emotionKeys = ['Angry', 'Calm', 'Disgust', 'Fear', 'Happy', 'Neutral', 'Sad', 'Surprise']
      const currentWeekDates = getWeekDatesForCalendar(currentDate.value)
      const chartData = []

      currentWeekDates.forEach((date, index) => {
        const dateStr = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
        const diary = mockDiaries.value[dateStr]
        const emotions = parseDiaryEmotions(diary)

        emotions.forEach((emotion) => {
          const emotionIndex = emotionKeys.indexOf(emotion)
          if (emotionIndex !== -1) {
            chartData.push([index, emotionIndex])
          }
        })
      })
      
      diaryWeeklyChart.setOption({
        tooltip: {
          formatter: function (params) {
            const xIndex = params.value[0]
            const emotionKey = emotionKeys[params.value[1]]
            const date = currentWeekDates[xIndex]
            const dateLabel = `${date.getMonth() + 1}月${date.getDate()}日`
            return `${days[xIndex]} ${dateLabel}<br/>${emotionConfig[emotionKey].name}`
          }
        },
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
        }],
        graphic: chartData.length === 0 ? [{
          type: 'text',
          left: 'center',
          top: 'middle',
          style: {
            text: '本周暂无情绪日记数据',
            fill: '#999',
            font: '14px sans-serif'
          }
        }] : []
      })
    }
  })
}

const initCharts = async () => {
  if (currentTab.value !== 'data') return
  const echarts = await getEcharts()

  nextTick(() => {
    // 1. Trend Chart
    if (trendChartRef.value) {
      if (!trendChart) {
        trendChart = echarts.init(trendChartRef.value)
      }
    }

    // 2. Distribution Chart
    if (distributionChartRef.value) {
      if (!distributionChart) {
        distributionChart = echarts.init(distributionChartRef.value)
      }
    }

    // 3. Heatmap Chart
    if (heatmapChartRef.value) {
      if (!heatmapChart) {
        heatmapChart = echarts.init(heatmapChartRef.value)
      }
    }

    syncDataCharts()
    if (isEmotionDataLoading.value) {
      updateDataChartLoading(true)
    }
  })
}

watch(currentTab, (newTab) => {
  if (newTab !== 'friends') {
    selectedFriend.value = null
  }
  if (newTab !== 'chat') {
    stopChatPolling()
  }
  loadTabData(newTab)
})

const loadTabData = async (tab, { force = false } = {}) => {
  if (tab === 'diary') {
    if (!force && isTabCacheFresh('diary')) return
    await fetchDiaries()
    await initDiaryWeeklyChart()
    markTabLoaded('diary')
    return
  }
  if (tab === 'data') {
    if (!force && isTabCacheFresh('data')) return
    await initCharts()
    await fetchEmotionData()
    markTabLoaded('data')
    return
  }
  if (tab === 'meditation') {
    if (!force && isTabCacheFresh('meditation')) return
    await fetchMeditationLogs()
    await initMeditationCharts()
    await fetchGardenData()
    markTabLoaded('meditation')
    return
  }
  if (tab === 'music') {
    await ensureMusicTabData()
    return
  }
  if (tab === 'social') {
    if (!force && isTabCacheFresh('social')) return
    await fetchSocialPosts()
    if (!force && isTabCacheFresh('social-notifications')) {
      markTabLoaded('social')
      return
    }
    await fetchSocialNotifications()
    markTabLoaded('social')
    markTabLoaded('social-notifications')
    return
  }
  if (tab === 'friends') {
    if (!force && isTabCacheFresh('friends')) return
    await fetchFriends()
    await fetchFriendRequests()
    markTabLoaded('friends')
    return
  }
  if (tab === 'chat') {
    await loadChatTabData()
    startChatPolling()
  }
}

watch(currentDate, () => {
  if (currentTab.value === 'diary') {
    void initDiaryWeeklyChart()
  }
})

onMounted(() => {
  loadCurrentUserProfile()
  Promise.all([
    musicStore.fetchUserData(),
    musicStore.fetchEmotionTags(),
    musicStore.fetchPublicTracks(),
  ])
  if (currentTab.value === 'diary') {
    fetchDiaries().then(() => {
      initDiaryWeeklyChart()
    })
  } else if (currentTab.value === 'data') {
    initCharts()
    fetchEmotionData()
  } else if (currentTab.value === 'feedback') {
    fetchFeedbackCenterData()
  } else if (currentTab.value === 'meditation') {
    fetchMeditationLogs().then(() => {
      initMeditationCharts()
    })
    fetchGardenData()
  } else if (currentTab.value === 'social') {
    fetchSocialPosts()
    fetchSocialNotifications()
  } else if (currentTab.value === 'friends') {
    fetchFriends()
    fetchFriendRequests()
  } else if (currentTab.value === 'chat') {
    loadChatTabData()
    startChatPolling()
  }
})

// --- Tab 4: Music State ---
const activeMusicModal = ref('')
const showAddToPlaylistModal = ref(false)
const showPlaylistDetailModal = ref(false)
const isUploadingTrack = ref(false)
const isDetectingUploadTags = ref(false)
const previewingUploadedTrackId = ref('')
const newPlaylistName = ref('')
const newPlaylistDescription = ref('')
const trackToAdd = ref(null)
const activePlaylist = ref(null)
const audioInputRef = ref(null)
const playlistCarouselRef = ref(null)
const uploadedPreviewAudio = typeof Audio === 'undefined' ? null : new Audio()
const MAX_AI_PREVIEW_FILE_BYTES = 20 * 1024 * 1024

const uploadForm = ref({
  file: null,
  title: '',
  artist: '',
  duration: 0,
  coverUrl: '',
  tagInput: '',
  tags: [],
  aiCaption: '',
})

const realMusicLibrary = computed(() => {
  return musicStore.publicTracks
})

const likedTracksList = computed(() => {
  return musicStore.likedTrackIds.map(id => {
    return musicStore.resolveTrackById(id) || realMusicLibrary.value.find(t => t.id === id)
  }).filter(Boolean)
})

const collectedTracksList = computed(() => {
  return musicStore.collectedTrackIds.map(id => {
    return musicStore.resolveTrackById(id) || realMusicLibrary.value.find(t => t.id === id)
  }).filter(Boolean)
})

const blockedTracksList = computed(() => {
  return musicStore.blockedTrackIds.map(id => {
    return musicStore.resolveTrackById(id) || realMusicLibrary.value.find(t => t.id === id)
  }).filter(Boolean)
})

const playlistModalTracks = computed(() => {
  const ids = activePlaylist.value?.trackIds || []
  return ids.map(id => {
    return musicStore.resolveTrackById(id) || realMusicLibrary.value.find(t => t.id === id)
  }).filter(Boolean)
})

const getTrackTags = (track) => {
  if (track.tags?.length) return track.tags.slice(0, 2)
  return ['舒缓', '纯音乐']
}

const getTrackArtist = (track) => {
  return track?.artist?.trim() || '佚名'
}

const formatDuration = (seconds) => {
  if (!seconds) return '0:00'
  const m = Math.floor(seconds / 60)
  const s = Math.floor(seconds % 60)
  return `${m}:${s.toString().padStart(2, '0')}`
}

const formatPlaylistMeta = (playlist) => {
  const ids = playlist?.trackIds || []
  const tracks = ids.map(id => {
    return musicStore.resolveTrackById(id) || realMusicLibrary.value.find(t => t.id === id)
  }).filter(Boolean)
  const totalSeconds = tracks.reduce((sum, track) => sum + (track.duration || 0), 0)
  const totalMinutes = Math.max(1, Math.round(totalSeconds / 60))
  return `${tracks.length} 首 · ${totalMinutes} 分钟`
}

const handleAudioSelect = async (event) => {
  const file = event.target.files[0]
  if (file) {
    uploadForm.value.file = file
    uploadForm.value.tags = []
    uploadForm.value.aiCaption = ''
    uploadForm.value.coverUrl = ''
    const fallbackTitle = file.name.replace(/\.[^/.]+$/, '')
    if (!uploadForm.value.title) {
      uploadForm.value.title = fallbackTitle
    }
    const audio = document.createElement('audio')
    const objectUrl = URL.createObjectURL(file)
    audio.src = objectUrl
    audio.onloadedmetadata = () => {
      uploadForm.value.duration = audio.duration
      URL.revokeObjectURL(objectUrl)
    }
    audio.onerror = () => {
      URL.revokeObjectURL(objectUrl)
    }

    const metadata = await readAudioTagMetadata(file)
    if (metadata.title && (!uploadForm.value.title || uploadForm.value.title === fallbackTitle)) {
      uploadForm.value.title = metadata.title
    }
    if (metadata.artist && !uploadForm.value.artist) {
      uploadForm.value.artist = metadata.artist
    }
    uploadForm.value.coverUrl = metadata.coverUrl || ''
  }
}

const triggerAudioSelect = () => {
  audioInputRef.value?.click()
}

const stopUploadedTrackPreview = () => {
  if (!uploadedPreviewAudio) return
  uploadedPreviewAudio.pause()
  uploadedPreviewAudio.currentTime = 0
  previewingUploadedTrackId.value = ''
}

const toggleUploadedTrackPreview = async (track) => {
  if (!uploadedPreviewAudio || (!track?.fileUrl && !track?.filename)) {
    ElMessage.warning('当前音乐暂不支持试听')
    return
  }

  if (previewingUploadedTrackId.value === track.id && !uploadedPreviewAudio.paused) {
    uploadedPreviewAudio.pause()
    previewingUploadedTrackId.value = ''
    return
  }

  try {
    uploadedPreviewAudio.pause()
    uploadedPreviewAudio.src = track.fileUrl || getMusicFileUrl(track.filename)
    uploadedPreviewAudio.currentTime = 0
    await uploadedPreviewAudio.play()
    previewingUploadedTrackId.value = track.id
  } catch (error) {
    previewingUploadedTrackId.value = ''
    ElMessage.error('试听失败，请确认音乐服务已启动')
    console.error('Preview uploaded track failed:', error)
  }
}

const openUploadModal = () => {
  activeMusicModal.value = 'upload'
}

const openCreatePlaylistModal = () => {
  activeMusicModal.value = 'playlist'
}

const resetUploadForm = () => {
  uploadForm.value = {
    file: null,
    title: '',
    artist: '',
    duration: 0,
    coverUrl: '',
    tagInput: '',
    tags: [],
    aiCaption: '',
  }
  if (audioInputRef.value) {
    audioInputRef.value.value = ''
  }
}

const closeMusicModal = () => {
  if (activeMusicModal.value === 'upload') {
    resetUploadForm()
  }
  if (activeMusicModal.value === 'playlist') {
    newPlaylistName.value = ''
    newPlaylistDescription.value = ''
  }
  activeMusicModal.value = ''
}

const addTag = () => {
  const tag = uploadForm.value.tagInput.trim()
  if (tag && !uploadForm.value.tags.includes(tag)) {
    uploadForm.value.tags.push(tag)
  }
  uploadForm.value.tagInput = ''
}

const autoDetectTags = async () => {
  if (!uploadForm.value.file) {
    ElMessage.warning('请先选择要上传的 MP3 文件')
    return
  }
  if (uploadForm.value.file.size > MAX_AI_PREVIEW_FILE_BYTES) {
    ElMessage.warning('当前文件超过 20MB，暂不支持上传前 AI 识别，请手动添加标签或压缩后重试')
    return
  }

  try {
    isDetectingUploadTags.value = true
    const result = await musicStore.previewTrackEmotionTags(uploadForm.value.file, {
      maxTags: 3,
      title: uploadForm.value.title,
      artist: uploadForm.value.artist,
    })
    uploadForm.value.tags = result.tagNames
    uploadForm.value.aiCaption = result.caption
    if (result.tagNames.length) {
      ElMessage.success('AI 已完成预识别，请确认标签后再上传')
    } else {
      ElMessage.warning('AI 未识别出明确标签，请手动补充后再上传')
    }
  } catch (error) {
    console.error('Preview upload tag detection failed:', error)
    const status = error?.response?.status
    if (status === 413) {
      ElMessage.error('当前音频文件过大，AI 预识别暂不支持，请压缩文件或先手动添加标签')
    } else if (status === 503) {
      ElMessage.error('AI 识别服务暂不可用，当前环境可能未配置 DASHSCOPE_API_KEY')
    } else if (status === 504 || error?.code === 'ECONNABORTED') {
      ElMessage.error('AI 识别超时，请稍后重试。当前音频会先上传到网关和 music-service，再调用 DashScope，处理时间可能超过 60 秒。')
    } else if (!error?.response) {
      ElMessage.error('AI 识别请求在返回响应前被中断，请检查 api-gateway、music-service 是否在线，或稍后重试。')
    } else {
      ElMessage.error(error?.response?.data?.message || error?.response?.data?.error || 'AI 识别失败，请稍后重试')
    }
  } finally {
    isDetectingUploadTags.value = false
  }
}

const submitUpload = async () => {
  if (!uploadForm.value.file || !uploadForm.value.title) return
  if (!uploadForm.value.tags.length) {
    ElMessage.warning('请先识别或手动确认至少一个情绪标签')
    return
  }
  
  try {
    isUploadingTrack.value = true
    await musicStore.uploadTrack({
      file: uploadForm.value.file,
      title: uploadForm.value.title.trim(),
      artist: uploadForm.value.artist.trim(),
      duration: uploadForm.value.duration || 0,
      coverUrl: uploadForm.value.coverUrl || '',
      tags: [...uploadForm.value.tags],
    })
    closeMusicModal()
    ElMessage.success('音乐上传成功')
  } catch (error) {
    ElMessage.error(error?.response?.data?.error || '音乐上传失败')
  } finally {
    isUploadingTrack.value = false
  }
}

const deleteUploadedTrack = async (trackId) => {
  if (!trackId || !window.confirm('确认删除这首已上传的音乐吗？')) {
    return
  }

  try {
    if (previewingUploadedTrackId.value === trackId) {
      stopUploadedTrackPreview()
    }
    await musicStore.removeUploadedTrack(trackId)
  } catch (error) {
    ElMessage.error(error?.response?.data?.error || '删除上传音乐失败')
  }
}

if (uploadedPreviewAudio) {
  uploadedPreviewAudio.onended = () => {
    previewingUploadedTrackId.value = ''
  }

  uploadedPreviewAudio.onpause = () => {
    if (uploadedPreviewAudio.ended) return
    previewingUploadedTrackId.value = ''
  }
}

onBeforeUnmount(() => {
  resetPendingAvatarUpload()
  stopUploadedTrackPreview()
  stopChatPolling()
})

const submitCreatePlaylist = async () => {
  if (!newPlaylistName.value) return
  await musicStore.createPlaylist(newPlaylistName.value, newPlaylistDescription.value.trim())
  newPlaylistName.value = ''
  newPlaylistDescription.value = ''
  closeMusicModal()
}

const openAddToPlaylistModal = (track) => {
  trackToAdd.value = track
  showAddToPlaylistModal.value = true
}

const confirmAddToPlaylist = async (playlistId) => {
  if (trackToAdd.value) {
    await musicStore.addTrackToPlaylist(playlistId, trackToAdd.value)
  }
  showAddToPlaylistModal.value = false
  trackToAdd.value = null
}

const selectPlaylist = (pl) => {
  activePlaylist.value = musicStore.customPlaylists.find(item => item.id === pl.id) || pl
}

const openPlaylistDetailModal = (pl) => {
  selectPlaylist(pl)
  showPlaylistDetailModal.value = true
}

const closePlaylistDetailModal = () => {
  showPlaylistDetailModal.value = false
}

const handlePlaylistWheel = (event) => {
  const container = playlistCarouselRef.value || event.currentTarget
  if (!container) return

  const delta = Math.abs(event.deltaY) > Math.abs(event.deltaX) ? event.deltaY : event.deltaX
  container.scrollBy({
    left: delta * 1.15,
    behavior: 'smooth'
  })
}

const deletePlaylistAndReset = async (playlistId) => {
  await musicStore.deletePlaylist(playlistId)
  if (activePlaylist.value?.id === playlistId) {
    activePlaylist.value = null
    showPlaylistDetailModal.value = false
  }
}

watch(
  () => musicStore.customPlaylists,
  (playlists) => {
    if (!playlists.length) {
      activePlaylist.value = null
      showPlaylistDetailModal.value = false
      return
    }

    if (!activePlaylist.value) {
      activePlaylist.value = playlists[0]
      return
    }

    activePlaylist.value = playlists.find(item => item.id === activePlaylist.value.id) || playlists[0]
  },
  { immediate: true, deep: true }
)

// --- Tab 5: Social State ---
const socialMoodSuggestions = ['轻松', '期待', '平静', '治愈']

const parseSocialMoodTags = (value) => {
  if (!value || typeof value !== 'string') return []
  return [...new Set(
    value
      .split(/[,，、\s]+/)
      .map(tag => tag.trim())
      .filter(Boolean)
  )]
}

const createSocialPost = (post) => ({
  ...post,
  likeUsers: Array.isArray(post.likeUsers) ? post.likeUsers.map(user => ({ ...user })) : [],
  highlights: Array.isArray(post.highlights) ? [...post.highlights] : [],
  comments: Array.isArray(post.comments)
    ? post.comments.map(comment => ({
        ...comment,
        likeUsers: Array.isArray(comment.likeUsers) ? comment.likeUsers.map(user => ({ ...user })) : [],
        replyDraft: comment.replyDraft || '',
        isReplyEditorOpen: Boolean(comment.isReplyEditorOpen),
      }))
    : [],
  commentDraft: '',
  isMenuOpen: false,
  isCommentEditorOpen: false,
  aiSuggestion: post.aiSuggestion || '',
  aiSuggestionError: '',
  isAiSuggestionLoading: false,
})

const showSocialComposer = ref(false)
const showMyPostsModal = ref(false)
const socialDraft = ref({
  content: '',
  mood: ''
})
const parsedSocialMoodTags = computed(() => parseSocialMoodTags(socialDraft.value.mood))
const socialPosts = ref([])
const socialPage = ref(0)
const socialPageSize = 10
const socialTotalPages = ref(1)
const isSocialPostsLoading = ref(false)
const mySocialPosts = ref([])
const isMyPostsLoading = ref(false)
const deletingPostId = ref(null)
const socialUserMap = ref({})
const socialNotifications = ref([])

const currentUserId = computed(() => {
  const id = Number(userProfile.value.id)
  return Number.isFinite(id) && id > 0 ? id : null
})

const ensureSocialUserSummaries = async (userIds = []) => {
  const ids = [...new Set(userIds.map(id => Number(id)).filter(id => Number.isFinite(id) && id > 0))]
  if (currentUserId.value) {
    socialUserMap.value[currentUserId.value] = {
      userId: currentUserId.value,
      nickname: userProfile.value.name,
      username: userProfile.value.username,
      avatarUrl: userProfile.value.avatar,
    }
  }
  const missingIds = ids.filter(id => !socialUserMap.value[id])
  if (!missingIds.length) return

  try {
    const res = await getUserSummariesApi(missingIds)
    const nextMap = { ...socialUserMap.value }
    ;(res.data || []).forEach((user) => {
      nextMap[user.userId] = {
        ...user,
        avatarUrl: resolveUserAvatarUrl(user?.avatarUrl),
      }
    })
    socialUserMap.value = nextMap
  } catch (e) {
    console.warn('获取社交用户摘要失败:', e)
  }
}

const getSocialUserSummary = (userId) => {
  const numericId = Number(userId)
  if (!Number.isFinite(numericId) || numericId <= 0) return null
  if (currentUserId.value === numericId) {
    return {
      userId: numericId,
      nickname: userProfile.value.name,
      username: userProfile.value.username,
      avatarUrl: userProfile.value.avatar,
    }
  }
  return socialUserMap.value[numericId] || friendUserMap.value[numericId] || null
}

const getSocialDisplayName = (userId) => {
  const user = getSocialUserSummary(userId)
  if (!userId) return '匿名'
  return user?.nickname || user?.username || `用户 ${userId}`
}

const getSocialAvatar = (userId) => getSocialUserSummary(userId)?.avatarUrl || ''

const formatSocialTime = (value) => {
  if (!value) return '未知时间'
  const date = parseApiDateTime(value)
  if (Number.isNaN(date.getTime())) return '未知时间'

  const diffMs = Date.now() - date.getTime()
  if (diffMs < 0) {
    return formatApiDateTime(date, {}, '未知时间')
  }
  if (diffMs < 60 * 1000) return '刚刚'
  if (diffMs < 60 * 60 * 1000) return `${Math.floor(diffMs / (60 * 1000))} 分钟前`
  if (diffMs < 24 * 60 * 60 * 1000) return `${Math.floor(diffMs / (60 * 60 * 1000))} 小时前`
  if (diffMs < 7 * 24 * 60 * 60 * 1000) return `${Math.floor(diffMs / (24 * 60 * 60 * 1000))} 天前`
  return formatApiDateTime(date, {}, '未知时间')
}

const formatSocialLikeNames = (likeUsers = []) => {
  const names = [...new Set(likeUsers.map(user => user.name).filter(Boolean))]
  return names.join('、')
}

const startOfToday = computed(() => {
  const date = new Date()
  date.setHours(0, 0, 0, 0)
  return date
})

const endOfToday = computed(() => {
  const date = new Date(startOfToday.value)
  date.setDate(date.getDate() + 1)
  return date
})

const isWithinToday = (value) => {
  const date = parseApiDateTime(value)
  if (Number.isNaN(date.getTime())) return false
  return date >= startOfToday.value && date < endOfToday.value
}

const formatNotificationMessage = (item) => {
  switch (item.type) {
    case 'POST_LIKE':
      return '点赞了你的帖子'
    case 'POST_COMMENT':
      return `评论了你的帖子：${item.content || ''}`.trim()
    case 'COMMENT_LIKE':
      return '点赞了你的评论'
    case 'COMMENT_REPLY':
      return `回复了你的评论：${item.content || ''}`.trim()
    default:
      return item.content || '有新的互动'
  }
}

const toggleSocialMenu = (targetPost) => {
  socialPosts.value.forEach((post) => {
    post.isMenuOpen = post.id === targetPost.id ? !post.isMenuOpen : false
  })
}

const openSocialCommentEditor = (targetPost) => {
  socialPosts.value.forEach((post) => {
    post.isMenuOpen = false
    if (post.id === targetPost.id) {
      post.isCommentEditorOpen = true
    }
  })
}

const openCommentReplyEditor = (post, targetComment) => {
  post.comments.forEach((comment) => {
    comment.isReplyEditorOpen = comment.id === targetComment.id ? !comment.isReplyEditorOpen : false
  })
}

const buildSocialInteractions = (interactions = []) => {
  const likeUsers = []
  const comments = []
  const commentMap = new Map()

  interactions.forEach((interaction) => {
    if (interaction.comment) {
      const commentItem = {
        id: interaction.interactionId,
        authorUserId: interaction.userId,
        author: getSocialDisplayName(interaction.userId),
        replyToName: interaction.targetInteractionId ? getSocialDisplayName(interactions.find(item => item.interactionId === interaction.targetInteractionId)?.userId) : '',
        content: interaction.comment,
        timeLabel: formatSocialTime(interaction.createdAt),
        targetInteractionId: interaction.targetInteractionId,
        likeUsers: [],
        likedByMe: false,
        replyDraft: '',
        isReplyEditorOpen: false,
      }
      commentMap.set(commentItem.id, commentItem)
      comments.push(commentItem)
      return
    }

    if (interaction.liked && !interaction.comment && !interaction.targetInteractionId) {
      likeUsers.push({
        userId: interaction.userId,
        name: getSocialDisplayName(interaction.userId)
      })
      return
    }

    if (interaction.liked && interaction.targetInteractionId) {
      const targetComment = commentMap.get(interaction.targetInteractionId)
      if (targetComment) {
        targetComment.likeUsers.push({
          userId: interaction.userId,
          name: getSocialDisplayName(interaction.userId)
        })
      }
    }
  })

  comments.forEach((comment) => {
    comment.likedByMe = comment.likeUsers.some(user => Number(user.userId) === currentUserId.value)
  })

  return { likeUsers, comments }
}

const fetchSocialNotifications = async () => {
  try {
    const res = await getMySocialNotificationsApi()
    const notifications = (res.data || []).filter(item => isWithinToday(item.createdAt))
    const actorIds = notifications.map(item => item.actorUserId)
    await ensureSocialUserSummaries(actorIds)
    socialNotifications.value = notifications.map((item) => ({
      id: item.interactionId,
      actorUserId: item.actorUserId,
      actorName: getSocialDisplayName(item.actorUserId),
      type: item.type,
      content: item.content,
      timeLabel: formatSocialTime(item.createdAt),
      createdAt: item.createdAt,
      message: formatNotificationMessage(item),
    }))
  } catch (e) {
    console.warn('获取互动提醒失败:', e)
    socialNotifications.value = []
  }
}

const mapSocialPostList = async (rawPosts = []) => {
  const interactionEntries = await Promise.all(
    rawPosts.map(async (post) => {
      try {
        const response = await getPostInteractionsApi(post.postId)
        return [post.postId, response.data || []]
      } catch (error) {
        console.warn('获取帖子互动失败:', post.postId, error)
        return [post.postId, []]
      }
    })
  )

  const interactionsMap = Object.fromEntries(interactionEntries)
  const allUserIds = rawPosts.flatMap((post) => [
    post.authorUserId,
    ...(interactionsMap[post.postId] || []).map(interaction => interaction.userId),
  ])
  await ensureSocialUserSummaries(allUserIds)

  return rawPosts.map((post) => {
    const interactions = interactionsMap[post.postId] || []
    const { likeUsers, comments } = buildSocialInteractions(interactions)

    return createSocialPost({
      id: post.postId,
      authorUserId: post.authorUserId,
      authorName: getSocialDisplayName(post.authorUserId),
      authorAvatarUrl: getSocialAvatar(post.authorUserId),
      authorRole: !post.authorUserId ? '匿名' : (currentUserId.value === Number(post.authorUserId) ? '我' : '好友'),
      timeLabel: formatSocialTime(post.createdAt),
      mood: parseSocialMoodTags(post.moodTag).join(' / ') || post.moodTag || '未知',
      content: post.content,
      highlights: Array.isArray(post.highlightTags) && post.highlightTags.length
        ? post.highlightTags
        : parseSocialMoodTags(post.moodTag),
      likeUsers,
      likedByMe: likeUsers.some(user => Number(user.userId) === currentUserId.value),
      comments,
    })
  })
}

const fetchSocialPosts = async () => {
  isSocialPostsLoading.value = true
  try {
    const res = await getPostsApi(socialPage.value, socialPageSize)
    const pageData = res.data
    if (pageData?.content) {
      socialPosts.value = await mapSocialPostList(pageData.content)
      socialTotalPages.value = Math.max(pageData.totalPages || 1, 1)
    } else {
      socialPosts.value = []
      socialTotalPages.value = 1
    }
  } catch (e) {
    console.error('Failed to fetch social posts:', e)
  } finally {
    isSocialPostsLoading.value = false
  }
}

const changeSocialPage = async (delta) => {
  const nextPage = socialPage.value + delta
  if (nextPage < 0 || nextPage >= socialTotalPages.value) return
  socialPage.value = nextPage
  await fetchSocialPosts()
}

const refreshSocialFeed = async () => {
  socialPage.value = 0
  await fetchSocialPosts()
  await fetchSocialNotifications()
  markTabLoaded('social')
  markTabLoaded('social-notifications')
}

const fetchMySocialPosts = async () => {
  isMyPostsLoading.value = true
  try {
    const res = await getMyPostsApi(0, 20)
    const rawPosts = res.data?.content || []
    mySocialPosts.value = await mapSocialPostList(rawPosts)
  } catch (e) {
    console.error('Failed to fetch my social posts:', e)
    mySocialPosts.value = []
  } finally {
    isMyPostsLoading.value = false
  }
}

const socialSummary = computed(() => {
  const todayNotifications = socialNotifications.value.filter((item) => isWithinToday(item.createdAt))

  return socialPosts.value.reduce((summary, post) => {
    if (Number(post.authorUserId) === currentUserId.value) {
      summary.postCount += 1
    }
    return summary
  }, {
    postCount: 0,
    commentCount: todayNotifications.filter(item => item.type === 'POST_COMMENT' || item.type === 'COMMENT_REPLY').length,
    likeCount: todayNotifications.filter(item => item.type === 'POST_LIKE' || item.type === 'COMMENT_LIKE').length
  })
})

const getNameInitial = (name) => {
  return (name || '我').trim().slice(0, 1).toUpperCase()
}

const resetSocialDraft = () => {
  socialDraft.value = {
    content: '',
    mood: ''
  }
}

const openSocialComposer = () => {
  showSocialComposer.value = true
}

const openMyPostsSpace = async () => {
  showMyPostsModal.value = true
  await fetchMySocialPosts()
}

const closeSocialComposer = () => {
  showSocialComposer.value = false
  resetSocialDraft()
}

const closeMyPostsSpace = () => {
  showMyPostsModal.value = false
}

const requestPostAiSuggestion = async (post) => {
  if (!post?.id || post.isAiSuggestionLoading) return

  post.isMenuOpen = false
  post.isAiSuggestionLoading = true
  post.aiSuggestionError = ''
  if (!post.aiSuggestion) {
    post.aiSuggestion = ''
  }
  try {
    const response = await getPostAiReplySuggestionApi(post.id)
    const suggestion = String(response.data?.response || '').trim()
    if (!suggestion) {
      throw new Error('empty suggestion')
    }
    post.aiSuggestion = suggestion
    ElMessage.success('已生成 AI 回帖建议')
  } catch (error) {
    console.error('获取 AI 回帖建议失败:', error)
    const status = error?.response?.status
    const details = error?.response?.data?.details || error?.response?.data?.error || ''
    post.aiSuggestion = ''
    if (status === 401) {
      post.aiSuggestionError = '当前登录状态已失效，请重新登录后再试。'
      return
    }
    if (status === 502) {
      post.aiSuggestionError = 'AI 服务当前不可用，请确认 AI-service 已启动。'
      ElMessage.error('AI 服务不可用，请稍后重试')
      return
    }
    if (status === 500) {
      post.aiSuggestionError = details
        ? `社交服务调用 AI 失败：${details}`
        : '社交服务调用 AI 失败，请检查 social-service 与 AI-service 日志。'
      ElMessage.error('AI 回帖建议生成失败')
      return
    }
    post.aiSuggestionError = '暂时无法生成建议，请稍后重试。'
    ElMessage.error('AI 回帖建议生成失败')
  } finally {
    post.isAiSuggestionLoading = false
  }
}

const applyAiSuggestionToComment = (post) => {
  const suggestion = String(post?.aiSuggestion || '').trim()
  if (!suggestion) return
  post.commentDraft = suggestion
  post.isCommentEditorOpen = true
  post.isMenuOpen = false
}

const toggleSocialLike = async (post) => {
  try {
    await likePostApi(post.id)
    post.isMenuOpen = false
    await refreshSocialFeed()
    
    if (!post.likedByMe) {
      appendUserBehaviorLogApi({
        actionType: 'like_post',
        targetType: 'post',
        targetId: post.id,
        metadata: { authorName: post.authorName }
      }).catch(e => console.warn('Log user behavior failed', e))
    }
  } catch (e) {
    ElMessage.error('点赞失败')
  }
}

const handleSocialMenuLike = async (post) => {
  await toggleSocialLike(post)
}

const toggleCommentLike = async (post, comment) => {
  try {
    await likeCommentApi(post.id, comment.id)
    await refreshSocialFeed()
  } catch (e) {
    if (e.response?.status === 403) {
      ElMessage.error('只有帖子相关好友与评论主人可以点赞')
    } else {
      ElMessage.error('评论点赞失败')
    }
  }
}

const submitSocialComment = async (post) => {
  const content = post.commentDraft.trim()
  if (!content) return

  try {
    await commentPostApi(post.id, { comment: content })
    post.commentDraft = ''
    post.isCommentEditorOpen = false
    ElMessage.success('评论成功')
    await refreshSocialFeed()
  } catch (e) {
    ElMessage.error('评论失败')
  }
}

const submitCommentReply = async (post, comment) => {
  const content = comment.replyDraft.trim()
  if (!content) return

  try {
    await replyCommentApi(post.id, comment.id, { comment: content })
    comment.replyDraft = ''
    comment.isReplyEditorOpen = false
    ElMessage.success('回复成功')
    await refreshSocialFeed()
  } catch (e) {
    if (e.response?.status === 403) {
      ElMessage.error('只有帖子相关好友与评论主人可以回复')
    } else {
      ElMessage.error('回复失败')
    }
  }
}

const submitSocialPost = async () => {
  const content = socialDraft.value.content.trim()
  const moodTags = parsedSocialMoodTags.value
  if (!content || moodTags.length === 0) return

  try {
    const normalizedMood = moodTags.join(',')
    const res = await createPostApi({ content, moodTag: normalizedMood })
    ElMessage.success('发布成功')
    closeSocialComposer()
    await refreshSocialFeed()
    
    // User Behavior Log
    appendUserBehaviorLogApi({
      actionType: 'publish_post',
      targetType: 'post',
      targetId: res.data?.postId || 0,
      metadata: { moodTags }
    }).catch(e => console.warn('Log user behavior failed', e))
    
  } catch (e) {
    ElMessage.error('发布失败')
  }
}

const deleteSocialPost = async (post, options = {}) => {
  const confirmed = window.confirm('确认删除这条帖子吗？删除后无法恢复。')
  if (!confirmed) return

  post.isMenuOpen = false
  deletingPostId.value = post.id
  try {
    await deletePostApi(post.id)
    ElMessage.success('帖子已删除')
    socialPosts.value = socialPosts.value.filter(item => item.id !== post.id)
    mySocialPosts.value = mySocialPosts.value.filter(item => item.id !== post.id)

    const tasks = [refreshSocialFeed()]
    if (showMyPostsModal.value) {
      tasks.push(fetchMySocialPosts())
    }
    await Promise.all(tasks)
  } catch (e) {
    ElMessage.error('删除帖子失败')
  } finally {
    deletingPostId.value = null
  }
}

// --- Tab 6: Friends State ---
const friendsList = ref([])
const friendRequests = ref([])
const newFriendId = ref('')
const searchResults = ref([])
const isSearching = ref(false)
const selectedFriend = ref(null)
const friendUserMap = ref({})
const chatConversations = ref([])
const chatMessages = ref([])
const activeChatFriendId = ref(null)
const chatDraft = ref('')
const isChatListLoading = ref(false)
const isChatLoading = ref(false)
const isSendingChat = ref(false)
const chatMessageListRef = ref(null)
const chatTextareaRef = ref(null)
const supportiveReplyModalVisible = ref(false)
const activeSupportiveReplyCategory = ref('empathy')

const supportiveReplyCategories = [
  { key: 'empathy', label: '共情回应', description: '先接住对方的情绪，减少被忽视感。' },
  { key: 'ask', label: '温和提问', description: '用问题帮助对方继续表达，而不是急着下结论。' },
  { key: 'encourage', label: '鼓励支持', description: '在不说教的前提下，给对方一点力量感。' },
  { key: 'boundary', label: '尊重边界', description: '给对方留空间，避免把聊天变成压力。' },
  { key: 'practical', label: '小步行动', description: '陪对方把注意力落到一个可执行的小动作。' },
]

const supportiveRepliesByCategory = {
  empathy: [
    '听起来你真的很不容易。',
    '谢谢你愿意跟我说这些。',
    '我能理解你会这样感受。',
    '你现在的感受很重要。',
    '我在听，你可以慢慢说。',
  ],
  ask: [
    '你希望我更多是倾听，还是给一点建议？',
    '发生了什么，让你现在这么难受？',
    '此刻最让你卡住的是哪一部分？',
    '你觉得你最需要的支持是什么？',
    '如果把这件事缩小一点，现在最紧急的是哪一步？',
  ],
  encourage: [
    '你已经很努力了，真的。',
    '能坚持到现在说明你很有力量。',
    '我们先把今天过好就行，不用一次解决全部。',
    '我在这里，陪你一起扛着。',
    '如果你愿意，我们可以一起找一个更轻一点的做法。',
  ],
  boundary: [
    '如果你不想展开讲也没关系。',
    '我尊重你的节奏，你想停就停。',
    '你不用立刻回复我，照顾好自己最重要。',
    '你想先休息一下也可以，我会在。',
    '如果这话题让你压力很大，我们可以换个方向聊。',
  ],
  practical: [
    '要不要先做一个很小的动作：喝口水/深呼吸三次？',
    '我们可以先把目标缩到 5 分钟：先做一个最小步骤。',
    '你愿意的话，我陪你把事情拆成 3 个小点，一次只看一个。',
    '要不要先把想法写下来一行：我担心的是____，接下来能做的是____。',
    '先把情绪安顿一下：你现在更像是焦虑、难过，还是生气？',
  ],
}

const activeSupportiveReplies = computed(() => supportiveRepliesByCategory[activeSupportiveReplyCategory.value] || [])
const activeSupportiveReplyCategoryMeta = computed(() => (
  supportiveReplyCategories.find(category => category.key === activeSupportiveReplyCategory.value)
  || supportiveReplyCategories[0]
))

const openSupportiveReplyModal = () => {
  supportiveReplyModalVisible.value = true
}

const closeSupportiveReplyModal = () => {
  supportiveReplyModalVisible.value = false
}

const applySupportiveReply = (text) => {
  const suggestion = String(text || '').trim()
  if (!suggestion) return

  const existing = String(chatDraft.value || '').trim()
  chatDraft.value = existing ? `${existing}\n${suggestion}` : suggestion
  supportiveReplyModalVisible.value = false
  nextTick(() => {
    chatTextareaRef.value?.focus?.()
  })
}
const FRIENDSHIP_FLOWER_STEP = 50
const MAX_FRIENDSHIP_FLOWERS = 3
const MAX_FRIENDSHIP_SCORE = FRIENDSHIP_FLOWER_STEP * MAX_FRIENDSHIP_FLOWERS
const CHAT_FETCH_LIMIT = 50
const CHAT_POLL_INTERVAL = 5000
let chatPollingTimer = null

const getDisplayName = (user) => {
  if (!user) return ''
  return user.nickname || user.username || ''
}

const clampFriendshipScore = (score) => {
  const numericScore = Number(score)
  if (!Number.isFinite(numericScore)) {
    return 0
  }
  return Math.max(0, Math.floor(numericScore))
}

const buildFriendshipDescription = (score, flowerCount) => {
  if (flowerCount >= MAX_FRIENDSHIP_FLOWERS) {
    return `当前友情度 ${score} 分，三朵花已全部点亮，继续保持这份默契吧。`
  }

  const nextThreshold = (flowerCount + 1) * FRIENDSHIP_FLOWER_STEP
  const remaining = Math.max(0, nextThreshold - score)
  return `当前友情度 ${score} 分，已点亮 ${flowerCount}/${MAX_FRIENDSHIP_FLOWERS} 朵花，再获得 ${remaining} 分即可点亮下一朵。`
}

const buildNextGoalText = (score, flowerCount) => {
  if (flowerCount >= MAX_FRIENDSHIP_FLOWERS) {
    return '三朵花已全部点亮'
  }
  return `${Math.max(0, (flowerCount + 1) * FRIENDSHIP_FLOWER_STEP - score)} 分后升级`
}

const getDateTimeValue = (value) => {
  if (!value) return 0
  const date = parseApiDateTime(value)
  return Number.isNaN(date.getTime()) ? 0 : date.getTime()
}

const formatFriendDate = (value) => {
  if (!value) return '暂未记录'
  const date = parseApiDateTime(value)
  if (Number.isNaN(date.getTime())) {
    return '暂未记录'
  }
  return formatApiDateTime(date, {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  }, '暂未记录')
}

const chatConversationMap = computed(() => {
  const map = {}
  chatConversations.value.forEach((conversation) => {
    const peerUserId = Number(conversation.peerUserId)
    if (Number.isFinite(peerUserId) && peerUserId > 0) {
      map[peerUserId] = {
        ...conversation,
        unreadCount: Number(conversation.unreadCount) || 0,
      }
    }
  })
  return map
})

const sortedChatFriends = computed(() => (
  friendsList.value
    .map((friend) => {
      const conversation = chatConversationMap.value[Number(friend.id)]
      return {
        ...friend,
        conversationId: conversation?.conversationId || null,
        lastMessagePreview: conversation?.lastMessagePreview || '',
        lastMessageAt: conversation?.lastMessageAt || '',
        unreadCount: Number(conversation?.unreadCount) || 0,
      }
    })
    .sort((a, b) => {
      const timeDiff = getDateTimeValue(b.lastMessageAt) - getDateTimeValue(a.lastMessageAt)
      if (timeDiff !== 0) {
        return timeDiff
      }
      return String(a.name || '').localeCompare(String(b.name || ''), 'zh-Hans-CN')
    })
))

const activeChatFriend = computed(() => (
  sortedChatFriends.value.find((friend) => Number(friend.id) === Number(activeChatFriendId.value))
  || null
))

const normalizeChatMessages = (messages = []) => (
  [...messages]
    .map((message) => ({
      ...message,
      messageId: Number(message.messageId) || 0,
      senderId: Number(message.senderId) || 0,
    }))
    .sort((a, b) => {
      const timeDiff = getDateTimeValue(a.createdAt) - getDateTimeValue(b.createdAt)
      if (timeDiff !== 0) {
        return timeDiff
      }
      return a.messageId - b.messageId
    })
)

const formatChatListTime = (value) => {
  if (!value) return ''
  const date = parseApiDateTime(value)
  if (Number.isNaN(date.getTime())) return ''

  const now = new Date()
  const isSameYear = now.getFullYear() === date.getFullYear()
  const isSameDay = isSameYear
    && now.getMonth() === date.getMonth()
    && now.getDate() === date.getDate()

  if (isSameDay) {
    return formatApiDateTime(date, {
      hour: '2-digit',
      minute: '2-digit',
    }, '')
  }

  return formatApiDateTime(date, isSameYear
    ? {
        month: '2-digit',
        day: '2-digit',
      }
    : {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
      }, '')
}

const formatChatMessageTime = (value) => {
  if (!value) return '刚刚'
  const date = parseApiDateTime(value)
  if (Number.isNaN(date.getTime())) return '刚刚'
  return formatApiDateTime(date, {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  }, '刚刚')
}

const scrollChatToBottom = () => {
  nextTick(() => {
    window.requestAnimationFrame(() => {
      const container = chatMessageListRef.value
      if (!container) return
      container.scrollTop = container.scrollHeight
    })
  })
}

const stopChatPolling = () => {
  if (chatPollingTimer) {
    window.clearInterval(chatPollingTimer)
    chatPollingTimer = null
  }
}

const startChatPolling = () => {
  stopChatPolling()
  if (currentTab.value !== 'chat') return
  chatPollingTimer = window.setInterval(async () => {
    if (currentTab.value !== 'chat') return
    await fetchChatConversations({ silent: true })
    if (activeChatFriendId.value) {
      await fetchChatMessages(activeChatFriendId.value, {
        silent: true,
        scroll: false,
        markRead: true,
      })
    }
  }, CHAT_POLL_INTERVAL)
}

const ensureUserSummaries = async (userIds = []) => {
  const ids = [...new Set(userIds.filter(Boolean))]
  const missingIds = ids.filter(id => !friendUserMap.value[id])

  if (missingIds.length === 0) {
    return
  }

  try {
    const res = await getUserSummariesApi(missingIds)
    const nextMap = { ...friendUserMap.value }
    ;(res.data || []).forEach(user => {
      nextMap[user.userId] = {
        ...user,
        avatarUrl: resolveUserAvatarUrl(user?.avatarUrl),
      }
    })
    friendUserMap.value = nextMap
  } catch (e) {
    console.warn('获取用户摘要失败:', e)
  }
}

const fetchFriends = async () => {
  try {
    const res = await getMyFriendsApi()
    if (res.data) {
      const previousSelectedId = selectedFriend.value?.id
      await ensureUserSummaries(res.data.map(f => f.friendUserId))
      const nextFriends = res.data.map(f => {
        const friendshipScore = clampFriendshipScore(f.friendshipScore)
        const intimacy = Math.max(0, Math.min(MAX_FRIENDSHIP_FLOWERS, Number(f.intimacyLevel) || 0))
        return {
        friendshipId: f.friendshipId,
        id: f.friendUserId,
        userId: f.friendUserId,
        name: getDisplayName(friendUserMap.value[f.friendUserId]) || `用户 ${f.friendUserId}`,
        username: friendUserMap.value[f.friendUserId]?.username || '',
        avatarUrl: friendUserMap.value[f.friendUserId]?.avatarUrl || '',
        bio: friendUserMap.value[f.friendUserId]?.bio || '',
        intimacy,
        friendshipScore,
        createdAt: f.createdAt || '',
        updatedAt: f.updatedAt || '',
        progressPercent: Math.min(100, Math.round((friendshipScore / MAX_FRIENDSHIP_SCORE) * 100)),
        friendshipDescription: buildFriendshipDescription(friendshipScore, intimacy),
        nextGoalText: buildNextGoalText(friendshipScore, intimacy)
      }
      })
      friendsList.value = nextFriends
      selectedFriend.value = nextFriends.find(friend => friend.id === previousSelectedId) || null
    }
  } catch (e) {
    console.error('获取好友列表失败:', e)
  }
}

const fetchFriendRequests = async () => {
  try {
    const res = await getReceivedFriendRequestsApi()
    if (res.data) {
      await ensureUserSummaries(res.data.map(req => req.senderId))
      friendRequests.value = res.data.map(req => ({
        ...req,
        senderName: getDisplayName(friendUserMap.value[req.senderId]) || `用户 ${req.senderId}`,
        senderUsername: friendUserMap.value[req.senderId]?.username || '',
        senderAvatarUrl: friendUserMap.value[req.senderId]?.avatarUrl || '',
      }))
    }
  } catch (e) {
    console.error('获取好友申请失败:', e)
  }
}

const fetchChatConversations = async ({ silent = false } = {}) => {
  try {
    if (!silent) {
      isChatListLoading.value = true
    }
    const res = await getChatConversationsApi()
    const conversations = Array.isArray(res.data) ? res.data : []
    await ensureUserSummaries(conversations.map(item => item.peerUserId))
    chatConversations.value = conversations
  } catch (e) {
    console.error('获取聊天会话失败:', e)
    if (!silent) {
      ElMessage.error('获取聊天会话失败')
    }
  } finally {
    if (!silent) {
      isChatListLoading.value = false
    }
  }
}

const fetchChatMessages = async (peerUserId, options = {}) => {
  const {
    silent = false,
    scroll = true,
    markRead = true,
  } = options

  const numericPeerUserId = Number(peerUserId)
  if (!Number.isFinite(numericPeerUserId) || numericPeerUserId <= 0) {
    chatMessages.value = []
    return
  }

  try {
    if (!silent) {
      isChatLoading.value = true
    }
    const res = await getChatMessagesApi(numericPeerUserId, { limit: CHAT_FETCH_LIMIT })
    chatMessages.value = normalizeChatMessages(Array.isArray(res.data) ? res.data : [])
    if (scroll) {
      scrollChatToBottom()
    }

    if (markRead && (chatConversationMap.value[numericPeerUserId]?.unreadCount || 0) > 0) {
      await markChatAsReadApi(numericPeerUserId)
      await fetchChatConversations({ silent: true })
    }
  } catch (e) {
    console.error('获取聊天记录失败:', e)
    if (!silent) {
      ElMessage.error('获取聊天记录失败')
    }
  } finally {
    if (!silent) {
      isChatLoading.value = false
    }
  }
}

const loadChatTabData = async ({ preferredFriendId = null } = {}) => {
  await fetchFriends()
  await fetchChatConversations()

  const validFriendIds = new Set(friendsList.value.map(friend => Number(friend.id)))
  const candidates = [
    Number(preferredFriendId),
    Number(activeChatFriendId.value),
    Number(selectedFriend.value?.id),
    Number(chatConversations.value[0]?.peerUserId),
    Number(friendsList.value[0]?.id),
  ].filter(id => Number.isFinite(id) && validFriendIds.has(id))

  const nextActiveFriendId = candidates[0] || null
  if (!nextActiveFriendId) {
    activeChatFriendId.value = null
    chatMessages.value = []
    return
  }

  if (Number(activeChatFriendId.value) !== nextActiveFriendId) {
    activeChatFriendId.value = nextActiveFriendId
  } else {
    await fetchChatMessages(nextActiveFriendId, { markRead: true, scroll: true })
  }
}

const searchUser = async () => {
  const keyword = newFriendId.value.trim()
  if (!keyword) {
    searchResults.value = []
    return
  }
  try {
    isSearching.value = true
    const res = await searchUsersApi(keyword)
    if (res.data) {
      searchResults.value = res.data
    }
  } catch (e) {
    ElMessage.error('搜索用户失败')
    searchResults.value = []
  } finally {
    isSearching.value = false
  }
}

const addFriend = async (targetId) => {
  if (!targetId) return
  try {
    await sendFriendRequestApi({ friendUserId: targetId, intimacyLevel: 1 })
    ElMessage.success('好友申请已发送')
    newFriendId.value = ''
    searchResults.value = []
  } catch (e) {
    ElMessage.error(e.response?.data || '发送好友申请失败')
  }
}

const handleRequest = async (requestId, accept) => {
  try {
    await handleFriendRequestApi(requestId, { accept })
    ElMessage.success(accept ? '已同意好友申请' : '已拒绝好友申请')
    await fetchFriendRequests()
    if (accept) {
      await fetchFriends()
    }
  } catch (e) {
    ElMessage.error('处理好友申请失败')
  }
}

const selectFriend = (friend) => {
  selectedFriend.value = friend
}

const closeSelectedFriend = () => {
  selectedFriend.value = null
}

const openChatWithFriend = async (friend) => {
  const nextFriendId = Number(friend?.id || friend?.peerUserId)
  if (!Number.isFinite(nextFriendId) || nextFriendId <= 0) {
    return
  }

  if (currentTab.value !== 'chat') {
    activeChatFriendId.value = nextFriendId
    currentTab.value = 'chat'
    return
  }

  if (Number(activeChatFriendId.value) === nextFriendId) {
    await fetchChatMessages(nextFriendId, { markRead: true, scroll: true })
    return
  }

  activeChatFriendId.value = nextFriendId
}

const sendCurrentChatMessage = async () => {
  const peerUserId = Number(activeChatFriendId.value)
  const content = chatDraft.value.trim()

  if (!Number.isFinite(peerUserId) || peerUserId <= 0 || !content || isSendingChat.value) {
    return
  }

  try {
    isSendingChat.value = true
    const res = await sendChatMessageApi(peerUserId, { content })
    chatMessages.value = normalizeChatMessages([
      ...chatMessages.value,
      ...(res.data ? [res.data] : []),
    ])
    chatDraft.value = ''
    scrollChatToBottom()
    await fetchChatConversations({ silent: true })
  } catch (e) {
    console.error('发送聊天消息失败:', e)
    ElMessage.error(typeof e?.response?.data === 'string' ? e.response.data : '发送消息失败')
  } finally {
    isSendingChat.value = false
  }
}

watch(activeChatFriendId, (newFriendId) => {
  if (currentTab.value !== 'chat') {
    return
  }

  chatDraft.value = ''
  if (!newFriendId) {
    chatMessages.value = []
    return
  }

  fetchChatMessages(newFriendId, { markRead: true, scroll: true })
})

watch(
  () => [currentTab.value, activeChatFriendId.value, chatMessages.value.length, isChatLoading.value],
  ([tab, friendId, messageCount, loading]) => {
    if (tab !== 'chat' || !friendId || loading || messageCount <= 0) {
      return
    }
    scrollChatToBottom()
  },
  { flush: 'post' }
)

const deleteFriend = async (friendshipId) => {
  if (confirm('确定要删除这位好友吗？')) {
    try {
      await deleteFriendshipApi(friendshipId)
      ElMessage.success('删除成功')
      selectedFriend.value = null
      await fetchFriends()
      await fetchChatConversations({ silent: true })
      if (!friendsList.value.some(friend => Number(friend.id) === Number(activeChatFriendId.value))) {
        activeChatFriendId.value = null
        chatMessages.value = []
      }
    } catch (e) {
      ElMessage.error('删除好友失败')
    }
  }
}

// --- Tab 3: Meditation State ---
const meditationChartRef = ref(null)
let meditationChart = null

const initMeditationCharts = async () => {
  const echarts = await getEcharts()
  nextTick(() => {
    if (meditationChartRef.value) {
      if (meditationChart) meditationChart.dispose();
      meditationChart = echarts.init(meditationChartRef.value)
      const yAxisConfig = getMeditationYAxisConfig()
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
          min: 0,
          max: yAxisConfig.max,
          interval: yAxisConfig.interval,
          minInterval: 1,
          splitLine: { lineStyle: { color: 'rgba(0,0,0,0.05)' } }
        },
        series: [{
          data: weeklyMeditationData.value,
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

// Garden gamification state
const seedInventory = ref([])
const fruitInventory = ref([])
const unlockedPlants = ref([])

const seedDict = {
  1: { name: '向日葵种子', icon: '🌻' },
  2: { name: '玫瑰种子', icon: '🌹' },
  3: { name: '仙人掌种子', icon: '🌵' }
}

const fruitDict = {
  1: { name: '向日葵籽', icon: '🌰' },
  2: { name: '玫瑰花瓣', icon: '🥀' }
}

const plantDict = {
  1: { name: '向日葵', icon: '🌻', description: '充满阳光的植物' },
  2: { name: '玫瑰', icon: '🌹', description: '代表热情的植物' },
  3: { name: '小雏菊', icon: '🌼', description: '清新淡雅' },
  4: { name: '仙人掌', icon: '🌵', description: '坚韧不拔' }
}

const fetchGardenData = async () => {
  try {
    const res = await getMyGardenApi()
    if (res.data) {
      seedInventory.value = (res.data.seeds || []).map(item => ({
        id: item.itemId,
        name: seedDict[item.itemId]?.name || '未知种子',
        icon: seedDict[item.itemId]?.icon || '🌱',
        count: item.count
      }))
      
      fruitInventory.value = (res.data.fruits || []).map(item => ({
        id: item.itemId,
        name: fruitDict[item.itemId]?.name || '未知果实',
        icon: fruitDict[item.itemId]?.icon || '🍎',
        count: item.count
      }))
      
      unlockedPlants.value = (res.data.unlockedPlantIds || []).map(id => ({
        id,
        name: plantDict[id]?.name || '未知植物',
        icon: plantDict[id]?.icon || '🌿',
        description: plantDict[id]?.description || '这是一种神奇的植物'
      }))
    }
  } catch (e) {
    console.error('获取花园数据失败:', e)
  }
}

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
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  const durationMap = buildMeditationDurationMap(meditationLogs.value)

  const days = []
  for(let i=1; i<=daysInMonth; i++) {
    const currentDate = new Date(year, month, i)
    currentDate.setHours(0, 0, 0, 0)
    const dateKey = formatLocalDateKey(currentDate)
    const totalMinutes = durationMap[dateKey] || 0

    let status = 'future'
    if (currentDate.getTime() > today.getTime()) {
      status = 'future'
    } else if (totalMinutes >= MEDITATION_DAY_TARGET_MINUTES) {
      status = 'completed'
    } else if (totalMinutes > 0) {
      status = 'partial'
    } else if (currentDate.getTime() < today.getTime()) {
      status = 'missed'
    }

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
  background:
    radial-gradient(circle at 16% 18%, rgba(255, 255, 255, 0.92), transparent 22%),
    radial-gradient(circle at 82% 20%, rgba(204, 226, 212, 0.8), transparent 26%),
    radial-gradient(circle at 78% 82%, rgba(225, 239, 229, 0.86), transparent 20%),
    linear-gradient(145deg, #f5fbf5 0%, #e4f1e8 48%, #edf7ef 100%);
}

.personal-space-page::before,
.personal-space-page::after {
  content: '';
  position: absolute;
  border-radius: 50%;
  pointer-events: none;
  filter: blur(6px);
}

.personal-space-page::before {
  width: 360px;
  height: 360px;
  top: -90px;
  right: -80px;
  background: radial-gradient(circle, rgba(255, 255, 255, 0.75) 0%, rgba(219, 236, 225, 0.2) 58%, transparent 78%);
}

.personal-space-page::after {
  width: 300px;
  height: 300px;
  bottom: -120px;
  left: -70px;
  background: radial-gradient(circle, rgba(206, 227, 214, 0.5) 0%, rgba(242, 249, 244, 0.12) 62%, transparent 78%);
}

.back-btn {
  position: absolute;
  top: 40px;
  left: 40px;
  display: flex;
  align-items: center;
  gap: 8px;
  color: #385058;
  text-decoration: none;
  font-size: 1rem;
  font-weight: 600;
  transition: all var(--transition-fast);
  z-index: 10;
  padding: 10px 16px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.62);
  border: 1px solid rgba(255, 255, 255, 0.75);
  box-shadow: 0 10px 24px rgba(94, 123, 114, 0.08);
  backdrop-filter: blur(10px);
}

.back-btn:hover {
  transform: translateX(-4px) translateY(-1px);
  color: var(--color-accent-sage);
  background: rgba(255, 255, 255, 0.82);
}

.page-shell {
  width: 100%;
  max-width: 1400px;
  display: flex;
  gap: 20px;
  margin-top: 40px;
  height: calc(100vh - 120px);
  position: relative;
  z-index: 1;
}

.glass-panel {
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.72) 0%, rgba(250, 252, 251, 0.66) 100%);
  backdrop-filter: blur(22px);
  border: 1px solid rgba(255, 255, 255, 0.82);
  border-radius: var(--radius-xl);
  box-shadow: 0 22px 48px rgba(93, 121, 112, 0.12);
  overflow: hidden;
}

.glass-panel-inner {
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.52) 0%, rgba(247, 250, 248, 0.44) 100%);
  border: 1px solid rgba(255, 255, 255, 0.66);
  border-radius: var(--radius-lg);
  padding: 20px;
}

/* Sidebar */
.sidebar {
  width: 250px;
  max-width: 100%;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  min-height: 0;
  padding: 22px 18px 20px;
  position: relative;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.82) 0%, rgba(246, 251, 248, 0.72) 100%);
}

.sidebar::before {
  content: '';
  position: absolute;
  inset: 0;
  background:
    radial-gradient(circle at 15% 15%, rgba(255, 255, 255, 0.75), transparent 26%),
    linear-gradient(180deg, rgba(180, 206, 188, 0.12), transparent 34%);
  pointer-events: none;
}

.sidebar-brand {
  position: relative;
  z-index: 1;
  margin-bottom: 22px;
  padding: 18px 16px 16px;
  border-radius: 22px;
  background: linear-gradient(145deg, rgba(255, 255, 255, 0.84), rgba(244, 249, 246, 0.6));
  border: 1px solid rgba(255, 255, 255, 0.72);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.65);
}

.sidebar-kicker {
  display: inline-flex;
  align-items: center;
  padding: 5px 10px;
  border-radius: 999px;
  font-size: 0.74rem;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: #6f8a7e;
  background: rgba(190, 214, 197, 0.36);
}

.sidebar-title {
  padding: 0;
  margin: 12px 0 8px;
  font-family: var(--font-serif);
  color: #2f4247;
  font-size: 1.6rem;
  letter-spacing: 0.02em;
}

.sidebar-subtitle {
  margin: 0;
  color: #7a8e88;
  font-size: 0.9rem;
  line-height: 1.65;
}

.space-nav {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-height: 0;
  gap: 8px;
  padding: 0;
  padding-right: 4px;
  overflow-x: hidden;
  overflow-y: auto;
  overscroll-behavior: contain;
  scrollbar-width: none;
  position: relative;
  z-index: 1;
}

.space-nav::-webkit-scrollbar {
  width: 0;
  height: 0;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 13px 16px;
  background: rgba(255, 255, 255, 0.2);
  border: 1px solid transparent;
  border-radius: 18px;
  color: #68807a;
  font-size: 0.96rem;
  font-weight: 500;
  cursor: pointer;
  transition: all var(--transition-fast);
  text-align: left;
  position: relative;
  overflow: hidden;
}

.nav-item::before {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(90deg, rgba(255, 255, 255, 0.35), transparent 62%);
  opacity: 0;
  transition: opacity var(--transition-fast);
}

.nav-item:hover {
  background: rgba(255, 255, 255, 0.62);
  color: #33484d;
  border-color: rgba(203, 220, 212, 0.85);
  transform: translateX(4px);
}

.nav-item:hover::before {
  opacity: 1;
}

.nav-item.active {
  background: linear-gradient(135deg, #9bb992 0%, #86aa8a 100%);
  color: #fff;
  border-color: rgba(255, 255, 255, 0.28);
  box-shadow: 0 14px 28px rgba(124, 152, 133, 0.24);
  transform: translateX(6px);
}

.nav-item.active::after {
  content: '';
  position: absolute;
  right: 16px;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.9);
  box-shadow: 0 0 0 5px rgba(255, 255, 255, 0.12);
}

.nav-icon {
  width: 18px;
  height: 18px;
  padding: 8px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.72);
  color: #6a857c;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.65);
  position: relative;
  z-index: 1;
}

.nav-item span {
  position: relative;
  z-index: 1;
}

.nav-item.active .nav-icon {
  background: rgba(255, 255, 255, 0.2);
  color: #fff;
}

/* Main Content */
.main-content {
  flex: 1;
  min-width: 0;
  padding: 30px;
  display: flex;
  flex-direction: column;
  overflow-y: auto;
  position: relative;
  background:
    radial-gradient(circle at top right, rgba(255, 255, 255, 0.36), transparent 18%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.72), rgba(246, 250, 247, 0.66));
}

.main-content::before {
  content: '';
  position: absolute;
  inset: 0 0 auto 0;
  height: 92px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.16), transparent);
  pointer-events: none;
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
  min-width: 0;
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
  width: min(400px, 100%);
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

.diary-actions {
  display: flex;
  gap: 10px;
  margin-bottom: 12px;
}

.diary-actions .action-btn {
  width: auto;
  flex: 1;
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

.diary-empty-state {
  background: rgba(255, 255, 255, 0.5);
  border-radius: var(--radius-lg);
  padding: 20px;
  min-height: 220px;
  display: flex;
  align-items: center;
  justify-content: center;
  text-align: center;
}

.diary-hint {
  color: var(--color-text-secondary);
  line-height: 1.7;
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

.feedback-layout {
  display: grid;
  grid-template-columns: minmax(0, 1.15fr) minmax(280px, 0.85fr);
  gap: 20px;
  margin-top: 20px;
}

.feedback-tab {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.feedback-tab-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
}

.feedback-header-desc {
  margin: -8px 0 0;
  color: var(--color-text-secondary);
  line-height: 1.7;
}

.feedback-card {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.feedback-card-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
}

.feedback-card-head p,
.feedback-history-item p,
.feedback-history-time {
  margin: 0;
  color: var(--color-text-secondary);
  line-height: 1.7;
}

.feedback-summary-pill {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 36px;
  padding: 0 14px;
  border-radius: var(--radius-pill);
  background: rgba(118, 150, 157, 0.12);
  color: #48656d;
  font-size: 0.9rem;
  font-weight: 600;
}

.feedback-form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.feedback-field {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.feedback-field span,
.feedback-history-head h5 {
  color: var(--color-text-primary);
  font-weight: 600;
}

.feedback-field.full-width {
  grid-column: 1 / -1;
}

.feedback-select,
.feedback-textarea {
  width: 100%;
}

.feedback-rating-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.feedback-rating-btn {
  min-width: 48px;
  min-height: 44px;
  padding: 0 14px;
  border-radius: 14px;
  border: 1px solid rgba(118, 150, 157, 0.18);
  background: rgba(255, 255, 255, 0.72);
  color: #48656d;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}

.feedback-rating-btn.active,
.feedback-rating-btn:hover {
  background: rgba(118, 150, 157, 0.14);
  border-color: rgba(118, 150, 157, 0.32);
  transform: translateY(-1px);
}

.feedback-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.feedback-actions .action-btn {
  width: auto;
  min-width: 120px;
  padding-inline: 22px;
}

.feedback-preference-grid,
.feedback-history-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.feedback-preference-item,
.feedback-history-item {
  padding: 14px 16px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid rgba(118, 150, 157, 0.12);
}

.feedback-preference-top,
.feedback-history-top,
.feedback-history-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.feedback-preference-top strong,
.feedback-history-top strong {
  color: #31474d;
}

.feedback-preference-top span,
.feedback-history-top span {
  color: #587078;
  font-weight: 600;
}

.feedback-progress-track {
  margin-top: 10px;
  width: 100%;
  height: 8px;
  border-radius: 999px;
  background: rgba(118, 150, 157, 0.12);
  overflow: hidden;
}

.feedback-progress-bar {
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #86aa8a 0%, #6e98a3 100%);
}

.feedback-empty-state {
  min-height: 120px;
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

/* Social Tab */
.social-tab {
  display: flex;
  flex-direction: column;
  gap: 20px;
  height: 100%;
  min-height: 0;
}

.social-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
}

.social-header-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  align-items: center;
}

.social-secondary-btn,
.social-publish-btn {
  width: 120px;
  min-height: 44px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0;
}

.social-secondary-btn {
  background: rgba(255, 255, 255, 0.8);
  color: #445b62;
  border: 1px solid rgba(118, 150, 157, 0.16);
}

.social-secondary-btn:hover {
  background: rgba(255, 255, 255, 0.95);
}

.social-header-desc {
  margin: 8px 0 0;
  color: var(--color-text-secondary);
  line-height: 1.6;
}

.social-comment-btn {
  width: auto;
  white-space: nowrap;
}

.social-layout {
  display: grid;
  grid-template-columns: minmax(0, 1.9fr) minmax(260px, 0.95fr);
  gap: 20px;
  flex: 1;
  min-height: 0;
}

.social-feed {
  display: flex;
  flex-direction: column;
  gap: 18px;
  min-height: 0;
  overflow-y: auto;
  padding-right: 6px;
}

.social-feed::-webkit-scrollbar {
  width: 8px;
}

.social-feed::-webkit-scrollbar-thumb {
  background: rgba(118, 150, 157, 0.3);
  border-radius: 999px;
}

.social-feed::-webkit-scrollbar-track {
  background: rgba(235, 242, 240, 0.72);
  border-radius: 999px;
}

.social-pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 8px 0 4px;
}

.social-page-indicator {
  color: #6b7d77;
  font-size: 14px;
}

.social-post-card {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 22px;
  border: 1px solid rgba(118, 150, 157, 0.14);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.9) 0%, rgba(245, 250, 248, 0.92) 100%);
}

.social-post-top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 14px;
}

.social-post-top-actions {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.social-author-block {
  display: flex;
  align-items: center;
  gap: 12px;
}

.social-avatar {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, rgba(118, 150, 157, 0.88), rgba(144, 181, 167, 0.92));
  color: #fff;
  font-weight: 700;
  box-shadow: 0 10px 22px rgba(118, 150, 157, 0.18);
}

.social-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 50%;
}

.social-author-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.social-role-tag {
  padding: 3px 10px;
  border-radius: 999px;
  background: rgba(118, 150, 157, 0.12);
  color: #56727a;
  font-size: 0.78rem;
}

.social-post-time,
.social-comment-time {
  color: #809299;
  font-size: 0.85rem;
}

.social-mood-pill {
  display: inline-flex;
  align-items: center;
  padding: 7px 14px;
  border-radius: 999px;
  background: rgba(118, 150, 157, 0.12);
  color: #3f5b63;
  font-size: 0.88rem;
  font-weight: 600;
  white-space: nowrap;
}

.social-post-content {
  margin: 0;
  color: #32444a;
  line-height: 1.8;
  white-space: pre-wrap;
}

.social-highlight-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.social-highlight-chip {
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.88);
  border: 1px solid rgba(118, 150, 157, 0.16);
  color: #56727a;
  font-size: 0.82rem;
}

.social-menu-wrap {
  position: relative;
}

.social-menu-btn {
  min-width: 42px;
  height: 42px;
  padding: 0 12px;
  border: none;
  border-radius: 12px;
  background: rgba(118, 150, 157, 0.12);
  color: #4b6870;
  font-size: 1.2rem;
  letter-spacing: 2px;
  cursor: pointer;
}

.social-menu-dropdown {
  position: absolute;
  top: calc(100% + 8px);
  right: 0;
  min-width: 112px;
  padding: 8px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.96);
  border: 1px solid rgba(118, 150, 157, 0.16);
  box-shadow: 0 12px 28px rgba(63, 89, 98, 0.14);
  display: flex;
  flex-direction: column;
  gap: 6px;
  z-index: 3;
}

.social-menu-item {
  border: none;
  border-radius: 10px;
  padding: 10px 12px;
  background: transparent;
  color: #36525a;
  text-align: left;
  cursor: pointer;
}

.social-menu-item:hover {
  background: rgba(118, 150, 157, 0.1);
}

.social-menu-item.danger {
  color: #c64f63;
}

.social-menu-item.danger:hover {
  background: rgba(255, 82, 82, 0.1);
}

.social-ai-suggestion-card {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 14px 16px;
  border-radius: 16px;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.92), rgba(239, 246, 244, 0.92));
  border: 1px solid rgba(118, 150, 157, 0.14);
}

.social-ai-suggestion-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  color: #3c5961;
}

.social-ai-suggestion-tools {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.social-ai-suggestion-text,
.social-ai-suggestion-placeholder,
.social-ai-suggestion-error {
  margin: 0;
  line-height: 1.7;
}

.social-ai-suggestion-text,
.social-ai-suggestion-placeholder {
  color: #425d64;
}

.social-ai-suggestion-error {
  color: #b44c5d;
}

.social-text-btn {
  border: none;
  padding: 0;
  background: transparent;
  color: #6d86ad;
  cursor: pointer;
  font-size: 0.9rem;
  font-weight: 600;
}

.social-feedback-card {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 14px 16px;
  border-radius: 16px;
  background: rgba(246, 249, 248, 0.9);
  border: 1px solid rgba(118, 150, 157, 0.08);
}

.social-like-line {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #566d75;
  line-height: 1.6;
}

.social-like-icon {
  font-size: 1.1rem;
  color: #6d86ad;
}

.social-comment-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.social-comment-item {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 8px;
  padding: 10px 14px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.7);
}

.social-comment-main {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
}

.social-comment-author {
  color: #2b4147;
  font-weight: 600;
}

.social-comment-reply-target {
  color: #6d86ad;
  font-weight: 600;
}

.social-comment-text {
  color: #4d6167;
}

.social-comment-like-line {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #677d84;
  font-size: 0.9rem;
}

.social-comment-actions {
  display: flex;
  gap: 10px;
}

.social-comment-action-btn {
  border: none;
  padding: 0;
  background: transparent;
  color: #6d86ad;
  cursor: pointer;
  font-size: 0.9rem;
}

.social-comment-action-btn:hover {
  color: #4f6d95;
}

.social-comment-reply-editor {
  width: 100%;
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 10px;
}

.social-comment-editor {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 10px;
}

.social-side-panel {
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-height: 0;
}

.social-side-card {
  padding: 22px;
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid rgba(118, 150, 157, 0.14);
}

.social-side-card.muted {
  background: rgba(244, 248, 246, 0.88);
}

.social-side-kicker {
  display: inline-block;
  margin-bottom: 10px;
  color: #7b9096;
  font-size: 0.8rem;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.social-side-card h4 {
  margin: 0 0 12px;
  color: #24353a;
}

.social-side-card p {
  margin: 0;
  color: #62757c;
  line-height: 1.7;
}

.social-stat-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.social-stat-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 14px;
  border-radius: 16px;
  background: rgba(248, 251, 249, 0.95);
}

.social-stat-item strong {
  font-size: 1.35rem;
  color: #2d4750;
}

.social-stat-item span {
  color: #7a8f95;
  font-size: 0.88rem;
}

.social-notification-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  max-height: 280px;
  overflow-y: auto;
  padding-right: 6px;
}

.social-notification-list::-webkit-scrollbar {
  width: 8px;
}

.social-notification-list::-webkit-scrollbar-thumb {
  background: rgba(118, 150, 157, 0.28);
  border-radius: 999px;
}

.social-notification-list::-webkit-scrollbar-track {
  background: rgba(235, 242, 240, 0.72);
  border-radius: 999px;
}

.social-notification-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 12px 14px;
  border-radius: 14px;
  background: rgba(248, 251, 249, 0.95);
}

.social-notification-item strong {
  color: #2d4750;
}

.social-notification-item span {
  color: #5d7178;
  line-height: 1.6;
}

.social-notification-item em {
  color: #889aa0;
  font-style: normal;
  font-size: 0.84rem;
}

.social-trend-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 14px;
}

.social-create-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.music-modal-card.my-posts-modal-card.modal-content {
  width: min(980px, 94vw);
  max-width: 980px;
  max-height: min(84vh, 900px);
}

.my-posts-scroll-area {
  max-height: 64vh;
  overflow-y: auto;
  padding-right: 6px;
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.my-posts-scroll-area::-webkit-scrollbar {
  width: 8px;
}

.my-posts-scroll-area::-webkit-scrollbar-thumb {
  background: rgba(118, 150, 157, 0.28);
  border-radius: 999px;
}

.my-posts-scroll-area::-webkit-scrollbar-track {
  background: rgba(235, 242, 240, 0.72);
  border-radius: 999px;
}

.my-post-card {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 22px;
  border-radius: 24px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.95) 0%, rgba(245, 250, 248, 0.96) 100%);
  border: 1px solid rgba(118, 150, 157, 0.14);
  box-shadow: 0 16px 34px rgba(110, 142, 150, 0.08);
}

.my-post-card-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
}

.my-post-card-head {
  display: flex;
  flex-direction: column;
  gap: 10px;
  flex: 1;
  min-width: 0;
}

.my-post-card-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.my-post-card-top strong {
  display: block;
  color: #2b4147;
  font-size: 1.08rem;
}

.my-post-delete-btn {
  width: auto !important;
  min-width: 128px;
  flex-shrink: 0;
}

.my-post-feedback-card {
  margin-top: 4px;
}

.social-draft-preview {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  padding: 14px 16px;
  border-radius: 16px;
  background: rgba(248, 251, 249, 0.96);
}

.social-preview-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.social-preview-label {
  color: #73878d;
  font-size: 0.9rem;
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
  gap: 18px;
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

.add-friend-section .action-btn {
  width: auto;
  padding: 8px 24px;
  font-size: 0.95rem;
}

.search-input {
  padding: 8px 15px;
  border-radius: var(--radius-full);
  border: 1px solid rgba(0,0,0,0.1);
  background: rgba(255,255,255,0.6);
  outline: none;
  min-width: 200px;
}

.friends-dashboard {
  flex: 1;
  min-height: 0;
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 20px;
}

.friends-dashboard.detail-open {
  grid-template-columns: minmax(280px, 1.05fr) minmax(260px, 0.92fr);
}

.friends-list-panel,
.friend-detail-panel {
  min-height: 0;
  display: flex;
  flex-direction: column;
  padding: 18px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.86) 0%, rgba(245, 250, 248, 0.92) 100%);
  border: 1px solid rgba(118, 150, 157, 0.14);
}

.friends-list-panel-header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
  margin-bottom: 18px;
}

.friend-panel-hint {
  margin: 6px 0 0;
  color: #768f96;
  line-height: 1.6;
  font-size: 0.9rem;
}

.friend-count-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 6px 14px;
  border-radius: 999px;
  background: rgba(118, 150, 157, 0.12);
  color: #45616a;
  font-size: 0.85rem;
  white-space: nowrap;
}

.friends-list {
  flex: 1;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding-right: 6px;
}

.friend-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 14px;
  padding: 16px 18px;
  background: rgba(255,255,255,0.6);
  border-radius: var(--radius-lg);
  cursor: pointer;
  transition: all 0.3s;
  border: 1px solid transparent;
}

.friend-item:hover {
  background: rgba(255,255,255,0.9);
  transform: translateY(-2px);
  box-shadow: 0 12px 28px rgba(118, 150, 157, 0.14);
}

.friend-item.active {
  border-color: rgba(118, 150, 157, 0.28);
  background: linear-gradient(135deg, rgba(222, 241, 234, 0.92), rgba(250, 252, 251, 0.96));
  box-shadow: 0 14px 30px rgba(118, 150, 157, 0.16);
}

.friend-item .action-btn {
  width: auto;
  padding: 6px 18px;
  font-size: 0.9rem;
  height: auto;
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
  overflow: hidden;
  flex-shrink: 0;
}

.friend-details {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.friend-name {
  font-weight: bold;
  color: var(--color-text-primary);
}

.friend-id {
  font-size: 0.8rem;
  color: #777;
}

.friend-desc {
  font-size: 0.82rem;
  color: #69828a;
  line-height: 1.5;
}

.friend-item-side {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 8px;
  flex-shrink: 0;
}

.friend-intimacy {
  display: flex;
  gap: 5px;
}

.friend-score-label {
  font-size: 0.82rem;
  color: #56727a;
  font-weight: 600;
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

.search-results-panel,
.friend-requests-panel {
  padding: 20px;
  background: rgba(255, 255, 255, 0.6);
  border-radius: var(--radius-lg);
  border: 1px solid rgba(118, 150, 157, 0.15);
}

.sub-section-title {
  margin: 0 0 16px 0;
  font-size: 1.1rem;
  color: #3f5b63;
  font-weight: 600;
}

.search-results-list,
.friend-requests-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.request-actions {
  display: flex;
  gap: 10px;
}

.friend-avatar img {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  object-fit: cover;
  display: block;
}

.friend-detail-card {
  display: flex;
  flex-direction: column;
  gap: 14px;
  min-height: 100%;
}

.friend-detail-toolbar {
  display: flex;
  justify-content: flex-end;
}

.friend-detail-close {
  border: 1px solid rgba(118, 150, 157, 0.18);
  background: rgba(255, 255, 255, 0.88);
  color: #5d757c;
  border-radius: 999px;
  padding: 5px 12px;
  font-size: 0.8rem;
  cursor: pointer;
  transition: all 0.2s ease;
}

.friend-detail-close:hover {
  background: rgba(245, 250, 248, 1);
  color: #405b62;
}

.friend-detail-top {
  display: flex;
  align-items: flex-start;
  gap: 14px;
}

.friend-detail-avatar {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  background: var(--color-accent-sage);
  color: white;
  display: flex;
  justify-content: center;
  align-items: center;
  font-size: 2rem;
  overflow: hidden;
  flex-shrink: 0;
  box-shadow: 0 10px 22px rgba(118, 150, 157, 0.18);
}

.friend-detail-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.friend-detail-main {
  flex: 1;
  min-width: 0;
}

.friend-detail-heading {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
}

.friend-detail-heading h4 {
  margin: 0;
  font-size: 1.2rem;
  color: #30464d;
}

.friend-detail-heading p {
  margin: 5px 0 0;
  color: #6d868d;
  font-size: 0.84rem;
}

.friendship-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 5px 10px;
  border-radius: 999px;
  background: rgba(255, 192, 203, 0.2);
  color: #b45d78;
  font-size: 0.78rem;
  font-weight: 600;
  white-space: nowrap;
}

.friend-bio {
  margin: 10px 0 0;
  color: #50666d;
  line-height: 1.6;
  font-size: 0.84rem;
  white-space: pre-wrap;
}

.friend-detail-stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.friend-stat-card {
  padding: 12px;
  border-radius: var(--radius-md);
  background: rgba(247, 250, 249, 0.88);
  border: 1px solid rgba(118, 150, 157, 0.12);
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.friend-stat-card span {
  color: #7b9197;
  font-size: 0.76rem;
}

.friend-stat-card strong {
  color: #2f4850;
  font-size: 1rem;
}

.friendship-progress-card {
  padding: 14px;
  border-radius: var(--radius-md);
  background: linear-gradient(135deg, rgba(255, 243, 247, 0.9), rgba(245, 250, 248, 0.92));
  border: 1px solid rgba(255, 196, 211, 0.4);
}

.friendship-progress-header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  color: #7d6270;
  font-size: 0.8rem;
  margin-bottom: 10px;
}

.detail-intimacy {
  justify-content: center;
  margin-bottom: 10px;
}

.friendship-progress-track {
  width: 100%;
  height: 8px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.9);
  overflow: hidden;
}

.friendship-progress-bar {
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #ff8bb0, #f7b6c7);
  box-shadow: 0 8px 20px rgba(255, 139, 176, 0.25);
}

.large-petal {
  font-size: 1.2rem;
}

.friendship-description {
  margin: 10px 0 6px;
  color: #5d6f76;
  line-height: 1.55;
  font-size: 0.8rem;
}

.friendship-rule-tip {
  margin: 0;
  color: #8b7a82;
  font-size: 0.76rem;
}

.friend-basic-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.friend-basic-item {
  padding: 12px;
  border-radius: var(--radius-md);
  background: rgba(255,255,255,0.68);
  border: 1px solid rgba(118, 150, 157, 0.12);
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.friend-basic-item span {
  color: #7b9197;
  font-size: 0.76rem;
}

.friend-basic-item strong {
  color: #365057;
  line-height: 1.45;
  font-size: 0.9rem;
}

.friend-detail-actions {
  width: 100%;
  margin-top: auto;
  display: flex;
  justify-content: flex-start;
  gap: 12px;
}

.friend-chat-btn,
.friend-delete-btn {
  width: auto;
  min-width: 112px;
  max-width: none;
  padding: 8px 18px;
  font-size: 0.88rem;
  height: auto;
  flex: 0 0 auto;
}

.chat-tab {
  display: flex;
  flex-direction: column;
  height: 100%;
  gap: 18px;
  min-height: 0;
}

.chat-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
}

.chat-header-desc {
  margin: 6px 0 0;
  color: #69828a;
  line-height: 1.6;
}

.chat-dashboard {
  flex: 1;
  min-height: 0;
  display: grid;
  grid-template-columns: minmax(280px, 0.9fr) minmax(0, 1.4fr);
  gap: 20px;
}

.chat-friend-panel,
.chat-window-panel {
  min-height: 0;
  display: flex;
  flex-direction: column;
  padding: 18px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.86) 0%, rgba(245, 250, 248, 0.92) 100%);
  border: 1px solid rgba(118, 150, 157, 0.14);
}

.chat-status-text {
  color: #7b9198;
  font-size: 0.88rem;
}

.chat-friend-list,
.chat-message-list {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding-right: 6px;
}

.chat-friend-item {
  border: 1px solid transparent;
  background: rgba(255,255,255,0.6);
  border-radius: var(--radius-lg);
  padding: 16px 18px;
  text-align: left;
  cursor: pointer;
  transition: all 0.3s;
}

.chat-friend-item:hover {
  background: rgba(255,255,255,0.9);
  transform: translateY(-2px);
  box-shadow: 0 12px 28px rgba(118, 150, 157, 0.14);
}

.chat-friend-item.active {
  border-color: rgba(118, 150, 157, 0.28);
  background: linear-gradient(135deg, rgba(222, 241, 234, 0.92), rgba(250, 252, 251, 0.96));
  box-shadow: 0 14px 30px rgba(118, 150, 157, 0.16);
}

.chat-friend-topline {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
}

.chat-friend-topline h4 {
  margin: 0;
  color: var(--color-text-primary);
}

.chat-list-time {
  color: #7b9198;
  font-size: 0.82rem;
  white-space: nowrap;
}

.chat-preview-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.chat-last-message {
  flex: 1;
  min-width: 0;
  color: #56727a;
  font-size: 0.86rem;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.chat-unread-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 24px;
  height: 24px;
  padding: 0 8px;
  border-radius: 999px;
  background: #4f6f61;
  color: #fff;
  font-size: 0.75rem;
  font-weight: 600;
  flex-shrink: 0;
}

.chat-window-header {
  padding-bottom: 16px;
  margin-bottom: 16px;
  border-bottom: 1px solid rgba(118, 150, 157, 0.14);
}

.large-avatar {
  width: 52px;
  height: 52px;
  font-size: 1.4rem;
}

.chat-message-row {
  display: flex;
}

.chat-message-row.own {
  justify-content: flex-end;
}

.chat-message-bubble {
  max-width: min(78%, 520px);
  padding: 12px 14px;
  border-radius: 18px 18px 18px 6px;
  background: rgba(255, 255, 255, 0.82);
  border: 1px solid rgba(118, 150, 157, 0.14);
  box-shadow: 0 10px 24px rgba(118, 150, 157, 0.08);
}

.chat-message-row.own .chat-message-bubble {
  border-radius: 18px 18px 6px 18px;
  background: rgba(222, 241, 234, 0.94);
}

.chat-message-bubble p {
  margin: 0;
  color: var(--color-text-primary);
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
}

.chat-message-meta {
  display: inline-block;
  margin-top: 8px;
  color: #7b9198;
  font-size: 0.78rem;
}

.chat-composer {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid rgba(118, 150, 157, 0.14);
}

.chat-composer-left {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.chat-suggestion-btn {
  border: none;
  cursor: pointer;
  padding: 10px 14px;
  border-radius: 999px;
  background: rgba(54, 95, 77, 0.10);
  color: #365f4d;
  font: inherit;
  font-weight: 600;
}

.supportive-replies-modal-card {
  width: min(calc(100vw - 48px), 920px);
  max-width: 920px;
  max-height: min(calc(100vh - 48px), 760px);
  overflow: hidden;
}

.supportive-replies-modal-card .modal-header {
  position: relative;
  display: flex;
  align-items: flex-start;
  justify-content: flex-start;
  min-height: 64px;
  padding: 22px 72px 18px 24px;
  border-bottom: 1px solid rgba(118, 150, 157, 0.14);
}

.supportive-replies-modal-card .modal-header h3 {
  margin: 0;
  min-width: 0;
  color: #22353b;
  font-size: 1.34rem;
  line-height: 1.35;
  white-space: normal;
  word-break: break-word;
}

.supportive-replies-modal-card .close-btn {
  position: absolute;
  top: 18px;
  right: 18px;
  width: 38px;
  height: 38px;
  border: none;
  border-radius: 50%;
  background: rgba(111, 124, 130, 0.12);
  color: #33464d;
  font-size: 1.4rem;
  line-height: 1;
  cursor: pointer;
  transition: background 0.2s ease, transform 0.2s ease;
}

.supportive-replies-modal-card .close-btn:hover {
  background: rgba(111, 124, 130, 0.2);
  transform: scale(1.03);
}

.supportive-modal-body {
  padding: 20px;
  display: grid;
  gap: 18px;
}

.supportive-modal-intro {
  padding: 16px 18px;
  border-radius: 18px;
  background: linear-gradient(135deg, rgba(238, 247, 242, 0.92) 0%, rgba(245, 250, 248, 0.96) 100%);
  border: 1px solid rgba(118, 150, 157, 0.14);
}

.supportive-modal-intro p {
  margin: 8px 0 0;
  color: #5f757d;
  line-height: 1.7;
}

.supportive-kicker {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  padding: 0 10px;
  border-radius: 999px;
  background: rgba(54, 95, 77, 0.1);
  color: #365f4d;
  font-size: 0.76rem;
  font-weight: 700;
  letter-spacing: 0.06em;
  text-transform: uppercase;
}

.supportive-modal-layout {
  min-height: 0;
  display: grid;
  grid-template-columns: minmax(220px, 260px) minmax(0, 1fr);
  gap: 18px;
}

.supportive-categories-panel,
.supportive-suggestions-panel {
  min-height: 0;
}

.supportive-categories-panel {
  display: grid;
  gap: 12px;
}

.supportive-category {
  border: 1px solid rgba(118, 150, 157, 0.14);
  cursor: pointer;
  padding: 14px 16px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.82);
  color: #365f4d;
  font: inherit;
  display: grid;
  gap: 6px;
  text-align: left;
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease, background 0.2s ease;
}

.supportive-category strong {
  font-size: 0.98rem;
}

.supportive-category span {
  color: #6c8289;
  font-size: 0.85rem;
  line-height: 1.6;
}

.supportive-category.active {
  background: linear-gradient(135deg, #edf6f0 0%, #f8fbf9 100%);
  border-color: rgba(77, 116, 98, 0.34);
  box-shadow: 0 16px 28px rgba(77, 116, 98, 0.12);
  transform: translateY(-1px);
}

.supportive-panel-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 14px;
}

.supportive-panel-head h4 {
  margin: 8px 0 0;
  color: #29453b;
  font-size: 1.16rem;
}

.supportive-panel-head p {
  margin: 0;
  max-width: 280px;
  color: #6b8088;
  line-height: 1.7;
  font-size: 0.9rem;
}

.supportive-suggestions-panel {
  display: flex;
  flex-direction: column;
  padding: 18px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.56);
  border: 1px solid rgba(118, 150, 157, 0.12);
  overflow: hidden;
}

.supportive-suggestions {
  display: grid;
  gap: 12px;
  max-height: 420px;
  overflow-y: auto;
  padding-right: 6px;
}

.supportive-chip {
  border: 1px solid rgba(118, 150, 157, 0.14);
  background: rgba(255, 255, 255, 0.92);
  border-radius: 18px;
  padding: 14px 16px;
  cursor: pointer;
  font: inherit;
  color: #28444c;
  text-align: left;
  display: grid;
  gap: 8px;
  transition: transform 0.2s ease, box-shadow 0.2s ease, background 0.2s ease;
}

.supportive-chip strong {
  color: #28444c;
  font-weight: 600;
}

.supportive-chip span {
  color: #6d828a;
  font-size: 0.82rem;
}

.supportive-chip:hover {
  background: rgba(244, 250, 246, 0.98);
  transform: translateY(-1px);
  box-shadow: 0 16px 28px rgba(118, 150, 157, 0.1);
}

.chat-textarea {
  width: 100%;
  min-height: 74px;
  resize: none;
  border-radius: 18px;
  border: 1px solid rgba(118, 150, 157, 0.18);
  background: rgba(255,255,255,0.72);
  padding: 14px 16px;
  outline: none;
  box-sizing: border-box;
  font: inherit;
  color: var(--color-text-primary);
  line-height: 1.6;
}

.chat-composer-footer {
  margin-top: 12px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.friend-chat-send-btn {
  width: auto;
  min-width: 108px;
  padding: 10px 22px;
}

@media (max-width: 1120px) {
  .friends-dashboard.detail-open {
    grid-template-columns: 1fr;
  }

  .chat-dashboard {
    grid-template-columns: 1fr;
  }

  .friend-detail-stats {
    grid-template-columns: 1fr;
  }

  .friend-basic-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .friends-header,
  .friends-list-panel-header,
  .friend-detail-heading,
  .chat-header,
  .chat-friend-topline,
  .chat-composer-footer,
  .feedback-tab-header,
  .supportive-panel-head {
    flex-direction: column;
    align-items: flex-start;
  }

  .add-friend-section {
    width: 100%;
    flex-wrap: wrap;
  }

  .search-input {
    min-width: 0;
    flex: 1;
  }

  .friend-item {
    align-items: flex-start;
  }

  .friend-item-side {
    align-items: flex-start;
  }

  .friend-detail-top {
    flex-direction: column;
  }

  .friend-detail-actions {
    justify-content: stretch;
  }

  .supportive-modal-layout {
    grid-template-columns: 1fr;
  }

  .supportive-panel-head p {
    max-width: none;
  }


  .friend-chat-btn,
  .friend-delete-btn {
    width: 100%;
  }

  .friend-chat-send-btn {
    width: 100%;
  }

  .chat-message-bubble {
    max-width: 92%;
  }
}

.danger-btn {
  background: #ff5252;
  color: white;
}

.danger-btn:hover {
  background: #ff1744;
}

/* === Music Tab Styles === */
.music-tab {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.music-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 18px;
  gap: 16px;
}

.music-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.music-main-btn {
  min-width: 128px;
  border-radius: 999px;
  padding: 10px 20px;
  font-weight: 600;
}

.music-content {
  flex: 1;
  padding: 10px 0 0;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.music-overview-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
  gap: 14px;
}

.music-overview-card {
  padding: 18px 20px;
  border-radius: 20px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.95) 0%, rgba(245, 250, 248, 0.9) 100%);
  border: 1px solid rgba(107, 140, 147, 0.14);
  box-shadow: 0 12px 28px rgba(63, 89, 98, 0.08);
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.music-overview-card strong {
  font-size: 1.9rem;
  line-height: 1;
  color: #22353b;
}

.music-overview-card p {
  margin: 0;
  font-size: 0.88rem;
  line-height: 1.6;
  color: #61777e;
}

.overview-label {
  font-size: 0.8rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: #72919a;
}

.music-sections-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
  align-items: start;
}

.section-meta {
  font-size: 0.86rem;
  color: #4f6870;
  font-weight: 600;
  padding: 6px 12px;
  min-width: 72px;
  min-height: 34px;
  border-radius: 999px;
  background: rgba(117, 151, 160, 0.12);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  white-space: nowrap;
  line-height: 1;
  flex-shrink: 0;
  writing-mode: horizontal-tb;
}

.music-showcase-card {
  height: 540px;
  padding: 22px;
  border-radius: 24px;
  border: 1px solid rgba(108, 140, 149, 0.14);
  box-shadow: 0 18px 36px rgba(57, 84, 92, 0.08);
  display: flex;
  flex-direction: column;
  gap: 18px;
  overflow: hidden;
}

.uploads-card {
  background: linear-gradient(180deg, rgba(251, 254, 252, 0.98) 0%, rgba(241, 249, 245, 0.96) 100%);
}

.likes-card {
  background: linear-gradient(180deg, rgba(255, 251, 251, 0.98) 0%, rgba(253, 242, 246, 0.96) 100%);
}

.collections-card {
  background: linear-gradient(180deg, rgba(251, 252, 255, 0.98) 0%, rgba(242, 246, 255, 0.96) 100%);
}

.playlists-card {
  background: linear-gradient(180deg, rgba(250, 252, 251, 0.98) 0%, rgba(239, 247, 243, 0.96) 100%);
}

.blocked-card {
  background: linear-gradient(180deg, rgba(255, 249, 249, 0.98) 0%, rgba(252, 241, 241, 0.96) 100%);
}

.music-showcase-header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
}

.music-showcase-header > div {
  min-width: 0;
  flex: 1;
}

.music-showcase-header h4 {
  margin: 6px 0 8px;
  font-size: 1.28rem;
  color: #203239;
}

.music-showcase-header p {
  margin: 0;
  color: #60777e;
  line-height: 1.65;
  font-size: 0.92rem;
}

.music-card-kicker {
  font-size: 0.78rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: #7f97a0;
}

.music-track-stack {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.music-track-scroll-area {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding-right: 6px;
}

.music-track-card {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 14px;
  align-items: center;
  padding: 14px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.74);
  border: 1px solid rgba(113, 144, 152, 0.1);
  transition: transform 0.22s ease, box-shadow 0.22s ease, background 0.22s ease;
}

.music-track-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 12px 24px rgba(63, 89, 98, 0.08);
  background: rgba(255, 255, 255, 0.94);
}

.track-main {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.track-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.track-title-row strong {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 1rem;
  color: #23363d;
}

.track-duration {
  flex-shrink: 0;
  color: #6d848c;
  font-size: 0.82rem;
  font-weight: 700;
}

.track-artist-name {
  color: #5f7077;
  font-size: 0.9rem;
}

.track-tag-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.track-action-row {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  flex-wrap: wrap;
}

.small-tag {
  background: rgba(var(--color-accent-sage-rgb), 0.22);
  color: var(--color-accent-sage);
  padding: 5px 10px;
  border-radius: 999px;
  font-size: 0.78rem;
  font-weight: 600;
  display: inline-flex;
  align-items: center;
}

.subtle-tag {
  background: rgba(111, 126, 160, 0.12);
  color: #5b6f96;
}

.blocked-tag {
  background: rgba(226, 93, 93, 0.14);
  color: #c14d4d;
}

.compact-empty-state {
  flex: 1;
  min-height: 180px;
  border-radius: 18px;
  border: 1px dashed rgba(112, 144, 152, 0.26);
  background: rgba(255, 255, 255, 0.48);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  text-align: center;
  color: #6a7f86;
}

.remove-tag {
  margin-left: 5px;
  cursor: pointer;
  font-weight: bold;
}

.icon-btn {
  background: rgba(243, 249, 247, 0.92);
  border: 1px solid rgba(86, 117, 126, 0.14);
  width: 36px;
  height: 36px;
  border-radius: 12px;
  cursor: pointer;
  font-size: 1rem;
  opacity: 0.95;
  transition: opacity 0.2s, transform 0.2s, border-color 0.2s, background 0.2s;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.icon-btn:hover {
  opacity: 1;
  transform: translateY(-1px);
  border-color: rgba(66, 98, 108, 0.36);
  background: rgba(255, 255, 255, 0.98);
}

.icon-btn.danger:hover {
  color: #ff5252;
}

.active-icon {
  opacity: 1;
}

.playlist-carousel-shell {
  flex: 1;
  min-height: 0;
  overflow-x: auto;
  overflow-y: hidden;
  padding-bottom: 6px;
  scroll-snap-type: x mandatory;
  scroll-behavior: smooth;
  scroll-padding-left: 2px;
}

.playlist-carousel {
  display: grid;
  grid-auto-flow: column;
  grid-auto-columns: minmax(220px, calc(50% - 8px));
  gap: 14px;
  transition: transform 0.35s ease;
}

.playlist-preview-card {
  background: rgba(255,255,255,0.82);
  border-radius: 18px;
  padding: 18px 16px;
  border: 1px solid rgba(112, 144, 152, 0.12);
  display: flex;
  flex-direction: column;
  gap: 12px;
  transition: transform 0.24s, box-shadow 0.24s, border-color 0.24s;
  cursor: pointer;
  scroll-snap-align: start;
  min-height: 100%;
}

.playlist-preview-card:hover,
.playlist-preview-card.active {
  transform: translateY(-4px);
  box-shadow: var(--shadow-md);
  background: rgba(255,255,255,0.95);
  border-color: rgba(97, 132, 140, 0.28);
}

.playlist-info {
  display: flex;
  flex-direction: column;
  gap: 6px;
  flex: 1;
}

.playlist-info h4 {
  font-size: 1.02rem;
  margin: 0;
  color: #22353b;
}

.playlist-info p {
  margin: 0;
  font-size: 0.86rem;
  line-height: 1.55;
  color: #64797f;
  min-height: 72px;
}

.playlist-info span {
  font-size: 0.82rem;
  color: #667d84;
}

.playlist-card-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
}

.slim-btn {
  height: 34px;
  border-radius: 999px;
  padding: 0 14px;
}

.feedback-refresh-btn {
  width: auto;
  min-width: 72px;
  padding-inline: 12px;
  font-size: 0.92rem;
  flex: 0 0 auto;
}

.delete-pl-btn {
  border-radius: 10px;
}

.always-show {
  display: inline-flex !important;
}

.playlist-focus-label {
  font-size: 0.78rem;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: #7b939b;
  font-weight: 700;
}

.playlist-focus-meta {
  color: #627981;
  font-size: 0.84rem;
  font-weight: 600;
}

.playlist-detail-modal-card.modal-content {
  width: min(calc(100vw - 48px), 620px);
  max-width: 620px;
}

.social-modal-card.modal-content {
  width: min(calc(100vw - 48px), 560px);
}

.playlist-detail-modal-title h3 {
  margin: 6px 0 6px;
}

.playlist-detail-modal-title p {
  margin: 0;
  color: #64797f;
  line-height: 1.6;
}

.playlist-detail-modal-meta {
  margin-bottom: 8px;
}

.playlist-track-scroll-area {
  max-height: min(50vh, 420px);
  overflow-y: auto;
  padding-right: 6px;
}

.info-textarea {
  min-height: 108px;
  resize: vertical;
}

.playlist-track-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.playlist-track-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  padding: 12px 14px;
  border-radius: 16px;
  background: rgba(247, 251, 249, 0.92);
}

.playlist-track-main {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.playlist-track-main strong {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #23363d;
}

.playlist-track-main span {
  color: #667b82;
  font-size: 0.88rem;
}

.playlist-track-side {
  display: flex;
  align-items: center;
  gap: 10px;
  color: #6b828a;
  font-size: 0.84rem;
}

.music-track-scroll-area::-webkit-scrollbar,
.playlist-track-scroll-area::-webkit-scrollbar,
.playlist-carousel-shell::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}

.music-track-scroll-area::-webkit-scrollbar-thumb,
.playlist-track-scroll-area::-webkit-scrollbar-thumb,
.playlist-carousel-shell::-webkit-scrollbar-thumb {
  background: rgba(118, 150, 157, 0.34);
  border-radius: 999px;
}

.music-track-scroll-area::-webkit-scrollbar-track,
.playlist-track-scroll-area::-webkit-scrollbar-track,
.playlist-carousel-shell::-webkit-scrollbar-track {
  background: rgba(235, 242, 240, 0.72);
  border-radius: 999px;
}

@media (max-width: 1180px) {
  .diary-layout,
  .charts-grid,
  .feedback-layout {
    grid-template-columns: 1fr;
    flex-direction: column;
  }

  .diary-editor-section {
    width: 100%;
  }

  .music-overview-grid,
  .music-sections-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .playlist-carousel {
    grid-auto-columns: minmax(220px, calc(70% - 8px));
  }

  .social-layout {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 980px) {
  .page-shell {
    height: auto;
    min-height: calc(100vh - 120px);
    flex-direction: column;
  }

  .sidebar {
    width: 100%;
  }

  .space-nav {
    flex-direction: row;
    overflow-x: auto;
    overflow-y: hidden;
    padding-bottom: 4px;
  }

  .nav-item {
    flex: 0 0 auto;
    min-width: 148px;
  }

  .main-content {
    min-height: 0;
  }
}

@media (max-width: 900px) {
  .music-sections-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 760px) {
  .back-btn {
    top: 18px;
    left: 18px;
  }

  .page-shell {
    margin-top: 64px;
    height: auto;
    min-height: calc(100vh - 120px);
    flex-direction: column;
  }

  .sidebar {
    width: 100%;
  }

  .space-nav {
    flex-wrap: nowrap;
  }

  .music-header {
    flex-direction: column;
    align-items: stretch;
  }

  .music-overview-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .music-showcase-card {
    height: auto;
    min-height: 520px;
    padding: 18px;
  }

  .social-header,
  .social-post-top {
    flex-direction: column;
    align-items: stretch;
  }

  .music-track-card {
    grid-template-columns: minmax(0, 1fr);
  }

  .track-action-row {
    grid-column: 1 / -1;
    justify-content: flex-start;
  }

  .charts-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 560px) {
  .personal-space-page {
    padding: 24px 12px;
  }

  .main-content {
    padding: 18px;
  }

  .sidebar-brand {
    padding: 16px 14px;
  }

  .nav-item {
    padding: 14px 16px;
  }

  .music-overview-grid {
    grid-template-columns: 1fr;
  }

  .music-track-card,
  .playlist-track-row {
    display: flex;
    flex-direction: column;
    align-items: flex-start;
  }

  .track-title-row,
  .track-action-row,
  .playlist-track-side {
    width: 100%;
  }

  .track-title-row,
  .playlist-track-side {
    justify-content: space-between;
  }

  .playlist-carousel {
    grid-auto-columns: 88%;
  }

  .feedback-layout,
  .feedback-form-grid {
    grid-template-columns: 1fr;
  }

  .social-comment-editor {
    grid-template-columns: 1fr;
  }

  .feedback-actions,
  .feedback-card-head,
  .feedback-history-head {
    align-items: flex-start;
    flex-direction: column;
  }
}

.music-upload-form .form-group,
.music-create-form .form-group,
.social-create-form .form-group {
  margin: 0;
  text-align: left;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.music-modal-card.modal-content {
  width: min(calc(100vw - 48px), 560px);
  max-height: min(calc(100vh - 48px), 720px);
  overflow-y: auto;
  box-sizing: border-box;
  padding: 28px;
  border-radius: 22px;
  background: linear-gradient(180deg, #f9fdfb 0%, #f2f9f6 100%);
  border: 1px solid rgba(118, 150, 157, 0.2);
  box-shadow: 0 18px 44px rgba(45, 74, 84, 0.16);
}

.upload-modal-card.modal-content {
  width: min(calc(100vw - 48px), 720px);
  max-width: 720px;
  max-height: min(calc(100vh - 48px), 820px);
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.playlist-modal-card.modal-content {
  width: min(calc(100vw - 48px), 440px);
  max-width: 440px;
}

.music-modal-card .modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 22px;
  padding-bottom: 16px;
  border-bottom: 1px solid rgba(118, 150, 157, 0.16);
}

.music-modal-card .modal-header h3 {
  font-size: 1.35rem;
  color: #1f3036;
  margin: 0;
}

.music-modal-card .close-btn {
  width: 38px;
  height: 38px;
  border: none;
  border-radius: 50%;
  background: rgba(111, 124, 130, 0.12);
  color: #33464d;
  font-size: 1.4rem;
  line-height: 1;
  cursor: pointer;
  transition: background 0.2s ease, transform 0.2s ease;
}

.music-modal-card .close-btn:hover {
  background: rgba(111, 124, 130, 0.2);
  transform: scale(1.03);
}

.upload-modal-card .modal-body {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding-right: 8px;
  margin-right: -8px;
}

.modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 3000;
  background: rgba(22, 35, 40, 0.32);
  backdrop-filter: blur(4px);
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 20px;
}

.modal-content {
  width: 100%;
  max-height: calc(100vh - 40px);
  overflow-y: auto;
}

.music-upload-form,
.music-create-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.music-upload-form label {
  display: inline-block;
  margin-bottom: 8px;
  color: #2d3c42;
  font-weight: 600;
}

.music-upload-form .info-input,
.music-create-form .info-input {
  min-height: 46px;
  border-radius: 14px;
  border: 1px solid rgba(124, 150, 156, 0.36);
  background: rgba(255, 255, 255, 0.94);
}

.hidden-file-input {
  display: none;
}

.file-select-wrap {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.selected-file-name {
  flex: 1;
  min-width: 0;
  color: #4c636b;
  font-size: 0.92rem;
  max-width: none;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  padding: 12px 14px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid rgba(124, 150, 156, 0.2);
}

.tag-input-group {
  display: flex;
  gap: 12px;
  margin-bottom: 10px;
}

.tag-input-group.stack {
  flex-direction: column;
}

.tag-action-row {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.music-upload-ai-caption {
  margin: 0 0 10px;
  padding: 10px 12px;
  border-radius: 12px;
  background: rgba(140, 133, 255, 0.1);
  color: #4a458a;
  font-size: 0.9rem;
  line-height: 1.6;
  max-height: 240px;
  overflow-y: auto;
  white-space: pre-wrap;
  word-break: break-word;
}

.music-upload-cover-preview {
  min-height: 112px;
  border: 1px dashed rgba(124, 150, 156, 0.28);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.72);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 12px;
}

.music-upload-cover-image {
  width: 96px;
  height: 96px;
  border-radius: 16px;
  object-fit: cover;
}

.music-upload-cover-placeholder {
  color: #6b7d77;
  font-size: 0.92rem;
  text-align: center;
}

.current-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.ai-btn {
  background: linear-gradient(135deg, #8c85ff, #a48dff);
  color: #fff;
  border: none;
}

.ai-btn:hover {
  background: linear-gradient(135deg, #7f79f2, #9681ef);
}

.full-width-btn {
  width: 100%;
  margin-bottom: 10px;
  justify-content: flex-start;
}

.add-to-pl-row {
  width: 100%;
  border: none;
  background: transparent;
  cursor: pointer;
  text-align: left;
}

.add-to-pl-row:hover {
  background: rgba(255, 255, 255, 0.6);
}

.add-icon {
  font-size: 1.2rem;
  opacity: 0.5;
  transition: opacity 0.2s;
}

.add-to-pl-row:hover .add-icon {
  opacity: 1;
}

.playlist-select-list {
  max-height: 300px;
  overflow-y: auto;
}

.modal-btn {
  border: none;
  border-radius: 14px;
  min-height: 44px;
  padding: 0 18px;
  font-weight: 700;
  font-size: 0.95rem;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s, opacity 0.2s;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.modal-btn:hover {
  transform: translateY(-1px);
}

.modal-btn:disabled {
  opacity: 0.58;
  cursor: not-allowed;
  transform: none;
}

.primary-btn {
  background: #6f7c82;
  color: #fff;
  box-shadow: 0 8px 18px rgba(77, 95, 101, 0.2);
}

.primary-btn:hover {
  background: #5f6c72;
}

.secondary-btn {
  background: #f9fdfc;
  color: #203039;
  border: 1px solid rgba(74, 98, 106, 0.32);
}

.secondary-btn:hover {
  background: #ffffff;
}

.large-btn {
  min-width: 165px;
}

.modal-actions {
  display: flex;
  gap: 12px;
  margin-top: 6px;
}

.modal-actions .modal-btn {
  flex: 1;
}

@media (max-width: 640px) {
  .modal-overlay {
    padding: 16px;
  }

  .music-modal-card.modal-content,
  .playlist-modal-card.modal-content {
    width: calc(100vw - 32px);
    max-width: none;
    max-height: calc(100vh - 32px);
    padding: 20px 18px;
    border-radius: 18px;
  }

  .music-modal-card .modal-header {
    margin-bottom: 18px;
    padding-bottom: 14px;
  }

  .file-select-wrap,
  .tag-action-row,
  .modal-actions {
    flex-direction: column;
  }

  .large-btn,
  .modal-actions .modal-btn,
  .tag-action-row .modal-btn {
    width: 100%;
  }
}

</style>
