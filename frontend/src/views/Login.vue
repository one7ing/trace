<template>
  <div class="login-shell">
    <!-- 左侧品牌区 -->
    <div class="login-left">
      <div class="left-bg">
        <div class="left-shape shape1"></div>
        <div class="left-shape shape2"></div>
        <div class="left-shape shape3"></div>
        <div class="left-shape shape4"></div>
      </div>
      <div class="left-illustration">
        <svg viewBox="0 0 320 280" fill="none" xmlns="http://www.w3.org/2000/svg" class="illus-svg">
          <!-- 底部网格线 -->
          <line x1="40" y1="240" x2="280" y2="240" stroke="rgba(0,0,0,.12)" stroke-width="1"/>
          <line x1="40" y1="200" x2="280" y2="200" stroke="rgba(0,0,0,.06)" stroke-width="1" stroke-dasharray="4 4"/>
          <line x1="40" y1="160" x2="280" y2="160" stroke="rgba(0,0,0,.06)" stroke-width="1" stroke-dasharray="4 4"/>
          <line x1="40" y1="120" x2="280" y2="120" stroke="rgba(0,0,0,.06)" stroke-width="1" stroke-dasharray="4 4"/>
          <line x1="40" y1="80" x2="280" y2="80" stroke="rgba(0,0,0,.06)" stroke-width="1" stroke-dasharray="4 4"/>
          <!-- Y轴 -->
          <line x1="40" y1="40" x2="40" y2="240" stroke="rgba(0,0,0,.15)" stroke-width="1.5"/>
          <!-- 面积图 -->
          <path d="M40 220 C70 210, 90 180, 120 190 C150 200, 170 140, 200 130 C230 120, 260 90, 280 70 L280 240 L40 240 Z"
            fill="url(#areaGrad)" opacity="0.5"/>
          <!-- 主线 -->
          <path d="M40 220 C70 210, 90 180, 120 190 C150 200, 170 140, 200 130 C230 120, 260 90, 280 70"
            stroke="#6C5CE7" stroke-width="2.5" stroke-linecap="round" fill="none"/>
          <!-- 数据点 -->
          <circle cx="120" cy="190" r="5" fill="#6C5CE7"/>
          <circle cx="200" cy="130" r="5" fill="#6C5CE7"/>
          <circle cx="280" cy="70" r="5" fill="#6C5CE7"/>
          <!-- 锚点标记 -->
          <circle cx="200" cy="130" r="14" fill="none" stroke="rgba(245,158,11,.6)" stroke-width="1.5" stroke-dasharray="3 3"/>
          <text x="218" y="134" fill="rgba(245,158,11,.9)" font-size="9">里程碑</text>
          <!-- 顶部浮动元素 -->
          <rect x="70" y="55" width="28" height="14" rx="7" fill="rgba(108,92,231,.08)"/>
          <rect x="155" y="35" width="22" height="12" rx="6" fill="rgba(108,92,231,.05)"/>
          <circle cx="260" cy="45" r="8" fill="none" stroke="rgba(108,92,231,.1)" stroke-width="1"/>
          <defs>
            <linearGradient id="areaGrad" x1="0" y1="0" x2="0" y2="1">
              <stop offset="0%" stop-color="rgba(108,92,231,.4)"/>
              <stop offset="100%" stop-color="rgba(108,92,231,0)"/>
            </linearGradient>
          </defs>
        </svg>
      </div>
      <div class="left-content">
        <h1 class="left-title">Trace</h1>
        <p class="left-desc">记录每一步成长<br/>看见每一天变化</p>
      </div>
      <div class="brand-footer">
        <span class="brand-version">v0.1.0</span>
      </div>
    </div>

    <!-- 右侧表单区 -->
    <div class="login-right">
      <div class="form-card">
        <div class="form-brand">
          <span class="form-brand-text">Trace</span>
        </div>
        <div class="form-tabs">
          <button :class="{ active: tab === 'login' }" @click="tab = 'login'">登录</button>
          <button :class="{ active: tab === 'register' }" @click="tab = 'register'">注册</button>
        </div>

        <template v-if="tab === 'login'">
          <div class="form-group">
            <label class="form-label">用户名</label>
            <div class="input-wrap">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" width="16" height="16" class="input-icon"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
              <input v-model="loginForm.username" placeholder="请输入用户名" @keydown.enter="handleLogin"/>
            </div>
          </div>
          <div class="form-group">
            <label class="form-label">密码</label>
            <div class="input-wrap">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" width="16" height="16" class="input-icon"><rect x="3" y="11" width="18" height="11" rx="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/></svg>
              <input v-model="loginForm.password" type="password" placeholder="请输入密码" @keydown.enter="handleLogin"/>
            </div>
          </div>
          <button class="submit-btn" @click="handleLogin" :disabled="loading">
            {{ loading ? '登录中...' : '登  录' }}
          </button>
        </template>

        <template v-else>
          <div class="form-group">
            <label class="form-label">用户名</label>
            <div class="input-wrap">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" width="16" height="16" class="input-icon"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
              <input v-model="registerForm.username" placeholder="3-50个字符"/>
            </div>
          </div>
          <div class="form-group">
            <label class="form-label">密码</label>
            <div class="input-wrap">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" width="16" height="16" class="input-icon"><rect x="3" y="11" width="18" height="11" rx="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/></svg>
              <input v-model="registerForm.password" type="password" placeholder="6-100个字符"/>
            </div>
          </div>
          <div class="form-group">
            <label class="form-label">邮箱 <span class="optional">选填</span></label>
            <div class="input-wrap">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" width="16" height="16" class="input-icon"><path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"/><polyline points="22,6 12,13 2,6"/></svg>
              <input v-model="registerForm.email" placeholder="your@email.com"/>
            </div>
          </div>
          <button class="submit-btn" @click="handleRegister" :disabled="loading">
            {{ loading ? '注册中...' : '注  册' }}
          </button>
        </template>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import api from '@/api'
