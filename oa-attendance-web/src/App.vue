<script setup>
import { computed, onMounted, ref } from 'vue'
import { authApi, departmentApi, positionApi, userApi } from './api/oa'
import messageIcon from './assets/sidebar/message.svg'
import contactsIcon from './assets/sidebar/contacts.svg'
import calendarIcon from './assets/sidebar/calendar.svg'
import tasksIcon from './assets/sidebar/tasks.svg'
import approvalIcon from './assets/sidebar/approval.svg'
import reportIcon from './assets/sidebar/report.svg'

const authenticated = ref(Boolean(localStorage.getItem('oa_token')))
const currentUser = ref(null)
const loginForm = ref({ username: 'admin', password: '123456' })
const loginLoading = ref(false)
const loginError = ref('')
const activeSection = ref('工作台')
const searchKeyword = ref('')
const moduleKeyword = ref('')
const employeeDeptFilter = ref('')
const employeeStatusFilter = ref('')
const employeeGenderFilter = ref('')
const loading = ref(false)
const errorMessage = ref('')
const records = ref([])
const departments = ref([])
const positions = ref([])
const users = ref([])
const dialog = ref({ visible: false, mode: 'create', form: {}, error: '' })
const profileDialog = ref({ visible: false, form: {}, password: {}, error: '', success: '' })
const notice = ref('')

const navItems = [
  { label: '消息', icon: messageIcon }, { label: '通讯录', icon: contactsIcon },
  { label: '日历', icon: calendarIcon }, { label: '任务', icon: tasksIcon },
  { label: '审批', icon: approvalIcon }, { label: '工作台', icon: null },
]

const applications = [
  { title: '员工管理', subtitle: '员工档案与信息维护', icon: '员', color: '#3370ff', permission: 'user:query' },
  { title: '部门管理', subtitle: '组织架构与部门维护', icon: '部', color: '#00b8a9', permission: 'department:query' },
  { title: '职位管理', subtitle: '职位信息与级别设置', icon: '职', color: '#7b61ff', permission: 'position:query' },
  { title: '考勤管理', subtitle: '签到、签退与考勤记录', icon: '勤', color: '#ff7a00' },
  { title: '请假审批', subtitle: '请假申请与审批进度', icon: '假', color: '#f54a6e' },
  { title: '加班申请', subtitle: '加班申请与工时记录', icon: '加', color: '#9c5cff' },
  { title: '补卡申请', subtitle: '异常考勤补卡处理', icon: '补', color: '#1fbf75' },
  { title: '数据报表', subtitle: '团队考勤数据统计', icon: '表', color: '#15a6d9' },
]

const managedSections = ['员工管理', '部门管理', '职位管理']
const sectionResource = { 员工管理: 'user', 部门管理: 'department', 职位管理: 'position' }
const permissionSet = computed(() => new Set(currentUser.value?.permissions || []))
const roleCode = computed(() => currentUser.value?.roleCode || '')
const availableApplications = computed(() => applications.filter((item) => !item.permission || hasPermission(item.permission)))
const filteredApplications = computed(() => {
  const keyword = searchKeyword.value.trim()
  return keyword ? availableApplications.value.filter((item) => `${item.title}${item.subtitle}`.includes(keyword)) : availableApplications.value
})
const filteredRecords = computed(() => {
  const keyword = moduleKeyword.value.trim().toLowerCase()
  return records.value.filter((item) => {
    if (keyword && !JSON.stringify(item).toLowerCase().includes(keyword)) return false
    if (activeSection.value === '员工管理') {
      if (employeeDeptFilter.value !== '' && Number(item.deptId) !== Number(employeeDeptFilter.value)) return false
      if (employeeStatusFilter.value !== '' && Number(item.status) !== Number(employeeStatusFilter.value)) return false
      if (employeeGenderFilter.value !== '' && item.gender !== employeeGenderFilter.value) return false
    }
    return true
  })
})
const displayName = computed(() => currentUser.value?.realName || currentUser.value?.username || 'admin')
const avatarText = computed(() => displayName.value.slice(0, 1).toUpperCase())
const today = computed(() => {
  const date = new Date()
  const weekdays = ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六']
  return { day: date.getDate(), detail: `${weekdays[date.getDay()]} · ${date.getMonth() + 1}月` }
})

