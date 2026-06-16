<template>
  <div class="study-plan">
    <h2>
      <svg viewBox="0 0 24 24" fill="none" stroke="#7B61FF" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" width="20" height="20" style="vertical-align:-4px;margin-right:6px"><path d="M12 20h9"/><path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z"/></svg>
      我的计划
    </h2>
    <p class="desc">输入你的学习目标，AI 会自动拆解为可执行的阶段性计划。生成过程异步进行，完成后会通知你。</p>

    <!-- 目标输入 -->
    <el-card shadow="hover" class="input-card">
      <div class="mode-toggle">
        <span :class="{ active: mode === 'ai' }" @click="mode = 'ai'">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" width="14" height="14" style="vertical-align:-2px;margin-right:4px"><circle cx="12" cy="12" r="10"/><path d="M9.09 9a3 3 0 0 1 5.83 1c0 2-3 3-3 3"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg>
          AI 生成
        </span>
        <span :class="{ active: mode === 'manual' }" @click="mode = 'manual'">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" width="14" height="14" style="vertical-align:-2px;margin-right:4px"><path d="M12 20h9"/><path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z"/></svg>
          手动创建
        </span>
      </div>
      <el-input
        v-model="goal"
        :placeholder="mode === 'ai' ? '例如：30天学完Java基础、3个月通过PMP认证...' : '输入计划名称，例如：每日阅读1小时'"
        :rows="2"
        type="textarea"
      />
      <div class="input-row">
        <span class="duration-label">计划时长</span>
        <el-input-number v-model="planDuration" :min="1" :max="365" :controls="false" size="small" style="width:110px" />
        <span class="duration-unit">天</span>
      </div>
      <div v-if="mode === 'manual'" style="margin-top:12px">
        <el-input v-model="manualContent" type="textarea" :rows="6"
          placeholder="规划每日任务，例如：&#10;第1天：学习Spring基础&#10;第2天：练习MyBatis&#10;第3天：...&#10;（支持 Markdown 格式）" />
      </div>
      <div class="input-actions">
        <el-button
          v-if="mode === 'ai'"
          type="primary" @click="generatePlan" :loading="generating" :disabled="!goal.trim()">
          AI 生成计划
        </el-button>
        <el-button
          v-else
          type="success" @click="createPlan" :loading="generating" :disabled="!goal.trim() || !planDuration">
          创建计划
        </el-button>
      </div>
    </el-card>

    <!-- 生成中的计划 -->
    <el-card v-if="generatingPlan" shadow="hover" class="plan-card generating-card">
      <template #header>
        <div class="plan-card-header">
          <span>
            <svg viewBox="0 0 24 24" fill="none" stroke="#7B61FF" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" width="16" height="16" style="vertical-align:-3px;margin-right:5px"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/></svg>
            {{ generatingPlan.goal }}
          </span>
          <el-tag type="warning" effect="dark">
            <el-icon class="is-loading"><Loading /></el-icon> 生成中...
          </el-tag>
        </div>
      </template>
      <div class="generating-placeholder">
        <el-icon class="is-loading" :size="32"><Loading /></el-icon>
        <p>AI 正在为你拆解目标、制定详细计划，预计需要 1-3 分钟...</p>
        <p class="hint">生成过程通过 AI 异步完成，请勿关闭页面</p>
      </div>
    </el-card>

    <!-- 刚完成的计划 -->
    <el-card v-if="newPlan" shadow="hover" class="plan-card new-plan">
      <template #header>
        <div class="plan-card-header">
          <span>
            <svg viewBox="0 0 24 24" fill="none" stroke="#67c23a" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" width="16" height="16" style="vertical-align:-3px;margin-right:5px"><polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/></svg>
            {{ newPlan.goal }}
          </span>
          <div>
            <el-tag type="success" size="small" style="margin-right: 8px;">已完成</el-tag>
            <el-button text type="primary" size="small" @click="downloadPdf(newPlan.id)">
              下载 PDF
            </el-button>
          </div>
        </div>
      </template>
      <MarkdownRenderer :content="newPlan.planContent || ''" />
    </el-card>

    <!-- 打卡 & 进度 -->
    <el-card v-if="completedPlans.length > 0" shadow="hover" class="checkin-card">
      <div class="checkin-row">
        <div class="checkin-left">
          <el-select v-model="checkinPlanId" placeholder="选择计划" size="small" style="width:180px">
            <el-option v-for="p in completedPlans" :key="p.id" :label="p.goal" :value="p.id" />
          </el-select>
          <div class="face-wrapper" :class="{ happy: checkinHover, bigsmile: checkinDone }">
            <div class="face">
              <div class="eye eye-left"></div>
              <div class="eye eye-right"></div>
              <div class="mouth"></div>
            </div>
          </div>
          <el-button
            type="primary" size="small"
            @click="handleCheckIn" :loading="checkingIn" :disabled="!checkinPlanId"
            @mouseenter="checkinHover = true" @mouseleave="checkinHover = false"
            class="checkin-btn"
          >
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" width="13" height="13" style="vertical-align:-2px;margin-right:3px">
              <polyline points="20 6 9 17 4 12"/>
            </svg>
            打卡
          </el-button>
        </div>
        <div v-if="activeCheckinPlan" class="checkin-right">
          <div class="mini-progress">
            <span class="mini-progress-text">{{ checkinProgress.checked }}/{{ activeCheckinPlan.totalDuration || '?' }}天</span>
            <el-progress :percentage="checkinProgress.percent" :stroke-width="6" :show-text="false"
              :color="checkinProgress.percent >= 100 ? '#67c23a' : '#7B61FF'" style="width:120px" />
          </div>
          <div class="week-dots">
            <span v-for="(d, i) in weekStatus" :key="i" class="week-dot"
              :class="{ checked: d.checked, today: d.isToday }"
              :title="d.day"
            >{{ d.label }}</span>
          </div>
        </div>
      </div>
    </el-card>

    <!-- AI 创建计划 -->
    <div v-loading="loading">
    <template v-if="aiPlans.length > 0">
      <h3 class="section-title">
        <svg viewBox="0 0 24 24" fill="none" stroke="#7B61FF" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" width="16" height="16" style="vertical-align:-3px;margin-right:5px"><circle cx="12" cy="12" r="10"/><path d="M9.09 9a3 3 0 0 1 5.83 1c0 2-3 3-3 3"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg>
        AI 创建计划
      </h3>
      <el-card v-for="plan in aiPlans" :key="plan.id" shadow="hover" class="history-card">
        <div class="history-header">
          <span class="history-goal">{{ plan.goal }}</span>
          <span class="history-date">{{ formatDate(plan.createdAt) }}</span>
          <el-button text type="primary" size="small" @click="viewDetail(plan)">详情</el-button>
          <el-button text size="small" @click="openEdit(plan)">编辑</el-button>
          <el-button text type="primary" size="small" @click="downloadPdf(plan.id)">下载</el-button>
          <el-popconfirm title="确定删除该计划？" @confirm="handleDelete(plan.id)">
            <template #reference><el-button text type="danger" size="small">删除</el-button></template>
          </el-popconfirm>
        </div>
      </el-card>
    </template>

    <!-- 用户创建计划 -->
    <template v-if="manualPlans.length > 0">
      <h3 class="section-title" style="margin-top:24px">
        <svg viewBox="0 0 24 24" fill="none" stroke="#34a853" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" width="16" height="16" style="vertical-align:-3px;margin-right:5px"><path d="M12 20h9"/><path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z"/></svg>
        用户创建计划
      </h3>
      <el-card v-for="plan in manualPlans" :key="plan.id" shadow="hover" class="history-card">
        <div class="history-header">
          <span class="history-goal">{{ plan.goal }}</span>
          <span class="history-date">{{ formatDate(plan.createdAt) }}</span>
          <el-button text type="primary" size="small" @click="viewDetail(plan)">详情</el-button>
          <el-button text size="small" @click="openEdit(plan)">编辑</el-button>
          <el-button text type="primary" size="small" @click="downloadPdf(plan.id)">下载</el-button>
          <el-popconfirm title="确定删除该计划？" @confirm="handleDelete(plan.id)">
            <template #reference><el-button text type="danger" size="small">删除</el-button></template>
          </el-popconfirm>
        </div>
      </el-card>
    </template>
    </div>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" title="计划详情" width="720px" top="5vh" destroy-on-close>
      <div class="detail-body">
        <MarkdownRenderer :content="detailContent" />
      </div>
    </el-dialog>

    <!-- 编辑弹窗 -->
    <el-dialog v-model="editVisible" title="编辑计划" width="720px" top="5vh" destroy-on-close>
      <el-input v-model="editContent" type="textarea" :rows="16" placeholder="编辑计划内容..." />
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" @click="saveEdit" :loading="saving">保存并重新生成 PDF</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import api from '@/api'
import MarkdownRenderer from '@/components/MarkdownRenderer.vue'
import { ElMessage, ElNotification } from 'element-plus'
import { Loading } from '@element-plus/icons-vue'

