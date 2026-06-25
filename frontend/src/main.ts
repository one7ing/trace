import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import './styles/main.scss'

// ── 清除旧版本缓存，强制重新登录 ──
const CACHE_VERSION = '3'
const storedVersion = localStorage.getItem('trace-cache-ver')
if (storedVersion !== CACHE_VERSION) {
  // 清除所有 trace-* 开头的缓存
  const keysToRemove: string[] = []
  for (let i = 0; i < localStorage.length; i++) {
    const key = localStorage.key(i)
    if (key && key.startsWith('trace-')) keysToRemove.push(key)
  }
  keysToRemove.forEach(k => localStorage.removeItem(k))
  localStorage.setItem('trace-cache-ver', CACHE_VERSION)
}

const app = createApp(App)
app.use(ElementPlus)
app.use(createPinia())
app.use(router)
app.mount('#app')
