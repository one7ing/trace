<template>
  <div class="diary-detail" v-loading="loading">
    <el-page-header @back="$router.push('/diary')">
      <template #content>
        <span>{{ diary.title }}</span>
      </template>
      <template #extra>
        <el-button v-if="isToday(diary.createdAt)" text type="primary" @click="$router.push(`/diary/write?id=${diary.id}`)">
          编辑
        </el-button>
        <el-popconfirm title="确定删除这篇日记？" @confirm="handleDelete">
          <template #reference>
            <el-button text type="danger">删除</el-button>
          </template>
        </el-popconfirm>
      </template>
    </el-page-header>

    <el-card v-if="!loading && diary.id" style="margin-top: 16px;">
      <div class="diary-meta">
        <el-tag :type="moodTagType(diary.moodTag)">
          {{ moodEmoji(diary.moodTag) }} {{ diary.moodTag || '无' }}
        </el-tag>
        <span class="diary-time">{{ diary.createdAt }}</span>
      </div>
      <el-divider />
      <div class="diary-content">{{ diary.content }}</div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '@/api'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const id = Number(route.params.id)
const loading = ref(false)
const diary = reactive<any>({})

function moodEmoji(mood: string) {
  const map: Record<string, string> = {
    '开心': '😊', '平静': '😌', '焦虑': '😰',
    '充实': '💪', '疲惫': '😫', '感恩': '🙏',
  }
  return map[mood] || ''
}

function moodTagType(mood: string) {
  const map: Record<string, string> = {
    '开心': 'success', '平静': '', '焦虑': 'danger',
    '充实': 'warning', '疲惫': 'info', '感恩': 'danger',
  }
  return map[mood] || 'info'
}

function isToday(dateStr: string) {
  if (!dateStr) return false
  return new Date(dateStr).toDateString() === new Date().toDateString()
}

async function fetchDiary() {
  loading.value = true
  try {
    const res: any = await api.get(`/diary/${id}`)
    Object.assign(diary, res.data || {})
  } catch { router.push('/diary') }
  loading.value = false
}

async function handleDelete() {
  try {
    await api.delete(`/diary/${id}`)
    ElMessage.success('日记已删除')
    router.push('/diary')
  } catch { /* handled */ }
}

onMounted(fetchDiary)
</script>

<style lang="scss" scoped>
.diary-detail {
  max-width: 750px;
  margin: 0 auto;

  .diary-meta {
    display: flex;
    justify-content: space-between;
    align-items: center;

    .diary-time {
      color: #999;
      font-size: 13px;
    }
  }

  .diary-content {
    font-size: 15px;
    line-height: 1.8;
    color: #333;
    white-space: pre-wrap;
    word-break: break-word;
  }
}
</style>
