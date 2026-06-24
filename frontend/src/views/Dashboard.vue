<template>
  <div class="growth-dash">
    <!-- Hero 三栏卡片 -->
    <div class="hero-row">
      <!-- 成长力环形 -->
      <div class="hero-card">
        <div class="score-ring">
          <svg viewBox="0 0 120 120" class="ring-svg">
            <circle cx="60" cy="60" r="52" fill="none" stroke="var(--color-border-light)" stroke-width="6"/>
            <circle cx="60" cy="60" r="52" fill="none" stroke="url(#sg)" stroke-width="6" stroke-linecap="round"
              :stroke-dasharray="326" :stroke-dashoffset="326 - 326 * growthScore / 100"
              transform="rotate(-90 60 60)" style="transition: stroke-dashoffset 1.2s cubic-bezier(0.4,0,0.2,1)"/>
            <defs><linearGradient id="sg" x1="0%" y1="0%" x2="100%" y2="100%">
              <stop offset="0%" stop-color="#6C5CE7"/><stop offset="100%" stop-color="#a78bfa"/></linearGradient></defs>
          </svg>
          <div class="score-inner">
            <span class="score-num">{{ growthScore }}</span>
            <span class="score-label">成长力</span>
          </div>
        </div>
        <span class="hero-card-title">综合成长力</span>
      </div>

      <!-- 较上月变化 -->
      <div class="hero-card">
        <div class="mom-change" :class="{ up: momChange>0, down: momChange<0 }">
          <span class="mom-num">{{ momChange>0?'+':'' }}{{ momChange }}%</span>
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="18" height="18">
            <polyline v-if="momChange>0" points="18 15 12 9 6 15"/><polyline v-else points="6 9 12 15 18 9"/>
          </svg>
        </div>
        <span class="hero-card-title">较上月变化</span>
      </div>

      <!-- 连续打卡 -->
      <div class="hero-card">
        <div class="streak-num">{{ streakDays }}</div>
        <span class="hero-card-title">连续打卡天数</span>
      </div>
    </div>

    <!-- 成长趋势图 -->
    <div class="glass-card">
      <div class="chart-header">
        <span class="chart-title">成长趋势</span>
        <div class="time-tabs">
          <button v-for="t in timeTabs" :key="t.value" class="time-tab"
            :class="{ active: activeTimeTab === t.value }"
            @click="activeTimeTab = t.value"
          >{{ t.label }}</button>
        </div>
      </div>
      <v-chart ref="chartRef" :option="trendOption" style="height:280px" autoresize />
      <p class="chart-hint">点击曲线上的点可添加记忆锚点</p>
    </div>

    <!-- 打卡周历 + 记忆锚点 并排 -->
    <div class="split-row">
      <!-- 本周打卡格子 -->
      <div class="glass-card split-card">
        <div class="chart-header"><span class="chart-title">本周打卡</span></div>
        <div class="checkin-grid">
          <div v-for="(d, i) in weekCheckin" :key="i" class="checkin-day"
            :class="{ checked: d.checked, today: d.isToday }"
            :title="d.fullLabel">
            <span class="checkin-dot" :style="d.checked ? { background: d.color, borderColor: d.color } : {}"></span>
            <span class="checkin-label" :style="d.checked ? { color: d.color } : {}">{{ d.label }}</span>
          </div>
        </div>
      </div>

      <!-- 记忆锚点 -->
      <div class="glass-card split-card">
        <div class="chart-header"><span class="chart-title">记忆锚点</span><span class="chart-sub">{{ anchors.length }} 个</span></div>
        <div v-if="anchors.length" class="anchors-list">
          <div v-for="a in anchors" :key="a.id" class="anchor-item">
            <div class="anchor-timeline">
              <span class="anchor-dot"></span>
              <span class="anchor-line" v-if="anchors.indexOf(a) < anchors.length - 1"></span>
            </div>
            <div class="anchor-body">
              <span class="anchor-date">{{ a.date }}</span>
              <span class="anchor-label">{{ a.label }}</span>
            </div>
            <button class="anchor-del" @click="deleteAnchor(a.id)" title="删除">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="12" height="12"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
            </button>
          </div>
        </div>
        <p v-else class="empty-text">点击趋势图上的点来添加锚点</p>
        <div v-if="anchorFormDate" class="anchor-form">
          <span class="anchor-form-label">{{ anchorFormDate }}</span>
          <input v-model="anchorFormLabel" placeholder="输入标签..." class="anchor-input" @keydown.enter="saveAnchor"/>
          <button class="anchor-save-btn" @click="saveAnchor">保存</button>
          <button class="anchor-cancel-btn" @click="anchorFormDate=''">取消</button>
        </div>
      </div>
    </div>

  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, nextTick } from 'vue'
