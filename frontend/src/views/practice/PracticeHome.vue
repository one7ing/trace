<template>
  <div class="practice-page">
    <div class="page-header">
      <div class="header-left">
        <h1 class="page-title">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="title-icon" width="20" height="20"><path d="M16 4h2a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h2"/><rect x="8" y="2" width="8" height="4" rx="1" ry="1"/></svg>
          刷题练习
        </h1>
        <p class="page-subtitle">选择系统方向或自定义题库，自由选答后 AI 判题</p>
      </div>
      <button class="btn-history" @click="$router.push('/practice/history')">刷题记录 →</button>
    </div>

    <!-- 刷题源选择：系统题库 / 我的题库 -->
    <div class="section"><h3 class="section-title">刷题来源</h3>
      <div class="source-tabs">
        <button class="source-tab" :class="{ active: practiceSource === 'system' }" @click="practiceSource = 'system'">系统题库</button>
        <button class="source-tab" :class="{ active: practiceSource === 'my' }" @click="practiceSource = 'my'">我的题库</button>
      </div>
    </div>

    <!-- 系统题库方向 -->
    <div class="section" v-if="practiceSource === 'system'">
      <h3 class="section-title">刷题方向</h3>
      <div class="tag-grid">
        <button v-for="d in directions" :key="d.value" class="tag-btn" :class="{ active: currentTopic === d.value }" @click="currentTopic = d.value">{{ d.label }}</button>
      </div>
    </div>

    <!-- 我的题库 -->
    <div class="section" v-if="practiceSource === 'my'">
      <div class="bank-header">
        <h3 class="section-title">我的题库 · {{ banks.length }} 个</h3>
        <button class="btn-create-bank" @click="showCreateBank = true">+ 创建题库</button>
      </div>
      <div class="bank-grid" v-if="banks.length">
        <div v-for="b in banks" :key="b.topic" class="bank-card" :class="{ active: selectedBankTopic === b.topic }" @click="selectedBankTopic = b.topic">
          <div class="bank-info">
            <span class="bank-name">{{ b.topic }}</span>
            <span class="bank-count">{{ b.count }} 题</span>
          </div>
          <div class="bank-actions">
            <button class="btn-bank-manage" @click.stop="openManageBank(b)" title="管理题目">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14"><path d="M12 20h9"/><path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z"/></svg>
            </button>
            <button class="btn-bank-delete" @click.stop="deleteBank(b.topic)" title="删除题库">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" width="14" height="14"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/></svg>
            </button>
          </div>
        </div>
      </div>
      <div class="empty" v-else><p>暂无自定义题库</p><p class="empty-hint">点击"+ 创建题库"开始</p></div>
    </div>

    <!-- 开始刷题 -->
    <div class="start-section">
      <button class="btn-start" @click="startPractice" :disabled="practiceSource === 'my' && !selectedBankTopic">
        <svg viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2" width="16" height="16"><polygon points="5 3 19 12 5 21 5 3"/></svg>
        开始刷题
      </button>
    </div>

    <!-- 创建题库弹窗 -->
    <el-dialog v-model="showCreateBank" title="创建题库" width="560px" destroy-on-close @closed="resetCreateForm">
      <el-input v-model="newBankName" placeholder="输入题库名称" maxlength="50" style="margin-bottom:14px"/>
      <!-- 导入方式切换 -->
      <div class="import-tabs">
        <button class="import-tab" :class="{ active: importMode === 'text' }" @click="importMode = 'text'">粘贴文本</button>
        <button class="import-tab" :class="{ active: importMode === 'file' }" @click="importMode = 'file'">上传文件</button>
      </div>
      <!-- 文本模式 -->
      <div v-if="importMode === 'text'" style="margin-top:10px">
        <el-input v-model="newBankContent" type="textarea" :rows="8" placeholder="粘贴 Q&A 格式题目，支持格式：&#10;Q: 问题&#10;A: 答案&#10;&#10;或&#10;1. 问题&#10;答案内容"/>
      </div>
      <!-- 文件模式 -->
      <div v-else style="margin-top:10px">
        <input type="file" accept=".pdf,.txt,.md" @change="handleFileSelect" ref="fileInputRef" style="display:none"/>
        <div class="file-upload-zone" :class="{ hasFile: uploadFile }" @click="fileInputRef?.click()">
          <template v-if="uploadFile">
            <span class="file-name">📄 {{ uploadFile.name }}</span>
            <button class="btn-file-clear" @click.stop="uploadFile = null; fileInputRef!.value = ''">✕</button>
          </template>
          <template v-else>
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" width="28" height="28" style="color:var(--color-text-muted)"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/></svg>
            <span class="file-hint">点击选择 PDF / TXT 文件</span>
          </template>
        </div>
      </div>
      <template #footer>
        <el-button @click="showCreateBank = false">取消</el-button>
        <el-button type="primary" @click="createBank" :disabled="!canCreate" :loading="creating">创建题库</el-button>
      </template>
    </el-dialog>

    <!-- 题库题目管理弹窗 -->
    <el-dialog v-model="showManageBank" :title="'管理题库：' + manageBankTopic" width="650px" destroy-on-close>
      <!-- 添加题目 -->
      <div class="add-question-row">
        <el-input v-model="newQuestion" type="textarea" :rows="2" placeholder="输入题目文本" style="margin-bottom:8px"/>
        <el-input v-model="newAnswer" type="textarea" :rows="2" placeholder="参考答案（选填，留空由 AI 独立判断）" style="margin-bottom:8px"/>
        <el-button type="primary" size="small" @click="addQuestion" :disabled="!newQuestion.trim()">添加题目</el-button>
      </div>
      <el-divider/>
      <!-- 题目列表 -->
      <div v-if="bankQuestions.length" class="question-list">
        <div v-for="q in bankQuestions" :key="q.id" class="question-item">
          <div class="q-text">{{ q.question }}</div>
          <div class="q-answer" v-if="q.referenceAnswer">📝 {{ q.referenceAnswer.substring(0, 60) }}{{ q.referenceAnswer.length > 60 ? '...' : '' }}</div>
          <button class="btn-q-del" @click="deleteQuestion(q.id)" title="删除题目">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="12" height="12"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
          </button>
        </div>
      </div>
      <div v-else class="empty" style="padding:20px"><p>暂无题目，请添加</p></div>
      <template #footer>
        <el-button @click="showManageBank = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import api from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()

