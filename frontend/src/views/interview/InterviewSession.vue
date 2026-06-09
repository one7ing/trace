<template>
  <div class="interview-session">
    <!-- 进度条 -->
    <div class="progress-bar">
      <el-progress
        :percentage="Math.round((currentQuestion / totalQuestions) * 100)"
        :text-inside="true"
        :stroke-width="20"
      >
        <span>{{ currentQuestion }} / {{ totalQuestions }}</span>
      </el-progress>
    </div>

    <!-- 题目卡片 -->
    <el-card shadow="hover" class="question-card" v-if="!finished">
      <div class="question-number">第 {{ currentQuestion }} 题</div>
      <div class="question-text">{{ currentQuestionText }}</div>
    </el-card>

    <!-- 回答区域 -->
    <div class="answer-area" v-if="!finished">
      <el-input
        v-model="answer"
        type="textarea"
        :rows="5"
        placeholder="请输入你的回答..."
        :disabled="evaluating"
      />
      <div class="answer-actions">
        <el-button
          type="primary"
          @click="submitAnswer"
          :disabled="!answer.trim() || evaluating"
          :loading="evaluating"
        >
          提交回答
        </el-button>
      </div>
    </div>

    <!-- 评分结果 -->
    <el-card v-if="lastScore !== null && !finished" class="score-card">
      <div class="score-display">
        <div class="score-circle">
          <span class="score-number">{{ lastScore }}</span>
          <span class="score-unit">/10</span>
        </div>
        <div class="score-comment">
          <MarkdownRenderer :content="lastComment" />
        </div>
      </div>
    </el-card>

    <!-- 面试结束 -->
    <el-result
      v-if="finished"
      icon="success"
      title="面试完成！"
      :sub-title="`平均分：${avgScore} / 10`"
    >
      <template #extra>
        <el-button type="primary" @click="downloadReport" :loading="downloading">
          下载面试报告 PDF
        </el-button>
        <el-button @click="$router.push('/interview/history')">
          查看历史记录
        </el-button>
        <el-button @click="$router.push('/interview')">
          再来一轮
        </el-button>
      </template>
    </el-result>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '@/api'
import MarkdownRenderer from '@/components/MarkdownRenderer.vue'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()

const sessionId = ref(route.query.sessionId as string || '')
const currentQuestion = ref(Number(route.query.current) || 1)
const totalQuestions = ref(Number(route.query.total) || 5)
const currentQuestionText = ref(route.query.question as string || '')
const answer = ref('')
const evaluating = ref(false)
const lastScore = ref<number | null>(null)
const lastComment = ref('')
const finished = ref(false)
const avgScore = ref('')
const recordId = ref<number | null>(null)
const downloading = ref(false)

async function submitAnswer() {
  if (!answer.value.trim()) return

  evaluating.value = true
  lastScore.value = null

  try {
    const res: any = await api.post('/interview/answer', {
      sessionId: sessionId.value,
      answer: answer.value,
    })

    const data = res.data
    lastScore.value = data.score
    lastComment.value = data.comment
    answer.value = ''

    if (data.isLast) {
      finished.value = true
      avgScore.value = data.avgScore
      recordId.value = data.recordId
    } else {
      currentQuestion.value++
      currentQuestionText.value = data.nextQuestion
    }
  } catch { /* handled */ }

  evaluating.value = false
}

async function downloadReport() {
  if (!recordId.value) return
  downloading.value = true
  try {
    const res: any = await api.get(`/interview/report/${recordId.value}/download`)
    window.open(res.data, '_blank')
  } catch { /* handled */ }
  downloading.value = false
}
</script>

<style lang="scss" scoped>
.interview-session {
  max-width: 700px;
  margin: 0 auto;

  .progress-bar {
    margin-bottom: 24px;
  }

  .question-card {
    margin-bottom: 20px;

    .question-number {
      font-size: 14px;
      color: #409EFF;
      margin-bottom: 8px;
      font-weight: 600;
    }

    .question-text {
      font-size: 16px;
      line-height: 1.6;
      color: #333;
    }
  }

  .answer-area {
    margin-bottom: 20px;

    .answer-actions {
      margin-top: 12px;
      text-align: right;
    }
  }

  .score-card {
    background: #f5f7fa;

    .score-display {
      display: flex;
      gap: 20px;
      align-items: flex-start;

      .score-circle {
        flex-shrink: 0;
        width: 72px;
        height: 72px;
        border-radius: 50%;
        background: #409EFF;
        color: #fff;
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;

        .score-number {
          font-size: 24px;
          font-weight: 700;
          line-height: 1;
        }

        .score-unit {
          font-size: 11px;
          opacity: 0.8;
        }
      }

      .score-comment {
        flex: 1;
        font-size: 14px;
      }
    }
  }
}
</style>
