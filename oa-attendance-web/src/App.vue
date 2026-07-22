<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { authApi, chatApi, menuApi, userApi } from './api/oa'
import AccessManagement from './components/AccessManagement.vue'
import AgentAssistant from './components/AgentAssistant.vue'
import ApprovalCenter from './components/ApprovalCenter.vue'
import AttendanceRuleManagement from './components/AttendanceRuleManagement.vue'
import AttendanceStats from './components/AttendanceStats.vue'
import CommunicationHub from './components/CommunicationHub.vue'
import DocumentCenter from './components/DocumentCenter.vue'
import PersonalAttendance from './components/PersonalAttendance.vue'
import PersonnelManagement from './components/PersonnelManagement.vue'
import WorkspaceHome from './components/WorkspaceHome.vue'
import approvalIcon from './assets/sidebar/approval.svg'
import calendarIcon from './assets/sidebar/calendar.svg'
import contactsIcon from './assets/sidebar/contacts.svg'
import messageIcon from './assets/sidebar/message.svg'
import reportIcon from './assets/sidebar/report.svg'
import tasksIcon from './assets/sidebar/tasks.svg'

const authenticated = ref(Boolean(localStorage.getItem('oa_token')))
const currentUser = ref(null)
const loginForm = ref({ username: 'admin', password: '123456' })
const loginLoading = ref(false)
const loginError = ref('')
const activeSection = ref('工作台')
const searchKeyword = ref('')
const currentMenuPaths = ref(new Set())
const favoriteAppTitles = ref([])
const favoritesLoaded = ref(false)
const manageAppsDialog = ref({ visible: false, selected: [] })
const profileDialog = ref({ visible: false, form: {}, password: {}, error: '', success: '' })
const totalUnread = ref(0)
const approvalPreset = ref('')
const currentClock = ref(new Date())
let clockTimer

const navItems = [
  { label: '消息', icon: messageIcon, permission: 'chat:query', menuPaths: ['/chat'] },
  { label: '通讯录', icon: contactsIcon, permission: 'colleague:query', menuPaths: ['/colleagues'] },
  { label: '文档', icon: tasksIcon, permission: 'document:query', menuPaths: ['/documents'] },
  { label: '日历', icon: calendarIcon },
  { label: '审批', icon: approvalIcon, permission: 'approval:self', menuPaths: ['/approvals', '/approvals/my', '/approvals/tasks'] },
  { label: '工作台', icon: null, menuPaths: ['/dashboard'] },
]

const applications = [
  { title: '员工管理', subtitle: '员工档案与信息维护', icon: '员', color: '#3370ff', permission: 'user:query', menuPaths: ['/system/user'] },
  { title: '部门管理', subtitle: '组织架构与部门维护', icon: '部', color: '#00b8a9', permission: 'department:query', menuPaths: ['/system/dept'] },
  { title: '职位管理', subtitle: '职位信息与级别设置', icon: '职', color: '#7b61ff', permission: 'position:query', menuPaths: ['/system/position'] },
  { title: '考勤管理', subtitle: '签到、签退与考勤记录', icon: '勤', color: '#ff7a00', menuPaths: ['/attendance', '/attendance/my', '/attendance/rules'] },
  { title: '请假审批', subtitle: '请假申请与审批进度', icon: '假', color: '#f54a6e', permission: 'approval:submit', menuPaths: ['/approvals', '/approvals/my'] },
  { title: '加班申请', subtitle: '加班申请与工时记录', icon: '加', color: '#9c5cff', permission: 'approval:submit', menuPaths: ['/approvals', '/approvals/my'] },
  { title: '补卡申请', subtitle: '异常考勤补卡处理', icon: '补', color: '#1fbf75', permission: 'approval:submit', menuPaths: ['/approvals', '/approvals/my'] },
  { title: '数据报表', subtitle: '团队考勤数据统计', icon: '表', color: '#15a6d9', permission: 'attendance:stats', menuPaths: ['/attendance/stats'] },
  { title: '权限管理', subtitle: '角色与功能菜单配置', icon: '权', color: '#5b6bda', permission: 'role:query', menuPaths: ['/system/role', '/system/menu'] },
]

