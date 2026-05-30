<template>
  <div class="admin-page">
    <div class="admin-shell">
      <header class="admin-header glass-card">
        <div>
          <p class="admin-kicker">Admin Console</p>
          <h1>管理员端</h1>
          <p class="admin-subtitle">将用户管理、服务反馈、官方歌单与敏感词审查拆分为独立子页面，方便专注处理不同任务。</p>
        </div>
        <div class="admin-header-actions">
          <span class="admin-badge">{{ currentAdminName }}</span>
          <button class="header-action-btn" type="button" :disabled="isLoggingOut" @click="handleLogout">
            {{ isLoggingOut ? '退出中...' : '退出登录' }}
          </button>
          <RouterLink class="header-link" to="/service">返回服务页</RouterLink>
        </div>
      </header>

      <nav class="admin-nav glass-card">
        <RouterLink
          class="admin-nav-link"
          :class="{ active: route.path.startsWith('/admin/users') }"
          to="/admin/users"
        >
          用户管理
        </RouterLink>
        <RouterLink
          class="admin-nav-link"
          :class="{ active: route.path.startsWith('/admin/feedback') }"
          to="/admin/feedback"
        >
          服务反馈
        </RouterLink>
        <RouterLink
          class="admin-nav-link"
          :class="{ active: route.path.startsWith('/admin/official-playlists') }"
          to="/admin/official-playlists"
        >
          官方情绪歌单
        </RouterLink>
        <RouterLink
          class="admin-nav-link"
          :class="{ active: route.path.startsWith('/admin/censor-words') }"
          to="/admin/censor-words"
        >
          敏感词审查
        </RouterLink>
      </nav>

      <RouterView />
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { RouterLink, RouterView, useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getCurrentUserFromStorage } from '@/api/user'
import { logoutSession } from '@/api/session'

const router = useRouter()
const route = useRoute()
const currentUser = computed(() => getCurrentUserFromStorage())
const currentAdminName = computed(() => currentUser.value?.nickname || currentUser.value?.username || '管理员')
const isLoggingOut = ref(false)

const handleLogout = async () => {
  if (isLoggingOut.value) return

  try {
    isLoggingOut.value = true
    await logoutSession()
    ElMessage.success('已退出登录')
    router.push({ name: 'login' })
  } catch (error) {
    console.error('Logout failed:', error)
    ElMessage.error('退出登录失败，请稍后重试')
  } finally {
    isLoggingOut.value = false
  }
}
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
  flex-wrap: wrap;
}

.admin-kicker {
  margin: 0 0 6px;
  font-size: 0.85rem;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: #6e8f80;
}

.admin-header h1 {
  margin: 0;
}

.admin-subtitle {
  margin: 6px 0 0;
  color: #6a7c76;
}

.admin-header-actions {
  display: flex;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.admin-badge,
.header-link,
.header-action-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 8px 14px;
  border-radius: 999px;
  background: rgba(125, 159, 143, 0.14);
  color: #37564a;
  text-decoration: none;
}

.header-action-btn {
  border: none;
  font: inherit;
  cursor: pointer;
}

.header-action-btn:disabled {
  opacity: 0.65;
  cursor: not-allowed;
}

.admin-nav {
  padding: 12px;
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.admin-nav-link {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 1 1 140px;
  min-width: min(140px, 100%);
  padding: 12px 18px;
  border-radius: 16px;
  color: #365f4d;
  text-decoration: none;
  background: rgba(54, 95, 77, 0.08);
  transition: background-color 0.2s ease, color 0.2s ease, box-shadow 0.2s ease;
}

.admin-nav-link.active {
  color: #fff;
  background: #365f4d;
  box-shadow: 0 10px 24px rgba(54, 95, 77, 0.2);
}

@media (max-width: 960px) {
  .admin-page {
    padding: 18px;
  }

  .admin-header {
    padding: 20px 22px;
  }

  .admin-header-actions {
    width: 100%;
    justify-content: flex-start;
  }
}

@media (max-width: 720px) {
  .admin-page {
    padding: 14px;
  }

  .admin-header {
    flex-direction: column;
    align-items: stretch;
  }

  .admin-nav {
    flex-direction: column;
  }

  .admin-nav-link {
    width: 100%;
  }
}
</style>
