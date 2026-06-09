<template>
  <div class="login-container">
    <div class="login-card">
      <h1>📊 Trace</h1>
      <p class="subtitle">个人成长轨迹记录与分析系统</p>

      <el-tabs v-model="activeTab" class="login-tabs">
        <el-tab-pane label="登录" name="login">
          <el-form @submit.prevent="handleLogin">
            <el-form-item>
              <el-input v-model="loginForm.username" placeholder="用户名" prefix-icon="User" />
            </el-form-item>
            <el-form-item>
              <el-input v-model="loginForm.password" type="password" placeholder="密码" prefix-icon="Lock" show-password
                @keyup.enter="handleLogin" />
            </el-form-item>
            <el-button type="primary" class="submit-btn" @click="handleLogin" :loading="loading">
              登 录
            </el-button>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="注册" name="register">
          <el-form @submit.prevent="handleRegister">
            <el-form-item>
              <el-input v-model="registerForm.username" placeholder="用户名（3-50个字符）" />
            </el-form-item>
            <el-form-item>
              <el-input v-model="registerForm.password" type="password" placeholder="密码（6-100个字符）" show-password />
            </el-form-item>
            <el-form-item>
              <el-input v-model="registerForm.email" placeholder="邮箱（选填）" />
            </el-form-item>
            <el-button type="primary" class="submit-btn" @click="handleRegister" :loading="loading">
              注 册
            </el-button>
          </el-form>
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

const loginForm = reactive({ username: '', password: '' })
const registerForm = reactive({ username: '', password: '', email: '' })

async function handleLogin() {
  if (!loginForm.username || !loginForm.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  loading.value = true
  try {
    const res: any = await api.post('/auth/login', loginForm)
    authStore.setAuth(res.data)
    ElMessage.success('登录成功')
    router.push('/knowledge')
  } catch { /* 错误已在拦截器中处理 */ }
  finally { loading.value = false }
}

async function handleRegister() {
  if (!registerForm.username || !registerForm.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  if (registerForm.username.length < 3) {
    ElMessage.warning('用户名至少3个字符')
    return
  }
  if (registerForm.password.length < 6) {
    ElMessage.warning('密码至少6个字符')
    return
  }
  loading.value = true
  try {
    const res: any = await api.post('/auth/register', registerForm)
    authStore.setAuth(res.data)
    ElMessage.success('注册成功')
    router.push('/knowledge')
  } catch { /* 错误已在拦截器中处理 */ }
  finally { loading.value = false }
}
</script>

<style lang="scss" scoped>
.login-container {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100vh;
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%);
}

.login-card {
  width: 400px;
  padding: 40px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);

  h1 {
    text-align: center;
    font-size: 28px;
    color: #1a1a2e;
    margin: 0;
  }

  .subtitle {
    text-align: center;
    color: #999;
    font-size: 13px;
    margin: 8px 0 24px;
  }

  .login-tabs {
    :deep(.el-tabs__nav-wrap::after) {
      display: none;
    }
  }

  .submit-btn {
    width: 100%;
    height: 42px;
    font-size: 15px;
    margin-top: 8px;
  }
}
</style>
