<template>
  <div class="knowledge-page">
    <!-- 欢迎界面 -->
    <div v-if="messages.length === 0 && !streamingContent" class="welcome-area">
      <div class="welcome-card">
        <div class="welcome-avatar">
          <svg viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="1.5" width="38" height="38">
            <circle cx="12" cy="12" r="10"/><path d="M9.09 9a3 3 0 0 1 5.83 1c0 2-3 3-3 3"/><line x1="12" y1="17" x2="12.01" y2="17"/>
          </svg>
        </div>
        <h1 class="welcome-title">Trace 问答</h1>
        <p class="welcome-sub">AI 联网搜索 · 专业解答 · 实时获取最新信息</p>
        <div class="domain-row">
          <button
            v-for="d in domains" :key="d"
            class="domain-chip" :class="{ active: selectedDomain === d }"
            @click="selectedDomain = selectedDomain === d ? '' : d"
          >{{ d }}</button>
        </div>
      </div>
    </div>

    <!-- 对话区 -->
    <div v-else class="chat-area" ref="chatAreaRef">
      <div class="chat-title">Trace 问答</div>

      <template v-for="(m, i) in messages" :key="i">
        <ChatMessage :role="m.role" :content="m.content" />
        <!-- AI 回复下方独立免责声明 -->
        <div v-if="m.role === 'ai'" class="ai-disclaimer">
          ⚠️ 以上内容基于互联网搜索结果整理，仅供参考。AI 可能会出错，请保持独立判断。
        </div>
      </template>

      <!-- 流式输出中 -->
      <div v-if="streaming" class="msg-row ai">
        <div class="msg-avatar ai">
          <svg viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2" width="16" height="16">
            <circle cx="12" cy="12" r="10"/><path d="M9.09 9a3 3 0 0 1 5.83 1c0 2-3 3-3 3"/><line x1="12" y1="17" x2="12.01" y2="17"/>
          </svg>
        </div>
        <div class="msg-body">
          <div class="msg-sender">Trace AI</div>
          <div class="msg-bubble ai">
            <MarkdownRenderer :content="streamingContent || '思考中...'" />
          </div>
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
          :placeholder="messages.length === 0 ? '欢迎给 Trace 发消息' : '输入你的问题，Enter 发送...'"
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
          <button class="btn-send" @click="sendMessage" :disabled="!inputText.trim()">
            <svg viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" width="17" height="17">
              <line x1="22" y1="2" x2="11" y2="13"/><polygon points="22 2 15 22 11 13 2 9 22 2"/>
            </svg>
          </button>
        </template>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick } from 'vue'
import ChatMessage from '@/components/ChatMessage.vue'
import MarkdownRenderer from '@/components/MarkdownRenderer.vue'
import { useAuthStore } from '@/stores/auth'
import api from '@/api'
import { ElMessage } from 'element-plus'

const authStore = useAuthStore()
const domains = ['IT', '金融', '法律', '医学', '教育']
const selectedDomain = ref('')
const inputText = ref('')
const messages = ref<{role:'user'|'ai',content:string}[]>([])
const streaming = ref(false)
const streamingContent = ref('')
const chatAreaRef = ref<HTMLElement>()
const inputRef = ref<HTMLInputElement>()
let abortController: AbortController | null = null

async function sendMessage() {
  const text = inputText.value.trim()
  if (!text || streaming.value) return
  messages.value.push({ role:'user', content:text })
  inputText.value = ''
  streaming.value = true; streamingContent.value = ''
  abortController = new AbortController()
  await scrollDown()

  try {
    const resp = await fetch('/api/knowledge/chat', {
      method:'POST',
      headers:{ 'Content-Type':'application/json', 'Authorization':`Bearer ${authStore.token}` },
      body: JSON.stringify({ message: text, domain: selectedDomain.value || undefined }),
      signal: abortController.signal,
    })
    if (!resp.ok || !resp.body) throw new Error('fail')
    const reader = resp.body.getReader(); const dec = new TextDecoder(); let buf = ''
    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      buf += dec.decode(value, { stream:true })
      const lines = buf.split('\n'); buf = lines.pop() || ''
      for (const l of lines) { if (l.startsWith('data:')) streamingContent.value += l.substring(5) }
      await scrollDown()
    }
  } catch (e: any) {
    if (e.name !== 'AbortError') streamingContent.value += '\n\n*[连接中断]*'
  }
  if (streamingContent.value) messages.value.push({ role:'ai', content:streamingContent.value })
  streaming.value = false
  streamingContent.value = ''
  abortController = null
}

