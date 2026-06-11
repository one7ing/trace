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
            <span class="user-avatar" :style="userAvatarUrl ? { backgroundImage: 'url('+userAvatarUrl+')', backgroundSize:'cover' } : {}">{{ userAvatarUrl ? '' : (authStore.username?.charAt(0)?.toUpperCase() || 'U') }}</span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="showAvatarDialog = true">🎨 设置头像</el-dropdown-item>
                <el-dropdown-item @click="showUsernameDialog = true">✏️ 修改昵称</el-dropdown-item>
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

    <!-- 用户名弹窗 -->
    <el-dialog v-model="showUsernameDialog" title="修改昵称" width="360px">
      <el-input v-model="newUsername" placeholder="输入新昵称（至少3个字符）" maxlength="50"/>
      <template #footer>
        <el-button @click="showUsernameDialog=false">取消</el-button>
        <el-button type="primary" @click="saveUsername">保存</el-button>
      </template>
    </el-dialog>

    <!-- 头像设置弹窗 -->
    <el-dialog v-model="showAvatarDialog" title="设置头像" width="400px">
      <div style="text-align:center">
        <div class="avatar-preview-lg" :style="{ backgroundImage: userAvatarUrl ? 'url('+userAvatarUrl+')' : 'none' }">
          <span v-if="!userAvatarUrl">{{ authStore.username?.charAt(0)?.toUpperCase() || 'U' }}</span>
        </div>
        <input type="file" accept="image/*" ref="avatarInputRef" @change="handleAvatarUpload" style="display:none"/>
        <el-button @click="($refs.avatarInputRef as HTMLInputElement).click()" style="margin-top:12px">选择图片</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import SideNav from '@/components/SideNav.vue'
import api from '@/api'
import { ElMessage } from 'element-plus'

const router = useRouter()
const authStore = useAuthStore()
const showAvatarDialog = ref(false)
const showUsernameDialog = ref(false)
const newUsername = ref(authStore.username || '')
const userAvatarUrl = ref(localStorage.getItem('trace-user-avatar-img') || '')
const avatarInputRef = ref<HTMLInputElement>()

async function saveUsername() {
  if (!newUsername.value || newUsername.value.trim().length < 3) { ElMessage.warning('昵称至少3个字符'); return }
  try {
    const res: any = await api.put('/auth/username', { username: newUsername.value.trim() })
    authStore.setAuth({ ...authStore.$state, username: res.data })
    localStorage.setItem('trace-username', res.data)
    showUsernameDialog.value = false
    ElMessage.success('昵称已更新')
  } catch (e: any) { ElMessage.error(e?.response?.data?.message || '修改失败') }
}

async function handleAvatarUpload(e: Event) {
  const t = e.target as HTMLInputElement; const file = t.files?.[0]; if (!file) return
  const reader = new FileReader()
  reader.onload = async (ev) => {
    userAvatarUrl.value = ev.target?.result as string
    localStorage.setItem('trace-user-avatar-img', userAvatarUrl.value)
    const form = new FormData(); form.append('file', file)
    try {
      await api.post('/auth/avatar', form, { headers: { 'Content-Type': 'multipart/form-data' } })
      showAvatarDialog.value = false
      ElMessage.success('头像已更新')
    } catch { ElMessage.error('上传失败') }
  }
  reader.readAsDataURL(file)
}

onMounted(() => {
  if (localStorage.getItem('trace-dark') === '1') document.documentElement.classList.add('dark')
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
      .avatar-preview-lg { width: 80px; height: 80px; border-radius: 16px; background: linear-gradient(135deg,#67c23a,#529b2e); color: #fff; font-size: 32px; font-weight: 700; display: inline-flex; align-items: center; justify-content: center; background-size: cover; background-position: center; }
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
