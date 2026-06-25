<template>
  <div id="app" class="trace-app">
    <template v-if="$route.path === '/login'">
      <router-view />
    </template>

    <div v-else class="app-shell">
      <!-- 顶部栏 -->
      <el-header class="top-header">
        <div class="header-left">
          <svg width="28" height="28" viewBox="0 0 32 32" class="brand-logo">
            <rect width="32" height="32" rx="8" fill="url(#logoGrad)"/>
            <defs><linearGradient id="logoGrad" x1="0%" y1="0%" x2="100%" y2="100%">
              <stop offset="0%" stop-color="#6C5CE7"/><stop offset="100%" stop-color="#8B7CF0"/>
            </linearGradient></defs>
            <text x="16" y="22" text-anchor="middle" fill="white" font-size="18" font-weight="700">T</text>
          </svg>
          <span class="header-brand">Trace</span>
          <!-- 邮箱胶囊下拉 -->
          <el-dropdown trigger="click" @command="handlePillCommand">
            <div class="email-pill">
              <span class="pill-avatar" :style="pillAvatarStyle">{{ pillAvatarInitial }}</span>
              <span class="pill-email">{{ authStore.username || authStore.email || '未登录' }}</span>
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="12" height="12" class="pill-arrow"><polyline points="6 9 12 15 18 9"/></svg>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="avatar">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" width="15" height="15" style="margin-right:6px;vertical-align:-3px"><rect x="3" y="3" width="18" height="18" rx="2"/><circle cx="8.5" cy="8.5" r="1.5"/><polyline points="21 15 16 10 5 21"/></svg>
                  修改头像
                </el-dropdown-item>
                <el-dropdown-item command="username">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" width="15" height="15" style="margin-right:6px;vertical-align:-3px"><path d="M12 20h9"/><path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z"/></svg>
                  修改昵称
                </el-dropdown-item>
                <el-dropdown-item command="logout" divided>
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" width="15" height="15" style="margin-right:6px;vertical-align:-3px"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/><polyline points="16 17 21 12 16 7"/><line x1="21" y1="12" x2="9" y2="12"/></svg>
                  退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
          <input type="file" accept="image/*" ref="pillAvatarInput" @change="handlePillAvatarUpload" style="display:none"/>
        </div>
      </el-header>

      <!-- 主体：左右两个独立圆角面板 -->
      <div class="body-layout">
        <el-aside class="side-bar">
          <SideNav />
        </el-aside>
        <el-main class="main-content">
          <router-view v-slot="{ Component }">
            <Transition name="page-fade" mode="out-in">
              <component :is="Component" />
            </Transition>
          </router-view>
          <div class="page-footer">Trace · 记录每一步成长</div>
        </el-main>
      </div>
    </div>

    <!-- 昵称修改弹窗 -->
    <el-dialog v-model="showUsernameDialog" title="修改昵称" width="360px">
      <el-input v-model="newUsername" placeholder="输入新昵称（至少3个字符）" maxlength="50"/>
      <template #footer>
        <el-button @click="showUsernameDialog=false">取消</el-button>
        <el-button type="primary" @click="saveUsername">保存</el-button>
      </template>
    </el-dialog>

  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import SideNav from '@/components/SideNav.vue'
import { useAuthStore } from '@/stores/auth'
import api from '@/api'
import { ElMessage } from 'element-plus'

const authStore = useAuthStore()
const router = useRouter()

// 深色模式
onMounted(() => {
  if (localStorage.getItem('trace-dark') === '1') {
    document.documentElement.classList.add('dark')
  }
})

// ── 头像 ──
const pillAvatarInput = ref<HTMLInputElement>()
const pillAvatarInitial = computed(() => authStore.username?.charAt(0)?.toUpperCase() || 'U')
const pillAvatarStyle = computed(() => authStore.avatarUrl
  ? { backgroundImage: `url(${authStore.avatarUrl})`, backgroundSize: 'cover', color: 'transparent' }
  : {})

// ── 昵称 ──
const showUsernameDialog = ref(false)
const newUsername = ref('')

/** 胶囊下拉菜单 */
async function handlePillCommand(command: string) {
  switch (command) {
    case 'avatar':
      pillAvatarInput.value?.click()
      break
    case 'username':
      newUsername.value = authStore.username || ''
      showUsernameDialog.value = true
      break
    case 'logout':
      await authStore.logout()
      router.push('/login')
      break
  }
}

