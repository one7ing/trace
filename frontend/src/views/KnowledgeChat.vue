<template>
  <div class="knowledge-page">
    <!-- 模式选择器 -->
    <div class="mode-bar">
      <button class="mode-btn" :class="{ active: chatMode === 'direct' }" @click="chatMode = 'direct'">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="15" height="15"><circle cx="12" cy="12" r="10"/><path d="M9.09 9a3 3 0 0 1 5.83 1c0 2-3 3-3 3"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg>
        默认对话
      </button>
      <button class="mode-btn" :class="{ active: chatMode === 'web' }" @click="chatMode = 'web'">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="15" height="15"><circle cx="12" cy="12" r="10"/><line x1="2" y1="12" x2="22" y2="12"/><path d="M12 2a15.3 15.3 0 0 1 4 10 15.3 15.3 0 0 1-4 10 15.3 15.3 0 0 1-4-10 15.3 15.3 0 0 1 4-10z"/></svg>
        联网搜索
      </button>
      <button class="mode-btn" :class="{ active: chatMode === 'rag' }" @click="chatMode = 'rag'">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="15" height="15"><path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"/><path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"/></svg>
        RAG 知识库
      </button>
    </div>

    <!-- RAG 知识库选择 -->
    <div v-if="chatMode === 'rag'" class="rag-selector">
      <span class="rag-label">选择知识库：</span>
      <el-select v-model="selectedKbTopic" placeholder="选择知识库" size="small" style="width:200px">
        <el-option v-for="kb in kbList" :key="kb" :label="kb" :value="kb"/>
      </el-select>
    </div>

    <!-- 欢迎界面 -->
    <div v-if="messages.length === 0 && !streamingContent && historyLoaded" class="welcome-area">
      <div class="welcome-card">
        <h1 class="welcome-title">Trace 问答</h1>
        <p class="welcome-sub">{{ modeLabel }}</p>
      </div>
    </div>

    <!-- 对话区 -->
    <div v-else class="chat-area" ref="chatAreaRef" @scroll="onChatScroll">
      <div v-if="loadingHistory" class="loading-more">加载中...</div>
      <div v-else-if="!hasMoreHistory && messages.length > 0" class="no-more">—— 已加载全部历史 ——</div>

      <!-- DeepSeek 风格消息 -->
      <div v-for="(m, i) in messages" :key="i" class="ds-msg" :class="{ user: m.role === 'user' }">
        <div class="ds-msg-inner">
          <MarkdownRenderer v-if="m.role === 'ai'" :content="m.content" />
          <template v-else>{{ m.content }}</template>
        </div>
      </div>

      <!-- 流式输出 -->
      <div v-if="streaming" class="ds-msg ai">
        <div class="ds-msg-inner">
          <MarkdownRenderer :content="streamingContent || '思考中...'" :streaming="true" />
        </div>
      </div>
    </div>

    <!-- 底部输入 -->
    <div class="input-bar">
      <div class="input-row">
        <input
          ref="inputRef"
          v-model="inputText"
          class="chat-input"
          :placeholder="inputPlaceholder"
          @keydown.enter="sendMessage"
          :disabled="streaming"
        />
        <template v-if="streaming">
          <button class="btn-stop" @click="stopGeneration">
            <svg viewBox="0 0 24 24" fill="white" width="14" height="14"><rect x="6" y="6" width="12" height="12" rx="2"/></svg>
            停止
          </button>
        </template>
        <template v-else>
          <button class="btn-send" @click="sendMessage" :disabled="!inputText.trim() || (chatMode === 'rag' && !selectedKbTopic)">
            <svg viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" width="17" height="17">
              <line x1="22" y1="2" x2="11" y2="13"/><polygon points="22 2 15 22 11 13 2 9 22 2"/>
            </svg>
          </button>
        </template>
      </div>
      <p class="global-disclaimer">以上内容均由 AI 回复，请谨慎参考</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, nextTick, onMounted } from 'vue'
import MarkdownRenderer from '@/components/MarkdownRenderer.vue'
import { useAuthStore } from '@/stores/auth'
import api from '@/api'
import { ElMessage } from 'element-plus'

const authStore = useAuthStore()

// 模式
const chatMode = ref<'direct'|'web'|'rag'>('direct')
const selectedKbTopic = ref('')
const kbList = ref<string[]>([])