// 系统题库方向
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

const practiceSource = ref<'system'|'my'>('system')
const currentTopic = ref('Redis')
const selectedBankTopic = ref('')

// 我的题库
const banks = ref<any[]>([])
const showCreateBank = ref(false)
const newBankName = ref('')
const newBankContent = ref('')
const importMode = ref<'text'|'file'>('text')
const uploadFile = ref<File | null>(null)
const fileInputRef = ref<HTMLInputElement>()
const creating = ref(false)
const canCreate = computed(() => {
  if (!newBankName.value.trim()) return false
  if (importMode.value === 'text') return newBankContent.value.trim().length > 0
  return uploadFile.value !== null
})

// 题库管理
const showManageBank = ref(false)
const manageBankTopic = ref('')
const bankQuestions = ref<any[]>([])
const newQuestion = ref('')
const newAnswer = ref('')

// ── 刷题 ──
async function startPractice() {
  try {
    const body: any = {
      questionCount: 100,
    }
    if (practiceSource.value === 'my') {
      body.bankTopic = selectedBankTopic.value
    } else {
      body.topic = currentTopic.value
    }
    const res: any = await api.post('/practice/start', body)
    router.push({
      path:'/practice/start',
      query:{ sessionId:res.data.sessionId, questions: JSON.stringify(res.data.questions), total: String(res.data.totalQuestions) }
    })
  } catch {}
}

// ── 题库列表 ──
async function fetchBanks() {
  try {
    const res: any = await api.get('/practice/banks')
    banks.value = res.data || []
  } catch {}
}

// ── 创建题库 ──
async function createBank() {
  const name = newBankName.value.trim()
  if (!name || creating.value) return
  creating.value = true
  try {
    if (importMode.value === 'text') {
      // 文本模式
      const res: any = await api.post('/practice/banks', {
        name,
        content: newBankContent.value.trim()
      })
      ElMessage.success(`题库「${name}」已创建，共 ${res.data.count} 题`)
    } else if (uploadFile.value) {
      // 文件模式
      const form = new FormData()
      form.append('file', uploadFile.value)
      form.append('name', name)
      const res: any = await api.post('/practice/banks/upload', form, {
        headers: { 'Content-Type': 'multipart/form-data' }
      })
      ElMessage.success(`题库「${name}」已创建，共 ${res.data.count} 题`)
    }
    showCreateBank.value = false
    resetCreateForm()
    fetchBanks()
  } catch {} finally {
    creating.value = false
  }
}
function handleFileSelect(e: Event) {
  const t = e.target as HTMLInputElement
  uploadFile.value = t.files?.[0] || null
}
function resetCreateForm() {
  newBankName.value = ''
  newBankContent.value = ''
  uploadFile.value = null
  importMode.value = 'text'
  if (fileInputRef.value) fileInputRef.value.value = ''
}

