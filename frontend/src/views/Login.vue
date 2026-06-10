<template>
  <div class="login-page">
    <div class="login-card">
      <div class="login-brand">
        <svg width="40" height="40" viewBox="0 0 32 32">
          <rect width="32" height="32" rx="8" fill="#6C5CE7"/>
          <text x="16" y="22" text-anchor="middle" fill="white" font-size="18" font-weight="700">T</text>
        </svg>
        <h1>Trace</h1>
        <p>个人成长轨迹记录与分析系统</p>
      </div>

      <el-tabs v-model="activeTab" class="login-tabs">
        <el-tab-pane label="登录" name="login">
          <div class="form-fields">
            <input v-model="loginForm.username" placeholder="用户名" class="form-input" @keydown.enter="handleLogin" />
            <input v-model="loginForm.password" type="password" placeholder="密码" class="form-input" @keydown.enter="handleLogin" />
            <button class="form-btn" @click="handleLogin" :disabled="loading">
              {{ loading ? '登录中...' : '登 录' }}
            </button>
          </div>
        </el-tab-pane>

        <el-tab-pane label="注册" name="register">
          <div class="form-fields">
            <input v-model="registerForm.username" placeholder="用户名（3-50个字符）" class="form-input" />
            <input v-model="registerForm.password" type="password" placeholder="密码（6-100个字符）" class="form-input" />
            <input v-model="registerForm.email" placeholder="邮箱（选填）" class="form-input" />
            <button class="form-btn" @click="handleRegister" :disabled="loading">
              {{ loading ? '注册中...' : '注 册' }}
            </button>
          </div>
        </el-tab-pane>
      </el-tabs>
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
const activeTab = ref('login')
const loading = ref(false)
const loginForm = reactive({ username:'', password:'' })
const registerForm = reactive({ username:'', password:'', email:'' })

async function handleLogin() {
  if (!loginForm.username || !loginForm.password) { ElMessage.warning('请输入用户名和密码'); return }
  loading.value = true
  try { const res:any = await api.post('/auth/login', loginForm); authStore.setAuth(res.data); ElMessage.success('登录成功'); router.push('/knowledge') }
  catch {}
  finally { loading.value = false }
}
async function handleRegister() {
  if (!registerForm.username || !registerForm.password) { ElMessage.warning('请输入用户名和密码'); return }
  if (registerForm.username.length < 3) { ElMessage.warning('用户名至少3个字符'); return }
  if (registerForm.password.length < 6) { ElMessage.warning('密码至少6个字符'); return }
  loading.value = true
  try { const res:any = await api.post('/auth/register', registerForm); authStore.setAuth(res.data); ElMessage.success('注册成功'); router.push('/knowledge') }
  catch {}
  finally { loading.value = false }
}
</script>

<style lang="scss" scoped>
.login-page {
  display:flex;align-items:center;justify-content:center;height:100vh;
  background: var(--bg-gradient);
}
.login-card {
  width:400px;padding:40px;background: var(--color-card);border-radius:16px;box-shadow:0 20px 60px rgba(108,92,231,.08);
  .login-brand { text-align:center;margin-bottom:24px;
    h1 { font-size:26px;color:#1a1a2e;margin:12px 0 4px; }
    p { color:#999;font-size:13px; }
  }
}
.login-tabs :deep(.el-tabs__nav-wrap::after) { display:none; }
.form-fields { display:flex;flex-direction:column;gap:14px; }
.form-input {
  height:44px;padding:0 16px;font-size:14px;border:1.5px solid var(--color-border);border-radius:10px;outline:none;
  background: var(--color-input);color: var(--color-text);
  transition:border-color var(--transition);
  &::placeholder { color: var(--color-text-muted); }
  &:focus { border-color:var(--color-primary);box-shadow:0 0 0 3px rgba(108,92,231,.08); }
}
.form-btn {
  height:44px;border:none;border-radius:10px;background:var(--color-primary);color:#fff;font-size:15px;
  font-weight:600;cursor:pointer;transition:all var(--transition);
  &:hover:not(:disabled) { background:var(--color-primary-hover); }
  &:disabled { opacity:.5;cursor:default; }
}
</style>
