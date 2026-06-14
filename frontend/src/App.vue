<template>
  <div id="app" class="trace-app">
    <template v-if="$route.path === '/login'">
      <router-view />
    </template>

    <el-container v-else class="main-layout">
      <!-- 顶部栏：仅保留品牌标识 -->
      <el-header class="top-header">
        <div class="header-left">
          <svg width="30" height="30" viewBox="0 0 32 32">
            <rect width="32" height="32" rx="8" fill="#6C5CE7"/>
            <text x="16" y="22" text-anchor="middle" fill="white" font-size="18" font-weight="700">T</text>
          </svg>
          <span class="header-brand">Trace · 个人成长轨迹</span>
        </div>
      </el-header>

      <el-container class="body-layout">
        <el-aside class="side-bar">
          <SideNav />
        </el-aside>
        <el-main class="main-content">
          <router-view />
        </el-main>
      </el-container>
    </el-container>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import SideNav from '@/components/SideNav.vue'

onMounted(() => {
  if (localStorage.getItem('trace-dark') === '1') {
    document.documentElement.classList.add('dark')
  }
})
</script>

<style lang="scss">
.trace-app {
  height: 100vh;
  background: var(--bg-gradient);

  .main-layout { height: 100%; display: flex; flex-direction: column; }

  .top-header {
    display: flex; align-items: center;
    height: 48px; flex-shrink: 0; padding: 0 24px;
    background: var(--color-surface);
    border-bottom: 1px solid var(--color-border-light);

    .header-left {
      display: flex; align-items: center; gap: 10px;
      .header-brand {
        font-size: 13px; color: var(--color-text-secondary);
        letter-spacing: 0.3px;
      }
    }
  }

  .body-layout { flex: 1; overflow: hidden; }

  .side-bar {
    width: 220px; background: var(--color-sidebar-bg); flex-shrink: 0;
    border-right: 1px solid var(--color-border-light);
    overflow-y: auto;
  }

  .main-content {
    flex: 1; overflow-y: auto;
    padding: 32px 44px;
    background: var(--bg-gradient);
  }
}
</style>
