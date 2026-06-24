<template>
  <div class="side-nav-wrapper">
    <!-- 导航分组 -->
    <div class="nav-group">
      <router-link to="/dashboard" class="nav-item" :class="{ active: isActive('/dashboard') }">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" class="nav-icon">
          <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/><polyline points="9 22 9 12 15 12 15 22"/>
        </svg>
        <span>首页</span>
      </router-link>
    </div>

    <div class="nav-group-label">Learn</div>
    <div class="nav-group">
      <router-link to="/knowledge" class="nav-item" :class="{ active: isActive('/knowledge') }">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" class="nav-icon">
          <circle cx="12" cy="12" r="10"/><path d="M12 16v-4"/><path d="M12 8h.01"/>
        </svg>
        <span>AI 问答</span>
      </router-link>
      <router-link to="/knowledge-base" class="nav-item" :class="{ active: isActive('/knowledge-base') }">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" class="nav-icon">
          <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"/><path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"/>
        </svg>
        <span>知识库</span>
      </router-link>
      <router-link to="/practice" class="nav-item" :class="{ active: isActive('/practice') && !isActive('/practice/history') }">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" class="nav-icon">
          <path d="M16 4h2a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h2"/><rect x="8" y="2" width="8" height="4" rx="1" ry="1"/>
        </svg>
        <span>刷题练习</span>
      </router-link>
      <router-link to="/practice/history" class="nav-item" :class="{ active: isActive('/practice/history') }">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" class="nav-icon">
          <circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/>
        </svg>
        <span>刷题记录</span>
      </router-link>
    </div>

    <div class="nav-group-label">Journal</div>
    <div class="nav-group">
      <router-link to="/diary" class="nav-item" :class="{ active: isActive('/diary') }">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" class="nav-icon">
          <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/>
        </svg>
        <span>日记本</span>
      </router-link>
      <router-link to="/weekly-report" class="nav-item" :class="{ active: isActive('/weekly-report') }">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" class="nav-icon">
          <polyline points="22 12 18 12 15 21 9 3 6 12 2 12"/>
        </svg>
        <span>成长周报</span>
      </router-link>
      <router-link to="/plan" class="nav-item" :class="{ active: isActive('/plan') }">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" class="nav-icon">
          <path d="M12 20h9"/><path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z"/>
        </svg>
        <span>我的计划</span>
      </router-link>
    </div>

    <!-- 底部：深色切换 -->
    <div class="nav-footer">
      <button class="dark-toggle" @click="toggleDark" :title="isDark ? '切换浅色模式' : '切换深色模式'">
        <svg v-if="isDark" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" width="17" height="17">
          <circle cx="12" cy="12" r="5"/><line x1="12" y1="1" x2="12" y2="3"/><line x1="12" y1="21" x2="12" y2="23"/>
          <line x1="4.22" y1="4.22" x2="5.64" y2="5.64"/><line x1="18.36" y1="18.36" x2="19.78" y2="19.78"/>
          <line x1="1" y1="12" x2="3" y2="12"/><line x1="21" y1="12" x2="23" y2="12"/>
          <line x1="4.22" y1="19.78" x2="5.64" y2="18.36"/><line x1="18.36" y1="5.64" x2="19.78" y2="4.22"/>
        </svg>
        <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" width="17" height="17">
          <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"/>
        </svg>
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const isDark = ref(false)

/** 当前路由是否匹配 */
function isActive(path: string) {
  return route.path === path || route.path.startsWith(path + '/')
}

/** 深色模式切换 */
function toggleDark() {
  isDark.value = !isDark.value
  document.documentElement.classList.toggle('dark', isDark.value)
  localStorage.setItem('trace-dark', isDark.value ? '1' : '0')
}

onMounted(() => {
  if (localStorage.getItem('trace-dark') === '1') {
    isDark.value = true
    document.documentElement.classList.add('dark')
  }
})
</script>

<style lang="scss" scoped>
.side-nav-wrapper {
  display: flex; flex-direction: column; height: 100%; padding: 0;

  // ── 分组标签 ──
  .nav-group-label {
    font-family: 'Georgia', 'Palatino Linotype', serif;
    font-size: 10px; font-weight: 700; font-style: italic;
    color: var(--color-text); letter-spacing: 0.6px;
    text-transform: uppercase;
    padding: 14px 16px 6px;
  }

  // ── 导航组 ──
  .nav-group {
    padding: 0 8px;
    &:first-child { padding-top: 12px; }
  }

  // ── 导航项 ──
  .nav-item {
    display: flex; align-items: center; gap: 10px; padding: 10px 10px; margin: 5px 0;
    border-radius: 8px; font-size: 13px; color: var(--color-text-secondary); text-decoration: none;
    transition: all var(--transition-fast); position: relative;
    .nav-icon { width: 17px; height: 17px; flex-shrink: 0; opacity: 0.7; }
    &:hover {
      background: var(--color-hover); color: var(--color-text);
      .nav-icon { opacity: 1; }
    }
    &.active {
      background: var(--color-active); color: var(--color-primary); font-weight: 500;
      .nav-icon { opacity: 1; color: var(--color-primary); }
      // 左侧指示条
      &::before {
        content: ''; position: absolute; left: -8px; top: 6px; bottom: 6px;
        width: 3px; border-radius: 0 2px 2px 0;
        background: var(--color-primary);
      }
    }
  }

  // ── 底部 ──
  .nav-footer {
    margin-top: auto; padding: 12px 12px;
    border-top: 1px solid var(--color-border-light);
    display: flex; justify-content: center;
    .dark-toggle {
      display: flex; align-items: center; justify-content: center;
      width: 36px; height: 36px; border-radius: 10px;
      border: 1px solid var(--color-border); background: transparent;
      color: var(--color-text-secondary); cursor: pointer;
      transition: all var(--transition);
      &:hover { border-color: var(--color-primary); color: var(--color-primary); background: var(--color-primary-bg); }
    }

  }
}
</style>
