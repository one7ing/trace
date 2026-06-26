<template>
  <div class="login-shell">
    <div class="login-card">
      <!-- 左侧：项目插画 -->
      <div class="card-illust">
        <svg viewBox="0 0 320 380" fill="none" xmlns="http://www.w3.org/2000/svg" class="illust-svg">
          <!-- 背景网格 -->
          <line x1="30" y1="340" x2="290" y2="340" stroke="rgba(108,92,231,.12)" stroke-width="1"/>
          <line x1="30" y1="290" x2="290" y2="290" stroke="rgba(108,92,231,.06)" stroke-width="1" stroke-dasharray="4 4"/>
          <line x1="30" y1="240" x2="290" y2="240" stroke="rgba(108,92,231,.06)" stroke-width="1" stroke-dasharray="4 4"/>
          <line x1="30" y1="190" x2="290" y2="190" stroke="rgba(108,92,231,.06)" stroke-width="1" stroke-dasharray="4 4"/>
          <line x1="30" y1="140" x2="290" y2="140" stroke="rgba(108,92,231,.06)" stroke-width="1" stroke-dasharray="4 4"/>
          <line x1="30" y1="90" x2="290" y2="90" stroke="rgba(108,92,231,.06)" stroke-width="1" stroke-dasharray="4 4"/>
          <!-- Y轴 -->
          <line x1="30" y1="40" x2="30" y2="340" stroke="rgba(108,92,231,.2)" stroke-width="1.5"/>
          <!-- 面积渐变 -->
          <defs>
            <linearGradient id="areaGradL" x1="0" y1="0" x2="0" y2="1">
              <stop offset="0%" stop-color="rgba(108,92,231,.35)"/>
              <stop offset="100%" stop-color="rgba(108,92,231,0)"/>
            </linearGradient>
            <linearGradient id="lineGradL" x1="0" y1="0" x2="1" y2="0">
              <stop offset="0%" stop-color="#6C5CE7"/>
              <stop offset="100%" stop-color="#a78bfa"/>
            </linearGradient>
          </defs>
          <!-- 面积 -->
          <path d="M30 320 C60 300, 80 270, 110 280 C140 290, 160 200, 190 180 C220 160, 250 110, 290 80 L290 340 L30 340 Z" fill="url(#areaGradL)" opacity="0.6"/>
          <!-- 主线 -->
          <path d="M30 320 C60 300, 80 270, 110 280 C140 290, 160 200, 190 180 C220 160, 250 110, 290 80" stroke="url(#lineGradL)" stroke-width="2.5" stroke-linecap="round" fill="none"/>
          <!-- 数据点 -->
          <circle cx="110" cy="280" r="6" fill="#6C5CE7" stroke="#fff" stroke-width="2"/>
          <circle cx="190" cy="180" r="6" fill="#8B7CF0" stroke="#fff" stroke-width="2"/>
          <circle cx="290" cy="80" r="6" fill="#a78bfa" stroke="#fff" stroke-width="2"/>
          <!-- 锚点 -->
          <circle cx="190" cy="180" r="16" fill="none" stroke="rgba(245,158,11,.5)" stroke-width="1.5" stroke-dasharray="4 3"/>
          <text x="215" y="184" fill="rgba(245,158,11,.85)" font-size="10" font-weight="500">里程碑</text>
          <!-- 浮动元素 -->
          <rect x="60" y="70" width="36" height="16" rx="8" fill="rgba(108,92,231,.06)"/>
          <rect x="145" y="45" width="28" height="12" rx="6" fill="rgba(108,92,231,.04)"/>
          <circle cx="270" cy="55" r="10" fill="none" stroke="rgba(108,92,231,.08)" stroke-width="1"/>
          <!-- 打卡小格子 -->
          <rect x="50" y="355" width="22" height="22" rx="6" fill="#6C5CE7" opacity="0.8"/>
          <rect x="78" y="355" width="22" height="22" rx="6" fill="#38bdf8" opacity="0.8"/>
          <rect x="106" y="355" width="22" height="22" rx="6" fill="none" stroke="rgba(108,92,231,.2)" stroke-width="1.5" stroke-dasharray="3 3"/>
          <rect x="134" y="355" width="22" height="22" rx="6" fill="none" stroke="rgba(108,92,231,.2)" stroke-width="1.5" stroke-dasharray="3 3"/>
          <rect x="162" y="355" width="22" height="22" rx="6" fill="#f56c6c" opacity="0.8"/>
          <rect x="190" y="355" width="22" height="22" rx="6" fill="#a78bfa" opacity="0.8"/>
          <rect x="218" y="355" width="22" height="22" rx="6" fill="#f59e0b" opacity="0.8"/>
        </svg>
        <p class="illust-tagline">记录每一步成长<br/>看见每一天变化</p>
      </div>

      <!-- 右侧：表单 -->
      <div class="card-form">
        <div class="form-brand">
          <span class="brand-text">Trace</span>
        </div>
        <div class="form-tabs">
          <button :class="{ active: tab === 'login' }" @click="tab = 'login'">登录</button>
          <button :class="{ active: tab === 'register' }" @click="tab = 'register'">注册</button>
          <button :class="{ active: tab === 'forgot' }" @click="tab = 'forgot'">忘记密码</button>
        </div>

        <!-- 登录 -->
        <template v-if="tab === 'login'">
          <div class="form-group">
            <div class="input-wrap">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" width="16" height="16" class="input-icon"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
              <input v-model="loginForm.account" placeholder="用户名或邮箱" @keydown.enter="handleLogin"/>
            </div>
          </div>
          <div class="form-group">
            <div class="input-wrap">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" width="16" height="16" class="input-icon"><rect x="3" y="11" width="18" height="11" rx="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/></svg>
              <input v-model="loginForm.password" type="password" placeholder="密码" @keydown.enter="handleLogin"/>
            </div>
          </div>
          <button class="submit-btn" @click="handleLogin" :disabled="loading">
            {{ loading ? '登录中...' : '登  录' }}
          </button>
          <p class="form-hint"><a href="#" @click.prevent="tab='forgot'">忘记密码？</a></p>
        </template>

        <!-- 注册 -->
        <template v-if="tab === 'register'">
          <div class="form-group">
            <div class="input-wrap">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" width="16" height="16" class="input-icon"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
              <input v-model="registerForm.username" placeholder="用户名（3-50字符）"/>
            </div>
          </div>
          <div class="form-group">
            <div class="input-wrap">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" width="16" height="16" class="input-icon"><path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"/><polyline points="22,6 12,13 2,6"/></svg>
              <input v-model="registerForm.email" placeholder="邮箱（必填）"/>
            </div>
          </div>
          <div class="form-group">
            <div class="input-wrap">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" width="16" height="16" class="input-icon"><rect x="3" y="11" width="18" height="11" rx="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/></svg>
              <input v-model="registerForm.password" type="password" placeholder="密码（至少6位）"/>
            </div>
          </div>
          <button class="submit-btn" @click="handleRegister" :disabled="loading">
            {{ loading ? '注册中...' : '注  册' }}
          </button>
        </template>

        <!-- 忘记密码 -->
        <template v-if="tab === 'forgot'">
          <p class="forgot-desc">输入注册邮箱和新密码即可重置</p>
          <div class="form-group">
            <div class="input-wrap">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" width="16" height="16" class="input-icon"><path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"/><polyline points="22,6 12,13 2,6"/></svg>
              <input v-model="forgotForm.email" placeholder="注册邮箱"/>
            </div>
          </div>
          <div class="form-group">
            <div class="input-wrap">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" width="16" height="16" class="input-icon"><rect x="3" y="11" width="18" height="11" rx="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/></svg>
              <input v-model="forgotForm.newPassword" type="password" placeholder="新密码（至少6位）"/>
            </div>
          </div>
          <button class="submit-btn" @click="handleForgot" :disabled="loading">
            {{ loading ? '重置中...' : '重置密码' }}
          </button>
          <p class="form-hint"><a href="#" @click.prevent="tab='login'">← 返回登录</a></p>
        </template>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import api from '@/api'
