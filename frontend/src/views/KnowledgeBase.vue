<template>
  <div class="kb-page">
    <div class="page-header">
      <h1 class="page-title"><svg viewBox="0 0 24 24" fill="none" stroke="#7B61FF" stroke-width="2" width="20" height="20" style="vertical-align:-3px;margin-right:8px"><path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"/><path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"/></svg>知识库</h1>
      <p class="page-subtitle">上传文档构建专属向量知识库，支持语义搜索、在线编辑</p>
    </div>

    <div class="toolbar">
      <div class="search-box"><input v-model="searchQuery" class="text-input" placeholder="输入关键词语义搜索..." @keydown.enter="doSearch"/><button class="btn-action" @click="doSearch">搜索</button></div>
      <div class="upload-box">
        <label class="btn-upload" :class="{ disabled: uploading }"><input type="file" accept=".pdf,.txt,.md" @change="handleUpload" style="display:none" ref="fileInputRef" :disabled="uploading"/>{{ uploading ? '上传中...' : '上传文件' }}</label>
        <button class="btn-danger" @click="clearAll" v-if="list.length">清空</button>
      </div>
    </div>

    <div class="section" v-if="searchResults.length">
      <h3 class="section-title">搜索结果 · {{ searchResults.length }} 个文档</h3>
      <div class="kb-list"><div v-for="r in searchResults" :key="'s'+r.id" class="kb-card">
        <div class="kb-card-head"><span class="kb-file">📄 {{ r.fileName }}</span></div>
        <p class="kb-content">{{ stripMeta(r.content)?.substring(0,150) }}...</p>
        <div class="kb-card-foot"><span class="kb-date">{{ r.createdAt?.substring(0,10) }}</span>
          <button class="btn-view" @click="viewFile(r.fileName)">查看全文</button>
        </div>
      </div></div>
    </div>

    <div class="section">
      <h3 class="section-title">我的知识库 · {{ list.length }} 个文档</h3>
      <div class="kb-list" v-if="paginatedList.length"><div v-for="item in paginatedList" :key="item.id" class="kb-card">
        <div class="kb-card-head">
          <span class="kb-file">📄 {{ item.fileName }}</span>
          <button class="btn-rename" @click="startRename(item)" title="重命名">✏️</button>
        </div>
        <p class="kb-content">{{ stripMeta(item.content)?.substring(0,150) }}{{ item.content?.length > 150 ? '...' : '' }}</p>
        <div class="kb-card-foot"><span class="kb-date">{{ item.createdAt?.substring(0,10) }}</span>
          <div class="kb-actions">
            <button class="btn-view" @click="viewFile(item.fileName)">查看全文</button>
            <button class="btn-del" @click="deleteFile(item.fileName)">删除</button>
          </div>
        </div>
      </div>
      <div class="pager" v-if="totalPages > 1"><button v-for="p in totalPages" :key="p" class="page-btn" :class="{ active: currentPage === p }" @click="currentPage = p">{{ p }}</button></div></div>
      <div class="empty" v-else><p>暂无知识库</p><p class="empty-hint">上传 PDF/TXT 文件构建专属知识库</p></div>
    </div>

    <!-- 重命名弹窗 -->
    <el-dialog v-model="renameVisible" title="重命名文件" width="400px">
      <el-input v-model="renameNewName" placeholder="输入新文件名（含扩展名）"/>
      <template #footer><el-button @click="renameVisible=false">取消</el-button><el-button type="primary" @click="doRename">确认</el-button></template>
    </el-dialog>

    <!-- 文档查看/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogFileName" width="75%" destroy-on-close>
      <template v-if="dialogLoading"><div class="dialog-loading">加载中...</div></template>
      <template v-else>
        <div class="dialog-toolbar">
          <el-button size="small" @click="editing = !editing">{{ editing ? '预览' : '编辑' }}</el-button>
          <el-button size="small" type="primary" @click="saveContent" :loading="saving" v-if="editing">保存</el-button>
        </div>
        <textarea v-if="editing" v-model="editContent" class="kb-editor"></textarea>
        <div v-else class="kb-viewer" v-text="stripMeta(fileContent)"></div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import api from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'

