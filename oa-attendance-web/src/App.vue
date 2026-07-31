<script setup>
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { applicationApi, attendanceApi, authApi, chatApi, colleagueApi, departmentApi, fileApi, groupChatApi, menuApi, positionApi, roleApi, userApi } from './api/oa'
import AttendanceRuleManagement from './components/AttendanceRuleManagement.vue'
import AttendanceRecordManagement from './components/AttendanceRecordManagement.vue'
import AttendanceStats from './components/AttendanceStats.vue'
import DocumentCenter from './components/DocumentCenter.vue'
import AccessManagement from './components/AccessManagement.vue'
import CalendarView from './components/CalendarView.vue'
import AiAssistant from './components/AiAssistant.vue'
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
const favoriteAppTitles = ref([])
const favoritesLoaded = ref(false)
const manageAppsDialog = ref({ visible: false, selected: [] })
const moduleKeyword = ref('')
const employeeDeptFilter = ref('')
const employeeStatusFilter = ref('')
const employeeGenderFilter = ref('')
const employeeSort = ref('default')
const employeeSortField = ref('')
const employeeSortDir = ref('asc')
function toggleEmployeeSort(field) {
  if (employeeSortField.value === field) {
    employeeSortDir.value = employeeSortDir.value === 'asc' ? 'desc' : 'asc'
  } else {
    employeeSortField.value = field
    employeeSortDir.value = 'asc'
  }
  const dir = employeeSortDir.value === 'asc' ? 'Asc' : 'Desc'
  employeeSort.value = field + dir
}
function sortIndicator(field) {
  if (employeeSortField.value !== field) return '↕'
  return employeeSortDir.value === 'asc' ? '↑' : '↓'
}
const departmentView = ref('tree')
const pageSizeOptions = [5, 10, 20]
const pageSizeMap = ref({})
const pageMap = ref({})
function sectionSize(section) { return pageSizeMap.value[section] || 5 }
function sectionPage(section) { return pageMap.value[section] || 1 }
function setSectionSize(section, size) { pageSizeMap.value = { ...pageSizeMap.value, [section]: size }; setSectionPage(section, 1) }
function setSectionPage(section, page) { pageMap.value = { ...pageMap.value, [section]: page } }
function resetSectionPage(section) { setSectionPage(section, 1) }
function paginate(list, section) {
  const size = sectionSize(section)
  const page = sectionPage(section)
  const start = (page - 1) * size
  return list.slice(start, start + size)
}
function totalPages(list, section) { return Math.max(1, Math.ceil(list.length / sectionSize(section))) }
const expandedDepartmentIds = ref(new Set())
const loading = ref(false)
const errorMessage = ref('')
const records = ref([])
const departments = ref([])
const positions = ref([])
const roles = ref([])
const users = ref([])
const dialog = ref({ visible: false, mode: 'create', form: {}, error: '' })
const profileDialog = ref({ visible: false, form: {}, password: {}, error: '', success: '' })
const notice = ref('')
const colleagueKeyword = ref('')
const colleagueResults = ref([])
const colleagueSearched = ref(false)
const colleagueLoading = ref(false)
const chatContacts = ref([])
const chatContactKeyword = ref('')
const selectedChatContact = ref(null)
const chatMessages = ref([])
const chatDraft = ref('')
const chatLoading = ref(false)
const chatSending = ref(false)
const chatMessagesContainer = ref(null)
const chatAttachments = ref([])
const chatUploading = ref(false)
const hiddenChatContacts = ref(new Set())
const chatGroups = ref([])
const selectedChatGroup = ref(null)
const groupMessages = ref([])
const groupDraft = ref('')
const groupSending = ref(false)
const createGroupDialog = ref({ visible: false, name: '', selectedUserIds: [], error: '', saving: false })
const wsConnection = ref(null)
const approvalTab = ref('mine')
const approvalSort = ref('createTimeDesc')
const myApplications = ref([])
const pendingApplications = ref([])
const handledApplications = ref([])
const applicationTypes = ref([])
const makeupOptions = ref([])
const applicationLoading = ref(false)
const applicationDialog = ref({ visible: false, form: {}, error: '', submitting: false })
const applicationDetail = ref({ visible: false, data: null, loading: false })
const decisionDialog = ref({ visible: false, action: 'approve', item: null, comment: '', error: '', submitting: false })
const attachmentFiles = ref([])
const attachmentUploading = ref(false)
const uploadedAttachmentUrls = ref([])
const attendanceRules = ref([])
const selectedAttendanceRuleId = ref(null)
const todayAttendanceRecords = ref([])
const selectedAttendanceMonth = ref(new Date().toISOString().slice(0, 7))
const attendanceMonth = ref({ calendar: [] })
const attendanceLoading = ref(false)
const attendanceChecking = ref('')
const attendanceMessage = ref('')
const attendanceCheckForm = ref({ latitude: null, longitude: null, locationAddress: '', wifiSsid: '', wifiBssid: '' })
const currentClock = ref(new Date())
const currentMenuPaths = ref(new Set())

const navItems = [
  { label: '消息', icon: messageIcon, permission: 'chat:query', menuPaths: ['/chat'] }, { label: '通讯录', icon: contactsIcon, permission: 'colleague:query', menuPaths: ['/colleagues'] },
  { label: '文档', icon: tasksIcon, permission: 'document:query', menuPaths: ['/documents'] }, { label: '日历', icon: calendarIcon, permission: 'attendance:self' },
  { label: '审批', icon: approvalIcon, permission: 'approval:self', menuPaths: ['/approvals', '/approvals/my', '/approvals/tasks'] }, { label: '工作台', icon: null, menuPaths: ['/dashboard'] },
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
  { title: '考勤修改', subtitle: '管理员修改考勤记录', icon: '改', color: '#d83931', permission: 'attendance:modify' },
]

const managedSections = ['员工管理', '部门管理', '职位管理']
const sectionResource = { 员工管理: 'user', 部门管理: 'department', 职位管理: 'position' }
const permissionSet = computed(() => new Set(currentUser.value?.permissions || []))
const availableNavItems = computed(() => navItems.filter((item) => (!item.permission || hasPermission(item.permission)) && hasMenuAccess(item.menuPaths)))
const roleCode = computed(() => currentUser.value?.roleCode || '')
const availableApplications = computed(() => applications.filter((item) => (!item.permission || hasPermission(item.permission)) && hasMenuAccess(item.menuPaths)))
const filteredApplications = computed(() => {
  const keyword = searchKeyword.value.trim()
  if (keyword) return availableApplications.value.filter((item) => `${item.title}${item.subtitle}`.includes(keyword))
  if (!favoritesLoaded.value) return availableApplications.value
  return availableApplications.value.filter((item) => favoriteAppTitles.value.includes(item.title))
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
const sortedEmployeeRecords = computed(() => {
  const result = [...filteredRecords.value]
  const textCompare = (left, right) => String(left || '').localeCompare(String(right || ''), 'zh-CN', { numeric: true })
  const sorters = {
    employeeNoAsc: (left, right) => textCompare(left.employeeNo, right.employeeNo),
    employeeNoDesc: (left, right) => textCompare(right.employeeNo, left.employeeNo),
    realNameAsc: (left, right) => textCompare(left.realName, right.realName),
    realNameDesc: (left, right) => textCompare(right.realName, left.realName),
    deptNameAsc: (left, right) => textCompare(left.deptName, right.deptName),
    deptNameDesc: (left, right) => textCompare(right.deptName, left.deptName),
    hireDateAsc: (left, right) => {
      const a = left.hireDate || ''; const b = right.hireDate || ''
      return textCompare(a, b)
    },
    hireDateDesc: (left, right) => {
      const a = left.hireDate || ''; const b = right.hireDate || ''
      return textCompare(b, a)
    },
  }
  const sorter = sorters[employeeSort.value]
  return sorter ? result.sort(sorter) : result
})
watch([moduleKeyword, employeeDeptFilter, employeeStatusFilter, employeeGenderFilter, employeeSort], () => {
  resetSectionPage('员工管理')
})
watch(approvalTab, () => { resetSectionPage('审批_' + approvalTab.value) })
watch(filteredRecords, () => {
  const pg = totalPages(filteredRecords.value, activeSection.value)
  if (sectionPage(activeSection.value) > pg) setSectionPage(activeSection.value, pg)
})
const departmentTreeRows = computed(() => {
  const source = filteredRecords.value
  const children = new Map()
  const deptIds = new Set(source.map((item) => Number(item.deptId)))
  source.forEach((item) => {
    const parentId = Number(item.parentId || 0)
    if (!children.has(parentId)) children.set(parentId, [])
    children.get(parentId).push(item)
  })
  const rows = []
  const visit = (parentId, depth) => (children.get(Number(parentId)) || []).forEach((item) => {
    const childCount = (children.get(Number(item.deptId)) || []).length
    const expanded = expandedDepartmentIds.value.has(Number(item.deptId))
    rows.push({ ...item, depth, childCount, expanded })
    if (expanded) visit(item.deptId, depth + 1)
  })
  // 找出根节点：parent_id 不在当前列表中的部门
  source.forEach((item) => {
    const parentId = Number(item.parentId || 0)
    if (parentId === 0 || !deptIds.has(parentId)) {
      if (!rows.some((r) => Number(r.deptId) === Number(item.deptId))) {
        const childCount = (children.get(Number(item.deptId)) || []).length
        const expanded = expandedDepartmentIds.value.has(Number(item.deptId))
        rows.push({ ...item, depth: 0, childCount, expanded })
        if (expanded) visit(item.deptId, 1)
      }
    }
  })
  return rows
})

function toggleDepartmentNode(deptId) {
  const next = new Set(expandedDepartmentIds.value)
  const id = Number(deptId)
  if (next.has(id)) next.delete(id)
  else next.add(id)
  expandedDepartmentIds.value = next
}
const displayName = computed(() => currentUser.value?.realName || currentUser.value?.username || 'admin')
const avatarText = computed(() => displayName.value.slice(0, 1).toUpperCase())
const filteredChatContacts = computed(() => {
  const keyword = chatContactKeyword.value.trim().toLowerCase()
  let list = chatContacts.value.filter((c) => !hiddenChatContacts.value.has(Number(c.userId)))
  if (keyword) {
    list = list.filter((item) => `${item.realName}${item.deptName || ''}${item.positionName || ''}`.toLowerCase().includes(keyword))
  }
  return list
})
const totalUnread = computed(() => chatContacts.value.reduce((sum, item) => sum + Number(item.unreadCount || 0), 0))
const isRobotContact = computed(() => Number(selectedChatContact.value?.userId) === 0)
const allApplications = ref([])
const approvalTabs = computed(() => [
  { value: 'mine', label: '我的申请', count: myApplications.value.length },
  ...(hasPermission('approval:handle') ? [
    { value: 'pending', label: '待我审批', count: pendingApplications.value.length },
    { value: 'handled', label: '已处理', count: handledApplications.value.length },
  ] : []),
  ...(hasPermission('approval:all') ? [
    { value: 'all', label: '全部审批', count: allApplications.value.length },
  ] : []),
])
const visibleApplications = computed(() => {
  if (approvalTab.value === 'pending') return pendingApplications.value
  if (approvalTab.value === 'handled') return handledApplications.value
  if (approvalTab.value === 'all') return allApplications.value
  return myApplications.value
})
const sortedApplications = computed(() => {
  const list = [...visibleApplications.value]
  switch (approvalSort.value) {
    case 'createTimeAsc': return list.sort((a, b) => (a.createTime || '').localeCompare(b.createTime || ''))
    case 'startTimeDesc': return list.sort((a, b) => (b.startTime || '').localeCompare(a.startTime || ''))
    case 'startTimeAsc': return list.sort((a, b) => (a.startTime || '').localeCompare(b.startTime || ''))
    default: return list.sort((a, b) => (b.createTime || '').localeCompare(a.createTime || ''))
  }
})
const selectedAttendanceRule = computed(() => attendanceRules.value.find((item) => Number(item.ruleId) === Number(selectedAttendanceRuleId.value)) || null)
const selectedTodayAttendance = computed(() => todayAttendanceRecords.value.find((item) => Number(item.ruleId) === Number(selectedAttendanceRuleId.value)) || null)
const currentAttendanceStatus = computed(() => selectedTodayAttendance.value?.attendanceStatus || (selectedAttendanceRule.value ? 'PENDING' : 'NO_RULE'))
const attendanceDateText = computed(() => currentClock.value.toLocaleDateString('zh-CN', { month: 'long', day: 'numeric', weekday: 'long' }))
const attendanceTimeText = computed(() => currentClock.value.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: false }))
const today = computed(() => {
  const date = new Date()
  const weekdays = ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六']
  return { day: date.getDate(), detail: `${weekdays[date.getDay()]} · ${date.getMonth() + 1}月` }
})

