<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { documentApi } from '../api/oa'
import ListPagination from './ListPagination.vue'

const props = defineProps({ permissions: { type: Array, default: () => [] } })
const documents = ref([])
const keyword = ref('')
const loading = ref(false)
const error = ref('')
const notice = ref('')
const editor = ref({ visible: false, mode: 'create', form: { title: '', content: '' }, error: '', saving: false })
const detail = ref({ visible: false, data: null, loading: false })
const permissionSet = computed(() => new Set(props.permissions))
const currentPage = ref(1)
const pageSize = 10
const paginatedDocuments = computed(() => documents.value.slice((currentPage.value - 1) * pageSize, currentPage.value * pageSize))

watch(() => documents.value.length, (total) => {
  currentPage.value = Math.min(currentPage.value, Math.max(1, Math.ceil(total / pageSize)))
})

onMounted(load)

function can(code) { return permissionSet.value.has(code) }

async function load() {
  loading.value = true
  error.value = ''
  try { const result = await documentApi.list(); documents.value = result.data || [] } catch (err) { error.value = err.message } finally { loading.value = false }
}

async function search() {
  currentPage.value = 1
  if (!keyword.value.trim()) { await load(); return }
  loading.value = true
  error.value = ''
  try { const result = await documentApi.search(keyword.value.trim()); documents.value = (result.data || []).map(cleanHighlights) } catch (err) { error.value = err.message } finally { loading.value = false }
}

function openCreate() { editor.value = { visible: true, mode: 'create', form: { title: '', content: '' }, error: '', saving: false } }
function openEdit(item) { editor.value = { visible: true, mode: 'edit', form: { docId: item.docId, title: item.title, content: item.content }, error: '', saving: false } }

async function save() {
  const state = editor.value
  state.error = ''
  state.saving = true
  try {
    const form = state.form
    await documentApi[state.mode === 'create' ? 'create' : 'update']({ ...(state.mode === 'edit' ? { docId: form.docId } : {}), title: form.title.trim(), content: form.content.trim() })
    state.visible = false
    showNotice(state.mode === 'create' ? '文档创建成功' : '文档修改成功')
    await load()
  } catch (err) { state.error = err.message } finally { state.saving = false }
}

async function openDetail(item) {
  detail.value = { visible: true, data: null, loading: true }
  try { const result = await documentApi.get(item.docId); detail.value.data = result.data } catch (err) { detail.value.visible = false; error.value = err.message } finally { detail.value.loading = false }
}

async function remove(item) {
  if (!window.confirm(`确定删除“${item.title}”吗？`)) return
  try { await documentApi.remove(item.docId); showNotice('文档已删除'); await load() } catch (err) { error.value = err.message }
}

async function rebuildIndex() {
  loading.value = true
  error.value = ''
  try { const result = await documentApi.rebuildIndex(); showNotice(result.msg || '搜索索引重建成功'); if (keyword.value) await search() } catch (err) { error.value = err.message } finally { loading.value = false }
}

function cleanHighlights(item) { return { ...item, title: stripTags(item.titleHighlight || item.title), content: stripTags(item.contentHighlight || item.content) } }
function stripTags(value) { return String(value || '').replace(/<[^>]*>/g, '') }
function preview(value, max = 130) { const text = String(value || '').replace(/\s+/g, ' ').trim(); return text.length > max ? `${text.slice(0, max)}…` : text }
function formatTime(value) { return value ? String(value).replace('T', ' ').slice(0, 16) : '—' }
function splitTags(value) { return String(value || '').split(',').map((item) => item.trim()).filter(Boolean) }
function showNotice(message) { notice.value = message; window.setTimeout(() => { if (notice.value === message) notice.value = '' }, 2600) }
</script>

