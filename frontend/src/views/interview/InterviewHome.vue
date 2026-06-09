<template>
  <div class="interview-home">
    <h2>🎯 模拟面试</h2>
    <p class="desc">AI 教练提供专业面试模拟，出题、评分、点评一步到位</p>

    <el-row :gutter="24" style="margin-top: 24px;">
      <!-- 新面试配置 -->
      <el-col :span="14">
        <el-card shadow="hover">
          <template #header>
            <span>开始新面试</span>
          </template>

          <el-form label-width="80px">
            <el-form-item label="选择行业">
              <el-select v-model="form.industry" placeholder="请选择行业" style="width: 100%;">
                <el-option label="IT / 互联网" value="IT" />
                <el-option label="金融" value="金融" />
                <el-option label="教育" value="教育" />
                <el-option label="医疗" value="医疗" />
                <el-option label="法律" value="法律" />
              </el-select>
            </el-form-item>

            <el-form-item label="技能方向">
              <el-select
                v-model="form.skills"
                multiple
                filterable
                allow-create
                placeholder="选择或输入技能标签"
                style="width: 100%;"
              >
                <el-option-group label="后端">
                  <el-option label="Java" value="Java" />
                  <el-option label="Spring" value="Spring" />
                  <el-option label="Python" value="Python" />
                  <el-option label="Go" value="Go" />
                </el-option-group>
                <el-option-group label="前端">
                  <el-option label="Vue" value="Vue" />
                  <el-option label="React" value="React" />
                  <el-option label="TypeScript" value="TypeScript" />
                </el-option-group>
                <el-option-group label="数据">
                  <el-option label="SQL" value="SQL" />
                  <el-option label="算法" value="算法" />
                </el-option-group>
              </el-select>
            </el-form-item>

            <el-form-item label="题目数量">
              <el-input-number v-model="form.questionCount" :min="1" :max="20" />
            </el-form-item>

            <el-form-item>
              <el-button type="primary" @click="startInterview" :disabled="!canStart">
                开始面试
              </el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>

      <!-- 快速入口 -->
      <el-col :span="10">
        <el-card shadow="hover">
          <template #header>
            <span>面试历史</span>
          </template>
          <div class="quick-actions">
            <el-button type="primary" @click="$router.push('/interview/history')" style="width: 100%; margin-bottom: 12px;">
              查看历史记录
            </el-button>
            <el-statistic title="平均分" :value="8.5" v-if="false" />
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { reactive, computed } from 'vue'
import { useRouter } from 'vue-router'
import api from '@/api'
import { ElMessage } from 'element-plus'

const router = useRouter()

const form = reactive({
  industry: '',
  skills: [] as string[],
  questionCount: 5,
})

const canStart = computed(() => form.industry && form.skills.length > 0)

async function startInterview() {
  try {
    const res: any = await api.post('/interview/start', form)
    router.push({
      path: '/interview/start',
      query: {
        sessionId: res.data.sessionId,
        question: res.data.question,
        current: String(res.data.currentQuestion),
        total: String(res.data.totalQuestions),
      },
    })
  } catch { /* 错误已在拦截器中处理 */ }
}
</script>

<style lang="scss" scoped>
.interview-home {
  max-width: 900px;
  margin: 0 auto;

  h2 {
    font-size: 22px;
    margin-bottom: 4px;
  }

  .desc {
    color: #999;
    font-size: 14px;
  }
}
</style>