function hasPermission(code) {
  return permissionSet.value.has(code)
}

function can(action) {
  const resource = sectionResource[activeSection.value]
  return resource ? hasPermission(`${resource}:${action}`) : false
}

function canEditItem(item) {
  if (!can('update')) return false
  if (roleCode.value === 'ADMIN') return true
  if (activeSection.value === '员工管理') return roleCode.value === 'MANAGER' && Number(item.deptId) === Number(currentUser.value?.deptId)
  if (activeSection.value === '职位管理') return roleCode.value === 'MANAGER' && Number(item.deptId) === Number(currentUser.value?.deptId)
  return false
}

function canDeleteItem(item) {
  if (!can('delete')) return false
  if (activeSection.value === '员工管理' && Number(item.userId) === Number(currentUser.value?.userId)) return false
  return roleCode.value === 'ADMIN'
}

onMounted(async () => {
  window.addEventListener('oa-unauthorized', resetLogin)
  if (authenticated.value) await loadCurrentUser()
})

async function login() {
  loginLoading.value = true
  loginError.value = ''
  try {
    const result = await authApi.login(loginForm.value.username, loginForm.value.password)
    localStorage.setItem('oa_token', result.data.token)
    authenticated.value = true
    currentUser.value = result.data.user || null
    if (!currentUser.value) await loadCurrentUser()
  } catch (error) {
    loginError.value = error.message
  } finally {
    loginLoading.value = false
  }
}

async function loadCurrentUser() {
  try {
    const result = await authApi.info()
    currentUser.value = result.data
  } catch (error) {
    resetLogin()
    loginError.value = error.message
  }
}

function resetLogin() {
  localStorage.removeItem('oa_token')
  authenticated.value = false
  currentUser.value = null
  activeSection.value = '工作台'
}

async function logout() {
  try { await authApi.logout() } catch (_) { /* JWT 退出只需清理本地状态 */ }
  resetLogin()
}

async function openApplication(title) {
  const app = applications.find((item) => item.title === title)
  if (app?.permission && !hasPermission(app.permission)) {
    errorMessage.value = '当前账号没有访问该模块的权限'
    return
  }
  activeSection.value = title
  moduleKeyword.value = ''
  employeeDeptFilter.value = ''
  employeeStatusFilter.value = ''
  employeeGenderFilter.value = ''
  errorMessage.value = ''
  if (managedSections.includes(title)) await loadSection()
}

async function loadReferenceData() {
  const [departmentResult, positionResult] = await Promise.all([departmentApi.list(), positionApi.list()])
  departments.value = departmentResult.data || []
  positions.value = positionResult.data || []
}

async function loadSection() {
  loading.value = true
  errorMessage.value = ''
  try {
    if (activeSection.value === '员工管理') {
      const [userResult] = await Promise.all([userApi.list(), loadReferenceData()])
      records.value = userResult.data || []
      users.value = records.value
    } else if (activeSection.value === '部门管理') {
      const requests = [departmentApi.list()]
      if (hasPermission('user:query')) requests.push(userApi.list())
      const [result, userResult] = await Promise.all(requests)
      records.value = result.data || []
      departments.value = records.value
      users.value = userResult?.data || []
    } else if (activeSection.value === '职位管理') {
      const [positionResult, departmentResult] = await Promise.all([
        positionApi.list(), departmentApi.list(),
      ])
      records.value = positionResult.data || []
      positions.value = records.value
      departments.value = departmentResult.data || []
    }
  } catch (error) {
    errorMessage.value = error.message
    records.value = []
  } finally {
    loading.value = false
  }
}

function openCreate() {
  if (!can('add')) return
  const defaults = activeSection.value === '员工管理'
    ? { employeeNo: '', username: '', password: '123456', realName: '', nickname: '', gender: '男', phone: '', email: '', birthDate: '', hireDate: '', deptId: roleCode.value === 'MANAGER' ? currentUser.value?.deptId : '', positionId: '', roleId: 1, status: 1 }
    : activeSection.value === '部门管理'
      ? { parentId: 0, deptName: '', deptCode: '', leaderId: null, description: '', status: 1 }
      : { positionName: '', deptId: '', level: 1, description: '' }
  dialog.value = { visible: true, mode: 'create', form: defaults, error: '' }
}