import api from '@/api'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { LineChart } from 'echarts/charts'
import { GridComponent, TooltipComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import { ElMessage } from 'element-plus'

use([LineChart, GridComponent, TooltipComponent, CanvasRenderer])

const chartRef = ref<any>(null)
const growthScore = ref(0)
const momChange = ref(0)
const streakDays = ref(0)
const trend = ref<any[]>([])
const anchors = ref<any[]>([])
const anchorFormDate = ref('')
const anchorFormLabel = ref('')
const dash = ref<any>({})
const activeTimeTab = ref('30d')

const timeTabs = [
  { label: '7天', value: '7d' },
  { label: '30天', value: '30d' },
  { label: '90天', value: '90d' },
]

// ── 本周打卡数据 ──
const dayColors = ['#7B61FF', '#38bdf8', '#34a853', '#f59e0b', '#f56c6c', '#a78bfa', '#fb7185']
const weekCheckin = computed(() => {
  const dist = dash.value.checkinDistribution || {}
  const days = ['周一','周二','周三','周四','周五','周六','周日']
  const todayIdx = (new Date().getDay() + 6) % 7
  return days.map((d, i) => ({
    label: days[i].charAt(1),
    fullLabel: d,
    checked: !!dist[d],
    isToday: i === todayIdx,
    color: dayColors[i],
  }))
})

// ── 趋势图选项 ──
const trendOption = computed(() => {
  const markPoints = anchors.value.map(a => {
    const idx = trend.value.findIndex((t: any) => t.date === a.date)
    return idx >= 0 ? {
      name: a.label, coord: [idx, trend.value[idx].score],
      value: a.label, symbol: 'pin', symbolSize: 30,
      itemStyle: { color: '#f59e0b' },
      label: { show: true, fontSize: 10, color: '#5e6078', formatter: a.label }
    } : null
  }).filter(Boolean)

  return {
    tooltip: { trigger: 'axis', formatter: (p: any) => `${p[0].axisValue}<br/>成长力：${p[0].value} 分` },
    grid: { left: 8, right: 16, top: 12, bottom: 8 },
    xAxis: {
      type: 'category', data: trend.value.map((t: any) => t.date?.slice(5) || ''),
      axisLabel: { fontSize: 10, color: 'var(--color-text-muted)' },
      axisLine: { lineStyle: { color: 'var(--color-border)' } }
    },
    yAxis: {
      type: 'value', min: 0, max: 100,
      splitLine: { lineStyle: { color: 'var(--color-border)', type: 'dashed' } },
      axisLabel: { fontSize: 10, color: 'var(--color-text-muted)' }
    },
    series: [{
      data: trend.value.map((t: any) => t.score || 0),
      type: 'line', smooth: true, symbol: 'circle', symbolSize: 5,
      lineStyle: { color: '#7B61FF', width: 2.5 },
      itemStyle: { color: '#7B61FF', borderColor: '#fff', borderWidth: 1.5 },
      areaStyle: { color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
        colorStops: [{ offset: 0, color: 'rgba(123,97,255,.2)' }, { offset: 1, color: 'rgba(123,97,255,0)' }] }
      },
      markPoint: { data: markPoints, animation: true }
    }],
  }
})

// ── 数据加载 ──
async function fetchGrowth() {
  try {
    const [gr, db] = await Promise.all([
      api.get('/dashboard/growth'),
      api.get('/dashboard'),
    ])
    const d = (gr as any).data || {}
    growthScore.value = d.growthScore || 0
    momChange.value = d.momChange || 0
    streakDays.value = d.streakDays || 0
    trend.value = d.trend || []
    anchors.value = d.anchors || []
    dash.value = (db as any).data || {}

    await nextTick()
    const instance = chartRef.value
    if (instance) {
      const chart = (instance as any).chart || instance
      chart.off('click')
      chart.on('click', (params: any) => {
        if (params.componentType === 'series') {
          const idx = params.dataIndex
          if (idx >= 0 && idx < trend.value.length) {
            anchorFormDate.value = trend.value[idx].date
            anchorFormLabel.value = ''
          }
        }
      })
    }
  } catch { /* handled */ }
}

async function saveAnchor() {
  if (!anchorFormLabel.value.trim() || !anchorFormDate.value) return
  try {
    const res: any = await api.post('/dashboard/anchor', {
      date: anchorFormDate.value, label: anchorFormLabel.value.trim()
    })
    anchors.value.push(res.data)
    anchorFormDate.value = ''
    anchorFormLabel.value = ''
    ElMessage.success('锚点已添加')
  } catch { ElMessage.error('添加失败') }
}

async function deleteAnchor(id: number) {
  try {
    await api.delete(`/dashboard/anchor/${id}`)
    anchors.value = anchors.value.filter(a => a.id !== id)
    ElMessage.success('已删除')
  } catch { ElMessage.error('删除失败') }
}

onMounted(fetchGrowth)
</script>

<style lang="scss" scoped>
.growth-dash { max-width: 960px; margin: 0 auto; padding-bottom: 40px; }

