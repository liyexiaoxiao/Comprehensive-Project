<template>
    <div id="background" class="background-layer"></div>

    <div class="meditation-page" :style="bgStyle">
        <div class="main-layout">
            <div class="nav">
                <button class="icon-button" :style="iconStyle" @click="returnHomepage" circle>
                    <font-awesome-icon icon="angle-left" />
                </button>
            </div>

            <div class="content">
                <div class="left">
                    <CircleTimer :colors="theme.colors" :total-time="selectedTime" key="circle-timer" />
                    
                    <button class="set-time-button" @click="showTimeOptions = !showTimeOptions" circle>
                        <font-awesome-icon icon="clock-rotate-left" />
                    </button>

                    <div class="set-time-wrapper">
                        <div v-if="showTimeOptions" class="login-modal-overlay" @click.self="showTimeOptions = false">
                            <div class="login-modal">
                                <div style="display: flex; flex-direction: row; gap: 12px;">
                                    <button v-for="option in timeOptions" :key="option" @click="selectTime(option)" class="login-btn">
                                        {{ option / 60 }} 分钟
                                    </button>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>

                <div class="right">
                    <div class="quote-section">
                        <QuotesDisplay />
                    </div>
                </div>
            </div>

            <div class="controls-section">
                <PlayerControls />
            </div>
        </div>
    </div>
</template>

<script setup>
import { ref, computed, onMounted, watch, onBeforeUnmount } from 'vue';
import { emotionThemes } from '@/utils/emotionThemes.js';

import Home from '@/views/Home.vue';
import CircleTimer from '@/components/CircleTimer.vue';
import QuotesDisplay from '@/components/QuotesDisplay.vue';
import PlayerControls from '@/components/PlayerControls.vue';

import axios from 'axios';
import { useRouter } from "vue-router";

const router = useRouter();

import * as THREE from 'three';
import FOG from 'vanta/dist/vanta.fog.min';

import { useSpeechStore } from '@/stores/speech'
const speechStore = useSpeechStore()

const emotions = ['joy', 'sadness', 'anger', 'fear', 'love', 'surprise', 'neutral']
const selectedEmotion = computed(() => speechStore.emotion)
const theme = computed(() => emotionThemes[selectedEmotion.value] || emotionThemes.neutral)

const vantaRef = ref(null);
let vantaEffect = null;

const showTimeOptions = ref(false)
const selectedTime = ref(180) // 默认 3 分钟
const timeOptions = [60, 180, 300, 600] // 秒数

const selectTime = (option) => {
    selectedTime.value = option;
    showTimeOptions.value = false;
}

// 整个容器背景样式，铺满屏幕
const bgStyle = computed(() => ({
    width: '100vw',
    height: '100vh',
    margin: 0,
    padding: 0,
    color: '#333',
    overflow: 'hidden', // 保证不出现黑边/滚动
    display: 'flex',
    justifyContent: 'center',
    alignItems: 'center',
}))

const iconStyle = computed(() => ({
    border: 'none',
    color: 'white',
    cursor: 'pointer',
    width: '40px',
    height: '40px',
    borderRadius: '50%', 
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    transition: 'background-color 0.3s ease',
    fontSize: '20px',
    boxShadow: '0 2px 6px rgba(0,0,0,0.15)',
    backgroundColor: theme.value.colors[0] || '#ffffff',
}))

onMounted(() => {
    initVanta();
});

const handleNodeClick = (nodeData) => {
  console.log('点击了节点：', nodeData.label)
}

onBeforeUnmount(() => {
    if (vantaEffect) vantaEffect.destroy();
});

watch(theme, () => {
    if (vantaEffect) vantaEffect.destroy();
    initVanta();
});

function initVanta() {
    vantaEffect = FOG({
    el: document.querySelector("#background"),
    THREE: THREE,
    mouseControls: true,
    touchControls: true,
    gyroControls: false,
    minHeight: 200.0,
    minWidth: 200.0,
    highlightColor: hexToInt(theme.value.colors[0]),
    midtoneColor: hexToInt(theme.value.colors[1]),
    lowlightColor: hexToInt(theme.value.colors[2] || theme.value.colors[1]),
    baseColor: 0xffebeb,
    blurFactor: 0.6,
    zoom: 1,
    speed: 1,
  });
}

function hexToInt(hex) {
    // 支持 "#rrggbb" 或 "rrggbb"
    return parseInt(hex.replace('#', ''), 16);
}

