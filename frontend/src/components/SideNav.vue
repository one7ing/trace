<template>
  <div class="side-nav-wrapper">
    <!-- 用户头像区 -->
    <div class="user-section">
      <el-dropdown trigger="click" @command="handleUserCommand">
        <span class="user-avatar-wrapper">
          <span
            class="user-avatar"
            :style="userAvatarUrl
              ? { backgroundImage: 'url(' + userAvatarUrl + ')', backgroundSize: 'cover' }
              : {}"
          >{{ userAvatarUrl ? '' : userInitial }}</span>
        </span>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="avatar">🎨 设置头像</el-dropdown-item>
            <el-dropdown-item command="username">✏️ 修改昵称</el-dropdown-item>
            <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
      <span class="user-name">{{ authStore.username }}</span>
      <el-dropdown trigger="click" @command="handleUserCommand">
        <span class="user-settings">个人设置 ▾</span>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="avatar">🎨 设置头像</el-dropdown-item>
            <el-dropdown-item command="username">✏️ 修改昵称</el-dropdown-item>
            <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>

    <div class="nav-divider"></div>

    <!-- 导航菜单 -->
    <router-link to="/dashboard" class="nav-item" :class="{ active: isActive('/dashboard') }">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" class="nav-icon">
        <rect x="3" y="3" width="7" height="7"/><rect x="14" y="3" width="7" height="7"/>
        <rect x="14" y="14" width="7" height="7"/><rect x="3" y="14" width="7" height="7"/>
      </svg>
      <span>仪表盘</span>
    </router-link>

    <router-link to="/knowledge" class="nav-item" :class="{ active: isActive('/knowledge') }">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" class="nav-icon">
        <circle cx="12" cy="12" r="10"/><path d="M12 16v-4"/><path d="M12 8h.01"/>
      </svg>
      <span>Trace 问答</span>
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

    <router-link to="/knowledge-base" class="nav-item" :class="{ active: isActive('/knowledge-base') }">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" class="nav-icon">
        <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"/><path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"/>
      </svg>
      <span>知识库</span>
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

    <!-- 底部：深色切换 -->
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

    <!-- 头像设置弹窗 -->
    <el-dialog v-model="showAvatarDialog" title="设置头像" width="400px">
      <div style="text-align:center">
        <div class="avatar-preview-lg" :style="{ backgroundImage: userAvatarUrl ? 'url('+userAvatarUrl+')' : 'none' }">
          <span v-if="!userAvatarUrl">{{ userInitial }}</span>
        </div>
        <input type="file" accept="image/*" ref="avatarInputRef" @change="handleAvatarUpload" style="display:none"/>
        <el-button @click="($refs.avatarInputRef as HTMLInputElement).click()" style="margin-top:12px">选择图片</el-button>
      </div>
    </el-dialog>

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
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import api from '@/api'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const isDark = ref(false)
const showAvatarDialog = ref(false)
const showUsernameDialog = ref(false)
const newUsername = ref(authStore.username || '')
const userAvatarUrl = ref(localStorage.getItem('trace-user-avatar-img') || '')
const avatarInputRef = ref<HTMLInputElement>()

const userInitial = computed(() => authStore.username?.charAt(0)?.toUpperCase() || 'U')

function isActive(path: string) {
  // 精确匹配或子路径，避免 /knowledge 误匹配 /knowledge-base
  return route.path === path || route.path.startsWith(path + '/')
}

/** 处理用户下拉菜单命令 */
function handleUserCommand(command: string) {
  switch (command) {
    case 'avatar':
      showAvatarDialog.value = true
      break
    case 'username':
      newUsername.value = authStore.username || ''
      showUsernameDialog.value = true
      break
    case 'logout':
      authStore.logout()
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
async function handleAvatarUpload(e: Event) {
  const t = e.target as HTMLInputElement
  const file = t.files?.[0]
  if (!file) {
    return
  }
  const reader = new FileReader()
  reader.onload = async (ev) => {
    userAvatarUrl.value = ev.target?.result as string
    localStorage.setItem('trace-user-avatar-img', userAvatarUrl.value)
    const form = new FormData()
    form.append('file', file)
    try {
      await api.post('/auth/avatar', form, { headers: { 'Content-Type': 'multipart/form-data' } })
      showAvatarDialog.value = false
      ElMessage.success('头像已更新')
    } catch {
      ElMessage.error('上传失败')
    }
  }
  reader.readAsDataURL(file)
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

  // === 用户区 ===
  .user-section {
    display: flex; flex-direction: column; align-items: center;
    padding: 18px 10px 10px; gap: 8px;
    .user-avatar-wrapper {
      display: inline-block; cursor: pointer;
      .user-avatar {
        display: flex; align-items: center; justify-content: center;
        width: 48px; height: 48px; border-radius: 14px;
        background: linear-gradient(135deg, #6C5CE7, #8B7CF0);
        color: #fff; font-size: 20px; font-weight: 600;
        transition: transform var(--transition), box-shadow var(--transition);
        &:hover { transform: scale(1.05); box-shadow: 0 4px 14px rgba(108,92,231,.25); }
      }
    }
    .user-name {
      font-size: 13px; font-weight: 600; color: var(--color-text);
      max-width: 160px; text-align: center;
      white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
    }
    .user-settings {
      font-size: 11px; color: var(--color-text-muted); cursor: pointer;
      padding: 2px 10px; border-radius: 4px;
      transition: all var(--transition);
      &:hover { color: var(--color-primary); background: var(--color-primary-bg); }
    }
  }

  // === 导航项 ===
  .nav-item {
    display: flex; align-items: center; gap: 10px; padding: 9px 18px; margin: 1px 8px;
    border-radius: 8px; font-size: 13px; color: var(--color-text-secondary); text-decoration: none;
    transition: all var(--transition);
    .nav-icon { width: 17px; height: 17px; flex-shrink: 0; }
    &:hover { background: var(--color-hover); color: var(--color-primary); }
    &.active { background: var(--color-active); color: #7B61FF; font-weight: 500; }
  }

  .nav-divider { height: 1px; background: var(--color-border-light); margin: 8px 16px; }

  // === 底部 ===
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

  // === 头像预览（弹窗内） ===
  .avatar-preview-lg {
    width: 80px; height: 80px; border-radius: 16px;
    background: linear-gradient(135deg, #6C5CE7, #8B7CF0);
    color: #fff; font-size: 32px; font-weight: 700;
    display: inline-flex; align-items: center; justify-content: center;
    background-size: cover; background-position: center;
  }
}
</style>
