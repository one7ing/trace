<template>
  <div class="msg-row" :class="role">
    <div class="msg-avatar" :class="role">
      <svg v-if="role === 'ai'" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" width="18" height="18">
        <path d="M12 2a4 4 0 0 1 4 4v2a4 4 0 0 1-8 0V6a4 4 0 0 1 4-4z"/>
        <rect x="3" y="10" width="18" height="10" rx="3"/>
        <circle cx="9" cy="15" r="1.5" fill="white" stroke="none"/>
        <circle cx="15" cy="15" r="1.5" fill="white" stroke="none"/>
        <path d="M9 18c.83.67 1.83 1 3 1s2.17-.33 3-1" stroke="white" stroke-width="1.5"/>
      </svg>
      <span v-else class="user-avatar-text">{{ getInitial() }}</span>
    </div>
    <div class="msg-body">
      <div class="msg-sender">{{ role === 'ai' ? 'Trace AI' : '我' }}</div>
      <div class="msg-bubble" :class="role">
        <MarkdownRenderer v-if="role === 'ai'" :content="content" />
        <template v-else>{{ content }}</template>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import MarkdownRenderer from './MarkdownRenderer.vue'
import { useAuthStore } from '@/stores/auth'

const props = defineProps<{ role: 'user' | 'ai'; content: string }>()
const auth = useAuthStore()
function getInitial() { return auth.username?.charAt(0)?.toUpperCase() || 'U' }
</script>

<style lang="scss" scoped>
.msg-row {
  display: flex; gap: 12px; margin-bottom: 22px; align-items: flex-start;
  &.user { flex-direction: row-reverse;
    .msg-body { align-items: flex-end; }
    .msg-sender { text-align: right; }
  }

  .msg-avatar {
    width: 34px; height: 34px; border-radius: 10px; flex-shrink: 0;
    display: flex; align-items: center; justify-content: center;
    &.ai { background: linear-gradient(135deg, #6C5CE7, #8B7CF0); }
    &.user { background: linear-gradient(135deg, #43B88C, #5CC9A0);
      .user-avatar-text { color: #fff; font-weight: 600; font-size: 14px; }
    }
  }

  .msg-body { display: flex; flex-direction: column; max-width: 68%; }
  .msg-sender { font-size: 11px; color: #aaa; margin-bottom: 4px; padding: 0 4px; }

  .msg-bubble {
    padding: 10px 16px; font-size: 14px; line-height: 1.65; word-break: break-word;
    border-radius: 6px 14px 14px 14px;
    &.ai { background: var(--color-bubble-ai); color: #333; }
    &.user { background: var(--color-primary); color: #fff; border-radius: 14px 6px 14px 14px; }
  }
}
</style>
