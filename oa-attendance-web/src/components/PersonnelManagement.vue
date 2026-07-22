<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { departmentApi, positionApi, userApi } from '../api/oa'

const props = defineProps({ section: { type: String, required: true }, currentUser: { type: Object, default: null }, permissions: { type: Array, default: () => [] } })
defineEmits(['back'])

const keyword = ref('')
const deptFilter = ref('')
const statusFilter = ref('')
const genderFilter = ref('')
const sort = ref('default')
const currentPage = ref(1)
const pageSize = 10
const departmentView = ref('tree')
const expandedIds = ref(new Set())
const loading = ref(false)
const error = ref('')
const notice = ref('')
const records = ref([])
const departments = ref([])
const positions = ref([])
const users = ref([])
const dialog = ref({ visible: false, mode: 'create', form: {}, error: '' })

const roleCode = computed(() => props.currentUser?.roleCode || '')
const permissionSet = computed(() => new Set(props.permissions))
const resource = computed(() => ({ 员工管理: 'user', 部门管理: 'department', 职位管理: 'position' })[props.section])
const filteredRecords = computed(() => {
  const search = keyword.value.trim().toLowerCase()
  return records.value.filter((item) => {
    if (search && !JSON.stringify(item).toLowerCase().includes(search)) return false
    if (props.section === '员工管理') {
      if (deptFilter.value !== '' && Number(item.deptId) !== Number(deptFilter.value)) return false
      if (statusFilter.value !== '' && Number(item.status) !== Number(statusFilter.value)) return false
      if (genderFilter.value !== '' && item.gender !== genderFilter.value) return false
    }
    return true
  })
})
const totalPages = computed(() => Math.max(1, Math.ceil(filteredRecords.value.length / pageSize)))
const sortedEmployees = computed(() => {
  const result = [...filteredRecords.value]
  const compare = (left, right) => String(left || '').localeCompare(String(right || ''), 'zh-CN', { numeric: true })
  const sorters = {
    employeeNoAsc: (a, b) => compare(a.employeeNo, b.employeeNo), employeeNoDesc: (a, b) => compare(b.employeeNo, a.employeeNo),
    nameAsc: (a, b) => compare(a.realName, b.realName), hireDateDesc: (a, b) => compare(b.hireDate, a.hireDate), hireDateAsc: (a, b) => compare(a.hireDate, b.hireDate),
  }
  return sorters[sort.value] ? result.sort(sorters[sort.value]) : result
})
const paginatedEmployees = computed(() => sortedEmployees.value.slice((currentPage.value - 1) * pageSize, currentPage.value * pageSize))
const treeRows = computed(() => {
  const source = filteredRecords.value
  const children = new Map()
  source.forEach((item) => {
    const parentId = Number(item.parentId || 0)
    if (!children.has(parentId)) children.set(parentId, [])
    children.get(parentId).push(item)
  })
  const rows = []
  const visit = (parentId, depth) => (children.get(Number(parentId)) || []).forEach((item) => {
    const childCount = (children.get(Number(item.deptId)) || []).length
    const expanded = expandedIds.value.has(Number(item.deptId))
    rows.push({ ...item, depth, childCount, expanded })
    if (expanded) visit(item.deptId, depth + 1)
  })
  visit(0, 0)
  source.filter((item) => !rows.some((row) => Number(row.deptId) === Number(item.deptId))).forEach((item) => rows.push({ ...item, depth: 0, childCount: 0 }))
  return rows
})

function hasPermission(code) { return permissionSet.value.has(code) }
function can(action) { return hasPermission(`${resource.value}:${action}`) }
function canEdit(item) {
  if (!can('update')) return false
  if (roleCode.value === 'ADMIN') return true
  return ['员工管理', '职位管理'].includes(props.section) && roleCode.value === 'MANAGER' && Number(item.deptId) === Number(props.currentUser?.deptId)
}
function canDelete(item) {
  if (!can('delete')) return false
  if (props.section === '员工管理' && Number(item.userId) === Number(props.currentUser?.userId)) return false
  return roleCode.value === 'ADMIN'
}

