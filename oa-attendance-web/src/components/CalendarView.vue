<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { attendanceApi, applicationApi } from '../api/oa'

const props = defineProps({
  permissions: { type: Array, default: () => [] },
  currentUser: { type: Object, default: null },
})

const emit = defineEmits(['back'])

const todayStr = new Date().toISOString().slice(0, 10)
const currentMonthStr = `${todayStr.slice(0, 4)}-${todayStr.slice(5, 7)}`
const selectedMonth = ref(currentMonthStr)
const attendanceData = ref({ calendar: [] })
const applications = ref([])
const loading = ref(false)
const errorMessage = ref('')
const selectedDay = ref(null)
const detailPanel = ref({ visible: false, day: null, record: null, apps: [] })

const monthLabel = computed(() => {
  const [y, m] = selectedMonth.value.split('-')
  return `${y}年${Number(m)}月`
})

const weekHeaders = ['一', '二', '三', '四', '五', '六', '日']

const calendarGrid = computed(() => {
  const [year, month] = selectedMonth.value.split('-').map(Number)
  const firstDay = new Date(year, month - 1, 1)
  const lastDay = new Date(year, month, 0)
  const daysInMonth = lastDay.getDate()
  const startDayOfWeek = firstDay.getDay() || 7

  const recordMap = new Map()
  for (const item of attendanceData.value.calendar || []) {
    recordMap.set(item.attendanceDate, item)
  }

  const appMap = new Map()
  for (const app of applications.value) {
    if (app.status !== 'APPROVED' && app.status !== 'PROCESSING') continue
    const start = app.startTime ? app.startTime.slice(0, 10) : null
    const end = app.endTime ? app.endTime.slice(0, 10) : null
    if (!start) continue
    let d = new Date(start)
    const endDate = end ? new Date(end) : new Date(start)
    while (d <= endDate) {
      const key = d.toISOString().slice(0, 10)
      if (!appMap.has(key)) appMap.set(key, [])
      appMap.get(key).push(app)
      d.setDate(d.getDate() + 1)
    }
  }

  const today = new Date().toISOString().slice(0, 10)
  const rows = []
  let week = { days: [] }
  for (let i = 0; i < startDayOfWeek - 1; i++) {
    week.days.push(null)
  }
  for (let day = 1; day <= daysInMonth; day++) {
    const date = `${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`
    const record = recordMap.get(date) || null
    const apps = appMap.get(date) || []
    week.days.push({ day, date, record, applications: apps, isToday: date === today })
    if (week.days.length === 7) {
      rows.push(week)
      week = { days: [] }
    }
  }
  while (week.days.length && week.days.length < 7) {
    week.days.push(null)
  }
  if (week.days.length) rows.push(week)
  return rows
})

const summary = computed(() => {
  const data = attendanceData.value
  return {
    workMinutes: data.workMinutes || 0,
    lateCount: data.lateCount || 0,
    absentCount: data.absentCount || 0,
    overtimeCount: data.overtimeCount || 0,
    overtimeMinutes: data.overtimeMinutes || 0,
    leaveCount: data.leaveCount || 0,
  }
})

const pendingCount = computed(() => applications.value.filter((a) => a.status === 'PROCESSING').length)

async function loadData() {
  loading.value = true
  errorMessage.value = ''
  try {
    const [attResult, appResult] = await Promise.all([
      attendanceApi.month(selectedMonth.value),
      applicationApi.mine(),
    ])
    attendanceData.value = { calendar: [], ...(attResult.data || {}) }
    applications.value = appResult.data || []
  } catch (e) {
    errorMessage.value = e.message
  } finally {
    loading.value = false
  }
}

function formatMonth(y, m) {
  return `${y}-${String(m).padStart(2, '0')}`
}

function prevMonth() {
  const [y, m] = selectedMonth.value.split('-').map(Number)
  const prev = m === 1 ? [y - 1, 12] : [y, m - 1]
  selectedMonth.value = formatMonth(prev[0], prev[1])
}

function nextMonth() {
  const [y, m] = selectedMonth.value.split('-').map(Number)
  const next = m === 12 ? [y + 1, 1] : [y, m + 1]
  selectedMonth.value = formatMonth(next[0], next[1])
}

function dayClass(day) {
  if (!day) return 'empty'
  const classes = []
  if (day.isToday) classes.push('today')
  if (day.record) {
    const status = day.record.attendanceStatus
    if (status === 'NORMAL') classes.push('day-normal')
    else if (status === 'LATE') classes.push('day-late')
    else if (status === 'EARLY') classes.push('day-early')
    else if (status === 'ABSENT') classes.push('day-absent')
    else if (status === 'LEAVE') classes.push('day-leave')
    else if (status === 'OVERTIME') classes.push('day-overtime')
  }
  if (day.applications?.length) classes.push('has-application')
  return classes.join(' ')
}

function statusDot(day) {
  if (!day?.record) return ''
  const map = { NORMAL: '✓', LATE: '迟', EARLY: '早', ABSENT: '缺', LEAVE: '假', OVERTIME: '加', PENDING: '待' }
  return map[day.record.attendanceStatus] || ''
}

function statusLabel(day) {
  if (!day?.record) return ''
  const map = { NORMAL: '正常出勤', LATE: '迟到', EARLY: '早退', ABSENT: '缺勤', LEAVE: '请假', OVERTIME: '加班', PENDING: '待打卡' }
  return map[day.record.attendanceStatus] || ''
}

function openDay(day) {
  if (!day) return
  detailPanel.value = { visible: true, day, record: day.record, apps: day.applications || [] }
}

function applicationTypeLabel(type) {
  return { LEAVE: '请假', OVERTIME: '加班', MAKEUP: '补卡' }[type] || type
}

