<template>
  <div class="study-plan">
    <h2>
      <svg viewBox="0 0 24 24" fill="none" stroke="#7B61FF" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" width="20" height="20" style="vertical-align:-4px;margin-right:6px"><path d="M12 20h9"/><path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z"/></svg>
      我的计划
    </h2>
    <p class="desc">输入你的学习目标，AI 会自动拆解为可执行的阶段性计划。生成过程异步进行，完成后会通知你。</p>

    <!-- 目标输入 -->
    <el-card shadow="hover" class="input-card">
      <el-input
        v-model="goal"
        placeholder="例如：30天学完Java基础、3个月通过PMP认证..."
        :rows="2"
        type="textarea"
      />
      <div class="input-actions">
        <el-button type="primary" @click="generatePlan" :loading="generating" :disabled="!goal.trim()">
          生成计划
        </el-button>
      </div>
    </el-card>

    <!-- 生成中的计划 -->
    <el-card v-if="generatingPlan" shadow="hover" class="plan-card generating-card">
      <template #header>
        <div class="plan-card-header">
          <span>📋 {{ generatingPlan.goal }}</span>
          <el-tag type="warning" effect="dark">
            <el-icon class="is-loading"><Loading /></el-icon> 生成中...
          </el-tag>
        </div>
      </template>
      <div class="generating-placeholder">
        <el-icon class="is-loading" :size="32"><Loading /></el-icon>
        <p>AI 正在为你拆解目标、制定详细计划...</p>
        <p class="hint">正在通过 AI 拆解目标，请耐心等待...</p>
      </div>
    </el-card>

    <!-- 刚完成的计划 -->
    <el-card v-if="newPlan" shadow="hover" class="plan-card new-plan">
      <template #header>
        <div class="plan-card-header">
          <span>✨ {{ newPlan.goal }}</span>
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

    <!-- 历史计划列表 -->
    <h3 style="margin-top: 32px;" v-if="completedPlans.length > 0">历史计划</h3>
    <div v-loading="loading">
      <el-card
        v-for="plan in completedPlans"
        :key="plan.id"
        shadow="hover"
        class="history-card"
      >
        <div class="history-header">
          <span class="history-goal">{{ plan.goal }}</span>
          <span class="history-date">{{ formatDate(plan.createdAt) }}</span>
          <el-button text type="primary" size="small" @click="viewDetail(plan)">详情</el-button>
          <el-button text size="small" @click="openEdit(plan)">编辑</el-button>
          <el-button text type="primary" size="small" @click="downloadPdf(plan.id)">下载</el-button>
          <el-popconfirm title="确定删除该计划？" @confirm="handleDelete(plan.id)">
            <template #reference>
              <el-button text type="danger" size="small">删除</el-button>
            </template>
          </el-popconfirm>
        </div>
      </el-card>
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
import { ref, onMounted } from 'vue'
import api from '@/api'
import MarkdownRenderer from '@/components/MarkdownRenderer.vue'
import { ElMessage, ElNotification } from 'element-plus'
import { Loading } from '@element-plus/icons-vue'

const goal = ref('')
const generating = ref(false)
const loading = ref(false)
const generatingPlan = ref<any>(null)
const newPlan = ref<any>(null)
const completedPlans = ref<any[]>([])

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

async function generatePlan() {
  if (!goal.value.trim()) return
  generating.value = true
  try {
    const res: any = await api.post('/plan/generate', { goal: goal.value })
    const plan = res.data

    // 立即显示占位卡片，建立 SSE 连接
    generatingPlan.value = plan
    const planGoal = plan.goal || goal.value
    goal.value = ''
    ElMessage.info('计划生成任务已提交，正在后台处理...')
    startStreaming(plan.id, planGoal)
  } catch { /* handled */ }
  generating.value = false
}

function startStreaming(planId: number, goal: string) {
  const token = localStorage.getItem('trace-token') || ''
  const es = new EventSource(`/api/plan/${planId}/stream?token=${encodeURIComponent(token)}`)

  // 60 秒超时（匹配服务端 SseEmitter）
  const timeout = setTimeout(() => {
    es.close()
    generatingPlan.value = null
    ElMessage.warning('计划生成超时，正在后台处理中，请稍后刷新页面查看')
  }, 60000)

  es.addEventListener('completed', (e: MessageEvent) => {
    clearTimeout(timeout)
    es.close()
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
    generatingPlan.value = null
    ElMessage.warning('计划生成超时，请稍后刷新页面查看')
  }
}

async function fetchPlans() {
  loading.value = true
  try {
    const res: any = await api.get('/plan', { params: { page: 0, size: 20 } })
    const allPlans = res.data?.records || []
    completedPlans.value = allPlans.filter(
      (p: any) => p.planUrl && p.planUrl !== '' && p.planContent !== '正在生成中...'
    )
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

onMounted(fetchPlans)
</script>

<style lang="scss" scoped>
.study-plan {
  max-width: 800px;
  margin: 0 auto;

  h2 { font-size: 22px; margin-bottom: 4px; }

  .desc {
    color: #999;
    font-size: 13px;
    margin-bottom: 20px;
  }

  .input-card {
    margin-bottom: 20px;

    .input-actions {
      margin-top: 12px;
      text-align: right;
    }
  }

  .plan-card {
    margin-bottom: 20px;

    &.generating-card {
      border: 2px dashed #e6a23c;
      background: #fef8ee;
    }

    &.new-plan {
      border: 2px solid #67c23a;
      animation: fadeIn 0.5s ease-in;
    }

    .plan-card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      font-weight: 600;
    }
  }

  .generating-placeholder {
    text-align: center;
    padding: 40px 20px;

    p {
      color: #666;
      margin: 12px 0 0;
    }

    .hint {
      color: #999;
      font-size: 12px;
    }
  }

  .history-card {
    margin-bottom: 8px;

    .history-header {
      display: flex;
      align-items: center;
      gap: 8px;
      flex-wrap: wrap;

      .history-goal {
        flex: 1;
        font-weight: 600;
        color: #1a1a2e;
        min-width: 120px;
      }

      .history-date {
        font-size: 12px;
        color: #999;
        white-space: nowrap;
      }
    }
  }

  .detail-body {
    max-height: 65vh;
    overflow-y: auto;
    :deep(.markdown-body) {
      font-size: 14px;
      line-height: 1.8;
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
