<template>
  <div class="interview-page">
    <div class="page-header">
      <div class="header-left">
        <h1 class="page-title">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="title-icon" width="20" height="20"><polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"/></svg>
          你的专属面试官
        </h1>
        <p class="page-subtitle">选择面试方向开始面试吧</p>
      </div>
      <button class="btn-history" @click="$router.push('/interview/history')">历史记录 →</button>
    </div>

    <div class="section"><h3 class="section-title">面试方向</h3>
      <div class="tag-grid"><button v-for="d in directions" :key="d.value" class="tag-btn" :class="{ active: currentDirection === d.value }" @click="selectDirection(d)">{{ d.label }}</button></div>
    </div>

    <div class="section" v-if="subCategories.length"><h3 class="section-title">{{ currentDirectionLabel }} · 技术细分</h3>
      <div v-for="cat in subCategories" :key="cat.name" class="sub-group">
        <span class="sub-name">{{ cat.name }}</span>
        <div class="tag-grid"><button v-for="s in cat.items" :key="s" class="tag-btn skill-tag" :class="{ active: form.skills.includes(s) }" @click="toggleSkill(s)">{{ s }}</button></div>
      </div>
    </div>

    <div class="section"><div class="inline-row">
      <div class="inline-group"><h3 class="section-title">面试难度</h3>
        <div class="btn-group"><button v-for="d in ['实习','校招','社招']" :key="d" class="btn-option" :class="{ active: form.difficulty === d }" @click="form.difficulty = d">{{ d }}</button></div>
      </div>
      <div class="inline-group"><h3 class="section-title">题目数量</h3>
        <div style="display:flex;align-items:center;gap:10px;">
          <div class="btn-group"><button v-for="n in [5,10,15]" :key="n" class="btn-option" :class="{ active: form.questionCount === n }" @click="form.questionCount = n">{{ n }}题</button></div>
          <input type="number" class="count-input" placeholder="自定义" min="1" max="30" :value="customCount" @input="onCustomCount"/>
        </div>
      </div>
    </div></div>

    <div class="section"><h3 class="section-title">上传简历（可选）</h3>
      <div class="upload-card" :class="{ 'has-file': resumeFile, dragging: isDragging }" @dragover.prevent="isDragging=true" @dragleave.prevent="isDragging=false" @drop.prevent="handleDrop" @click="triggerUpload">
        <template v-if="!resumeFile">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" width="28" height="28" class="upload-icon"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="12" y1="18" x2="12" y2="12"/><line x1="9" y1="15" x2="15" y2="15"/></svg>
          <p class="upload-text">点击或拖拽上传简历</p><p class="upload-hint">支持 PDF、Word、TXT</p>
        </template>
        <template v-else><div class="file-row"><span>📄</span><span class="file-name">{{ resumeFile.name }}</span><span class="file-remove" @click.stop="resumeFile=null">✕</span></div></template>
      </div>
      <input ref="fileInputRef" type="file" accept=".pdf,.doc,.docx,.txt" style="display:none" @change="handleFileChange"/>
    </div>

    <div class="start-section">
      <button class="btn-start" :disabled="form.skills.length===0" @click="startInterview">
        <svg viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2" width="16" height="16"><polygon points="5 3 19 12 5 21 5 3"/></svg>
        开始面试（{{ form.questionCount }}题 · {{ form.difficulty }}）
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { useRouter } from 'vue-router'
import api from '@/api'

