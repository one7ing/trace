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
      <div v-if="aiAnalysis || evaluating" class="analysis-body">
        <MarkdownRenderer v-if="aiAnalysis" :content="aiAnalysis" :key="aiAnalysis.length" />
        <div v-else-if="evaluating" class="evaluating-hint">
          <span class="evaluating-spinner"></span>
          AI 正在分析你的面试表现，请稍候...
        </div>
      </div>
      <div v-else-if="notGenerated" class="evaluating-hint">
        AI 评价生成超时，请稍后刷新页面查看。
      </div>
      <div v-else class="evaluating-hint">
        AI 评价尚未生成，请稍后刷新页面查看。
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import api from '@/api'
import MarkdownRenderer from '@/components/MarkdownRenderer.vue'
import { ElMessage } from 'element-plus'

const route = useRoute()
const id = Number(route.params.id)

const loading = ref(false)
const downloading = ref(false)
const record = ref<any>({})
const aiAnalysis = ref('')       // ★ 单独 ref，确保响应式
const evaluating = ref(false)     // true=等待中, false=已完成或未开始
const notGenerated = ref(false)   // true=超时/未生成
let eventSource: EventSource | null = null
let sseTimeout: ReturnType<typeof setTimeout> | null = null

function formatDateTime(dateStr: string) {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  return d.toLocaleDateString('zh-CN') + ' ' + d.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

async function fetchDetail() {
  loading.value = true
  try {
    const res: any = await api.get(`/interview/records/${id}/details`)
    record.value = { ...res.data || {} }
    if (res.data?.aiAnalysis) {
      aiAnalysis.value = res.data.aiAnalysis
    } else {
      startSse()
    }
  } catch { /* handled */ }
  loading.value = false
}

function startSse() {
  closeSse()
  evaluating.value = true
  notGenerated.value = false

  const token = localStorage.getItem('trace-token') || ''
  const es = new EventSource(`/api/interview/records/${id}/stream?token=${encodeURIComponent(token)}`)
  eventSource = es

  sseTimeout = setTimeout(() => {
    closeSse()
    evaluating.value = false
    if (!aiAnalysis.value) {
      notGenerated.value = true
      fallbackPoll()
    }
  }, 300000)

  es.addEventListener('completed', (e: MessageEvent) => {
    clearTimeout(sseTimeout!)
    sseTimeout = null
    es.close()
    eventSource = null
    try {
      const d = JSON.parse(e.data)
      if (d.aiAnalysis) {
        aiAnalysis.value = d.aiAnalysis
        evaluating.value = false
      }
    } catch {
      evaluating.value = false
      notGenerated.value = true
    }
  })

  es.onerror = () => {
    clearTimeout(sseTimeout!)
    sseTimeout = null
    es.close()
    eventSource = null
    if (!aiAnalysis.value) {
      evaluating.value = false
      fallbackPoll()
    }
  }
}

function closeSse() {
  if (eventSource) {
    eventSource.close()
    eventSource = null
  }
  if (sseTimeout) {
    clearTimeout(sseTimeout)
    sseTimeout = null
  }
}

function fallbackPoll() {
  let attempts = 0
  notGenerated.value = false
  evaluating.value = true
  const timer = setInterval(async () => {
    attempts++
    try {
      const res: any = await api.get(`/interview/records/${id}/details`)
      if (res.data?.aiAnalysis) {
        aiAnalysis.value = res.data.aiAnalysis
        evaluating.value = false
        clearInterval(timer)
        return
      }
    } catch {}
    if (attempts >= 20) {
      clearInterval(timer)
      evaluating.value = false
      notGenerated.value = true
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
onUnmounted(closeSse)
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
      font-size: 14px; line-height: 1.8; color: var(--color-text);
      h1 { font-size: 20px; margin: 24px 0 12px; padding-bottom: 8px; border-bottom: 2px solid var(--color-border); color: var(--color-text); }
      h2 { font-size: 17px; margin: 22px 0 10px; padding-bottom: 6px; border-bottom: 1px solid var(--color-border); color: var(--color-text); }
      h3 { font-size: 15px; margin: 16px 0 8px; color: var(--color-text); }
      strong { font-weight: 650; color: var(--color-text); }
      ul, ol { padding-left: 22px; margin: 8px 0; }
      li { margin: 4px 0; line-height: 1.7; }
      p { margin: 8px 0; }
      code { background: var(--color-primary-bg); padding: 2px 6px; border-radius: 3px; font-size: 12.5px; }
      blockquote { border-left: 3px solid var(--color-primary); padding: 8px 14px; margin: 12px 0; background: var(--color-primary-bg); color: var(--color-text-secondary); border-radius: 0 6px 6px 0; }
      hr { border: none; border-top: 1px solid var(--color-border); margin: 16px 0; }
    }
  }

  .evaluating-hint {
    display: flex; align-items: center; gap: 10px; padding: 20px 0;
    color: var(--color-text-muted); font-size: 13px;
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