const goal = ref('')
const planDuration = ref<number>()
const manualContent = ref('')
const mode = ref<'ai' | 'manual'>('ai')
const generating = ref(false)
const loading = ref(false)
const generatingPlan = ref<any>(null)
const newPlan = ref<any>(null)
const completedPlans = ref<any[]>([])

// 打卡
const checkinPlanId = ref<number | null>(null)
const checkingIn = ref(false)
const checkinProgress = ref({ checked: 0, percent: 0 })
const weekStatus = ref<any[]>([])
const checkinHover = ref(false)
const checkinDone = ref(false)

// SSE 连接追踪 —— 用于组件卸载时清理 + 返回页面时重连
const activeEventSource = ref<EventSource | null>(null)
const sseTimeoutHandle = ref<ReturnType<typeof setTimeout> | null>(null)

// 详情弹窗
const detailVisible = ref(false)
const detailContent = ref('')

// 编辑弹窗
const editVisible = ref(false)
const editContent = ref('')
const editPlanId = ref<number>(0)
const saving = ref(false)

function formatDate(dateStr: string) {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString('zh-CN')
}

const activeCheckinPlan = computed(() =>
  completedPlans.value.find(p => p.id === checkinPlanId.value) || completedPlans.value[0]
)

const aiPlans = computed(() => completedPlans.value.filter(p => p.source !== 'manual'))
const manualPlans = computed(() => completedPlans.value.filter(p => p.source === 'manual'))

