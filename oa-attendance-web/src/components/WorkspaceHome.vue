<script setup>
import { computed, onMounted, ref } from 'vue'
import { applicationApi, attendanceApi, userApi } from '../api/oa'

const props = defineProps({
  displayName: String,
  today: Object,
  applications: { type: Array, default: () => [] },
  searchKeyword: String,
  currentUser: { type: Object, default: null },
  permissions: { type: Array, default: () => [] },
})
const emit = defineEmits(['open', 'manage'])

const loading = ref(false)
const unavailableCount = ref(0)
const monthData = ref({ calendar: [] })
const todayRecords = ref([])
const myApplications = ref([])
const pendingApplications = ref([])
const users = ref([])
const stats = ref({})

const permissionSet = computed(() => new Set(props.permissions))
const can = (code) => permissionSet.value.has(code)
const isManagerView = computed(() => can('approval:handle') || can('attendance:stats') || can('user:query'))
const pendingMine = computed(() => myApplications.value.filter((item) => item.status === 'PENDING'))
const monthAttendanceDays = computed(() => (monthData.value.calendar || []).filter((item) => item.checkInTime).length)
const todayRecord = computed(() => todayRecords.value[0] || null)
const todayStatus = computed(() => {
  if (!todayRecord.value) return '待打卡'
  return ({ NORMAL: '正常', LATE: '迟到', EARLY: '早退', ABSENT: '缺勤', LEAVE: '请假', OVERTIME: '加班', PENDING: '待打卡' })[todayRecord.value.attendanceStatus] || todayRecord.value.attendanceStatus || '待打卡'
})

const metricCards = computed(() => isManagerView.value ? [
  { label: props.currentUser?.roleCode === 'MANAGER' ? '部门人数' : '员工数量', value: users.value.length, suffix: '人', icon: '员', tone: 'blue' },
  { label: '综合出勤率', value: stats.value.attendanceRate || '—', suffix: '', icon: '勤', tone: 'green' },
  { label: '待处理审批', value: pendingApplications.value.length, suffix: '项', icon: '审', tone: 'purple' },
  { label: '本月考勤异常', value: Number(stats.value.lateCount || 0) + Number(stats.value.absentCount || 0), suffix: '次', icon: '异', tone: 'orange' },
] : [
  { label: '本月出勤', value: monthAttendanceDays.value, suffix: '天', icon: '勤', tone: 'blue' },
  { label: '本月迟到', value: monthData.value.lateCount || 0, suffix: '次', icon: '迟', tone: 'orange' },
  { label: '待审批申请', value: pendingMine.value.length, suffix: '项', icon: '审', tone: 'purple' },
  { label: '今日考勤', value: todayStatus.value, suffix: '', icon: '今', tone: 'green' },
])

const todoItems = computed(() => {
  const items = []
  if (can('attendance:self')) {
    const record = todayRecord.value
    const complete = Boolean(record?.checkInTime && record?.checkOutTime)
    items.push({ id: 'attendance', icon: complete ? '✓' : '勤', title: complete ? '今日打卡已完成' : record?.checkInTime ? '今天还未签退' : '今天还未签到', detail: record?.ruleName || '前往考勤页查看打卡规则', target: '考勤管理', done: complete })
  }
  pendingApplications.value.slice(0, 3).forEach((item) => items.push({ id: `pending-${item.taskId || item.applicationId}`, icon: '审', title: `${item.applicantName || '员工'}的${item.applicationTypeLabel || '申请'}`, detail: item.currentTaskName || item.reason || '等待你的审批', target: '审批' }))
  pendingMine.value.slice(0, 2).forEach((item) => items.push({ id: `mine-${item.applicationId}`, icon: '申', title: `${item.applicationTypeLabel || '申请'}正在审批`, detail: item.currentTaskName || item.statusLabel || '等待处理', target: '审批' }))
  return items
})

function localDateText(date = new Date()) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

