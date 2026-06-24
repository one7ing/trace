<template>
  <div class="kb-page">
    <div class="page-header">
      <h1 class="page-title"><svg viewBox="0 0 24 24" fill="none" stroke="#7B61FF" stroke-width="2" width="20" height="20" style="vertical-align:-3px;margin-right:8px"><path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"/><path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"/></svg>知识库</h1>
      <p class="page-subtitle">上传文档构建专属知识库，支持名称搜索、分类管理</p>
    </div>

    <div class="toolbar">
      <div class="search-box"><input v-model="searchQuery" class="text-input" placeholder="按名称搜索知识库..." @keydown.enter="doSearch"/><button class="btn-action" @click="doSearch">搜索</button></div>
      <div class="upload-box">
        <el-select v-model="uploadCategory" style="width:150px; height:38px" placeholder="分类">
          <el-option label="专业知识问答" value="专业知识问答"/>
          <el-option label="闲聊问答" value="闲聊问答"/>
        </el-select>
        <label class="btn-upload" :class="{ disabled: uploading }"><input type="file" accept=".pdf,.txt,.md" @change="handleUpload" style="display:none" ref="fileInputRef" :disabled="uploading"/>{{ uploading ? '上传中...' : '上传文件' }}</label>
        <button class="btn-danger" @click="clearAll" v-if="total > 0">清空</button>
      </div>
    </div>

    <!-- 搜索结果 -->
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

    <!-- 卡片网格视图 -->
    <div class="section">
      <h3 class="section-title">我的知识库 · {{ total }} 个文档</h3>

      <div class="kb-card-grid" v-if="tableData.length">
        <div v-for="row in tableData" :key="row.id" class="kb-card">
          <div class="kb-card-icon">
            <span class="kb-icon">📄</span>
            <span class="kb-category-tag" :class="{ chat: row.category === '闲聊问答' }">{{ row.category || '专业知识问答' }}</span>
          </div>
          <div class="kb-card-body">
            <template v-if="editingRowId === row.id">
              <el-input v-model="editName" size="small" @keydown.enter="saveName(row)" @blur="saveName(row)" ref="nameInputRef" style="width:100%"/>
            </template>
            <template v-else>
              <span class="kb-card-name" @dblclick="startEditName(row)">{{ row.fileName }}</span>
            </template>
            <span class="kb-card-date">{{ row.createdAt?.substring(0,10) }}</span>
          </div>
          <div class="kb-card-actions">
            <button class="btn-edit-icon" @click="startEditName(row)" title="编辑名称">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="13" height="13"><path d="M12 20h9"/><path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z"/></svg>
            </button>
            <button class="btn-view" @click="viewFile(row.fileName)">查看</button>
            <button class="btn-del" @click="deleteFile(row.fileName)">删除</button>
          </div>
        </div>
      </div>

      <!-- 分页 -->
      <div class="pager" v-if="tableData.length && totalPages > 1">
        <button v-for="p in totalPages" :key="p" class="page-btn" :class="{ active: currentPage === p - 1 }" @click="currentPage = p - 1; fetchItems()">{{ p }}</button>
      </div>

      <div class="empty" v-if="!tableData.length"><p>暂无知识库</p><p class="empty-hint">上传 PDF/TXT 文件构建专属知识库</p></div>
    </div>

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
import { ref, computed, onMounted, nextTick } from 'vue'
import api from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'

// ── 搜索 & 上传 ──
const searchQuery = ref('')
const searchResults = ref<any[]>([])
const fileInputRef = ref<HTMLInputElement>()
const uploading = ref(false)
const uploadCategory = ref('专业知识问答')

// ── 表格数据 ──
const tableData = ref<any[]>([])
const currentPage = ref(0)
const pageSize = 8
const total = ref(0)
const totalPages = computed(() => Math.ceil(total.value / pageSize))

// ── 行内编辑 ──
const editingRowId = ref<number | null>(null)
const editName = ref('')
const nameInputRef = ref<any>()

// ── 文档查看/编辑 ──
const dialogVisible = ref(false); const dialogFileName = ref(''); const dialogLoading = ref(false)
const fileContent = ref(''); const editContent = ref(''); const editing = ref(false); const saving = ref(false)

// ── 工具 ──
function stripMeta(text: string) {
  if (!text) return ''
  return text.replace(/^(fileName:|chunkIndex:|knowledgeType:|userId:).*$/gm, '').replace(/\n{3,}/g, '\n\n').trim()
}

