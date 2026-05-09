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
                      <button class="icon-btn" @click="openAddToPlaylistModal(track)" title="加入歌单">➕</button>
                      <button class="icon-btn danger" @click="musicStore.removeUploadedTrack(track.id)" title="删除">🗑️</button>
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
                      <button class="icon-btn" @click="musicStore.toggleCollect(track.id)" :title="musicStore.collectedTrackIds.includes(track.id) ? '取消收藏' : '收藏'">
                        <span :class="{ 'active-icon': musicStore.collectedTrackIds.includes(track.id) }">⭐</span>
                      </button>
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
                      <button class="icon-btn" @click="musicStore.toggleLike(track.id)" :title="musicStore.likedTrackIds.includes(track.id) ? '取消喜欢' : '喜欢'">
                        <span :class="{ 'active-icon': musicStore.likedTrackIds.includes(track.id) }">❤️</span>
                      </button>
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
            <button class="action-btn social-publish-btn" @click="openSocialComposer">发布</button>
          </div>

          <div class="social-layout">
            <div class="social-feed">
              <article v-for="post in socialPosts" :key="post.id" class="social-post-card glass-panel-inner">
                <div class="social-post-top">
                  <div class="social-author-block">
                    <div class="social-avatar">{{ getNameInitial(post.authorName) }}</div>
                    <div>
                      <div class="social-author-row">
                        <strong>{{ post.authorName }}</strong>
                        <span class="social-role-tag">{{ post.authorRole }}</span>
                      </div>
                      <span class="social-post-time">{{ post.timeLabel }}</span>
                    </div>
                  </div>
                  <span class="social-mood-pill">现在的情绪 · {{ post.mood }}</span>
                </div>

                <p class="social-post-content">{{ post.content }}</p>

                <div v-if="post.highlights?.length" class="social-highlight-row">
                  <span v-for="item in post.highlights" :key="item" class="social-highlight-chip"># {{ item }}</span>
                </div>

                <div class="social-post-actions">
                  <button class="social-action-chip" :class="{ active: post.likedByMe }" @click="toggleSocialLike(post)">
                    {{ post.likedByMe ? '已赞' : '点赞' }} · {{ post.likes }}
                  </button>
                  <span class="social-comment-total">{{ post.comments.length }} 条评论</span>
                </div>

                <div class="social-comment-list">
                  <div v-for="comment in post.comments" :key="comment.id" class="social-comment-item">
                    <span class="social-comment-author">{{ comment.author }}：</span>
                    <span class="social-comment-text">{{ comment.content }}</span>
                    <span class="social-comment-time">{{ comment.timeLabel }}</span>
                  </div>
                </div>

                <div class="social-comment-editor">
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

              <div v-if="socialPosts.length === 0" class="empty-state compact-empty-state">
                朋友圈暂时还没有动态，点击右上角发布第一条帖子吧！
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
                    <span>评论</span>
                  </div>
                  <div class="social-stat-item">
                    <strong>{{ socialSummary.likeCount }}</strong>
                    <span>点赞</span>
                  </div>
                  <div class="social-stat-item">
                    <strong>{{ friendsList.length }}</strong>
                    <span>好友</span>
                  </div>
                </div>
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
              <label>情绪标签</label>
              <div class="tag-input-group stack">
                <input type="text" v-model="uploadForm.tagInput" placeholder="输入情绪(如: 安静)" class="info-input" />
                <div class="tag-action-row">
                  <button class="modal-btn secondary-btn" @click="addTag">添加标签</button>
                  <button class="modal-btn ai-btn" @click="autoDetectTags">AI 辅助判别</button>
                </div>
              </div>
              <div class="current-tags">
                <span v-for="(tag, index) in uploadForm.tags" :key="index" class="small-tag">
                  {{ tag }} <span class="remove-tag" @click="uploadForm.tags.splice(index, 1)">&times;</span>
                </span>
              </div>
            </div>
            <div class="modal-actions">
              <button class="modal-btn secondary-btn" @click="closeMusicModal">取消</button>
              <button class="modal-btn primary-btn" @click="submitUpload" :disabled="!uploadForm.file || !uploadForm.title">确认上传</button>
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
        <div class="modal-content glass-panel">
          <div class="modal-header">
            <h3>加入歌单</h3>
            <button class="close-btn" @click="showAddToPlaylistModal = false">&times;</button>
          </div>
          <div class="modal-body">
            <div v-if="musicStore.customPlaylists.length === 0" class="empty-state">
              暂无歌单，请先创建歌单。
            </div>
            <div v-else class="playlist-select-list">
              <button
                v-for="pl in musicStore.customPlaylists" 
                :key="pl.id" 
                class="modal-btn secondary-btn full-width-btn"
                @click="confirmAddToPlaylist(pl.id)"
              >
                {{ pl.name }}
              </button>
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
                placeholder="例如：轻松、期待、平静"
              />
            </div>
            <div class="social-draft-preview">
              <span class="social-preview-label">预览标签</span>
              <span class="social-mood-pill">{{ socialDraft.mood.trim() || '未填写情绪' }}</span>
            </div>
            <div class="modal-actions">
              <button class="modal-btn secondary-btn" @click="closeSocialComposer">取消</button>
              <button
                class="modal-btn primary-btn"
                @click="submitSocialPost"
                :disabled="!socialDraft.content.trim() || !socialDraft.mood.trim()"
              >
                发布动态
              </button>
            </div>
          </div>
        </div>
      </div>

    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch, nextTick } from 'vue'
