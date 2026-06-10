<template>
  <div class="interview-session">
    <div class="session-hero">
      <div class="hero-avatar">
        <svg viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2" width="22" height="22"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg>
      </div>
      <div class="hero-info">
        <h2>你的专属面试官</h2>
        <div class="hero-meta">
          <span class="meta-tag">{{ difficultyLabel }}</span>
          <span class="meta-progress">第 {{ currentQuestion }} / {{ totalQuestions }} 题</span>
        </div>
      </div>
    </div>
    <div class="progress-bar-wrap"><div class="progress-bar" :style="{ width: progressPct + '%' }"></div></div>

    <div class="interview-chat" ref="chatRef">
      <template v-if="!finished">
        <!-- 题目（流式输出中或已稳定） -->
        <div class="msg-row ai">
          <div class="msg-avatar">
            <svg viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2" width="16" height="16"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg>
          </div>
          <div class="msg-body">
            <div class="msg-sender">面试官 · Trace AI</div>
            <div class="msg-bubble ai">
              <div class="question-label">{{ questionStreaming ? '正在出题...' : '📝 面试题' }}</div>
              <p class="question-text">{{ currentQuestionText || '准备中...' }}</p>
            </div>
          </div>
        </div>

        <div v-if="lastScore !== null" class="msg-row ai">
          <div class="msg-avatar"><svg viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2" width="16" height="16"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg></div>
          <div class="msg-body">
            <div class="msg-sender">面试官 · Trace AI</div>
            <div class="msg-bubble score-bubble">
              <div class="score-row">
                <div class="score-badge" :class="scoreLevel"><span class="score-val">{{ lastScore }}</span><span class="score-max">/10</span></div>
                <div class="score-level-text">{{ scoreLabel }}</div>
              </div>
              <MarkdownRenderer :content="lastComment"/>
            </div>
          </div>
        </div>

        <div v-if="userAnswers.length" class="msg-row user" v-for="(ua, i) in userAnswers" :key="i">
          <div class="msg-body"><div class="msg-sender">我</div><div class="msg-bubble user"><p>{{ ua }}</p></div></div>
          <div class="msg-avatar user-avatar-circle">{{ username.charAt(0).toUpperCase() }}</div>
        </div>
      </template>

      <div v-if="finished" class="finish-card">
        <div class="finish-icon">🎉</div><h3>面试完成</h3>
        <div class="finish-score"><span class="big-score">{{ avgScore }}</span><span class="score-label">/10</span></div>
        <p class="finish-desc">平均得分</p>
        <div class="finish-actions">
          <button class="btn-download" @click="downloadReport" :disabled="downloading">📥 {{ downloading?'生成中...':'下载报告 PDF' }}</button>
          <button class="btn-redo" @click="$router.push('/interview')">再来一轮</button>
          <button class="btn-history-link" @click="$router.push('/interview/history')">查看历史</button>
        </div>
      </div>
    </div>

    <div class="answer-bar" v-if="!finished && !questionStreaming">
      <div class="answer-row">
        <textarea v-model="answer" class="answer-input" placeholder="输入你的回答，Enter 发送..." @keydown.enter.prevent="submitAnswer" :disabled="evaluating" rows="3"></textarea>
        <button class="btn-submit" @click="submitAnswer" :disabled="!answer.trim() || evaluating">
          <template v-if="evaluating"><span class="spinner"></span> 评分中</template>
          <template v-else><svg viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2" width="16" height="16"><line x1="22" y1="2" x2="11" y2="13"/><polygon points="22 2 15 22 11 13 2 9 22 2"/></svg> 提交</template>
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '@/api'
import { useAuthStore } from '@/stores/auth'
import MarkdownRenderer from '@/components/MarkdownRenderer.vue'

const route = useRoute(); const router = useRouter()
const auth = useAuthStore(); const username = computed(() => auth.username||'U')

const sessionId = ref('')
const currentQuestion = ref(1)
const totalQuestions = ref(5)
const currentQuestionText = ref('')
const questionStreaming = ref(true)
const difficultyLabel = ref('社招')