// ── 删除题库 ──
async function deleteBank(topic: string) {
  try {
    await ElMessageBox.confirm(`确定删除题库「${topic}」及其所有题目？`, '确认', { type: 'warning' })
  } catch { return }
  try {
    await api.delete(`/practice/banks/${encodeURIComponent(topic)}`)
    ElMessage.success('题库已删除')
    if (selectedBankTopic.value === topic) selectedBankTopic.value = ''
    fetchBanks()
  } catch {}
}

// ── 管理题目 ──
async function openManageBank(bank: any) {
  manageBankTopic.value = bank.topic
  showManageBank.value = true
  await fetchQuestions()
}
async function fetchQuestions() {
  try {
    const res: any = await api.get(`/practice/banks/${encodeURIComponent(manageBankTopic.value)}/questions`)
    bankQuestions.value = res.data || []
  } catch {}
}
async function addQuestion() {
  const q = newQuestion.value.trim()
  if (!q) return
  try {
    await api.post(`/practice/banks/${encodeURIComponent(manageBankTopic.value)}/questions`, {
      question: q,
      referenceAnswer: newAnswer.value.trim()
    })
    ElMessage.success('题目已添加')
    newQuestion.value = ''
    newAnswer.value = ''
    fetchQuestions()
    fetchBanks()
  } catch {}
}
async function deleteQuestion(qid: number) {
  try {
    await api.delete(`/practice/banks/${encodeURIComponent(manageBankTopic.value)}/questions/${qid}`)
    ElMessage.success('题目已删除')
    fetchQuestions()
    fetchBanks()
  } catch {}
}

onMounted(() => {
  fetchBanks()
})
</script>

