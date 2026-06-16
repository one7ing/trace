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
          <div class="card-right">
            <button class="btn-delete" @click.stop="handleDelete(record)" title="删除记录">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" width="15" height="15"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/></svg>
            </button>
          </div>
        </div>

        <!-- AI 分析摘要 -->
        <div class="card-analysis" v-if="record.aiAnalysis">
          <div class="analysis-label">
            <svg viewBox="0 0 24 24" fill="none" stroke="#7B61FF" stroke-width="1.5" width="14" height="14" style="vertical-align:-2px;margin-right:4px">
              <circle cx="12" cy="12" r="10"/><path d="M9.09 9a3 3 0 0 1 5.83 1c0 2-3 3-3 3"/><line x1="12" y1="17" x2="12.01" y2="17"/>
            </svg>
            AI 分析
          </div>
          <div class="analysis-preview">
            <MarkdownRenderer :content="record.aiAnalysis" />
          </div>
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
import MarkdownRenderer from '@/components/MarkdownRenderer.vue'
import { ElMessageBox, ElMessage } from 'element-plus'

const router = useRouter()
const records = ref<any[]>([])
const loading = ref(false)

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

async function handleDelete(record: any) {
  try {
    await ElMessageBox.confirm(
      `确定要删除这条 ${record.industry} 面试记录吗？删除后无法恢复。`,
      '删除确认',
      { confirmButtonText: '确认删除', cancelButtonText: '取消', type: 'warning' }
    )
  } catch { return }
  try {
    await api.delete(`/interview/records/${record.id}`)
    records.value = records.value.filter(r => r.id !== record.id)
    ElMessage.success('记录已删除')
  } catch {
    ElMessage.error('删除失败')
  }
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
.card-right { display: flex; flex-direction: column; align-items: flex-end; gap: 8px; flex-shrink: 0; margin-left: 16px; }
.btn-delete {
  background: none; border: none; color: var(--color-text-muted); cursor: pointer; padding: 2px;
  border-radius: 4px; transition: all var(--transition);
  &:hover { color: #f56c6c; background: rgba(245,108,108,.08); }
}
.card-industry { font-size: 15px; font-weight: 600; color: var(--color-text); margin-right: 10px; }
.card-tags { display: inline-flex; gap: 6px; flex-wrap: wrap; margin-top: 4px; }
.card-tag { font-size: 11px; padding: 2px 8px; border-radius: 4px; background: var(--color-active); color: #7B61FF; }

.card-analysis {
  padding: 12px 14px; border-radius: 8px; background: var(--color-score-bg); border: 1px solid var(--color-analysis-border); margin-bottom: 8px;
  .analysis-label { font-size: 11px; font-weight: 600; color: #7B61FF; margin-bottom: 6px; }
  .analysis-text { font-size: 12px; color: var(--color-text-secondary); line-height: 1.5; margin: 0; }
  .analysis-preview {
    font-size: 12px;
    color: var(--color-text-secondary);
    line-height: 1.5;
    max-height: 80px;
    overflow: hidden;
    position: relative;
    &::after {
      content: '';
      position: absolute;
      bottom: 0;
      left: 0;
      right: 0;
      height: 24px;
      background: linear-gradient(transparent, var(--color-score-bg, #fafafa));
    }
    :deep(.markdown-body) {
      font-size: 12px;
      line-height: 1.5;
      h1, h2, h3, h4 { font-size: 13px; margin: 4px 0; }
      p { margin: 2px 0; }
      ul, ol { padding-left: 16px; margin: 2px 0; }
      li { margin: 1px 0; }
      strong { font-weight: 600; }
      code { font-size: 11px; }
    }
  }
  .weak-tags { margin-top: 8px; display: flex; flex-wrap: wrap; gap: 4px; align-items: center; }
  .weak-label { font-size: 11px; color: #e6a23c; }
  .weak-tag { font-size: 10px; padding: 2px 7px; border-radius: 4px; background: var(--color-weak-bg); color: var(--color-weak-text); border: 1px solid var(--color-weak-border); }
}

.card-footer { display: flex; justify-content: space-between; align-items: center; margin-top: 4px; }
.card-meta { font-size: 12px; color: var(--color-text-muted); }
.card-link { font-size: 12px; color: #7B61FF; font-weight: 500; }
</style>
