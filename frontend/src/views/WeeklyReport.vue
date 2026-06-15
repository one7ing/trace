<template>
  <div class="weekly-report">
    <div class="page-header">
      <h2>
        <svg viewBox="0 0 24 24" fill="none" stroke="#7B61FF" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" width="20" height="20" style="vertical-align:-4px;margin-right:6px"><polyline points="22 12 18 12 15 21 9 3 6 12 2 12"/></svg>
        成长周报
      </h2>
    </div>

    <p class="desc">每周日自动为你生成成长轨迹汇总。AI 会分析你的日记、面试记录和知识探索，生成结构化周报。</p>

    <!-- 月份筛选 -->
    <div class="filter-bar">
      <span class="filter-label">筛选月份：</span>
      <el-select v-model="selectedMonth" placeholder="全部月份" clearable size="small" @change="onMonthChange" style="width:140px">
        <el-option v-for="m in availableMonths" :key="m.value" :label="m.label" :value="m.value" />
      </el-select>
    </div>

    <!-- 周报列表 -->
    <div class="report-list" v-loading="loading">
      <el-card
        v-for="report in pagedReports"
        :key="report.id"
        shadow="hover"
        class="report-card"
      >
        <div class="report-header">
          <span class="report-week">
            {{ report.weekStart }} ~ {{ report.weekEnd }}
          </span>
          <span class="report-date">{{ formatDate(report.generatedAt) }}</span>
        </div>
        <el-divider style="margin: 8px 0;" />
        <p class="report-summary">{{ report.summary }}</p>
        <div class="report-actions">
          <el-button text type="primary" @click="viewDetail(report.id)">
            查看详情
          </el-button>
          <el-button text type="primary" @click="downloadPdf(report.id)">
            下载 PDF
          </el-button>
        </div>
      </el-card>
    </div>

    <!-- 分页 -->
    <div class="pagination-box" v-if="filteredTotal > size">
      <el-pagination
        v-model:current-page="currentPage"
        :page-size="size"
        :total="filteredTotal"
        layout="prev, pager, next"
      />
    </div>

    <el-empty v-if="!loading && reports.length === 0" description="暂无周报，每周日自动生成" />

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" title="周报详情" width="720px" top="5vh" destroy-on-close>
      <div v-loading="detailLoading" class="detail-body">
        <MarkdownRenderer v-if="detailContent" :content="detailContent" />
      </div>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import api from '@/api'
import MarkdownRenderer from '@/components/MarkdownRenderer.vue'

const reports = ref<any[]>([])
const loading = ref(false)
const size = 4
const currentPage = ref(1)
const selectedMonth = ref('')

const detailVisible = ref(false)
const detailLoading = ref(false)
const detailContent = ref('')

// 从已有数据提取可用月份列表
const availableMonths = computed(() => {
  const months = new Map<string, string>()
  for (const r of reports.value) {
    const d = new Date(r.weekStart || r.generatedAt)
    if (isNaN(d.getTime())) continue
    const key = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`
    if (!months.has(key)) {
      months.set(key, `${d.getFullYear()}年${d.getMonth() + 1}月`)
    }
  }
  return Array.from(months.entries()).map(([value, label]) => ({ value, label }))
})

// 按月份筛选
const filteredReports = computed(() => {
  if (!selectedMonth.value) return reports.value
  return reports.value.filter(r => {
    const d = new Date(r.weekStart || r.generatedAt)
    const key = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`
    return key === selectedMonth.value
  })
})

const filteredTotal = computed(() => filteredReports.value.length)

const pagedReports = computed(() => {
  const start = (currentPage.value - 1) * size
  return filteredReports.value.slice(start, start + size)
})

function onMonthChange() {
  currentPage.value = 1
}

function formatDate(dateStr: string) {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString('zh-CN')
}

async function fetchReports() {
  loading.value = true
  try {
    // 获取所有记录，前端做分页和筛选
    const res: any = await api.get('/weekly-report', { params: { page: 0, size: 200 } })
    reports.value = res.data?.records || []
  } catch { /* handled */ }
  loading.value = false
}

async function viewDetail(id: number) {
  detailVisible.value = true
  detailLoading.value = true
  detailContent.value = ''
  try {
    const res: any = await api.get(`/weekly-report/${id}`)
    detailContent.value = res.data?.fullContent || res.data?.summary || ''
  } catch { /* handled */ }
  detailLoading.value = false
}

async function downloadPdf(id: number) {
  try {
    const res: any = await api.get(`/weekly-report/${id}/download`)
    window.open(res.data, '_blank')
  } catch { /* handled */ }
}

onMounted(fetchReports)
</script>

<style lang="scss" scoped>
.weekly-report {
  max-width: 800px;
  margin: 0 auto;

  .page-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 8px;
    h2 { font-size: 22px; margin: 0; }
  }

  .desc {
    color: #999;
    font-size: 13px;
    margin-bottom: 16px;
  }

  .filter-bar {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 16px;
    .filter-label { font-size: 13px; color: #666; }
  }

  .report-card {
    margin-bottom: 12px;

    .report-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      .report-week { font-weight: 600; color: #409EFF; }
      .report-date { font-size: 12px; color: #999; }
    }

    .report-summary {
      color: #666;
      font-size: 13px;
      line-height: 1.5;
    }

    .report-actions {
      text-align: right;
      margin-top: 4px;
      display: flex;
      justify-content: flex-end;
      gap: 4px;
    }
  }

  .pagination-box {
    margin-top: 16px;
    display: flex;
    justify-content: center;
  }

  .detail-body {
    max-height: 65vh;
    overflow-y: auto;
    :deep(.markdown-body) {
      font-size: 14px;
      line-height: 1.8;
      color: #333;
      h1 { font-size: 20px; margin: 20px 0 12px; padding-bottom: 8px; border-bottom: 2px solid #e8e8e8; }
      h2 { font-size: 17px; margin: 18px 0 10px; padding-bottom: 4px; border-bottom: 1px solid #eee; }
      h3 { font-size: 15px; margin: 14px 0 8px; color: #555; }
      ul, ol { padding-left: 22px; margin: 8px 0; }
      li { margin: 4px 0; }
      p { margin: 8px 0; }
      code { background: #f3f3f8; padding: 2px 6px; border-radius: 3px; font-size: 12.5px; }
      blockquote { border-left: 3px solid #7B61FF; padding: 6px 14px; margin: 12px 0; background: rgba(123,97,255,.04); color: #666; border-radius: 0 6px 6px 0; }
    }
  }
}
</style>