function hasPermission(code) {
  return permissionSet.value.has(code)
}

function hasMenuAccess(paths) {
  return !paths?.length || currentMenuPaths.value.size === 0 || paths.some((path) => currentMenuPaths.value.has(path))
}

async function loadCurrentMenus() {
  try {
    const result = await menuApi.currentTree()
    const paths = []
    const collect = (items) => (items || []).forEach((item) => { if (item.menuPath) paths.push(item.menuPath); collect(item.children) })
    collect(result.data)
    currentMenuPaths.value = new Set(paths)
  } catch (_) {
    currentMenuPaths.value = new Set()
  }
  loadFavoriteApplications()
}

function favoriteStorageKey() {
  return `oa_favorite_apps_${currentUser.value?.userId || 'guest'}`
}

function loadFavoriteApplications() {
  try {
    const stored = JSON.parse(localStorage.getItem(favoriteStorageKey()))
    favoriteAppTitles.value = Array.isArray(stored) ? stored : availableApplications.value.map((item) => item.title)
  } catch (_) {
    favoriteAppTitles.value = availableApplications.value.map((item) => item.title)
  }
  favoritesLoaded.value = true
}

function openManageApplications() {
  manageAppsDialog.value = { visible: true, selected: [...favoriteAppTitles.value] }
}

function saveFavoriteApplications() {
  favoriteAppTitles.value = [...manageAppsDialog.value.selected]
  favoritesLoaded.value = true
  localStorage.setItem(favoriteStorageKey(), JSON.stringify(favoriteAppTitles.value))
  manageAppsDialog.value.visible = false
  showNotice('常用应用已更新')
}

function resetFavoriteApplications() {
  manageAppsDialog.value.selected = availableApplications.value.map((item) => item.title)
}

function can(action) {
  const resource = sectionResource[activeSection.value]
  return resource ? hasPermission(`${resource}:${action}`) : false
}

function canEditItem(item) {
  if (!can('update')) return false
  if (hasPermission('approval:all')) return true
  if (!hasPermission('approval:handle')) return false
  const itemDeptId = Number(item.deptId)
  const myDeptId = Number(currentUser.value?.deptId)
  if (!itemDeptId || !myDeptId) return false
  return itemDeptId === myDeptId || isDeptDescendantOf(myDeptId, itemDeptId)
}

function canDeleteItem(item) {
  if (!can('delete')) return false
  if (activeSection.value === '员工管理' && Number(item.userId) === Number(currentUser.value?.userId)) return false
  if (hasPermission('approval:all')) return true
  if (!hasPermission('approval:handle')) return false
  const itemDeptId = Number(item.deptId)
  const myDeptId = Number(currentUser.value?.deptId)
  if (!itemDeptId || !myDeptId) return false
  return itemDeptId === myDeptId || isDeptDescendantOf(myDeptId, itemDeptId)
}

function isDeptDescendantOf(ancestorId, descendantId) {
  let currentId = descendantId
  while (currentId && currentId > 0) {
    const dept = departments.value.find((d) => Number(d.deptId) === currentId)
    if (!dept || !dept.parentId || Number(dept.parentId) === 0) return false
    if (Number(dept.parentId) === ancestorId) return true
    currentId = Number(dept.parentId)
  }
  return false
}

onMounted(async () => {
  window.addEventListener('oa-unauthorized', resetLogin)
  window.setInterval(() => { currentClock.value = new Date() }, 1000)
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
    else await loadCurrentMenus()
    if (hasPermission('chat:query')) await loadChatContacts(false)
    connectWebSocket()
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
    await loadCurrentMenus()
    if (hasPermission('chat:query')) await loadChatContacts(false)
    connectWebSocket()
  } catch (error) {
    resetLogin()
    loginError.value = error.message
  }
}

function resetLogin() {
  disconnectWebSocket()
  localStorage.removeItem('oa_token')
  authenticated.value = false
  currentUser.value = null
  currentMenuPaths.value = new Set()
  favoriteAppTitles.value = []
  favoritesLoaded.value = false
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
  const nav = navItems.find((item) => item.label === title)
  if (nav?.permission && !hasPermission(nav.permission)) {
    errorMessage.value = '当前账号没有访问该模块的权限'
    return
  }
  const applicationPreset = { 请假审批: 'LEAVE', 加班申请: 'OVERTIME', 补卡申请: 'MAKEUP' }
  const presetType = applicationPreset[title]
  let targetSection = presetType ? '审批' : title
  if (title === '考勤管理') {
    if (hasPermission('attendance:self')) targetSection = '考勤管理'
    else if (hasPermission('attendance:rule:query')) targetSection = '考勤规则'
    else {
      errorMessage.value = '当前账号没有个人考勤或规则管理权限'
      return
    }
  }
  activeSection.value = targetSection
  moduleKeyword.value = ''
  employeeDeptFilter.value = ''
  employeeStatusFilter.value = ''
  employeeGenderFilter.value = ''
  employeeSort.value = 'default'
  employeeSortField.value = ''
  employeeSortDir.value = 'asc'
  resetSectionPage(title)
  errorMessage.value = ''
  if (managedSections.includes(title)) await loadSection()
  if (title === '消息') await loadChatContacts()
  if (targetSection === '考勤管理') await loadAttendance()
  if (title === '审批' || presetType) {
    approvalTab.value = 'mine'
    await loadApplications()
    if (presetType) openApplicationDialog(presetType)
  }
}