const router = useRouter()
const directions = [
  { value:'backend', label:'后端开发', categories:[{name:'编程语言',items:['Java','Go','Python','Node.js']},{name:'框架',items:['Spring Boot','Spring Cloud','Gin','FastAPI','Express']},{name:'数据库',items:['MySQL','PostgreSQL','Redis','MongoDB']},{name:'消息队列',items:['Kafka','RocketMQ','RabbitMQ']},{name:'容器化',items:['Docker','Kubernetes']}]},
  { value:'frontend', label:'前端开发', categories:[{name:'基础',items:['HTML5','CSS3','JavaScript','TypeScript']},{name:'框架',items:['Vue3','React','Next.js','Nuxt.js']},{name:'构建',items:['Webpack','Vite','Rollup']},{name:'样式',items:['Tailwind CSS','Sass','CSS-in-JS']}]},
  { value:'fullstack', label:'全栈开发', categories:[{name:'综合',items:['前后端联调','数据库设计','API设计','JWT/OAuth2','Nginx','Docker','CI/CD']}]},
  { value:'data', label:'数据科学', categories:[{name:'语言',items:['Python','Pandas','NumPy','SQL']},{name:'ML',items:['Scikit-learn','XGBoost','PyTorch','TensorFlow']},{name:'大数据',items:['Spark','Hadoop','Hive']}]},
  { value:'ops', label:'运维SRE', categories:[{name:'系统',items:['Linux','Shell','TCP/IP','DNS']},{name:'容器',items:['Docker','Kubernetes','Helm']},{name:'监控',items:['Prometheus','Grafana','ELK']},{name:'CI/CD',items:['Jenkins','GitLab CI','ArgoCD']}]},
  { value:'mobile', label:'移动端', categories:[{name:'iOS',items:['Swift','SwiftUI','UIKit']},{name:'Android',items:['Kotlin','Jetpack Compose']},{name:'跨平台',items:['Flutter','React Native']}]},
  { value:'embedded', label:'嵌入式', categories:[{name:'语言',items:['C','C++']},{name:'RTOS',items:['FreeRTOS','Zephyr']},{name:'MCU',items:['STM32','ESP32']},{name:'协议',items:['I2C','SPI','UART','MQTT','BLE']}]},
  { value:'qa', label:'测试开发', categories:[{name:'框架',items:['JUnit','pytest','Jest','Cypress','Playwright']},{name:'性能',items:['JMeter','Locust']},{name:'CI',items:['Jenkins','GitHub Actions']}]},
  { value:'security', label:'安全工程', categories:[{name:'Web安全',items:['SQL注入','XSS','CSRF','SSRF']},{name:'渗透',items:['Burp Suite','Metasploit','Nmap']},{name:'审计',items:['CodeQL','SonarQube']},{name:'运维',items:['WAF','SIEM','IDS/IPS']}]},
]

const currentDirection = ref('backend')
const currentDirectionLabel = computed(() => directions.find(d=>d.value===currentDirection.value)?.label||'')
const subCategories = computed(() => directions.find(d=>d.value===currentDirection.value)?.categories||[])
const form = reactive({ industry:'IT', skills:[] as string[], difficulty:'社招', questionCount:10 })
const resumeFile = ref<File|null>(null)
const isDragging = ref(false)
const fileInputRef = ref<HTMLInputElement>()
const customCount = ref('')

function selectDirection(d:typeof directions[0]) { currentDirection.value=d.value; form.skills=[]; customCount.value=''; form.questionCount=10 }
function toggleSkill(s:string) { const i=form.skills.indexOf(s); i>=0?form.skills.splice(i,1):form.skills.push(s) }
function onCustomCount(e:Event) { const v=(e.target as HTMLInputElement).value; customCount.value=v; const n=parseInt(v); if(n>0&&n<=30) form.questionCount=n }
function triggerUpload() { fileInputRef.value?.click() }
function handleFileChange(e:Event) { const t=e.target as HTMLInputElement; if(t.files?.length) resumeFile.value=t.files[0] }
function handleDrop(e:DragEvent) { isDragging.value=false; if(e.dataTransfer?.files?.length) resumeFile.value=e.dataTransfer.files[0] }

async function startInterview() {
  try {
    const res: any = await api.post('/interview/start', {
      industry:'IT', skills:form.skills, questionCount:form.questionCount, difficulty:form.difficulty
    })
    router.push({ path:'/interview/start', query:{ sessionId:res.data.sessionId, question:res.data.question, current:'1', total:String(form.questionCount), difficulty:form.difficulty } })
  } catch {}
}

// 初始化第一个方向
selectDirection(directions[0])
</script>