import * as echarts from 'echarts'
import { useMusicStore } from '@/stores/musicStore'
import { ElMessage } from 'element-plus'
import {
  getCurrentUserApi,
  getCurrentUserFromStorage,
  saveCurrentUserToStorage,
  updateMyProfileApi,
} from '@/api/user'
import { getMusicFileUrl, getMusicListApi } from '@/api/python'
import { buildRealMusicCategories, getRealMusicCover, readRemoteAudioDuration } from '@/utils/realMusic'

const musicStore = useMusicStore()
const realMusicFiles = ref([])
const realMusicDurations = ref({})

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

const mapUserToProfile = (user) => ({
  id: user?.userId ?? '',
  name: user?.nickname?.trim() || user?.username?.trim() || '未设置昵称',
  username: user?.username?.trim() || '',
  email: user?.email?.trim() || '',
  phone: user?.phone?.trim() || '',
  description: user?.bio?.trim() || '',
  avatar: user?.avatarUrl?.trim() || ''
})

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

const handleProfileAction = async () => {
  if (!isEditingProfile.value) {
    isEditingProfile.value = true
    return
  }

  isProfileSubmitting.value = true
  try {
    await updateMyProfileApi({
      nickname: userProfile.value.name.trim(),
      email: userProfile.value.email.trim() || null,
      phone: userProfile.value.phone.trim() || null,
      avatarUrl: userProfile.value.avatar || null,
      bio: userProfile.value.description.trim() || null,
    })

    await loadCurrentUserProfile()
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
  loadCurrentUserProfile()
  loadRealMusicFiles()
  if (currentTab.value === 'diary') {
    initDiaryWeeklyChart()
  } else if (currentTab.value === 'data') {
    initCharts()
  } else if (currentTab.value === 'meditation') {
    initMeditationCharts()
  }
})

// --- Tab 4: Music State ---
const activeMusicModal = ref('')
const showAddToPlaylistModal = ref(false)
const showPlaylistDetailModal = ref(false)
const newPlaylistName = ref('')
const newPlaylistDescription = ref('')
const trackToAdd = ref(null)
const activePlaylist = ref(null)
const audioInputRef = ref(null)
const playlistCarouselRef = ref(null)

const uploadForm = ref({
  file: null,
  title: '',
  artist: '',
  duration: 0,
  tagInput: '',
  tags: []
})

const realMusicLibrary = computed(() => {
  return buildRealMusicCategories(realMusicFiles.value, {
    likedIds: musicStore.likedTrackIds,
    collectedIds: musicStore.collectedTrackIds,
    durationMap: realMusicDurations.value,
  }).flatMap(category => category.tracks)
})

const likedTracksList = computed(() => {
  return musicStore.likedTrackIds.map(id => {
    return musicStore.uploadedTracks.find(t => t.id === id) || realMusicLibrary.value.find(t => t.id === id)
  }).filter(Boolean)
})

const collectedTracksList = computed(() => {
  return musicStore.collectedTrackIds.map(id => {
    return musicStore.uploadedTracks.find(t => t.id === id) || realMusicLibrary.value.find(t => t.id === id)
  }).filter(Boolean)
})

