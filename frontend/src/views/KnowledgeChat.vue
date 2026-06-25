<template>
  <div class="knowledge-page">
    <!-- 欢迎界面（无消息时） -->
    <div v-if="messages.length === 0 && !streamingContent && historyLoaded" class="welcome-area">
      <div class="welcome-bg"></div>
      <div class="welcome-content">
        <div class="welcome-logo">
          <svg viewBox="0 0 60 60" width="60" height="60">
            <rect width="60" height="60" rx="16" fill="url(#wl)"/>
            <defs><linearGradient id="wl" x1="0%" y1="0%" x2="100%" y2="100%">
              <stop offset="0%" stop-color="#6C5CE7"/><stop offset="100%" stop-color="#8B7CF0"/>
            </linearGradient></defs>
            <text x="30" y="40" text-anchor="middle" fill="white" font-size="30" font-weight="700">T</text>
          </svg>
        </div>
        <h1 class="welcome-title">有什么我可以帮你的？</h1>
        <p class="welcome-sub">{{ modeLabel }}</p>
        <!-- 内联输入 -->
        <div class="welcome-input-row">
          <div class="mode-capsule">
            <button class="capsule-btn" :class="{ active: chatMode === 'direct' }" @click="chatMode = 'direct'" title="默认对话">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14"><circle cx="12" cy="12" r="10"/><path d="M9.09 9a3 3 0 0 1 5.83 1c0 2-3 3-3 3"/></svg>
            </button>
            <button class="capsule-btn" :class="{ active: chatMode === 'web' }" @click="chatMode = 'web'" title="联网搜索">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14"><circle cx="12" cy="12" r="10"/><line x1="2" y1="12" x2="22" y2="12"/><path d="M12 2a15.3 15.3 0 0 1 4 10 15.3 15.3 0 0 1-4 10 15.3 15.3 0 0 1-4-10 15.3 15.3 0 0 1 4-10z"/></svg>
            </button>
            <button class="capsule-btn" :class="{ active: chatMode === 'rag' }" @click="chatMode = 'rag'" title="RAG 知识库">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14"><path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"/><path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"/></svg>
            </button>
          </div>
          <div class="input-group">
            <!-- RAG 知识库选择 -->
            <div v-if="chatMode === 'rag'" class="rag-inline">
              <el-select v-model="selectedKbTopic" placeholder="选择知识库" size="small" style="width:160px">
                <el-option v-for="kb in kbList" :key="kb" :label="kb" :value="kb"/>
              </el-select>
            </div>
            <input
              ref="inputRef"
              v-model="inputText"
              class="chat-input-welcome"
              :placeholder="inputPlaceholder"
              @keydown.enter="sendMessage"
            />
            <button class="btn-send-welcome" @click="sendMessage" :disabled="!inputText.trim() || (chatMode === 'rag' && !selectedKbTopic)">
              <svg viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" width="16" height="16">
                <line x1="12" y1="19" x2="12" y2="5"/><polyline points="5 12 12 5 19 12"/>
              </svg>
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 对话区（有消息时） -->
    <div v-else class="chat-layout">
      <!-- 顶部模式栏 -->
      <div class="chat-topbar">
        <div class="mode-capsule">
          <button class="capsule-btn" :class="{ active: chatMode === 'direct' }" @click="chatMode = 'direct'" title="默认对话">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14"><circle cx="12" cy="12" r="10"/><path d="M9.09 9a3 3 0 0 1 5.83 1c0 2-3 3-3 3"/></svg>
          </button>
          <button class="capsule-btn" :class="{ active: chatMode === 'web' }" @click="chatMode = 'web'" title="联网搜索">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14"><circle cx="12" cy="12" r="10"/><line x1="2" y1="12" x2="22" y2="12"/><path d="M12 2a15.3 15.3 0 0 1 4 10 15.3 15.3 0 0 1-4 10 15.3 15.3 0 0 1-4-10 15.3 15.3 0 0 1 4-10z"/></svg>
          </button>
          <button class="capsule-btn" :class="{ active: chatMode === 'rag' }" @click="chatMode = 'rag'" title="RAG 知识库">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14"><path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"/><path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"/></svg>
          </button>
        </div>
        <div v-if="chatMode === 'rag'" class="rag-inline">
          <el-select v-model="selectedKbTopic" placeholder="选择知识库" size="small" style="width:160px">
            <el-option v-for="kb in kbList" :key="kb" :label="kb" :value="kb"/>
          </el-select>
        </div>
      </div>

      <!-- 消息列表 -->
      <div class="chat-area" ref="chatAreaRef" @scroll="onChatScroll">
        <div v-if="loadingHistory" class="loading-more">加载中...</div>
        <div v-else-if="!hasMoreHistory && messages.length > 0" class="no-more">—— 已加载全部历史 ——</div>

        <div v-for="(m, i) in messages" :key="i" class="ds-msg" :class="{ user: m.role === 'user' }">
          <div class="ds-msg-inner">
            <MarkdownRenderer v-if="m.role === 'ai'" :content="m.content" />
            <template v-else>{{ m.content }}</template>
          </div>
        </div>

        <!-- 流式输出 -->
        <div v-if="streaming" class="ds-msg ai">
          <div class="ds-msg-inner streaming">
            <MarkdownRenderer :content="streamingContent || '思考中...'" :streaming="true" />
            <span class="streaming-cursor">|</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 底部输入浮栏（有消息时） -->
    <div v-if="messages.length > 0 || streaming" class="input-bar-float">
      <div class="input-row-float">
        <input
          ref="inputRef"
          v-model="inputText"
          class="chat-input-float"
          :placeholder="inputPlaceholder"
          @keydown.enter="sendMessage"
          :disabled="streaming"
        />
        <template v-if="streaming">
          <button class="btn-stop-float" @click="stopGeneration">
            <svg viewBox="0 0 24 24" fill="white" width="14" height="14"><rect x="6" y="6" width="12" height="12" rx="2"/></svg>
            停止
          </button>
        </template>
        <template v-else>
          <button class="btn-send-float" @click="sendMessage" :disabled="!inputText.trim() || (chatMode === 'rag' && !selectedKbTopic)">
            <svg viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" width="16" height="16">
              <line x1="12" y1="19" x2="12" y2="5"/><polyline points="5 12 12 5 19 12"/>
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
  return '基于大模型知识直接回答'
})
const inputPlaceholder = computed(() => {
  if (chatMode.value === 'web') return '联网搜索模式，输入问题...'
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

    // 发送流式请求（使用fetch绕过axios）
    let resp = await fetch('/api/knowledge/chat', {
      method:'POST',
      headers:{ 'Content-Type':'application/json', 'Authorization':`Bearer ${authStore.accessToken}` },
      body: JSON.stringify(body),
      signal: abortController.signal,
    })

    // 如果accessToken过期，用refreshToken刷新后重试
    if (resp.status === 401) {
      const refreshToken = localStorage.getItem('trace-refreshToken')
      if (refreshToken) {
        const refreshResp = await fetch('/api/auth/refresh', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ refreshToken }),
        })
        const refreshData = await refreshResp.json()
        if (refreshData.code === 200 && refreshData.data?.accessToken) {
          const newToken = refreshData.data.accessToken
          localStorage.setItem('trace-accessToken', newToken)
          authStore.accessToken = newToken
          resp = await fetch('/api/knowledge/chat', {
            method:'POST',
            headers:{ 'Content-Type':'application/json', 'Authorization':`Bearer ${newToken}` },
            body: JSON.stringify(body),
            signal: abortController.signal,
          })
        }
      }
    }

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
  display: flex; flex-direction: column; height: 100%;
  max-width: 760px; margin: 0 auto;
}

