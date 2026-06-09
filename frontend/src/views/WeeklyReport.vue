<template>
  <div class="weekly-report">
    <div class="page-header">
      <h2>📈 成长周报</h2>
      <el-button type="primary" @click="generateReport" :loading="generating">
        生成本周周报
      </el-button>
    </div>

    <p class="desc">每周自动生成你的成长轨迹汇总。AI 会分析你的日记、面试记录和知识探索，生成结构化周报。</p>

    <!-- 周报列表 -->
    <div class="report-list" v-loading="loading">
      <el-card
        v-for="report in reports"
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
          <el-button text type="primary" @click="downloadPdf(report.id)">
            下载 PDF
          </el-button>
        </div>
      </el-card>
    </div>

    <div class="pagination-box" v-if="total > 0">
      <el-pagination
        v-model:current-page="page"
        :page-size="size"
        :total="total"
        layout="prev, pager, next"
        @current-change="fetchReports"
      />
    </div>

    <el-empty v-if="!loading && reports.length === 0" description="本周周报尚未生成">
      <el-button type="primary" @click="generateReport">立即生成</el-button>
    </el-empty>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import api from '@/api'
import { ElMessage } from 'element-plus'

const reports = ref<any[]>([])
const loading = ref(false)
const generating = ref(false)
const page = ref(0)
const size = ref(10)
const total = ref(0)

function formatDate(dateStr: string) {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString('zh-CN')
}

async function fetchReports() {
  loading.value = true
  try {
    const res: any = await api.get('/weekly-report', { params: { page: page.value, size: size.value } })
    reports.value = res.data?.content || []
    total.value = res.data?.totalElements || 0
  } catch { /* handled */ }
  loading.value = false
}

async function generateReport() {
  generating.value = true
  try {
    await api.post('/weekly-report/generate')
    ElMessage.success('周报已生成')
    fetchReports()
  } catch { /* handled */ }
  generating.value = false
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
    margin-bottom: 24px;
  }

  .report-card {
    margin-bottom: 12px;

    .report-header {
      display: flex;
      justify-content: space-between;
      align-items: center;

      .report-week {
        font-weight: 600;
        color: #409EFF;
      }

      .report-date {
        font-size: 12px;
        color: #999;
      }
    }

    .report-summary {
      color: #666;
      font-size: 13px;
      line-height: 1.5;
    }

    .report-actions {
      text-align: right;
      margin-top: 4px;
    }
  }

  .pagination-box {
    margin-top: 16px;
    display: flex;
    justify-content: center;
  }
}
</style>
