<template>
  <div class="diary-list">
    <div class="page-header">
      <h2>日记本</h2>
      <el-button v-if="!hasTodayDiary" type="primary" @click="$router.push('/diary/write')">
        <el-icon><Edit /></el-icon> 写日记
      </el-button>
      <el-tag v-else type="success">今日已记录 ✅</el-tag>
    </div>

    <!-- 心情标签快速筛选 -->
    <div class="mood-filters">
      <el-tag
        v-for="mood in moods"
        :key="mood"
        :type="selectedMood === mood ? '' : 'info'"
        :effect="selectedMood === mood ? 'dark' : 'plain'"
        @click="selectedMood = selectedMood === mood ? '' : mood"
        style="cursor: pointer; margin-right: 8px;"
      >
        {{ moodEmoji(mood) }} {{ mood }}
      </el-tag>
    </div>

    <!-- 日记时间线 -->
    <el-timeline v-loading="loading" style="margin-top: 20px;">
      <el-timeline-item
        v-for="diary in currentPageDiaries"
        :key="diary.id"
        :timestamp="formatDate(diary.createdAt)"
        placement="top"
        :color="moodColor(diary.moodTag)"
      >
        <el-card shadow="hover" class="diary-card" @click="$router.push(`/diary/${diary.id}`)">
          <div class="card-header">
            <span class="diary-title">{{ diary.title }}</span>
            <el-tag size="small" :type="moodTagType(diary.moodTag)">
              {{ moodEmoji(diary.moodTag) }} {{ diary.moodTag || '无' }}
            </el-tag>
          </div>
          <p class="diary-preview">{{ diary.content?.substring(0, 150) }}{{ diary.content?.length > 150 ? '...' : '' }}</p>
        </el-card>
      </el-timeline-item>
    </el-timeline>

    <!-- 分页 -->
    <div class="pagination-box" v-if="weekPages.length > 1">
      <el-pagination
        v-model:current-page="currentPage"
        :page-size="1"
        :total="weekPages.length"
        layout="prev, pager, next"
      />
    </div>

    <el-empty v-if="!loading && allDiaries.length === 0" description="还没有日记，开始记录吧">
      <el-button type="primary" @click="$router.push('/diary/write')">写第一篇日记</el-button>
    </el-empty>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import api from '@/api'
import { Edit } from '@element-plus/icons-vue'

const moods = ['开心', '平静', '焦虑', '充实', '疲惫', '感恩']
const selectedMood = ref('')
const allDiaries = ref<any[]>([])
const loading = ref(false)
const currentPage = ref(1)

/** 今天是否已有日记 */
const hasTodayDiary = computed(() => {
  const today = new Date().toDateString()
  return allDiaries.value.some(d => new Date(d.createdAt).toDateString() === today)
})

function getMonday(d: Date): string {
  const day = d.getDay()
  const diff = d.getDate() - day + (day === 0 ? -6 : 1)
  const mon = new Date(d.getFullYear(), d.getMonth(), diff)
  return mon.toISOString().slice(0, 10)
}

/** 按心情过滤 */
const filteredDiaries = computed(() => {
  if (!selectedMood.value) return allDiaries.value
  return allDiaries.value.filter(d => d.moodTag === selectedMood.value)
})

/** 按周分组，从旧到新排序 */
const weekPages = computed(() => {
  const map = new Map<string, any[]>()
  for (const d of filteredDiaries.value) {
    const dObj = new Date(d.createdAt)
    if (isNaN(dObj.getTime())) continue
    const mon = getMonday(dObj)
    if (!map.has(mon)) map.set(mon, [])
    map.get(mon)!.push(d)
  }
  // 按周一日期排序（旧→新），与分页 page 1=最新周保持一致
  const sorted = Array.from(map.entries())
    .sort((a, b) => b[0].localeCompare(a[0]))
  return sorted
})

/** 当前页的日记 */
const currentPageDiaries = computed(() => {
  const idx = currentPage.value - 1
  if (idx < 0 || idx >= weekPages.value.length) return []
  return weekPages.value[idx][1]
})

function moodEmoji(mood: string) {
  const map: Record<string, string> = {
    '开心': '😊', '平静': '😌', '焦虑': '😰',
    '充实': '💪', '疲惫': '😫', '感恩': '🙏',
  }
  return map[mood] || ''
}

function moodColor(mood: string) {
  const map: Record<string, string> = {
    '开心': '#67c23a', '平静': '#409EFF', '焦虑': '#f56c6c',
    '充实': '#e6a23c', '疲惫': '#909399', '感恩': '#e040fb',
  }
  return map[mood] || '#409EFF'
}

function moodTagType(mood: string) {
  const map: Record<string, string> = {
    '开心': 'success', '平静': '', '焦虑': 'danger',
    '充实': 'warning', '疲惫': 'info', '感恩': 'danger',
  }
  return map[mood] || 'info'
}

function formatDate(dateStr: string) {
  if (!dateStr) return '-'
  const d = new Date(dateStr)
  return d.toLocaleDateString('zh-CN') + ' ' +
    d.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

async function fetchAllDiaries() {
  loading.value = true
  try {
    const res: any = await api.get('/diary', { params: { page: 0, size: 500 } })
    allDiaries.value = res.data?.records || []
  } catch { /* handled */ }
  loading.value = false
}

onMounted(fetchAllDiaries)
</script>

<style lang="scss" scoped>
.diary-list {
  max-width: 750px;
  margin: 0 auto;

  .page-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;
    h2 { font-size: 22px; margin: 0; color: var(--color-text); }
  }

  .mood-filters { margin: 12px 0; }

  .diary-card {
    cursor: pointer; border-radius: var(--radius-lg);
    transition: all var(--transition);
    &:hover { transform: translateY(-2px); box-shadow: var(--shadow-md); }

    .card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 8px;
      .diary-title { font-size: 16px; font-weight: 600; color: var(--color-text); }
    }

    .diary-preview {
      color: var(--color-text-secondary);
      font-size: 13px;
      line-height: 1.5;
      margin: 0;
    }
  }

  .pagination-box {
    margin-top: 20px;
    display: flex;
    justify-content: center;
  }
}
</style>
