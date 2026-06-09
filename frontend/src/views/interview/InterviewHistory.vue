<template>
  <div class="interview-history">
    <h2>📋 面试历史</h2>

    <el-table :data="records" stripe v-loading="loading" style="margin-top: 16px;">
      <el-table-column prop="id" label="编号" width="70" />
      <el-table-column prop="industry" label="行业" width="120" />
      <el-table-column label="技能" min-width="180">
        <template #default="{ row }">
          <el-tag v-for="tag in row.skillTags" :key="tag" size="small" style="margin-right: 4px;">
            {{ tag }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="totalQuestions" label="题数" width="70" />
      <el-table-column label="平均分" width="100">
        <template #default="{ row }">
          <span class="avg-score" :class="scoreClass(row.avgScore)">
            {{ row.avgScore }}
          </span>
        </template>
      </el-table-column>
      <el-table-column prop="completedAt" label="完成时间" width="170">
        <template #default="{ row }">
          {{ formatDate(row.completedAt) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="140">
        <template #default="{ row }">
          <el-button text type="primary" @click="$router.push(`/interview/history/${row.id}`)">
            查看详情
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-box" v-if="total > 0">
      <el-pagination
        v-model:current-page="page"
        :page-size="size"
        :total="total"
        layout="prev, pager, next"
        @current-change="fetchRecords"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import api from '@/api'

const records = ref<any[]>([])
const loading = ref(false)
const page = ref(0)
const size = ref(10)
const total = ref(0)

function scoreClass(score: number) {
  if (score >= 8) return 'score-high'
  if (score >= 6) return 'score-mid'
  return 'score-low'
}

function formatDate(dateStr: string) {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString('zh-CN')
}

async function fetchRecords() {
  loading.value = true
  try {
    const res: any = await api.get('/interview/records', { params: { page: page.value, size: size.value } })
    records.value = res.data?.content || []
    total.value = res.data?.totalElements || 0
  } catch { /* handled */ }
  loading.value = false
}

onMounted(fetchRecords)
</script>

<style lang="scss" scoped>
.interview-history {
  max-width: 900px;
  margin: 0 auto;

  h2 { font-size: 22px; }

  .avg-score {
    font-weight: 700;
    font-size: 16px;

    &.score-high { color: #67c23a; }
    &.score-mid  { color: #e6a23c; }
    &.score-low  { color: #f56c6c; }
  }

  .pagination-box {
    margin-top: 16px;
    display: flex;
    justify-content: center;
  }
}
</style>