import { ElMessage } from 'element-plus'

const router = useRouter()
const authStore = useAuthStore()
const tab = ref<'login'|'register'|'forgot'>('login')
const loading = ref(false)
const loginForm = reactive({ account:'', password:'' })
const registerForm = reactive({ username:'', email:'', password:'' })
const forgotForm = reactive({ email:'', newPassword:'' })

// 已登录则自动跳转到首页
onMounted(() => {
  if (localStorage.getItem('trace-accessToken')) {
    router.push('/dashboard')
  }
})

/** 判断是否为邮箱格式 */
function isEmail(s: string) { return s.includes('@') }

async function handleLogin() {
  if (!loginForm.account || !loginForm.password) { ElMessage.warning('请输入账号和密码'); return }
  loading.value = true
  try {
    const body: any = { password: loginForm.password }
    if (isEmail(loginForm.account)) body.email = loginForm.account
    else body.username = loginForm.account
    const res: any = await api.post('/auth/login', body)
    console.log('Login response:', res) // debug
    authStore.setAuth(res.data)
    router.push('/dashboard')
  } catch {} finally { loading.value = false }
}

async function handleRegister() {
  if (!registerForm.username || !registerForm.email || !registerForm.password) {
    ElMessage.warning('请填写所有字段'); return
  }
  if (registerForm.username.length < 3) { ElMessage.warning('用户名至少3个字符'); return }
  if (!registerForm.email.includes('@')) { ElMessage.warning('请输入正确的邮箱'); return }
  if (registerForm.password.length < 6) { ElMessage.warning('密码至少6个字符'); return }
  loading.value = true
  try {
    const res: any = await api.post('/auth/register', registerForm)
    authStore.setAuth(res.data)
    ElMessage.success('注册成功')
    router.push('/dashboard')
  } catch {} finally { loading.value = false }
}

