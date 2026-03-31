<template>
  <div class="landing-page">
    <div class="ambient-orbs">
      <div class="orb orb-1"></div>
      <div class="orb orb-2"></div>
      <div class="orb orb-3"></div>
    </div>

    <header class="site-header glass-panel animate-fade-in">
      <div class="brand-mark">
        <div class="brand-logo">EH</div>
        <div class="brand-text">
          <span class="brand-name">EmotionHealing</span>
          <span class="brand-subtitle">沉浸式情绪花园</span>
        </div>
      </div>
      <nav class="site-nav">
        <a href="#garden-path" class="nav-link">情绪路径</a>
        <a href="#companions" class="nav-link">陪伴方式</a>
        <RouterLink class="btn-premium" to="/login">进入花园</RouterLink>
      </nav>
    </header>

    <main class="page-content">
      <section class="hero-section animate-fade-in" style="animation-delay: 0.2s;">
        <div class="hero-text-content">
          <p class="section-kicker">Immersive Garden</p>
          <h1 class="hero-title">为纷乱的心绪，<br/>寻一处静谧花园。</h1>
          <p class="hero-description">
            我们不再局限于冷冰冰的工具台，而是为您打造一个有温度、有呼吸感的入口。
            通过音乐、倾诉、冥想与个人空间，找到最适合当下状态的那条小径。
          </p>
          <div class="hero-actions">
            <RouterLink class="btn-premium" to="/login">开始沉浸体验</RouterLink>
            <a class="btn-outline-premium" href="#companions">探索陪伴模块</a>
          </div>
        </div>
        
        <div class="hero-visual glass-panel">
          <div class="visual-badge">Tonight's entrance</div>
          <h2 class="visual-title">雨声森林</h2>
          <p class="visual-desc">放慢语速，将注意力交还给呼吸与身体。</p>
          <div class="visual-tags">
            <span class="tag">深度沉浸</span>
            <span class="tag">自然疗愈</span>
          </div>
        </div>
      </section>

      <section id="garden-path" class="path-section animate-fade-in" style="animation-delay: 0.4s;">
        <div class="section-header">
          <p class="section-kicker">Garden Path</p>
          <h2>不同的情绪，同一座花园。</h2>
          <p class="section-desc">每一处空间都承载着独特的情绪表达，通过优雅的设计语言相互连接。</p>
        </div>

        <div class="features-grid">
          <article 
            v-for="(feature, index) in homeFeatures" 
            :key="feature.title"
            class="feature-card glass-panel"
          >
            <div class="feature-number">{{ String(index + 1).padStart(2, '0') }}</div>
            <h3 class="feature-title">{{ feature.title }}</h3>
            <p class="feature-text">{{ feature.text }}</p>
          </article>
        </div>
      </section>

      <section id="companions" class="companion-section animate-fade-in" style="animation-delay: 0.6s;">
        <div class="companion-content glass-panel">
          <p class="section-kicker">Companion Modes</p>
          <h2>选择您期待的陪伴方式。</h2>
          <p class="section-desc">
            或许您需要声音的慰藉，或许是音乐的环绕，又或者仅仅是一处安静的角落。
            我们提供多种入口，让情绪得以自然流淌。
          </p>
          <div class="scene-list">
            <div v-for="scene in homeScenes" :key="scene" class="scene-item">
              <span class="scene-dot"></span>
              {{ scene }}
            </div>
          </div>
        </div>
      </section>

      <section class="cta-section glass-panel animate-fade-in" style="animation-delay: 0.8s;">
        <div class="cta-content">
          <h2 class="cta-title">准备好开始了吗？</h2>
          <p class="cta-desc">登录后即可进入您的专属音乐舱、对话舱与情绪花园。</p>
        </div>
        <RouterLink class="btn-premium" to="/login">前往登录</RouterLink>
      </section>
    </main>
  </div>
</template>

<script setup>
import { RouterLink } from 'vue-router'
import { homeFeatures, homeScenes } from '@/data/mockContent'
</script>

<style scoped>
.landing-page {
  position: relative;
  padding: 40px 5%;
  max-width: 1440px;
  margin: 0 auto;
}

/* Ambient Background Orbs */
.ambient-orbs {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  z-index: -1;
  overflow: hidden;
  pointer-events: none;
}

.orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.6;
  animation: floatGentle 10s ease-in-out infinite alternate;
}

.orb-1 {
  top: -10%;
  left: -10%;
  width: 50vw;
  height: 50vw;
  background: var(--color-accent-blush);
}

.orb-2 {
  bottom: -20%;
  right: -10%;
  width: 60vw;
  height: 60vw;
  background: var(--color-accent-sage);
  animation-delay: -5s;
}

.orb-3 {
  top: 40%;
  left: 60%;
  width: 40vw;
  height: 40vw;
  background: var(--color-accent-sky);
  animation-delay: -2s;
}