watch(activeCheckinPlan, (p) => {
  if (p) fetchCheckinProgress(p.id)
}, { immediate: false })

watch(checkinPlanId, (id) => {
  fetchWeekStatus(id)
})

watch(completedPlans, (plans) => {
  if (plans.length > 0 && !checkinPlanId.value) {
    checkinPlanId.value = plans[0].id
  }
})

async function generatePlan() {
  if (!goal.value.trim()) return
  generating.value = true
  try {
    const body: any = { goal: goal.value }
    if (planDuration.value && planDuration.value > 0) body.totalDuration = planDuration.value
    const res: any = await api.post('/plan/generate', body)
    const plan = res.data
    generatingPlan.value = plan
    goal.value = ''
    planDuration.value = undefined
    ElMessage.info('计划生成任务已提交，预计需要 1-3 分钟...')
    startStreaming(plan.id, plan.goal)
  } catch {}
  generating.value = false
}

async function createPlan() {
  if (!goal.value.trim() || !planDuration.value) return
  generating.value = true
  try {
    const res: any = await api.post('/plan/create', {
      goal: goal.value, totalDuration: planDuration.value,
      planContent: manualContent.value || ''
    })
    ElMessage.success('计划已创建，PDF 已自动生成')
    goal.value = ''
    planDuration.value = undefined
    manualContent.value = ''
    fetchPlans()
  } catch { ElMessage.error('创建失败') }
  generating.value = false
}

