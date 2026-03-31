import { createRouter, createWebHistory } from 'vue-router'

import LandingPage from '@/views/LandingPage.vue'
import LoginPage from '@/views/LoginPage.vue'
import ServicePage from '@/views/ServicePage.vue'

const routes = [
  { path: '/', name: 'landing', component: LandingPage },
  { path: '/login', name: 'login', component: LoginPage },
  { path: '/service', name: 'service', component: ServicePage },
  {
    path: '/meditation-room',
    name: 'meditation-room',
    component: () => import('@/views/Meditation.vue'),
  },
  {
    path: '/personal-space',
    redirect: { name: 'service', query: { panel: 'personal-space' } },
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