/* Header */
.site-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 32px;
  border-radius: var(--radius-pill);
  margin-bottom: 60px;
}

.brand-mark {
  display: flex;
  align-items: center;
  gap: 16px;
}

.brand-logo {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  background: var(--color-text-primary);
  color: var(--color-bg-primary);
  border-radius: 50%;
  font-family: var(--font-serif);
  font-size: 1.2rem;
  font-weight: 600;
}

.brand-text {
  display: flex;
  flex-direction: column;
}

.brand-name {
  font-family: var(--font-serif);
  font-size: 1.2rem;
  font-weight: 600;
  color: var(--color-text-primary);
}

.brand-subtitle {
  font-size: 0.8rem;
  color: var(--color-text-secondary);
  letter-spacing: 1px;
}

.site-nav {
  display: flex;
  align-items: center;
  gap: 32px;
}

.nav-link {
  font-size: 1rem;
  color: var(--color-text-secondary);
  font-weight: 500;
}

.nav-link:hover {
  color: var(--color-text-primary);
}

/* Typography Utilities */
.section-kicker {
  font-family: var(--font-sans);
  text-transform: uppercase;
  letter-spacing: 2px;
  font-size: 0.85rem;
  color: var(--color-accent-terracotta);
  margin-bottom: 16px;
  font-weight: 600;
}

/* Hero Section */
.hero-section {
  display: grid;
  grid-template-columns: 1.2fr 1fr;
  gap: 60px;
  align-items: center;
  margin-bottom: 120px;
}

.hero-title {
  font-size: 4rem;
  line-height: 1.1;
  margin-bottom: 24px;
}

.hero-description {
  font-size: 1.2rem;
  color: var(--color-text-secondary);
  margin-bottom: 40px;
  max-width: 90%;
}

.hero-actions {
  display: flex;
  gap: 20px;
}

.hero-visual {
  padding: 60px 40px;
  position: relative;
  overflow: hidden;
  border-radius: var(--radius-xl);
}

.hero-visual::before {
  content: '';
  position: absolute;
  top: 0; left: 0; right: 0; bottom: 0;
  background: linear-gradient(135deg, rgba(255,255,255,0.4), rgba(255,255,255,0.1));
  z-index: -1;
}

.visual-badge {
  display: inline-block;
  padding: 6px 16px;
  background: rgba(255,255,255,0.5);
  border-radius: var(--radius-pill);
  font-size: 0.8rem;
  margin-bottom: 20px;
  color: var(--color-text-secondary);
}

.visual-title {
  font-size: 2.5rem;
  margin-bottom: 16px;
}

.visual-tags {
  display: flex;
  gap: 12px;
  margin-top: 32px;
}

.tag {
  padding: 6px 16px;
  border: 1px solid rgba(44, 48, 46, 0.1);
  border-radius: var(--radius-pill);
  font-size: 0.9rem;
}

/* Path Section */
.path-section {
  margin-bottom: 120px;
}

.section-header {
  text-align: center;
  max-width: 600px;
  margin: 0 auto 60px auto;
}

.section-header h2 {
  font-size: 2.5rem;
  margin-bottom: 16px;
}

.features-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 30px;
}

.feature-card {
  padding: 40px 30px;
  transition: transform var(--transition-medium);
}

.feature-card:hover {
  transform: translateY(-5px);
}

.feature-number {
  font-family: var(--font-serif);
  font-size: 2rem;
  color: var(--color-accent-terracotta);
  opacity: 0.5;
  margin-bottom: 20px;
}

.feature-title {
  font-size: 1.5rem;
  margin-bottom: 16px;
}

/* Companion Section */
.companion-section {
  margin-bottom: 120px;
}

.companion-content {
  padding: 80px 60px;
  text-align: center;
  border-radius: var(--radius-xl);
}

.companion-content h2 {
  font-size: 2.5rem;
  margin-bottom: 24px;
}

.companion-content .section-desc {
  max-width: 600px;
  margin: 0 auto 40px auto;
  font-size: 1.1rem;
}

.scene-list {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 20px;
}

.scene-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 24px;
  background: rgba(255,255,255,0.4);
  border-radius: var(--radius-pill);
  font-weight: 500;
}

.scene-dot {
  width: 8px;
  height: 8px;
  background: var(--color-accent-sage);
  border-radius: 50%;
}

/* CTA Section */
.cta-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 60px;
  border-radius: var(--radius-xl);
  margin-bottom: 60px;
}

.cta-title {
  font-size: 2rem;
  margin-bottom: 8px;
}

.cta-desc {
  margin: 0;
  font-size: 1.1rem;
}

@media (max-width: 992px) {
  .hero-section {
    grid-template-columns: 1fr;
  }
  .hero-title {
    font-size: 3rem;
  }
  .cta-section {
    flex-direction: column;
    text-align: center;
    gap: 30px;
  }
  .site-nav {
    display: none; /* simple mobile handling */
  }
}
</style>
