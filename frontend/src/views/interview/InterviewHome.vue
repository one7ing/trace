<template>
  <div class="interview-page">
    <div class="page-header">
      <div class="header-left">
        <h1 class="page-title">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="title-icon" width="20" height="20"><polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"/></svg>
          你的专属面试官
        </h1>
        <p class="page-subtitle">选择面试方向，上传简历（可选），即刻开始</p>
      </div>
      <button class="btn-history" @click="$router.push('/interview/history')">历史记录 →</button>
    </div>

    <div class="section"><h3 class="section-title">面试方向</h3>
      <div class="tag-grid"><button v-for="d in directions" :key="d.value" class="tag-btn" :class="{ active: currentDirection === d.value }" @click="currentDirection = d.value">{{ d.label }}</button></div>
    </div>

    <div class="section"><h3 class="section-title">题目数量</h3>
      <div class="count-row">
        <button v-for="c in presetCounts" :key="c" class="count-btn" :class="{ active: questionCount === c }" @click="questionCount = c; customCount = ''">{{ c }} 题</button>
        <div class="custom-count-wrap" :class="{ focused: customFocused }">
          <input v-model="customCount" class="custom-count-input" type="number" min="1" max="50" placeholder="自定义" @focus="customFocused=true" @blur="customFocused=false; syncCustomCount()" @keyup.enter="syncCustomCount()"/>
        </div>
      </div>
    </div>

    <div class="section" v-if="hasKnowledgeBase">
      <h3 class="section-title">知识库选择</h3>
      <label class="kb-checkbox">
        <input type="checkbox" v-model="useKnowledgeBase" />
        <span class="kb-label">加上我的知识库出题</span>
        <span class="kb-hint">AI 将结合面试题库和你的知识库文档生成题目</span>
      </label>
    </div>

    <div class="section"><h3 class="section-title">上传简历（可选，AI 根据简历内容提问）</h3>
      <div class="upload-card" :class="{ 'has-file': resumeFile, dragging: isDragging }" @dragover.prevent="isDragging=true" @dragleave.prevent="isDragging=false" @drop.prevent="handleDrop" @click="triggerUpload">
        <template v-if="!resumeFile">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" width="28" height="28" class="upload-icon"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="12" y1="18" x2="12" y2="12"/><line x1="9" y1="15" x2="15" y2="15"/></svg>
          <p class="upload-text">点击或拖拽上传简历</p><p class="upload-hint">支持 PDF、Word、TXT，AI 将根据简历内容深入提问项目经验</p>
        </template>
        <template v-else><div class="file-row"><span>📄</span><span class="file-name">{{ resumeFile.name }}</span><span class="file-remove" @click.stop="resumeFile=null">✕</span></div></template>
      </div>
      <input ref="fileInputRef" type="file" accept=".pdf,.doc,.docx,.txt" style="display:none" @change="handleFileChange"/>
    </div>

    <div class="start-section">
      <button class="btn-start" @click="startInterview">
        <svg viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2" width="16" height="16"><polygon points="5 3 19 12 5 21 5 3"/></svg>
        开始面试（{{ questionCount }}题）
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
  { value:'backend', label:'后端开发' },
  { value:'frontend', label:'前端开发' },
  { value:'fullstack', label:'全栈开发' },
  { value:'data', label:'数据科学' },
  { value:'ops', label:'运维SRE' },
  { value:'mobile', label:'移动端' },
  { value:'embedded', label:'嵌入式' },
  { value:'qa', label:'测试开发' },
  { value:'security', label:'安全工程' },
]

const currentDirection = ref('backend')
const resumeFile = ref<File|null>(null)
const isDragging = ref(false)
const fileInputRef = ref<HTMLInputElement>()

const presetCounts = [5, 10, 20]
const questionCount = ref(5)
const customCount = ref('')
const customFocused = ref(false)
const useKnowledgeBase = ref(false)
const hasKnowledgeBase = ref(false)

function syncCustomCount() {
  const n = parseInt(customCount.value)
  if (n >= 1 && n <= 50) { questionCount.value = n; presetCounts.forEach(c => { if (c === n) customCount.value = '' }) }
  else if (customCount.value === '') { /* keep current */ }
  else { customCount.value = '' }
}