async function handleForgot() {
  if (!forgotForm.email || !forgotForm.newPassword) { ElMessage.warning('请填写邮箱和新密码'); return }
  if (!forgotForm.email.includes('@')) { ElMessage.warning('请输入正确的邮箱'); return }
  if (forgotForm.newPassword.length < 6) { ElMessage.warning('新密码至少6个字符'); return }
  loading.value = true
  try {
    await api.post('/auth/forgot-password', forgotForm)
    ElMessage.success('密码已重置，请使用邮箱登录')
    tab.value = 'login'
    loginForm.account = forgotForm.email
    loginForm.password = ''
    forgotForm.email = ''
    forgotForm.newPassword = ''
  } catch {} finally { loading.value = false }
}
</script>

<style lang="scss" scoped>
.login-shell {
  display: flex; align-items: center; justify-content: center;
  height: 100vh; background: var(--color-page-bg);
}

// ── 悬浮卡片（左右分栏 + 入场动画）──
.login-card {
  display: flex; width: 720px; min-height: 480px;
  background: var(--color-card);
  border: 1px solid var(--color-shell-border);
  border-radius: var(--radius-shell);
  box-shadow: var(--shadow-shell); overflow: hidden;
  // 入场动画
  animation: cardEnter 0.5s cubic-bezier(0.4,0,0.2,1);
}
@keyframes cardEnter {
  from { opacity: 0; transform: translateY(12px) scale(0.98); }
  to { opacity: 1; transform: translateY(0) scale(1); }
}