// ── 模式胶囊 ──
.mode-capsule {
  display: flex; gap: 2px; background: var(--color-border-light); border-radius: 10px; padding: 3px;
  .capsule-btn {
    display: flex; align-items: center; justify-content: center;
    width: 32px; height: 30px; border: none; border-radius: 8px;
    background: transparent; color: var(--color-text-muted); cursor: pointer;
    transition: all var(--transition-fast);
    &:hover { color: var(--color-text-secondary); }
    &.active { background: var(--color-card); color: var(--color-primary); box-shadow: var(--shadow-xs); }
  }
}

// ── 欢迎区 ──
.welcome-area {
  flex: 1; display: flex; align-items: center; justify-content: center; position: relative;
  .welcome-content {
    position: relative; z-index: 1; text-align: center; max-width: 560px; width: 100%;
  }
  .welcome-logo { margin-bottom: 16px; }
  .welcome-title { font-size: 26px; font-weight: 700; color: var(--color-text); margin: 0 0 8px; }
  .welcome-sub { font-size: 13px; color: var(--color-text-muted); margin: 0 0 24px; }
  .welcome-input-row {
    display: flex; flex-direction: column; align-items: center; gap: 12px;
    .input-group { display: flex; align-items: center; gap: 8px; width: 100%; }
    .rag-inline { margin-right: 4px; }
  }
  .chat-input-welcome {
    flex: 1; height: 48px; padding: 0 20px; font-size: 14px;
    border: 1.5px solid var(--color-border); border-radius: var(--radius-full);
    outline: none; background: var(--color-input); color: var(--color-text);
    transition: all var(--transition);
    &::placeholder { color: var(--color-text-muted); }
    &:focus { border-color: var(--color-primary); box-shadow: 0 0 0 3px rgba(108,92,231,.08); }
  }
  .btn-send-welcome {
    width: 44px; height: 44px; border-radius: 50%; border: none; flex-shrink: 0;
    background: var(--color-primary-gradient); color: #fff; cursor: pointer;
    display: flex; align-items: center; justify-content: center; transition: all var(--transition);
    box-shadow: 0 4px 14px rgba(108,92,231,.25);
    &:hover:not(:disabled) { transform: scale(1.06); box-shadow: 0 6px 20px rgba(108,92,231,.35); }
    &:disabled { opacity: .35; cursor: default; box-shadow: none; }
  }
}

