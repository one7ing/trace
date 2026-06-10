<template>
  <div id="app" class="trace-app">
    <template v-if="$route.path === '/login'">
      <router-view />
    </template>

    <el-container v-else class="main-layout">
      <!-- 顶部导航 -->
      <el-header class="top-header">
        <div class="header-left">
          <div class="brand">
            <svg class="brand-icon" width="28" height="28" viewBox="0 0 32 32">
              <rect width="32" height="32" rx="8" fill="#6C5CE7"/>
              <text x="16" y="22" text-anchor="middle" fill="white" font-size="18" font-weight="700">T</text>
            </svg>
            <span class="brand-name">Trace</span>
          </div>
        </div>
        <div class="header-right">
          <span class="user-name">{{ authStore.username }}</span>
          <el-dropdown trigger="click">
            <span class="user-avatar">{{ authStore.username?.charAt(0)?.toUpperCase() || 'U' }}</span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="handleLogout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
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
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import SideNav from '@/components/SideNav.vue'

const router = useRouter()
const authStore = useAuthStore()

onMounted(() => {
  if (localStorage.getItem('trace-dark') === '1') {
    document.documentElement.classList.add('dark')
  }
})

function handleLogout() { authStore.logout(); router.push('/login') }
</script>

<style lang="scss">
.trace-app {
  height: 100vh;
  background: var(--bg-gradient);

  .main-layout { height: 100%; display: flex; flex-direction: column; }

  .top-header {
    display: flex; align-items: center; justify-content: space-between;
    height: 54px; flex-shrink: 0; padding: 0 28px;
    background: var(--color-surface);
    border-bottom: 1px solid var(--color-border-light);

    .header-left {
      display: flex; align-items: center;
      .brand { display: flex; align-items: center; gap: 10px; }
      .brand-icon { flex-shrink: 0; }
      .brand-name { font-size: 18px; font-weight: 700; color: var(--color-text); letter-spacing: 0.5px; }
    }
    .header-right {
      display: flex; align-items: center; gap: 12px;
      .user-name { color: var(--color-text-secondary); font-size: 13px; }
      .user-avatar {
        width: 32px; height: 32px; border-radius: 10px;
        background: var(--color-primary); color: #fff; display: flex; align-items: center; justify-content: center;
        font-size: 14px; font-weight: 600; cursor: pointer; transition: opacity var(--transition);
        &:hover { opacity: 0.85; }
      }
    }
  }

  .body-layout { flex: 1; overflow: hidden; }

  .side-bar {
    width: 200px; background: var(--color-sidebar-bg); flex-shrink: 0;
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