async function loadReferenceData() {
  const [departmentResult, positionResult] = await Promise.all([departmentApi.list(), positionApi.list()])
  departments.value = departmentResult.data || []
  positions.value = positionResult.data || []
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    if (props.section === '员工管理') {
      const [result] = await Promise.all([userApi.list(), loadReferenceData()])
      records.value = result.data || []
      users.value = records.value
    } else if (props.section === '部门管理') {
      const requests = [departmentApi.list()]
      if (hasPermission('user:query')) requests.push(userApi.list())
      const [result, userResult] = await Promise.all(requests)
      records.value = result.data || []
      departments.value = records.value
      expandedIds.value = new Set(records.value.map((item) => Number(item.deptId)))
      users.value = userResult?.data || []
    } else {
      const [positionResult, departmentResult] = await Promise.all([positionApi.list(), departmentApi.list()])
      records.value = positionResult.data || []
      positions.value = records.value
      departments.value = departmentResult.data || []
    }
  } catch (requestError) {
    error.value = requestError.message
    records.value = []
  } finally {
    loading.value = false
  }
}

function openCreate() {
  if (!can('add')) return
  const defaults = props.section === '员工管理'
    ? { employeeNo: '', username: '', password: '123456', realName: '', nickname: '', gender: '男', phone: '', email: '', birthDate: '', hireDate: '', deptId: roleCode.value === 'MANAGER' ? props.currentUser?.deptId : '', positionId: '', roleId: 1, status: 1 }
    : props.section === '部门管理' ? { parentId: 0, deptName: '', deptCode: '', leaderId: null, description: '', status: 1 } : { positionName: '', deptId: '', level: 1, description: '' }
  dialog.value = { visible: true, mode: 'create', form: defaults, error: '' }
}
function openEdit(item) { if (canEdit(item)) dialog.value = { visible: true, mode: 'edit', form: { ...item }, error: '' } }
function optionalNumber(value) { return value === '' || value == null ? null : Number(value) }
function buildPayload() {
  const form = dialog.value.form
  if (props.section === '员工管理') {
    const common = { realName: form.realName?.trim(), nickname: form.nickname?.trim() || null, gender: form.gender || null, phone: form.phone?.trim() || null, email: form.email?.trim() || null, birthDate: form.birthDate || null, hireDate: form.hireDate || null, deptId: Number(form.deptId), positionId: optionalNumber(form.positionId), roleId: Number(form.roleId), status: Number(form.status) }
    return dialog.value.mode === 'create' ? { ...common, employeeNo: form.employeeNo?.trim(), username: form.username?.trim(), password: form.password } : { ...common, userId: Number(form.userId) }
  }
  if (props.section === '部门管理') return { ...(dialog.value.mode === 'edit' ? { deptId: Number(form.deptId) } : {}), parentId: optionalNumber(form.parentId) ?? 0, deptName: form.deptName?.trim(), deptCode: form.deptCode?.trim(), leaderId: optionalNumber(form.leaderId), description: form.description?.trim() || null, status: Number(form.status) }
  return { ...(dialog.value.mode === 'edit' ? { positionId: Number(form.positionId) } : {}), positionName: form.positionName?.trim(), deptId: Number(form.deptId), level: optionalNumber(form.level), description: form.description?.trim() || null }
}
async function save() {
  loading.value = true
  dialog.value.error = ''
  try {
    const api = props.section === '员工管理' ? userApi : props.section === '部门管理' ? departmentApi : positionApi
    await api[dialog.value.mode === 'create' ? 'create' : 'update'](buildPayload())
    const action = dialog.value.mode === 'create' ? '新增' : '修改'
    dialog.value.visible = false
    showNotice(`${props.section.replace('管理', '')}${action}成功`)
    await load()
  } catch (requestError) { dialog.value.error = requestError.message } finally { loading.value = false }
}
async function remove(item) {
  if (!canDelete(item) || !window.confirm(`确定删除“${item.realName || item.deptName || item.positionName}”吗？`)) return
  try {
    const api = props.section === '员工管理' ? userApi : props.section === '部门管理' ? departmentApi : positionApi
    await api.remove(item.userId || item.deptId || item.positionId)
    showNotice('删除成功')
    await load()
  } catch (requestError) { error.value = requestError.message }
}
async function resetPassword(item) {
  if (!canEdit(item) || Number(item.userId) === Number(props.currentUser?.userId) || !window.confirm(`确定将“${item.realName}”的密码重置为 123456 吗？`)) return
  try { await userApi.resetPassword(item.userId); showNotice('密码已重置为 123456') } catch (requestError) { error.value = requestError.message }
}
function showNotice(value) { notice.value = value; window.setTimeout(() => { if (notice.value === value) notice.value = '' }, 2600) }
function toggleNode(deptId) { const next = new Set(expandedIds.value); const id = Number(deptId); next.has(id) ? next.delete(id) : next.add(id); expandedIds.value = next }
function departmentName(id) { return departments.value.find((item) => Number(item.deptId) === Number(id))?.deptName || '—' }