async function stopGeneration() {
  abortController?.abort()
  try { await api.post('/knowledge/stop') } catch {}
  if (streamingContent.value) messages.value.push({ role:'ai', content:streamingContent.value })
  streaming.value = false
  streamingContent.value = ''
  abortController = null
  ElMessage.info('已停止生成')
}

async function scrollDown() {
  await nextTick()
  if (chatAreaRef.value) chatAreaRef.value.scrollTop = chatAreaRef.value.scrollHeight
}
</script>

<style lang="scss" scoped>
.knowledge-page {
  display: flex; flex-direction: column; height: calc(100vh - 54px - 50px); max-width: 760px; margin: 0 auto;
}

// ===== 欢迎区 =====
.welcome-area {
  flex:1; display:flex; align-items:center; justify-content:center;
  .welcome-card { text-align:center; }
  .welcome-avatar {
    width:76px;height:76px;border-radius:20px;
    background:linear-gradient(135deg,#6C5CE7,#8B7CF0);
    display:flex;align-items:center;justify-content:center;margin:0 auto 22px;
    box-shadow:0 10px 30px rgba(108,92,231,.18);
  }
  .welcome-title { font-size:30px;font-weight:700;color:var(--color-text);margin:0 0 10px;letter-spacing:-0.5px; }
  .welcome-sub { font-size:14px;color:var(--color-text-secondary);margin:0 0 30px; }
  .domain-row { display:flex;gap:10px;justify-content:center;flex-wrap:wrap; }
  .domain-chip {
    padding:9px 22px;border-radius:22px;border:1.5px solid var(--color-border);background:var(--color-card-bg);color:var(--color-text-secondary);
    font-size:13px;cursor:pointer;transition:all var(--transition);
    &:hover { border-color:#b8b0f0;color:var(--color-primary);background:var(--color-primary-bg);transform:translateY(-1px); }
    &.active { border-color:var(--color-primary);background:var(--color-primary-bg);color:var(--color-primary);font-weight:600;box-shadow:0 2px 8px rgba(108,92,231,.12); }
  }
}

// ===== 对话区 =====
.chat-area {
  flex:1;overflow-y:auto;padding:24px 0 8px;
  .chat-title { font-size:16px;font-weight:600;color:var(--color-text);margin-bottom:22px;padding-bottom:14px;border-bottom:1px solid var(--color-border); }
}

// AI 回复下方独立免责声明
.ai-disclaimer {
  font-size: 11px; color: var(--color-text-muted); margin: -8px 0 20px 46px;
  line-height: 1.5; opacity: 0.75;
}

// 流式消息行
.msg-row { display:flex;gap:12px;margin-bottom:22px;align-items:flex-start;
  .msg-avatar { width:34px;height:34px;border-radius:10px;display:flex;align-items:center;justify-content:center;flex-shrink:0;
    &.ai { background:linear-gradient(135deg,#6C5CE7,#8B7CF0); }
  }
  .msg-body { display:flex;flex-direction:column;max-width:68%; }
  .msg-sender { font-size:11px;color:var(--color-text-muted);margin-bottom:5px;padding:0 2px; }
  .msg-bubble.ai { padding:11px 17px;font-size:14px;line-height:1.65;border-radius:6px 16px 16px 16px;background:var(--color-bubble-ai);color:var(--color-text); }
}

// ===== 底部输入 =====
.input-bar { padding:14px 0 8px; }
.input-row { display:flex;gap:10px;align-items:center; }
.chat-input {
  flex:1;height:48px;padding:0 20px;font-size:14px;border:1.5px solid var(--color-border);border-radius:24px;outline:none;
  background:var(--color-card-bg);color:var(--color-text);transition:all var(--transition);
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
