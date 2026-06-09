<template>
  <div class="message-bubble" :class="role">
    <div class="bubble-content">
      <MarkdownRenderer v-if="role === 'ai'" :content="content" />
      <div v-else class="user-text">{{ content }}</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import MarkdownRenderer from './MarkdownRenderer.vue'

defineProps<{
  role: 'user' | 'ai'
  content: string
}>()
</script>

<style lang="scss" scoped>
.user-text {
  white-space: pre-wrap;
  word-break: break-word;
}
</style>
