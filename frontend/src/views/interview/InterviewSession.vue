<template>
  <div class="interview-session">
    <div class="session-hero">
      <div class="hero-avatar">
        <svg viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2" width="20" height="20"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg>
      </div>
      <div class="hero-info">
        <h2>AI 面试官</h2>
        <span class="meta-progress">第 {{ currentQuestion }} / {{ totalQuestions }} 题</span>
      </div>
      <button v-if="!finished" class="btn-abort" @click="handleAbort">中断面试</button>
    </div>
    <div class="progress-bar-wrap"><div class="progress-bar" :style="{ width: progressPct + '%' }"></div></div>

    <div class="interview-chat" ref="chatRef">
      <div class="msg-row" :class="m.role" v-for="(m, i) in chatLog" :key="i">
  <div class="msg-avatar ai-avatar" v-if="m.role==='ai'">
    <svg viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2" width="14" height="14"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg>
  </div>
  <div class="msg-body">
    <div class="msg-sender">{{ m.role==='ai' ? 'AI 面试官' : '' }}</div>
    <div class="msg-bubble" :class="[m.role, { comment: m.type==='comment' }]">
      <MarkdownRenderer v-if="m.role==='ai'" :content="m.content"/>
      <template v-else>{{ m.content }}</template>
    </div>
  </div>
  <div class="msg-avatar user-avatar" v-if="m.role==='user'" :style="userAvatarImg ? { backgroundImage: 'url('+userAvatarImg+')', backgroundSize:'cover' } : {}">
    <span v-if="!userAvatarImg">{{ username.charAt(0).toUpperCase() }}</span>
  </div>
</div>

      <div v-if="finished" class="finish-card">
          <div class="finish-icon">🎉</div><h3>面试完成</h3>
          <div v-if="finalEvaluation" class="final-eval">
            <MarkdownRenderer :content="finalEvaluation"/>
          </div>
        <div class="finish-actions">
          <button class="btn-download" @click="downloadReport" :disabled="downloading">📥 {{ downloading?'生成中...':'下载报告 PDF' }}</button>
          <button class="btn-primary-link" @click="$router.push(`/interview/history/${recordId}`)">📋 查看详细点评</button>
          <button class="btn-redo" @click="$router.push('/interview')">再来一轮</button>
          <button class="btn-history-link" @click="$router.push('/interview/history')">查看历史</button>
        </div>
      </div>
    </div>

    <div class="answer-bar" v-if="!finished && !questionStreaming">
      <div class="answer-row">
        <button class="btn-voice"
                :class="{ recording: isRecording }"
                @click="toggleVoice" :disabled="evaluating"
                :title="isRecording ? '停止录音' : '语音输入'">
          <svg viewBox="0 0 24 24" fill="none" :stroke="isRecording ? '#fff' : '#6C5CE7'" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" width="16" height="16">
            <path d="M12 1a3 3 0 0 0-3 3v8a3 3 0 0 0 6 0V4a3 3 0 0 0-3-3z"/><path d="M19 10v2a7 7 0 0 1-14 0v-2"/><line x1="12" y1="19" x2="12" y2="23"/>
          </svg>
        </button>
        <textarea v-model="answer" class="answer-input" placeholder="输入你的回答..." @keydown.enter.prevent="submitAnswer" :disabled="evaluating" rows="2"></textarea>
        <button class="btn-submit" @click="submitAnswer" :disabled="!answer.trim() || evaluating">
          <template v-if="evaluating"><span class="spinner"></span> 思考中</template>
          <template v-else><svg viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2" width="14" height="14"><line x1="22" y1="2" x2="11" y2="13"/><polygon points="22 2 15 22 11 13 2 9 22 2"/></svg> 发送</template>
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRoute, useRouter, onBeforeRouteLeave } from 'vue-router'
import api from '@/api'
import { useAuthStore } from '@/stores/auth'
import MarkdownRenderer from '@/components/MarkdownRenderer.vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const route = useRoute(); const router = useRouter()
const auth = useAuthStore(); const username = computed(() => auth.username||'U')
const userAvatarImg = localStorage.getItem('trace-user-avatar-img') || ''

const sessionId = ref('')
const currentQuestion = ref(1)
const totalQuestions = ref(20)
const questionStreaming = ref(true)

const answer = ref('')
interface ChatItem { role:'ai'|'user', type?:string, content:string }
const chatLog = ref<ChatItem[]>([])
const evaluating = ref(false)
const finished = ref(false)
const recordId = ref<number|null>(null)
const finalEvaluation = ref('')
const downloading = ref(false); const chatRef = ref<HTMLElement>()
const isRecording = ref(false)
let recognition: any = null

const progressPct = computed(() => Math.round((currentQuestion.value/totalQuestions.value)*100))