const playlistModalTracks = computed(() => activePlaylist.value?.tracks ?? [])

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
  const totalSeconds = playlist.tracks.reduce((sum, track) => sum + (track.duration || 0), 0)
  const totalMinutes = Math.max(1, Math.round(totalSeconds / 60))
  return `${playlist.tracks.length} 首 · ${totalMinutes} 分钟`
}

const loadRealMusicFiles = async () => {
  try {
    const response = await getMusicListApi()
    realMusicFiles.value = Array.isArray(response?.data?.music_files) ? response.data.music_files : []
    const durationEntries = await Promise.all(
      realMusicFiles.value.map(async (filename) => [filename, await readRemoteAudioDuration(getMusicFileUrl(filename))]),
    )
    realMusicDurations.value = Object.fromEntries(durationEntries)
  } catch (error) {
    realMusicFiles.value = []
    realMusicDurations.value = {}
    console.error('Personal space real music load failed:', error)
  }
}

const handleAudioSelect = (event) => {
  const file = event.target.files[0]
  if (file) {
    uploadForm.value.file = file
    if (!uploadForm.value.title) {
      uploadForm.value.title = file.name.replace(/\.[^/.]+$/, "") // Remove extension
    }
    const audio = document.createElement('audio')
    audio.src = URL.createObjectURL(file)
    audio.onloadedmetadata = () => {
      uploadForm.value.duration = audio.duration
    }
  }
}

