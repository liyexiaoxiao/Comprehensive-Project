import { createRouter, createWebHistory } from 'vue-router';
import Meditation from "@/views/Meditation.vue";
import Home from '@/views/Home.vue';

const routes = [
    { path: '/', component: Home },
    { path: '/meditation', component: Meditation }
]

const router = createRouter({
    history: createWebHistory(),
    routes,
});

export default router;