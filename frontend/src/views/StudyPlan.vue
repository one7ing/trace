<template>
  <div class="study-plan">
    <h2>🎯 我的计划</h2>
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
        <p class="hint">这通常需要 15-30 秒，完成后会自动通知你</p>
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
          <el-button text type="primary" size="small" @click="downloadPdf(plan.id)">
            下载 PDF
          </el-button>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
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
let pollTimer: ReturnType<typeof setInterval> | null = null

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

    // 立即显示占位卡片
    generatingPlan.value = plan
    goal.value = ''
    ElMessage.info('计划生成任务已提交，正在后台处理...')

    // 开始轮询状态
    startPolling(plan.id)
  } catch { /* handled */ }
  generating.value = false
}

function startPolling(planId: number) {
  // 清除之前的轮询
  if (pollTimer) clearInterval(pollTimer)

  pollTimer = setInterval(async () => {
    try {
      const res: any = await api.get(`/plan/${planId}/status`)
      if (res.data?.completed) {
        // 生成完成！
        clearInterval(pollTimer!)
        pollTimer = null

        // 移除生成中卡片，显示完成卡片
        generatingPlan.value = null
        newPlan.value = {
          id: planId,
          goal: res.data.planContent ? generatingPlan.value?.goal : '',
          planContent: res.data.planContent,
          planUrl: res.data.planUrl,
        }

        // 弹窗通知
        ElNotification({
          title: '✅ 计划生成完毕',
          message: '你的学习计划已生成，可以查看和下载 PDF 了。',
          type: 'success',
          duration: 5000,
        })

        // 同时更新完成卡片的目标（从已完成的列表获取）
        const planRes: any = await api.get(`/plan/${planId}/status`)
        if (newPlan.value) {
          // goal 在 controller status 里没有，我们读一次完整 plan
        }
        fetchPlans()
      }
    } catch {
      // 轮询失败，继续重试
    }
  }, 3000) // 每 3 秒轮询一次
}

async function fetchPlans() {
  loading.value = true
  try {
    const res: any = await api.get('/plan', { params: { page: 0, size: 20 } })
    const allPlans = res.data?.content || []
    // 已经完成的计划（有 planUrl 且不是"正在生成中..."）
    completedPlans.value = allPlans.filter(
      (p: any) => p.planUrl && p.planContent !== '正在生成中...'
    )
  } catch { /* handled */ }
  loading.value = false
}

async function downloadPdf(id: number) {
  try {
    const res: any = await api.get(`/plan/${id}/download`)
    if (res.data) {
      window.open(res.data, '_blank')
    }
  } catch { /* handled */ }
}

onMounted(fetchPlans)

onUnmounted(() => {
  if (pollTimer) clearInterval(pollTimer)
})
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
      gap: 16px;

      .history-goal {
        flex: 1;
        font-weight: 600;
        color: #1a1a2e;
      }

      .history-date {
        font-size: 12px;
        color: #999;
      }
    }
  }
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(-10px); }
  to { opacity: 1; transform: translateY(0); }
}
</style>