const modeLabel = computed(() => {
  if (chatMode.value === 'web') return '联网搜索 · 实时获取最新信息'
  if (chatMode.value === 'rag') return 'RAG 知识库 · 基于你的文档回答'
  return '大模型直接回答 · 基于内置知识'
})
const inputPlaceholder = computed(() => {
  if (chatMode.value === 'web') return '联网搜索模式，Enter 发送...'
  if (chatMode.value === 'rag') return selectedKbTopic.value ? `向「${selectedKbTopic.value}」提问...` : '请先选择知识库'
  return '输入你的问题，Enter 发送...'
})

const inputText = ref('')
const messages = ref<{role:'user'|'ai',content:string}[]>([])
const streaming = ref(false)
const streamingContent = ref('')

// 历史记录
const historyLoaded = ref(false)
const hasMoreHistory = ref(true)
const loadingHistory = ref(false)
let oldestId: number | null = null

const chatAreaRef = ref<HTMLElement>()
const inputRef = ref<HTMLInputElement>()
let abortController: AbortController | null = null

// ── 加载知识库列表 ──
async function fetchKbList() {
  try {
    const res: any = await api.get('/knowledge-base/items', { params: { page: 0, size: 50 } })
    const records = res.data?.records || []
    kbList.value = records.map((r: any) => r.fileName)
  } catch {}
}

// ── 发送消息 ──
async function sendMessage() {
  const text = inputText.value.trim()
  if (!text || streaming.value) return
  if (chatMode.value === 'rag' && !selectedKbTopic.value) {
    ElMessage.warning('请先选择知识库')
    return
  }
  messages.value.push({ role:'user', content:text })
  inputText.value = ''
  streaming.value = true; streamingContent.value = ''
  abortController = new AbortController()
  await scrollDown()

  try {
    const body: any = { message: text, mode: chatMode.value }
    if (chatMode.value === 'rag') body.knowledgeBaseTopic = selectedKbTopic.value

    const resp = await fetch('/api/knowledge/chat', {
      method:'POST',
      headers:{ 'Content-Type':'application/json', 'Authorization':`Bearer ${authStore.token}` },
      body: JSON.stringify(body),
      signal: abortController.signal,
    })
    if (!resp.ok || !resp.body) throw new Error('请求失败')
    const reader = resp.body.getReader(); const dec = new TextDecoder(); let buf = ''
    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      buf += dec.decode(value, { stream:true })
      const lines = buf.split('\n'); buf = lines.pop() || ''
      for (const l of lines) { if (l.startsWith('data:')) streamingContent.value += l.substring(5) + '\n' }
      await scrollDown()
    }
  } catch (e: any) {
    if (e.name !== 'AbortError') streamingContent.value += '\n\n*[连接中断]*'
  }
  if (streamingContent.value) {
    const cleaned = streamingContent.value
      .replace(/([^\n])\n([^\n])/g, '$1$2')
      .replace(/\n{3,}/g, '\n\n')
    messages.value.push({ role:'ai', content: cleaned })
  }
  streaming.value = false
  streamingContent.value = ''
  abortController = null
}

async function stopGeneration() {
  abortController?.abort()
  try { await api.post('/knowledge/stop') } catch {}
  if (streamingContent.value) {
    const cleaned = streamingContent.value
      .replace(/([^\n])\n([^\n])/g, '$1$2')
      .replace(/\n{3,}/g, '\n\n')
    messages.value.push({ role:'ai', content: cleaned })
  }
  streaming.value = false
  streamingContent.value = ''
  abortController = null
  ElMessage.info('已停止生成')
}

async function scrollDown() {
  await nextTick()
  if (chatAreaRef.value) chatAreaRef.value.scrollTop = chatAreaRef.value.scrollHeight
}

async function loadHistory() {
  if (loadingHistory.value || !hasMoreHistory.value) return
  loadingHistory.value = true
  const prevScrollHeight = chatAreaRef.value?.scrollHeight || 0
  try {
    const res: any = await api.get('/knowledge/history', { params: { beforeId: oldestId, limit: 20 } })
    const records = res.data?.records || []
    hasMoreHistory.value = res.data?.hasMore || false
    if (records.length > 0) {
      const newMsgs = records.reverse().map((r: any) => ({ role: r.role as 'user'|'ai', content: r.content }))
      messages.value = [...newMsgs, ...messages.value]
      oldestId = records[0]?.id || null
      await nextTick()
      if (chatAreaRef.value) chatAreaRef.value.scrollTop = chatAreaRef.value.scrollHeight - prevScrollHeight
    }
    historyLoaded.value = true
  } catch { console.error('Failed to load history') }
  finally { loadingHistory.value = false }
}