// ── Hero 三栏卡片 ──
.hero-row {
  display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 16px; margin-bottom: 20px;
}
.hero-card {
  background: var(--color-card); border: 1px solid var(--color-border);
  border-radius: var(--radius-lg); padding: 20px 16px;
  display: flex; flex-direction: column; align-items: center; gap: 10px;
  box-shadow: var(--shadow-xs); transition: all var(--transition);
  &:hover { box-shadow: var(--shadow-sm); transform: translateY(-1px); }
  .hero-card-title { font-size: 12px; color: var(--color-text-muted); }
  // 环形图
  .score-ring { position: relative; width: 90px; height: 90px;
    .ring-svg { width: 90px; height: 90px; }
    .score-inner { position: absolute; inset: 0; display: flex; flex-direction: column; align-items: center; justify-content: center;
      .score-num { font-size: 26px; font-weight: 700; color: var(--color-text); line-height: 1; }
      .score-label { font-size: 10px; color: var(--color-text-muted); }
    }
  }
  // 月度变化
  .mom-change { display: flex; align-items: center; gap: 6px;
    .mom-num { font-size: 28px; font-weight: 700; }
    &.up { color: #34a853; } &.down { color: #f56c6c; }
    svg { flex-shrink: 0; }
  }
  // 连续打卡
  .streak-num { font-size: 32px; font-weight: 700; color: var(--color-primary); }
}

// ── 卡片 ──
.glass-card {
  background: var(--color-card); border: 1px solid var(--color-border);
  border-radius: var(--radius-lg); padding: 18px 22px; margin-bottom: 16px;
  box-shadow: var(--shadow-xs);
}
.chart-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.chart-title { font-size: 14px; font-weight: 600; color: var(--color-text); }
.chart-sub { font-size: 11px; color: var(--color-text-muted); }
.chart-hint { text-align: center; font-size: 11px; color: var(--color-text-muted); margin-top: 6px; }

// ── 时间切换 ──
.time-tabs { display: flex; gap: 2px; background: var(--color-border-light); border-radius: 8px; padding: 2px; }
.time-tab {
  padding: 4px 12px; border: none; background: transparent; border-radius: 6px;
  font-size: 12px; color: var(--color-text-muted); cursor: pointer; transition: all var(--transition-fast);
  &.active { background: var(--color-card); color: var(--color-primary); font-weight: 500; box-shadow: var(--shadow-xs); }
}

// ── 打卡格子 ──
.checkin-grid {
  display: flex; justify-content: space-between; padding: 10px 0 6px;
}
.checkin-day {
  display: flex; flex-direction: column; align-items: center; gap: 6px; flex: 1;
  .checkin-dot {
    width: 36px; height: 36px; border-radius: 50%;
    border: 2px dashed var(--color-border);
    display: flex; align-items: center; justify-content: center;
    transition: all var(--transition);
  }
  .checkin-label { font-size: 11px; color: var(--color-text-muted); }
  &.checked {
    .checkin-dot { border-style: solid; }
    .checkin-label { font-weight: 500; }
  }
  &.today:not(.checked) {
    .checkin-dot { border-color: var(--color-primary); border-style: solid; border-width: 2px; }
    .checkin-label { color: var(--color-primary); }
  }
}

// ── 并排 ──
.split-row { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; margin-bottom: 16px; }
.split-card { min-height: 260px; margin-bottom: 0 !important; }

// ── 记忆锚点时间线 ──
.empty-text { text-align: center; color: var(--color-text-muted); font-size: 12px; padding: 16px 0; }
.anchors-list { display: flex; flex-direction: column; }
.anchor-item { display: flex; align-items: flex-start; gap: 8px; padding: 2px 0; }
.anchor-timeline { display: flex; flex-direction: column; align-items: center; width: 16px; flex-shrink: 0;
  .anchor-dot { width: 8px; height: 8px; border-radius: 50%; background: #7B61FF; margin-top: 5px; }
  .anchor-line { width: 1.5px; flex: 1; min-height: 16px; background: var(--color-border); margin-top: 4px; }
}
.anchor-body { flex: 1; min-width: 0;
  .anchor-date { font-size: 11px; color: var(--color-text-muted); display: block; }
  .anchor-label { font-size: 13px; color: var(--color-text); font-weight: 500; }
}
.anchor-del {
  background: none; border: none; color: var(--color-text-muted); cursor: pointer; padding: 4px;
  border-radius: 4px; opacity: 0; transition: all var(--transition-fast); margin-top: 3px;
  &:hover { color: #f56c6c; background: var(--color-border-light); }
}
.anchor-item:hover .anchor-del { opacity: 1; }

.anchor-form { display: flex; align-items: center; gap: 8px; margin-top: 12px; padding: 10px 14px; background: var(--color-bubble-ai); border-radius: var(--radius-sm); flex-wrap: wrap; }
.anchor-form-label { font-size: 12px; color: var(--color-text-secondary); }
.anchor-input { flex: 1; min-width: 120px; padding: 6px 10px; border: 1px solid var(--color-border); border-radius: 6px; font-size: 13px; background: var(--color-input); color: var(--color-text); outline: none; &:focus { border-color: #7B61FF; } }
.anchor-save-btn { padding: 5px 14px; border: none; border-radius: 6px; background: var(--color-primary); color: #fff; font-size: 12px; cursor: pointer; }
.anchor-cancel-btn { padding: 5px 12px; border: 1px solid var(--color-border); border-radius: 6px; background: transparent; color: var(--color-text-muted); font-size: 12px; cursor: pointer; }
</style>
