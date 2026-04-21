import { createRouter, createWebHistory } from 'vue-router'

import LandingPage from '@/views/LandingPage.vue'
import LoginPage from '@/views/LoginPage.vue'
import ServicePage from '@/views/ServicePage.vue'
import MusicPlayerPage from '@/views/MusicPlayerPage.vue'

const routes = [
  { path: '/', name: 'landing', component: LandingPage },
  { path: '/login', name: 'login', component: LoginPage },
  { path: '/service', name: 'service', component: ServicePage },
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
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior() {
    return { top: 0 }
  },
})

export default router
