<template>
  <div class="chat-container">
    <!-- 消息列表 -->
    <div class="message-list" ref="messageListRef">
      <div v-if="messages.length === 0" class="welcome">
        <h2>📚 知识科普</h2>
        <p>基于互联网实时搜索，提供专业知识解答。</p>
        <div class="domain-tags">
          <el-tag
            v-for="d in domains"
            :key="d"
            :type="selectedDomain === d ? '' : 'info'"
            :effect="selectedDomain === d ? 'dark' : 'plain'"
            @click="selectedDomain = selectedDomain === d ? '' : d"
            style="cursor: pointer; margin: 4px;"
          >
            {{ d }}
          </el-tag>
        </div>
      </div>

      <ChatMessage
        v-for="(msg, index) in messages"
        :key="index"
        :role="msg.role"
        :content="msg.content"
      />

      <!-- 正在输入中 -->
      <div v-if="streaming" class="message-bubble ai">
        <div class="bubble-content">
          <MarkdownRenderer :content="streamingContent || '思考中...'" />
        </div>
      </div>
    </div>

    <!-- 输入区域 -->
    <div class="input-area">
      <el-input
        v-model="inputText"
        placeholder="输入你的问题..."
        @keyup.enter="sendMessage"
        :disabled="streaming"
        clearable
      />
      <el-button type="primary" @click="sendMessage" :disabled="streaming || !inputText.trim()">
        发送
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick } from 'vue'
import ChatMessage from '@/components/ChatMessage.vue'
import MarkdownRenderer from '@/components/MarkdownRenderer.vue'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()

interface Message {
  role: 'user' | 'ai'
  content: string
}

const domains = ['IT', '金融', '法律', '医学', '教育', '其他']
const selectedDomain = ref('')
const inputText = ref('')
const messages = ref<Message[]>([])
const streaming = ref(false)
const streamingContent = ref('')
const messageListRef = ref<HTMLElement>()

async function sendMessage() {
  const text = inputText.value.trim()
  if (!text || streaming.value) return

  messages.value.push({ role: 'user', content: text })
  inputText.value = ''
  streaming.value = true
  streamingContent.value = ''
  await scrollToBottom()

  try {
    const token = authStore.token
    const response = await fetch(`/api/knowledge/chat`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`,
      },
      body: JSON.stringify({
        message: text,
        domain: selectedDomain.value || undefined,
      }),
    })

    if (!response.ok || !response.body) {
      throw new Error('Request failed')
    }

    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''

    while (true) {
      const { done, value } = await reader.read()
      if (done) break

      buffer += decoder.decode(value, { stream: true })
      const lines = buffer.split('\n')
      buffer = lines.pop() || ''

      for (const line of lines) {
        if (line.startsWith('data:')) {
          streamingContent.value += line.substring(5)
        }
      }
      await scrollToBottom()
    }
  } catch (err) {
    streamingContent.value += '\n\n*[连接中断，请重试]*'
  }

  if (streamingContent.value) {
    messages.value.push({ role: 'ai', content: streamingContent.value })
  }
  streaming.value = false
  streamingContent.value = ''
}

async function scrollToBottom() {
  await nextTick()
  if (messageListRef.value) {
    messageListRef.value.scrollTop = messageListRef.value.scrollHeight
  }
}
</script>

<style lang="scss" scoped>
.welcome {
  text-align: center;
  padding: 60px 20px;

  h2 {
    font-size: 24px;
    color: #1a1a2e;
    margin-bottom: 8px;
  }

  p {
    color: #999;
    font-size: 14px;
  }

  .domain-tags {
    margin-top: 20px;
  }
}
</style>