// 从 URL 获取初始数据（同步返回的第一题已在 query 中）
sessionId.value = route.query.sessionId as string || ''
totalQuestions.value = Number(route.query.total) || 5

// 加载第一题
function loadFirstQuestion() {
  const q = route.query.question as string
  questionStreaming.value = false
  chatLog.value.push({ role:'ai', type:'question', content: q || '面试官正在出题...' })
}

loadFirstQuestion()

/** 中断面试 */
async function handleAbort() {
  try {
    // 3 秒后自动关闭弹窗
    const closeTimer = setTimeout(() => ElMessageBox.close(), 3000)
    await ElMessageBox.confirm(
      '确定要中断当前面试吗？已答题目不会保存。',
      '中断面试',
      { confirmButtonText: '确认中断', cancelButtonText: '继续面试', type: 'warning' }
    )
    clearTimeout(closeTimer)
  } catch { return }
  try { await api.post(`/interview/${sessionId.value}/abort`) } catch {}
  finished.value = true
  ElMessage({
    message: '面试已中断，下次再来！💪 随时准备好迎接你的下一次挑战。',
    type: 'info',
    duration: 4000,
    showClose: true
  })
  router.push('/interview')
}

/** 路由离开守卫：面试进行中拦截跳转 */
onBeforeRouteLeave((_to, _from, next) => {
  if (finished.value) { next(); return }
  ElMessageBox.confirm(
    '面试正在进行中，请先点击「中断面试」后再离开。',
    '提示',
    { confirmButtonText: '我知道了', showCancelButton: false, type: 'warning' }
  ).then(() => next(false)).catch(() => next(false))
})

// 语音转文字
function toggleVoice() {
  if (isRecording.value) { stopVoice(); return }
  const SpeechRecognition = (window as any).SpeechRecognition || (window as any).webkitSpeechRecognition
  if (!SpeechRecognition) { return }
  recognition = new SpeechRecognition()
  recognition.lang = 'zh-CN'
  recognition.interimResults = true
  recognition.onresult = (e: any) => {
    let t = ''
    for (let i = e.resultIndex; i < e.results.length; i++) t += e.results[i][0].transcript
    answer.value = t
  }
  recognition.onend = () => { isRecording.value = false }
  recognition.start()
  isRecording.value = true
}
function stopVoice() { if (recognition) { recognition.stop(); isRecording.value = false } }