function openEdit(item) {
  if (!canEditItem(item)) return
  dialog.value = { visible: true, mode: 'edit', form: { ...item }, error: '' }
}

function optionalNumber(value) {
  return value === '' || value == null ? null : Number(value)
}

function buildPayload() {
  const form = dialog.value.form
  if (activeSection.value === '员工管理') {
    const common = {
      realName: form.realName?.trim(), nickname: form.nickname?.trim() || null,
      gender: form.gender || null, phone: form.phone?.trim() || null, email: form.email?.trim() || null,
      birthDate: form.birthDate || null, hireDate: form.hireDate || null,
      deptId: Number(form.deptId), positionId: optionalNumber(form.positionId),
      roleId: Number(form.roleId), status: Number(form.status),
    }
    return dialog.value.mode === 'create'
      ? { ...common, employeeNo: form.employeeNo?.trim(), username: form.username?.trim(), password: form.password }
      : { ...common, userId: Number(form.userId) }
  }
  if (activeSection.value === '部门管理') {
    return {
      ...(dialog.value.mode === 'edit' ? { deptId: Number(form.deptId) } : {}),
      parentId: optionalNumber(form.parentId) ?? 0, deptName: form.deptName?.trim(), deptCode: form.deptCode?.trim(),
      leaderId: optionalNumber(form.leaderId), description: form.description?.trim() || null, status: Number(form.status),
    }
  }
  return {
    ...(dialog.value.mode === 'edit' ? { positionId: Number(form.positionId) } : {}),
    positionName: form.positionName?.trim(), deptId: Number(form.deptId),
    level: optionalNumber(form.level), description: form.description?.trim() || null,
  }
}

async function saveRecord() {
  loading.value = true
  dialog.value.error = ''
  try {
    const api = activeSection.value === '员工管理' ? userApi : activeSection.value === '部门管理' ? departmentApi : positionApi
    const payload = buildPayload()
    await api[dialog.value.mode === 'create' ? 'create' : 'update'](payload)
    dialog.value.visible = false
    showNotice(`${activeSection.value.replace('管理', '')}${dialog.value.mode === 'create' ? '新增' : '修改'}成功`)
    await loadSection()
  } catch (error) {
    dialog.value.error = error.message
  } finally {
    loading.value = false
  }
}

async function removeRecord(item) {
  if (!canDeleteItem(item)) return
  const id = item.userId || item.deptId || item.positionId
  if (!window.confirm(`确定删除“${item.realName || item.deptName || item.positionName}”吗？`)) return
  try {
    const api = activeSection.value === '员工管理' ? userApi : activeSection.value === '部门管理' ? departmentApi : positionApi
    await api.remove(id)
    showNotice('删除成功')
    await loadSection()
  } catch (error) {
    errorMessage.value = error.message
  }
}

async function resetUserPassword(item) {
  if (!canEditItem(item) || Number(item.userId) === Number(currentUser.value?.userId)) return
  if (!window.confirm(`确定将“${item.realName}”的密码重置为 123456 吗？`)) return
  try {
    await userApi.resetPassword(item.userId)
    showNotice('密码已重置为 123456')
  } catch (error) {
    errorMessage.value = error.message
  }
}

function showNotice(message) {
  notice.value = message
  window.setTimeout(() => { if (notice.value === message) notice.value = '' }, 2600)
}

async function openProfile() {
  let detail = currentUser.value || {}
  if (hasPermission('user:query')) {
    try {
      const result = await userApi.get(currentUser.value.userId)
      detail = result.data || detail
    } catch (_) { /* 个人资料仍可直接编辑 */ }
  }
  profileDialog.value = {
    visible: true,
    form: { nickname: detail.nickname || '', gender: detail.gender || '', phone: detail.phone || '', email: detail.email || '', birthDate: detail.birthDate || '' },
    password: { oldPassword: '', newPassword: '', confirmPassword: '' }, error: '', success: '',
  }
}