async function loadReferenceData() {
  const [departmentResult, positionResult, roleResult] = await Promise.all([departmentApi.list(), positionApi.list(), roleApi.list()])
  departments.value = departmentResult.data || []
  positions.value = positionResult.data || []
  roles.value = roleResult.data || []
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
      expandedDepartmentIds.value = new Set(records.value.map((item) => Number(item.deptId)))
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
  if (!window.confirm(`确定删除"${item.realName || item.deptName || item.positionName}"吗？`)) return
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
  if (!window.confirm(`确定将"${item.realName}"的密码重置为 123456 吗？`)) return
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

async function searchColleagues() {
  const keyword = colleagueKeyword.value.trim()
  colleagueSearched.value = true
  errorMessage.value = ''
  if (!keyword) {
    colleagueResults.value = []
    return
  }
  colleagueLoading.value = true
  try {
    const result = await colleagueApi.search(keyword)
    colleagueResults.value = result.data || []
  } catch (error) {
    errorMessage.value = error.message
    colleagueResults.value = []
  } finally {
    colleagueLoading.value = false
  }
}

async function loadChatContacts(autoSelect = true) {
  chatLoading.value = true
  errorMessage.value = ''
  try {
    const result = await chatApi.contacts()
    chatContacts.value = result.data || []
    await loadChatGroups()
    if (!autoSelect) return
    const currentId = selectedChatContact.value?.userId
    const nextContact = chatContacts.value.find((item) => Number(item.userId) === Number(currentId)) || chatContacts.value[0]
    if (nextContact) await selectChatContact(nextContact)
    else {
      selectedChatContact.value = null
      chatMessages.value = []
    }
  } catch (error) {
    errorMessage.value = error.message
  } finally {
    chatLoading.value = false
  }
}

async function selectChatContact(contact) {
  selectedChatContact.value = contact
  chatMessages.value = []
  chatLoading.value = true
  errorMessage.value = ''
  try {
    const result = await chatApi.messages(contact.userId)
    chatMessages.value = result.data || []
    const stored = chatContacts.value.find((item) => Number(item.userId) === Number(contact.userId))
    if (stored) stored.unreadCount = 0
    await scrollChatToBottom()
  } catch (error) {
    errorMessage.value = error.message
  } finally {
    chatLoading.value = false
  }
}

function connectWebSocket() {
  const token = localStorage.getItem('oa_token')
  if (!token) return
  disconnectWebSocket()
  const protocol = location.protocol === 'https:' ? 'wss:' : 'ws:'
  const url = `${protocol}//${location.host}/ws/chat?token=${encodeURIComponent(token)}`
  const ws = new WebSocket(url)
  ws.onopen = () => { /* connected */ }
  ws.onmessage = (event) => {
    try {
      const msg = JSON.parse(event.data)
      if (msg.type === 'message') {
        const senderId = Number(msg.senderId)
        const myId = Number(currentUser.value?.userId)
        const contactId = senderId === myId ? Number(msg.receiverId) : senderId
        const isCurrentChat = selectedChatContact.value && Number(selectedChatContact.value.userId) === contactId
        if (isCurrentChat) {
          chatMessages.value.push(msg)
          scrollChatToBottom()
        }
        let contact = chatContacts.value.find((c) => Number(c.userId) === contactId)
        if (!contact) {
          contact = { userId: contactId, realName: senderId === 0 ? '机器人通知' : (msg.senderName || '用户'), avatar: null, deptName: '', positionName: '', roleName: '', unreadCount: 0, lastMessage: '', lastMessageTime: '' }
          chatContacts.value.unshift(contact)
        }
        contact.lastMessage = msg.content
        contact.lastMessageTime = formatContactTime(new Date(msg.createTime))
        if (!isCurrentChat) contact.unreadCount = (contact.unreadCount || 0) + 1
      } else if (msg.type === 'group_message') {
        const groupId = Number(msg.groupId)
        const isCurrentGroup = selectedChatGroup.value && Number(selectedChatGroup.value.groupId) === groupId
        if (isCurrentGroup) {
          groupMessages.value.push(msg)
          scrollChatToBottom()
        }
        const group = chatGroups.value.find((g) => Number(g.groupId) === groupId)
        if (group) {
          group.lastMessage = (msg.senderName || '') + ': ' + (msg.content || '')
          group.lastMessageTime = formatContactTime(new Date(msg.createTime))
          if (!isCurrentGroup) group.unreadCount = (group.unreadCount || 0) + 1
        }
      }
    } catch (_) { /* ignore malformed messages */ }
  }
  ws.onclose = () => { wsConnection.value = null }
  ws.onerror = () => { /* retry handled by onclose */ }
  wsConnection.value = ws
}

function disconnectWebSocket() {
  if (wsConnection.value) {
    wsConnection.value.close()
    wsConnection.value = null
  }
}

async function sendChatMessage() {
  const content = chatDraft.value.trim()
  const hasAttachments = chatAttachments.value.length > 0
  if (!content && !hasAttachments) return
  if (!selectedChatContact.value || chatSending.value) return

  chatSending.value = true
  errorMessage.value = ''
  try {
    // Upload attachments first
    const uploadedFiles = []
    if (hasAttachments) {
      chatUploading.value = true
      for (const att of chatAttachments.value) {
        const result = await fileApi.upload(att.file)
        uploadedFiles.push({ name: att.file.name, size: att.file.size, url: result.data.url, type: att.file.type })
      }
      chatUploading.value = false
    }

    // Build message content with attachments
    let finalContent = content
    if (uploadedFiles.length) {
      const fileParts = uploadedFiles.map((f) => {
        const sizeStr = f.size < 1024 ? f.size + 'B' : f.size < 1048576 ? (f.size / 1024).toFixed(1) + 'KB' : (f.size / 1048576).toFixed(1) + 'MB'
        const isImage = f.type.startsWith('image/')
        if (isImage) return `[图片:${f.name}](${f.url})`
        return `[文件:${f.name}|${sizeStr}](${f.url})`
      })
      finalContent = finalContent ? finalContent + '\n' + fileParts.join('\n') : fileParts.join('\n')
    }

    const result = await chatApi.send(selectedChatContact.value.userId, finalContent)
    chatMessages.value.push(result.data)
    chatDraft.value = ''
    chatAttachments.value = []
    const stored = chatContacts.value.find((item) => Number(item.userId) === Number(selectedChatContact.value.userId))
    if (stored) {
      stored.lastMessage = content || '[文件]'
      stored.lastMessageTime = formatContactTime(new Date())
    }
    await scrollChatToBottom()
  } catch (error) {
    errorMessage.value = error.message
  } finally {
    chatSending.value = false
    chatUploading.value = false
  }
}

function handleChatFileSelect(event) {
  const files = event.target.files
  if (!files || !files.length) return
  for (const file of files) {
    if (file.size > 20 * 1024 * 1024) {
      errorMessage.value = `文件 ${file.name} 超过 20MB 限制`
      continue
    }
    const exists = chatAttachments.value.some((a) => a.file.name === file.name && a.file.size === file.size)
    if (!exists) {
      chatAttachments.value.push({ file, name: file.name, size: file.size, type: file.type })
    }
  }
  event.target.value = ''
}

function removeChatAttachment(index) {
  chatAttachments.value.splice(index, 1)
}

function hideChatContact(contact) {
  hiddenChatContacts.value.add(Number(contact.userId))
  hiddenChatContacts.value = new Set(hiddenChatContacts.value)
  if (selectedChatContact.value && Number(selectedChatContact.value.userId) === Number(contact.userId)) {
    selectedChatContact.value = null
    chatMessages.value = []
  }
}

function hideChatGroup(group) {
  chatGroups.value = chatGroups.value.filter(g => Number(g.groupId) !== Number(group.groupId))
  if (selectedChatGroup.value && Number(selectedChatGroup.value.groupId) === Number(group.groupId)) {
    selectedChatGroup.value = null
    groupMessages.value = []
  }
}

// ===== Group Chat =====
async function loadChatGroups() {
  try {
    const result = await groupChatApi.groups()
    chatGroups.value = result.data || []
  } catch (_) { chatGroups.value = [] }
}

async function selectChatGroup(group) {
  selectedChatGroup.value = group
  selectedChatContact.value = null
  groupMessages.value = []
  chatLoading.value = true
  try {
    const result = await groupChatApi.messages(group.groupId)
    groupMessages.value = result.data || []
    await nextTick()
    if (chatMessagesContainer.value) chatMessagesContainer.value.scrollTop = chatMessagesContainer.value.scrollHeight
  } catch (e) { errorMessage.value = e.message } finally { chatLoading.value = false }
  // Mark unread
  group.unreadCount = 0
}

async function sendGroupMessage() {
  const content = groupDraft.value.trim()
  if (!content || !selectedChatGroup.value || groupSending.value) return
  groupSending.value = true
  errorMessage.value = ''
  try {
    const result = await groupChatApi.send(selectedChatGroup.value.groupId, content)
    groupMessages.value.push(result.data)
    groupDraft.value = ''
    const g = chatGroups.value.find((item) => Number(item.groupId) === Number(selectedChatGroup.value.groupId))
    if (g) { g.lastMessage = '我: ' + content; g.lastMessageTime = formatContactTime(new Date()) }
    await nextTick()
    if (chatMessagesContainer.value) chatMessagesContainer.value.scrollTop = chatMessagesContainer.value.scrollHeight
  } catch (e) { errorMessage.value = e.message } finally { groupSending.value = false }
}

function openCreateGroup() {
  createGroupDialog.value = { visible: true, name: '', selectedUserIds: [], error: '', saving: false }
  loadUsers()
}

async function createGroup() {
  const state = createGroupDialog.value
  if (!state.name.trim()) { state.error = '请输入群名称'; return }
  if (!state.selectedUserIds.length) { state.error = '请至少选择一个成员'; return }
  state.saving = true; state.error = ''
  try {
    await groupChatApi.create(state.name.trim(), state.selectedUserIds)
    state.visible = false
    showNotice('群聊创建成功')
    await loadChatGroups()
  } catch (e) { state.error = e.message } finally { state.saving = false }
}

async function leaveGroup() {
  if (!selectedChatGroup.value || !window.confirm('确定退出该群聊？')) return
  try {
    await groupChatApi.leave(selectedChatGroup.value.groupId)
    chatGroups.value = chatGroups.value.filter(g => Number(g.groupId) !== Number(selectedChatGroup.value.groupId))
    selectedChatGroup.value = null
    groupMessages.value = []
    showNotice('已退出群聊')
  } catch (e) { errorMessage.value = e.message }
}

async function loadUsers() {
  if (users.value.length) return
  try { const r = await userApi.list(); users.value = r.data || [] } catch (_) {}
}

function renderMessageContent(content) {
  if (!content) return ''
  // Convert [图片:name](url) and [文件:name|size](url) to HTML
  let html = escapeHtml(content)
  html = html.replace(/\[图片:([^\]]+)\]\(([^)]+)\)/g, '<a href="$2" target="_blank" class="msg-image-link"><span class="msg-image-icon">🖼</span><span class="msg-image-name">$1</span></a>')
  html = html.replace(/\[文件:([^|]+)\|([^\]]+)\]\(([^)]+)\)/g, '<a href="$3" target="_blank" class="msg-file-link"><span class="msg-file-icon">📎</span><span class="msg-file-name">$1</span><span class="msg-file-size">$2</span></a>')
  html = html.replace(/\n/g, '<br>')
  return html
}

function escapeHtml(text) {
  if (!text) return ''
  return text.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
}

async function startChatWith(person) {
  await openApplication('消息')
  let contact = chatContacts.value.find((item) => Number(item.userId) === Number(person.userId))
  if (!contact) {
    contact = {
      userId: person.userId, realName: person.realName, avatar: person.avatar || null,
      deptName: person.deptName || '', positionName: person.positionName || '', roleName: person.roleName || '',
      unreadCount: 0, lastMessage: '', lastMessageTime: '',
    }
    chatContacts.value.push(contact)
  }
  await selectChatContact(contact)
}

async function scrollChatToBottom() {
  await nextTick()
  const element = chatMessagesContainer.value
  if (element) element.scrollTop = element.scrollHeight
}

function initial(name) {
  return name?.trim()?.slice(0, 1) || '同'
}

function contactInitial(contact) {
  return Number(contact?.userId) === 0 ? '🤖' : initial(contact?.realName)
}