function onChatScroll() {
  const el = chatAreaRef.value
  if (!el || loadingHistory.value || !hasMoreHistory.value) return
  if (el.scrollTop <= 20) loadHistory()
}

onMounted(() => {
  loadHistory()
  fetchKbList()
})
</script>

<style lang="scss" scoped>
.knowledge-page {
  display: flex; flex-direction: column; height: calc(100vh - 54px - 50px); max-width: 760px; margin: 0 auto;
}

// ===== 模式栏 =====
.mode-bar {
  display: flex; gap: 6px; padding: 12px 0 6px; justify-content: center;
  .mode-btn {
    display: flex; align-items: center; gap: 5px;
    padding: 7px 16px; border-radius: 20px; border: 1.5px solid var(--color-border);
    background: var(--color-card); color: var(--color-text-secondary);
    font-size: 12.5px; cursor: pointer; transition: all var(--transition);
    &:hover { border-color: var(--color-primary); color: var(--color-primary); }
    &.active { background: var(--color-primary); color: #fff; border-color: var(--color-primary); }
  }
}

// RAG 选择器
.rag-selector {
  display: flex; align-items: center; gap: 8px; justify-content: center; padding: 4px 0 8px;
  .rag-label { font-size: 12px; color: var(--color-text-muted); }
}

// ===== 欢迎区 =====
.welcome-area {
  flex:1; display:flex; align-items:center; justify-content:center;
  .welcome-card { text-align:center; }
  .welcome-title { font-size:30px;font-weight:700;color:var(--color-text);margin:0 0 10px;letter-spacing:-0.5px; }
  .welcome-sub { font-size:14px;color:var(--color-text-secondary);margin:0; }
}

// ===== 对话区 DeepSeek 风格 =====
.chat-area {
  flex:1;overflow-y:auto;padding:20px 0 8px;
}

.ds-msg {
  display: flex; margin-bottom: 16px;
  &.user { justify-content: flex-end;
    .ds-msg-inner { background: var(--color-primary); color: #fff; border-radius: 14px 4px 14px 14px; }
  }
  &.ai, &:not(.user) {
    .ds-msg-inner { background: var(--color-bubble-ai); color: var(--color-text); border-radius: 4px 14px 14px 14px; }
  }
  .ds-msg-inner {
    max-width: 82%; padding: 10px 16px; font-size: 14px; line-height: 1.65; word-break: break-word;
    :deep(.markdown-body) { font-size: 14px; }
  }
}

// ===== 底部输入 =====
.input-bar { padding: 14px 0 8px; }
.input-row { display:flex;gap:10px;align-items:center; }
.chat-input {
  flex:1;height:48px;padding:0 20px;font-size:14px;border:1.5px solid var(--color-border);border-radius:24px;outline:none;
  background:var(--color-card);color:var(--color-text);transition:all var(--transition);
  &::placeholder { color:var(--color-text-muted); }
  &:focus { border-color:var(--color-primary);box-shadow:0 0 0 3px rgba(108,92,231,.08); }
  &:disabled { opacity:0.6; }
}
.btn-send {
  width:44px;height:44px;border-radius:50%;border:none;
  background:linear-gradient(135deg,#6C5CE7,#7C6EF0);color:#fff;cursor:pointer;
  display:flex;align-items:center;justify-content:center;transition:all var(--transition);flex-shrink:0;
  box-shadow:0 4px 14px rgba(108,92,231,.25);
  &:hover:not(:disabled) { transform:scale(1.06);box-shadow:0 6px 20px rgba(108,92,231,.35); }
  &:disabled { opacity:.35;cursor:default;box-shadow:none; }
}
.btn-stop {
  display:flex;align-items:center;gap:6px;padding:0 22px;height:44px;border-radius:22px;border:none;
  background:linear-gradient(135deg,#6C5CE7,#7C6EF0);color:#fff;font-size:13px;font-weight:500;
  cursor:pointer;transition:all var(--transition);flex-shrink:0;
  box-shadow:0 4px 14px rgba(108,92,231,.25);
  &:hover { transform:translateY(-1px);box-shadow:0 6px 20px rgba(108,92,231,.35); }
}
</style>
<style lang="scss" scoped>
.global-disclaimer { font-size: 11px; color: var(--color-text-muted) !important; text-align: center; margin: 6px auto 0; max-width: 500px; line-height: 1.4; }
.loading-more { text-align: center; font-size: 12px; color: var(--color-text-muted); padding: 8px 0; }
.no-more { text-align: center; font-size: 11px; color: var(--color-text-muted); padding: 12px 0 4px; opacity: 0.6; }
</style>
