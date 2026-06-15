<template>
  <div class="interview-detail" v-loading="loading">
    <el-page-header @back="$router.push('/interview/history')">
      <template #content>
        <span>面试详情 - {{ record.industry }}</span>
      </template>
    </el-page-header>

    <el-card style="margin-top: 16px;" v-if="record.id">
      <div class="summary-bar">
        <el-tag type="primary">{{ record.industry }}</el-tag>
        <span>题目数：{{ record.totalQuestions }}</span>
        <span>{{ formatDateTime(record.completedAt) }}</span>
        <el-button type="primary" size="small" @click="downloadReport" :loading="downloading">
          下载 PDF 报告
        </el-button>
      </div>
    </el-card>

    <!-- AI 综合评价 -->
    <el-card class="analysis-card">
      <template #header>
        <span class="analysis-title">AI 面试评价</span>
      </template>
      <div v-if="record.aiAnalysis || evaluating" class="analysis-body">
        <MarkdownRenderer v-if="record.aiAnalysis" :content="record.aiAnalysis" />
        <div v-else class="evaluating-hint">
          <span class="evaluating-spinner"></span>
          AI 正在分析你的面试表现，请稍候...
        </div>
      </div>
      <div v-else class="evaluating-hint">
        AI 评价尚未生成，请稍后刷新页面查看。
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import api from '@/api'
import MarkdownRenderer from '@/components/MarkdownRenderer.vue'

const route = useRoute()
const id = Number(route.params.id)

const loading = ref(false)
const downloading = ref(false)
const record = reactive<any>({})
const evaluating = ref(false)
let pollTimer: ReturnType<typeof setInterval> | null = null

function formatDateTime(dateStr: string) {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  return d.toLocaleDateString('zh-CN') + ' ' + d.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

async function fetchDetail() {
  loading.value = true
  try {
    const res: any = await api.get(`/interview/records/${id}/details`)
    Object.assign(record, res.data || {})
    if (record.aiAnalysis) {
      evaluating.value = false
    } else {
      checkAndPoll()
    }
  } catch { /* handled */ }
  loading.value = false
}

function checkAndPoll() {
  if (pollTimer) { clearInterval(pollTimer); pollTimer = null }
  evaluating.value = true
  let attempts = 0
  pollTimer = setInterval(async () => {
    attempts++
    try {
      const res: any = await api.get(`/interview/records/${id}/details`)
      const fresh = res.data || {}
      Object.assign(record, fresh)
      if (record.aiAnalysis) {
        evaluating.value = false
        if (pollTimer) { clearInterval(pollTimer); pollTimer = null }
        return
      }
    } catch {}
    if (attempts >= 10 && pollTimer) {
      clearInterval(pollTimer); pollTimer = null
      evaluating.value = false
    }
  }, 3000)
}

async function downloadReport() {
  downloading.value = true
  try {
    const res: any = await api.get(`/interview/report/${id}/download`)
    window.open(res.data, '_blank')
  } catch { /* handled */ }
  downloading.value = false
}

onMounted(fetchDetail)
onUnmounted(() => { if (pollTimer) { clearInterval(pollTimer); pollTimer = null } })
</script>

<style lang="scss" scoped>
.interview-detail {
  max-width: 800px;
  margin: 0 auto;

  .summary-bar {
    display: flex;
    align-items: center;
    gap: 16px;
    flex-wrap: wrap;
  }

  .analysis-card {
    margin-top: 16px;
    .analysis-title {
      font-size: 16px;
      font-weight: 600;
    }
    .analysis-body {
      min-height: 60px;
    }
    :deep(.markdown-body) {
      font-size: 14px;
      line-height: 1.8;
      color: #333;
      h1 { font-size: 20px; margin: 24px 0 12px; padding-bottom: 8px; border-bottom: 2px solid #e8e8e8; color: #1a1a1a; }
      h2 { font-size: 17px; margin: 22px 0 10px; padding-bottom: 6px; border-bottom: 1px solid #eee; color: #1a1a1a; }
      h3 { font-size: 15px; margin: 16px 0 8px; color: #333; }
      strong { font-weight: 650; color: #1a1a1a; }
      ul, ol { padding-left: 22px; margin: 8px 0; }
      li { margin: 4px 0; line-height: 1.7; }
      p { margin: 8px 0; }
      code { background: #f3f3f8; padding: 2px 6px; border-radius: 3px; font-size: 12.5px; color: #d14; }
      blockquote { border-left: 3px solid #7B61FF; padding: 8px 14px; margin: 12px 0; background: rgba(123,97,255,.04); color: #666; border-radius: 0 6px 6px 0; }
      hr { border: none; border-top: 1px solid #eee; margin: 16px 0; }
    }
  }

  .evaluating-hint {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 20px 0;
    color: #909399;
    font-size: 13px;
  }
  .evaluating-spinner {
    width: 16px; height: 16px;
    border: 2px solid #e0e0e0;
    border-top-color: #7B61FF;
    border-radius: 50%;
    animation: eval-spin 0.8s linear infinite;
  }
  @keyframes eval-spin { to { transform: rotate(360deg); } }
}
</style>
