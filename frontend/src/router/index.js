import { createRouter, createWebHistory } from 'vue-router'
import { getCurrentUserFromStorage, isAdminUser } from '@/api/user'
import { getStoredAuthToken } from '@/api/http'
import { resolveAuthenticatedRoute, restoreSession } from '@/api/session'

import LandingPage from '@/views/LandingPage.vue'
import LoginPage from '@/views/LoginPage.vue'
import ServicePage from '@/views/ServicePage.vue'
import MusicPlayerPage from '@/views/MusicPlayerPage.vue'
import AllPlaylistsPage from '@/views/AllPlaylistsPage.vue'

const routes = [
  { path: '/', name: 'landing', component: LandingPage },
  { path: '/login', name: 'login', component: LoginPage, meta: { guestOnly: true } },
  { path: '/service', name: 'service', component: ServicePage, meta: { requiresAuth: true } },
  { path: '/playlists', name: 'all-playlists', component: AllPlaylistsPage, meta: { requiresAuth: true } },
  { path: '/music-player', name: 'music-player', component: MusicPlayerPage, meta: { requiresAuth: true } },
  {
    path: '/meditation-room',
    name: 'meditation-room',
    component: () => import('@/views/Meditation.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/personal-space',
    name: 'personal-space',
    component: () => import('@/views/PersonalSpace.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/mini-missions',
    name: 'mini-missions',
    component: () => import('@/views/MiniMissions.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/admin',
    name: 'admin',
    component: () => import('@/views/AdminDashboard.vue'),
    meta: { requiresAuth: true, requiresAdmin: true },
    redirect: { name: 'admin-users' },
    children: [
      {
        path: 'users',
        name: 'admin-users',
        component: () => import('@/views/admin/AdminUsersPage.vue'),
      },
      {
        path: 'official-playlists',
        name: 'admin-official-playlists',
        component: () => import('@/views/admin/AdminOfficialPlaylistsPage.vue'),
      },
      {
        path: 'censor-words',
        name: 'admin-censor-words',
        component: () => import('@/views/admin/AdminCensorWordsPage.vue'),
      },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior() {
    return { top: 0 }
  },
})

router.beforeEach(async (to) => {
  const requiresAuth = to.matched.some((record) => record.meta?.requiresAuth || record.meta?.requiresAdmin)
  const requiresAdmin = to.matched.some((record) => record.meta?.requiresAdmin)
  const guestOnly = to.matched.some((record) => record.meta?.guestOnly)

  const token = getStoredAuthToken()
  if (!token) {
    if (requiresAuth) {
      return { name: 'login', query: { redirect: to.fullPath } }
    }
    return true
  }

  let currentUser = getCurrentUserFromStorage()
  try {
    currentUser = await restoreSession()
  } catch {
    if (requiresAuth) {
      return { name: 'login', query: { redirect: to.fullPath } }
    }
    return true
  }

  if (guestOnly) {
    return resolveAuthenticatedRoute(currentUser)
  }

  if (requiresAdmin && !isAdminUser(currentUser)) {
    return { name: 'service' }
  }

  return true
})

export default router