<style lang="scss" scoped>
.interview-page { max-width: 860px; margin: 0 auto; padding-bottom: 40px; }
.page-header { display:flex; align-items:flex-start; justify-content:space-between; margin-bottom:24px;
  .page-title { display:flex; align-items:center; gap:8px; font-size:20px; font-weight:700; color:var(--color-text); margin:0 0 6px; .title-icon { color:#7B61FF; } }
  .page-subtitle { font-size:13px; color:var(--color-text-muted); margin:0; }
  .btn-history { padding:7px 16px; border-radius:8px; border: 1.5px solid var(--color-border); background:var(--color-card-bg); color:var(--color-text-secondary); font-size:13px; cursor:pointer; white-space:nowrap; transition:all var(--transition);
    &:hover { border-color:#7B61FF; color:#7B61FF; } }
}
.section { margin-bottom:20px; }
.section-title { font-size:13px; font-weight:600; color:var(--color-text); margin:0 0 10px; }
.tag-grid { display:flex; flex-wrap:wrap; gap:7px; }
.tag-btn { padding:6px 14px; border-radius:7px; border: 1.5px solid var(--color-border); background:var(--color-card-bg); color:var(--color-text-secondary); font-size:12.5px; cursor:pointer; transition:all var(--transition);
  &:hover { border-color:var(--color-primary); color:#7B61FF; }
  &.active { border-color:#7B61FF; background:var(--color-active); color:#7B61FF; font-weight:500; }
  &.skill-tag { font-size:12px; padding:5px 11px; border-radius:6px; }
}
.sub-group { margin-bottom:10px; }
.sub-name { display:block; font-size:11.5px; font-weight:600; color:var(--color-text-muted); margin-bottom:6px; text-transform:uppercase; letter-spacing:.5px; }
.inline-row { display:flex; gap:36px; }
.inline-group { flex:1; }
.btn-group { display:flex; border-radius:8px; overflow:hidden; border: 1.5px solid var(--color-border); width:fit-content; }
.btn-option { padding:7px 18px; border:none; border-right:1.5px solid #e8e9ef; background:var(--color-card-bg); color:var(--color-text-secondary); font-size:13px; cursor:pointer; transition:all var(--transition);
  &:last-child { border-right:none; }
  &:hover { background:var(--color-hover); }
  &.active { background:#7B61FF; color:#fff; font-weight:500; }
}
.count-input { width:72px; height:34px; padding:0 10px; border: 1.5px solid var(--color-border); border-radius:8px; font-size:13px; text-align:center; outline:none; background:var(--color-card-bg); color:var(--color-text);
  &:focus { border-color:#7B61FF; } }
.upload-card { display:flex; flex-direction:column; align-items:center; justify-content:center; padding:18px; border: 1.5px dashed var(--color-upload-border); border-radius:10px; min-height:70px; cursor:pointer; transition:all var(--transition); background:var(--color-card-bg);
  &:hover,&.dragging { border-color:#7B61FF; background:var(--color-active); }
  &.has-file { border-style:solid; border-color:#43B88C; background:var(--color-upload-bg); }
  .upload-icon { color:var(--color-text-muted); margin-bottom:2px; }
  .upload-text { font-size:13px; color:var(--color-text-secondary); margin:0; }
  .upload-hint { font-size:11px; color:#c8ccd4; margin:4px 0 0; }
}
.file-row { display:flex; align-items:center; gap:10px; font-size:13px; color:var(--color-text); .file-name { flex:1; word-break:break-all; } .file-remove { color:#f56c6c; cursor:pointer; font-size:14px; font-weight:600; } }
.start-section { text-align:center; margin-top:24px; }
.btn-start { display:inline-flex; align-items:center; gap:8px; padding:12px 42px; border-radius:12px; border:none; background:#7B61FF; color:#fff; font-size:15px; font-weight:600; cursor:pointer; transition:all var(--transition); box-shadow:0 4px 16px rgba(123,97,255,.2);
  &:hover:not(:disabled) { background:#6a50f0; transform:translateY(-1px); box-shadow:0 6px 24px rgba(123,97,255,.3); }
  &:disabled { opacity:.4; cursor:default; box-shadow:none; } }
</style>