// ── 数据加载 ──
async function fetchItems() {
  try {
    const res: any = await api.get('/knowledge-base/items', {
      params: { category: '', page: currentPage.value, size: pageSize }
    })
    const page = res.data
    tableData.value = (page.records || []).map((item: any) => ({ ...item }))
    total.value = page.total || 0
  } catch {}
}

// ── 搜索 ──
async function doSearch() {
  if (!searchQuery.value.trim()) return
  try {
    const res: any = await api.get('/knowledge-base/search', {
      params: { query: searchQuery.value, limit: 10 }
    })
    searchResults.value = res.data || []
  } catch {}
}

// ── 上传 ──
async function handleUpload(e: Event) {
  const t = e.target as HTMLInputElement; const file = t.files?.[0]; if (!file) return
  if (uploading.value) return
  uploading.value = true
  const form = new FormData(); form.append('file', file); form.append('category', uploadCategory.value)
  try {
    await api.post('/knowledge-base/upload', form, { headers: { 'Content-Type': 'multipart/form-data' } })
    ElMessage.success('上传成功')
    fetchItems()
  } catch {} finally {
    uploading.value = false; t.value = ''
  }
}

// ── 删除 ──
async function deleteFile(fileName: string) {
  try {
    await api.delete('/knowledge-base/file', { params: { fileName } })
    ElMessage.success('已删除')
    fetchItems()
  } catch {}
}
async function clearAll() {
  try {
    await ElMessageBox.confirm('确定清空所有知识库？', '确认', { type: 'warning' })
    await api.delete('/knowledge-base/clear')
    ElMessage.success('已清空')
    fetchItems()
  } catch {}
}

// ── 名称编辑（同时更新文件名）──
function startEditName(row: any) {
  editingRowId.value = row.id
  editName.value = row.fileName
  nextTick(() => { nameInputRef.value?.focus() })
}
async function saveName(row: any) {
  if (editingRowId.value === null) return
  const newName = editName.value.trim()
  if (!newName || newName === row.fileName) {
    editingRowId.value = null; return
  }
  try {
    await api.put(`/knowledge-base/${row.id}/name`, { name: newName })
    row.fileName = newName
    ElMessage.success('名称已更新')
  } catch {} finally {
    editingRowId.value = null
  }
}

// ── 查看文件 ──
async function viewFile(fileName: string) {
  dialogFileName.value = fileName; dialogVisible.value = true; dialogLoading.value = true; editing.value = false
  try {
    const res: any = await api.get('/knowledge-base/file', { params: { fileName } })
    const raw = res.data?.content || ''; fileContent.value = stripMeta(raw); editContent.value = fileContent.value
  } catch { fileContent.value = '加载失败'; editContent.value = '' }
  dialogLoading.value = false
}
async function saveContent() {
  saving.value = true
  try {
    await api.put('/knowledge-base/file', { content: editContent.value }, { params: { fileName: dialogFileName.value } })
    ElMessage.success('已保存并重新向量化'); dialogVisible.value = false
    fetchItems()
  } catch {} finally { saving.value = false }
}

onMounted(() => { fetchItems() })
</script>

