<script setup>
import { computed, onMounted, ref } from 'vue'
import { authApi, departmentApi, positionApi, userApi } from './api/oa'

const authenticated = ref(Boolean(localStorage.getItem('oa_token')))
const currentUser = ref(null)
const loginForm = ref({ username: 'admin', password: '123456' })
const loginLoading = ref(false)
const loginError = ref('')
const activeSection = ref('工作台')
const searchKeyword = ref('')
const moduleKeyword = ref('')
const loading = ref(false)
const errorMessage = ref('')
const records = ref([])
const departments = ref([])
const positions = ref([])
const dialog = ref({ visible: false, mode: 'create', form: {} })

const navItems = [
  { label: '消息', icon: '💬' }, { label: '通讯录', icon: '◉' },
  { label: '日历', icon: '▣' }, { label: '任务', icon: '✓' },
  { label: '审批', icon: '◫' }, { label: '工作台', icon: '✦' },
]

const applications = [
  { title: '员工管理', subtitle: '员工档案与信息维护', icon: '员', color: '#3370ff' },
  { title: '部门管理', subtitle: '组织架构与部门维护', icon: '部', color: '#00b8a9' },
  { title: '职位管理', subtitle: '职位信息与级别设置', icon: '职', color: '#7b61ff' },
  { title: '考勤管理', subtitle: '签到、签退与考勤记录', icon: '勤', color: '#ff7a00' },
  { title: '请假审批', subtitle: '请假申请与审批进度', icon: '假', color: '#f54a6e' },
  { title: '加班申请', subtitle: '加班申请与工时记录', icon: '加', color: '#9c5cff' },
  { title: '补卡申请', subtitle: '异常考勤补卡处理', icon: '补', color: '#1fbf75' },
  { title: '数据报表', subtitle: '团队考勤数据统计', icon: '表', color: '#15a6d9' },
]

const managedSections = ['员工管理', '部门管理', '职位管理']
const filteredApplications = computed(() => {
  const keyword = searchKeyword.value.trim()
  return keyword ? applications.filter((item) => `${item.title}${item.subtitle}`.includes(keyword)) : applications
})
const filteredRecords = computed(() => {
  const keyword = moduleKeyword.value.trim().toLowerCase()
  if (!keyword) return records.value
  return records.value.filter((item) => JSON.stringify(item).toLowerCase().includes(keyword))
})
const displayName = computed(() => currentUser.value?.realName || currentUser.value?.username || 'admin')

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
  activeSection.value = title
  moduleKeyword.value = ''
  errorMessage.value = ''
  if (managedSections.includes(title)) await loadSection()
}

async function loadReferenceData() {
  const [departmentResult, positionResult] = await Promise.all([
    departmentApi.list(), positionApi.list(),
  ])
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
    } else if (activeSection.value === '部门管理') {
      const result = await departmentApi.list()
      records.value = result.data || []
      departments.value = records.value
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
  const defaults = activeSection.value === '员工管理'
    ? { employeeNo: '', username: '', password: '123456', realName: '', gender: '男', phone: '', email: '', hireDate: '', deptId: '', positionId: '', roleId: 1, status: 1 }
    : activeSection.value === '部门管理'
      ? { parentId: 0, deptName: '', deptCode: '', leaderId: null, description: '', status: 1 }
      : { positionName: '', deptId: '', level: 1, description: '' }
  dialog.value = { visible: true, mode: 'create', form: defaults }
}

function openEdit(item) {
  dialog.value = { visible: true, mode: 'edit', form: { ...item } }
}

async function saveRecord() {
  loading.value = true
  errorMessage.value = ''
  try {
    const api = activeSection.value === '员工管理' ? userApi : activeSection.value === '部门管理' ? departmentApi : positionApi
    const payload = { ...dialog.value.form }
    if (activeSection.value === '员工管理' && dialog.value.mode === 'edit') {
      delete payload.employeeNo
      delete payload.username
      delete payload.password
      delete payload.deptName
      delete payload.positionName
      delete payload.roleName
      delete payload.createTime
      delete payload.updateTime
    }
    await api[dialog.value.mode === 'create' ? 'create' : 'update'](payload)
    dialog.value.visible = false
    await loadSection()
  } catch (error) {
    errorMessage.value = error.message
  } finally {
    loading.value = false
  }
}

