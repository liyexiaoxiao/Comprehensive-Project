<template>
  <div class="login-page">
    <div class="ambient-orbs">
      <div class="orb orb-login-1"></div>
      <div class="orb orb-login-2"></div>
    </div>

    <div class="login-container animate-fade-in">
      <aside class="story-panel glass-panel">
        <div class="brand-logo-small">CE</div>
        <h1 class="story-title">进入您的<br/>情绪花园</h1>
        <p class="story-desc">
          这里没有喧嚣，只有属于您的宁静时刻。
          让每一次登录，都成为一次与自己对话的仪式。
        </p>

        <div class="feature-steps">
          <div class="step">
            <span class="step-num">01</span>
            <div>
              <h4>静下心来</h4>
              <p>柔和的空间，让输入动作也没有压迫感。</p>
            </div>
          </div>
          <div class="step">
            <span class="step-num">02</span>
            <div>
              <h4>开启旅程</h4>
              <p>进入专属音乐舱、对话舱及个人空间。</p>
            </div>
          </div>
        </div>

        <RouterLink class="back-link" to="/">
          ← 返回首页
        </RouterLink>
      </aside>

      <main class="form-panel glass-panel">
        <div class="form-header">
          <h2>欢迎回来</h2>
          <p>继续您的沉浸陪伴之旅</p>
        </div>

        <form class="login-form" @submit.prevent="handleSubmit">
          <div class="input-group">
            <label>账号</label>
            <input v-model.trim="form.username" type="text" placeholder="输入您的账号" />
          </div>

          <div class="input-group">
            <label>密码</label>
            <input v-model.trim="form.password" type="password" placeholder="输入您的密码" />
          </div>

          <p v-if="errorMessage" class="error-message">{{ errorMessage }}</p>

          <button class="btn-premium submit-btn" type="submit">
            进入花园
          </button>
        </form>

        <div class="demo-notes">
          <span>Secure Demo</span>
          <span class="dot">·</span>
          <span>前端演示版本</span>
        </div>
      </main>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'

const router = useRouter()

const form = reactive({
  username: '',
  password: '',
})

const errorMessage = ref('')

const handleSubmit = () => {
  if (!form.username || !form.password) {
    errorMessage.value = '请先完整输入账号和密码。'
    return
  }

  errorMessage.value = ''
  router.push('/service')
}
</script>

<style scoped>
.login-page {
  position: relative;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
}

.ambient-orbs {
  position: fixed;
  inset: 0;
  z-index: -1;
  overflow: hidden;
}

.orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(90px);
  opacity: 0.5;
  animation: floatGentle 12s ease-in-out infinite alternate;
}

.orb-login-1 {
  top: -20%;
  left: -10%;
  width: 70vw;
  height: 70vw;
  background: var(--color-accent-sand);
}

.orb-login-2 {
  bottom: -20%;
  right: -10%;
  width: 60vw;
  height: 60vw;
  background: var(--color-accent-sky);
  animation-delay: -6s;
}

.login-container {
  display: grid;
  grid-template-columns: 1fr 1fr;
  max-width: 1000px;
  width: 100%;
  gap: 20px;
}

.story-panel {
  padding: 60px 50px;
  display: flex;
  flex-direction: column;
  background: rgba(255,255,255,0.4);
}

.brand-logo-small {
  width: 40px;
  height: 40px;
  background: var(--color-text-primary);
  color: var(--color-bg-primary);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-family: var(--font-serif);
  font-weight: 600;
  margin-bottom: 40px;
}

.story-title {
  font-size: 2.5rem;
  margin-bottom: 20px;
}

.story-desc {
  font-size: 1.1rem;
  margin-bottom: 40px;
  line-height: 1.8;
}

.feature-steps {
  display: flex;
  flex-direction: column;
  gap: 30px;
  margin-bottom: auto;
}

.step {
  display: flex;
  gap: 20px;
}

.step-num {
  font-family: var(--font-serif);
  font-size: 1.5rem;
  color: var(--color-accent-terracotta);
  opacity: 0.6;
}

.step h4 {
  font-family: var(--font-sans);
  font-size: 1.1rem;
  margin-bottom: 4px;
}

.step p {
  font-size: 0.9rem;
  margin: 0;
}

.back-link {
  margin-top: 40px;
  display: inline-block;
  font-weight: 500;
  color: var(--color-text-secondary);
}

.back-link:hover {
  color: var(--color-text-primary);
}

.form-panel {
  padding: 60px 50px;
  background: rgba(255,255,255,0.8);
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.form-header {
  margin-bottom: 40px;
}

.form-header h2 {
  font-size: 2rem;
  margin-bottom: 8px;
}

.login-form {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.input-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.input-group label {
  font-size: 0.9rem;
  font-weight: 500;
  color: var(--color-text-secondary);
}

.input-group input {
  width: 100%;
  padding: 16px 20px;
  border: 1px solid rgba(44, 48, 46, 0.1);
  border-radius: var(--radius-md);
  background: rgba(255,255,255,0.5);
  font-size: 1rem;
  transition: all var(--transition-medium);
}

.input-group input:focus {
  outline: none;
  border-color: var(--color-accent-terracotta);
  background: #fff;
  box-shadow: 0 0 0 4px rgba(200, 138, 117, 0.1);
}

.error-message {
  color: #D9534F;
  font-size: 0.9rem;
  margin: 0;
}

.submit-btn {
  margin-top: 10px;
  width: 100%;
  padding: 18px;
  font-size: 1.1rem;
}

.demo-notes {
  margin-top: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  font-size: 0.85rem;
  color: var(--color-text-light);
}

.dot {
  font-weight: bold;
}

@media (max-width: 768px) {
  .login-container {
    grid-template-columns: 1fr;
  }
  .story-panel {
    display: none; /* Hide story on mobile for a cleaner login */
  }
}
</style>
