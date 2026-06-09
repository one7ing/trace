<template>
  <div class="diary-write">
    <el-page-header @back="$router.push('/diary')">
      <template #content>
        <span>{{ isEdit ? '编辑日记' : '写日记' }}</span>
      </template>
    </el-page-header>

    <el-card style="margin-top: 16px;">
      <el-form label-width="70px">
        <el-form-item label="标题">
          <el-input v-model="form.title" placeholder="给今天的日记起个标题..." maxlength="200" show-word-limit />
        </el-form-item>

        <el-form-item label="心情">
          <div class="mood-selector">
            <el-tag
              v-for="mood in moods"
              :key="mood"
              :type="form.moodTag === mood ? '' : 'info'"
              :effect="form.moodTag === mood ? 'dark' : 'plain'"
              @click="form.moodTag = form.moodTag === mood ? '' : mood"
              style="cursor: pointer; margin-right: 8px;"
            >
              {{ moodEmoji(mood) }} {{ mood }}
            </el-tag>
          </div>
        </el-form-item>

        <el-form-item label="内容">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="12"
            placeholder="今天发生了什么？有什么感悟..."
          />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="submit" :loading="saving">
            {{ isEdit ? '保存修改' : '发布日记' }}
          </el-button>
          <el-button @click="$router.push('/diary')">取消</el-button>
        </el-form-item>
      </el-form>
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
const isEdit = ref(false)
const saving = ref(false)

const moods = ['开心', '平静', '焦虑', '充实', '疲惫', '感恩']

const form = reactive({
  title: '',
  content: '',
  moodTag: '',
})

function moodEmoji(mood: string) {
  const map: Record<string, string> = {
    '开心': '😊', '平静': '😌', '焦虑': '😰',
    '充实': '💪', '疲惫': '😫', '感恩': '🙏',
  }
  return map[mood] || ''
}

async function submit() {
  if (!form.title.trim() || !form.content.trim()) {
    ElMessage.warning('标题和内容不能为空')
    return
  }

  saving.value = true
  try {
    if (isEdit.value) {
      await api.put(`/diary/${route.params.id}`, form)
      ElMessage.success('日记已更新')
    } else {
      await api.post('/diary', form)
      ElMessage.success('日记已发布')
    }
    router.push('/diary')
  } catch { /* handled */ }
  saving.value = false
}

onMounted(async () => {
  const id = route.params.id
  if (id && id !== 'write') {
    isEdit.value = true
    try {
      const res: any = await api.get(`/diary/${id}`)
      Object.assign(form, {
        title: res.data.title,
        content: res.data.content,
        moodTag: res.data.moodTag,
      })
    } catch { router.push('/diary') }
  }
})
</script>

<style lang="scss" scoped>
.diary-write {
  max-width: 750px;
  margin: 0 auto;

  .mood-selector {
    display: flex;
    flex-wrap: wrap;
    gap: 4px;
  }
}
</style>
