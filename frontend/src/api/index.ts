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

// 裸axios实例，不走拦截器，专用于token刷新，防止死锁
const refreshApi = axios.create({ baseURL: '/api', timeout: 10000 })

// ── 无感刷新相关状态 ──
let isRefreshing = false
let pendingRequests: Array<{
  resolve: (token: string) => void
  reject: (err: any) => void
}> = []

/** 将等待刷新完成的请求加入队列 */
function addPendingRequest(resolve: (token: string) => void, reject: (err: any) => void) {
  pendingRequests.push({ resolve, reject })
}

/** 刷新成功后，用新token重放所有等待中的请求 */
function resolvePendingRequests(token: string) {
  pendingRequests.forEach(({ resolve }) => resolve(token))
  pendingRequests = []
}

/** 刷新失败后，拒绝所有等待中的请求 */
function rejectPendingRequests(err: any) {
  pendingRequests.forEach(({ reject }) => reject(err))
  pendingRequests = []
}

// ── 请求拦截器：自动添加 accessToken ──
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('trace-accessToken')
  // 认证接口不附加token，防止过期token被JwtAuthenticationFilter拦截
  if (token && !config.url?.startsWith('/auth/')) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// ── 响应拦截器：统一错误处理 + 无感刷新 ──
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
  async (error) => {
    if (error.response) {
      const { status, data } = error.response
      const code = data?.code
      const msg = data?.message || ''

      // ── 401 处理 ──
      if (status === 401) {
        // 40101 = accessToken 过期，尝试无感刷新
        if (code === 40101) {
          const refreshToken = localStorage.getItem('trace-refreshToken')
          if (!refreshToken) {
            // 没有refreshToken，直接跳登录
            clearAuthAndRedirect()
            return Promise.reject(error)
          }

          // 如果当前正在刷新中，将请求加入等待队列
          if (isRefreshing) {
            return new Promise((resolve, reject) => {
              addPendingRequest((newToken: string) => {
                error.config.headers.Authorization = `Bearer ${newToken}`
                resolve(api.request(error.config))
              }, reject)
            })
          }

          // 开始刷新流程
          isRefreshing = true
          try {
            const res: any = await refreshApi.post('/auth/refresh', { refreshToken })
            const newAccessToken = res.data.data.accessToken
            // 更新本地存储
            localStorage.setItem('trace-accessToken', newAccessToken)
            // 重放等待队列中的请求
            resolvePendingRequests(newAccessToken)
            // 重试当前请求
            error.config.headers.Authorization = `Bearer ${newAccessToken}`
            return api.request(error.config)
          } catch (refreshError) {
            // 刷新失败，清除认证信息跳转登录
            rejectPendingRequests(refreshError)
            clearAuthAndRedirect()
            ElMessage.error('登录已过期，请重新登录')
            return Promise.reject(refreshError)
          } finally {
            isRefreshing = false
          }
        }

        // 40102 = refreshToken 无效，直接跳登录
        if (code === 40102) {
          clearAuthAndRedirect()
          ElMessage.error('登录已过期，请重新登录')
          return Promise.reject(error)
        }

        // 登录页面的 401 是用户名密码错误
        if (router.currentRoute.value.path === '/login') {
          ElMessage.error(msg || '用户名或密码错误')
        } else {
          clearAuthAndRedirect()
          ElMessage.error('登录已过期，请重新登录')
        }
        return Promise.reject(error)
      }

      // ── 其他状态码处理 ──
      switch (status) {
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

/** 清除本地认证信息并跳转登录页 */
function clearAuthAndRedirect() {
  localStorage.removeItem('trace-accessToken')
  localStorage.removeItem('trace-refreshToken')
  localStorage.removeItem('trace-username')
  localStorage.removeItem('trace-email')
  localStorage.removeItem('trace-avatarUrl')
  localStorage.removeItem('trace-userId')
  if (router.currentRoute.value.path !== '/login') {
    router.push('/login')
  }
}

export default api
