<template>
  <div class="goal-page">
    <!-- 标题 -->
    <div class="page-header">
      <div class="header-left">
        <h1 class="page-title">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="title-icon" width="20" height="20">
            <circle cx="12" cy="12" r="10"/><path d="M12 6v6l4 2"/>
          </svg>
          目标导航
        </h1>
        <p class="page-subtitle">设定目标，记录每一天，用数据追踪你的成长轨迹</p>
      </div>
    </div>

    <div class="goal-layout">
      <!-- 左：目标 + 计划 + 记录 -->
      <div class="goal-main">
        <!-- 目标设置 -->
        <div class="section">
          <h3 class="section-title">
            <svg viewBox="0 0 24 24" fill="none" stroke="#7B61FF" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" width="16" height="16" style="vertical-align:-2px;margin-right:6px"><circle cx="12" cy="12" r="10"/><circle cx="12" cy="12" r="6"/><circle cx="12" cy="12" r="2"/></svg>
            我的目标</h3>
          <div class="goal-input-row">
            <input
              v-model="goalText"
              class="text-input"
              placeholder="输入你的目标，例如：30天掌握 Spring Boot..."
              @keydown.enter="setGoal"
            />
            <button class="btn-primary" @click="setGoal">设定目标</button>
          </div>
          <div v-if="currentGoal" class="current-goal">
            <span class="goal-label">当前目标：</span>
            <span class="goal-value">{{ currentGoal }}</span>
            <span class="goal-date">设定于 {{ goalDate }}</span>
          </div>
        </div>

        <!-- 每日计划 -->
        <div class="section">
          <h3 class="section-title">
            <svg viewBox="0 0 24 24" fill="none" stroke="#7B61FF" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" width="16" height="16" style="vertical-align:-2px;margin-right:6px"><rect x="3" y="4" width="18" height="18" rx="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/><path d="M8 14h.01"/><path d="M12 14h.01"/><path d="M16 14h.01"/><path d="M8 18h.01"/><path d="M12 18h.01"/><path d="M16 18h.01"/></svg>
            每日计划</h3>
          <div class="plan-input-row">
            <input v-model="newPlanItem" class="text-input" placeholder="添加今日待办事项..." @keydown.enter="addPlanItem" />
            <button class="btn-primary btn-sm" @click="addPlanItem" :disabled="!newPlanItem.trim()">添加</button>
          </div>
          <div class="plan-list" v-if="planItems.length">
            <div v-for="(item, i) in planItems" :key="i" class="plan-item" :class="{ done: item.done }">
              <button class="check-btn" @click="item.done = !item.done; saveData()">
                <svg v-if="item.done" viewBox="0 0 24 24" fill="none" stroke="#43B88C" stroke-width="3" width="16" height="16"><polyline points="20 6 9 17 4 12"/></svg>
                <svg v-else viewBox="0 0 24 24" fill="none" stroke="var(--color-upload-border)" stroke-width="2" width="16" height="16"><rect x="3" y="3" width="18" height="18" rx="4"/></svg>
              </button>
              <span class="plan-text" :class="{ done: item.done }">{{ item.text }}</span>
              <button class="del-btn" @click="planItems.splice(i,1); saveData()">✕</button>
            </div>
          </div>
        </div>

        <!-- 今日时间记录 -->
        <div class="section">
          <h3 class="section-title">
            <svg viewBox="0 0 24 24" fill="none" stroke="#7B61FF" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" width="16" height="16" style="vertical-align:-2px;margin-right:6px"><circle cx="12" cy="13" r="8"/><path d="M12 9v4l2.5 2.5"/><path d="M4.5 3.5l3 3"/><path d="M19.5 3.5l-3 3"/></svg>
            今日时间记录</h3>
          <p class="section-hint">记录今天做了什么，系统自动生成时间分布图</p>
          <div class="time-input-row">
            <input v-model="newActivity.name" class="text-input" placeholder="活动名称，如：学习 Java" style="flex:2" />
            <input v-model.number="newActivity.hours" type="number" class="text-input" placeholder="小时" min="0.5" max="24" step="0.5" style="flex:0.8" @keydown.enter="addActivity" />
            <button class="btn-primary btn-sm" @click="addActivity" :disabled="!newActivity.name || !newActivity.hours">记录</button>
          </div>
          <div class="activity-list" v-if="activities.length">
            <div v-for="(a, i) in activities" :key="i" class="activity-item">
              <span class="act-dot" :style="{ background: colors[i % colors.length] }"></span>
              <span class="act-name">{{ a.name }}</span>
              <span class="act-hours">{{ a.hours }}h</span>
              <button class="del-btn" @click="activities.splice(i,1); saveData()">✕</button>
            </div>
          </div>
        </div>
      </div>

      <!-- 右：扇形统计图 -->
      <div class="goal-side">
        <div class="section">
          <h3 class="section-title">
            <svg viewBox="0 0 24 24" fill="none" stroke="#7B61FF" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" width="16" height="16" style="vertical-align:-2px;margin-right:6px"><path d="M21.21 15.89A10 10 0 1 1 8 2.83"/><path d="M22 12A10 10 0 0 0 12 2v10z"/></svg>
            今日时间分布</h3>
          <div class="chart-container">
            <!-- SVG 扇形图 -->
            <svg viewBox="0 0 200 200" class="donut-chart" v-if="totalHours > 0">
              <circle cx="100" cy="100" r="80" fill="none" stroke="var(--color-border-light)" stroke-width="24"/>
              <g v-for="(seg, i) in segments" :key="i">
                <circle cx="100" cy="100" r="80" fill="none"
                  :stroke="seg.color" stroke-width="24"
                  :stroke-dasharray="seg.dashArray"
                  :stroke-dashoffset="seg.dashOffset"
                  transform="rotate(-90 100 100)"
                  stroke-linecap="butt"
                  style="transition: all 0.6s ease"
                />
              </g>
              <text x="100" y="95" text-anchor="middle" class="chart-total">{{ totalHours }}h</text>
              <text x="100" y="113" text-anchor="middle" class="chart-label">总计</text>
            </svg>
            <div v-else class="chart-empty">
              <svg viewBox="0 0 24 24" fill="none" stroke="var(--color-upload-border)" stroke-width="1.5" width="36" height="36">
                <circle cx="12" cy="12" r="10"/><path d="M12 6v6l4 2"/>
              </svg>
              <p>记录今日活动后<br/>自动生成时间分布图</p>
            </div>
          </div>

          <!-- 图例 -->
          <div class="legend" v-if="activities.length">
            <div v-for="(a, i) in activities" :key="i" class="legend-item">
              <span class="legend-dot" :style="{ background: colors[i % colors.length] }"></span>
              <span class="legend-name">{{ a.name }}</span>
              <span class="legend-pct">{{ getPercent(a.hours) }}%</span>
            </div>
          </div>
        </div>

        <!-- 统计卡片 -->
        <div class="stats-row" v-if="activities.length">
          <div class="stat-card">
            <span class="stat-val">{{ totalHours }}h</span>
            <span class="stat-label">今日总时长</span>
          </div>
          <div class="stat-card">
            <span class="stat-val">{{ activities.length }}</span>
            <span class="stat-label">活动项目</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch, onMounted } from 'vue'