function formatMessageTime(value) {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return String(value).replace('T', ' ').slice(0, 16)
  const todayDate = new Date()
  const sameDay = date.toDateString() === todayDate.toDateString()
  return sameDay
    ? date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
    : date.toLocaleString('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}

function formatContactTime(value) {
  const date = value instanceof Date ? value : new Date(value)
  if (Number.isNaN(date.getTime())) return ''
  return `${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
}

async function loadAttendance() {
  attendanceLoading.value = true
  attendanceMessage.value = ''
  errorMessage.value = ''
  try {
    const [ruleResult, todayResult, monthResult] = await Promise.all([
      attendanceApi.availableRules(), attendanceApi.todayRecords(), attendanceApi.month(selectedAttendanceMonth.value),
    ])
    attendanceRules.value = ruleResult.data || []
    todayAttendanceRecords.value = todayResult.data || []
    attendanceMonth.value = { calendar: [], ...(monthResult.data || {}) }
    if (!attendanceRules.value.some((item) => Number(item.ruleId) === Number(selectedAttendanceRuleId.value))) {
      selectedAttendanceRuleId.value = attendanceRules.value[0]?.ruleId || null
    }
  } catch (error) {
    errorMessage.value = error.message
  } finally {
    attendanceLoading.value = false
  }
}

async function loadAttendanceMonth() {
  attendanceLoading.value = true
  errorMessage.value = ''
  try {
    const result = await attendanceApi.month(selectedAttendanceMonth.value)
    attendanceMonth.value = { calendar: [], ...(result.data || {}) }
  } catch (error) {
    errorMessage.value = error.message
  } finally {
    attendanceLoading.value = false
  }
}

async function submitAttendance(action) {
  if (!selectedAttendanceRuleId.value || attendanceChecking.value) return
  attendanceChecking.value = action
  attendanceMessage.value = ''
  errorMessage.value = ''
  try {
    const form = attendanceCheckForm.value
    const api = action === 'in' ? attendanceApi.checkIn : attendanceApi.checkOut
    const result = await api({
      ruleId: Number(selectedAttendanceRuleId.value),
      latitude: form.latitude,
      longitude: form.longitude,
      locationAddress: form.locationAddress || null,
      wifiSsid: form.wifiSsid || null,
      wifiBssid: form.wifiBssid || null,
      clientInfo: navigator.userAgent,
    })
    const successMessage = result.msg || (action === 'in' ? '签到成功' : '签退成功')
    await loadAttendance()
    attendanceMessage.value = successMessage
  } catch (error) {
    errorMessage.value = error.message
  } finally {
    attendanceChecking.value = ''
  }
}

function getAttendanceLocation() {
  attendanceMessage.value = ''
  errorMessage.value = ''
  if (!navigator.geolocation) {
    errorMessage.value = '当前浏览器不支持定位'
    return
  }
  attendanceMessage.value = '正在获取定位…'
  navigator.geolocation.getCurrentPosition((position) => {
    attendanceCheckForm.value.latitude = Number(position.coords.latitude.toFixed(6))
    attendanceCheckForm.value.longitude = Number(position.coords.longitude.toFixed(6))
    attendanceCheckForm.value.locationAddress = '浏览器定位'
    attendanceMessage.value = '定位已获取，可以打卡'
  }, (error) => {
    if (error.code === 1) {
      errorMessage.value = '定位权限被拒绝，请在浏览器设置中允许本网站访问位置'
    } else if (error.code === 2) {
      errorMessage.value = '无法获取定位，请检查设备 GPS 是否开启'
    } else if (error.code === 3) {
      errorMessage.value = '定位超时，请移动到开阔地带或手动输入坐标后重试'
    } else {
      errorMessage.value = error.message || '定位获取失败'
    }
  }, { enableHighAccuracy: false, timeout: 30000, maximumAge: 60000 })
}

function attendanceRuleStatus(rule) {
  if (!rule) return { cls: '', label: '—' }
  const today = new Date().toISOString().slice(0, 10)
  if (!Number(rule.enabled)) return { cls: 'disabled', label: '已停用' }
  if (rule.effectiveStartDate && today < String(rule.effectiveStartDate).slice(0, 10)) return { cls: 'upcoming', label: '待生效' }
  if (rule.effectiveEndDate && today > String(rule.effectiveEndDate).slice(0, 10)) return { cls: 'expired', label: '已过期' }
  return { cls: 'active', label: '生效中' }
}

function attendanceRuleCheckText(rule) {
  const checks = []
  if (Number(rule?.requireWifi) === 1) checks.push('企业 WiFi')
  if (Number(rule?.requireLocation) === 1) checks.push('定位')
  return checks.length ? checks.join(' + ') : '无需校验'
}

function formatRuleTime(value) {
  return value ? String(value).slice(0, 5) : '—'
}

function formatCheckTime(value) {
  return value ? String(value).replace('T', ' ').slice(11, 16) : '—'
}

function attendanceStatusLabel(value) {
  return { NORMAL: '正常', LATE: '迟到', EARLY: '早退', ABSENT: '缺勤', LEAVE: '请假', OVERTIME: '加班', PENDING: '待打卡', NO_RULE: '无考勤规则' }[value] || '待确认'
}

function attendanceStatusClass(value) {
  if (value === 'NORMAL' || value === 'OVERTIME') return 'normal'
  if (value === 'LATE' || value === 'EARLY' || value === 'PENDING') return 'warning'
  if (value === 'LEAVE') return 'leave'
  return 'abnormal'
}

function minutesToHours(value) {
  return `${(Number(value || 0) / 60).toFixed(1)} 小时`
}

async function loadApplications() {
  applicationLoading.value = true
  errorMessage.value = ''
  try {
    const requests = [applicationApi.mine(), applicationApi.types(), applicationApi.makeupOptions()]
    if (hasPermission('approval:handle')) requests.push(applicationApi.pending(), applicationApi.handled())
    if (hasPermission('approval:all')) requests.push(applicationApi.all())
    const results = await Promise.all(requests)
    let idx = 0
    myApplications.value = results[idx++].data || []
    applicationTypes.value = results[idx++].data || []
    makeupOptions.value = results[idx++].data || []
    pendingApplications.value = hasPermission('approval:handle') ? results[idx++].data || [] : []
    handledApplications.value = hasPermission('approval:handle') ? results[idx++].data || [] : []
    allApplications.value = hasPermission('approval:all') ? results[idx++].data || [] : []
  } catch (error) {
    errorMessage.value = error.message
  } finally {
    applicationLoading.value = false
  }
}

function openApplicationDialog(type = '') {
  if (!hasPermission('approval:submit')) return
  applicationDialog.value = {
    visible: true,
    form: { applicationType: type, attendanceRecordId: null, startTime: '', endTime: '', reason: '', remark: '' },
    error: '',
    submitting: false,
  }
  attachmentFiles.value = []
  uploadedAttachmentUrls.value = []
}

function changeApplicationType() {
  if (applicationDialog.value.form.applicationType !== 'MAKEUP') {
    applicationDialog.value.form.attendanceRecordId = null
  }
  applicationDialog.value.form.startTime = ''
  applicationDialog.value.form.endTime = ''
}

function selectMakeupRecord() {
  const selected = makeupOptions.value.find((item) => String(item.recordId) === String(applicationDialog.value.form.attendanceRecordId))
  if (!selected) return
  applicationDialog.value.form.startTime = dateTimeInput(selected.suggestedStartTime)
  applicationDialog.value.form.endTime = dateTimeInput(selected.suggestedEndTime)
}

async function submitApplicationForm() {
  const state = applicationDialog.value
  state.error = ''
  if (state.form.endTime <= state.form.startTime) {
    state.error = '结束时间必须晚于开始时间'
    return
  }
  state.submitting = true
  try {
    if (attachmentFiles.value.length) {
      const uploaded = await uploadAttachments()
      if (!uploaded) return
    }
    const form = state.form
    await applicationApi.submit({
      applicationType: form.applicationType,
      attendanceRecordId: form.applicationType === 'MAKEUP' ? Number(form.attendanceRecordId) : null,
      startTime: form.startTime,
      endTime: form.endTime,
      reason: form.reason.trim(),
      remark: form.remark.trim() || null,
      attachmentUrls: uploadedAttachmentUrls.value.length ? [...uploadedAttachmentUrls.value] : [],
    })
    state.visible = false
    approvalTab.value = 'mine'
    showNotice('申请提交成功')
    await loadApplications()
  } catch (error) {
    state.error = error.message
  } finally {
    state.submitting = false
  }
}

async function cancelApplication(item) {
  if (!window.confirm('确定撤回这条申请吗？')) return
  errorMessage.value = ''
  try {
    await applicationApi.cancel(item.applicationId)
    showNotice('申请已撤回')
    await loadApplications()
  } catch (error) {
    errorMessage.value = error.message
  }
}

async function openApplicationDetail(item) {
  applicationDetail.value = { visible: true, data: null, loading: true }
  try {
    const result = await applicationApi.detail(item.applicationId)
    applicationDetail.value.data = result.data
  } catch (error) {
    applicationDetail.value.visible = false
    errorMessage.value = error.message
  } finally {
    applicationDetail.value.loading = false
  }
}

function openDecision(item, action) {
  decisionDialog.value = { visible: true, action, item, comment: '', error: '', submitting: false }
}

async function submitDecision() {
  const state = decisionDialog.value
  const comment = state.comment.trim()
  if (state.action === 'reject' && !comment) {
    state.error = '驳回申请时请填写审批意见'
    return
  }
  state.error = ''
  state.submitting = true
  try {
    await applicationApi[state.action](state.item.taskId, comment)
    state.visible = false
    showNotice(state.action === 'approve' ? '申请已通过' : '申请已驳回')
    await loadApplications()
  } catch (error) {
    state.error = error.message
  } finally {
    state.submitting = false
  }
}

function dateTimeInput(value) {
  return value ? String(value).slice(0, 16) : ''
}

function formatApplicationTime(value) {
  return value ? String(value).replace('T', ' ').slice(0, 16) : '—'
}

function handleAttachmentSelect(event) {
  const files = event.target.files
  if (!files || !files.length) return
  for (const file of files) {
    const exists = attachmentFiles.value.some((f) => f.name === file.name && f.size === file.size)
    if (exists) continue
    attachmentFiles.value.push({ file, name: file.name, size: file.size })
  }
  event.target.value = ''
}

function removeAttachment(index) {
  attachmentFiles.value.splice(index, 1)
}

function formatFileSize(bytes) {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

async function uploadAttachments() {
  if (!attachmentFiles.value.length) return true
  attachmentUploading.value = true
  applicationDialog.value.error = ''
  try {
    const urls = [...uploadedAttachmentUrls.value]
    for (const item of attachmentFiles.value) {
      const result = await fileApi.upload(item.file)
      urls.push(result.data.url)
    }
    uploadedAttachmentUrls.value = urls
    attachmentFiles.value = []
    return true
  } catch (error) {
    applicationDialog.value.error = '附件上传失败: ' + error.message
    return false
  } finally {
    attachmentUploading.value = false
  }
}

function makeupOptionText(item) {
  return `${item.attendanceDate} · ${item.ruleName || '考勤'} · ${item.issueLabel || '异常'} `
}

function applicationStatusClass(status) {
  if (status === 'APPROVED') return 'approved'
  if (status === 'REJECTED' || status === 'CANCELED') return 'rejected'
  return 'processing'
}

function approvalDecisionLabel(decision) {
  return { APPROVED: '通过', REJECTED: '驳回', CANCELED: '已撤回' }[decision] || '处理中'
}

function shortText(value, length = 24) {
  if (!value) return '—'
  return value.length > length ? `${value.slice(0, length)}…` : value
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
      <label class="search-box"><span>⌕</span><input v-model="searchKeyword" type="search" placeholder="搜索应用" @focus="activeSection = '工作台'" /></label>
      <nav class="main-nav">
        <button v-for="item in availableNavItems" :key="item.label" :class="['nav-item', { active: activeSection === item.label }]" @click="openApplication(item.label)">
          <img v-if="item.icon" class="nav-icon" :src="item.icon" alt="" /><span v-else class="nav-icon workbench-icon">{{ item.glyph || '✦' }}</span><span>{{ item.label }}</span><span v-if="item.label === '消息' && totalUnread" class="nav-unread">{{ totalUnread > 99 ? '99+' : totalUnread }}</span>
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
          <div class="section-heading"><div><h2>常用应用</h2><p>快速开始你的日常工作</p></div><button class="link-button" @click="openManageApplications">管理常用应用 →</button></div>
          <div v-if="filteredApplications.length" class="application-grid">
            <button v-for="application in filteredApplications" :key="application.title" class="application-card" @click="openApplication(application.title)">
              <span class="app-icon" :style="{ background: application.color }">{{ application.icon }}</span>
              <span class="app-copy"><strong>{{ application.title }}</strong><small>{{ application.subtitle }}</small></span><span class="app-arrow">›</span>
            </button>
          </div>
          <div v-else class="empty-state">{{ searchKeyword ? `没有找到"${searchKeyword}"相关应用` : '尚未添加常用应用，点击右上角进行管理' }}</div>
        </section>
      </section>

      <DocumentCenter v-else-if="activeSection === '文档'" :permissions="currentUser?.permissions || []" />

      <section v-else-if="activeSection === '通讯录'" class="communication-page contacts-page">
        <div class="communication-heading"><div><p class="eyebrow">CONTACTS</p><h1>查找同事</h1><p>通过姓名、工号或手机号查找权限范围内的同事。</p></div></div>
        <form class="colleague-search" @submit.prevent="searchColleagues">
          <span>⌕</span><input v-model.trim="colleagueKeyword" placeholder="搜索姓名、工号或手机号" autofocus /><button :disabled="colleagueLoading">{{ colleagueLoading ? '搜索中…' : '搜索' }}</button>
        </form>
        <div v-if="errorMessage" class="api-error">{{ errorMessage }}</div>
        <div v-if="colleagueLoading" class="communication-empty"><span class="empty-illustration">⌕</span><strong>正在查找同事…</strong></div>
        <div v-else-if="colleagueResults.length" class="colleague-grid">
          <article v-for="person in colleagueResults" :key="person.userId" class="colleague-card">
            <div class="colleague-card-top"><span class="person-avatar large-person">{{ initial(person.realName) }}</span><div><h3>{{ person.realName }}</h3><p>{{ person.positionName || person.roleName || '员工' }}</p></div><span class="department-pill">{{ person.deptName || '未分配部门' }}</span></div>
            <dl><div><dt>工号</dt><dd>{{ person.employeeNo || '—' }}</dd></div><div><dt>性别</dt><dd>{{ person.gender || '—' }}</dd></div><div><dt>手机号</dt><dd>{{ person.phone || '—' }}</dd></div><div><dt>邮箱</dt><dd>{{ person.email || '—' }}</dd></div><div><dt>入职日期</dt><dd>{{ person.hireDate || '—' }}</dd></div></dl>
            <button v-if="hasPermission('chat:query')" class="contact-message-button" @click="startChatWith(person)"><span>✉</span> 发消息</button>
          </article>
        </div>
        <div v-else-if="colleagueSearched" class="communication-empty"><span class="empty-illustration">无</span><strong>没有找到相关同事</strong><p>请尝试完整姓名、工号或手机号。</p></div>
        <div v-else class="communication-empty"><span class="empty-illustration">人</span><strong>搜索并联系你的同事</strong><p>可查看部门、职位和联系方式。</p></div>
      </section>

      <section v-else-if="activeSection === '消息'" class="chat-page">
        <aside class="chat-contact-panel">
          <div class="chat-panel-heading"><div><h1>消息</h1><span v-if="totalUnread">{{ totalUnread }} 条未读</span></div><button title="新建群聊" :disabled="chatLoading" @click="openCreateGroup">＋</button><button title="刷新" :disabled="chatLoading" @click="loadChatContacts">↻</button></div>
          <label class="chat-contact-search"><span>⌕</span><input v-model="chatContactKeyword" placeholder="搜索联系人" /></label>
          <div class="chat-contact-list">
            <div v-if="chatGroups.length" class="chat-contact-divider"><span>群聊</span></div>
            <div v-for="group in chatGroups" :key="'g'+group.groupId" :class="['chat-contact-item', { active: selectedChatGroup?.groupId === group.groupId }]" @click="selectChatGroup(group)">
              <span class="person-avatar">👥</span><span class="chat-contact-copy"><strong>{{ group.groupName }}</strong><small>{{ group.lastMessage || `${group.memberCount} 位成员` }}</small></span><span class="chat-contact-meta"><time>{{ group.lastMessageTime || '' }}</time><b v-if="group.unreadCount">{{ group.unreadCount > 99 ? '99+' : group.unreadCount }}</b><button class="chat-hide-btn" title="隐藏对话" @click.stop="hideChatGroup(group)">×</button></span>
            </div>
            <div v-if="filteredChatContacts.length" class="chat-contact-divider"><span>联系人</span></div>
            <div v-for="contact in filteredChatContacts" :key="contact.userId" :class="['chat-contact-item', { active: selectedChatContact?.userId === contact.userId }]" @click="selectChatContact(contact); selectedChatGroup = null">
              <span class="person-avatar">{{ contactInitial(contact) }}</span><span class="chat-contact-copy"><strong>{{ contact.realName }}</strong><small>{{ contact.lastMessage || `${contact.deptName || ''} ${contact.positionName || contact.roleName || ''}`.trim() || '同事' }}</small></span><span class="chat-contact-meta"><time>{{ contact.lastMessageTime || '' }}</time><b v-if="contact.unreadCount">{{ contact.unreadCount > 99 ? '99+' : contact.unreadCount }}</b><button class="chat-hide-btn" title="隐藏对话" @click.stop="hideChatContact(contact)">×</button></span>
            </div>
            <div v-if="!chatLoading && !filteredChatContacts.length && !chatGroups.length" class="chat-list-empty">暂无联系人</div>
          </div>
        </aside>
        <section v-if="selectedChatContact" class="conversation-panel">
          <header class="conversation-heading"><span class="person-avatar">{{ contactInitial(selectedChatContact) }}</span><div><strong>{{ selectedChatContact.realName }}</strong><small>{{ selectedChatContact.deptName || '未分配部门' }} · {{ selectedChatContact.positionName || selectedChatContact.roleName || '员工' }}</small></div></header>
          <div v-if="errorMessage" class="chat-error">{{ errorMessage }}</div>
          <div ref="chatMessagesContainer" class="message-list">
            <div v-if="chatLoading && !chatMessages.length" class="chat-loading">正在加载聊天记录…</div>
            <div v-else-if="!chatMessages.length && isRobotContact" class="conversation-empty"><span>🤖</span><strong>系统通知消息</strong><p>这里会显示签到提醒、审批状态等自动通知。</p></div>
            <div v-else-if="!chatMessages.length" class="conversation-empty"><span>✉</span><strong>开始聊天吧</strong><p>发送一条消息向 {{ selectedChatContact.realName }} 打个招呼。</p></div>
            <div v-for="message in chatMessages" :key="message.messageId" :class="['message-row', { mine: Number(message.senderId) === Number(currentUser?.userId) && Number(message.senderId) !== 0 }]">
              <span class="person-avatar message-avatar">{{ Number(message.senderId) === 0 ? '🤖' : initial(message.senderName || (Number(message.senderId) === Number(currentUser?.userId) ? displayName : selectedChatContact.realName)) }}</span><div class="message-content"><div class="message-bubble" v-html="renderMessageContent(message.content)"></div><time>{{ formatMessageTime(message.createTime) }}</time></div>
            </div>
          </div>
          <form v-if="!isRobotContact" class="message-composer" @submit.prevent="sendChatMessage">
            <div v-if="chatAttachments.length" class="chat-attachment-preview">
              <div v-for="(att, idx) in chatAttachments" :key="idx" class="chat-attachment-item">
                <span class="chat-att-icon">{{ att.type && att.type.startsWith('image/') ? '🖼' : '📎' }}</span>
                <span class="chat-att-name">{{ att.name }}</span>
                <span class="chat-att-size">{{ formatFileSize(att.size) }}</span>
                <button type="button" class="chat-att-remove" @click="removeChatAttachment(idx)">×</button>
              </div>
            </div>
            <textarea v-model="chatDraft" maxlength="2000" placeholder="输入消息，Enter 发送，Shift + Enter 换行" @keydown.enter.exact.prevent="sendChatMessage"></textarea>
            <div class="composer-actions">
              <label class="chat-file-btn" title="发送文件"><input type="file" multiple accept=".jpg,.jpeg,.png,.gif,.bmp,.webp,.pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.txt,.csv,.zip,.rar" @change="handleChatFileSelect" hidden /><span>📎</span></label>
              <span>{{ chatDraft.length }}/2000</span>
              <button :disabled="chatSending || chatUploading || (!chatDraft.trim() && !chatAttachments.length)">{{ chatUploading ? '上传中…' : chatSending ? '发送中…' : '发送' }}</button>
            </div>
          </form>
          <div v-else class="robot-notice">系统通知由机器人自动发送，无需回复</div>
        </section>
        <section v-else-if="selectedChatGroup" class="conversation-panel">
          <header class="conversation-heading"><span class="person-avatar">👥</span><div><strong>{{ selectedChatGroup.groupName }}</strong><small>{{ selectedChatGroup.memberCount || chatGroups.find(g => g.groupId === selectedChatGroup.groupId)?.memberCount || '' }} 位成员</small></div><button class="chat-leave-btn" title="退出群聊" @click="leaveGroup">退出</button></header>
          <div v-if="errorMessage" class="chat-error">{{ errorMessage }}</div>
          <div ref="chatMessagesContainer" class="message-list">
            <div v-if="chatLoading && !groupMessages.length" class="chat-loading">正在加载聊天记录…</div>
            <div v-else-if="!groupMessages.length" class="conversation-empty"><span>👥</span><strong>群聊开始</strong><p>发送一条消息开启群聊。</p></div>
            <div v-for="message in groupMessages" :key="message.messageId" :class="['message-row', { mine: Number(message.senderId) === Number(currentUser?.userId) }]">
              <span class="person-avatar message-avatar">{{ Number(message.senderId) === 0 ? '🤖' : initial(message.senderName || '群') }}</span><div class="message-content"><div class="message-bubble" v-html="renderMessageContent(message.content)"></div><time><span v-if="Number(message.senderId) !== 0">{{ message.senderName }} · </span>{{ formatMessageTime(message.createTime) }}</time></div>
            </div>
          </div>
          <form class="message-composer" @submit.prevent="sendGroupMessage">
            <textarea v-model="groupDraft" maxlength="2000" placeholder="输入消息，Enter 发送" @keydown.enter.exact.prevent="sendGroupMessage"></textarea>
            <div class="composer-actions">
              <span>{{ groupDraft.length }}/2000</span>
              <button :disabled="groupSending || !groupDraft.trim()">{{ groupSending ? '发送中…' : '发送' }}</button>
            </div>
          </form>
        </section>
        <section v-else class="no-conversation"><span>✉</span><h2>选择一个联系人</h2><p>从左侧联系人列表开始聊天。</p></section>
      </section>

      <AttendanceRuleManagement v-else-if="activeSection === '考勤规则'" :permissions="currentUser?.permissions || []" :role-code="roleCode" :current-dept-id="currentUser?.deptId" @back="openApplication('工作台')" />
      <AttendanceRecordManagement v-else-if="activeSection === '考勤修改'" :permissions="currentUser?.permissions || []" :current-dept-id="currentUser?.deptId" @back="openApplication('工作台')" />
      <AttendanceStats v-else-if="activeSection === '数据报表'" :role-code="roleCode" :current-dept-id="currentUser?.deptId" @back="openApplication('工作台')" />
      <AccessManagement v-else-if="activeSection === '权限管理'" :permissions="currentUser?.permissions || []" @back="openApplication('工作台')" />

      <section v-else-if="activeSection === '考勤管理'" class="attendance-page">
        <div class="management-heading attendance-heading">
          <div><button class="back-button" @click="openApplication('工作台')">← 返回工作台</button><h1>我的考勤</h1><p>完成每日签到、签退并查看考勤记录</p></div>
          <button class="attendance-refresh" :disabled="attendanceLoading" @click="loadAttendance"><span>↻</span>{{ attendanceLoading ? '刷新中' : '刷新' }}</button>
        </div>
        <div v-if="attendanceMessage" class="attendance-notice">✓ {{ attendanceMessage }}</div><div v-if="errorMessage" class="api-error">{{ errorMessage }}</div>
        <div class="attendance-overview">
          <article class="clock-card"><p>{{ attendanceDateText }}</p><strong>{{ attendanceTimeText }}</strong><span>请在规定时间与考勤范围内完成打卡</span><i></i><i></i></article>
          <article class="attendance-card rule-card">
            <div class="attendance-card-title"><div><span>今日考勤规则</span><h2>{{ selectedAttendanceRule?.ruleName || '暂无可用规则' }}</h2><b :class="attendanceRuleStatus(selectedAttendanceRule).cls">{{ attendanceRuleStatus(selectedAttendanceRule).label }}</b></div></div>
            <label v-if="attendanceRules.length > 1" class="attendance-rule-select">选择规则<select v-model.number="selectedAttendanceRuleId"><option v-for="rule in attendanceRules" :key="rule.ruleId" :value="rule.ruleId">{{ rule.ruleName }} ({{ attendanceRuleStatus(rule).label }})</option></select></label>
            <div v-if="selectedAttendanceRule" class="rule-detail-grid"><div><span>上班时间</span><strong>{{ formatRuleTime(selectedAttendanceRule.workStartTime) }}</strong></div><div><span>下班时间</span><strong>{{ formatRuleTime(selectedAttendanceRule.workEndTime) }}</strong></div><div><span>适用部门</span><strong>{{ selectedAttendanceRule.deptName || '全公司' }}</strong></div><div><span>校验方式</span><strong>{{ attendanceRuleCheckText(selectedAttendanceRule) }}</strong></div></div>
            <div v-else class="attendance-inline-empty">请联系管理员配置今日生效的考勤规则</div>
          </article>
          <article class="attendance-card punch-card">
            <div class="attendance-card-title"><div><span>今日打卡</span><h2>{{ attendanceStatusLabel(currentAttendanceStatus) }}</h2></div><b :class="attendanceStatusClass(currentAttendanceStatus)">{{ attendanceStatusLabel(currentAttendanceStatus) }}</b></div>
            <div class="punch-timeline"><div :class="{ done: selectedTodayAttendance?.checkInTime }"><i></i><span>上班签到</span><strong>{{ formatCheckTime(selectedTodayAttendance?.checkInTime) }}</strong><small>{{ attendanceStatusLabel(selectedTodayAttendance?.checkInStatus || 'PENDING') }}</small></div><div :class="{ done: selectedTodayAttendance?.checkOutTime }"><i></i><span>下班签退</span><strong>{{ formatCheckTime(selectedTodayAttendance?.checkOutTime) }}</strong><small>{{ attendanceStatusLabel(selectedTodayAttendance?.checkOutStatus || 'PENDING') }}</small></div></div>
            <div v-if="Number(selectedAttendanceRule?.requireLocation) === 1" class="location-row"><button type="button" @click="getAttendanceLocation">⌖ 获取当前位置</button><span>{{ attendanceCheckForm.locationAddress || '尚未获取定位' }}</span></div>
            <p v-if="Number(selectedAttendanceRule?.requireWifi) === 1" class="wifi-tip">当前规则要求企业 WiFi；浏览器无法自动读取 WiFi 名称，请使用已接入企业 WiFi 能力的客户端打卡。</p>
            <div class="punch-actions"><button :disabled="!selectedAttendanceRule || Boolean(selectedTodayAttendance?.checkInTime) || Boolean(attendanceChecking)" @click="submitAttendance('in')">{{ attendanceChecking === 'in' ? '签到中…' : selectedTodayAttendance?.checkInTime ? '已签到' : '签到' }}</button><button class="checkout-button" :disabled="!selectedAttendanceRule || !selectedTodayAttendance?.checkInTime || Boolean(selectedTodayAttendance?.checkOutTime) || Boolean(attendanceChecking)" @click="submitAttendance('out')">{{ attendanceChecking === 'out' ? '签退中…' : selectedTodayAttendance?.checkOutTime ? '已签退' : '签退' }}</button></div>
          </article>
        </div>

        <div class="attendance-month-heading"><div><p class="eyebrow">MONTHLY ATTENDANCE</p><h2>月度考勤</h2></div><label><input v-model="selectedAttendanceMonth" type="month" /><button :disabled="attendanceLoading" @click="loadAttendanceMonth">查询</button></label></div>
        <div class="attendance-metrics"><article><span>累计工时</span><strong>{{ minutesToHours(attendanceMonth.workMinutes) }}</strong></article><article><span>迟到</span><strong>{{ attendanceMonth.lateCount || 0 }}<small> 次</small></strong></article><article><span>缺勤</span><strong>{{ attendanceMonth.absentCount || 0 }}<small> 次</small></strong></article><article><span>加班</span><strong>{{ attendanceMonth.overtimeCount || 0 }}<small> 次</small></strong></article><article><span>加班时长</span><strong>{{ minutesToHours(attendanceMonth.overtimeMinutes) }}</strong></article><article><span>请假</span><strong>{{ attendanceMonth.leaveCount || 0 }}<small> 次</small></strong></article></div>
        <div class="data-panel attendance-records"><table><thead><tr><th>日期</th><th>考勤规则</th><th>签到</th><th>签退</th><th>状态</th><th>工作时长</th><th>加班时长</th></tr></thead><tbody><tr v-for="item in paginate(attendanceMonth.calendar || [], '考勤记录')" :key="item.recordId"><td class="name-cell">{{ item.attendanceDate }}</td><td>{{ item.ruleName || '—' }}</td><td>{{ formatCheckTime(item.checkInTime) }}</td><td>{{ formatCheckTime(item.checkOutTime) }}</td><td><span :class="['attendance-status-tag', attendanceStatusClass(item.attendanceStatus)]">{{ attendanceStatusLabel(item.attendanceStatus) }}</span></td><td>{{ minutesToHours(item.workMinutes) }}</td><td>{{ minutesToHours(item.overtimeMinutes) }}</td></tr></tbody></table><div v-if="!attendanceLoading && (attendanceMonth.calendar || []).length > sectionSize('考勤记录')" class="table-pagination"><span>第 {{ sectionPage('考勤记录') }} 页 / 共 {{ (attendanceMonth.calendar || []).length }} 条</span><div class="pagination-actions"><select class="page-size-select" :value="sectionSize('考勤记录')" @change="setSectionSize('考勤记录', Number($event.target.value))"><option v-for="s in pageSizeOptions" :key="s" :value="s">{{ s }}条/页</option></select><button :disabled="sectionPage('考勤记录') === 1" @click="setSectionPage('考勤记录', sectionPage('考勤记录') - 1)">‹</button><strong>{{ sectionPage('考勤记录') }}</strong><button :disabled="sectionPage('考勤记录') >= totalPages(attendanceMonth.calendar || [], '考勤记录')" @click="setSectionPage('考勤记录', sectionPage('考勤记录') + 1)">›</button></div></div><div v-if="!attendanceLoading && !(attendanceMonth.calendar || []).length" class="approval-empty"><span>勤</span><strong>本月暂无考勤记录</strong><p>完成签到后，记录会自动显示在这里。</p></div></div>
      </section>

      <section v-else-if="activeSection === '审批'" class="approval-page">
        <div class="management-heading approval-heading">
          <div><button class="back-button" @click="openApplication('工作台')">← 返回工作台</button><h1>审批中心</h1><p>提交申请并查看审批进度</p></div>
          <div class="approval-heading-actions"><button class="refresh-button" :disabled="applicationLoading" @click="loadApplications"><span>↻</span>{{ applicationLoading ? '刷新中' : '刷新' }}</button><button v-if="hasPermission('approval:submit')" class="primary-button" @click="openApplicationDialog()">＋ 新建申请</button></div>
        </div>
        <div class="approval-tabs">
          <button v-for="tab in approvalTabs" :key="tab.value" :class="{ active: approvalTab === tab.value }" @click="approvalTab = tab.value">{{ tab.label }}<span>{{ tab.count }}</span></button>
          <select v-model="approvalSort" class="approval-sort-select">
            <option value="createTimeDesc">提交时间: 最新</option>
            <option value="createTimeAsc">提交时间: 最早</option>
            <option value="startTimeDesc">开始时间: 最新</option>
            <option value="startTimeAsc">开始时间: 最早</option>
          </select>
        </div>
        <div v-if="errorMessage" class="api-error">{{ errorMessage }}</div>
        <div class="data-panel approval-panel">
          <div v-if="applicationLoading" class="empty-state">正在加载审批数据…</div>
          <table v-else>
            <thead><tr><th v-if="approvalTab !== 'mine'">申请人</th><th v-if="approvalTab !== 'mine'">部门</th><th>申请类型</th><th>时间范围</th><th>申请原因</th><th>状态</th><th>提交时间</th><th>操作</th></tr></thead>
            <tbody>
              <tr v-for="item in paginate(sortedApplications, '审批_' + approvalTab)" :key="item.applicationId">
                <td v-if="approvalTab !== 'mine'" class="name-cell"><span class="table-avatar">{{ initial(item.applicantName) }}</span>{{ item.applicantName }}</td><td v-if="approvalTab !== 'mine'">{{ item.applicantDeptName || '—' }}</td>
                <td class="name-cell">{{ item.applicationTypeLabel || item.applicationType }}</td><td><div class="time-range"><span>{{ formatApplicationTime(item.startTime) }}</span><small>至 {{ formatApplicationTime(item.endTime) }}</small></div></td><td class="reason-cell" :title="item.reason">{{ shortText(item.reason) }}</td><td><span :class="['approval-status', applicationStatusClass(item.status)]">{{ item.statusLabel || item.currentTaskName || '处理中' }}</span></td><td>{{ formatApplicationTime(item.createTime) }}</td>
                <td class="actions-cell"><button class="text-action" @click="openApplicationDetail(item)">详情</button><template v-if="approvalTab === 'mine'"><button v-if="item.canCancel" class="text-action danger" @click="cancelApplication(item)">撤回</button></template><template v-else-if="approvalTab === 'pending'"><button class="text-action success" @click="openDecision(item, 'approve')">通过</button><button class="text-action danger" @click="openDecision(item, 'reject')">驳回</button></template></td>
              </tr>
            </tbody>
          </table>
          <div v-if="!applicationLoading && sortedApplications.length > sectionSize('审批_' + approvalTab)" class="table-pagination">
            <span>第 {{ sectionPage('审批_' + approvalTab) }} 页 / 共 {{ sortedApplications.length }} 条</span>
            <div class="pagination-actions">
              <select class="page-size-select" :value="sectionSize('审批_' + approvalTab)" @change="setSectionSize('审批_' + approvalTab, Number($event.target.value))"><option v-for="s in pageSizeOptions" :key="s" :value="s">{{ s }}条/页</option></select>
              <button :disabled="sectionPage('审批_' + approvalTab) === 1" @click="setSectionPage('审批_' + approvalTab, sectionPage('审批_' + approvalTab) - 1)">‹</button>
              <strong>{{ sectionPage('审批_' + approvalTab) }}</strong>
              <button :disabled="sectionPage('审批_' + approvalTab) >= totalPages(visibleApplications, '审批_' + approvalTab)" @click="setSectionPage('审批_' + approvalTab, sectionPage('审批_' + approvalTab) + 1)">›</button>
            </div>
          </div>
          <div v-if="!applicationLoading && !sortedApplications.length" class="approval-empty"><span>✓</span><strong>{{ approvalTab === 'pending' ? '暂无待审批事项' : approvalTab === 'handled' ? '暂无已处理记录' : '还没有申请记录' }}</strong><p v-if="approvalTab === 'mine'">点击"新建申请"提交请假、加班或补卡申请。</p></div>
        </div>
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
          <div v-if="activeSection === '部门管理'" class="department-view-switch"><button :class="{ active: departmentView === 'tree' }" @click="departmentView = 'tree'">组织树</button><button :class="{ active: departmentView === 'list' }" @click="departmentView = 'list'">列表</button></div>
          <button class="refresh-button" :disabled="loading" title="刷新数据" @click="loadSection"><span>↻</span>{{ loading ? '刷新中' : '刷新' }}</button>
        </div>
        <div v-if="errorMessage" class="api-error">{{ errorMessage }}</div>
        <div class="data-panel">
          <div v-if="loading" class="empty-state">正在加载数据…</div>
          <template v-else-if="activeSection === '员工管理'">
            <table>
              <thead><tr><th class="sortable" @click="toggleEmployeeSort('employeeNo')">工号 <span class="sort-indicator">{{ sortIndicator('employeeNo') }}</span></th><th class="sortable" @click="toggleEmployeeSort('realName')">姓名 <span class="sort-indicator">{{ sortIndicator('realName') }}</span></th><th>性别</th><th class="sortable" @click="toggleEmployeeSort('deptName')">部门 <span class="sort-indicator">{{ sortIndicator('deptName') }}</span></th><th>职位</th><th>手机号</th><th class="sortable" @click="toggleEmployeeSort('hireDate')">入职日期 <span class="sort-indicator">{{ sortIndicator('hireDate') }}</span></th><th>状态</th><th>操作</th></tr></thead>
              <tbody><tr v-for="item in paginate(sortedEmployeeRecords, '员工管理')" :key="item.userId"><td>{{ item.employeeNo }}</td><td class="name-cell"><span class="table-avatar">{{ item.realName?.slice(0,1) }}</span>{{ item.realName }}</td><td>{{ item.gender || '—' }}</td><td>{{ item.deptName || '—' }}</td><td>{{ item.positionName || '—' }}</td><td>{{ item.phone || '—' }}</td><td>{{ item.hireDate || '—' }}</td><td><span :class="['status-tag', item.status ? 'on' : 'off']">{{ item.status ? '在职' : '离职' }}</span></td><td class="actions-cell"><button v-if="canEditItem(item)" class="text-action" @click="openEdit(item)">编辑</button><button v-if="canEditItem(item) && Number(item.userId) !== Number(currentUser?.userId)" class="text-action" @click="resetUserPassword(item)">重置密码</button><button v-if="canDeleteItem(item)" class="text-action danger" @click="removeRecord(item)">删除</button><span v-if="!canEditItem(item) && !canDeleteItem(item)" class="no-action">—</span></td></tr></tbody>
            </table>
            <div v-if="filteredRecords.length > sectionSize('员工管理')" class="table-pagination">
              <span>第 {{ sectionPage('员工管理') }} 页 / 共 {{ filteredRecords.length }} 条</span>
              <div class="pagination-actions">
                <select class="page-size-select" :value="sectionSize('员工管理')" @change="setSectionSize('员工管理', Number($event.target.value))"><option v-for="s in pageSizeOptions" :key="s" :value="s">{{ s }}条/页</option></select>
                <button :disabled="sectionPage('员工管理') === 1" aria-label="上一页" @click="setSectionPage('员工管理', sectionPage('员工管理') - 1)">‹</button>
                <strong>{{ sectionPage('员工管理') }}</strong>
                <button :disabled="sectionPage('员工管理') >= totalPages(filteredRecords, '员工管理')" aria-label="下一页" @click="setSectionPage('员工管理', sectionPage('员工管理') + 1)">›</button>
              </div>
            </div>
          </template>
          <template v-else-if="activeSection === '部门管理' && departmentView === 'list'">
            <table>
              <thead><tr><th>部门编码</th><th>部门名称</th><th>上级部门</th><th>负责人</th><th>说明</th><th>状态</th><th>操作</th></tr></thead>
              <tbody><tr v-for="item in paginate(filteredRecords, '部门管理')" :key="item.deptId"><td>{{ item.deptCode }}</td><td class="name-cell">{{ item.deptName }}</td><td>{{ item.parentDeptName || '—' }}</td><td>{{ item.leaderName || '—' }}</td><td>{{ item.description || '—' }}</td><td><span :class="['status-tag', item.status ? 'on' : 'off']">{{ item.status ? '启用' : '禁用' }}</span></td><td><button v-if="canEditItem(item)" class="text-action" @click="openEdit(item)">编辑</button><button v-if="canDeleteItem(item)" class="text-action danger" @click="removeRecord(item)">删除</button><span v-if="!canEditItem(item) && !canDeleteItem(item)" class="no-action">—</span></td></tr></tbody>
            </table>
            <div v-if="filteredRecords.length > sectionSize('部门管理')" class="table-pagination">
              <span>第 {{ sectionPage('部门管理') }} 页 / 共 {{ filteredRecords.length }} 条</span>
              <div class="pagination-actions">
                <select class="page-size-select" :value="sectionSize('部门管理')" @change="setSectionSize('部门管理', Number($event.target.value))"><option v-for="s in pageSizeOptions" :key="s" :value="s">{{ s }}条/页</option></select>
                <button :disabled="sectionPage('部门管理') === 1" @click="setSectionPage('部门管理', sectionPage('部门管理') - 1)">‹</button>
                <strong>{{ sectionPage('部门管理') }}</strong>
                <button :disabled="sectionPage('部门管理') >= totalPages(filteredRecords, '部门管理')" @click="setSectionPage('部门管理', sectionPage('部门管理') + 1)">›</button>
              </div>
            </div>
          </template>
          <table v-else-if="activeSection === '部门管理'" class="department-tree-table">
            <thead><tr><th>组织架构</th><th>部门编码</th><th>负责人</th><th>下级部门</th><th>说明</th><th>状态</th><th>操作</th></tr></thead>
            <tbody><tr v-for="item in departmentTreeRows" :key="item.deptId"><td class="name-cell"><span class="tree-indent" :style="{ width: `${item.depth * 28}px` }"></span><button v-if="item.childCount" class="tree-toggle" :class="{ expanded: item.expanded }" :aria-label="item.expanded ? `收起${item.deptName}` : `展开${item.deptName}`" @click="toggleDepartmentNode(item.deptId)">›</button><span v-else class="tree-toggle-placeholder"></span><span class="department-tree-icon">部</span>{{ item.deptName }}</td><td>{{ item.deptCode }}</td><td>{{ item.leaderName || '—' }}</td><td>{{ item.childCount ? `${item.childCount} 个` : '—' }}</td><td>{{ item.description || '—' }}</td><td><span :class="['status-tag', item.status ? 'on' : 'off']">{{ item.status ? '启用' : '禁用' }}</span></td><td><button v-if="canEditItem(item)" class="text-action" @click="openEdit(item)">编辑</button><button v-if="canDeleteItem(item)" class="text-action danger" @click="removeRecord(item)">删除</button><span v-if="!canEditItem(item) && !canDeleteItem(item)" class="no-action">—</span></td></tr></tbody>
          </table>
          <template v-else>
            <table>
              <thead><tr><th>职位名称</th><th>所属部门</th><th>职位等级</th><th>职位说明</th><th>操作</th></tr></thead>
              <tbody><tr v-for="item in paginate(filteredRecords, '职位管理')" :key="item.positionId"><td class="name-cell">{{ item.positionName }}</td><td>{{ item.deptName || departmentName(item.deptId) }}</td><td>等级 {{ item.level }}</td><td>{{ item.description || '—' }}</td><td><button v-if="canEditItem(item)" class="text-action" @click="openEdit(item)">编辑</button><button v-if="canDeleteItem(item)" class="text-action danger" @click="removeRecord(item)">删除</button><span v-if="!canEditItem(item) && !canDeleteItem(item)" class="no-action">—</span></td></tr></tbody>
            </table>
            <div v-if="filteredRecords.length > sectionSize('职位管理')" class="table-pagination">
              <span>第 {{ sectionPage('职位管理') }} 页 / 共 {{ filteredRecords.length }} 条</span>
              <div class="pagination-actions">
                <select class="page-size-select" :value="sectionSize('职位管理')" @change="setSectionSize('职位管理', Number($event.target.value))"><option v-for="s in pageSizeOptions" :key="s" :value="s">{{ s }}条/页</option></select>
                <button :disabled="sectionPage('职位管理') === 1" @click="setSectionPage('职位管理', sectionPage('职位管理') - 1)">‹</button>
                <strong>{{ sectionPage('职位管理') }}</strong>
                <button :disabled="sectionPage('职位管理') >= totalPages(filteredRecords, '职位管理')" @click="setSectionPage('职位管理', sectionPage('职位管理') + 1)">›</button>
              </div>
            </div>
          </template>
          <div v-if="!loading && !filteredRecords.length" class="empty-state">暂无数据</div>
        </div>
      </section>

      <CalendarView v-else-if="activeSection === '日历'" :permissions="currentUser?.permissions || []" :current-user="currentUser" @back="openApplication('工作台')" />
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
          <label>角色<select v-model.number="dialog.form.roleId" :disabled="roleCode === 'MANAGER'"><option v-for="r in roles" :key="r.roleId" :value="r.roleId">{{ r.roleName }}</option></select></label><label>状态<select v-model.number="dialog.form.status"><option :value="1">在职</option><option :value="0">离职</option></select></label>
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
    <div v-if="applicationDialog.visible" class="dialog-mask" @click.self="applicationDialog.visible = false">
      <form class="data-dialog application-form-dialog" @submit.prevent="submitApplicationForm">
        <div class="dialog-heading"><div><h2>新建申请</h2><p>提交后将进入部门主管审批流程</p></div><button type="button" @click="applicationDialog.visible = false">×</button></div>
        <div class="form-grid">
          <label class="full-field">申请类型<select v-model="applicationDialog.form.applicationType" required @change="changeApplicationType"><option value="" disabled>请选择申请类型</option><option v-for="item in applicationTypes" :key="item.value" :value="item.value">{{ item.label }}</option></select></label>
          <label v-if="applicationDialog.form.applicationType === 'MAKEUP'" class="full-field">异常考勤记录<select v-model="applicationDialog.form.attendanceRecordId" required @change="selectMakeupRecord"><option :value="null" disabled>请选择需要补卡的记录</option><option v-for="item in makeupOptions" :key="item.recordId" :value="item.recordId">{{ makeupOptionText(item) }}</option></select><small class="field-tip">{{ makeupOptions.length ? '只能选择本人尚未申请补卡的异常记录' : '当前没有可补卡的缺勤、迟到、早退或缺卡记录' }}</small></label>
          <label>{{ applicationDialog.form.applicationType === 'MAKEUP' ? '补卡签到时间' : '开始时间' }}<input v-model="applicationDialog.form.startTime" type="datetime-local" required /></label><label>{{ applicationDialog.form.applicationType === 'MAKEUP' ? '补卡签退时间' : '结束时间' }}<input v-model="applicationDialog.form.endTime" type="datetime-local" required /></label>
          <label class="full-field">申请原因<textarea v-model.trim="applicationDialog.form.reason" maxlength="1000" placeholder="请详细说明申请原因" required></textarea></label><label class="full-field">备注<textarea v-model.trim="applicationDialog.form.remark" maxlength="1000" placeholder="选填"></textarea></label>
          <div class="full-field attachment-section">
            <span class="attachment-section-label">附件（可选，支持图片/文档/压缩包）</span>
            <label class="attachment-add-btn"><span>＋</span><input type="file" multiple accept=".jpg,.jpeg,.png,.gif,.bmp,.webp,.pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.txt,.csv,.zip,.rar" @change="handleAttachmentSelect" hidden /></label>
            <div v-if="uploadedAttachmentUrls.length" class="attachment-list uploaded">
              <div v-for="(url, idx) in uploadedAttachmentUrls" :key="'up-' + idx" class="attachment-item done">
                <span class="attachment-icon">✓</span>
                <span class="attachment-name" :title="url">{{ url.split('/').pop() }}</span>
                <button type="button" class="attachment-remove" @click="uploadedAttachmentUrls.splice(idx, 1)" title="移除">×</button>
              </div>
            </div>
            <div v-if="attachmentFiles.length" class="attachment-list pending">
              <div v-for="(item, idx) in attachmentFiles" :key="'pf-' + idx" class="attachment-item">
                <span class="attachment-icon">📎</span>
                <span class="attachment-name" :title="item.name">{{ item.name }}</span>
                <span class="attachment-size">{{ formatFileSize(item.size) }}</span>
                <button type="button" class="attachment-remove" @click="removeAttachment(idx)" title="移除">×</button>
              </div>
            </div>
            <small v-if="!attachmentFiles.length && !uploadedAttachmentUrls.length" class="field-tip">提交时将自动上传所选文件</small>
          </div>
        </div>
        <div v-if="applicationDialog.error" class="form-error">{{ applicationDialog.error }}</div>
        <div class="dialog-actions"><button type="button" @click="applicationDialog.visible = false">取消</button><button class="primary-button" :disabled="applicationDialog.submitting || attachmentUploading || (applicationDialog.form.applicationType === 'MAKEUP' && !makeupOptions.length)">{{ applicationDialog.submitting ? (attachmentUploading ? '附件上传中…' : '提交中…') : '提交申请' }}</button></div>
      </form>
    </div>
    <div v-if="applicationDetail.visible" class="dialog-mask" @click.self="applicationDetail.visible = false">
      <div class="data-dialog application-detail-dialog">
        <div class="dialog-heading"><div><h2>申请详情</h2><p>申请内容与审批记录</p></div><button type="button" @click="applicationDetail.visible = false">×</button></div>
        <div v-if="applicationDetail.loading" class="empty-state">正在加载详情…</div>
        <template v-else-if="applicationDetail.data">
          <div class="application-detail-grid"><div><span>申请类型</span><strong>{{ applicationDetail.data.applicationTypeLabel }}</strong></div><div><span>当前状态</span><strong><i :class="['approval-status', applicationStatusClass(applicationDetail.data.status)]">{{ applicationDetail.data.statusLabel }}</i></strong></div><div><span>申请人</span><strong>{{ applicationDetail.data.applicantName }}</strong></div><div><span>所属部门</span><strong>{{ applicationDetail.data.applicantDeptName || '—' }}</strong></div><div><span>开始时间</span><strong>{{ formatApplicationTime(applicationDetail.data.startTime) }}</strong></div><div><span>结束时间</span><strong>{{ formatApplicationTime(applicationDetail.data.endTime) }}</strong></div><div class="detail-wide"><span>申请原因</span><p>{{ applicationDetail.data.reason }}</p></div><div v-if="applicationDetail.data.remark" class="detail-wide"><span>备注</span><p>{{ applicationDetail.data.remark }}</p></div></div>
          <section v-if="applicationDetail.data.attachmentUrls?.length" class="detail-attachments"><h3>附件 ({{ applicationDetail.data.attachmentUrls.length }})</h3><div class="detail-attachment-list"><a v-for="(url, idx) in applicationDetail.data.attachmentUrls" :key="idx" :href="url" target="_blank" class="detail-attachment-link"><span class="detail-attachment-icon">📎</span><span class="detail-attachment-name">{{ url.split('/').pop() }}</span><span class="detail-attachment-arrow">↗</span></a></div></section>
          <section v-if="applicationDetail.data.approvalHistory?.length" class="approval-history"><h3>审批记录</h3><div v-for="(history, index) in applicationDetail.data.approvalHistory" :key="index" class="history-item"><span class="history-dot"></span><div><strong>{{ history.taskName }} · {{ approvalDecisionLabel(history.decision) }}</strong><p>{{ history.approverName || history.approverUsername || '待处理' }}<template v-if="history.comment">：{{ history.comment }}</template></p><small>{{ formatApplicationTime(history.endTime || history.startTime) }}</small></div></div></section>
        </template>
      </div>
    </div>
    <div v-if="decisionDialog.visible" class="dialog-mask" @click.self="decisionDialog.visible = false">
      <form class="data-dialog decision-dialog" @submit.prevent="submitDecision">
        <div class="dialog-heading"><div><h2>{{ decisionDialog.action === 'approve' ? '通过申请' : '驳回申请' }}</h2><p>{{ decisionDialog.item?.applicantName }} · {{ decisionDialog.item?.applicationTypeLabel }}</p></div><button type="button" @click="decisionDialog.visible = false">×</button></div>
        <div class="decision-summary"><span>申请时间</span><strong>{{ formatApplicationTime(decisionDialog.item?.startTime) }} 至 {{ formatApplicationTime(decisionDialog.item?.endTime) }}</strong><span>申请原因</span><p>{{ decisionDialog.item?.reason }}</p></div>
        <label>审批意见<textarea v-model.trim="decisionDialog.comment" maxlength="500" :placeholder="decisionDialog.action === 'reject' ? '驳回时必须填写原因' : '选填'"></textarea></label>
        <div v-if="decisionDialog.error" class="form-error">{{ decisionDialog.error }}</div>
        <div class="dialog-actions"><button type="button" @click="decisionDialog.visible = false">取消</button><button :class="['primary-button', { 'danger-submit': decisionDialog.action === 'reject' }]" :disabled="decisionDialog.submitting">{{ decisionDialog.submitting ? '处理中…' : decisionDialog.action === 'approve' ? '确认通过' : '确认驳回' }}</button></div>
      </form>
    </div>
    <div v-if="manageAppsDialog.visible" class="dialog-mask" @click.self="manageAppsDialog.visible = false">
      <div class="data-dialog favorite-app-dialog">
        <div class="dialog-heading"><div><h2>管理常用应用</h2><p>选择后将显示在当前账号的工作台</p></div><button type="button" @click="manageAppsDialog.visible = false">×</button></div>
        <div class="favorite-app-summary"><span>已选择 {{ manageAppsDialog.selected.length }} 个应用</span><button @click="resetFavoriteApplications">全部选择</button></div>
        <div class="favorite-app-grid"><label v-for="item in availableApplications" :key="item.title" :class="{ selected: manageAppsDialog.selected.includes(item.title) }"><input v-model="manageAppsDialog.selected" type="checkbox" :value="item.title" /><span class="app-icon" :style="{ background: item.color }">{{ item.icon }}</span><span><strong>{{ item.title }}</strong><small>{{ item.subtitle }}</small></span><i>✓</i></label></div>
        <div class="dialog-actions"><button @click="manageAppsDialog.visible = false">取消</button><button class="primary-button" @click="saveFavoriteApplications">保存设置</button></div>
      </div>
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
    <div v-if="createGroupDialog.visible" class="dialog-mask" @click.self="createGroupDialog.visible = false">
      <form class="data-dialog" @submit.prevent="createGroup">
        <div class="dialog-heading"><div><h2>创建群聊</h2><p>选择好友并创建群聊</p></div><button type="button" @click="createGroupDialog.visible = false">×</button></div>
        <div class="form-grid">
          <label class="full-field">群名称<input v-model.trim="createGroupDialog.name" maxlength="50" placeholder="输入群聊名称" required /></label>
          <div class="full-field group-member-select">
            <span>选择成员（{{ createGroupDialog.selectedUserIds.length }} 人已选）</span>
            <div class="group-member-grid">
              <label v-for="u in (users.length ? users : (chatContacts.length ? chatContacts : []))" :key="u.userId || u.userId" :class="{ selected: createGroupDialog.selectedUserIds.includes(Number(u.userId)) }" class="group-member-item">
                <input type="checkbox" :value="Number(u.userId)" v-model="createGroupDialog.selectedUserIds" />
                <span class="person-avatar small">{{ initial(u.realName) }}</span>
                <span>{{ u.realName }}</span>
              </label>
            </div>
          </div>
        </div>
        <div v-if="createGroupDialog.error" class="form-error">{{ createGroupDialog.error }}</div>
        <div class="dialog-actions"><button type="button" @click="createGroupDialog.visible = false">取消</button><button class="primary-button" :disabled="createGroupDialog.saving">{{ createGroupDialog.saving ? '创建中…' : '创建群聊' }}</button></div>
      </form>
    </div>
    <transition name="notice"><div v-if="notice" class="notice-toast">✓ {{ notice }}</div></transition>
    <AiAssistant v-if="authenticated" :current-user="currentUser" />
  </div>
</template>