function triggerUpload() { fileInputRef.value?.click() }
function handleFileChange(e:Event) { const t=e.target as HTMLInputElement; if(t.files?.length) resumeFile.value=t.files[0] }
function handleDrop(e:DragEvent) { isDragging.value=false; if(e.dataTransfer?.files?.length) resumeFile.value=e.dataTransfer.files[0] }

async function startInterview() {
  try {
    const body: any = { industry: 'IT', questionCount: questionCount.value, useKnowledgeBase: useKnowledgeBase.value }
    if (resumeFile.value) {
      body.resumeText = await resumeFile.value.text()
    }
    const res: any = await api.post('/interview/start', body)
    const total = res.data.totalQuestions || questionCount.value
    router.push({
      path:'/interview/start',
      query:{ sessionId:res.data.sessionId, question:res.data.question, current:'1', total: String(total) }
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
.interview-page { max-width: 860px; margin: 0 auto; padding-bottom: 40px; }
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
.upload-card { display:flex; flex-direction:column; align-items:center; justify-content:center; padding:18px; border: 1.5px dashed var(--color-upload-border); border-radius:10px; min-height:70px; cursor:pointer; transition:all var(--transition); background:var(--color-card-bg);
  &:hover,&.dragging { border-color:#7B61FF; background:var(--color-active); }
  &.has-file { border-style:solid; border-color:#43B88C; background:var(--color-upload-bg); }
  .upload-icon { color:var(--color-text-muted); margin-bottom:2px; }
  .upload-text { font-size:13px; color:var(--color-text-secondary); margin:0; }
  .upload-hint { font-size:11px; color:#c8ccd4; margin:4px 0 0; }
}
.file-row { display:flex; align-items:center; gap:10px; font-size:13px; color:var(--color-text); .file-name { flex:1; word-break:break-all; } .file-remove { color:#f56c6c; cursor:pointer; font-size:14px; font-weight:600; } }
.start-section { text-align:center; margin-top:24px; }
.kb-checkbox { display:flex; align-items:center; gap:8px; cursor:pointer; font-size:13px; color:var(--color-text);
  input[type="checkbox"] { width:16px;height:16px;accent-color:#7B61FF;cursor:pointer; }
  .kb-label { font-weight:500; }
  .kb-hint { color:var(--color-text-muted); font-size:11px; }
}
.count-row { display:flex; align-items:center; gap:7px; flex-wrap:wrap; }
.count-btn { padding:6px 14px; border-radius:7px; border:1.5px solid var(--color-border); background:var(--color-card-bg); color:var(--color-text-secondary); font-size:12.5px; cursor:pointer; transition:all var(--transition);
  &:hover { border-color:var(--color-primary); color:#7B61FF; }
  &.active { border-color:#7B61FF; background:var(--color-active); color:#7B61FF; font-weight:500; }
}
.custom-count-wrap { display:flex; align-items:center; border:1.5px solid var(--color-border); border-radius:7px; padding:4px 8px; background:var(--color-card-bg); transition:all var(--transition);
  &.focused { border-color:#7B61FF; }
  .custom-count-input { width:60px; border:none; outline:none; background:transparent; color:var(--color-text); font-size:12.5px; text-align:center; &::placeholder { color:var(--color-text-muted); font-size:11px; }
    /* hide arrows */ &::-webkit-outer-spin-button,&::-webkit-inner-spin-button { -webkit-appearance:none;margin:0; } -moz-appearance:textfield; }
}
.btn-start { display:inline-flex; align-items:center; gap:8px; padding:12px 42px; border-radius:12px; border:none; background:#7B61FF; color:#fff; font-size:15px; font-weight:600; cursor:pointer; transition:all var(--transition); box-shadow:0 4px 16px rgba(123,97,255,.2);
  &:hover:not(:disabled) { background:#6a50f0; transform:translateY(-1px); box-shadow:0 6px 24px rgba(123,97,255,.3); }
  &:disabled { opacity:.4; cursor:default; box-shadow:none; } }
</style>
