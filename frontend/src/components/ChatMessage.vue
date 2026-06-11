<template>
  <div class="msg-row" :class="role">
    <div class="msg-avatar" :class="role" :style="role === 'user' && userAvatarImg ? { backgroundImage: 'url('+userAvatarImg+')', backgroundSize:'cover' } : {}">
      <span v-if="role === 'ai'">🤖</span>
      <span v-else-if="!userAvatarImg">{{ userInitial }}</span>
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
import { ref, computed, onMounted } from 'vue'
import MarkdownRenderer from './MarkdownRenderer.vue'

defineProps<{ role: 'user' | 'ai'; content: string }>()
import { useAuthStore } from '@/stores/auth'
const auth = useAuthStore()
const userInitial = computed(() => auth.username?.charAt(0)?.toUpperCase() || 'U')
const userAvatarImg = ref('')

onMounted(() => {
  userAvatarImg.value = localStorage.getItem('trace-user-avatar-img') || ''
})
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