// 颜色盘
const colors = ['#7B61FF', '#43B88C', '#f0a050', '#e86070', '#5090e8', '#9060d0', '#50b8c8', '#d090c0', '#80b860', '#c8a040']

// 目标
const goalText = ref('')
const currentGoal = ref('')
const goalDate = ref('')

function setGoal() {
  if (!goalText.value.trim()) return
  currentGoal.value = goalText.value.trim()
  goalDate.value = new Date().toLocaleDateString('zh-CN')
  goalText.value = ''
  saveData()
}

// 每日计划
interface PlanItem { text: string; done: boolean }
const planItems = ref<PlanItem[]>([])
const newPlanItem = ref('')

function addPlanItem() {
  if (!newPlanItem.value.trim()) return
  planItems.value.push({ text: newPlanItem.value.trim(), done: false })
  newPlanItem.value = ''
  saveData()
}

// 时间活动
interface Activity { name: string; hours: number }
const activities = ref<Activity[]>([])
const newActivity = reactive({ name: '', hours: null as number | null })

function addActivity() {
  if (!newActivity.name || !newActivity.hours) return
  activities.value.push({ name: newActivity.name, hours: newActivity.hours })
  newActivity.name = ''; newActivity.hours = null
  saveData()
}

const totalHours = computed(() => {
  return activities.value.reduce((s, a) => s + a.hours, 0)
})

function getPercent(h: number) {
  if (totalHours.value === 0) return 0
  return Math.round((h / totalHours.value) * 100)
}

// 扇形图计算
interface Segment { color: string; dashArray: string; dashOffset: string }
const segments = computed(() => {
  const circumference = 2 * Math.PI * 80 // r=80
  const segs: Segment[] = []
  let offset = 0
  for (let i = 0; i < activities.value.length; i++) {
    const pct = activities.value[i].hours / totalHours.value
    const length = circumference * pct
    segs.push({
      color: colors[i % colors.length],
      dashArray: `${length} ${circumference - length}`,
      dashOffset: String(-offset),
    })
    offset += length
  }
  return segs
})

// localStorage 持久化
const STORAGE_KEY = 'trace-goal-data'
function saveData() {
  localStorage.setItem(STORAGE_KEY, JSON.stringify({
    goal: currentGoal.value, goalDate: goalDate.value,
    planItems: planItems.value, activities: activities.value,
  }))
}
function loadData() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (!raw) return
    const data = JSON.parse(raw)
    if (data.goal) { currentGoal.value = data.goal; goalDate.value = data.goalDate }
    if (data.planItems) planItems.value = data.planItems
    if (data.activities) activities.value = data.activities
  } catch {}
}

onMounted(loadData)
</script>

<style lang="scss" scoped>
.goal-page { max-width: 960px; margin: 0 auto; padding-bottom: 40px; }