function appStatusClass(status) {
  return { PROCESSING: 'processing', APPROVED: 'approved', REJECTED: 'rejected', CANCELED: 'rejected' }[status] || ''
}

function appStatusLabel(status) {
  return { PROCESSING: '审批中', APPROVED: '已通过', REJECTED: '已驳回', CANCELED: '已撤回' }[status] || status
}

function formatDateTime(value) {
  return value ? String(value).replace('T', ' ').slice(0, 16) : '—'
}

function minutesToHours(value) {
  return `${(Number(value || 0) / 60).toFixed(1)}h`
}

watch(selectedMonth, loadData)
onMounted(loadData)
</script>

<template>
  <div class="calendar-page">
    <div class="management-heading">
      <div>
        <button class="back-button" @click="emit('back')">← 返回工作台</button>
        <h1>考勤日历</h1>
        <p>查看每日考勤状态与审批信息</p>
      </div>
      <button class="refresh-button" :disabled="loading" @click="loadData">
        <span>↻</span>{{ loading ? '刷新中' : '刷新' }}
      </button>
    </div>

    <div v-if="errorMessage" class="api-error">{{ errorMessage }}</div>

    <!-- Month navigator -->
    <div class="calendar-nav">
      <button @click="prevMonth">‹</button>
      <h2>{{ monthLabel }}</h2>
      <button @click="nextMonth">›</button>
      <button class="today-btn" @click="selectedMonth = currentMonthStr">今天</button>
    </div>

    <!-- Summary cards -->
    <div class="calendar-summary">
      <article class="cal-summary-card normal"><span>正常</span><strong>{{ summary.workMinutes ? ((summary.workMinutes / 60 / 22).toFixed(0)) : '—' }}<small> 天</small></strong></article>
      <article class="cal-summary-card late"><span>迟到</span><strong>{{ summary.lateCount }}<small> 次</small></strong></article>
      <article class="cal-summary-card absent"><span>缺勤</span><strong>{{ summary.absentCount }}<small> 次</small></strong></article>
      <article class="cal-summary-card leave"><span>请假</span><strong>{{ summary.leaveCount }}<small> 次</small></strong></article>
      <article class="cal-summary-card overtime"><span>加班</span><strong>{{ summary.overtimeCount }}<small> 次</small></strong></article>
      <article class="cal-summary-card pending"><span>待审批</span><strong>{{ pendingCount }}<small> 条</small></strong></article>
    </div>

    <!-- Legend -->
    <div class="calendar-legend">
      <span v-for="item in [
        { cls: 'day-normal', label: '正常' }, { cls: 'day-late', label: '迟到' },
        { cls: 'day-absent', label: '缺勤' }, { cls: 'day-leave', label: '请假' },
        { cls: 'day-overtime', label: '加班' }, { cls: 'day-pending', label: '待打卡' },
      ]" :key="item.cls" class="legend-item"><i :class="item.cls"></i>{{ item.label }}</span>
    </div>

    <!-- Calendar grid -->
    <div class="calendar-grid-wrapper">
      <div v-if="loading" class="calendar-loading">加载中…</div>
      <table v-else class="calendar-grid-table">
        <thead>
          <tr>
            <th v-for="h in weekHeaders" :key="h" :class="{ weekend: h === '六' || h === '日' }">{{ h }}</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(row, ri) in calendarGrid" :key="ri">
            <td v-for="(day, di) in row.days" :key="di" :class="dayClass(day)" @click="openDay(day)">
              <template v-if="day">
                <span class="cal-day-num">{{ day.day }}</span>
                <span v-if="statusDot(day)" class="cal-day-dot">{{ statusDot(day) }}</span>
                <span v-if="day.applications?.length" class="cal-app-badge">{{ day.applications.length }}</span>
              </template>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Day detail popover -->
    <div v-if="detailPanel.visible" class="dialog-mask" @click.self="detailPanel.visible = false">
      <div class="data-dialog calendar-detail-dialog">
        <div class="dialog-heading">
          <div><h2>{{ detailPanel.day?.date }}</h2><p>{{ statusLabel(detailPanel.day) || '未打卡' }}</p></div>
          <button type="button" @click="detailPanel.visible = false">×</button>
        </div>

        <template v-if="detailPanel.record">
          <div class="cal-detail-grid">
            <div><span>签到</span><strong>{{ formatDateTime(detailPanel.record.checkInTime) }}</strong></div>
            <div><span>签退</span><strong>{{ formatDateTime(detailPanel.record.checkOutTime) }}</strong></div>
            <div><span>考勤状态</span><strong>{{ statusLabel(detailPanel.day) }}</strong></div>
            <div><span>考勤规则</span><strong>{{ detailPanel.record.ruleName || '—' }}</strong></div>
            <div><span>工作时长</span><strong>{{ minutesToHours(detailPanel.record.workMinutes) }}</strong></div>
            <div><span>加班时长</span><strong>{{ minutesToHours(detailPanel.record.overtimeMinutes) }}</strong></div>
          </div>
        </template>
        <div v-else class="cal-detail-empty">当日无考勤记录</div>

        <div v-if="detailPanel.apps.length" class="cal-detail-apps">
          <h3>关联申请</h3>
          <div v-for="app in detailPanel.apps" :key="app.applicationId" class="cal-detail-app-item">
            <span :class="['approval-status', appStatusClass(app.status)]">{{ appStatusLabel(app.status) }}</span>
            <strong>{{ applicationTypeLabel(app.applicationType) }}</strong>
            <small>{{ formatDateTime(app.startTime) }} ~ {{ formatDateTime(app.endTime) }}</small>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