import { ElMessage } from 'element-plus'

const router = useRouter()
const authStore = useAuthStore()
const tab = ref('login')
const loading = ref(false)
const loginForm = reactive({ username:'', password:'' })
const registerForm = reactive({ username:'', password:'', email:'' })

async function handleLogin() {
  if (!loginForm.username || !loginForm.password) { ElMessage.warning('请输入用户名和密码'); return }
  loading.value = true
  try { const res:any = await api.post('/auth/login', loginForm); authStore.setAuth(res.data); router.push('/dashboard') }
  catch {} finally { loading.value = false }
}
async function handleRegister() {
  if (!registerForm.username || !registerForm.password) { ElMessage.warning('请输入用户名和密码'); return }
  if (registerForm.username.length < 3) { ElMessage.warning('用户名至少3个字符'); return }
  if (registerForm.password.length < 6) { ElMessage.warning('密码至少6个字符'); return }
  loading.value = true
  try { const res:any = await api.post('/auth/register', registerForm); authStore.setAuth(res.data); ElMessage.success('注册成功'); router.push('/dashboard') }
  catch {} finally { loading.value = false }
}
</script>

<style lang="scss" scoped>
.login-shell {
  display: flex; height: 100vh; overflow: hidden;
  background: #f2f1f8;
}

// ── 左侧品牌 ──
.login-left {
  flex: 1; position: relative; overflow: hidden;
  background: linear-gradient(160deg, #eae7f5 0%, #e0ddf0 30%, #e8e4f4 60%, #f0edf7 100%);
  display: flex; flex-direction: column; align-items: center; justify-content: center;
}
.left-bg {
  position: absolute; inset: 0; overflow: hidden;
  .left-shape {
    position: absolute; border-radius: 50%; opacity: .06;
    &.shape1 { width: 500px; height: 500px; background: #6C5CE7; top: -150px; right: -200px; }
    &.shape2 { width: 300px; height: 300px; background: #a78bfa; bottom: -80px; left: -80px; }
    &.shape3 { width: 200px; height: 200px; background: #f59e0b; top: 40%; right: -60px; opacity: .04; }
    &.shape4 { width: 160px; height: 160px; background: #6C5CE7; top: 20%; left: -40px; opacity: .08; }
  }
}
.left-illustration {
  position: relative; z-index: 1; width: 340px; margin-bottom: 24px;
  .illus-svg { width: 100%; height: auto; }
}
.left-content {
  position: relative; z-index: 1; text-align: center;
  .left-title {
    font-size: 34px; font-weight: 700; color: #3d3d52; margin: 0 0 8px;
    letter-spacing: 3px;
  }
  .left-desc {
    font-size: 14px; color: #8e8ea0; margin: 0; line-height: 1.8;
    letter-spacing: 1px;
  }
}
.brand-footer { position: absolute; bottom: 28px; left: 28px; .brand-version { font-size: 11px; color: #b0b0c0; } }

// ── 右侧表单 ──
.login-right {
  width: 440px; display: flex; align-items: center; justify-content: center;
  background: #fff; box-shadow: -4px 0 30px rgba(0,0,0,.05);
}
.form-card { width: 340px; }
.form-brand {
  text-align: center; margin-bottom: 20px;
  .form-brand-text {
    font-size: 26px; font-weight: 700; letter-spacing: 2px;
    background: linear-gradient(135deg, #6C5CE7 0%, #a78bfa 100%);
    -webkit-background-clip: text; -webkit-text-fill-color: transparent;
    background-clip: text;
    font-family: 'Georgia', 'Times New Roman', serif;
    font-style: italic;
  }
}
.form-tabs {
  display: flex; gap: 0; margin-bottom: 32px; border-bottom: 2px solid #eeeef4;
  button {
    flex: 1; padding: 12px 0; border: none; background: none; font-size: 15px;
    color: #b0b0c0; cursor: pointer; font-weight: 500; position: relative; transition: color .2s;
    &.active { color: #6C5CE7;
      &::after { content:''; position:absolute; bottom:-2px; left:20%; width:60%; height:2px; background:#6C5CE7; border-radius:1px; }
    }
    &:hover:not(.active) { color: #888; }
  }
}
.form-group { margin-bottom: 20px; }
.form-label { display: block; font-size: 13px; color: #6e7088; margin-bottom: 8px; font-weight: 500;
  .optional { color: #b0b0c0; font-weight: 400; font-size: 11px; }
}
.input-wrap {
  display: flex; align-items: center; gap: 10px;
  padding: 0 14px; height: 44px; border: 1.5px solid #e4e4ee; border-radius: 12px;
  background: #fafafd; transition: all .2s;
  &:focus-within { border-color: #6C5CE7; background: #fff; box-shadow: 0 0 0 3px rgba(108,92,231,.06); }
  .input-icon { color: #c0c0d4; flex-shrink: 0; }
  input {
    flex: 1; border: none; outline: none; background: transparent;
    font-size: 14px; color: #2d2d3f;
    &::placeholder { color: #c4c4d4; }
  }
}
.submit-btn {
  width: 100%; height: 46px; border: none; border-radius: 12px;
  background: linear-gradient(135deg, #6C5CE7, #8B7CF0);
  color: #fff; font-size: 15px; font-weight: 600; cursor: pointer; margin-top: 8px;
  transition: all .2s;
  &:hover:not(:disabled) { transform: translateY(-1px); box-shadow: 0 4px 16px rgba(108,92,231,.25); }
  &:disabled { opacity: .5; cursor: default; }
}
</style>
