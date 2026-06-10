<template>
  <div class="side-nav-wrapper">
    <router-link to="/knowledge" class="nav-item" :class="{ active: isActive('/knowledge') }">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" class="nav-icon">
        <circle cx="12" cy="12" r="10"/><path d="M12 16v-4"/><path d="M12 8h.01"/>
      </svg>
      <span>Trace 问答</span>
    </router-link>

    <router-link to="/interview" class="nav-item" :class="{ active: isActive('/interview') && !isActive('/interview/history') }">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" class="nav-icon">
        <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
      </svg>
      <span>模拟面试</span>
    </router-link>

    <router-link to="/interview/history" class="nav-item" :class="{ active: isActive('/interview/history') }">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" class="nav-icon">
        <circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/>
      </svg>
      <span>面试记录</span>
    </router-link>

    <router-link to="/goal" class="nav-item" :class="{ active: isActive('/goal') }">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" class="nav-icon">
        <circle cx="12" cy="12" r="10"/><path d="M12 6v6l4 2"/>
      </svg>
      <span>目标</span>
    </router-link>

    <div class="nav-divider"></div>

    <router-link to="/diary" class="nav-item" :class="{ active: isActive('/diary') }">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="nav-icon">
        <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/>
      </svg>
      <span>日记本</span>
    </router-link>

    <router-link to="/weekly-report" class="nav-item" :class="{ active: isActive('/weekly-report') }">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="nav-icon">
        <polyline points="22 12 18 12 15 21 9 3 6 12 2 12"/>
      </svg>
      <span>成长周报</span>
    </router-link>

    <router-link to="/plan" class="nav-item" :class="{ active: isActive('/plan') }">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="nav-icon">
        <path d="M12 20h9"/><path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z"/>
      </svg>
      <span>我的计划</span>
    </router-link>

    <div class="nav-footer">
      <button class="dark-toggle" @click="toggleDark" :title="isDark ? '切换浅色' : '切换深色'">
        <svg v-if="isDark" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" width="16" height="16">
          <circle cx="12" cy="12" r="5"/><line x1="12" y1="1" x2="12" y2="3"/><line x1="12" y1="21" x2="12" y2="23"/>
        </svg>
        <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" width="16" height="16">
          <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"/>
        </svg>
        <span>{{ isDark ? '浅色' : '深色' }}</span>
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const isDark = ref(false)

function isActive(path: string) { return route.path.startsWith(path) }

function toggleDark() {
  isDark.value = !isDark.value
  document.documentElement.classList.toggle('dark', isDark.value)
  localStorage.setItem('trace-dark', isDark.value ? '1' : '0')
}

onMounted(() => {
  if (localStorage.getItem('trace-dark') === '1') {
    isDark.value = true; document.documentElement.classList.add('dark')
  }
})
</script>

<style lang="scss" scoped>
.side-nav-wrapper {
  display: flex; flex-direction: column; height: 100%; padding: 12px 0;

  .nav-item {
    display: flex; align-items: center; gap: 10px; padding: 9px 18px; margin: 1px 8px;
    border-radius: 8px; font-size: 13px; color: var(--color-text-secondary); text-decoration: none;
    transition: all var(--transition);
    .nav-icon { width: 17px; height: 17px; flex-shrink: 0; }
    &:hover { background: var(--color-hover); color: var(--color-primary); }
    &.active { background: var(--color-active); color: #7B61FF; font-weight: 500; }
  }

  .nav-divider { height: 1px; background: var(--color-border-light); margin: 8px 16px; }

  .nav-footer {
    margin-top: auto; padding: 14px 18px; border-top: 1px solid var(--color-border);
    .dark-toggle {
      display: flex; align-items: center; gap: 8px; width: 100%; padding: 8px 12px;
      border-radius: 8px; border: 1px solid var(--color-border); background: transparent;
      color: var(--color-text-secondary); font-size: 12px; cursor: pointer;
      transition: all var(--transition);
      &:hover { border-color: var(--color-primary); color: var(--color-primary); }
    }
  }
}
</style>