async function saveProfile() {
  profileDialog.value.error = ''
  profileDialog.value.success = ''
  try {
    const form = profileDialog.value.form
    const result = await userApi.updateProfile({
      nickname: form.nickname?.trim() || null, gender: form.gender || null,
      phone: form.phone?.trim() || null, email: form.email?.trim() || null, birthDate: form.birthDate || null,
    })
    currentUser.value = { ...currentUser.value, ...(result.data || {}), nickname: form.nickname }
    profileDialog.value.success = '个人资料保存成功'
  } catch (error) {
    profileDialog.value.error = error.message
  }
}

async function changeMyPassword() {
  const password = profileDialog.value.password
  profileDialog.value.error = ''
  profileDialog.value.success = ''
  if (!password.oldPassword || password.newPassword.length < 6) {
    profileDialog.value.error = '请输入旧密码，新密码至少 6 位'
    return
  }
  if (password.newPassword !== password.confirmPassword) {
    profileDialog.value.error = '两次输入的新密码不一致'
    return
  }
  try {
    await userApi.changePassword(currentUser.value.userId, password.oldPassword, password.newPassword)
    profileDialog.value.password = { oldPassword: '', newPassword: '', confirmPassword: '' }
    profileDialog.value.success = '密码修改成功，下次登录请使用新密码'
  } catch (error) {
    profileDialog.value.error = error.message
  }
}

function departmentName(id) {
  return departments.value.find((item) => Number(item.deptId) === Number(id))?.deptName || '—'
}
</script>

