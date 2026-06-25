import { defineStore } from 'pinia'
import { ref } from 'vue'
import api from '@/api'

export const useAuthStore = defineStore('auth', () => {
  const accessToken = ref(localStorage.getItem('trace-accessToken') || '')
  const refreshToken = ref(localStorage.getItem('trace-refreshToken') || '')
  const username = ref(localStorage.getItem('trace-username') || '')
  const email = ref(localStorage.getItem('trace-email') || '')
  const avatarUrl = ref(localStorage.getItem('trace-avatarUrl') || '')
  const userId = ref(Number(localStorage.getItem('trace-userId')) || 0)

  /** 登录/注册成功后设置双Token和用户信息 */
  function setAuth(authData: {
    accessToken: string
    refreshToken: string
    username: string
    email?: string
    avatarUrl?: string
    userId: number
  }) {
    accessToken.value = authData.accessToken
    refreshToken.value = authData.refreshToken
    username.value = authData.username
    email.value = authData.email || ''
    avatarUrl.value = authData.avatarUrl || ''
    userId.value = authData.userId
    localStorage.setItem('trace-accessToken', authData.accessToken)
    localStorage.setItem('trace-refreshToken', authData.refreshToken)
    localStorage.setItem('trace-username', authData.username)
    localStorage.setItem('trace-email', authData.email || '')
    localStorage.setItem('trace-avatarUrl', authData.avatarUrl || '')
    localStorage.setItem('trace-userId', String(authData.userId))
  }

  /** 登出：调后端删除refreshToken，再清除本地缓存 */
  async function logout() {
    try {
      await api.post('/auth/logout')
    } catch {
      // 即使后端调用失败也清除本地缓存
    }
    accessToken.value = ''
    refreshToken.value = ''
    username.value = ''
    email.value = ''
    avatarUrl.value = ''
    userId.value = 0
    localStorage.removeItem('trace-accessToken')
    localStorage.removeItem('trace-refreshToken')
    localStorage.removeItem('trace-username')
    localStorage.removeItem('trace-email')
    localStorage.removeItem('trace-avatarUrl')
    localStorage.removeItem('trace-userId')
  }

  return { accessToken, refreshToken, username, email, avatarUrl, userId, setAuth, logout }
})
