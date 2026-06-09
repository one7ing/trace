<template>
  <el-menu
    :default-active="activeIndex"
    router
    class="side-nav"
    background-color="#f5f6fa"
    text-color="#333"
    active-text-color="#409EFF"
  >
    <el-menu-item index="/knowledge">
      <el-icon><Search /></el-icon>
      <span>知识科普</span>
    </el-menu-item>

    <el-menu-item index="/interview">
      <el-icon><ChatLineSquare /></el-icon>
      <span>模拟面试</span>
    </el-menu-item>

    <el-menu-item index="/diary">
      <el-icon><Notebook /></el-icon>
      <span>日记本</span>
    </el-menu-item>

    <el-menu-item index="/weekly-report">
      <el-icon><DataAnalysis /></el-icon>
      <span>成长周报</span>
    </el-menu-item>

    <el-menu-item index="/plan">
      <el-icon><Tickets /></el-icon>
      <span>我的计划</span>
    </el-menu-item>
  </el-menu>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { Search, ChatLineSquare, Notebook, DataAnalysis, Tickets } from '@element-plus/icons-vue'

const route = useRoute()
const activeIndex = computed(() => {
  const path = route.path
  if (path.startsWith('/knowledge')) return '/knowledge'
  if (path.startsWith('/interview')) return '/interview'
  if (path.startsWith('/diary')) return '/diary'
  if (path.startsWith('/weekly-report')) return '/weekly-report'
  if (path.startsWith('/plan')) return '/plan'
  return '/knowledge'
})
</script>

<style lang="scss" scoped>
.side-nav {
  border-right: none;
  height: 100%;

  .el-menu-item {
    font-size: 14px;
    margin: 4px 8px;
    border-radius: 6px;
    height: 44px;
    line-height: 44px;

    &:hover {
      background-color: #e8edf5;
    }
  }
}
</style>
