<template>
  <div class="home">
    <video autoplay muted loop playsinline class="bg-video">
      <source src="/images/waveBackground.mp4" type="video/mp4" />
    </video>
    <!-- Fixed header with improved text alignment -->
    <div class="fixed-header">
      <div class="header-text-container">
        <h1 class="title">EmotionHealing</h1>
        <p class="subtitle">Let your voice guide your mind.</p>
      </div>
    </div>

    <div class="background"></div> <!-- 🌈 Dynamic background -->
    <div class="centered-container">
      <div class="content-box">
        <h2>🌟 What can it do?</h2>
        <ul class="centered-list">
          <li>🎙️ Voice Emotion Recognition (Chinese / English supported)</li>
          <li>🎧 Recommend customized meditation guidance</li>
          <li>⏳ Provide focus countdown with reminders</li>
        </ul>

        <el-divider class="centered-divider" />

        <h2>🎤 Speak your feelings</h2>
        <div class="button-container">
          <el-button type="primary" @click="startListening">🎤 Click to Speak</el-button>
        </div>

        <div v-if="listening" class="listening-tag">
          <el-tag type="warning">🎙️ Listening... Please speak now</el-tag>
        </div>

        <div v-if="result.text || result.emotion" class="result-box">
          <p>📝 <strong>Recognized Text:</strong> {{ result.text }}</p>
          <p>🧠 <strong>Detected Emotion:</strong>
            <span :style="{ color: emotionColor }">{{ result.emotion }}</span>
          </p>
        </div>

        <p class="philosophy">
          🌱 Our philosophy: Through emotion-aware guidance, we help you calm your mind, understand your emotions, and focus on the present.
        </p>

        <!-- Image gallery -->
        <div class="image-gallery">
          <img src="/images/feature-img-1.jpg" alt="Feature 1" class="gallery-img" />
          <img src="/images/feature-img-2.jpg" alt="Feature 2" class="gallery-img" />
          <img src="/images/feature-img-3.jpg" alt="Feature 3" class="gallery-img" />
          <img src="/images/feature-img-4.jpg" alt="Feature 4" class="gallery-img" />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'

import { analyzeEmotionApi } from '@/api/python'
import { useSpeechStore } from '@/stores/speech'
import { useRouter } from 'vue-router'
const speechStore = useSpeechStore()
const router = useRouter()

const result = ref({ text: '', emotion: '' })
const listening = ref(false)

function startListening() {
  const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition
  if (!SpeechRecognition) {
    ElMessage.error('❌ Your browser does not support Speech Recognition.')
    return
  }

  const recognition = new SpeechRecognition()
  recognition.lang = 'zh-CN'
  recognition.interimResults = false
  recognition.maxAlternatives = 1

  listening.value = true
  recognition.start()

  recognition.onresult = async (event) => {
    const spokenText = event.results[0][0].transcript
    console.log("🎙️ 识别结果：", spokenText)
    result.value.text = spokenText
    listening.value = false

    try {
      const res = await analyzeEmotionApi({ text: spokenText })
      result.value.emotion = res.data.emotion

      speechStore.setEmotion(res.data.emotion)
      // 保存情绪到 localStorage
      localStorage.setItem('currentEmotion', res.data.emotion)

      // 成功后跳转
      setTimeout(() => {
        router.push('/meditation-room')
      }, 5000)

    } catch (err) {
      ElMessage.error('❌ Failed to analyze emotion.')
      console.error(err)
    }
  }

  recognition.onerror = (event) => {
    listening.value = false
    ElMessage.error('❌ Speech recognition failed.')
    console.error('Speech Recognition Error:', event)
  }
}

const emotionColor = computed(() => {
  const emotion = result.value.emotion.toLowerCase()
  if (emotion.includes('anger')) return 'red'
  if (emotion.includes('joy') || emotion.includes('happy')) return 'green'
  if (emotion.includes('sad')) return 'blue'
  if (emotion.includes('anxiety') || emotion.includes('fear')) return 'orange'
  return 'black'
})
</script>