<template>
  <div v-if="!authenticated" class="login-page">
    <div class="login-brand-panel">
      <span class="brand-mark large"><i></i><i></i><i></i><i></i></span>
      <p>OA ATTENDANCE SYSTEM</p>
      <h1>让组织与考勤管理<br />清晰、高效、简单</h1>
      <div class="login-decoration"><span></span><span></span><span></span></div>
    </div>
    <form class="login-card" @submit.prevent="login">
      <div class="mobile-brand"><span class="brand-mark"><i></i><i></i><i></i><i></i></span>OA 工作台</div>
      <h2>欢迎登录</h2>
      <p>使用你的 OA 账号进入工作台</p>
      <label>账号<input v-model.trim="loginForm.username" autocomplete="username" placeholder="请输入账号" /></label>
      <label>密码<input v-model="loginForm.password" type="password" autocomplete="current-password" placeholder="请输入密码" /></label>
      <div v-if="loginError" class="form-error">{{ loginError }}</div>
      <button class="login-button" :disabled="loginLoading">{{ loginLoading ? '登录中…' : '登录' }}</button>
      <small>开发账号：admin　密码：123456</small>
    </form>
  </div>

  <div v-else class="app-shell">
    <aside class="sidebar">
      <div class="profile-row"><button class="avatar avatar-button" title="个人设置" @click="openProfile">{{ avatarText }}</button></div>
      <label class="search-box"><span>⌕</span><input v-model="searchKeyword" type="search" placeholder="搜索应用" /></label>
      <nav class="main-nav">
        <button v-for="item in navItems" :key="item.label" :class="['nav-item', { active: activeSection === item.label }]" @click="openApplication(item.label)">
          <img v-if="item.icon" class="nav-icon" :src="item.icon" alt="" /><span v-else class="nav-icon workbench-icon">✦</span><span>{{ item.label }}</span>
        </button>
      </nav>
      <div class="sidebar-divider"></div>
      <button class="nav-item"><img class="nav-icon report-icon" :src="reportIcon" alt="" /><span>汇报</span></button>
      <div class="account-card">
        <button class="avatar small avatar-button" title="个人设置" @click="openProfile">{{ avatarText }}</button><div><strong>{{ displayName }}</strong><span>{{ currentUser?.roleName || '管理员' }}</span></div><button @click="logout">退出</button>
      </div>
    </aside>

    <main class="workspace">
      <header class="topbar">
        <div class="brand"><span class="brand-mark"><i></i><i></i><i></i><i></i></span><strong>{{ activeSection }}</strong></div>
        <div class="top-actions"><button @click="openProfile">♙ <span>个人设置</span></button></div>
      </header>

      <section v-if="activeSection === '工作台'" class="page-content">
        <div class="welcome-row">
          <div><p class="eyebrow">OA ATTENDANCE</p><h1>上午好，{{ displayName }}</h1><p>在这里快速进入人事、考勤和审批工作。</p></div>
          <div class="date-card"><span>今日</span><strong>{{ today.day }}</strong><small>{{ today.detail }}</small></div>
        </div>
        <section class="section-block">
          <div class="section-heading"><div><h2>常用应用</h2><p>快速开始你的日常工作</p></div><button class="link-button">管理常用应用 →</button></div>
          <div v-if="filteredApplications.length" class="application-grid">
            <button v-for="application in filteredApplications" :key="application.title" class="application-card" @click="openApplication(application.title)">
              <span class="app-icon" :style="{ background: application.color }">{{ application.icon }}</span>
              <span class="app-copy"><strong>{{ application.title }}</strong><small>{{ application.subtitle }}</small></span><span class="app-arrow">›</span>
            </button>
          </div>
          <div v-else class="empty-state">没有找到“{{ searchKeyword }}”相关应用</div>
        </section>
      </section>

      <section v-else-if="managedSections.includes(activeSection)" class="management-page">
        <div class="management-heading">
          <div><button class="back-button" @click="openApplication('工作台')">← 返回工作台</button><h1>{{ activeSection }}</h1></div>
          <button v-if="can('add')" class="primary-button" @click="openCreate">＋ 新增{{ activeSection.replace('管理', '') }}</button>
        </div>
        <div class="table-toolbar">
          <input v-model="moduleKeyword" :placeholder="`搜索${activeSection.replace('管理', '')}`" />
          <template v-if="activeSection === '员工管理'">
            <select v-model="employeeDeptFilter"><option value="">全部部门</option><option v-for="item in departments" :key="item.deptId" :value="item.deptId">{{ item.deptName }}</option></select>
            <select v-model="employeeStatusFilter"><option value="">全部状态</option><option value="1">在职</option><option value="0">离职</option></select>
            <select v-model="employeeGenderFilter"><option value="">全部性别</option><option value="男">男</option><option value="女">女</option></select>
          </template>
          <button class="refresh-button" :disabled="loading" title="刷新数据" @click="loadSection"><span>↻</span>{{ loading ? '刷新中' : '刷新' }}</button>
        </div>
        <div v-if="errorMessage" class="api-error">{{ errorMessage }}</div>
        <div class="data-panel">
          <div v-if="loading" class="empty-state">正在加载数据…</div>
          <table v-else-if="activeSection === '员工管理'">
            <thead><tr><th>工号</th><th>姓名</th><th>性别</th><th>部门</th><th>职位</th><th>手机号</th><th>入职日期</th><th>状态</th><th>操作</th></tr></thead>
            <tbody><tr v-for="item in filteredRecords" :key="item.userId"><td>{{ item.employeeNo }}</td><td class="name-cell"><span class="table-avatar">{{ item.realName?.slice(0,1) }}</span>{{ item.realName }}</td><td>{{ item.gender || '—' }}</td><td>{{ item.deptName || '—' }}</td><td>{{ item.positionName || '—' }}</td><td>{{ item.phone || '—' }}</td><td>{{ item.hireDate || '—' }}</td><td><span :class="['status-tag', item.status ? 'on' : 'off']">{{ item.status ? '在职' : '离职' }}</span></td><td class="actions-cell"><button v-if="canEditItem(item)" class="text-action" @click="openEdit(item)">编辑</button><button v-if="canEditItem(item) && Number(item.userId) !== Number(currentUser?.userId)" class="text-action" @click="resetUserPassword(item)">重置密码</button><button v-if="canDeleteItem(item)" class="text-action danger" @click="removeRecord(item)">删除</button><span v-if="!canEditItem(item) && !canDeleteItem(item)" class="no-action">—</span></td></tr></tbody>
          </table>
          <table v-else-if="activeSection === '部门管理'">
            <thead><tr><th>部门编码</th><th>部门名称</th><th>上级部门</th><th>负责人</th><th>说明</th><th>状态</th><th>操作</th></tr></thead>
            <tbody><tr v-for="item in filteredRecords" :key="item.deptId"><td>{{ item.deptCode }}</td><td class="name-cell">{{ item.deptName }}</td><td>{{ item.parentDeptName || '—' }}</td><td>{{ item.leaderName || '—' }}</td><td>{{ item.description || '—' }}</td><td><span :class="['status-tag', item.status ? 'on' : 'off']">{{ item.status ? '启用' : '禁用' }}</span></td><td><button v-if="canEditItem(item)" class="text-action" @click="openEdit(item)">编辑</button><button v-if="canDeleteItem(item)" class="text-action danger" @click="removeRecord(item)">删除</button><span v-if="!canEditItem(item) && !canDeleteItem(item)" class="no-action">—</span></td></tr></tbody>
          </table>
          <table v-else>
            <thead><tr><th>职位名称</th><th>所属部门</th><th>职位等级</th><th>职位说明</th><th>操作</th></tr></thead>
            <tbody><tr v-for="item in filteredRecords" :key="item.positionId"><td class="name-cell">{{ item.positionName }}</td><td>{{ item.deptName || departmentName(item.deptId) }}</td><td>等级 {{ item.level }}</td><td>{{ item.description || '—' }}</td><td><button v-if="canEditItem(item)" class="text-action" @click="openEdit(item)">编辑</button><button v-if="canDeleteItem(item)" class="text-action danger" @click="removeRecord(item)">删除</button><span v-if="!canEditItem(item) && !canDeleteItem(item)" class="no-action">—</span></td></tr></tbody>
          </table>
          <div v-if="!loading && !filteredRecords.length" class="empty-state">暂无数据</div>
        </div>
      </section>

      <section v-else class="module-placeholder"><button class="back-button" @click="openApplication('工作台')">← 返回工作台</button><div class="module-icon">{{ applications.find((item) => item.title === activeSection)?.icon || 'OA' }}</div><h1>{{ activeSection }}</h1><p>后端暂未提供该模块接口。</p></section>
    </main>

    <div v-if="dialog.visible" class="dialog-mask" @click.self="dialog.visible = false">
      <form class="data-dialog" @submit.prevent="saveRecord">
        <div class="dialog-heading"><div><h2>{{ dialog.mode === 'create' ? '新增' : '编辑' }}{{ activeSection.replace('管理', '') }}</h2><p>填写完成后保存到后端数据库</p></div><button type="button" @click="dialog.visible = false">×</button></div>
        <div v-if="activeSection === '员工管理'" class="form-grid">
          <label v-if="dialog.mode === 'create'">工号<input v-model="dialog.form.employeeNo" required /></label><label v-if="dialog.mode === 'create'">登录账号<input v-model="dialog.form.username" required /></label><label v-if="dialog.mode === 'create'">初始密码<input v-model="dialog.form.password" type="password" required /></label>
          <label>姓名<input v-model.trim="dialog.form.realName" required /></label><label>昵称<input v-model.trim="dialog.form.nickname" /></label><label>性别<select v-model="dialog.form.gender"><option value="">未设置</option><option>男</option><option>女</option></select></label>
          <label>部门<select v-model.number="dialog.form.deptId" :disabled="roleCode === 'MANAGER'" required><option value="" disabled>请选择</option><option v-for="item in departments" :key="item.deptId" :value="item.deptId">{{ item.deptName }}</option></select></label>
          <label>职位<select v-model.number="dialog.form.positionId"><option value="">暂不设置</option><option v-for="item in positions.filter((p) => !dialog.form.deptId || Number(p.deptId) === Number(dialog.form.deptId))" :key="item.positionId" :value="item.positionId">{{ item.positionName }}</option></select></label>
          <label>手机号<input v-model.trim="dialog.form.phone" inputmode="tel" pattern="1[0-9]{10}" placeholder="11 位手机号" /></label><label>邮箱<input v-model.trim="dialog.form.email" type="email" /></label><label>出生日期<input v-model="dialog.form.birthDate" type="date" /></label><label>入职日期<input v-model="dialog.form.hireDate" type="date" /></label>
          <label>角色<select v-model.number="dialog.form.roleId" :disabled="roleCode === 'MANAGER'"><option :value="1">员工</option><option v-if="roleCode === 'ADMIN'" :value="2">主管</option><option v-if="roleCode === 'ADMIN'" :value="3">管理员</option></select></label><label>状态<select v-model.number="dialog.form.status"><option :value="1">在职</option><option :value="0">离职</option></select></label>
        </div>
        <div v-else-if="activeSection === '部门管理'" class="form-grid">
          <label>部门名称<input v-model="dialog.form.deptName" required /></label><label>部门编码<input v-model="dialog.form.deptCode" required /></label>
          <label>上级部门<select v-model.number="dialog.form.parentId"><option :value="0">无</option><option v-for="item in departments.filter((d) => d.deptId !== dialog.form.deptId)" :key="item.deptId" :value="item.deptId">{{ item.deptName }}</option></select></label>
          <label>部门负责人<select v-model.number="dialog.form.leaderId"><option :value="null">暂不设置</option><option v-for="item in users" :key="item.userId" :value="item.userId">{{ item.realName }}（{{ item.employeeNo }}）</option></select></label><label>状态<select v-model.number="dialog.form.status"><option :value="1">启用</option><option :value="0">禁用</option></select></label><label class="full-field">部门说明<textarea v-model="dialog.form.description"></textarea></label>
        </div>
        <div v-else class="form-grid">
          <label>职位名称<input v-model="dialog.form.positionName" required /></label><label>所属部门<select v-model.number="dialog.form.deptId" required><option value="" disabled>请选择</option><option v-for="item in departments" :key="item.deptId" :value="item.deptId">{{ item.deptName }}</option></select></label>
          <label>职位等级<input v-model.number="dialog.form.level" type="number" min="1" required /></label><label class="full-field">职位说明<textarea v-model="dialog.form.description"></textarea></label>
        </div>
        <div v-if="dialog.error" class="form-error">{{ dialog.error }}</div>
        <div class="dialog-actions"><button type="button" @click="dialog.visible = false">取消</button><button class="primary-button" :disabled="loading">{{ loading ? '保存中…' : '保存' }}</button></div>
      </form>
    </div>
    <div v-if="profileDialog.visible" class="dialog-mask" @click.self="profileDialog.visible = false">
      <div class="data-dialog profile-dialog">
        <div class="dialog-heading"><div><h2>个人设置</h2><p>维护自己的联系方式和登录密码</p></div><button type="button" @click="profileDialog.visible = false">×</button></div>
        <section class="profile-section">
          <h3>个人资料</h3>
          <form class="form-grid" @submit.prevent="saveProfile">
            <label>昵称<input v-model.trim="profileDialog.form.nickname" /></label><label>性别<select v-model="profileDialog.form.gender"><option value="">未设置</option><option>男</option><option>女</option></select></label>
            <label>手机号<input v-model.trim="profileDialog.form.phone" inputmode="tel" pattern="1[0-9]{10}" placeholder="11 位手机号" /></label><label>邮箱<input v-model.trim="profileDialog.form.email" type="email" /></label><label>出生日期<input v-model="profileDialog.form.birthDate" type="date" /></label>
            <div class="full-field inline-submit"><button class="primary-button">保存资料</button></div>
          </form>
        </section>
        <section class="profile-section password-section">
          <h3>修改密码</h3>
          <form class="form-grid" @submit.prevent="changeMyPassword">
            <label>旧密码<input v-model="profileDialog.password.oldPassword" type="password" autocomplete="current-password" required /></label><label>新密码<input v-model="profileDialog.password.newPassword" type="password" minlength="6" autocomplete="new-password" required /></label><label>确认新密码<input v-model="profileDialog.password.confirmPassword" type="password" minlength="6" autocomplete="new-password" required /></label>
            <div class="full-field inline-submit"><button class="primary-button">修改密码</button></div>
          </form>
        </section>
        <div v-if="profileDialog.error" class="form-error">{{ profileDialog.error }}</div><div v-if="profileDialog.success" class="form-success">{{ profileDialog.success }}</div>
      </div>
    </div>
    <transition name="notice"><div v-if="notice" class="notice-toast">✓ {{ notice }}</div></transition>
  </div>
</template>