const searchQuery = ref(''); const list = ref<any[]>([]); const searchResults = ref<any[]>([])
const fileInputRef = ref<HTMLInputElement>(); const currentPage = ref(1); const pageSize = 8
const uploading = ref(false)
const totalPages = computed(() => Math.ceil(list.value.length / pageSize))
const paginatedList = computed(() => list.value.slice((currentPage.value - 1) * pageSize, currentPage.value * pageSize))

// 文档查看/编辑
const dialogVisible = ref(false); const dialogFileName = ref(''); const dialogLoading = ref(false)
const fileContent = ref(''); const editContent = ref(''); const editing = ref(false); const saving = ref(false)
// 重命名
const renameVisible = ref(false); const renameOldName = ref(''); const renameNewName = ref('')

function stripMeta(text: string) {
  if (!text) return ''
  return text.replace(/^(fileName:|chunkIndex:|knowledgeType:|userId:).*$/gm, '').replace(/\n{3,}/g, '\n\n').trim()
}

async function fetchList() {
  try { const res: any = await api.get('/knowledge-base', { params: { knowledgeType: 'USER' } }); list.value = res.data || []; currentPage.value = 1 } catch {}
}
async function doSearch() {
  if (!searchQuery.value.trim()) return
  try { const res: any = await api.get('/knowledge-base/search', { params: { query: searchQuery.value, knowledgeType: 'USER', limit: 10 } }); searchResults.value = res.data || [] } catch {}
}
async function handleUpload(e: Event) {
  const t = e.target as HTMLInputElement; const file = t.files?.[0]; if (!file) return
  if (uploading.value) return
  uploading.value = true
  const form = new FormData(); form.append('file', file); form.append('knowledgeType', 'USER')
  try { await api.post('/knowledge-base/upload', form, { headers: { 'Content-Type': 'multipart/form-data' } }); ElMessage.success('上传成功'); fetchList() } catch {}
  uploading.value = false
  t.value = ''
}
async function deleteFile(fileName: string) {
  try { await api.delete('/knowledge-base/file', { params: { fileName } }); ElMessage.success('已删除'); fetchList() } catch {}
}
async function clearAll() {
  try { await ElMessageBox.confirm('确定清空所有知识库？', '确认', { type: 'warning' }); await api.delete('/knowledge-base/clear'); ElMessage.success('已清空'); fetchList() } catch {}
}

// 查看文件
async function viewFile(fileName: string) {
  dialogFileName.value = fileName; dialogVisible.value = true; dialogLoading.value = true; editing.value = false
  try {
    const res: any = await api.get('/knowledge-base/file', { params: { fileName } })
    const raw = res.data?.content || ''; fileContent.value = stripMeta(raw); editContent.value = fileContent.value
  } catch { fileContent.value = '加载失败'; editContent.value = '' }
  dialogLoading.value = false
}
// 保存编辑
async function saveContent() {
  saving.value = true
  try {
    await api.put('/knowledge-base/file', { content: editContent.value }, { params: { fileName: dialogFileName.value } })
    ElMessage.success('已保存并重新向量化'); dialogVisible.value = false; fetchList()
  } catch {} finally { saving.value = false }
}
// 重命名
function startRename(item: any) { renameOldName.value = item.fileName; renameNewName.value = item.fileName; renameVisible.value = true }
async function doRename() {
  if (!renameNewName.value.trim()) return
  try { await api.put('/knowledge-base/rename', { oldName: renameOldName.value, newName: renameNewName.value }); ElMessage.success('已重命名'); renameVisible.value = false; fetchList() } catch {}
}

onMounted(fetchList)
</script>