const answer = ref('')
const userAnswers = ref<string[]>([])
const evaluating = ref(false)
const lastScore = ref<number|null>(null); const lastComment = ref('')
const finished = ref(false); const avgScore = ref(''); const recordId = ref<number|null>(null)
const downloading = ref(false); const chatRef = ref<HTMLElement>()

const progressPct = computed(() => Math.round((currentQuestion.value/totalQuestions.value)*100))
const scoreLevel = computed(() => { const s=lastScore.value||0; return s>=8?'high':s>=6?'mid':'low' })
const scoreLabel = computed(() => { const s=lastScore.value||0; return s>=8?'优秀':s>=6?'良好':'需改进' })

// 从 URL 获取初始数据（同步返回的第一题已在 query 中）
sessionId.value = route.query.sessionId as string || ''
totalQuestions.value = Number(route.query.total) || 5
difficultyLabel.value = (route.query.difficulty as string) || '社招'

// 流式加载第一题
async function loadFirstQuestion() {
  const q = route.query.question as string
  if (q) {
    // 已有题目直接显示
    questionStreaming.value = false
    currentQuestionText.value = q
  } else {
    // 等待流式
    questionStreaming.value = true
    currentQuestionText.value = '面试官正在出题...'
    // 两秒后如果还没内容就显示默认
    setTimeout(() => {
      if (questionStreaming.value && currentQuestionText.value === '面试官正在出题...') {
        questionStreaming.value = false
        currentQuestionText.value = '请介绍你最有挑战的一个项目经历？'
      }
    }, 5000)
  }
}

loadFirstQuestion()

async function submitAnswer() {
  if (!answer.value.trim() || evaluating.value || questionStreaming.value) return
  userAnswers.value.push(answer.value.trim())
  evaluating.value = true; lastScore.value = null

  try {
    const res: any = await api.post('/interview/answer', {
      sessionId: sessionId.value, answer: answer.value
    })
    const d = res.data; lastScore.value = d.score; lastComment.value = d.comment; answer.value = ''
    if (d.isLast) {
      finished.value = true; avgScore.value = d.avgScore; recordId.value = d.recordId
    } else {
      currentQuestion.value++
      questionStreaming.value = false
      currentQuestionText.value = d.nextQuestion || '面试官正在出题...'
    }
  } catch {}
  evaluating.value = false
}

async function downloadReport() {
  if (!recordId.value) return; downloading.value = true
  try { const res: any = await api.get(`/interview/report/${recordId.value}/download`); window.open(res.data,'_blank') } catch {} finally { downloading.value = false }
}
</script>

