<template>
  <div id="app" class="trace-app">
    <!-- 登录页不显示布局 -->
    <template v-if="$route.path === '/login'">
      <router-view />
    </template>

    <!-- 主布局 -->
    <el-container v-else class="main-container">
      <!-- 顶部栏 -->
      <el-header class="app-header">
        <div class="header-left">
          <span class="logo">📊 Trace</span>
          <span class="subtitle">个人成长轨迹</span>
        </div>
        <div class="header-right">
          <span class="username">{{ authStore.username }}</span>
          <el-button type="danger" text @click="handleLogout">退出登录</el-button>
        </div>
      </el-header>

      <el-container>
        <!-- 左侧导航 -->
        <el-aside width="200px" class="app-aside">
          <SideNav />
        </el-aside>

        <!-- 右侧内容区 -->
        <el-main class="app-main">
          <router-view />
        </el-main>
      </el-container>

      <!-- 底部状态栏 -->
      <el-footer class="app-footer">
        <span>Trace v0.1.0</span>
        <span>⚠️ AI 可能会出错，请保持独立判断</span>
        <span>© 2026 Trace</span>
      </el-footer>
    </el-container>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import SideNav from '@/components/SideNav.vue'

const router = useRouter()
const authStore = useAuthStore()

function handleLogout() {
  authStore.logout()
  router.push('/login')
}
</script>

<style lang="scss">
html, body, #app {
  margin: 0;
  padding: 0;
  height: 100%;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}

.trace-app {
  height: 100vh;
  display: flex;
  flex-direction: column;

  .main-container {
    height: 100%;
    display: flex;
    flex-direction: column;
  }

  .app-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    background: #1a1a2e;
    color: #fff;
    padding: 0 24px;
    height: 56px;
    flex-shrink: 0;

    .header-left {
      display: flex;
      align-items: center;
      gap: 12px;

      .logo {
        font-size: 20px;
        font-weight: 700;
      }

      .subtitle {
        font-size: 13px;
        color: #a0a0b8;
      }
    }

    .header-right {
      display: flex;
      align-items: center;
      gap: 12px;

      .username {
        color: #e0e0f0;
        font-size: 14px;
      }
    }
  }

  .app-aside {
    background: #f5f6fa;
    border-right: 1px solid #e8e8e8;
    overflow-y: auto;
  }

  .app-main {
    background: #ffffff;
    padding: 24px;
    overflow-y: auto;
    flex: 1;
  }

  .app-footer {
    display: flex;
    justify-content: space-between;
    align-items: center;
    background: #f5f6fa;
    border-top: 1px solid #e8e8e8;
    padding: 0 24px;
    height: 32px;
    font-size: 12px;
    color: #999;
    flex-shrink: 0;
  }
}
</style>
