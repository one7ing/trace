<template>
  <div class="practice-page">
    <div class="page-header">
      <div class="header-left">
        <h1 class="page-title">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="title-icon" width="20" height="20"><path d="M16 4h2a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h2"/><rect x="8" y="2" width="8" height="4" rx="1" ry="1"/></svg>
          刷题练习
        </h1>
        <p class="page-subtitle">选择方向，展示全部题目，自由选答后提交</p>
      </div>
      <button class="btn-history" @click="$router.push('/practice/history')">刷题记录 →</button>
    </div>

    <div class="section"><h3 class="section-title">刷题方向</h3>
      <div class="tag-grid"><button v-for="d in directions" :key="d.value" class="tag-btn" :class="{ active: currentTopic === d.value }" @click="currentTopic = d.value">{{ d.label }}</button></div>
    </div>

    <div class="section" v-if="hasKnowledgeBase">
      <label class="kb-checkbox">
        <input type="checkbox" v-model="useKnowledgeBase" />
        <span class="kb-label">结合我的知识库判题</span>
        <span class="kb-hint">AI 判题时将参考你的知识库文档</span>
      </label>
    </div>

    <div class="start-section">
      <button class="btn-start" @click="startPractice">
        <svg viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2" width="16" height="16"><polygon points="5 3 19 12 5 21 5 3"/></svg>
        开始刷题
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import api from '@/api'

const router = useRouter()
const directions = [
  { value:'Redis', label:'Redis' },
  { value:'Java多线程', label:'Java多线程' },
  { value:'Java基础', label:'Java基础' },
  { value:'分布式', label:'分布式' },
  { value:'MySQL', label:'MySQL' },
  { value:'SSM框架', label:'SSM框架' },
  { value:'微服务', label:'微服务' },
  { value:'消息中间件', label:'消息中间件' },
]

const currentTopic = ref('Redis')
const useKnowledgeBase = ref(false)
const hasKnowledgeBase = ref(false)

async function startPractice() {
  try {
    const body: any = { topic: currentTopic.value, questionCount: 100, useKnowledgeBase: useKnowledgeBase.value }
    const res: any = await api.post('/practice/start', body)
    router.push({
      path:'/practice/start',
      query:{ sessionId:res.data.sessionId, questions: JSON.stringify(res.data.questions), total: String(res.data.totalQuestions) }
    })
  } catch {}
}

onMounted(async () => {
  try {
    const res: any = await api.get('/knowledge-base/has-files')
    hasKnowledgeBase.value = res.data || false
  } catch {}
})
</script>

<style lang="scss" scoped>
.practice-page { max-width: 860px; margin: 0 auto; padding-bottom: 40px; }
.page-header { display:flex; align-items:flex-start; justify-content:space-between; margin-bottom:24px;
  .page-title { display:flex; align-items:center; gap:8px; font-size:20px; font-weight:700; color:var(--color-text); margin:0 0 6px; .title-icon { color:#7B61FF; } }
  .page-subtitle { font-size:13px; color:var(--color-text-muted); margin:0; }
  .btn-history { padding:7px 16px; border-radius:8px; border: 1.5px solid var(--color-border); background:var(--color-card-bg); color:var(--color-text-secondary); font-size:13px; cursor:pointer; white-space:nowrap; transition:all var(--transition);
    &:hover { border-color:#7B61FF; color:#7B61FF; } }
}
.section { margin-bottom:20px; }
.section-title { font-size:13px; font-weight:600; color:var(--color-text); margin:0 0 10px; }
.tag-grid { display:flex; flex-wrap:wrap; gap:7px; }
.tag-btn { padding:6px 14px; border-radius:7px; border: 1.5px solid var(--color-border); background:var(--color-card-bg); color:var(--color-text-secondary); font-size:12.5px; cursor:pointer; transition:all var(--transition);
  &:hover { border-color:var(--color-primary); color:#7B61FF; }
  &.active { border-color:#7B61FF; background:var(--color-active); color:#7B61FF; font-weight:500; }
}
.kb-checkbox { display:flex; align-items:center; gap:8px; cursor:pointer; font-size:13px; color:var(--color-text);
  input[type="checkbox"] { width:16px;height:16px;accent-color:#7B61FF;cursor:pointer; }
  .kb-label { font-weight:500; }
  .kb-hint { color:var(--color-text-muted); font-size:11px; }
}
.start-section { text-align:center; margin-top:24px; }
.btn-start { display:inline-flex; align-items:center; gap:8px; padding:12px 42px; border-radius:12px; border:none; background:#7B61FF; color:#fff; font-size:15px; font-weight:600; cursor:pointer; transition:all var(--transition); box-shadow:0 4px 16px rgba(123,97,255,.2);
  &:hover:not(:disabled) { background:#6a50f0; transform:translateY(-1px); box-shadow:0 6px 24px rgba(123,97,255,.3); }
  &:disabled { opacity:.4; cursor:default; box-shadow:none; } }
</style>