async function removeRecord(item) {
  const id = item.userId || item.deptId || item.positionId
  if (!window.confirm(`确定删除“${item.realName || item.deptName || item.positionName}”吗？`)) return
  try {
    const api = activeSection.value === '员工管理' ? userApi : activeSection.value === '部门管理' ? departmentApi : positionApi
    await api.remove(id)
    await loadSection()
  } catch (error) {
    errorMessage.value = error.message
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
      <div class="profile-row"><div class="avatar">A</div><button class="round-action">＋</button></div>
      <label class="search-box"><span>⌕</span><input v-model="searchKeyword" type="search" placeholder="搜索应用" /><kbd>⌘ K</kbd></label>
      <nav class="main-nav">
        <button v-for="item in navItems" :key="item.label" :class="['nav-item', { active: activeSection === item.label }]" @click="openApplication(item.label)">
          <span class="nav-icon">{{ item.icon }}</span><span>{{ item.label }}</span>
        </button>
      </nav>
      <div class="sidebar-divider"></div>
      <button class="nav-item"><span class="nav-icon report-icon">▤</span><span>汇报</span></button>
      <div class="account-card">
        <div class="avatar small">A</div><div><strong>{{ displayName }}</strong><span>{{ currentUser?.roleName || '管理员' }}</span></div><button @click="logout">退出</button>
      </div>
    </aside>

    <main class="workspace">
      <header class="topbar">
        <div class="brand"><span class="brand-mark"><i></i><i></i><i></i><i></i></span><strong>{{ activeSection }}</strong></div>
        <div class="top-actions"><button>⌁ <span>帮助中心</span></button><button>⚙ <span>设置</span></button></div>
      </header>

      <section v-if="activeSection === '工作台'" class="page-content">
        <div class="welcome-row">
          <div><p class="eyebrow">OA ATTENDANCE</p><h1>上午好，{{ displayName }}</h1><p>在这里快速进入人事、考勤和审批工作。</p></div>
          <div class="date-card"><span>今日</span><strong>20</strong><small>星期一 · 7月</small></div>
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
          <div><button class="back-button" @click="openApplication('工作台')">← 返回工作台</button><h1>{{ activeSection }}</h1><p>数据来自 Spring Boot 后端，操作后会自动刷新。</p></div>
          <button class="primary-button" @click="openCreate">＋ 新增{{ activeSection.replace('管理', '') }}</button>
        </div>
        <div class="table-toolbar"><input v-model="moduleKeyword" :placeholder="`搜索${activeSection.replace('管理', '')}`" /><button @click="loadSection">刷新</button></div>
        <div v-if="errorMessage" class="api-error">{{ errorMessage }}</div>
        <div class="data-panel">
          <div v-if="loading" class="empty-state">正在加载数据…</div>
          <table v-else-if="activeSection === '员工管理'">
            <thead><tr><th>工号</th><th>姓名</th><th>性别</th><th>部门</th><th>职位</th><th>手机号</th><th>入职日期</th><th>状态</th><th>操作</th></tr></thead>
            <tbody><tr v-for="item in filteredRecords" :key="item.userId"><td>{{ item.employeeNo }}</td><td class="name-cell"><span class="table-avatar">{{ item.realName?.slice(0,1) }}</span>{{ item.realName }}</td><td>{{ item.gender || '—' }}</td><td>{{ item.deptName || '—' }}</td><td>{{ item.positionName || '—' }}</td><td>{{ item.phone || '—' }}</td><td>{{ item.hireDate || '—' }}</td><td><span :class="['status-tag', item.status ? 'on' : 'off']">{{ item.status ? '在职' : '离职' }}</span></td><td><button class="text-action" @click="openEdit(item)">编辑</button><button class="text-action danger" @click="removeRecord(item)">删除</button></td></tr></tbody>
          </table>
          <table v-else-if="activeSection === '部门管理'">
            <thead><tr><th>部门编码</th><th>部门名称</th><th>上级部门</th><th>负责人</th><th>说明</th><th>状态</th><th>操作</th></tr></thead>
            <tbody><tr v-for="item in filteredRecords" :key="item.deptId"><td>{{ item.deptCode }}</td><td class="name-cell">{{ item.deptName }}</td><td>{{ item.parentDeptName || '—' }}</td><td>{{ item.leaderName || '—' }}</td><td>{{ item.description || '—' }}</td><td><span :class="['status-tag', item.status ? 'on' : 'off']">{{ item.status ? '启用' : '禁用' }}</span></td><td><button class="text-action" @click="openEdit(item)">编辑</button><button class="text-action danger" @click="removeRecord(item)">删除</button></td></tr></tbody>
          </table>
          <table v-else>
            <thead><tr><th>职位名称</th><th>所属部门</th><th>职位等级</th><th>职位说明</th><th>操作</th></tr></thead>
            <tbody><tr v-for="item in filteredRecords" :key="item.positionId"><td class="name-cell">{{ item.positionName }}</td><td>{{ item.deptName || departmentName(item.deptId) }}</td><td>等级 {{ item.level }}</td><td>{{ item.description || '—' }}</td><td><button class="text-action" @click="openEdit(item)">编辑</button><button class="text-action danger" @click="removeRecord(item)">删除</button></td></tr></tbody>
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
          <label>姓名<input v-model="dialog.form.realName" required /></label><label>性别<select v-model="dialog.form.gender"><option>男</option><option>女</option></select></label>
          <label>部门<select v-model.number="dialog.form.deptId" required><option value="" disabled>请选择</option><option v-for="item in departments" :key="item.deptId" :value="item.deptId">{{ item.deptName }}</option></select></label>
          <label>职位<select v-model.number="dialog.form.positionId" required><option value="" disabled>请选择</option><option v-for="item in positions.filter((p) => !dialog.form.deptId || Number(p.deptId) === Number(dialog.form.deptId))" :key="item.positionId" :value="item.positionId">{{ item.positionName }}</option></select></label>
          <label>手机号<input v-model="dialog.form.phone" /></label><label>邮箱<input v-model="dialog.form.email" type="email" /></label><label>入职日期<input v-model="dialog.form.hireDate" type="date" /></label>
          <label>角色<select v-model.number="dialog.form.roleId"><option :value="1">员工</option><option :value="2">主管</option><option :value="3">管理员</option></select></label><label>状态<select v-model.number="dialog.form.status"><option :value="1">在职</option><option :value="0">离职</option></select></label>
        </div>
        <div v-else-if="activeSection === '部门管理'" class="form-grid">
          <label>部门名称<input v-model="dialog.form.deptName" required /></label><label>部门编码<input v-model="dialog.form.deptCode" required /></label>
          <label>上级部门<select v-model.number="dialog.form.parentId"><option :value="0">无</option><option v-for="item in departments.filter((d) => d.deptId !== dialog.form.deptId)" :key="item.deptId" :value="item.deptId">{{ item.deptName }}</option></select></label>
          <label>状态<select v-model.number="dialog.form.status"><option :value="1">启用</option><option :value="0">禁用</option></select></label><label class="full-field">部门说明<textarea v-model="dialog.form.description"></textarea></label>
        </div>
        <div v-else class="form-grid">
          <label>职位名称<input v-model="dialog.form.positionName" required /></label><label>所属部门<select v-model.number="dialog.form.deptId" required><option value="" disabled>请选择</option><option v-for="item in departments" :key="item.deptId" :value="item.deptId">{{ item.deptName }}</option></select></label>
          <label>职位等级<input v-model.number="dialog.form.level" type="number" min="1" required /></label><label class="full-field">职位说明<textarea v-model="dialog.form.description"></textarea></label>
        </div>
        <div v-if="errorMessage" class="form-error">{{ errorMessage }}</div>
        <div class="dialog-actions"><button type="button" @click="dialog.visible = false">取消</button><button class="primary-button" :disabled="loading">{{ loading ? '保存中…' : '保存' }}</button></div>
      </form>
    </div>
  </div>
</template>
