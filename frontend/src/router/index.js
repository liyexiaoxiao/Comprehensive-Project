import { createRouter, createWebHistory } from 'vue-router'
import { AUTH_TOKEN_STORAGE_KEY } from '@/api/http'
import { getCurrentUserFromStorage, isAdminUser } from '@/api/user'

import LandingPage from '@/views/LandingPage.vue'
import LoginPage from '@/views/LoginPage.vue'
import ServicePage from '@/views/ServicePage.vue'
import MusicPlayerPage from '@/views/MusicPlayerPage.vue'
import AllPlaylistsPage from '@/views/AllPlaylistsPage.vue'

const routes = [
  { path: '/', name: 'landing', component: LandingPage },
  { path: '/login', name: 'login', component: LoginPage },
  { path: '/service', name: 'service', component: ServicePage },
  { path: '/playlists', name: 'all-playlists', component: AllPlaylistsPage },
  { path: '/music-player', name: 'music-player', component: MusicPlayerPage },
  {
    path: '/meditation-room',
    name: 'meditation-room',
    component: () => import('@/views/Meditation.vue'),
  },
  {
    path: '/personal-space',
    name: 'personal-space',
    component: () => import('@/views/PersonalSpace.vue'),
  },
  {
    path: '/admin',
    name: 'admin',
    component: () => import('@/views/AdminDashboard.vue'),
    meta: { requiresAdmin: true },
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior() {
    return { top: 0 }
  },
})

router.beforeEach((to) => {
  if (!to.meta?.requiresAdmin) {
    return true
  }

  const token = window.localStorage.getItem(AUTH_TOKEN_STORAGE_KEY)
  const currentUser = getCurrentUserFromStorage()

  if (!token) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }

  if (!isAdminUser(currentUser)) {
    return { name: 'service' }
  }

  return true
})

export default router