// ── 左侧插画区 ──
.card-illust {
  width: 340px; flex-shrink: 0;
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  background: linear-gradient(160deg, #f4f2fb 0%, #edeaf7 40%, #f6f4fc 100%);
  padding: 40px 24px; position: relative; overflow: hidden;
  .illust-svg { width: 280px; height: auto; }
  .illust-tagline {
    margin-top: 16px; font-size: 12px; color: #8e8cb0; text-align: center;
    line-height: 1.8; letter-spacing: 0.5px;
  }
  // 数据点呼吸灯
  :deep(circle[fill="#6C5CE7"]), :deep(circle[fill="#8B7CF0"]), :deep(circle[fill="#a78bfa"]) {
    animation: breathe 2.5s ease-in-out infinite;
    &:nth-child(2) { animation-delay: 0.6s; }
    &:nth-child(3) { animation-delay: 1.2s; }
  }
}
@keyframes breathe {
  0%, 100% { opacity: 0.7; r: 6; }
  50% { opacity: 1; r: 8; }
}

// ── 右侧表单区 ──
.card-form {
  flex: 1; padding: 36px 32px 28px; display: flex; flex-direction: column;
}

.form-brand {
  text-align: center; margin-bottom: 16px;
  .brand-text {
    font-family: 'Georgia', 'Palatino Linotype', serif;
    font-size: 24px; font-weight: 700; font-style: italic; letter-spacing: 3px;
    background: linear-gradient(135deg, #6C5CE7, #8B7CF0);
    -webkit-background-clip: text; -webkit-text-fill-color: transparent;
    background-clip: text;
  }
}

// ── 标签切换（带滑动过渡）──
.form-tabs {
  display: flex; gap: 0; margin-bottom: 22px; border-bottom: 2px solid var(--color-border-light);
  button {
    flex: 1; padding: 8px 0; border: none; background: none; font-size: 13px;
    color: var(--color-text-muted); cursor: pointer; font-weight: 500; position: relative;
    transition: color 0.25s ease;
    &.active { color: var(--color-primary);
      &::after { content:''; position:absolute; bottom:-2px; left:20%; width:60%; height:2px; background:var(--color-primary); border-radius:1px;
        transition: left 0.25s ease, width 0.25s ease; }
    }
    &:hover:not(.active) { color: var(--color-text-secondary); }
  }
}

// ── 表单 ──
.form-group { margin-bottom: 14px; }
.input-wrap {
  display: flex; align-items: center; gap: 10px;
  padding: 0 14px; height: 42px; border: 1.5px solid var(--color-border); border-radius: 12px;
  background: var(--color-input); transition: all 0.2s ease;
  &:focus-within { border-color: var(--color-primary); box-shadow: 0 0 0 3px rgba(108,92,231,.08); transform: translateY(-1px); }
  .input-icon { color: var(--color-text-muted); flex-shrink: 0; transition: color 0.2s; }
  &:focus-within .input-icon { color: var(--color-primary); }
  input {
    flex: 1; border: none; outline: none; background: transparent;
    font-size: 13px; color: var(--color-text);
    &::placeholder { color: var(--color-text-muted); }
  }
}

.submit-btn {
  width: 100%; height: 44px; border: none; border-radius: 12px;
  background: var(--color-primary-gradient);
  color: #fff; font-size: 14px; font-weight: 600; cursor: pointer; margin-top: 4px;
  transition: all 0.25s ease; position: relative; overflow: hidden;
  &:hover:not(:disabled) { transform: translateY(-2px); box-shadow: var(--shadow-xl); }
  &:active:not(:disabled) { transform: translateY(0); }
  &:disabled { opacity: .5; cursor: default; }
  // loading 脉冲
  &:disabled::after {
    content: ''; position: absolute; inset: 0;
    background: rgba(255,255,255,.15);
    animation: pulse 1.2s ease-in-out infinite;
  }
}
@keyframes pulse {
  0%, 100% { opacity: 0.3; }
  50% { opacity: 0.7; }
}
.form-hint { text-align: center; margin-top: 12px;
  a { color: var(--color-text-muted); font-size: 12px; text-decoration: none; transition: color 0.2s;
    &:hover { color: var(--color-primary); } }
}
.forgot-desc { font-size: 12px; color: var(--color-text-muted); text-align: center; margin-bottom: 14px; }
</style>