function startStreaming(planId: number, goal: string) {
  // 关闭已有连接（避免重复连接）
  closeSse()

  const token = localStorage.getItem('trace-token') || ''
  const es = new EventSource(`/api/plan/${planId}/stream?token=${encodeURIComponent(token)}`)
  activeEventSource.value = es

  // 5 分钟超时（匹配服务端 SseEmitter）
  const timeout = setTimeout(() => {
    es.close()
    activeEventSource.value = null
    sseTimeoutHandle.value = null
    generatingPlan.value = null
    ElMessage.warning('计划生成超时，可能仍在后台处理中，请稍后刷新页面查看')
  }, 300000)
  sseTimeoutHandle.value = timeout

  es.addEventListener('completed', (e: MessageEvent) => {
    clearTimeout(timeout)
    es.close()
    activeEventSource.value = null
    sseTimeoutHandle.value = null
    generatingPlan.value = null
    const d = JSON.parse(e.data)

    if (d.planContent && d.planContent.startsWith('生成失败')) {
      ElMessage.error('计划生成失败，请重试')
      return
    }

    newPlan.value = {
      id: d.planId,
      goal: d.goal || goal,
      planContent: d.planContent || '',
      planUrl: d.planUrl || '',
    }
    ElNotification({
      title: '计划生成完毕',
      message: '你的学习计划已生成，可以查看和下载 PDF 了。',
      type: 'success',
      duration: 5000,
    })
  })

  es.onerror = () => {
    clearTimeout(timeout)
    es.close()
    activeEventSource.value = null
    sseTimeoutHandle.value = null
    generatingPlan.value = null
    ElMessage.warning('计划生成连接断开，可能仍在后台处理中，请稍后刷新页面查看')
  }
}

/** 关闭当前 SSE 连接并清理 */
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

async function fetchPlans() {
  loading.value = true
  try {
    const res: any = await api.get('/plan', { params: { page: 0, size: 20 } })
    const allPlans: any[] = res.data?.records || []

    // 已完成的计划：内容不是占位文本（含 AI 生成和手动创建）
    completedPlans.value = allPlans.filter(
      (p: any) => p.planContent !== '正在生成中...'
    )

    // 进行中的计划：内容仍是占位文本
    const inProgress = allPlans.filter(
      (p: any) => p.planContent === '正在生成中...'
    )

    // 如果没有活跃的 SSE 连接且有进行中的计划，重新建立 SSE
    if (inProgress.length > 0 && !activeEventSource.value) {
      const plan = inProgress[0] // 最新提交的计划排在最前面
      generatingPlan.value = plan
      startStreaming(plan.id, plan.goal)
    }
  } catch { /* handled */ }
  loading.value = false
}

async function downloadPdf(id: number) {
  try {
    const res: any = await api.get(`/plan/${id}/download`)
    const url = res.data || res
    if (url && typeof url === 'string') {
      window.open(url, '_blank')
    } else {
      ElMessage.error('下载链接无效')
    }
  } catch {
    ElMessage.error('下载失败，请稍后重试')
  }
}

function viewDetail(plan: any) {
  detailContent.value = plan.planContent || ''
  detailVisible.value = true
}

function openEdit(plan: any) {
  editPlanId.value = plan.id
  editContent.value = plan.planContent || ''
  editVisible.value = true
}

async function saveEdit() {
  if (!editContent.value.trim()) return
  saving.value = true
  try {
    await api.put(`/plan/${editPlanId.value}`, { planContent: editContent.value })
    await api.post(`/plan/${editPlanId.value}/regenerate-pdf`)
    ElMessage.success('计划已更新，PDF 已重新生成')
    editVisible.value = false
    fetchPlans()
  } catch {
    ElMessage.error('保存失败')
  }
  saving.value = false
}

