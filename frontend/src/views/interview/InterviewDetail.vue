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
        <span class="score-text">平均分：<strong>{{ record.avgScore }}</strong></span>
        <span>题目数：{{ record.totalQuestions }}</span>
        <el-button type="primary" size="small" @click="downloadReport" :loading="downloading">
          下载 PDF 报告
        </el-button>
      </div>
    </el-card>

    <!-- AI 分析 -->
    <el-card v-if="record.aiAnalysis" class="analysis-card">
      <template #header>
        <span class="analysis-title">🤖 AI 面试分析与经验总结</span>
      </template>
      <MarkdownRenderer :content="record.aiAnalysis" />
      <div v-if="record.weakSkills && record.weakSkills !== '无'" style="margin-top:14px;display:flex;flex-wrap:wrap;gap:6px;align-items:center;">
        <span style="font-size:12px;color:#e6a23c;font-weight:600;">⚠️ 薄弱技能：</span>
        <el-tag v-for="w in record.weakSkills.split(',')" :key="w" type="warning" size="small">{{ w }}</el-tag>
      </div>
    </el-card>

    <!-- 逐题详情 -->
    <el-timeline style="margin-top: 24px;">
      <el-timeline-item
        v-for="(q, index) in details"
        :key="q.id"
        :timestamp="`第 ${index + 1} 题 · 得分 ${q.score}`"
        placement="top"
        :color="q.score >= 8 ? '#67c23a' : q.score >= 6 ? '#e6a23c' : '#f56c6c'"
      >
        <el-card shadow="hover" size="small">
          <div class="question-label">📝 题目</div>
          <p>{{ q.question }}</p>

          <div class="question-label">💬 你的回答</div>
          <p>{{ q.userAnswer || '未回答' }}</p>

          <div class="question-label">🤖 AI 点评</div>
          <MarkdownRenderer :content="q.aiComment || '无点评'" />
        </el-card>
      </el-timeline-item>
    </el-timeline>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import api from '@/api'
import MarkdownRenderer from '@/components/MarkdownRenderer.vue'

const route = useRoute()
const id = Number(route.params.id)

const loading = ref(false)
const downloading = ref(false)
const record = reactive<any>({})
const details = ref<any[]>([])

async function fetchDetail() {
  loading.value = true
  try {
    const res: any = await api.get(`/interview/records/${id}/details`)
    Object.assign(record, res.data || {})
    details.value = res.data?.questionDetails || []
  } catch { /* handled */ }
  loading.value = false
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

    .score-text strong {
      font-size: 18px;
      color: #409EFF;
    }
  }

  .question-label {
    font-size: 13px;
    font-weight: 600;
    color: #666;
    margin: 12px 0 4px;
  }

  p {
    margin: 4px 0;
    line-height: 1.6;
    color: #333;
  }
}
</style>
