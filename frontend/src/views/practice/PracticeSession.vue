<template>
  <div class="practice-session">
    <div class="session-hero">
      <div class="hero-avatar">
        <svg viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2" width="20" height="20"><path d="M16 4h2a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h2"/></svg>
      </div>
      <div class="hero-info">
        <h2>刷题练习</h2>
        <span class="meta-progress">共 {{ totalQuestions }} 题 · 已答 {{ answeredCount }}</span>
      </div>
      <button v-if="!finished" class="btn-abort" @click="handleAbort">中断刷题</button>
    </div>

    <div class="all-mode-chat" ref="chatRef">
      <div v-for="(q, i) in questions" :key="i" class="question-card" :class="{ answered: allAnswers[i+1] && allAnswers[i+1].trim() }">
        <div class="q-header">第 {{ i + 1 }} 题</div>
        <div class="q-body">{{ q.question }}</div>
        <textarea v-model="allAnswers[i+1]" class="q-answer" placeholder="输入你的回答（可跳过）..." rows="3" :disabled="finished" @input="updateCount"></textarea>
      </div>
      <div v-if="!finished" class="submit-all-wrap">
        <button class="btn-submit-all" @click="submitAll" :disabled="submitting || answeredCount === 0">
          {{ submitting ? '提交中...' : `提交（${answeredCount} 题）` }}
        </button>
      </div>
    </div>

    <div v-if="finished" class="finish-card">
      <div class="finish-icon">🎉</div><h3>已提交 {{ answeredCount }} 题</h3>
      <div class="finish-actions">
        <button class="btn-primary-link" @click="$router.push(`/practice/history/${recordId}`)">📋 查看判题结果</button>
        <button class="btn-redo" @click="$router.push('/practice')">再来一轮</button>
        <button class="btn-history-link" @click="$router.push('/practice/history')">查看历史</button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRoute, useRouter, onBeforeRouteLeave } from 'vue-router'
import api from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'

const route = useRoute(); const router = useRouter()
const sessionId = ref(route.query.sessionId as string || '')
const totalQuestions = ref(Number(route.query.total) || 0)
const questions = ref<any[]>([])
const allAnswers = ref<Record<number, string>>({})

const submitting = ref(false)
const finished = ref(false)
const recordId = ref<number|null>(null)
const chatRef = ref<HTMLElement>()

const answeredCount = ref(0)
function updateCount() {
  let c = 0
  for (const k in allAnswers.value) {
    if (allAnswers.value[Number(k)]?.trim()) c++
  }
  answeredCount.value = c
}

function loadQuestions() {
  try {
    const raw = route.query.questions as string
    if (raw) {
      questions.value = JSON.parse(raw)
      questions.value.forEach((q: any) => { allAnswers.value[q.sequenceNum] = '' })
    }
  } catch {}
}
loadQuestions()

async function handleAbort() {
  try {
    const ct = setTimeout(() => ElMessageBox.close(), 3000)
    await ElMessageBox.confirm('确定中断？已填内容不会保存。', '中断刷题',
      { confirmButtonText: '确认中断', cancelButtonText: '继续', type: 'warning' })
    clearTimeout(ct)
  } catch { return }
  try { await api.post(`/practice/${sessionId.value}/abort`) } catch {}
  finished.value = true
  router.push('/practice')
}

onBeforeRouteLeave((_to, _from, next) => {
  if (finished.value) { next(); return }
  ElMessageBox.confirm('刷题进行中，请先点击「中断刷题」。', '提示',
    { confirmButtonText: '我知道了', showCancelButton: false, type: 'warning' })
    .then(() => next(false)).catch(() => next(false))
})

