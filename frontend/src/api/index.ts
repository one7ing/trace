import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

const api = axios.create({
  baseURL: '/api',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  },
})

// 请求拦截器：自动添加 JWT Token
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('trace-token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// 响应拦截器：统一错误处理
api.interceptors.response.use(
  (response) => {
    const body = response.data
    // 后端统一返回 {code, message, data} 格式
    if (body && typeof body === 'object' && 'code' in body) {
      if (body.code !== 200) {
        ElMessage.error(body.message || '请求失败')
        return Promise.reject(new Error(body.message || '请求失败'))
      }
    }
    return body
  },
  (error) => {
    if (error.response) {
      const { status, data } = error.response
      const msg = data?.message || ''
      switch (status) {
        case 401:
          // 登录页面的 401 是用户名密码错误，不是过期
          if (router.currentRoute.value.path === '/login') {
            ElMessage.error(msg || '用户名或密码错误')
          } else {
            localStorage.removeItem('trace-token')
            localStorage.removeItem('trace-username')
            localStorage.removeItem('trace-userId')
            router.push('/login')
            ElMessage.error('登录已过期，请重新登录')
          }
          break
        case 403:
          ElMessage.error('没有权限')
          break
        case 500:
          ElMessage.error(msg || '服务器错误')
          break
        default:
          ElMessage.error(msg || '请求失败')
      }
    } else {
      ElMessage.error('网络异常')
    }
    return Promise.reject(error)
  }
)

export default api