<style lang="scss" scoped>
.kb-page { max-width: 860px; margin: 0 auto; padding-bottom: 40px; }
.page-header { margin-bottom: 22px; .page-title { font-size: 20px; font-weight:700; color:var(--color-text); margin:0 0 6px; } .page-subtitle { font-size:13px; color:var(--color-text-muted); margin:0; } }
.toolbar { display: flex; gap: 12px; margin-bottom: 20px; align-items: center; }
.search-box { flex: 1; display: flex; gap: 8px; }
.text-input { flex: 1; height: 38px; padding: 0 14px; border: 1.5px solid var(--color-border); border-radius: 9px; font-size: 13px; outline: none; background: var(--color-input); color: var(--color-text); &:focus { border-color: #7B61FF; } }
.btn-action { padding: 0 18px; border: none; border-radius: 9px; background: #7B61FF; color: #fff; font-size: 13px; cursor: pointer; &:hover { background: #6a50f0; } }
.upload-box { display: flex; gap: 8px; }
.btn-upload { display: inline-flex; align-items: center; padding: 0 18px; height: 38px; border-radius: 9px; border: 1.5px solid #7B61FF; background: #f8f6ff; color: #7B61FF; font-size: 13px; cursor: pointer; &:hover { background: #f0edff; } &.disabled { opacity: .5; cursor: not-allowed; pointer-events: none; } }
.btn-danger { padding: 0 16px; border: 1.5px solid #f56c6c; border-radius: 9px; background: #fff; color: #f56c6c; font-size: 12px; cursor: pointer; &:hover { background: #fef0f0; } }
.section { margin-bottom: 22px; }
.section-title { font-size: 13px; font-weight: 600; color: var(--color-text); margin: 0 0 10px; }
.kb-list { display: flex; flex-direction: column; gap: 8px; }
.kb-card { padding: 12px 16px; border-radius: 10px; border: 1.5px solid var(--color-border); background: var(--color-card); }
.kb-card-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 6px; }
.kb-file { font-size: 14px; font-weight: 600; color: var(--color-text); }
.btn-rename { border: none; background: none; cursor: pointer; font-size: 14px; opacity: .4; &:hover { opacity: 1; } }
.kb-content { font-size: 12px; color: var(--color-text-secondary); line-height: 1.5; margin: 0 0 8px; word-break: break-all; }
.kb-card-foot { display: flex; justify-content: space-between; align-items: center; }
.kb-date { font-size: 11px; color: var(--color-text-muted); }
.kb-actions { display: flex; gap: 8px; }
.btn-view { border: 1.5px solid #7B61FF; border-radius: 6px; background: #fff; color: #7B61FF; font-size: 11px; cursor: pointer; padding: 2px 10px; &:hover { background: #f0edff; } }
.btn-del { border: none; background: none; color: #f56c6c; font-size: 11px; cursor: pointer; }
.pager { display: flex; gap: 6px; margin-top: 12px; justify-content: center; }
.page-btn { width: 30px; height: 30px; border-radius: 6px; border: 1.5px solid var(--color-border); background: var(--color-card); color: var(--color-text-secondary); font-size: 12px; cursor: pointer; &.active { background: #7B61FF; color: #fff; border-color: #7B61FF; } }
.empty { text-align: center; padding: 40px; p { color: var(--color-text-muted); font-size: 13px; } .empty-hint { font-size: 12px; opacity: .6; } }
.dialog-loading { text-align: center; padding: 60px; color: var(--color-text-muted); }
.dialog-toolbar { margin-bottom: 12px; display: flex; gap: 8px; justify-content: flex-end; }
.kb-editor { width: 100%; min-height: 400px; padding: 16px; border: 1.5px solid var(--color-border); border-radius: 10px; font-size: 14px; font-family: inherit; resize: vertical; background: var(--color-input); color: var(--color-text); }
.kb-viewer { padding: 16px; background: var(--color-bubble-ai); border-radius: 10px; max-height: 500px; overflow-y: auto; white-space: pre-wrap; line-height: 1.8; }
</style>
