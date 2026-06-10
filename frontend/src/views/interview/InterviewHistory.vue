<template>
  <div class="history-page">
    <div class="page-header">
      <h1 class="page-title">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="18" height="18" style="color:#7B61FF;margin-right:8px;vertical-align:-2px">
          <circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/>
        </svg>
        面试记录
      </h1>
      <p class="page-subtitle">回顾每次面试表现，查看 AI 分析与改进建议</p>
    </div>

    <div v-loading="loading">
      <div v-for="record in records" :key="record.id" class="record-card" @click="$router.push(`/interview/history/${record.id}`)">
        <div class="card-top">
          <div class="card-main">
            <span class="card-industry">{{ record.industry }}</span>
            <div class="card-tags">
              <span v-for="t in record.skillTags" :key="t" class="card-tag">{{ t }}</span>
            </div>
          </div>
          <div class="card-score" :class="scoreClass(record.avgScore)">{{ record.avgScore }}</div>
        </div>

        <!-- AI 分析摘要 -->
        <div class="card-analysis" v-if="record.aiAnalysis">
          <div class="analysis-label">
            <svg viewBox="0 0 24 24" fill="none" stroke="#7B61FF" stroke-width="1.5" width="14" height="14" style="vertical-align:-2px;margin-right:4px">
              <circle cx="12" cy="12" r="10"/><path d="M9.09 9a3 3 0 0 1 5.83 1c0 2-3 3-3 3"/><line x1="12" y1="17" x2="12.01" y2="17"/>
            </svg>
            AI 分析
          </div>
          <p class="analysis-text">{{ record.aiAnalysis?.substring(0, 120) }}{{ record.aiAnalysis?.length > 120 ? '...' : '' }}</p>
          <div class="weak-tags" v-if="record.weakSkills && record.weakSkills !== '无'">
            <span class="weak-label">薄弱项：</span>
            <span v-for="w in record.weakSkills?.split(',')" :key="w" class="weak-tag">{{ w }}</span>
          </div>
        </div>

        <div class="card-footer">
          <span class="card-meta">{{ record.totalQuestions }} 题 · {{ formatDate(record.completedAt) }}</span>
          <span class="card-link">查看详情 →</span>
        </div>
      </div>

      <el-empty v-if="!loading && records.length === 0" description="暂无面试记录">
        <el-button type="primary" @click="$router.push('/interview')">开始模拟面试</el-button>
      </el-empty>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import api from '@/api'

const router = useRouter()
const records = ref<any[]>([])
const loading = ref(false)

function scoreClass(s: number) {
  if (s >= 8) return 'high'
  if (s >= 6) return 'mid'
  return 'low'
}
function formatDate(d: string) {
  if (!d) return '-'
  return new Date(d).toLocaleDateString('zh-CN')
}

async function fetchRecords() {
  loading.value = true
  try {
    const res: any = await api.get('/interview/records', { params: { page: 0, size: 20 } })
    records.value = res.data?.records || []
  } catch {}
  loading.value = false
}

onMounted(fetchRecords)
</script>

<style lang="scss" scoped>
.history-page { max-width: 800px; margin: 0 auto; padding-bottom: 40px; }
.page-header { margin-bottom: 22px;
  .page-title { font-size: 20px; font-weight: 700; color: var(--color-text); margin: 0 0 6px; }
  .page-subtitle { font-size: 13px; color: var(--color-text-muted); margin: 0; }
}

.record-card {
  padding: 18px 20px; border-radius: 10px; border: 1.5px solid var(--color-border); background: var(--color-card);
  margin-bottom: 12px; cursor: pointer; transition: all var(--transition);
  &:hover { border-color: var(--color-primary); box-shadow: 0 2px 12px rgba(108,92,231,.06); transform: translateY(-1px); }
}
.card-top { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 12px; }
.card-main { flex: 1; }
.card-industry { font-size: 15px; font-weight: 600; color: var(--color-text); margin-right: 10px; }
.card-tags { display: inline-flex; gap: 6px; flex-wrap: wrap; margin-top: 4px; }
.card-tag { font-size: 11px; padding: 2px 8px; border-radius: 4px; background: var(--color-active); color: #7B61FF; }
.card-score { font-size: 26px; font-weight: 700; flex-shrink: 0; margin-left: 16px;
  &.high { color: #43B88C; } &.mid { color: #e6a23c; } &.low { color: #f56c6c; }
}

.card-analysis {
  padding: 12px 14px; border-radius: 8px; background: var(--color-score-bg); border: 1px solid var(--color-analysis-border); margin-bottom: 8px;
  .analysis-label { font-size: 11px; font-weight: 600; color: #7B61FF; margin-bottom: 6px; }
  .analysis-text { font-size: 12px; color: var(--color-text-secondary); line-height: 1.5; margin: 0; }
  .weak-tags { margin-top: 8px; display: flex; flex-wrap: wrap; gap: 4px; align-items: center; }
  .weak-label { font-size: 11px; color: #e6a23c; }
  .weak-tag { font-size: 10px; padding: 2px 7px; border-radius: 4px; background: var(--color-weak-bg); color: var(--color-weak-text); border: 1px solid var(--color-weak-border); }
}

.card-footer { display: flex; justify-content: space-between; align-items: center; margin-top: 4px; }
.card-meta { font-size: 12px; color: var(--color-text-muted); }
.card-link { font-size: 12px; color: #7B61FF; font-weight: 500; }
</style>
