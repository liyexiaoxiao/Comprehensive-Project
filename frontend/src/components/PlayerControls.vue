<template>
    <div class="player">
        <div class="controls">
            <button @click="prev" circle>
                <font-awesome-icon icon="backward-step" />
            </button>
            <button @click="togglePlay" circle>
                <font-awesome-icon :icon="isPlaying ? 'pause' : 'play'" />
            </button>
            <button @click="next" circle>
                <font-awesome-icon icon="forward-step" />
            </button>
            <button @click="toggleMute" circle>
                <font-awesome-icon :icon="isMuted ? 'volume-xmark' : 'volume-high'" />
            </button>
        </div>

        <div class="progress">
            <span>{{ formatTime(currentTime) }}</span>
            <input
                type="range"
                min="0"
                :max="duration"
                step="0.1"
                v-model="currentTime"
                @input="seek"
            />
            <span>{{ formatTime(duration) }}</span>
        </div>
    </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue';
import axios from 'axios';

import '@fortawesome/fontawesome-free/css/all.css';

const isPlaying = ref(false);
const isMuted = ref(false);
const audio = new Audio();
const currentTrack = ref({});
const currentTime = ref(0);
const duration = ref(0);

const fetchAndPlay = async (endpoint) => {
    try {
        const res = await axios.get(endpoint);
        currentTrack.value = res.data;
        audio.src = res.data.url;
        audio.play();
        isPlaying.value = true;
    } catch (e) {
        console.error("播放失败", e);
    }
};

const togglePlay = () => {
    if (isPlaying.value) {
        audio.pause();
    }
    else {
        audio.play();
    }
    isPlaying.value = !isPlaying.value;
};

const toggleMute = () => {
    audio.muted = !audio.muted;
    isMuted.value = audio.muted;
}

const prev = () => fetchAndPlay('/api/music/prev');
const next = () => fetchAndPlay('/api/music/next');

// 进度条更新
audio.ontimeupdate = () => {
    currentTime.value = audio.currentTime;
};
audio.onloadedmetadata = () => {
    duration.value = audio.duration;
};
audio.onended = () => {
    next();  // 自动播放下一首
};

const seek = () => {
    audio.currentTime = currentTime.value;
};

const formatTime = (seconds) => {
    const m = Math.floor(seconds / 60).toString().padStart(2, '0');
    const s = Math.floor(seconds % 60).toString().padStart(2, '0');
    return `${m}:${s}`;
};

// 初次加载默认播放
onMounted(() => {
    fetchAndPlay('/api/music/play');
});
</script>

<style scope>
.player {
    color: white;
    text-align: center;
    /*padding: 20px;*/
}

.controls {
    display: flex;
    justify-content: center;
    gap: 30px;
    margin-bottom: 20px;
}

.controls button {
    width: 40px;
    height: 40px;
    font-size: 1.2rem;
    background: rgba(255, 255, 255, 0.1);
    border: none;
    border-radius: 50%;
    cursor: pointer;
    color: inherit;
    display: flex;
    justify-content: center;
    align-items: center;
    transition: background-color 0.3s;
    padding: 7px;
}

.controls button:hover {
    background: rgba(255, 255, 255, 0.3);
}

.progress {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 10px;
    /*margin-top: 3px;*/
}

input[type="range"] {
    width: 1000px;
}
</style>