<style scoped>
.home {
  position: relative;
  min-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: flex-start;
  text-align: center;
  z-index: 1;
  padding-top: 100px;
}

/* Improved header with perfect baseline alignment */
.fixed-header {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  background: var(--color-bg-glass);
  backdrop-filter: blur(16px);
  border-bottom: 1px solid rgba(255, 255, 255, 0.4);
  padding: 16px 0;
  z-index: 1000;
  display: flex;
  justify-content: center;
}

.header-text-container {
  display: flex;
  align-items: baseline;
  gap: 20px;
  max-width: 1200px;
  width: 100%;
  padding: 0 40px;
}

.title {
  font-family: var(--font-serif);
  font-size: 2.5rem;
  font-weight: 600;
  color: var(--color-text-primary);
  line-height: 1;
  margin: 0;
}

.subtitle {
  font-family: var(--font-sans);
  font-size: 1.1rem;
  color: var(--color-text-secondary);
  line-height: 1;
  margin: 0;
}

.bg-video {
  position: fixed;
  top: 0;
  left: 0;
  z-index: -1;
  width: 100%;
  height: 100%;
  object-fit: cover;
  opacity: 0.8;
  filter: brightness(0.95) saturate(0.8);
}

.content-box {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 100%;
  max-width: 800px;
  padding: 40px;
  margin-top: 20px;
  background: rgba(249, 248, 246, 0.85);
  backdrop-filter: blur(24px);
  border: 1px solid rgba(255, 255, 255, 0.6);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-float);
  animation: slideUp 0.8s cubic-bezier(0.22, 1, 0.36, 1) forwards;
}

.centered-list {
  text-align: left;
  list-style: none;
  padding: 0;
  margin: 0 0 2rem 0;
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.centered-list li {
  font-size: 1.1rem;
  line-height: 1.6;
  color: var(--color-text-primary);
  display: flex;
  align-items: center;
  gap: 12px;
}

.centered-divider {
  width: 100%;
  margin: 2rem auto;
  border-top-color: rgba(44, 48, 46, 0.1);
}

.button-container {
  margin: 2rem 0;
}

.listening-tag {
  margin: 1rem 0;
}

.result-box {
  margin: 1.5rem 0;
  background: var(--color-bg-glass);
  backdrop-filter: blur(12px);
  padding: 1.5rem;
  border-radius: var(--radius-lg);
  border: 1px solid rgba(255, 255, 255, 0.6);
  box-shadow: var(--shadow-soft);
  width: 100%;
  max-width: 600px;
  text-align: left;
}

.result-box p {
  margin: 0.5rem 0;
  line-height: 1.6;
  color: var(--color-text-primary);
  font-size: 1.05rem;
}

.philosophy {
  margin: 2.5rem 0 1rem;
  color: var(--color-text-secondary);
  max-width: 600px;
  line-height: 1.8;
  font-size: 1.1rem;
  font-style: italic;
}

/* 标题样式 */
h1, h2 {
  font-family: var(--font-serif);
  font-weight: 600;
  margin: 1rem 0 2rem;
  color: var(--color-text-primary);
}

h2 {
  font-size: 1.8rem;
}

.image-gallery {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
  margin-top: 2rem;
  width: 100%;
}

.gallery-img {
  width: 100%;
  height: 200px;
  object-fit: cover;
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-soft);
  transition: all var(--transition-medium);
}

.gallery-img:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-float);
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@media (max-width: 768px) {
  .header-text-container {
    flex-direction: column;
    align-items: center;
    gap: 8px;
    padding: 0 20px;
  }
  .title {
    font-size: 2rem;
  }
  .content-box {
    margin: 20px;
    padding: 24px;
  }
  .image-gallery {
    grid-template-columns: 1fr;
  }
}
</style>
