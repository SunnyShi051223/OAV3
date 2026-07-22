<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { attendanceApi } from '../api/oa'

defineEmits(['back'])

const rules = ref([])
const selectedRuleId = ref(null)
const todayRecords = ref([])
const selectedMonth = ref(new Date().toISOString().slice(0, 7))
const monthData = ref({ calendar: [] })
const loading = ref(false)
const checking = ref('')
const message = ref('')
const error = ref('')
const checkForm = ref({ latitude: null, longitude: null, locationAddress: '', wifiSsid: '', wifiBssid: '' })
const clock = ref(new Date())
let clockTimer

const selectedRule = computed(() => rules.value.find((item) => Number(item.ruleId) === Number(selectedRuleId.value)) || null)
const todayRecord = computed(() => todayRecords.value.find((item) => Number(item.ruleId) === Number(selectedRuleId.value)) || null)
const currentStatus = computed(() => todayRecord.value?.attendanceStatus || (selectedRule.value ? 'PENDING' : 'NO_RULE'))
const dateText = computed(() => clock.value.toLocaleDateString('zh-CN', { month: 'long', day: 'numeric', weekday: 'long' }))
const timeText = computed(() => clock.value.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: false }))

async function load() {
  loading.value = true
  error.value = ''
  try {
    const [ruleResult, recordResult, monthResult] = await Promise.all([
      attendanceApi.availableRules(), attendanceApi.todayRecords(), attendanceApi.month(selectedMonth.value),
    ])
    rules.value = ruleResult.data || []
    todayRecords.value = recordResult.data || []
    monthData.value = monthResult.data || { calendar: [] }
    if (!rules.value.some((item) => Number(item.ruleId) === Number(selectedRuleId.value))) selectedRuleId.value = rules.value[0]?.ruleId || null
  } catch (requestError) {
    error.value = requestError.message
  } finally {
    loading.value = false
  }
}

async function loadMonth() {
  loading.value = true
  error.value = ''
  try {
    const result = await attendanceApi.month(selectedMonth.value)
    monthData.value = result.data || { calendar: [] }
  } catch (requestError) {
    error.value = requestError.message
  } finally {
    loading.value = false
  }
}

async function submit(action) {
  if (!selectedRule.value) return
  checking.value = action
  error.value = ''
  message.value = ''
  try {
    const payload = { ruleId: selectedRule.value.ruleId, ...checkForm.value, clientInfo: navigator.userAgent }
    const result = action === 'in' ? await attendanceApi.checkIn(payload) : await attendanceApi.checkOut(payload)
    message.value = result.msg || (action === 'in' ? '签到成功' : '签退成功')
    await load()
  } catch (requestError) {
    error.value = requestError.message
  } finally {
    checking.value = ''
  }
}

function getLocation() {
  error.value = ''
  if (!navigator.geolocation) {
    error.value = '当前浏览器不支持定位'
    return
  }
  navigator.geolocation.getCurrentPosition((position) => {
    checkForm.value.latitude = Number(position.coords.latitude.toFixed(6))
    checkForm.value.longitude = Number(position.coords.longitude.toFixed(6))
    checkForm.value.locationAddress = '浏览器定位'
  }, (locationError) => {
    error.value = locationError.message || '定位获取失败，请检查浏览器定位权限'
  }, { enableHighAccuracy: true, timeout: 10000 })
}

function ruleCheckText(rule) {
  const checks = []
  if (Number(rule?.requireLocation) === 1) checks.push('定位')
  if (Number(rule?.requireWifi) === 1) checks.push('WiFi')
  return checks.length ? checks.join(' + ') : '无需范围校验'
}

function formatRuleTime(value) { return value ? String(value).slice(0, 5) : '—' }
function formatCheckTime(value) { return value ? String(value).slice(0, 5) : '—' }
function statusLabel(value) {
  return ({ NORMAL: '正常', PENDING: '待打卡', LATE: '迟到', EARLY: '早退', ABSENT: '缺勤', LEAVE: '请假', OVERTIME: '加班', NO_RULE: '无考勤规则' })[value] || value || '待打卡'
}
function statusClass(value) { return String(value || 'PENDING').toLowerCase() }
function minutesToHours(value) {
  const minutes = Number(value || 0)
  return `${Math.floor(minutes / 60)}h ${minutes % 60}m`
}

onMounted(() => {
  load()
  clockTimer = window.setInterval(() => { clock.value = new Date() }, 1000)
})
onBeforeUnmount(() => window.clearInterval(clockTimer))
</script>

