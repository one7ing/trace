<template>
  <div class="markdown-body" v-html="renderedHtml"></div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import MarkdownIt from 'markdown-it'

const props = defineProps<{
  content: string
  /** 流式模式：自动闭合未完成的 markdown 结构后再渲染 */
  streaming?: boolean
}>()

const md = new MarkdownIt({
  html: false,
  linkify: true,
  breaks: true,
  typographer: true,
})

/**
 * 流式安全渲染：自动闭合未完成的代码块/行内代码/加粗，
 * 然后用完整 markdown-it 渲染，让用户实时看到格式化效果。
 */
function streamSafeRender(text: string): string {
  let safe = text

  // 1. 闭合未完成的围栏代码块 (```)
  //    统计 ``` 出现次数，奇数说明有未闭合的
  const fenceMatches = safe.match(/^```/gm)
  if (fenceMatches && fenceMatches.length % 2 === 1) {
    safe += '\n```'
  }

  // 2. 闭合未闭合的加粗 **
  const boldCount = (safe.match(/\*\*/g) || []).length
  if (boldCount % 2 === 1) {
    safe += '**'
  }

  // 3. 闭合未闭合的行内代码 `
  //    排除代码块中的 ```（已在上一步处理）
  const lines = safe.split('\n')
  let inFence = false
  let inlineTicks = 0
  for (const line of lines) {
    if (line.trim().startsWith('```')) { inFence = !inFence; continue }
    if (!inFence) {
      // 统计行内反引号（奇数个 = 未闭合）
      const ticks = line.match(/(?<!`)`(?!`)/g)
      if (ticks) inlineTicks += ticks.length
    }
  }
  if (inlineTicks % 2 === 1) {
    safe += '`'
  }

  try {
    return md.render(safe)
  } catch {
    // 极端情况，回退到最简单的段落渲染
    return '<p>' + safe.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/\n\n+/g, '</p><p>').replace(/\n/g, '<br>') + '</p>'
  }
}

const renderedHtml = computed(() => {
  if (!props.content) return ''
  if (props.streaming) {
    return streamSafeRender(props.content)
  }
  try {
    return md.render(props.content)
  } catch {
    return streamSafeRender(props.content)
  }
})
</script>
