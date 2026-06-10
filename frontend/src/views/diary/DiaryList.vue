<template>
  <div class="diary-list">
    <div class="page-header">
      <h2>📓 日记本</h2>
      <el-button type="primary" @click="$router.push('/diary/write')">
        <el-icon><Edit /></el-icon> 写日记
      </el-button>
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
    <el-timeline v-loading="loading" style="margin-top: 24px;">
      <el-timeline-item
        v-for="diary in diaries"
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

    <div class="pagination-box" v-if="total > 0">
      <el-pagination
        v-model:current-page="page"
        :page-size="size"
        :total="total"
        layout="prev, pager, next"
        @current-change="fetchDiaries"
      />
    </div>

    <el-empty v-if="!loading && diaries.length === 0" description="还没有日记，开始记录吧 ✍️">
      <el-button type="primary" @click="$router.push('/diary/write')">写第一篇日记</el-button>
    </el-empty>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import api from '@/api'
import { Edit } from '@element-plus/icons-vue'

const moods = ['开心', '平静', '焦虑', '充实', '疲惫', '感恩']
const selectedMood = ref('')
const diaries = ref<any[]>([])
const loading = ref(false)
const page = ref(0)
const size = ref(10)
const total = ref(0)

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
  return new Date(dateStr).toLocaleString('zh-CN', {
    year: 'numeric', month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit',
  })
}

async function fetchDiaries() {
  loading.value = true
  try {
    const res: any = await api.get('/diary', { params: { page: page.value, size: size.value } })
    diaries.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch { /* handled */ }
  loading.value = false
}

onMounted(fetchDiaries)
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

    h2 { font-size: 22px; margin: 0; }
  }

  .mood-filters {
    margin: 12px 0;
  }

  .diary-card {
    cursor: pointer;
    transition: transform 0.15s;

    &:hover {
      transform: translateY(-2px);
    }

    .card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 8px;

      .diary-title {
        font-size: 16px;
        font-weight: 600;
        color: #1a1a2e;
      }
    }

    .diary-preview {
      color: #666;
      font-size: 13px;
      line-height: 1.5;
      margin: 0;
    }
  }

  .pagination-box {
    margin-top: 16px;
    display: flex;
    justify-content: center;
  }
}
</style>
