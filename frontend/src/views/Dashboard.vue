<template>
  <div class="growth-dash">
    <!-- Hero -->
    <div class="hero-section">
      <div class="hero-score">
        <div class="score-ring">
          <svg viewBox="0 0 120 120" class="ring-svg">
            <circle cx="60" cy="60" r="52" fill="none" stroke="var(--color-border)" stroke-width="6"/>
            <circle cx="60" cy="60" r="52" fill="none" stroke="url(#sg)" stroke-width="6" stroke-linecap="round"
              :stroke-dasharray="326" :stroke-dashoffset="326 - 326 * growthScore / 100"
              transform="rotate(-90 60 60)" style="transition: stroke-dashoffset 1s ease"/>
            <defs><linearGradient id="sg" x1="0%" y1="0%" x2="100%" y2="100%">
              <stop offset="0%" stop-color="#7B61FF"/><stop offset="100%" stop-color="#a78bfa"/></linearGradient></defs>
          </svg>
          <div class="score-inner"><span class="score-num">{{ growthScore }}</span><span class="score-label">成长力</span></div>
        </div>
        <div class="score-meta">
          <span class="mom-change" :class="{ up: momChange>0, down: momChange<0 }">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14" style="vertical-align:-2px">
              <polyline v-if="momChange>0" points="18 15 12 9 6 15"/><polyline v-else points="6 9 12 15 18 9"/>
            </svg>{{ momChange>0?'+':'' }}{{ momChange }}%</span>
          <span class="mom-label">较上月</span>
        </div>
      </div>
      <p class="hero-desc">综合成长力指数 — 打卡 · 面试 · 学习行为</p>
    </div>

    <!-- 面积图 -->
    <div class="glass-card">
      <div class="chart-header"><span class="chart-title">成长趋势</span><span class="chart-sub">最近 30 天</span></div>
      <v-chart ref="chartRef" :option="trendOption" style="height:280px" autoresize />
      <p class="chart-hint">点击曲线上的点可添加记忆锚点</p>
    </div>

    <!-- 打卡扇形 + 记忆锚点 并排 -->
    <div class="split-row">
      <div class="glass-card split-card">
        <div class="chart-header"><span class="chart-title">本周打卡</span></div>
        <v-chart :option="checkinOption" style="height:180px" autoresize />
      </div>

      <div class="glass-card split-card">
        <div class="chart-header"><span class="chart-title">记忆锚点</span><span class="chart-sub">{{ anchors.length }} 个</span></div>
        <div v-if="anchors.length" class="anchors-list">
          <div v-for="a in anchors" :key="a.id" class="anchor-item">
            <span class="anchor-dot"></span><span class="anchor-date">{{ a.date }}</span>
            <span class="anchor-label">{{ a.label }}</span>
            <button class="anchor-del" @click="deleteAnchor(a.id)">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="12" height="12"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
            </button>
          </div>
        </div>
        <p v-else class="empty-text">点击趋势图上的点来添加</p>
        <div v-if="anchorFormDate" class="anchor-form">
          <span class="anchor-form-label">{{ anchorFormDate }}</span>
          <input v-model="anchorFormLabel" placeholder="输入标签" class="anchor-input" @keydown.enter="saveAnchor"/>
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
import { LineChart, PieChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import { ElMessage } from 'element-plus'

use([LineChart, PieChart, GridComponent, TooltipComponent, LegendComponent, CanvasRenderer])

const chartRef = ref<any>(null)
const growthScore = ref(0)
const momChange = ref(0)
const trend = ref<any[]>([])
const heatmap = ref<any[]>([])
const anchors = ref<any[]>([])
const anchorFormDate = ref('')
const anchorFormLabel = ref('')

const totalActivities = computed(() => 0) // kept for compatibility

// ── 打卡扇形图 ──
const checkinOption = computed(() => {
  const dist = dash.value.checkinDistribution || {}
  const days = ['周一','周二','周三','周四','周五','周六','周日']
  const data = days.map(d => ({
    name: d, value: dist[d] ? 1 : 0,
    itemStyle: { color: dist[d] ? '#7B61FF' : '#d5d5e0' },
  }))
  return {
    tooltip: { trigger:'item', formatter: (p:any)=>`${p.name}: ${p.value?'已打卡':'未打卡'}` },
    legend: { bottom:0, textStyle:{fontSize:10,color:'var(--color-text)'} },
    series:[{ type:'pie', radius:['45%','68%'], center:['50%','45%'], data,
      label:{ show:true, formatter:'{b}', fontSize:11, color:'var(--color-text)', fontWeight:500 } }],
  }
})

const trendOption = computed(() => {
  const markPoints = anchors.value.map(a => { const idx = trend.value.findIndex((t: any) => t.date === a.date); return idx >= 0 ? { name:a.label, coord:[idx, trend.value[idx].score], value:a.label, symbol:'pin', symbolSize:28, itemStyle:{color:'#f59e0b'}, label:{show:true,fontSize:10,color:'#333',formatter:a.label} } : null }).filter(Boolean)
  return {
    tooltip: { trigger:'axis', formatter: (p:any) => `${p[0].axisValue}<br/>成长力：${p[0].value} 分` },
    grid: { left:8, right:16, top:12, bottom:8 },
    xAxis: { type:'category', data:trend.value.map((t:any)=>t.date?.slice(5)||''), axisLabel:{fontSize:9,color:'var(--color-text-muted)'}, axisLine:{lineStyle:{color:'var(--color-border)'}} },
    yAxis: { type:'value', min:0, max:100, splitLine:{lineStyle:{color:'var(--color-border)',type:'dashed'}}, axisLabel:{fontSize:10,color:'var(--color-text-muted)'} },
    series:[{ data:trend.value.map((t:any)=>t.score||0), type:'line', smooth:true, symbol:'circle', symbolSize:5, lineStyle:{color:'#7B61FF',width:2}, itemStyle:{color:'#7B61FF',borderColor:'#fff',borderWidth:1},
      areaStyle:{color:{type:'linear',x:0,y:0,x2:0,y2:1,colorStops:[{offset:0,color:'rgba(123,97,255,.18)'},{offset:1,color:'rgba(123,97,255,0)'}]}}, markPoint:{data:markPoints,animation:true} }],
  }
})

const dash = ref<any>({})

async function fetchGrowth() {
  try {
    const [gr, db] = await Promise.all([
      api.get('/dashboard/growth'),
      api.get('/dashboard'),
    ])
    const d = (gr as any).data || {}
    growthScore.value = d.growthScore || 0; momChange.value = d.momChange || 0
    trend.value = d.trend || []; anchors.value = d.anchors || []
    dash.value = (db as any).data || {}
    await nextTick()
    const instance = chartRef.value
    if (instance) {
      const chart = (instance as any).chart || instance
      chart.off('click'); chart.on('click', (params: any) => {
        if (params.componentType === 'series') { const idx = params.dataIndex; if (idx >= 0 && idx < trend.value.length) { anchorFormDate.value = trend.value[idx].date; anchorFormLabel.value = '' } }
      })
    }
  } catch {}
}

async function saveAnchor() {
  if (!anchorFormLabel.value.trim() || !anchorFormDate.value) return
  try { const res: any = await api.post('/dashboard/anchor', { date: anchorFormDate.value, label: anchorFormLabel.value.trim() }); anchors.value.push(res.data); anchorFormDate.value = ''; anchorFormLabel.value = ''; ElMessage.success('锚点已添加') } catch { ElMessage.error('添加失败') }
}
async function deleteAnchor(id: number) {
  try { await api.delete(`/dashboard/anchor/${id}`); anchors.value = anchors.value.filter(a => a.id !== id); ElMessage.success('已删除') } catch { ElMessage.error('删除失败') }
}

onMounted(fetchGrowth)
</script>

<style lang="scss" scoped>
.growth-dash { max-width:960px; margin:0 auto; }
.hero-section { text-align:center; margin-bottom:28px; .hero-desc { font-size:13px; color:var(--color-text-muted); margin:12px 0 0; } }
.hero-score { display:flex; align-items:center; justify-content:center; gap:24px; }
.score-ring { position:relative; width:120px; height:120px; .ring-svg { width:120px; height:120px; } .score-inner { position:absolute; inset:0; display:flex; flex-direction:column; align-items:center; justify-content:center; .score-num { font-size:32px; font-weight:700; color:var(--color-text); line-height:1; } .score-label { font-size:12px; color:var(--color-text-muted); } } }
.score-meta { text-align:left; } .mom-change { font-size:22px; font-weight:700; &.up { color:#34a853; } &.down { color:#f56c6c; } } .mom-label { font-size:12px; color:var(--color-text-muted); display:block; }
.glass-card { background:var(--color-card); border:1px solid var(--color-border); border-radius:12px; padding:16px 20px; margin-bottom:16px; box-shadow:var(--shadow-sm); }
.chart-header { display:flex; justify-content:space-between; align-items:center; margin-bottom:8px; }
.chart-title { font-size:14px; font-weight:600; color:var(--color-text); }
.chart-sub { font-size:11px; color:var(--color-text-muted); }
.chart-hint { text-align:center; font-size:11px; color:var(--color-text-muted); margin-top:6px; }

.split-row { display:grid; grid-template-columns:1fr 1fr; gap:16px; margin-bottom:16px; }
.split-card { min-height:260px; margin-bottom:0 !important; }

.empty-text { text-align:center; color:var(--color-text-muted); font-size:13px; padding:16px 0; }
.anchors-list { display:flex; flex-direction:column; gap:8px; }
.anchor-item { display:flex; align-items:center; gap:10px; font-size:13px; } .anchor-dot { width:8px; height:8px; border-radius:50%; background:#7B61FF; flex-shrink:0; } .anchor-date { color:var(--color-text-muted); font-size:12px; } .anchor-label { color:var(--color-text); flex:1; } .anchor-del { background:none; border:none; color:var(--color-text-muted); cursor:pointer; &:hover { color:#f56c6c; } }
.anchor-form { display:flex; align-items:center; gap:8px; margin-top:12px; padding:10px 14px; background:var(--color-bubble-ai); border-radius:8px; flex-wrap:wrap; } .anchor-form-label { font-size:12px; color:var(--color-text-secondary); } .anchor-input { flex:1; min-width:180px; padding:6px 10px; border:1px solid var(--color-border); border-radius:6px; font-size:13px; background:var(--color-input); color:var(--color-text); outline:none; &:focus { border-color:#7B61FF; } } .anchor-save-btn { padding:5px 12px; border:none; border-radius:6px; background:#7B61FF; color:#fff; font-size:12px; cursor:pointer; } .anchor-cancel-btn { padding:5px 12px; border:1px solid var(--color-border); border-radius:6px; background:transparent; color:var(--color-text-muted); font-size:12px; cursor:pointer; }
</style>
