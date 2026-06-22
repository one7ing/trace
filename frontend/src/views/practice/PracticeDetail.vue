<template>
  <div class="practice-detail" v-loading="loading">
    <el-page-header @back="$router.push('/practice/history')">
      <template #content>
        <span>刷题详情 - {{ record.topic }}</span>
      </template>
    </el-page-header>

    <el-card style="margin-top: 16px;" v-if="record.id">
      <div class="summary-bar">
        <el-tag type="success">{{ record.topic }}</el-tag>
        <span>题目数：{{ record.totalQuestions }}</span>
        <span>答对：{{ record.correctCount }}</span>
        <span v-if="record.score">平均分：{{ record.score }}/10</span>
        <span>{{ formatDateTime(record.completedAt) }}</span>
      </div>
    </el-card>

    <!-- 逐题详情 -->
    <el-card v-if="questionDetails.length > 0" class="questions-card" style="margin-top: 16px;">
      <template #header><span class="analysis-title">逐题判题详情</span></template>
      <div v-for="(qd, i) in questionDetails" :key="i" class="question-item" :class="{ correct: qd.isCorrect, wrong: !qd.isCorrect }">
        <div class="q-header">
          <span class="q-num">第 {{ qd.sequenceNum }} 题</span>
          <span class="q-badge" :class="qd.isCorrect ? 'badge-correct' : 'badge-wrong'">
            {{ qd.isCorrect ? '✓ 正确' : '✗ 错误' }}
          </span>
          <span class="q-score">{{ qd.score }} 分</span>
        </div>
        <div class="q-section"><strong>题目：</strong><MarkdownRenderer :content="qd.question"/></div>
        <div class="q-section ref"><strong>参考答案：</strong><MarkdownRenderer :content="qd.referenceAnswer"/></div>
        <div class="q-section user"><strong>你的回答：</strong>{{ qd.userAnswer || '(未作答)' }}</div>
        <div class="q-section comment" v-if="qd.aiComment"><strong>AI 点评：</strong>{{ qd.aiComment }}</div>
      </div>
    </el-card>

    <!-- 加载中 -->
    <div v-if="loadingDetails" class="evaluating-hint">
      <span class="evaluating-spinner"></span> 加载判题详情...
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import api from '@/api'
import MarkdownRenderer from '@/components/MarkdownRenderer.vue'

const route = useRoute()
const id = Number(route.params.id)

const loading = ref(false)
const loadingDetails = ref(false)
const record = ref<any>({})
const questionDetails = ref<any[]>([])

function formatDateTime(dateStr: string) {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  return d.toLocaleDateString('zh-CN') + ' ' + d.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

async function fetchDetail() {
  loading.value = true
  try {
    const res: any = await api.get(`/practice/records/${id}`)
    record.value = { ...res.data || {} }
  } catch {}
  loading.value = false

  // 加载逐题详情
  loadingDetails.value = true
  try {
    const res: any = await api.get(`/practice/records/${id}/questions`)
    questionDetails.value = res.data || []
  } catch {}
  loadingDetails.value = false
}

onMounted(fetchDetail)
</script>

<style lang="scss" scoped>
.practice-detail { max-width: 800px; margin: 0 auto;
  .summary-bar { display: flex; align-items: center; gap: 16px; flex-wrap: wrap; }
  .analysis-title { font-size: 16px; font-weight: 600; }
  .questions-card { margin-top: 16px; }
  .question-item {
    padding: 16px; margin-bottom: 14px; border-radius: 10px; border: 1.5px solid var(--color-border); background: var(--color-card-bg);
    &.correct { border-left: 4px solid #43B88C; }
    &.wrong { border-left: 4px solid #f56c6c; }
    .q-header { display: flex; align-items: center; gap: 12px; margin-bottom: 12px; flex-wrap: wrap;
      .q-num { font-weight: 600; font-size: 14px; color: var(--color-text); }
      .q-badge { font-size: 12px; padding: 2px 10px; border-radius: 4px; font-weight: 600;
        &.badge-correct { background: #e8f5e9; color: #2e7d32; }
        &.badge-wrong { background: #ffebee; color: #c62828; }
      }
      .q-score { font-size: 13px; color: var(--color-text-secondary); }
    }
    .q-section { margin-bottom: 8px; font-size: 13px; line-height: 1.6; color: var(--color-text);
      &.ref { background: #f0f7ff; padding: 10px 14px; border-radius: 8px; border: 1px solid #d0e3f7; }
      &.user { background: #fff8e1; padding: 10px 14px; border-radius: 8px; border: 1px solid #ffe082; }
      &.comment { background: #f3e5f5; padding: 10px 14px; border-radius: 8px; border: 1px solid #e1bee7; color: #6a1b9a; }
      strong { color: var(--color-text); }
      :deep(.markdown-body) { font-size: 13px; line-height: 1.6; p { margin: 4px 0; } }
    }
  }
  .evaluating-hint { display: flex; align-items: center; gap: 10px; padding: 20px 0; color: var(--color-text-muted); font-size: 13px; }
  .evaluating-spinner { width: 16px; height: 16px; border: 2px solid #e0e0e0; border-top-color: #43B88C; border-radius: 50%; animation: eval-spin 0.8s linear infinite; }
  @keyframes eval-spin { to { transform: rotate(360deg); } }
}
</style>