// 标题
.page-header { margin-bottom: 24px;
  .page-title { display: flex; align-items: center; gap: 8px; font-size: 20px; font-weight: 700; color: var(--color-text); margin: 0 0 6px;
    .title-icon { color: #7B61FF; } }
  .page-subtitle { font-size: 13px; color: var(--color-text-muted); margin: 0; }
}

// 布局
.goal-layout { display: flex; gap: 28px; }
.goal-main { flex: 1; min-width: 0; }
.goal-side { width: 300px; flex-shrink: 0; }

// 通用
.section { margin-bottom: 22px; }
.section-title { font-size: 13px; font-weight: 600; color: var(--color-text); margin: 0 0 10px; }
.section-hint { font-size: 12px; color: var(--color-text-muted); margin: 0 0 8px; }

// 输入
.text-input {
  flex: 1; height: 38px; padding: 0 14px; border: 1.5px solid var(--color-border); border-radius: 9px;
  font-size: 13px; outline: none; background: var(--color-card); color: var(--color-text); transition: all var(--transition);
  &::placeholder { color: #c8ccd4; }
  &:focus { border-color: #7B61FF; box-shadow: 0 0 0 3px rgba(123,97,255,.08); }
}
.btn-primary {
  display: flex; align-items: center; gap: 6px; padding: 0 20px; height: 38px; border-radius: 9px; border: none;
  background: #7B61FF; color: #fff; font-size: 13px; font-weight: 500; cursor: pointer; white-space: nowrap;
  transition: all var(--transition); box-shadow: 0 2px 8px rgba(123,97,255,.15);
  &:hover:not(:disabled) { background: #6a50f0; }
  &:disabled { opacity: .4; cursor: default; }
}
.btn-sm { padding: 0 16px; height: 34px; font-size: 12px; }

// 目标
.goal-input-row { display: flex; gap: 8px; }
.current-goal {
  margin-top: 10px; padding: 10px 14px; background: var(--color-active); border-radius: 9px; border: 1px solid var(--color-stat-border);
  .goal-label { font-size: 11px; color: var(--color-text-muted); }
  .goal-value { font-size: 14px; font-weight: 600; color: #7B61FF; margin-left: 4px; }
  .goal-date { font-size: 11px; color: #c0b8d0; margin-left: 12px; }
}

// 计划
.plan-input-row { display: flex; gap: 8px; margin-bottom: 8px; }
.plan-list { display: flex; flex-direction: column; gap: 4px; }
.plan-item {
  display: flex; align-items: center; gap: 8px; padding: 7px 10px; border-radius: 8px; transition: all var(--transition);
  &:hover { background: var(--color-upload-bg); }
  &.done { opacity: .6; }
  .check-btn { border: none; background: none; cursor: pointer; padding: 0; display: flex; }
  .plan-text { flex: 1; font-size: 13px; color: var(--color-text);
    &.done { text-decoration: line-through; color: #c0c4cc; } }
  .del-btn { border: none; background: none; color: var(--color-upload-border); cursor: pointer; font-size: 12px; padding: 2px;
    &:hover { color: #f56c6c; } }
}

// 活动记录
.time-input-row { display: flex; gap: 8px; }
.activity-list { display: flex; flex-direction: column; gap: 4px; margin-top: 8px; }
.activity-item {
  display: flex; align-items: center; gap: 8px; padding: 7px 10px; border-radius: 8px;
  &:hover { background: var(--color-upload-bg); }
  .act-dot { width: 8px; height: 8px; border-radius: 50%; flex-shrink: 0; }
  .act-name { flex: 1; font-size: 13px; color: var(--color-text); }
  .act-hours { font-size: 12px; font-weight: 600; color: var(--color-text-secondary); }
  .del-btn { border: none; background: none; color: var(--color-upload-border); cursor: pointer; font-size: 12px; padding: 2px;
    &:hover { color: #f56c6c; } }
}

// 扇形图
.chart-container { display: flex; align-items: center; justify-content: center; min-height: 220px; }
.donut-chart { width: 200px; height: 200px;
  .chart-total { font-size: 22px; font-weight: 700; fill: var(--color-text); }
  .chart-label { font-size: 11px; fill: var(--color-text-muted); }
}
.chart-empty { text-align: center;
  p { font-size: 12px; color: #c8ccd4; line-height: 1.6; margin-top: 8px; }
}

// 图例
.legend { margin-top: 12px; display: flex; flex-direction: column; gap: 6px; }
.legend-item { display: flex; align-items: center; gap: 8px; font-size: 12px; }
.legend-dot { width: 8px; height: 8px; border-radius: 2px; flex-shrink: 0; }
.legend-name { flex: 1; color: var(--color-text-secondary); }
.legend-pct { font-weight: 600; color: var(--color-text); }

// 统计卡片
.stats-row { display: flex; gap: 10px; margin-top: 16px; }
.stat-card {
  flex: 1; text-align: center; padding: 14px 10px; border-radius: 10px;
  background: var(--color-active); border: 1.5px solid var(--color-stat-border);
  .stat-val { display: block; font-size: 20px; font-weight: 700; color: #7B61FF; }
  .stat-label { display: block; font-size: 11px; color: var(--color-text-muted); margin-top: 2px; }
}
</style>
