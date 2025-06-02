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

        <div class="voice-control-section">
            <div class="voice-control-status" :class="{ 'listening': isListening }">
                <font-awesome-icon icon="microphone" />
                <span>{{ isListening ? '语音控制已开启' : '语音控制已关闭' }}</span>
            </div>
            <button class="language-toggle" @click="toggleLanguage">
                {{ currentLanguage === 'zh-CN' ? 'EN' : '中' }}
            </button>
        </div>
    </div>
</template>

<script setup>
import { ref, onMounted, watch, onUnmounted } from 'vue';
import axios from 'axios';

import '@fortawesome/fontawesome-free/css/all.css';

const isPlaying = ref(false);
const isMuted = ref(false);
const audio = new Audio();
const currentTrack = ref({});
const currentTime = ref(0);
const duration = ref(0);
const currentEmotion = ref(localStorage.getItem('currentEmotion') || 'neutral');
const isListening = ref(false);
const recognition = ref(null);
const currentLanguage = ref('zh-CN'); // 默认使用中文

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

// 根据情绪播放音乐
const playEmotionMusic = async (emotion) => {
    try {
        const response = await axios.get(`http://localhost:5000/api/music/${emotion}`, {
            responseType: 'blob'
        });
        
        const url = URL.createObjectURL(response.data);
        audio.src = url;
        audio.play();
        isPlaying.value = true;
        currentEmotion.value = emotion;
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

const prev = async () => {
    try {
        const response = await axios.get(`http://localhost:5000/api/music/${currentEmotion.value}/prev`, {
            responseType: 'blob'
        });
        
        const url = URL.createObjectURL(response.data);
        audio.src = url;
        audio.play();
        isPlaying.value = true;
    } catch (e) {
        console.error("切换上一首失败", e);
    }
};

const next = async () => {
    try {
        const response = await axios.get(`http://localhost:5000/api/music/${currentEmotion.value}/next`, {
            responseType: 'blob'
        });
        
        const url = URL.createObjectURL(response.data);
        audio.src = url;
        audio.play();
        isPlaying.value = true;
    } catch (e) {
        console.error("切换下一首失败", e);
    }
};

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

// 初始化语音识别
const initSpeechRecognition = () => {
    const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
    if (!SpeechRecognition) {
        console.error('浏览器不支持语音识别');
        return;
    }

    recognition.value = new SpeechRecognition();
    recognition.value.lang = currentLanguage.value;
    recognition.value.continuous = true;  // 持续识别
    recognition.value.interimResults = true;  // 实时结果

    recognition.value.onresult = (event) => {
        const result = event.results[event.results.length - 1];
        const transcript = result[0].transcript.trim().toLowerCase();
        
        // 处理语音命令
        if (result.isFinal) {
            console.log('识别到的命令：', transcript);
            handleVoiceCommand(transcript);
        }
    };

    recognition.value.onerror = (event) => {
        console.error('语音识别错误：', event.error);
        // 如果发生错误，尝试重新启动识别
        if (isListening.value) {
            startListening();
        }
    };

    recognition.value.onend = () => {
        // 如果仍在监听状态，则重新启动识别
        if (isListening.value) {
            startListening();
        }
    };
};

// 切换语言
const toggleLanguage = () => {
    currentLanguage.value = currentLanguage.value === 'zh-CN' ? 'en-US' : 'zh-CN';
    if (recognition.value) {
        recognition.value.lang = currentLanguage.value;
        // 重新启动识别以应用新的语言设置
        stopListening();
        startListening();
    }
};

// 处理语音命令
const handleVoiceCommand = (command) => {
    // 中文命令
    if (command.includes('暂停') || command.includes('停止')) {
        if (isPlaying.value) {
            togglePlay();
        }
    } else if (command.includes('播放') || command.includes('继续')) {
        if (!isPlaying.value) {
            togglePlay();
        }
    } else if (command.includes('下一首') || command.includes('下一首歌')) {
        next();
    } else if (command.includes('上一首') || command.includes('上一首歌')) {
        prev();
    }
    // 英文命令
    else if (command.includes('pause') || command.includes('stop')) {
        if (isPlaying.value) {
            togglePlay();
        }
    } else if (command.includes('play') || command.includes('continue')) {
        if (!isPlaying.value) {
            togglePlay();
        }
    } else if (command.includes('next') || command.includes('next song')) {
        next();
    } else if (command.includes('previous') || command.includes('last song')) {
        prev();
    }
};

// 开始监听
const startListening = () => {
    if (recognition.value) {
        try {
            recognition.value.start();
            isListening.value = true;
        } catch (e) {
            console.error('启动语音识别失败：', e);
        }
    }
};

// 停止监听
const stopListening = () => {
    if (recognition.value) {
        recognition.value.stop();
        isListening.value = false;
    }
};

// 组件挂载时初始化语音识别并开始监听
onMounted(() => {
    const emotion = localStorage.getItem('currentEmotion') || 'neutral';
    playEmotionMusic(emotion);
    initSpeechRecognition();
    startListening();
});

// 组件卸载时停止监听
onUnmounted(() => {
    stopListening();
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

.voice-control-section {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 15px;
    margin-top: 15px;
}

.voice-control-status {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
    font-size: 0.9rem;
    color: rgba(255, 255, 255, 0.7);
    transition: all 0.3s ease;
}

.voice-control-status.listening {
    color: #4CAF50;
    animation: pulse 2s infinite;
}

.language-toggle {
    background: rgba(255, 255, 255, 0.1);
    border: none;
    border-radius: 4px;
    color: white;
    padding: 4px 8px;
    font-size: 0.8rem;
    cursor: pointer;
    transition: background-color 0.3s;
}

.language-toggle:hover {
    background: rgba(255, 255, 255, 0.2);
}

@keyframes pulse {
    0% {
        opacity: 0.7;
    }
    50% {
        opacity: 1;
    }
    100% {
        opacity: 0.7;
    }
}
</style>