<template>
  <section class="document-page">
    <div class="document-heading"><div><p class="eyebrow">DOCUMENT CENTER</p><h1>制度文档</h1><p>查找公司制度、流程规范和常用说明</p></div><div><button v-if="can('document:index')" class="index-button" :disabled="loading" @click="rebuildIndex">重建搜索索引</button><button v-if="can('document:add')" class="primary-button" @click="openCreate">＋ 新建文档</button></div></div>
    <form class="document-search" @submit.prevent="search"><span>⌕</span><input v-model.trim="keyword" placeholder="搜索制度名称或正文内容" /><button :disabled="loading">{{ loading ? '搜索中…' : '搜索文档' }}</button></form>
    <div v-if="notice" class="attendance-notice">✓ {{ notice }}</div><div v-if="error" class="api-error">{{ error }}</div>
    <div class="document-result-heading"><strong>{{ keyword ? `“${keyword}”的搜索结果` : '全部文档' }}</strong><span>共 {{ documents.length }} 篇</span></div>
    <div v-if="documents.length" class="document-grid"><article v-for="item in paginatedDocuments" :key="item.docId" class="document-card" @click="openDetail(item)"><header><span class="document-symbol">文</span><div><h2>{{ item.title }}</h2><p>{{ item.authorName || '系统' }} · {{ formatTime(item.updateTime || item.createTime) }}</p></div></header><p class="document-preview">{{ preview(item.content) || '暂无内容摘要' }}</p><div v-if="splitTags(item.tags).length" class="document-tags"><span v-for="tag in splitTags(item.tags)" :key="tag">{{ tag }}</span></div><footer><button @click.stop="openDetail(item)">阅读</button><button v-if="can('document:update')" @click.stop="openEdit(item)">编辑</button><button v-if="can('document:delete')" class="danger" @click.stop="remove(item)">删除</button></footer></article></div>
    <ListPagination v-if="documents.length" v-model:page="currentPage" :total="documents.length" :page-size="pageSize" />
    <div v-if="!documents.length && !loading" class="document-empty"><span>文</span><strong>{{ keyword ? '没有找到相关文档' : '暂无制度文档' }}</strong><p>{{ keyword ? '尝试更换关键词，或清空搜索条件。' : '管理员创建文档后会显示在这里。' }}</p></div>

    <div v-if="editor.visible" class="dialog-mask" @click.self="editor.visible = false"><form class="data-dialog document-editor" @submit.prevent="save"><div class="dialog-heading"><div><h2>{{ editor.mode === 'create' ? '新建' : '编辑' }}制度文档</h2><p>保存后将同步到全文搜索索引</p></div><button type="button" @click="editor.visible = false">×</button></div><label>文档标题<input v-model.trim="editor.form.title" maxlength="200" placeholder="请输入清晰的制度名称" required /></label><label>正文内容<textarea v-model.trim="editor.form.content" placeholder="输入制度正文、办理流程或注意事项" required></textarea></label><div v-if="editor.error" class="form-error">{{ editor.error }}</div><div class="dialog-actions"><button type="button" @click="editor.visible = false">取消</button><button class="primary-button" :disabled="editor.saving">{{ editor.saving ? '保存中…' : '保存文档' }}</button></div></form></div>
    <div v-if="detail.visible" class="dialog-mask" @click.self="detail.visible = false"><article class="data-dialog document-reader"><div class="dialog-heading"><div><p class="eyebrow">制度文档</p><h2>{{ detail.data?.title || '正在加载…' }}</h2></div><button type="button" @click="detail.visible = false">×</button></div><div v-if="detail.loading" class="empty-state">正在加载文档…</div><template v-else-if="detail.data"><div class="reader-meta"><span>{{ detail.data.authorName || '系统' }}</span><span>更新于 {{ formatTime(detail.data.updateTime || detail.data.createTime) }}</span></div><div v-if="splitTags(detail.data.tags).length" class="document-tags"><span v-for="tag in splitTags(detail.data.tags)" :key="tag">{{ tag }}</span></div><div class="reader-content">{{ detail.data.content }}</div><div class="dialog-actions"><button v-if="can('document:update')" class="primary-button" @click="detail.visible = false; openEdit(detail.data)">编辑文档</button><button @click="detail.visible = false">关闭</button></div></template></article></div>
  </section>
</template>