async function loadDashboard() {
  loading.value = true
  unavailableCount.value = 0
  const today = localDateText()
  const month = today.slice(0, 7)
  const tasks = []
  const add = (request, assign) => tasks.push(request.then((result) => assign(result.data)).catch(() => { unavailableCount.value += 1 }))

  if (can('attendance:self')) {
    add(attendanceApi.month(month), (data) => { monthData.value = data || { calendar: [] } })
    add(attendanceApi.todayRecords(), (data) => { todayRecords.value = data || [] })
  }
  if (can('approval:self') || can('approval:submit')) add(applicationApi.mine(), (data) => { myApplications.value = data || [] })
  if (can('approval:handle')) add(applicationApi.pending(), (data) => { pendingApplications.value = data || [] })
  if (can('user:query')) add(userApi.list(), (data) => { users.value = data || [] })
  if (can('attendance:stats')) {
    const params = { startDate: `${month}-01`, endDate: today }
    if (props.currentUser?.roleCode === 'MANAGER' && props.currentUser?.deptId) params.deptId = props.currentUser.deptId
    add(attendanceApi.stats(params), (data) => { stats.value = data || {} })
  }
  await Promise.all(tasks)
  loading.value = false
}

onMounted(loadDashboard)
</script>

<template>
  <section class="dashboard-page">
    <div class="welcome-row">
      <div><p class="eyebrow">OA WORKSPACE</p><h1>你好，{{ displayName }}</h1><p>今天也一起把工作安排得清清楚楚。</p></div>
      <div class="welcome-actions"><button class="refresh-button" :disabled="loading" @click="loadDashboard"><span>↻</span>{{ loading ? '更新中' : '更新数据' }}</button><div class="date-card"><span>今日</span><strong>{{ today.day }}</strong><small>{{ today.detail }}</small></div></div>
    </div>

    <div v-if="unavailableCount" class="partial-notice">部分数据暂时无法加载，其他工作台功能仍可正常使用。</div>

    <div class="metric-grid">
      <article v-for="item in metricCards" :key="item.label" class="metric-card"><span :class="['metric-icon', item.tone]">{{ item.icon }}</span><div><p>{{ item.label }}</p><strong>{{ item.value }}<small>{{ item.suffix }}</small></strong></div></article>
    </div>

    <div class="dashboard-columns">
      <section class="dashboard-panel todo-panel">
        <div class="panel-heading"><div><h2>我的待办</h2><p>{{ todoItems.length ? `还有 ${todoItems.filter((item) => !item.done).length} 项需要关注` : '当前没有待办事项' }}</p></div><button v-if="can('approval:self')" @click="emit('open', '审批')">查看审批</button></div>
        <div v-if="todoItems.length" class="todo-list"><button v-for="item in todoItems" :key="item.id" :class="{ done: item.done }" @click="emit('open', item.target)"><span>{{ item.icon }}</span><div><strong>{{ item.title }}</strong><small>{{ item.detail }}</small></div><b>›</b></button></div>
        <div v-else class="todo-empty"><span>✓</span><div><strong>今天的事项都处理好了</strong><small>新的审批和考勤提醒会出现在这里</small></div></div>
      </section>
    </div>

    <section class="section-block app-section">
      <div class="section-heading"><div><h2>常用应用</h2><p>快速开始你的日常工作</p></div><button class="link-button" @click="emit('manage')">管理常用应用 →</button></div>
      <div v-if="applications.length" class="application-grid"><button v-for="application in applications" :key="application.title" class="application-card" @click="emit('open', application.title)"><span class="app-icon" :style="{ background: application.color }">{{ application.icon }}</span><span class="app-copy"><strong>{{ application.title }}</strong><small>{{ application.subtitle }}</small></span><span class="app-arrow">›</span></button></div>
      <div v-else class="empty-state">{{ searchKeyword ? `没有找到“${searchKeyword}”相关应用` : '尚未添加常用应用，点击右上角进行管理' }}</div>
    </section>
  </section>
</template>