/** 保存昵称 */
async function saveUsername() {
  if (!newUsername.value || newUsername.value.trim().length < 3) {
    ElMessage.warning('昵称至少3个字符')
    return
  }
  try {
    const res: any = await api.put('/auth/username', { username: newUsername.value.trim() })
    authStore.setAuth({ ...authStore.$state, username: res.data })
    localStorage.setItem('trace-username', res.data)
    showUsernameDialog.value = false
    ElMessage.success('昵称已更新')
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '修改失败')
  }
}

/** 上传头像 */
async function handlePillAvatarUpload(e: Event) {
  const t = e.target as HTMLInputElement
  const file = t.files?.[0]
  if (!file) return
  const reader = new FileReader()
  reader.onload = async (ev) => {
    const base64 = ev.target?.result as string
    // 立即更新本地显示
    authStore.avatarUrl = base64
    localStorage.setItem('trace-avatarUrl', base64)
    const form = new FormData()
    form.append('file', file)
    try {
      await api.post('/auth/avatar', form, { headers: { 'Content-Type': 'multipart/form-data' } })
      ElMessage.success('头像已更新')
    } catch { ElMessage.error('上传失败') }
  }
  reader.readAsDataURL(file)
}
</script>

<style lang="scss">
.trace-app {
  height: 100vh;
  display: flex; align-items: center; justify-content: center;
  background: var(--color-page-bg);

  // ── 外层玻璃壳 ──
  .app-shell {
    width: calc(100vw - 24px); height: calc(100vh - 24px);
    display: flex; flex-direction: column;
    border-radius: var(--radius-shell);
    background: var(--color-shell-bg);
    border: 1px solid var(--color-shell-border);
    box-shadow: var(--shadow-shell);
    overflow: hidden;
    // 毛玻璃
    backdrop-filter: blur(16px);
    -webkit-backdrop-filter: blur(16px);
  }

  // ── 顶部栏 ──
  .top-header {
    display: flex; align-items: center; justify-content: flex-start;
    height: 52px; flex-shrink: 0; padding: 0 22px;
    border-bottom: 1px solid var(--color-border-light);

    .header-left {
      display: flex; align-items: center; gap: 12px;
      .brand-logo { flex-shrink: 0; }
      .header-brand {
        font-family: 'Georgia', 'Palatino Linotype', 'Book Antiqua', serif;
        font-size: 18px; font-weight: 700; font-style: italic; letter-spacing: 2px;
        color: var(--color-text);
      }
      // ── 邮箱胶囊（透明）──
      .email-pill {
        display: flex; align-items: center; gap: 8px;
        padding: 5px 14px 5px 5px; border-radius: 22px; cursor: pointer;
        background: rgba(108,92,231,.06);
        border: 1px solid rgba(108,92,231,.12);
        transition: all var(--transition-fast);
        &:hover { background: rgba(108,92,231,.10); border-color: rgba(108,92,231,.25); }
        .pill-avatar {
          display: flex; align-items: center; justify-content: center;
          width: 26px; height: 26px; border-radius: 50%; flex-shrink: 0;
          background: var(--color-primary-gradient);
          color: #fff; font-size: 11px; font-weight: 600;
        }
        .pill-email {
          font-size: 12px; color: var(--color-text-secondary); font-weight: 500;
          white-space: nowrap; overflow: hidden; text-overflow: ellipsis; max-width: 180px;
        }
        .pill-arrow { opacity: 0.4; flex-shrink: 0; color: var(--color-text-muted); }
      }
    }
  }

  // ── 主体：左右两个面板 ──
  .body-layout {
    flex: 1; display: flex; padding: 10px; gap: 10px; overflow: hidden;
  }

  // ── 左侧导航面板 ──
  .side-bar {
    width: 200px; flex-shrink: 0; overflow-y: auto;
    border-radius: 16px;
    background: var(--color-sidebar-bg);
    box-shadow: var(--shadow-panel);
    // 无 el-aside 默认边框
    border: none;
  }

  // ── 右侧内容面板 ──
  .main-content {
    flex: 1; overflow-y: auto;
    border-radius: 16px;
    background: var(--color-card);
    box-shadow: var(--shadow-panel);
    padding: 24px 32px 0;
    position: relative;
  }

  // ── 页脚 ──
  .page-footer {
    text-align: center; padding: 20px 0 16px;
    font-size: 11px; color: var(--color-text-muted);
    opacity: 0.45;
  }

  // ── 页面过渡动效 ──
  .page-fade-enter-active, .page-fade-leave-active {
    transition: opacity 0.2s ease, transform 0.2s ease;
  }
  .page-fade-enter-from { opacity: 0; transform: translateY(6px); }
  .page-fade-leave-to { opacity: 0; transform: translateY(-6px); }
}
</style>
