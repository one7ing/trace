import { defineStore } from 'pinia'
import { ref } from 'vue'
import api from '@/api'

/**
 * 计划生成通知 Store —— 管理 plan SSE EventSource，生命周期绑定 App.vue。
 * 
 * 职责：
 *   - App.vue onMounted 调用 init()，检查是否有"生成中"计划，自动建 SSE
 *   - StudyPlan.vue 提交生成后调用 startStreaming(planId, goal)
 *   - plan_completed 时更新状态，App.vue watch newPlan 弹 ElNotification
 *   - 路由切换时 SSE 不中断（因为 Store 不被销毁）
 */
export const usePlanStore = defineStore('plan', () => {
  // ── 状态 ──
  const generating = ref(false)
  const generatingPlan = ref<any>(null)   // { id, goal }
  const newPlan = ref<any>(null)           // 刚完成的计划，App.vue watch 后弹通知
  const activeEventSource = ref<EventSource | null>(null)
  const sseTimeoutHandle = ref<ReturnType<typeof setTimeout> | null>(null)

  // ── 方法 ──

  /** App.vue onMounted 调用：检查是否有进行中的计划，自动建立 SSE */
  async function init() {
    try {
      const res: any = await api.get('/plan', { params: { page: 0, size: 20 } })
      const plans: any[] = res.data?.records || []
      const inProgress = plans.find((p: any) => p.planContent === '正在生成中...')
      if (inProgress) {
        generatingPlan.value = { id: inProgress.id, goal: inProgress.goal }
        generating.value = true
        startStreaming(inProgress.id, inProgress.goal)
      }
    } catch { /* ignore */ }
  }

  /** 建立 plan SSE 连接（由 StudyPlan.vue 或 init() 调用） */
  function startStreaming(planId: number, goal: string) {
    closeSse()  // 关闭旧连接

    const token = localStorage.getItem('trace-accessToken') || ''
    const es = new EventSource(`/api/plan/${planId}/stream?token=${encodeURIComponent(token)}`)
    activeEventSource.value = es

    // 5 分钟超时
    const timeout = setTimeout(() => {
      es.close()
      activeEventSource.value = null
      sseTimeoutHandle.value = null
      generatingPlan.value = null
      generating.value = false
    }, 300_000)
    sseTimeoutHandle.value = timeout

    es.addEventListener('completed', (e: MessageEvent) => {
      clearTimeout(timeout)
      es.close()
      activeEventSource.value = null
      sseTimeoutHandle.value = null
      generatingPlan.value = null
      generating.value = false
      const d = JSON.parse(e.data)

      if (d.planContent && d.planContent.startsWith('生成失败')) {
        // 失败：设置 newPlan 触发 App.vue 错误通知
        newPlan.value = {
          id: d.planId,
          goal: d.goal || goal,
          planContent: '生成失败',
        }
        return
      }

      // 成功：设置 newPlan 触发 App.vue 通知
      newPlan.value = {
        id: d.planId,
        goal: d.goal || goal,
        planContent: d.planContent || '',
        planUrl: d.planUrl || '',
      }
    })

    es.onerror = () => {
      clearTimeout(timeout)
      es.close()
      activeEventSource.value = null
      sseTimeoutHandle.value = null
      generatingPlan.value = null
      generating.value = false
    }
  }

  /** 关闭 SSE 连接 */
  function closeSse() {
    if (activeEventSource.value) {
      activeEventSource.value.close()
      activeEventSource.value = null
    }
    if (sseTimeoutHandle.value) {
      clearTimeout(sseTimeoutHandle.value)
      sseTimeoutHandle.value = null
    }
  }

  /** StudyPlan.vue 调用：提交生成任务后设置状态 */
  function setGenerating(plan: { id: number; goal: string }) {
    generatingPlan.value = plan
    generating.value = true
    startStreaming(plan.id, plan.goal)
  }

  /** 清除 newPlan（App.vue 通知弹出后调用） */
  function clearNewPlan() {
    newPlan.value = null
  }

  return {
    generating, generatingPlan, newPlan, activeEventSource,
    init, startStreaming, closeSse, setGenerating, clearNewPlan
  }
})