async function submitAnswer() {
  if (!answer.value.trim() || evaluating.value || questionStreaming.value) return
  const ans = answer.value.trim()
  chatLog.value.push({ role:'user', content: ans })
  answer.value = ''
  evaluating.value = true

  try {
    const res: any = await api.post('/interview/answer', {
      sessionId: sessionId.value, answer: ans
    })
    const d = res.data
    if (d.isLast) {
      finished.value = true
      recordId.value = d.recordId
      // 立即跳转到结束页面，异步生成评价
      ElMessage({ message: '面试报告生成中，请稍后查看 📋', type: 'success', duration: 3000, showClose: true })
      router.push(`/interview/history/${d.recordId}`)
    } else {
      currentQuestion.value++
      questionStreaming.value = false
      if (d.nextQuestion) chatLog.value.push({ role:'ai', type:'question', content: d.nextQuestion })
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
  .hero-info h2 { font-size:17px;font-weight:700;color:var(--color-text);margin:0 0 4px; }
  .hero-meta { display:flex;align-items:center;gap:8px; .meta-tag { font-size:11px;padding:3px 10px;border-radius:10px;background:var(--color-primary-bg);color:var(--color-primary); } .meta-progress { font-size:12px;color:var(--color-text-muted); } }
}
.progress-bar-wrap { height:4px;border-radius:2px;background:var(--color-border);margin-bottom:22px;overflow:hidden; }
.progress-bar { height:100%;border-radius:2px;background:var(--color-primary);transition:width .4s ease; }
.interview-chat { flex:1;overflow-y:auto;padding:0 0 16px; }
.msg-row { display:flex;gap:12px;margin-bottom:20px;align-items:flex-start;
  &.ai { flex-direction:row; } &.user { flex-direction:row; justify-content:flex-end; .msg-body { align-items:flex-end; } }
  .msg-avatar { width:30px;height:30px;border-radius:9px;display:flex;align-items:center;justify-content:center;flex-shrink:0;
    &.ai-avatar { background:linear-gradient(135deg,#6C5CE7,#8B7CF0); }
    &.user-avatar { background:linear-gradient(135deg,#43B88C,#5CC9A0);color:#fff;font-weight:600;font-size:13px; }
  }
  .msg-body { display:flex;flex-direction:column;max-width:72%; }
  .msg-sender { font-size:11px;color:var(--color-text-muted);margin-bottom:4px;padding:0 4px; }
  .msg-bubble { padding:10px 14px;border-radius:8px 16px 16px 16px;font-size:13px;line-height:1.6;
    &.ai { background:var(--color-bubble-ai);color:var(--color-text);border-radius:8px 16px 16px 16px; }
    &.user { background:var(--color-primary);color:#fff;border-radius:16px 8px 16px 16px;font-size:13px; }
    &.comment { background:var(--color-analysis-bg);border:1px solid var(--color-analysis-border);font-size:12.5px; }
  }
}
.score-row { display:flex;align-items:center;gap:14px;margin-bottom:10px; }
.score-badge { width:56px;height:56px;border-radius:14px;display:flex;flex-direction:column;align-items:center;justify-content:center;color:#fff;flex-shrink:0;
  &.high { background:linear-gradient(135deg,#43B88C,#5CC9A0); } &.mid { background:linear-gradient(135deg,#e6a23c,#f0b84d); } &.low { background:linear-gradient(135deg,#f56c6c,#f87a7a); }
  .score-val { font-size:22px;font-weight:700;line-height:1; } .score-max { font-size:10px;opacity:.8; }
}
.score-level-text { font-size:15px;font-weight:600;color:var(--color-text); }
.finish-card { text-align:center;padding:40px 20px;
  .finish-icon { font-size:48px;margin-bottom:12px; } h3 { font-size:22px;color:var(--color-text);margin:0 0 12px; }
  .final-eval { text-align:left;max-width:600px;margin:0 auto 20px;padding:16px 20px;background:var(--color-analysis-bg);border-radius:12px;border:1px solid var(--color-analysis-border); }
  .finish-desc { color:var(--color-text-secondary);font-size:13px;margin:0 0 20px; }
  .finish-actions { display:flex;gap:10px;justify-content:center;flex-wrap:wrap;
    .btn-download { padding:10px 22px;border-radius:10px;border:none;background:var(--color-primary);color:#fff;font-size:14px;cursor:pointer; &:hover { background:var(--color-primary-hover); } }
    .btn-primary-link { padding:10px 22px;border-radius:10px;border:none;background:#43B88C;color:#fff;font-size:14px;cursor:pointer; &:hover { background:#38a67a; } }
    .btn-redo { padding:10px 22px;border-radius:10px;border:1.5px solid var(--color-border);background:var(--color-card-bg);color:var(--color-text);font-size:14px;cursor:pointer; &:hover { border-color:var(--color-primary);color:var(--color-primary); } }
    .btn-history-link { padding:10px 22px;border:none;background:transparent;color:var(--color-text-secondary);font-size:13px;cursor:pointer; &:hover { color:var(--color-primary); } }
  }
}
.btn-abort {
  margin-left:auto;padding:6px 16px;border-radius:8px;
  border:1.5px solid #f56c6c;background:transparent;color:#f56c6c;
  font-size:12px;cursor:pointer;flex-shrink:0;transition:all var(--transition);
  &:hover { background:#fef0f0; }
}
.answer-bar { padding-top:10px;border-top:1px solid var(--color-border); }
.answer-row { display:flex;gap:8px;align-items:center; }
.answer-input { flex:1;padding:8px 14px;border:1.5px solid var(--color-border);border-radius:14px;background:var(--color-card-bg);color:var(--color-text);font-size:13px;resize:none;outline:none;font-family:inherit;line-height:1.5;transition:all var(--transition);height:42px;
  &::placeholder { color:var(--color-text-muted); } &:focus { border-color:var(--color-primary);box-shadow:0 0 0 3px rgba(108,92,231,.08); } }
.btn-voice {
  width:38px;height:42px;border-radius:12px;border:1.5px solid var(--color-border);
  background:var(--color-card-bg);cursor:pointer;display:flex;align-items:center;justify-content:center;
  flex-shrink:0;transition:all var(--transition);
  &:hover { border-color:var(--color-primary); }
  &.recording { background:#f56c6c;border-color:#f56c6c;animation:pulse 1.2s infinite; }
}
@keyframes pulse { 0%,100%{box-shadow:0 0 0 0 rgba(245,108,108,.3)} 50%{box-shadow:0 0 0 8px rgba(245,108,108,0)} }
.btn-submit { display:flex;align-items:center;gap:6px;padding:0 18px;height:42px;border-radius:14px;border:none;background:linear-gradient(135deg,#6C5CE7,#7C6EF0);color:#fff;font-size:13px;font-weight:500;cursor:pointer;transition:all var(--transition);flex-shrink:0;white-space:nowrap;
  &:hover:not(:disabled) { transform:translateY(-1px);box-shadow:0 4px 14px rgba(108,92,231,.25); } &:disabled { opacity:.4;cursor:default; } }
.spinner { width:14px;height:14px;border:2px solid rgba(255,255,255,.3);border-top-color:#fff;border-radius:50%;animation:spin .6s linear infinite; }
@keyframes spin { to { transform:rotate(360deg); } }
</style>