watch([keyword, deptFilter, statusFilter, genderFilter, sort], () => { currentPage.value = 1 })
watch(filteredRecords, () => { if (currentPage.value > totalPages.value) currentPage.value = totalPages.value })
watch(() => props.section, () => { keyword.value = ''; deptFilter.value = ''; statusFilter.value = ''; genderFilter.value = ''; sort.value = 'default'; currentPage.value = 1; load() })
onMounted(load)
</script>

<template>
  <section class="management-page">
    <div class="management-heading"><div><button class="back-button" @click="$emit('back')">← 返回工作台</button><h1>{{ section }}</h1></div><button v-if="can('add')" class="primary-button" @click="openCreate">＋ 新增{{ section.replace('管理', '') }}</button></div>
    <div class="table-toolbar"><input v-model="keyword" :placeholder="`搜索${section.replace('管理', '')}`" /><template v-if="section === '员工管理'"><select v-model="deptFilter"><option value="">全部部门</option><option v-for="item in departments" :key="item.deptId" :value="item.deptId">{{ item.deptName }}</option></select><select v-model="statusFilter"><option value="">全部状态</option><option value="1">在职</option><option value="0">离职</option></select><select v-model="genderFilter"><option value="">全部性别</option><option value="男">男</option><option value="女">女</option></select><select v-model="sort" aria-label="员工排序"><option value="default">默认排序</option><option value="employeeNoAsc">工号：升序</option><option value="employeeNoDesc">工号：降序</option><option value="nameAsc">姓名：A–Z</option><option value="hireDateDesc">入职日期：最新优先</option><option value="hireDateAsc">入职日期：最早优先</option></select></template><div v-if="section === '部门管理'" class="department-view-switch"><button :class="{ active: departmentView === 'tree' }" @click="departmentView = 'tree'">组织树</button><button :class="{ active: departmentView === 'list' }" @click="departmentView = 'list'">列表</button></div><button class="refresh-button" :disabled="loading" title="刷新数据" @click="load"><span>↻</span>{{ loading ? '刷新中' : '刷新' }}</button></div>
    <div v-if="error" class="api-error">{{ error }}</div>
    <div class="data-panel"><div v-if="loading" class="empty-state">正在加载数据…</div>
      <table v-else-if="section === '员工管理'"><thead><tr><th>工号</th><th>姓名</th><th>性别</th><th>部门</th><th>职位</th><th>手机号</th><th>入职日期</th><th>状态</th><th>操作</th></tr></thead><tbody><tr v-for="item in paginatedEmployees" :key="item.userId"><td>{{ item.employeeNo }}</td><td class="name-cell"><span class="table-avatar">{{ item.realName?.slice(0,1) }}</span>{{ item.realName }}</td><td>{{ item.gender || '—' }}</td><td>{{ item.deptName || '—' }}</td><td>{{ item.positionName || '—' }}</td><td>{{ item.phone || '—' }}</td><td>{{ item.hireDate || '—' }}</td><td><span :class="['status-tag', item.status ? 'on' : 'off']">{{ item.status ? '在职' : '离职' }}</span></td><td class="actions-cell"><button v-if="canEdit(item)" class="text-action" @click="openEdit(item)">编辑</button><button v-if="canEdit(item) && Number(item.userId) !== Number(currentUser?.userId)" class="text-action" @click="resetPassword(item)">重置密码</button><button v-if="canDelete(item)" class="text-action danger" @click="remove(item)">删除</button><span v-if="!canEdit(item) && !canDelete(item)" class="no-action">—</span></td></tr></tbody></table>
      <div v-if="!loading && section === '员工管理' && filteredRecords.length" class="table-pagination"><span>第 {{ currentPage }} 页 / 共 {{ filteredRecords.length }} 条</span><div class="pagination-actions"><button :disabled="currentPage === 1" aria-label="上一页" @click="currentPage--">‹</button><strong>{{ currentPage }}</strong><button :disabled="currentPage === totalPages" aria-label="下一页" @click="currentPage++">›</button></div></div>
      <table v-else-if="section === '部门管理' && departmentView === 'list'"><thead><tr><th>部门编码</th><th>部门名称</th><th>上级部门</th><th>负责人</th><th>说明</th><th>状态</th><th>操作</th></tr></thead><tbody><tr v-for="item in filteredRecords" :key="item.deptId"><td>{{ item.deptCode }}</td><td class="name-cell">{{ item.deptName }}</td><td>{{ item.parentDeptName || '—' }}</td><td>{{ item.leaderName || '—' }}</td><td>{{ item.description || '—' }}</td><td><span :class="['status-tag', item.status ? 'on' : 'off']">{{ item.status ? '启用' : '禁用' }}</span></td><td><button v-if="canEdit(item)" class="text-action" @click="openEdit(item)">编辑</button><button v-if="canDelete(item)" class="text-action danger" @click="remove(item)">删除</button><span v-if="!canEdit(item) && !canDelete(item)" class="no-action">—</span></td></tr></tbody></table>
      <table v-else-if="section === '部门管理'" class="department-tree-table"><thead><tr><th>组织架构</th><th>部门编码</th><th>负责人</th><th>下级部门</th><th>说明</th><th>状态</th><th>操作</th></tr></thead><tbody><tr v-for="item in treeRows" :key="item.deptId"><td class="name-cell"><span class="tree-indent" :style="{ width: `${item.depth * 28}px` }"></span><button v-if="item.childCount" class="tree-toggle" :class="{ expanded: item.expanded }" @click="toggleNode(item.deptId)">›</button><span v-else class="tree-toggle-placeholder"></span><span class="department-tree-icon">部</span>{{ item.deptName }}</td><td>{{ item.deptCode }}</td><td>{{ item.leaderName || '—' }}</td><td>{{ item.childCount ? `${item.childCount} 个` : '—' }}</td><td>{{ item.description || '—' }}</td><td><span :class="['status-tag', item.status ? 'on' : 'off']">{{ item.status ? '启用' : '禁用' }}</span></td><td><button v-if="canEdit(item)" class="text-action" @click="openEdit(item)">编辑</button><button v-if="canDelete(item)" class="text-action danger" @click="remove(item)">删除</button><span v-if="!canEdit(item) && !canDelete(item)" class="no-action">—</span></td></tr></tbody></table>
      <table v-else><thead><tr><th>职位名称</th><th>所属部门</th><th>职位等级</th><th>职位说明</th><th>操作</th></tr></thead><tbody><tr v-for="item in filteredRecords" :key="item.positionId"><td class="name-cell">{{ item.positionName }}</td><td>{{ item.deptName || departmentName(item.deptId) }}</td><td>等级 {{ item.level }}</td><td>{{ item.description || '—' }}</td><td><button v-if="canEdit(item)" class="text-action" @click="openEdit(item)">编辑</button><button v-if="canDelete(item)" class="text-action danger" @click="remove(item)">删除</button><span v-if="!canEdit(item) && !canDelete(item)" class="no-action">—</span></td></tr></tbody></table>
      <div v-if="!loading && !filteredRecords.length" class="empty-state">暂无数据</div>
    </div>
  </section>

  <div v-if="dialog.visible" class="dialog-mask" @click.self="dialog.visible = false"><form class="data-dialog" @submit.prevent="save">
    <div class="dialog-heading"><div><h2>{{ dialog.mode === 'create' ? '新增' : '编辑' }}{{ section.replace('管理', '') }}</h2><p>填写完成后保存到后端数据库</p></div><button type="button" @click="dialog.visible = false">×</button></div>
    <div v-if="section === '员工管理'" class="form-grid"><label v-if="dialog.mode === 'create'">工号<input v-model="dialog.form.employeeNo" required /></label><label v-if="dialog.mode === 'create'">登录账号<input v-model="dialog.form.username" required /></label><label v-if="dialog.mode === 'create'">初始密码<input v-model="dialog.form.password" type="password" required /></label><label>姓名<input v-model.trim="dialog.form.realName" required /></label><label>昵称<input v-model.trim="dialog.form.nickname" /></label><label>性别<select v-model="dialog.form.gender"><option value="">未设置</option><option>男</option><option>女</option></select></label><label>部门<select v-model.number="dialog.form.deptId" :disabled="roleCode === 'MANAGER'" required><option value="" disabled>请选择</option><option v-for="item in departments" :key="item.deptId" :value="item.deptId">{{ item.deptName }}</option></select></label><label>职位<select v-model.number="dialog.form.positionId"><option value="">暂不设置</option><option v-for="item in positions.filter((p) => !dialog.form.deptId || Number(p.deptId) === Number(dialog.form.deptId))" :key="item.positionId" :value="item.positionId">{{ item.positionName }}</option></select></label><label>手机号<input v-model.trim="dialog.form.phone" inputmode="tel" pattern="1[0-9]{10}" placeholder="11 位手机号" /></label><label>邮箱<input v-model.trim="dialog.form.email" type="email" /></label><label>出生日期<input v-model="dialog.form.birthDate" type="date" /></label><label>入职日期<input v-model="dialog.form.hireDate" type="date" /></label><label>角色<select v-model.number="dialog.form.roleId" :disabled="roleCode === 'MANAGER'"><option :value="1">员工</option><option v-if="roleCode === 'ADMIN'" :value="2">主管</option><option v-if="roleCode === 'ADMIN'" :value="3">管理员</option></select></label><label>状态<select v-model.number="dialog.form.status"><option :value="1">在职</option><option :value="0">离职</option></select></label></div>
    <div v-else-if="section === '部门管理'" class="form-grid"><label>部门名称<input v-model="dialog.form.deptName" required /></label><label>部门编码<input v-model="dialog.form.deptCode" required /></label><label>上级部门<select v-model.number="dialog.form.parentId"><option :value="0">无</option><option v-for="item in departments.filter((d) => d.deptId !== dialog.form.deptId)" :key="item.deptId" :value="item.deptId">{{ item.deptName }}</option></select></label><label>部门负责人<select v-model.number="dialog.form.leaderId"><option :value="null">暂不设置</option><option v-for="item in users" :key="item.userId" :value="item.userId">{{ item.realName }}（{{ item.employeeNo }}）</option></select></label><label>状态<select v-model.number="dialog.form.status"><option :value="1">启用</option><option :value="0">禁用</option></select></label><label class="full-field">部门说明<textarea v-model="dialog.form.description"></textarea></label></div>
    <div v-else class="form-grid"><label>职位名称<input v-model="dialog.form.positionName" required /></label><label>所属部门<select v-model.number="dialog.form.deptId" required><option value="" disabled>请选择</option><option v-for="item in departments" :key="item.deptId" :value="item.deptId">{{ item.deptName }}</option></select></label><label>职位等级<input v-model.number="dialog.form.level" type="number" min="1" required /></label><label class="full-field">职位说明<textarea v-model="dialog.form.description"></textarea></label></div>
    <div v-if="dialog.error" class="form-error">{{ dialog.error }}</div><div class="dialog-actions"><button type="button" @click="dialog.visible = false">取消</button><button class="primary-button" :disabled="loading">{{ loading ? '保存中…' : '保存' }}</button></div>
  </form></div>
  <transition name="notice"><div v-if="notice" class="notice-toast">✓ {{ notice }}</div></transition>
</template>
