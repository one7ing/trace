import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('trace-token') || '')
  const username = ref(localStorage.getItem('trace-username') || '')
  const email = ref(localStorage.getItem('trace-email') || '')
  const userId = ref(Number(localStorage.getItem('trace-userId')) || 0)

  function setAuth(authData: { token: string; username: string; email?: string; userId: number }) {
    token.value = authData.token
    username.value = authData.username
    email.value = authData.email || ''
    userId.value = authData.userId
    localStorage.setItem('trace-token', authData.token)
    localStorage.setItem('trace-username', authData.username)
    localStorage.setItem('trace-email', authData.email || '')
    localStorage.setItem('trace-userId', String(authData.userId))
  }

  function logout() {
    token.value = ''
    username.value = ''
    email.value = ''
    userId.value = 0
    localStorage.removeItem('trace-token')
    localStorage.removeItem('trace-username')
    localStorage.removeItem('trace-email')
    localStorage.removeItem('trace-userId')
  }

  return { token, username, email, userId, setAuth, logout }
})