async function handleDelete(id: number) {
  try {
    await api.delete(`/plan/${id}`)
    ElMessage.success('已删除')
    fetchPlans()
  } catch {
    ElMessage.error('删除失败')
  }
}

async function handleCheckIn() {
  if (!checkinPlanId.value) return
  checkingIn.value = true
  try {
    await api.post('/checkin', { planId: checkinPlanId.value })
    ElMessage.success('打卡成功')
    checkinDone.value = true
    fetchCheckinProgress(checkinPlanId.value)
    fetchWeekStatus(checkinPlanId.value)
  } catch (e: any) {
    ElMessage.warning(e?.response?.data?.message || '打卡失败')
  }
  checkingIn.value = false
}

async function fetchCheckinProgress(planId: number) {
  try {
    const res: any = await api.get('/checkin/progress', { params: { planId } })
    const p = activeCheckinPlan.value
    const total = p?.totalDuration || 0
    const checked = res.data?.totalChecked || 0
    checkinProgress.value = {
      checked,
      percent: total > 0 ? Math.min(100, Math.round((checked / total) * 100)) : 0,
    }
  } catch { /* ignore */ }
}

async function fetchWeekStatus(planId?: number | null) {
  try {
    const params: any = {}
    if (planId) params.planId = planId
    const res: any = await api.get('/checkin/week', { params })
    const days = res.data?.days || []
    const today = new Date().getDay()
    weekStatus.value = days.map((d: any, i: number) => ({
      ...d,
      label: ['一','二','三','四','五','六','日'][i],
      isToday: (i + 1) % 7 === today || (i === 6 && today === 0),
    }))
    // 更新当前选中计划的笑脸状态
    const todayDot = weekStatus.value.find((d: any) => d.isToday)
    checkinDone.value = !!todayDot?.checked
  } catch {}
}

onMounted(() => {
  generatingPlan.value = null
  newPlan.value = null
  fetchPlans().then(() => {
    if (activeCheckinPlan.value) {
      fetchCheckinProgress(activeCheckinPlan.value.id)
    }
    fetchWeekStatus(activeCheckinPlan.value?.id)
  })
})

onUnmounted(() => {
  closeSse()
})
</script>