<template>
  <section class="attendance-page">
    <div class="management-heading attendance-heading">
      <div><button class="back-button" @click="$emit('back')">← 返回工作台</button><h1>我的考勤</h1><p>完成每日签到、签退并查看考勤记录</p></div>
      <button class="attendance-refresh" :disabled="loading" @click="load"><span>↻</span>{{ loading ? '刷新中' : '刷新' }}</button>
    </div>
    <div v-if="message" class="attendance-notice">✓ {{ message }}</div><div v-if="error" class="api-error">{{ error }}</div>
    <div class="attendance-overview">
      <article class="clock-card"><p>{{ dateText }}</p><strong>{{ timeText }}</strong><span>请在规定时间与考勤范围内完成打卡</span><i></i><i></i></article>
      <article class="attendance-card rule-card">
        <div class="attendance-card-title"><div><span>今日考勤规则</span><h2>{{ selectedRule?.ruleName || '暂无可用规则' }}</h2></div><b>{{ rules.length }} 条</b></div>
        <label v-if="rules.length > 1" class="attendance-rule-select">选择规则<select v-model.number="selectedRuleId"><option v-for="rule in rules" :key="rule.ruleId" :value="rule.ruleId">{{ rule.ruleName }}</option></select></label>
        <div v-if="selectedRule" class="rule-detail-grid"><div><span>上班时间</span><strong>{{ formatRuleTime(selectedRule.workStartTime) }}</strong></div><div><span>下班时间</span><strong>{{ formatRuleTime(selectedRule.workEndTime) }}</strong></div><div><span>适用部门</span><strong>{{ selectedRule.deptName || '全公司' }}</strong></div><div><span>校验方式</span><strong>{{ ruleCheckText(selectedRule) }}</strong></div></div>
        <div v-else class="attendance-inline-empty">请联系管理员配置今日生效的考勤规则</div>
      </article>
      <article class="attendance-card punch-card">
        <div class="attendance-card-title"><div><span>今日打卡</span><h2>{{ statusLabel(currentStatus) }}</h2></div><b :class="statusClass(currentStatus)">{{ statusLabel(currentStatus) }}</b></div>
        <div class="punch-timeline"><div :class="{ done: todayRecord?.checkInTime }"><i></i><span>上班签到</span><strong>{{ formatCheckTime(todayRecord?.checkInTime) }}</strong><small>{{ statusLabel(todayRecord?.checkInStatus || 'PENDING') }}</small></div><div :class="{ done: todayRecord?.checkOutTime }"><i></i><span>下班签退</span><strong>{{ formatCheckTime(todayRecord?.checkOutTime) }}</strong><small>{{ statusLabel(todayRecord?.checkOutStatus || 'PENDING') }}</small></div></div>
        <div v-if="Number(selectedRule?.requireLocation) === 1" class="location-row"><button type="button" @click="getLocation">⌖ 获取当前位置</button><span>{{ checkForm.locationAddress || '尚未获取定位' }}</span></div>
        <p v-if="Number(selectedRule?.requireWifi) === 1" class="wifi-tip">当前规则要求企业 WiFi；浏览器无法自动读取 WiFi 名称，请使用已接入企业 WiFi 能力的客户端打卡。</p>
        <div class="punch-actions"><button :disabled="!selectedRule || Boolean(todayRecord?.checkInTime) || Boolean(checking)" @click="submit('in')">{{ checking === 'in' ? '签到中…' : todayRecord?.checkInTime ? '已签到' : '签到' }}</button><button class="checkout-button" :disabled="!selectedRule || !todayRecord?.checkInTime || Boolean(todayRecord?.checkOutTime) || Boolean(checking)" @click="submit('out')">{{ checking === 'out' ? '签退中…' : todayRecord?.checkOutTime ? '已签退' : '签退' }}</button></div>
      </article>
    </div>
    <div class="attendance-month-heading"><div><p class="eyebrow">MONTHLY ATTENDANCE</p><h2>月度考勤</h2></div><label><input v-model="selectedMonth" type="month" /><button :disabled="loading" @click="loadMonth">查询</button></label></div>
    <div class="attendance-metrics"><article><span>累计工时</span><strong>{{ minutesToHours(monthData.workMinutes) }}</strong></article><article><span>迟到</span><strong>{{ monthData.lateCount || 0 }}<small> 次</small></strong></article><article><span>缺勤</span><strong>{{ monthData.absentCount || 0 }}<small> 次</small></strong></article><article><span>加班</span><strong>{{ monthData.overtimeCount || 0 }}<small> 次</small></strong></article><article><span>加班时长</span><strong>{{ minutesToHours(monthData.overtimeMinutes) }}</strong></article><article><span>请假</span><strong>{{ monthData.leaveCount || 0 }}<small> 次</small></strong></article></div>
    <div class="data-panel attendance-records"><table><thead><tr><th>日期</th><th>考勤规则</th><th>签到</th><th>签退</th><th>状态</th><th>工作时长</th><th>加班时长</th></tr></thead><tbody><tr v-for="item in monthData.calendar || []" :key="item.recordId"><td class="name-cell">{{ item.attendanceDate }}</td><td>{{ item.ruleName || '—' }}</td><td>{{ formatCheckTime(item.checkInTime) }}</td><td>{{ formatCheckTime(item.checkOutTime) }}</td><td><span :class="['attendance-status-tag', statusClass(item.attendanceStatus)]">{{ statusLabel(item.attendanceStatus) }}</span></td><td>{{ minutesToHours(item.workMinutes) }}</td><td>{{ minutesToHours(item.overtimeMinutes) }}</td></tr></tbody></table><div v-if="!loading && !(monthData.calendar || []).length" class="approval-empty"><span>勤</span><strong>本月暂无考勤记录</strong><p>完成签到后，记录会自动显示在这里。</p></div></div>
  </section>
</template>
