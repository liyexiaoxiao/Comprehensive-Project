<template>
    <div class="circle-timer" :class="{ breath: isBreathing }">
        <svg viewBox="0 0 440 440">
            <defs>
                <linearGradient id="grad" x1="0" y1="0" x2="1" y2="1">
                    <stop offset="0%" :stop-color="colors[0]" />
                    <stop  offset="100%" :stop-color="colors[1]" />
                </linearGradient>
            </defs>
            <circle class="bg" cx="220" cy="220" r="180" />
            <circle class="progress" :stroke-dasharray="dashArray" cx="220" cy="220" r="180" stroke="url(#grad)" />
        </svg>
        <div class="time-text">{{ formatTime(timeLeft) }}</div>
    </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch } from 'vue';

const props = defineProps({
    colors: {
        type: Array,
        default: () => ['#ffffff', '#eeeeee']
    },
    totalTime: {
        type: Number,
        default: 180
    }
});

const timeLeft = ref(props.totalTime)
const dashArray = ref('0, 1130')  // 初始值
const isBreathing = ref(true)

const radius = 180
const circumference = 2 * Math.PI * radius

const formatTime = (sec) => {
    const minutes = Math.floor(sec / 60);
    const seconds = sec % 60;
    return `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`;
}

const updateProgress = () => {
    const percent = (props.totalTime - timeLeft.value) / props.totalTime;
    dashArray.value = `${circumference * percent}, ${circumference}`;
};

let interval = null

const startTimer = () => {
    interval = setInterval(() => {
        if (timeLeft.value > 0) {
            timeLeft.value--;
            updateProgress();
        }
        else {
            clearInterval(interval);
            interval = null;
        }
    }, 1000);
};

onMounted(() => {
    updateProgress();
    startTimer();
});

// 组件卸载时清理定时器
onUnmounted(() => {
    if (interval) {
        clearInterval(interval)
    }
})

// 如果 totalTime 发生变化，重置倒计时
watch(() => props.totalTime, (newVal) => {
    timeLeft.value = newVal
    updateProgress()
    if (interval) {
        clearInterval(interval)
    }
    startTimer()
})
</script>

<style scoped>
.circle-timer {
    max-width: 100%;
    max-height: 100%;
    box-sizing: border-box;
    width: 440px;
    height: 440px;
    overflow: hidden;
    position: relative;
    transform-origin: center center;
    background: var(--color-bg-glass);
    backdrop-filter: blur(12px);
    border: 1px solid rgba(255, 255, 255, 0.4);
    border-radius: 50%;
    padding: 20px;
    box-shadow: var(--shadow-float);
    animation: breath 4s ease-in-out infinite;
}

svg {
    width: 100%;
    height: 100%;
    transform: rotate(-90deg);
}

.bg {
    fill: none;
}

.progress {
    fill: none;
    stroke: #fff;
    stroke-width: 12;
    stroke-linecap: round;
    transition: stroke-dasharray 0.5s ease;
}

.time-text {
    position: absolute;
    top: 50%;
    left: 50%;
    transform: translate(-50%, -50%);
    font-size: 3.2rem; 
    font-weight: 600; 
    color: var(--color-text-primary); 
    font-family: var(--font-serif);
}

.breath {
    animation: breath 4s ease-in-out infinite;
}

@keyframes breath {
    0%, 100% {
        transform: scale(1);
    }
    50% {
        transform: scale(1.05);
    }
}
</style>