<style lang="scss" scoped>
.interview-session { max-width:760px; margin:0 auto; display:flex; flex-direction:column; height:calc(100vh - 54px - 64px); }
.session-hero { display:flex; align-items:center; gap:14px; margin-bottom:16px;
  .hero-avatar { width:46px;height:46px;border-radius:12px;background:linear-gradient(135deg,#6C5CE7,#8B7CF0);display:flex;align-items:center;justify-content:center;flex-shrink:0; }
  .hero-info h2 { font-size:20px;font-weight:700;color:var(--color-text);margin:0 0 6px; }
  .hero-meta { display:flex;align-items:center;gap:8px; .meta-tag { font-size:11px;padding:3px 10px;border-radius:10px;background:var(--color-primary-bg);color:var(--color-primary); } .meta-progress { font-size:12px;color:var(--color-text-muted); } }
}
.progress-bar-wrap { height:4px;border-radius:2px;background:var(--color-border);margin-bottom:22px;overflow:hidden; }
.progress-bar { height:100%;border-radius:2px;background:var(--color-primary);transition:width .4s ease; }
.interview-chat { flex:1;overflow-y:auto;padding:0 0 16px; }
.msg-row { display:flex;gap:12px;margin-bottom:20px;align-items:flex-start;
  &.ai { flex-direction:row; } &.user { flex-direction:row-reverse; .msg-body { align-items:flex-end; } .msg-sender { text-align:right; } }
  .msg-avatar { width:34px;height:34px;border-radius:10px;background:linear-gradient(135deg,#6C5CE7,#8B7CF0);display:flex;align-items:center;justify-content:center;flex-shrink:0; }
  .user-avatar-circle { background:linear-gradient(135deg,#43B88C,#5CC9A0);color:#fff;font-weight:600;font-size:14px; }
  .msg-body { display:flex;flex-direction:column;max-width:72%; }
  .msg-sender { font-size:11px;color:var(--color-text-muted);margin-bottom:4px;padding:0 4px; }
  .msg-bubble { padding:14px 18px;border-radius:8px 16px 16px 16px;font-size:14px;line-height:1.65;
    &.ai { background:var(--color-bubble-ai);color:var(--color-text); }
    &.user { background:var(--color-primary);color:#fff;border-radius:16px 8px 16px 16px; }
    &.score-bubble { background:var(--color-score-bg);border:1px solid rgba(108,92,231,.12); }
  }
  .question-label { font-size:11px;font-weight:600;color:var(--color-text-muted);margin-bottom:6px; }
  .question-text { margin:0;font-size:15px;line-height:1.7; }
}
.score-row { display:flex;align-items:center;gap:14px;margin-bottom:10px; }
.score-badge { width:56px;height:56px;border-radius:14px;display:flex;flex-direction:column;align-items:center;justify-content:center;color:#fff;flex-shrink:0;
  &.high { background:linear-gradient(135deg,#43B88C,#5CC9A0); } &.mid { background:linear-gradient(135deg,#e6a23c,#f0b84d); } &.low { background:linear-gradient(135deg,#f56c6c,#f87a7a); }
  .score-val { font-size:22px;font-weight:700;line-height:1; } .score-max { font-size:10px;opacity:.8; }
}
.score-level-text { font-size:15px;font-weight:600;color:var(--color-text); }
.finish-card { text-align:center;padding:40px 20px;
  .finish-icon { font-size:48px;margin-bottom:12px; } h3 { font-size:22px;color:var(--color-text);margin:0 0 12px; }
  .finish-score { display:flex;align-items:baseline;justify-content:center;gap:4px;margin-bottom:6px; .big-score { font-size:42px;font-weight:700;color:var(--color-primary); } .score-label { font-size:16px;color:var(--color-text-muted); } }
  .finish-desc { color:var(--color-text-secondary);font-size:13px;margin:0 0 20px; }
  .finish-actions { display:flex;gap:10px;justify-content:center;flex-wrap:wrap;
    .btn-download { padding:10px 22px;border-radius:10px;border:none;background:var(--color-primary);color:#fff;font-size:14px;cursor:pointer; &:hover { background:var(--color-primary-hover); } }
    .btn-redo { padding:10px 22px;border-radius:10px;border:1.5px solid var(--color-border);background:var(--color-card-bg);color:var(--color-text);font-size:14px;cursor:pointer; &:hover { border-color:var(--color-primary);color:var(--color-primary); } }
    .btn-history-link { padding:10px 22px;border:none;background:transparent;color:var(--color-text-secondary);font-size:13px;cursor:pointer; &:hover { color:var(--color-primary); } }
  }
}
.answer-bar { padding-top:12px;border-top:1px solid var(--color-border); }
.answer-row { display:flex;gap:10px;align-items:flex-end; }
.answer-input { flex:1;padding:12px 16px;border:1.5px solid var(--color-border);border-radius:14px;background:var(--color-card-bg);color:var(--color-text);font-size:14px;resize:none;outline:none;font-family:inherit;line-height:1.5;transition:all var(--transition);
  &::placeholder { color:var(--color-text-muted); } &:focus { border-color:var(--color-primary);box-shadow:0 0 0 3px rgba(108,92,231,.08); } }
.btn-submit { display:flex;align-items:center;gap:6px;padding:0 22px;height:44px;border-radius:14px;border:none;background:linear-gradient(135deg,#6C5CE7,#7C6EF0);color:#fff;font-size:14px;font-weight:500;cursor:pointer;transition:all var(--transition);flex-shrink:0;white-space:nowrap;
  &:hover:not(:disabled) { transform:translateY(-1px);box-shadow:0 4px 14px rgba(108,92,231,.25); } &:disabled { opacity:.4;cursor:default; } }
.spinner { width:14px;height:14px;border:2px solid rgba(255,255,255,.3);border-top-color:#fff;border-radius:50%;animation:spin .6s linear infinite; }
@keyframes spin { to { transform:rotate(360deg); } }
</style>