// ── 对话布局 ──
.chat-layout {
  flex: 1; display: flex; flex-direction: column; overflow: hidden;
}

// ── 顶部模式栏 ──
.chat-topbar {
  display: flex; align-items: center; gap: 10px; padding: 8px 0 12px;
  .rag-inline { flex-shrink: 0; }
}

// ── 对话区 ──
.chat-area {
  flex: 1; overflow-y: auto; padding: 8px 0 0;
}

// ── 消息气泡 ──
.ds-msg {
  display: flex; margin-bottom: 14px;
  &.user { justify-content: flex-end;
    .ds-msg-inner {
      background: var(--color-primary-gradient); color: #fff;
      border-radius: 14px 4px 14px 14px;
    }
  }
  &.ai, &:not(.user) {
    .ds-msg-inner {
      background: var(--color-bubble-ai); color: var(--color-text);
      border-radius: 4px 14px 14px 14px; position: relative;
    }
  }
  .ds-msg-inner {
    max-width: 82%; padding: 10px 16px; font-size: 14px; line-height: 1.65; word-break: break-word;
    box-shadow: var(--shadow-xs);
    :deep(.markdown-body) { font-size: 14px; }
  }
}

// ── 流式动画 ──
.streaming {
  .streaming-cursor {
    display: inline-block; color: var(--color-primary); font-weight: 700;
    animation: blink 0.8s infinite; margin-left: 2px;
  }
}
@keyframes blink {
  0%, 100% { opacity: 1; } 50% { opacity: 0; }
}

// ── 底部输入浮栏 ──
.input-bar-float {
  padding: 12px 0 8px;
}
.input-row-float {
  display: flex; gap: 10px; align-items: center;
}
.chat-input-float {
  flex: 1; height: 48px; padding: 0 20px; font-size: 14px;
  border: 1.5px solid var(--color-border); border-radius: var(--radius-full);
  outline: none; background: var(--color-input); color: var(--color-text);
  transition: all var(--transition);
  &::placeholder { color: var(--color-text-muted); }
  &:focus { border-color: var(--color-primary); box-shadow: 0 0 0 3px rgba(108,92,231,.08); }
  &:disabled { opacity: 0.6; }
}
.btn-send-float {
  width: 44px; height: 44px; border-radius: 50%; border: none; flex-shrink: 0;
  background: var(--color-primary-gradient); color: #fff; cursor: pointer;
  display: flex; align-items: center; justify-content: center; transition: all var(--transition);
  box-shadow: 0 4px 14px rgba(108,92,231,.25);
  &:hover:not(:disabled) { transform: scale(1.06); box-shadow: 0 6px 20px rgba(108,92,231,.35); }
  &:disabled { opacity: .35; cursor: default; box-shadow: none; }
}
.btn-stop-float {
  display: flex; align-items: center; gap: 6px; padding: 0 22px; height: 44px;
  border-radius: var(--radius-full); border: none;
  background: var(--color-primary-gradient); color: #fff; font-size: 13px; font-weight: 500;
  cursor: pointer; transition: all var(--transition); flex-shrink: 0;
  box-shadow: 0 4px 14px rgba(108,92,231,.25);
  &:hover { transform: translateY(-1px); }
}

// ── 其他 ──
.global-disclaimer, .loading-more, .no-more {
  font-size: 11px; color: var(--color-text-muted); text-align: center;
  margin: 6px auto 0; max-width: 500px; line-height: 1.4;
}
.loading-more { padding: 8px 0; }
.no-more { padding: 12px 0 4px; opacity: 0.6; }
</style>