<style lang="scss" scoped>
.practice-page { max-width: 860px; margin: 0 auto; padding-bottom: 40px; }
.page-header { display:flex; align-items:flex-start; justify-content:space-between; margin-bottom:24px;
  .page-title { display:flex; align-items:center; gap:8px; font-size:20px; font-weight:700; color:var(--color-text); margin:0 0 6px; .title-icon { color:#7B61FF; } }
  .page-subtitle { font-size:13px; color:var(--color-text-muted); margin:0; }
  .btn-history { padding:7px 16px; border-radius:8px; border: 1.5px solid var(--color-border); background:var(--color-card); color:var(--color-text-secondary); font-size:13px; cursor:pointer; white-space:nowrap; transition:all var(--transition);
    &:hover { border-color:#7B61FF; color:#7B61FF; } }
}
.section { margin-bottom:20px; }
.section-title { font-size:13px; font-weight:600; color:var(--color-text); margin:0 0 10px; }

// 刷题来源切换
.source-tabs { display:flex; gap:0; border-radius:8px; overflow:hidden; border:1.5px solid var(--color-border); width:fit-content; }
.source-tab { padding:7px 22px; border:none; background:var(--color-card); color:var(--color-text-secondary); font-size:13px; cursor:pointer; transition:all var(--transition);
  &:first-child { border-right:1.5px solid var(--color-border); }
  &.active { background:#7B61FF; color:#fff; }
  &:hover:not(.active) { background:var(--color-hover); }
}

// 系统题库方向标签
.tag-grid { display:flex; flex-wrap:wrap; gap:7px; }
.tag-btn { padding:8px 18px; border-radius:10px; border: 1.5px solid var(--color-border); background:var(--color-card); color:var(--color-text-secondary); font-size:13px; cursor:pointer; transition:all var(--transition); font-weight: 500;
  &:hover { border-color:var(--color-primary); color:var(--color-primary); transform: translateY(-1px); }
  &.active { border-color:var(--color-primary); background:var(--color-primary); color:#fff; font-weight:500; }
}

// 我的题库
.bank-header { display:flex; align-items:center; justify-content:space-between; margin-bottom:10px;
  .section-title { margin-bottom:0; } }
.btn-create-bank { padding:5px 14px; border-radius:7px; border: 1.5px solid #7B61FF; background:var(--color-card); color:#7B61FF; font-size:12px; cursor:pointer; transition:all var(--transition);
  &:hover { background:var(--color-active); } }
.bank-grid { display:flex; flex-direction:column; gap:8px; }
.bank-card { display:flex; align-items:center; justify-content:space-between; padding:14px 18px; border-radius:var(--radius-lg); border:1.5px solid var(--color-border); background:var(--color-card); cursor:pointer; transition:all var(--transition);
  &:hover { border-color:var(--color-primary); transform: translateY(-1px); box-shadow: var(--shadow-sm); }
  &.active { border-color:var(--color-primary); background:var(--color-active); } }
.bank-info { display:flex; align-items:center; gap:10px; }
.bank-name { font-size:14px; font-weight:500; color:var(--color-text); }
.bank-count { font-size:12px; color:var(--color-text-muted); background:var(--color-bubble-ai); padding:2px 8px; border-radius:10px; }
.bank-actions { display:flex; gap:4px; }
.btn-bank-manage, .btn-bank-delete { background:none; border:none; cursor:pointer; padding:4px; border-radius:4px; transition:all var(--transition); color:var(--color-text-muted);
  &:hover { background:var(--color-hover); } }
.btn-bank-delete:hover { color:#f56c6c; background:rgba(245,108,108,.08); }

// 知识库结合
.kb-checkbox { display:flex; align-items:center; gap:8px; cursor:pointer; font-size:13px; color:var(--color-text);
  input[type="checkbox"] { width:16px;height:16px;accent-color:#7B61FF;cursor:pointer; }
  .kb-label { font-weight:500; }
  .kb-hint { color:var(--color-text-muted); font-size:11px; } }

// 开始按钮
.start-section { text-align:center; margin-top:24px; }
.btn-start { display:inline-flex; align-items:center; gap:8px; padding:12px 42px; border-radius:12px; border:none; background:#7B61FF; color:#fff; font-size:15px; font-weight:600; cursor:pointer; transition:all var(--transition); box-shadow:0 4px 16px rgba(123,97,255,.2);
  &:hover:not(:disabled) { background:#6a50f0; transform:translateY(-1px); box-shadow:0 6px 24px rgba(123,97,255,.3); }
  &:disabled { opacity:.4; cursor:default; box-shadow:none; } }

// 题库管理弹窗
.add-question-row { display:flex; flex-direction:column; }

// 导入方式切换
.import-tabs { display:flex; gap:0; border-radius:8px; overflow:hidden; border:1.5px solid var(--color-border); width:fit-content; }
.import-tab { padding:6px 18px; border:none; background:var(--color-card); color:var(--color-text-secondary); font-size:12px; cursor:pointer; transition:all var(--transition);
  &:first-child { border-right:1.5px solid var(--color-border); }
  &.active { background:#7B61FF; color:#fff; }
  &:hover:not(.active) { background:var(--color-hover); } }

// 文件上传区域
.file-upload-zone { display:flex; flex-direction:column; align-items:center; justify-content:center; gap:8px;
  padding:28px 16px; border:2px dashed var(--color-upload-border); border-radius:10px; background:var(--color-upload-bg);
  cursor:pointer; transition:all var(--transition); min-height:110px;
  &:hover { border-color:var(--color-primary); background:var(--color-primary-bg); }
  &.hasFile { border-style:solid; border-color:#7B61FF; padding:14px; min-height:auto; flex-direction:row; gap:10px; }
  .file-hint { font-size:12px; color:var(--color-text-muted); }
  .file-name { font-size:13px; color:var(--color-text); font-weight:500; flex:1; }
  .btn-file-clear { background:none; border:none; cursor:pointer; color:var(--color-text-muted); font-size:14px; padding:2px 6px; border-radius:4px;
    &:hover { color:#f56c6c; background:rgba(245,108,108,.08); } } }
.question-list { display:flex; flex-direction:column; gap:6px; max-height:350px; overflow-y:auto; }
.question-item { display:flex; align-items:flex-start; gap:10px; padding:10px 12px; border-radius:8px; border:1px solid var(--color-border-light); background:var(--color-card);
  .q-text { flex:1; font-size:13px; color:var(--color-text); line-height:1.5; }
  .q-answer { font-size:11px; color:var(--color-text-muted); margin-top:4px; }
  .btn-q-del { flex-shrink:0; background:none; border:none; cursor:pointer; padding:2px; border-radius:4px; color:var(--color-text-muted); margin-top:2px;
    &:hover { color:#f56c6c; } }
}

// 空状态
.empty { text-align: center; padding: 24px; p { color: var(--color-text-muted); font-size: 13px; margin:0; } .empty-hint { font-size: 12px; opacity: .6; margin-top:4px; } }
</style>