async function submitAll() {
  if (submitting.value || answeredCount.value === 0) return
  submitting.value = true

  // 只提交有内容的答案
  const toSubmit: Record<number, string> = {}
  for (const k in allAnswers.value) {
    const v = allAnswers.value[Number(k)]?.trim()
    if (v) toSubmit[Number(k)] = v
  }

  try {
    const res: any = await api.post('/practice/answer', {
      sessionId: sessionId.value, allAnswers: toSubmit
    })
    const d = res.data
    if (d.isLast) {
      finished.value = true
      recordId.value = d.recordId
      ElMessage({ message: '判题中，请稍后查看结果', type: 'success', duration: 3000 })
      router.push(`/practice/history/${d.recordId}`)
    }
  } catch {}
  submitting.value = false
}
</script>

<style lang="scss" scoped>
.practice-session { max-width:760px; margin:0 auto; display:flex; flex-direction:column; height:calc(100vh - 54px - 64px); }
.session-hero { display:flex; align-items:center; gap:14px; margin-bottom:16px;
  .hero-avatar { width:46px;height:46px;border-radius:12px;background:linear-gradient(135deg,#43B88C,#5CC9A0);display:flex;align-items:center;justify-content:center;flex-shrink:0; }
  .hero-info h2 { font-size:17px;font-weight:700;color:var(--color-text);margin:0 0 4px; }
  .meta-progress { font-size:12px;color:var(--color-text-muted); }
}
.all-mode-chat { flex:1;overflow-y:auto;padding:0 0 16px; }
.question-card { padding:16px; border:1.5px solid var(--color-border); border-radius:10px; margin-bottom:14px; background:var(--color-card-bg); transition: all .2s;
  &.answered { border-left: 3px solid #43B88C; }
  .q-header { font-size:12px; font-weight:600; color:var(--color-primary); margin-bottom:8px; }
  .q-body { font-size:14px; color:var(--color-text); line-height:1.6; margin-bottom:12px; }
  .q-answer { width:100%; padding:10px 14px; border:1.5px solid var(--color-border); border-radius:8px; background:var(--color-bg); color:var(--color-text); font-size:13px; resize:vertical; outline:none; font-family:inherit; line-height:1.5; box-sizing:border-box;
    &:focus { border-color:var(--color-primary); }
    &::placeholder { color:var(--color-text-muted); }
  }
}
.submit-all-wrap { text-align:center; margin-top:16px; }
.btn-submit-all { padding:12px 48px; border-radius:12px; border:none; background:linear-gradient(135deg,#43B88C,#5CC9A0); color:#fff; font-size:15px; font-weight:600; cursor:pointer; transition:all var(--transition);
  &:hover:not(:disabled) { transform:translateY(-1px); box-shadow:0 4px 14px rgba(67,184,140,.25); }
  &:disabled { opacity:.4; cursor:default; }
}
.btn-abort { margin-left:auto;padding:6px 16px;border-radius:8px;border:1.5px solid #f56c6c;background:transparent;color:#f56c6c;font-size:12px;cursor:pointer;flex-shrink:0;transition:all var(--transition);
  &:hover { background:#fef0f0; }
}
.finish-card { text-align:center;padding:40px 20px;
  .finish-icon { font-size:48px;margin-bottom:12px; } h3 { font-size:22px;color:var(--color-text);margin:0 0 12px; }
  .finish-actions { display:flex;gap:10px;justify-content:center;flex-wrap:wrap;
    .btn-primary-link { padding:10px 22px;border-radius:10px;border:none;background:#43B88C;color:#fff;font-size:14px;cursor:pointer; &:hover { background:#38a67a; } }
    .btn-redo { padding:10px 22px;border-radius:10px;border:1.5px solid var(--color-border);background:var(--color-card-bg);color:var(--color-text);font-size:14px;cursor:pointer; &:hover { border-color:var(--color-primary);color:var(--color-primary); } }
    .btn-history-link { padding:10px 22px;border:none;background:transparent;color:var(--color-text-secondary);font-size:13px;cursor:pointer; &:hover { color:var(--color-primary); } }
  }
}
</style>