<style lang="scss" scoped>
.study-plan {
  max-width: 800px;
  margin: 0 auto;

  h2 { font-size: 22px; margin-bottom: 4px; color: var(--color-text); }

  .section-title {
    font-size: 15px; font-weight: 600; color: var(--color-text); margin: 24px 0 10px;
  }

  .desc {
    color: var(--color-text-muted);
    font-size: 13px;
    margin-bottom: 20px;
  }

  .input-card {
    margin-bottom: 20px;

    .mode-toggle {
      display: flex; gap: 0; margin-bottom: 12px; border-radius: 6px;
      overflow: hidden; border: 1px solid var(--color-border); width: fit-content;
      span {
        padding: 5px 14px; font-size: 12px; cursor: pointer; color: var(--color-text-muted);
        background: var(--color-bubble-ai); transition: all .15s;
        &:first-child { border-right: 1px solid var(--color-border); }
        &.active { background: var(--color-primary); color: #fff; }
      }
    }

    .input-row {
      display: flex; align-items: center; gap: 8px; margin-top: 12px;
      .duration-label { font-size: 13px; color: var(--color-text-secondary); }
      .duration-unit { font-size: 12px; color: var(--color-text-muted); }
    }

    .input-actions { margin-top: 12px; text-align: right; }
  }

  .plan-card {
    margin-bottom: 20px;

    &.generating-card {
      border: 2px dashed #e6a23c;
      background: var(--color-weak-bg);
    }

    &.new-plan {
      border: 2px solid #67c23a;
      animation: fadeIn 0.5s ease-in;
    }

    .plan-card-header {
      display: flex; justify-content: space-between; align-items: center; font-weight: 600;
    }
  }

  .generating-placeholder {
    text-align: center; padding: 40px 20px;
    p { color: var(--color-text-secondary); margin: 12px 0 0; }
    .hint { color: var(--color-text-muted); font-size: 12px; }
  }

  .history-card {
    margin-bottom: 8px;
    .history-header {
      display: flex; align-items: center; gap: 8px; flex-wrap: wrap;
      .history-goal { flex: 1; font-weight: 600; color: var(--color-text); min-width: 120px; }
      .history-date { font-size: 12px; color: var(--color-text-muted); white-space: nowrap; }
    }
  }

  .checkin-card {
    margin-bottom: 20px;
    :deep(.el-card__body) { padding: 14px 20px; }
    .checkin-row { display: flex; align-items: center; justify-content: space-between; gap: 16px; flex-wrap: wrap; }
    .checkin-left { display: flex; align-items: center; gap: 10px; }
    .checkin-right { display: flex; align-items: center; gap: 14px; }
    .mini-progress { display: flex; align-items: center; gap: 8px;
      .mini-progress-text { font-size: 12px; color: var(--color-text-secondary); white-space: nowrap; }
    }

    .face-wrapper {
      width: 32px; height: 32px; position: relative;
      .face {
        width: 32px; height: 32px; border-radius: 50%;
        background: var(--color-weak-bg); border: 1.5px solid var(--color-weak-border);
        position: relative; transition: background .4s, border-color .4s;
      }
      .eye {
        position: absolute; top: 9px; width: 4px; height: 4px;
        border-radius: 50%; background: var(--color-weak-text);
        transition: transform .3s, height .3s;
        &.eye-left { left: 8px; } &.eye-right { right: 8px; }
      }
      .mouth {
        position: absolute; bottom: 7px; left: 50%; transform: translateX(-50%);
        width: 14px; height: 6px; border-radius: 0 0 14px 14px;
        border-bottom: 2px solid var(--color-weak-text); transition: all .4s;
      }
      .mouth { border-radius: 14px 14px 0 0; border-bottom: none; border-top: 2px solid var(--color-weak-text); }
      .eye { transform: translateY(0); }
      &.happy {
        .face { background: var(--color-score-bg); border-color: #f7dc6f; }
        .mouth { border-radius: 0 0 14px 14px; border-top: none; border-bottom: 2px solid var(--color-weak-text); height: 8px; width: 16px; }
        .eye { transform: translateY(-1px); }
      }
      &.bigsmile {
        .face { background: #eafaf1; border-color: #82e0aa; }
        .mouth { border-radius: 0 0 16px 16px; border-top: none; border-bottom: 2px solid #27ae60; height: 10px; width: 18px; }
        .eye { transform: scaleY(0.4) translateY(-2px); }
      }
    }

    .checkin-btn { transition: all .2s; }
    .week-dots { display: flex; gap: 4px; }
    .week-dot {
      width: 22px; height: 22px; border-radius: 50%;
      display: flex; align-items: center; justify-content: center;
      font-size: 10px; color: var(--color-text-muted); background: var(--color-bubble-ai);
      transition: all .15s;
      &.checked { background: var(--color-primary); color: #fff; }
      &.today { box-shadow: 0 0 0 2px var(--color-primary); background: var(--color-primary-bg); color: var(--color-primary);
        &.checked { background: var(--color-primary); color: #fff; } }
    }
  }

  .detail-body {
    max-height: 65vh; overflow-y: auto;
    :deep(.markdown-body) {
      font-size: 14px; line-height: 1.8;
      h1 { font-size: 20px; margin: 16px 0 10px; }
      h2 { font-size: 17px; margin: 14px 0 8px; }
      h3 { font-size: 15px; margin: 12px 0 6px; }
      ul, ol { padding-left: 20px; }
      p { margin: 6px 0; }
    }
  }
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(-10px); }
  to { opacity: 1; transform: translateY(0); }
}
</style>