const triggerAudioSelect = () => {
  audioInputRef.value?.click()
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
    tagInput: '',
    tags: []
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

const autoDetectTags = () => {
  ElMessage.info('AI 正在分析音频情绪特征...')
  setTimeout(() => {
    const mockAITags = ['平静', '治愈', '孤独']
    mockAITags.forEach(t => {
      if (!uploadForm.value.tags.includes(t)) {
        uploadForm.value.tags.push(t)
      }
    })
    ElMessage.success('AI 标签生成完成')
  }, 1000)
}

const submitUpload = () => {
  if (!uploadForm.value.file || !uploadForm.value.title) return
  
  const newTrack = {
    id: 'upload_' + Date.now(),
    title: uploadForm.value.title.trim(),
    artist: uploadForm.value.artist.trim(),
    duration: uploadForm.value.duration || 180,
    tags: [...uploadForm.value.tags],
    cover: getRealMusicCover(uploadForm.value.tags[0] || 'neutral'),
    file: uploadForm.value.file
  }
  
  musicStore.addUploadedTrack(newTrack)
  closeMusicModal()
  ElMessage.success('音乐上传成功')
}

const submitCreatePlaylist = () => {
  if (!newPlaylistName.value) return
  musicStore.createPlaylist(newPlaylistName.value, newPlaylistDescription.value.trim())
  newPlaylistName.value = ''
  newPlaylistDescription.value = ''
  closeMusicModal()
  ElMessage.success('歌单创建成功')
}

const openAddToPlaylistModal = (track) => {
  trackToAdd.value = track
  showAddToPlaylistModal.value = true
}

const confirmAddToPlaylist = (playlistId) => {
  if (trackToAdd.value) {
    musicStore.addTrackToPlaylist(playlistId, trackToAdd.value)
    ElMessage.success('已加入歌单')
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

const deletePlaylistAndReset = (playlistId) => {
  musicStore.deletePlaylist(playlistId)
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

const createSocialPost = (post) => ({
  ...post,
  highlights: Array.isArray(post.highlights) ? [...post.highlights] : [],
  comments: Array.isArray(post.comments) ? post.comments.map(comment => ({ ...comment })) : [],
  commentDraft: ''
})

const showSocialComposer = ref(false)
const socialDraft = ref({
  content: '',
  mood: ''
})
const socialPosts = ref([
  createSocialPost({
    id: 'post_1',
    authorName: '小明',
    authorRole: '好友',
    timeLabel: '10 分钟前',
    mood: '轻松',
    content: '今天下课后沿着操场走了一圈，风很舒服，感觉整个人都慢下来了一点。',
    highlights: ['散步', '晚风'],
    likes: 18,
    likedByMe: false,
    comments: [
      { id: 'comment_1', author: 'Alice', content: '听起来很治愈，我也想去走走。', timeLabel: '8 分钟前' },
      { id: 'comment_2', author: '我', content: '这种慢下来的时刻真的很难得。', timeLabel: '5 分钟前' }
    ]
  }),
  createSocialPost({
    id: 'post_2',
    authorName: 'Alice',
    authorRole: '好友',
    timeLabel: '32 分钟前',
    mood: '期待',
    content: '把这周的任务清单整理完了，晚上准备听点歌，给明天留一点盼头。',
    highlights: ['计划感', '夜晚歌单'],
    likes: 25,
    likedByMe: true,
    comments: [
      { id: 'comment_3', author: '李华', content: '完成清单的成就感最适合配音乐。', timeLabel: '20 分钟前' }
    ]
  }),
  createSocialPost({
    id: 'post_3',
    authorName: '李华',
    authorRole: '好友',
    timeLabel: '1 小时前',
    mood: '平静',
    content: '午后在图书馆坐了很久，什么都没急着做，只是把手头的内容一点点收完。',
    highlights: ['图书馆', '专注'],
    likes: 12,
    likedByMe: false,
    comments: [
      { id: 'comment_4', author: '小明', content: '这种节奏感很舒服。', timeLabel: '56 分钟前' }
    ]
  })
])

const socialSummary = computed(() => {
  return socialPosts.value.reduce((summary, post) => {
    summary.postCount += 1
    summary.commentCount += post.comments.length
    summary.likeCount += post.likes
    return summary
  }, {
    postCount: 0,
    commentCount: 0,
    likeCount: 0
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

const closeSocialComposer = () => {
  showSocialComposer.value = false
  resetSocialDraft()
}

const toggleSocialLike = (post) => {
  post.likedByMe = !post.likedByMe
  post.likes += post.likedByMe ? 1 : -1
}

const submitSocialComment = (post) => {
  const content = post.commentDraft.trim()
  if (!content) return

  post.comments.push({
    id: `comment_${Date.now()}_${Math.random().toString(16).slice(2, 6)}`,
    author: userProfile.value.name || '我',
    content,
    timeLabel: '刚刚'
  })
  post.commentDraft = ''
  ElMessage.success('评论成功')
}

const submitSocialPost = () => {
  const content = socialDraft.value.content.trim()
  const mood = socialDraft.value.mood.trim()
  if (!content || !mood) return

  socialPosts.value.unshift(createSocialPost({
    id: `post_${Date.now()}`,
    authorName: userProfile.value.name || '我',
    authorRole: '我',
    timeLabel: '刚刚',
    mood,
    content,
    highlights: [mood],
    likes: 0,
    likedByMe: false,
    comments: []
  }))
  closeSocialComposer()
  ElMessage.success('动态发布成功')
}

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
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
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
  gap: 10px;
  padding: 0;
  position: relative;
  z-index: 1;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 15px 20px;
  background: rgba(255, 255, 255, 0.2);
  border: 1px solid transparent;
  border-radius: 18px;
  color: #68807a;
  font-size: 1.05rem;
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
  width: 20px;
  height: 20px;
  padding: 9px;
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
  align-items: flex-start;
  gap: 16px;
}

.social-header-desc {
  margin: 8px 0 0;
  color: var(--color-text-secondary);
  line-height: 1.6;
}

.social-publish-btn,
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

.social-post-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  padding-top: 6px;
  border-top: 1px solid rgba(118, 150, 157, 0.12);
}

.social-action-chip {
  border: none;
  border-radius: 999px;
  padding: 8px 14px;
  background: rgba(118, 150, 157, 0.1);
  color: #436068;
  cursor: pointer;
  transition: all 0.2s ease;
}

.social-action-chip:hover,
.social-action-chip.active {
  background: rgba(118, 150, 157, 0.2);
  color: #29434a;
}

.social-comment-total {
  color: #71848b;
  font-size: 0.9rem;
}

.social-comment-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.social-comment-item {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
  padding: 10px 14px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.7);
}

.social-comment-author {
  color: #2b4147;
  font-weight: 600;
}

.social-comment-text {
  color: #4d6167;
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

.social-draft-preview {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  border-radius: 16px;
  background: rgba(248, 251, 249, 0.96);
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
  grid-template-columns: repeat(4, minmax(0, 1fr));
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
  .sidebar {
    width: 232px;
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

  .social-comment-editor {
    grid-template-columns: 1fr;
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
  max-width: 560px;
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