const managedSections = ['员工管理', '部门管理', '职位管理']
const permissionSet = computed(() => new Set(currentUser.value?.permissions || []))
const roleCode = computed(() => currentUser.value?.roleCode || '')
const displayName = computed(() => currentUser.value?.realName || currentUser.value?.username || 'admin')
const avatarText = computed(() => displayName.value.slice(0, 1).toUpperCase())
const availableNavItems = computed(() => navItems.filter((item) => (!item.permission || hasPermission(item.permission)) && hasMenuAccess(item.menuPaths)))
const availableApplications = computed(() => applications.filter((item) => (!item.permission || hasPermission(item.permission)) && hasMenuAccess(item.menuPaths)))
const filteredApplications = computed(() => {
  const keyword = searchKeyword.value.trim()
  if (keyword) return availableApplications.value.filter((item) => `${item.title}${item.subtitle}`.includes(keyword))
  if (!favoritesLoaded.value) return availableApplications.value
  return availableApplications.value.filter((item) => favoriteAppTitles.value.includes(item.title))
})
const today = computed(() => {
  const date = currentClock.value
  return { day: date.getDate(), detail: date.toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', weekday: 'short' }) }
})

function hasPermission(code) { return permissionSet.value.has(code) }
function hasMenuAccess(paths) { return !paths?.length || currentMenuPaths.value.size === 0 || paths.some((path) => currentMenuPaths.value.has(path)) }

async function loadCurrentMenus() {
  try {
    const result = await menuApi.currentTree()
    const paths = []
    const visit = (items) => (items || []).forEach((item) => { if (item.path) paths.push(item.path); visit(item.children) })
    visit(result.data)
    currentMenuPaths.value = new Set(paths)
  } catch (_) {
    currentMenuPaths.value = new Set()
  }
  loadFavoriteApplications()
}

async function loadUnreadCount() {
  if (!hasPermission('chat:query')) return
  try {
    const result = await chatApi.contacts()
    totalUnread.value = (result.data || []).reduce((sum, item) => sum + Number(item.unreadCount || 0), 0)
  } catch (_) { totalUnread.value = 0 }
}

function favoriteStorageKey() { return `oa_favorite_apps_${currentUser.value?.userId || 'guest'}` }
function loadFavoriteApplications() {
  favoritesLoaded.value = true
  try {
    const stored = JSON.parse(localStorage.getItem(favoriteStorageKey()))
    favoriteAppTitles.value = Array.isArray(stored) ? stored.filter((title) => availableApplications.value.some((item) => item.title === title)) : availableApplications.value.map((item) => item.title)
  } catch (_) { favoriteAppTitles.value = availableApplications.value.map((item) => item.title) }
}
function openManageApplications() { manageAppsDialog.value = { visible: true, selected: [...favoriteAppTitles.value] } }
function saveFavoriteApplications() {
  favoriteAppTitles.value = [...manageAppsDialog.value.selected]
  favoritesLoaded.value = true
  localStorage.setItem(favoriteStorageKey(), JSON.stringify(favoriteAppTitles.value))
  manageAppsDialog.value.visible = false
}
function resetFavoriteApplications() { manageAppsDialog.value.selected = availableApplications.value.map((item) => item.title) }

async function login() {
  loginLoading.value = true
  loginError.value = ''
  try {
    const result = await authApi.login(loginForm.value.username, loginForm.value.password)
    localStorage.setItem('oa_token', result.data.token)
    authenticated.value = true
    currentUser.value = result.data.user || null
    if (!currentUser.value) await loadCurrentUser()
    else { await loadCurrentMenus(); await loadUnreadCount() }
  } catch (error) { loginError.value = error.message } finally { loginLoading.value = false }
}
async function loadCurrentUser() {
  try { const result = await authApi.info(); currentUser.value = result.data; await loadCurrentMenus(); await loadUnreadCount() }
  catch (error) { resetLogin(); loginError.value = error.message }
}
function resetLogin() {
  localStorage.removeItem('oa_token')
  authenticated.value = false
  currentUser.value = null
  currentMenuPaths.value = new Set()
  favoriteAppTitles.value = []
  favoritesLoaded.value = false
  activeSection.value = '工作台'
}
async function logout() { try { await authApi.logout() } catch (_) { /* 本地状态仍需清理 */ } resetLogin() }

function openApplication(title) {
  const app = applications.find((item) => item.title === title)
  const nav = navItems.find((item) => item.label === title)
  if ((app?.permission && !hasPermission(app.permission)) || (nav?.permission && !hasPermission(nav.permission))) return
  const presets = { 请假审批: 'LEAVE', 加班申请: 'OVERTIME', 补卡申请: 'MAKEUP' }
  approvalPreset.value = presets[title] || ''
  if (approvalPreset.value) activeSection.value = '审批'
  else if (title === '考勤管理' && !hasPermission('attendance:self') && hasPermission('attendance:rule:query')) activeSection.value = '考勤规则'
  else activeSection.value = title
}

async function openProfile() {
  profileDialog.value = { visible: true, form: { nickname: currentUser.value?.nickname || '', gender: currentUser.value?.gender || '', phone: currentUser.value?.phone || '', email: currentUser.value?.email || '', birthDate: currentUser.value?.birthDate || '' }, password: { oldPassword: '', newPassword: '', confirmPassword: '' }, error: '', success: '' }
}
async function saveProfile() {
  profileDialog.value.error = ''
  profileDialog.value.success = ''
  try {
    await userApi.updateProfile(profileDialog.value.form)
    await loadCurrentUser()
    profileDialog.value.success = '个人资料已保存'
  } catch (error) { profileDialog.value.error = error.message }
}
async function changeMyPassword() {
  const state = profileDialog.value
  state.error = ''
  state.success = ''
  if (state.password.newPassword !== state.password.confirmPassword) { state.error = '两次输入的新密码不一致'; return }
  try {
    await userApi.changePassword(currentUser.value.userId, state.password.oldPassword, state.password.newPassword)
    state.password = { oldPassword: '', newPassword: '', confirmPassword: '' }
    state.success = '密码修改成功'
  } catch (error) { state.error = error.message }
}

onMounted(async () => {
  window.addEventListener('oa-unauthorized', resetLogin)
  clockTimer = window.setInterval(() => { currentClock.value = new Date() }, 60000)
  if (authenticated.value) await loadCurrentUser()
})
onBeforeUnmount(() => {
  window.removeEventListener('oa-unauthorized', resetLogin)
  window.clearInterval(clockTimer)
})
</script>

<template>
  <div v-if="!authenticated" class="login-page">
    <div class="login-brand-panel"><span class="brand-mark large"><i></i><i></i><i></i><i></i></span><p>OA ATTENDANCE SYSTEM</p><h1>让组织与考勤管理<br />清晰、高效、简单</h1><div class="login-decoration"><span></span><span></span><span></span></div></div>
    <form class="login-card" @submit.prevent="login"><div class="mobile-brand"><span class="brand-mark"><i></i><i></i><i></i><i></i></span>OA 工作台</div><h2>欢迎登录</h2><p>使用你的 OA 账号进入工作台</p><label>账号<input v-model.trim="loginForm.username" autocomplete="username" placeholder="请输入账号" /></label><label>密码<input v-model="loginForm.password" type="password" autocomplete="current-password" placeholder="请输入密码" /></label><div v-if="loginError" class="form-error">{{ loginError }}</div><button class="login-button" :disabled="loginLoading">{{ loginLoading ? '登录中…' : '登录' }}</button><small>开发账号：admin　密码：123456</small></form>
  </div>

  <div v-else class="app-shell">
    <aside class="sidebar">
      <div class="profile-row"><button class="avatar avatar-button" title="个人设置" @click="openProfile">{{ avatarText }}</button></div>
      <label class="search-box"><span>⌕</span><input v-model="searchKeyword" type="search" placeholder="搜索应用" /></label>
      <nav class="main-nav"><button v-for="item in availableNavItems" :key="item.label" :class="['nav-item', { active: activeSection === item.label }]" @click="openApplication(item.label)"><img v-if="item.icon" class="nav-icon" :src="item.icon" alt="" /><span v-else class="nav-icon workbench-icon">✦</span><span>{{ item.label }}</span><span v-if="item.label === '消息' && totalUnread" class="nav-unread">{{ totalUnread > 99 ? '99+' : totalUnread }}</span></button></nav>
      <div class="sidebar-divider"></div><button class="nav-item"><img class="nav-icon report-icon" :src="reportIcon" alt="" /><span>汇报</span></button>
      <div class="account-card"><button class="avatar small avatar-button" title="个人设置" @click="openProfile">{{ avatarText }}</button><div><strong>{{ displayName }}</strong><span>{{ currentUser?.roleName || '管理员' }}</span></div><button @click="logout">退出</button></div>
    </aside>

    <main class="workspace">
      <header class="topbar"><div class="brand"><span class="brand-mark"><i></i><i></i><i></i><i></i></span><strong>{{ activeSection }}</strong></div><div class="top-actions"><button @click="openProfile">♙ <span>个人设置</span></button></div></header>
      <WorkspaceHome v-if="activeSection === '工作台'" :display-name="displayName" :today="today" :applications="filteredApplications" :search-keyword="searchKeyword" :current-user="currentUser" :permissions="currentUser?.permissions || []" @open="openApplication" @manage="openManageApplications" />
      <DocumentCenter v-else-if="activeSection === '文档'" :permissions="currentUser?.permissions || []" />
      <CommunicationHub v-else-if="['通讯录', '消息'].includes(activeSection)" :mode="activeSection" :current-user="currentUser" :can-chat="hasPermission('chat:query')" @open-messages="activeSection = '消息'" @unread-change="totalUnread = $event" />
      <AttendanceRuleManagement v-else-if="activeSection === '考勤规则'" :permissions="currentUser?.permissions || []" :role-code="roleCode" :current-dept-id="currentUser?.deptId" @back="openApplication('工作台')" />
      <AttendanceStats v-else-if="activeSection === '数据报表'" :role-code="roleCode" :current-dept-id="currentUser?.deptId" @back="openApplication('工作台')" />
      <AccessManagement v-else-if="activeSection === '权限管理'" :permissions="currentUser?.permissions || []" @back="openApplication('工作台')" />
      <PersonalAttendance v-else-if="activeSection === '考勤管理'" @back="openApplication('工作台')" />
      <ApprovalCenter v-else-if="activeSection === '审批'" :permissions="currentUser?.permissions || []" :initial-type="approvalPreset" @preset-consumed="approvalPreset = ''" @back="openApplication('工作台')" />
      <PersonnelManagement v-else-if="managedSections.includes(activeSection)" :section="activeSection" :current-user="currentUser" :permissions="currentUser?.permissions || []" @back="openApplication('工作台')" />
      <section v-else class="module-placeholder"><button class="back-button" @click="openApplication('工作台')">← 返回工作台</button><div class="module-icon">{{ applications.find((item) => item.title === activeSection)?.icon || 'OA' }}</div><h1>{{ activeSection }}</h1><p>后端暂未提供该模块接口。</p></section>
    </main>

    <div v-if="manageAppsDialog.visible" class="dialog-mask" @click.self="manageAppsDialog.visible = false"><div class="data-dialog favorite-app-dialog"><div class="dialog-heading"><div><h2>管理常用应用</h2><p>选择后将显示在当前账号的工作台</p></div><button type="button" @click="manageAppsDialog.visible = false">×</button></div><div class="favorite-app-summary"><span>已选择 {{ manageAppsDialog.selected.length }} 个应用</span><button @click="resetFavoriteApplications">全部选择</button></div><div class="favorite-app-grid"><label v-for="item in availableApplications" :key="item.title" :class="{ selected: manageAppsDialog.selected.includes(item.title) }"><input v-model="manageAppsDialog.selected" type="checkbox" :value="item.title" /><span class="app-icon" :style="{ background: item.color }">{{ item.icon }}</span><span><strong>{{ item.title }}</strong><small>{{ item.subtitle }}</small></span><i>✓</i></label></div><div class="dialog-actions"><button @click="manageAppsDialog.visible = false">取消</button><button class="primary-button" @click="saveFavoriteApplications">保存设置</button></div></div></div>

    <div v-if="profileDialog.visible" class="dialog-mask" @click.self="profileDialog.visible = false"><div class="data-dialog profile-dialog"><div class="dialog-heading"><div><h2>个人设置</h2><p>维护自己的联系方式和登录密码</p></div><button type="button" @click="profileDialog.visible = false">×</button></div><section class="profile-section"><h3>个人资料</h3><form class="form-grid" @submit.prevent="saveProfile"><label>昵称<input v-model.trim="profileDialog.form.nickname" /></label><label>性别<select v-model="profileDialog.form.gender"><option value="">未设置</option><option>男</option><option>女</option></select></label><label>手机号<input v-model.trim="profileDialog.form.phone" inputmode="tel" pattern="1[0-9]{10}" placeholder="11 位手机号" /></label><label>邮箱<input v-model.trim="profileDialog.form.email" type="email" /></label><label>出生日期<input v-model="profileDialog.form.birthDate" type="date" /></label><div class="full-field inline-submit"><button class="primary-button">保存资料</button></div></form></section><section class="profile-section password-section"><h3>修改密码</h3><form class="form-grid" @submit.prevent="changeMyPassword"><label>旧密码<input v-model="profileDialog.password.oldPassword" type="password" autocomplete="current-password" required /></label><label>新密码<input v-model="profileDialog.password.newPassword" type="password" minlength="6" autocomplete="new-password" required /></label><label>确认新密码<input v-model="profileDialog.password.confirmPassword" type="password" minlength="6" autocomplete="new-password" required /></label><div class="full-field inline-submit"><button class="primary-button">修改密码</button></div></form></section><div v-if="profileDialog.error" class="form-error">{{ profileDialog.error }}</div><div v-if="profileDialog.success" class="form-success">{{ profileDialog.success }}</div></div></div>

    <AgentAssistant @navigate="openApplication" />
  </div>
</template>
