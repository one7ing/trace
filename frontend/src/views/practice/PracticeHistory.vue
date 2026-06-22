<template>
  <div class="history-page">
    <div class="page-header">
      <h1 class="page-title">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="18" height="18" style="color:#43B88C;margin-right:8px;vertical-align:-2px">
          <circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/>
        </svg>
        刷题记录
      </h1>
      <p class="page-subtitle">回顾每次刷题表现，查看 AI 判题与分析</p>
    </div>

    <div v-loading="loading">
      <div v-for="record in records" :key="record.id" class="record-card" @click="$router.push(`/practice/history/${record.id}`)">
        <div class="card-top">
          <div class="card-main">
            <span class="card-topic">{{ record.topic }}</span>
            <span class="card-meta">{{ record.totalQuestions }} 题 · {{ record.correctCount }} 对 · {{ record.score ? record.score + '/10' : '-' }}</span>
          </div>
          <div class="card-right">
            <button class="btn-delete" @click.stop="handleDelete(record)" title="删除记录">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" width="15" height="15"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/></svg>
            </button>
          </div>
        </div>
        <div class="card-analysis" v-if="record.aiAnalysis">
          <div class="analysis-preview">
            <MarkdownRenderer :content="record.aiAnalysis" />
          </div>
        </div>
        <div class="card-footer">
          <span class="card-date">{{ formatDate(record.completedAt) }}</span>
          <span class="card-link">查看详情 →</span>
        </div>
      </div>

      <el-empty v-if="!loading && records.length === 0" description="暂无刷题记录">
        <el-button type="primary" @click="$router.push('/practice')">开始刷题</el-button>
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
    const res: any = await api.get('/practice/records', { params: { page: 0, size: 20 } })
    records.value = res.data?.records || []
  } catch {}
  loading.value = false
}

async function handleDelete(record: any) {
  try {
    await ElMessageBox.confirm(`确定要删除这条 ${record.topic} 刷题记录吗？`, '删除确认',
      { confirmButtonText: '确认删除', cancelButtonText: '取消', type: 'warning' })
  } catch { return }
  try {
    await api.delete(`/practice/records/${record.id}`)
    records.value = records.value.filter(r => r.id !== record.id)
    ElMessage.success('记录已删除')
  } catch { ElMessage.error('删除失败') }
}

onMounted(fetchRecords)
</script>

<style lang="scss" scoped>
.history-page { max-width: 800px; margin: 0 auto; padding-bottom: 40px; }
.page-header { margin-bottom: 22px;
  .page-title { font-size: 20px; font-weight: 700; color: var(--color-text); margin: 0 0 6px; }
  .page-subtitle { font-size: 13px; color: var(--color-text-muted); margin: 0; }
}
.record-card { padding: 18px 20px; border-radius: 10px; border: 1.5px solid var(--color-border); background: var(--color-card); margin-bottom: 12px; cursor: pointer; transition: all var(--transition);
  &:hover { border-color: var(--color-primary); box-shadow: 0 2px 12px rgba(108,92,231,.06); transform: translateY(-1px); }
}
.card-top { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 12px; }
.card-main { flex: 1; display:flex; align-items:center; gap:12px; flex-wrap:wrap; }
.card-right { flex-shrink: 0; margin-left: 16px; }
.btn-delete { background: none; border: none; color: var(--color-text-muted); cursor: pointer; padding: 2px; border-radius: 4px; transition: all var(--transition);
  &:hover { color: #f56c6c; background: rgba(245,108,108,.08); }
}
.card-topic { font-size: 15px; font-weight: 600; color: var(--color-text); }
.card-meta { font-size: 12px; color: var(--color-text-secondary); }
.card-analysis { padding: 12px 14px; border-radius: 8px; background: var(--color-score-bg); border: 1px solid var(--color-analysis-border); margin-bottom: 8px;
  .analysis-preview { font-size: 12px; color: var(--color-text-secondary); line-height: 1.5; max-height: 60px; overflow: hidden; position: relative;
    &::after { content:'';position:absolute;bottom:0;left:0;right:0;height:20px;background:linear-gradient(transparent,var(--color-score-bg,#fafafa)); }
    :deep(.markdown-body) { font-size:12px;line-height:1.5;h1,h2,h3,h4{font-size:13px;margin:4px 0}p{margin:2px 0}ul,ol{padding-left:16px;margin:2px 0}li{margin:1px 0}strong{font-weight:600}code{font-size:11px} }
  }
}
.card-footer { display: flex; justify-content: space-between; align-items: center; }
.card-date { font-size: 12px; color: var(--color-text-muted); }
.card-link { font-size: 12px; color: #7B61FF; font-weight: 500; }
</style>