<style scoped>
.document-page { width: min(1400px, 100%); margin: 0 auto; padding: 38px 42px 60px; }
.document-heading { display: flex; align-items: flex-end; justify-content: space-between; gap: 18px; }.document-heading h1 { margin: 6px 0; font-size: 28px; }.document-heading > div > p:last-child { margin: 0; color: #8f959e; font-size: 12px; }.document-heading > div:last-child { display: flex; gap: 9px; }.document-heading .primary-button { margin: 0; }.index-button { height: 39px; padding: 0 15px; border: 1px solid #dfe3e9; border-radius: 9px; color: #3370ff; background: #fff; cursor: pointer; }
.document-search { height: 52px; display: grid; grid-template-columns: auto 1fr auto; align-items: center; gap: 10px; margin: 26px 0 14px; padding: 6px 7px 6px 17px; border: 1px solid #dfe3e9; border-radius: 13px; background: #fff; box-shadow: 0 8px 25px rgba(31,35,41,.045); }.document-search > span { font-size: 22px; transform: rotate(-18deg); }.document-search input { min-width: 0; border: 0; outline: 0; background: transparent; }.document-search button { height: 38px; padding: 0 18px; border: 0; border-radius: 9px; color: #fff; background: #3370ff; cursor: pointer; }
.document-result-heading { display: flex; align-items: center; justify-content: space-between; margin: 24px 0 12px; }.document-result-heading strong { font-size: 16px; }.document-result-heading span { color: #8f959e; font-size: 10px; }
.document-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 14px; }.document-card { padding: 20px; border: 1px solid #e8ebf0; border-radius: 16px; background: #fff; box-shadow: 0 7px 24px rgba(31,35,41,.04); cursor: pointer; transition: .18s ease; }.document-card:hover { transform: translateY(-2px); border-color: #cfdbfa; box-shadow: 0 12px 30px rgba(51,112,255,.08); }.document-card header { display: flex; align-items: center; gap: 12px; }.document-symbol { width: 42px; height: 42px; flex: 0 0 42px; display: grid; place-items: center; border-radius: 12px; color: #3370ff; background: #edf3ff; font-weight: 700; }.document-card h2 { margin: 0 0 5px; font-size: 16px; }.document-card header p { margin: 0; color: #a2a7ae; font-size: 9px; }.document-preview { min-height: 44px; margin: 17px 0; color: #646a73; font-size: 11px; line-height: 1.8; }.document-tags { display: flex; flex-wrap: wrap; gap: 6px; }.document-tags span { padding: 4px 8px; border-radius: 10px; color: #3370ff; background: #edf3ff; font-size: 9px; }.document-card footer { display: flex; justify-content: flex-end; gap: 3px; margin-top: 15px; padding-top: 12px; border-top: 1px solid #f0f1f3; }.document-card footer button { padding: 4px 7px; border: 0; color: #3370ff; background: transparent; cursor: pointer; }.document-card footer .danger { color: #d83931; }
.document-page :deep(.table-pagination) { min-width: 0; margin-top: 14px; border: 1px solid #e8ebf0; border-radius: 12px; background: #fff; }
.document-empty { min-height: 340px; display: flex; flex-direction: column; align-items: center; justify-content: center; color: #8f959e; text-align: center; }.document-empty > span { width: 65px; height: 65px; display: grid; place-items: center; border-radius: 20px; color: #3370ff; background: #edf3ff; font-size: 20px; font-weight: 700; }.document-empty strong { margin-top: 15px; color: #50555d; }.document-empty p { margin: 7px 0 0; font-size: 11px; }
.document-editor { width: min(760px, 100%); }.document-editor > label { display: flex; flex-direction: column; gap: 8px; margin-top: 16px; color: #50555d; font-size: 12px; font-weight: 600; }.document-editor textarea { min-height: 330px; line-height: 1.75; }.document-reader { width: min(820px, 100%); }.reader-meta { display: flex; gap: 13px; margin-bottom: 16px; color: #8f959e; font-size: 10px; }.reader-content { min-height: 250px; margin-top: 20px; color: #3f444b; font-size: 13px; line-height: 2; white-space: pre-wrap; word-break: break-word; }
@media (max-width: 760px) { .document-page { padding: 28px 17px 45px; }.document-heading { align-items: flex-start; flex-direction: column; }.document-heading > div:last-child { width: 100%; }.document-heading > div:last-child button { flex: 1; }.document-grid { grid-template-columns: 1fr; }.document-editor textarea { min-height: 240px; } }
</style>