const returnHomepage = () => {
    router.push('/')  // 返回首页
}
</script>

<style scoped>
body {
    overflow: hidden;
}

.background-layer {
    position: fixed;
    top: 0; left: 0;
    width: 100vw;
    height: 100vh;
    z-index: -1;
}

.meditation-page {
    font-family: var(--font-sans);
    text-align: center;
    width: 100vw;
    height: 100vh;
    overflow: hidden;
    display: flex;
    justify-content: center;
    align-items: center;
}

.main-layout {
    width: 100%;
    height: 100%;
    display: flex;
    flex-direction: column;
}

.nav {
    position: fixed;
    top: 40px;
    left: 40px;
    z-index: 10;
}

.icon-button {
    backdrop-filter: blur(8px);
    border: 1px solid rgba(255,255,255,0.4) !important;
}
.icon-button:hover {
    transform: scale(1.1);
}

.content {
    flex: 1;
    display: flex;
    flex-direction: row;
    padding: 2rem;
    box-sizing: border-box;
    overflow: hidden;
}

.left {
    position: relative;
    flex: 1.5;
    display: flex;
    justify-content: center;
    flex-direction: column;
    align-items: center;
    overflow: hidden;
}

.left .set-time-wrapper {
    position: absolute;
    top: calc(50% + 250px);
    left: 50%;
    transform: translateX(-50%);
}

.left .set-time-button {
    position: fixed;
    top: calc(50% + 250px);
    left: 50%;
    transform: translateX(-50%);
    background-color: var(--color-bg-glass);
    backdrop-filter: blur(12px);
    border: 1px solid rgba(255,255,255,0.4);
    border-radius: 50%;
    width: 48px;
    height: 48px;
    display: flex;
    align-items: center;
    justify-content: center;
    color: var(--color-text-primary);
    font-size: 20px;
    cursor: pointer;
    box-shadow: var(--shadow-soft);
    transition: all var(--transition-fast);
}
.left .set-time-button:hover {
    background-color: rgba(255,255,255,0.8);
    transform: translateX(-50%) scale(1.05);
}

.left .login-modal-overlay {
  position: fixed;
  top: 0; left: 0; right: 0; bottom: 0;
  background-color: rgba(0,0,0,0.2);
  backdrop-filter: blur(4px);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.left .login-modal {
    position: absolute;
    top: 100%;
    left: 50%;
    transform: translateX(-50%);
    margin-top: 10px;
    width: auto;
    background: rgba(255, 255, 255, 0.85);
    backdrop-filter: blur(16px);
    border: 1px solid rgba(255, 255, 255, 0.6);
    border-radius: var(--radius-lg);
    padding: 24px;
    box-shadow: var(--shadow-float);
    animation: fadeIn 0.3s ease;
    display: flex;
    flex-direction: column;
    align-items: center;
    z-index: 10;
}

/* 按钮样式 */
.left .login-btn {
    width: auto;
    padding: 12px 20px;
    background-color: rgba(255,255,255,0.6);
    color: var(--color-text-primary);
    border: 1px solid rgba(255,255,255,0.4);
    border-radius: var(--radius-md);
    font-size: 16px;
    font-weight: 500;
    cursor: pointer;
    transition: all var(--transition-fast);
}

.login-btn:hover {
    background-color: #fff;
    transform: translateY(-2px);
    box-shadow: var(--shadow-soft);
}

/* 弹窗动画 */
@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateX(-50%) scale(0.96);
  }
  to {
    opacity: 1;
    transform: translateX(-50%) scale(1);
  }
}

.right {
    flex: 1.5;
    display: flex;
    justify-content: center;
    align-items: center;
    overflow: hidden;
}

.quote-section {
    font-family: var(--font-serif);
    font-size: 1.8rem;
    color: var(--color-text-primary);
    text-align: center;
    width: 100%;
    padding: 40px;
    background: var(--color-bg-glass);
    backdrop-filter: blur(16px);
    border-radius: var(--radius-xl);
    border: 1px solid rgba(255,255,255,0.4);
    box-shadow: var(--shadow-soft);
}

.controls-section {
    padding: 30px 40px;
    background: rgba(255, 255, 255, 0.4);
    backdrop-filter: blur(16px);
    border-top: 1px solid rgba(255, 255, 255, 0.6);
    box-shadow: 0 -4px 24px rgba(0, 0, 0, 0.05);
}
</style>