<template>
  <div class="login-page">
    <div class="ambient-orbs">
      <div class="orb orb-login-1"></div>
      <div class="orb orb-login-2"></div>
    </div>

    <div class="login-container animate-fade-in">
      <aside class="story-panel glass-panel">
        <div class="brand-logo-small">EH</div>
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
        <div class="auth-switch">
          <button
            type="button"
            :class="['auth-switch-btn', { active: mode === 'login' }]"
            @click="switchMode('login')"
          >
            登录
          </button>
          <button
            type="button"
            :class="['auth-switch-btn', { active: mode === 'register' }]"
            @click="switchMode('register')"
          >
            注册
          </button>
        </div>

        <div class="form-header">
          <h2>{{ mode === 'login' ? '欢迎回来' : '创建账号' }}</h2>
          <p>{{ mode === 'login' ? '继续您的沉浸陪伴之旅' : '用简单三步开启属于你的情绪花园' }}</p>
        </div>

        <form class="login-form" @submit.prevent="handleSubmit">
          <div v-if="mode === 'register'" class="input-group">
            <label>用户名</label>
            <input v-model.trim="registerForm.username" type="text" placeholder="输入您的用户名" />
          </div>

          <div v-if="mode === 'register'" class="input-group">
            <label>邮箱</label>
            <input v-model.trim="registerForm.email" type="email" placeholder="输入您的邮箱" />
          </div>

          <div class="input-group">
            <label>{{ mode === 'login' ? '账号' : '密码' }}</label>
            <input
              v-if="mode === 'login'"
              v-model.trim="loginForm.username"
              type="text"
              placeholder="输入您的账号"
            />
            <input
              v-else
              v-model.trim="registerForm.password"
              type="password"
              placeholder="设置登录密码"
            />
          </div>

          <div v-if="mode === 'login'" class="input-group">
            <label>密码</label>
            <input v-model.trim="loginForm.password" type="password" placeholder="输入您的密码" />
          </div>

          <p v-if="errorMessage" class="error-message">{{ errorMessage }}</p>
          <p v-if="successMessage" class="success-message">{{ successMessage }}</p>

          <button class="btn-premium submit-btn" type="submit" :disabled="isSubmitting">
            {{ isSubmitting ? '提交中...' : mode === 'login' ? '进入花园' : '完成注册' }}
          </button>
        </form>

        <button class="mode-hint" type="button" @click="switchMode(mode === 'login' ? 'register' : 'login')">
          {{ mode === 'login' ? '还没有账号？去注册' : '已有账号？返回登录' }}
        </button>
      </main>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { AUTH_TOKEN_STORAGE_KEY, CURRENT_USER_STORAGE_KEY } from '@/api/http'
import { getMeApi, loginApi, registerApi } from '@/api/auth'
import { isAdminUser } from '@/api/user'

const router = useRouter()
const route = useRoute()

const mode = ref('login')
const isSubmitting = ref(false)

const loginForm = reactive({
  username: '',
  password: '',
})

const registerForm = reactive({
  username: '',
  email: '',
  password: '',
})

const errorMessage = ref('')
const successMessage = ref('')

const resetMessages = () => {
  errorMessage.value = ''
  successMessage.value = ''
}

const getErrorMessage = (error, fallbackMessage) => {
  const responseData = error?.response?.data
  if (typeof responseData === 'string' && responseData.trim()) {
    return responseData
  }
  if (responseData?.message) {
    return responseData.message
  }
  return fallbackMessage
}

const switchMode = (nextMode) => {
  mode.value = nextMode
  resetMessages()
}

const handleSubmit = async () => {
  resetMessages()

  if (mode.value === 'login') {
    if (!loginForm.username || !loginForm.password) {
      errorMessage.value = '请先完整输入账号和密码。'
      return
    }

    isSubmitting.value = true
    try {
      const loginResponse = await loginApi({
        username: loginForm.username,
        password: loginForm.password,
      })

      const token = typeof loginResponse.data === 'string' ? loginResponse.data : ''
      if (!token) {
        throw new Error('登录返回的 token 无效')
      }

      window.localStorage.setItem(AUTH_TOKEN_STORAGE_KEY, token)

      const currentUserResponse = await getMeApi()
      const currentUser = currentUserResponse.data
      window.localStorage.setItem(CURRENT_USER_STORAGE_KEY, JSON.stringify(currentUser))

      const redirectPath = typeof route.query.redirect === 'string' ? route.query.redirect : ''
      if (redirectPath) {
        router.push(redirectPath)
      } else {
        router.push(isAdminUser(currentUser) ? '/admin' : '/service')
      }
    } catch (error) {
      window.localStorage.removeItem(AUTH_TOKEN_STORAGE_KEY)
      window.localStorage.removeItem(CURRENT_USER_STORAGE_KEY)
      errorMessage.value = getErrorMessage(error, '登录失败，请稍后再试。')
    } finally {
      isSubmitting.value = false
    }
    return
  }

  if (!registerForm.username || !registerForm.email || !registerForm.password) {
    errorMessage.value = '请完整输入用户名、邮箱和密码。'
    return
  }

  const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
  if (!emailPattern.test(registerForm.email)) {
    errorMessage.value = '请输入正确的邮箱格式。'
    return
  }

  isSubmitting.value = true
  try {
    await registerApi({
      username: registerForm.username,
      email: registerForm.email,
      password: registerForm.password,
    })

    loginForm.username = registerForm.username
    loginForm.password = ''
    registerForm.username = ''
    registerForm.email = ''
    registerForm.password = ''
    mode.value = 'login'
    successMessage.value = '注册成功，请使用新账号登录。'
  } catch (error) {
    errorMessage.value = getErrorMessage(error, '注册失败，请稍后再试。')
  } finally {
    isSubmitting.value = false
  }
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

.auth-switch {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  padding: 8px;
  margin-bottom: 28px;
  background: rgba(255, 255, 255, 0.52);
  border-radius: var(--radius-pill);
  border: 1px solid rgba(44, 48, 46, 0.08);
}

.auth-switch-btn {
  border: none;
  border-radius: var(--radius-pill);
  padding: 12px 16px;
  background: transparent;
  color: var(--color-text-secondary);
  font-weight: 600;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.auth-switch-btn.active {
  background: var(--color-text-primary);
  color: #fff;
  box-shadow: 0 8px 18px rgba(44, 48, 46, 0.12);
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

.success-message {
  color: #3b7d53;
  font-size: 0.9rem;
  margin: 0;
}

.submit-btn {
  margin-top: 10px;
  width: 100%;
  padding: 18px;
  font-size: 1.1rem;
}

.submit-btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.mode-hint {
  margin-top: 18px;
  align-self: center;
  border: none;
  background: transparent;
  color: var(--color-text-secondary);
  font-size: 0.95rem;
  cursor: pointer;
  transition: color var(--transition-fast);
}

.mode-hint:hover {
  color: var(--color-text-primary);
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
