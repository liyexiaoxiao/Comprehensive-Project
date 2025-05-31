<template>
  <div class="home">
    <video autoplay muted loop playsinline class="bg-video">
      <source src="/images/waveBackground.mp4" type="video/mp4" />
    </video>
    <!-- Fixed header with improved text alignment -->
    <div class="fixed-header">
      <div class="header-text-container">
        <h1 class="title">CalmEcho</h1>
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
import axios from 'axios'
import { ElMessage } from 'element-plus'

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
      const res = await axios.post('http://localhost:5000/api/analyze', { text: spokenText })
      result.value.emotion = res.data.emotion
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
@import url('https://fonts.googleapis.com/css2?family=Quicksand:wght@400;500;700&display=swap');

.home {
  position: relative;
  min-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: flex-start;
  font-family: 'Quicksand', sans-serif;
  text-align: center;
  z-index: 1;
  padding-top: 60px;
}

/* Improved header with perfect baseline alignment */
.fixed-header {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  background-color: rgba(255, 255, 255, 0.8);
  padding: 8px 0;
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
  padding: 0 20px;
}

.title {
  font-size: 3.5rem;
  font-weight: 700;
  color: #333;
  line-height: 1;
  margin: 0;
  padding-bottom: 0.1em; /* Fine-tuned alignment */
}

.subtitle {
  font-size: 1.2rem;
  color: #666;
  line-height: 1;
  margin: 0;
  padding-bottom: 0.3em; /* Fine-tuned alignment */
}


@keyframes gradient {
  0% { background-position: 0% 50%; }
  50% { background-position: 100% 50%; }
  100% { background-position: 0% 50%; }
}

.centered-list li {
  margin: 12px 0;
  font-size: 1.1rem;
  line-height: 1.6;
  color: #FFFFFF; /* 使用白色字体 */
  text-shadow: 1px 1px 3px rgba(0, 0, 0, 0.6); /* 黑色阴影，增加可读性 */
}

.result-box p {
  margin: 0.5rem 0;
  line-height: 1.6;
  color: #DCEFFF; /* 使用浅天蓝色字体 */
  text-shadow: 1px 1px 4px rgba(0, 0, 0, 0.5); /* 给文字加上轻微的阴影 */
}

.content-box {
  display: flex;
  font-weight: 700;
  flex-direction: column;
  align-items: center;
  width: 100%;
}

.centered-list {
  text-align: center;
  list-style: none;
  padding: 0;
  margin: 0 0 2rem 0;
  width: 100%;
}

.bg-video {
  position: fixed;
  top: 0;
  left: 0;
  z-index: -1;
  width: 100%;
  height: 100%;
  object-fit: cover;
  opacity: 0.7;
}

.centered-list li {
  margin: 12px 0;
  font-size: 1.1rem;
  line-height: 1.6;
}
.bg-video {
  filter: brightness(0.9) blur(1px);
}


.centered-divider {
  width: 80%;
  margin: 1.5rem auto;
}

.button-container {
  margin: 1rem 0;
}

.listening-tag {
  margin: 1rem 0;
}

.result-box {
  margin: 1.5rem 0;
  background:rgb(139, 181, 224);
  padding: 1.5rem;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  width: 90%;
  max-width: 600px;
}


.philosophy {
  font-weight: 500;
  margin: 2.5rem 0 1rem;
  font-style: italic;
  color: #DCEFFF;  /* 使用浅天蓝色字体 */
  text-shadow: 1px 1px 3px rgba(0, 0, 0, 0.5); /* 为哲学文字加上阴影 */
  max-width: 600px;
  line-height: 1.6;
  font-size: 1.1rem;
}

/* 标题样式 */
h1, h2 {
  font-family: 'Quicksand', sans-serif;
  font-weight: 700;
  margin: 1rem 0;
  color: #FFFFFF; /* 使用白色字体 */
  text-shadow: 2px 2px 5px rgba(0, 0, 0, 0.4); /* 强化阴影效果 */
}

h2 {
  font-size: 1.8rem;
}

.image-gallery {
  display: flex;
  justify-content: center;
  gap: 20px;
  margin-top: 2rem;
}

.gallery-img {
  width: 100%;
  max-width: 250px;
  height: auto;
  border-radius: 8px;
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
}
</style>