<style lang="scss" scoped>
.kb-page { max-width: 900px; margin: 0 auto; padding-bottom: 40px; }
.page-header { margin-bottom: 22px; .page-title { font-size: 20px; font-weight:700; color:var(--color-text); margin:0 0 6px; } .page-subtitle { font-size:13px; color:var(--color-text-muted); margin:0; } }
.toolbar { display: flex; gap: 12px; margin-bottom: 20px; align-items: center; }
.search-box { flex: 1; display: flex; gap: 8px; }
.text-input { flex: 1; height: 38px; padding: 0 14px; border: 1.5px solid var(--color-border); border-radius: 9px; font-size: 13px; outline: none; background: var(--color-input); color: var(--color-text); &:focus { border-color: #7B61FF; } }
.btn-action { padding: 0 18px; border: none; border-radius: 9px; background: #7B61FF; color: #fff; font-size: 13px; cursor: pointer; &:hover { background: #6a50f0; } }
.upload-box { display: flex; gap: 8px; align-items: center;
  :deep(.el-select) { .el-input__wrapper { height: 38px; } }
}
.btn-upload { display: inline-flex; align-items: center; padding: 0 18px; height: 38px; border-radius: 9px; border: 1.5px solid #7B61FF; background: #f8f6ff; color: #7B61FF; font-size: 13px; cursor: pointer; &:hover { background: #f0edff; } &.disabled { opacity: .5; cursor: not-allowed; pointer-events: none; } }
.btn-danger { display: inline-flex; align-items: center; padding: 0 16px; height: 38px; border: 1.5px solid #f56c6c; border-radius: 9px; background: #fff; color: #f56c6c; font-size: 12px; cursor: pointer; &:hover { background: #fef0f0; } }
.section { margin-bottom: 22px; }
.section-title { font-size: 13px; font-weight: 600; color: var(--color-text); margin: 0 0 10px; }

// ── 卡片网格 ──
.kb-card-grid {
  display: grid; grid-template-columns: repeat(auto-fill, minmax(260px, 1fr)); gap: 12px;
}
.kb-card {
  background: var(--color-card); border: 1px solid var(--color-border);
  border-radius: var(--radius-lg); padding: 16px;
  display: flex; align-items: center; gap: 14px;
  box-shadow: var(--shadow-xs); transition: all var(--transition);
  &:hover { box-shadow: var(--shadow-sm); transform: translateY(-1px); }
  .kb-card-icon {
    display: flex; flex-direction: column; align-items: center; gap: 6px; flex-shrink: 0;
    .kb-icon { font-size: 28px; }
    .kb-category-tag {
      font-size: 10px; padding: 1px 8px; border-radius: 8px;
      background: var(--color-active); color: #7B61FF;
      &.chat { background: #fef7e0; color: #d09030; }
    }
  }
  .kb-card-body { flex: 1; min-width: 0;
    .kb-card-name { font-size: 13px; font-weight: 600; color: var(--color-text); display: block; cursor: pointer;
      white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
      &:hover { color: var(--color-primary); }
    }
    .kb-card-date { font-size: 11px; color: var(--color-text-muted); display: block; margin-top: 4px; }
  }
  .kb-card-actions { display: flex; flex-direction: column; gap: 4px; flex-shrink: 0; }
}

.name-cell { display: flex; align-items: center; gap: 8px; }
.name-text { font-size: 13px; color: var(--color-text); font-weight: 500; }
.btn-edit-icon {
  border: none; background: none; cursor: pointer; color: var(--color-text-muted);
  padding: 3px; border-radius: 4px; display: inline-flex; align-items: center;
  transition: all var(--transition);
  &:hover { color: var(--color-primary); background: var(--color-primary-bg); }
}

.btn-view { border: 1.5px solid var(--color-primary); border-radius: 6px; background: transparent; color: var(--color-primary); font-size: 11px; cursor: pointer; padding: 3px 10px; transition: all var(--transition);
  &:hover { background: var(--color-primary-bg); } }
.btn-del { border: none; background: none; color: #f56c6c; font-size: 11px; cursor: pointer; padding: 3px 6px; border-radius: 4px; transition: all var(--transition);
  &:hover { background: #fef0f0; } }

.pager { display: flex; gap: 6px; margin-top: 14px; justify-content: center; padding-bottom: 8px; }
.page-btn { width: 30px; height: 30px; border-radius: 6px; border: 1.5px solid var(--color-border); background: var(--color-card); color: var(--color-text-secondary); font-size: 12px; cursor: pointer; &.active { background: #7B61FF; color: #fff; border-color: #7B61FF; } }

.empty { text-align: center; padding: 40px; p { color: var(--color-text-muted); font-size: 13px; } .empty-hint { font-size: 12px; opacity: .6; } }

.kb-list { display: flex; flex-direction: column; gap: 8px; }
.kb-card { padding: 12px 16px; border-radius: 10px; border: 1.5px solid var(--color-border); background: var(--color-card); }
.kb-card-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 6px; }
.kb-file { font-size: 14px; font-weight: 600; color: var(--color-text); }
.kb-content { font-size: 12px; color: var(--color-text-secondary); line-height: 1.5; margin: 0 0 8px; word-break: break-all; }
.kb-card-foot { display: flex; justify-content: space-between; align-items: center; }
.kb-date { font-size: 11px; color: var(--color-text-muted); }

.dialog-loading { text-align: center; padding: 60px; color: var(--color-text-muted); }
.dialog-toolbar { margin-bottom: 12px; display: flex; gap: 8px; justify-content: flex-end; }
.kb-editor { width: 100%; min-height: 400px; padding: 16px; border: 1.5px solid var(--color-border); border-radius: 10px; font-size: 14px; font-family: inherit; resize: vertical; background: var(--color-input); color: var(--color-text); }
.kb-viewer { padding: 16px; background: var(--color-bubble-ai); border-radius: 10px; max-height: 500px; overflow-y: auto; white-space: pre-wrap; line-height: 1.8; }
</style>