<style scoped>
.dashboard-page { width: min(1500px, 100%); margin: 0 auto; padding: 34px 42px 60px; }
.welcome-row { display: flex; align-items: center; justify-content: space-between; gap: 24px; margin-bottom: 23px; }.welcome-row h1 { margin: 5px 0 8px; font-size: 28px; }.welcome-row > div > p:last-child { margin: 0; color: #8f959e; font-size: 12px; }.eyebrow { margin: 0; color: #3370ff; font-size: 9px; font-weight: 700; letter-spacing: 1.4px; }.welcome-actions { display: flex; align-items: center; gap: 11px; }.refresh-button { height: 38px; padding: 0 14px; border: 1px solid #dfe3e9; border-radius: 10px; color: #626870; background: #fff; cursor: pointer; }.refresh-button span { margin-right: 5px; color: #3370ff; }.date-card { min-width: 174px; display: grid; grid-template-columns: auto auto; align-items: center; column-gap: 9px; padding: 11px 15px; border: 1px solid #e7e9ed; border-radius: 13px; background: #fff; }.date-card span { color: #8f959e; font-size: 9px; }.date-card strong { grid-row: span 2; font-size: 28px; }.date-card small { color: #50555d; font-size: 9px; }
.partial-notice { margin: -9px 0 14px; padding: 9px 12px; border-radius: 9px; color: #9b6500; background: #fff7e6; font-size: 10px; }
.metric-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 13px; }.metric-card { min-height: 106px; display: flex; align-items: center; gap: 14px; padding: 18px; border: 1px solid #e9ebef; border-radius: 15px; background: #fff; box-shadow: 0 6px 22px rgba(31,35,41,.035); }.metric-icon { width: 43px; height: 43px; display: grid; place-items: center; flex: none; border-radius: 12px; font-size: 11px; font-style: normal; font-weight: 700; }.metric-card p { margin: 0 0 7px; color: #8f959e; font-size: 10px; }.metric-card strong { font-size: 23px; }.metric-card small { margin-left: 3px; color: #8f959e; font-size: 9px; font-weight: 400; }.blue { color: #3370ff; background: #edf3ff; }.green { color: #16865a; background: #e8f8f1; }.purple { color: #7655d9; background: #f1edff; }.orange { color: #bd6900; background: #fff3df; }
.dashboard-columns { margin-top: 15px; }.dashboard-panel { min-height: 220px; padding: 20px; border: 1px solid #e9ebef; border-radius: 16px; background: #fff; }.panel-heading { display: flex; align-items: center; justify-content: space-between; margin-bottom: 15px; }.panel-heading h2 { margin: 0 0 5px; font-size: 16px; }.panel-heading p { margin: 0; color: #9aa0a9; font-size: 9px; }.panel-heading > button { border: 0; color: #3370ff; background: transparent; font-size: 9px; cursor: pointer; }
.todo-list { display: flex; flex-direction: column; gap: 7px; }.todo-list button { display: grid; grid-template-columns: auto 1fr auto; align-items: center; gap: 10px; padding: 10px 3px; border: 0; border-bottom: 1px solid #f0f1f3; background: transparent; text-align: left; cursor: pointer; }.todo-list button > span { width: 33px; height: 33px; display: grid; place-items: center; border-radius: 10px; color: #3370ff; background: #edf3ff; font-size: 9px; }.todo-list strong, .todo-list small { display: block; }.todo-list strong { color: #383c42; font-size: 10px; }.todo-list small { max-width: 260px; overflow: hidden; margin-top: 4px; color: #9aa0a9; font-size: 8px; white-space: nowrap; text-overflow: ellipsis; }.todo-list b { color: #bdc1c7; font-weight: 400; }.todo-list button.done { opacity: .65; }.todo-list button.done > span { color: #16865a; background: #e8f8f1; }.todo-empty { min-height: 160px; display: flex; align-items: center; justify-content: center; gap: 12px; color: #7f858e; }.todo-empty > span { width: 42px; height: 42px; display: grid; place-items: center; border-radius: 14px; color: #16865a; background: #e8f8f1; }.todo-empty strong, .todo-empty small { display: block; }.todo-empty strong { color: #50555d; font-size: 11px; }.todo-empty small { margin-top: 5px; font-size: 8px; }
.app-section { margin-top: 15px; padding: 20px; border: 1px solid #e9ebef; border-radius: 16px; background: #fff; }.section-heading { margin-bottom: 14px; }.section-heading h2 { font-size: 17px; }.application-grid { grid-template-columns: repeat(3, 1fr); }.application-card { box-shadow: none; background: #fafbfc; }
@media (max-width: 1100px) { .metric-grid { grid-template-columns: repeat(2, 1fr); }.application-grid { grid-template-columns: repeat(2, 1fr); } }
@media (max-width: 760px) { .dashboard-page { padding: 24px 16px 45px; }.welcome-row { align-items: flex-start; flex-direction: column; }.welcome-actions { width: 100%; }.date-card { flex: 1; }.metric-grid, .application-grid { grid-template-columns: 1fr; } }
</style>
