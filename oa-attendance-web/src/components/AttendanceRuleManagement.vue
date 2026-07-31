<script setup>
import { computed, onMounted, ref } from 'vue'
import { attendanceApi, departmentApi } from '../api/oa'

const props = defineProps({ permissions: { type: Array, default: () => [] }, roleCode: { type: String, default: '' }, currentDeptId: { type: Number, default: null } })
const emit = defineEmits(['back'])
const rules = ref([])
const departments = ref([])
const loading = ref(false)
const error = ref('')
const notice = ref('')
const deptFilter = ref('')
const statusFilter = ref('')
const dialog = ref({ visible: false, mode: 'create', form: defaultForm(), error: '', saving: false })
const weekdays = [{ value: 1, label: '周一' }, { value: 2, label: '周二' }, { value: 3, label: '周三' }, { value: 4, label: '周四' }, { value: 5, label: '周五' }, { value: 6, label: '周六' }, { value: 7, label: '周日' }]

const permissionSet = computed(() => new Set(props.permissions))
const filteredRules = computed(() => rules.value.filter((item) => {
  if (deptFilter.value !== '' && Number(item.deptId || 0) !== Number(deptFilter.value)) return false
  if (statusFilter.value && ruleState(item).value !== statusFilter.value) return false
  return true
}))

onMounted(load)

function can(code) { return permissionSet.value.has(code) }
function canManage(item) {
  if (props.roleCode === 'ADMIN') return true
  if (props.currentDeptId == null) return false
  const ruleDeptId = Number(item.deptId)
  if (ruleDeptId === Number(props.currentDeptId)) return true
  // 沿部门树向上查找：规则所属部门是否为当前用户部门的子孙
  return isAncestorOf(Number(props.currentDeptId), ruleDeptId)
}
function isAncestorOf(ancestorId, descendantId) {
  let currentId = descendantId
  while (currentId && currentId > 0) {
    const dept = departments.value.find((d) => Number(d.deptId) === currentId)
    if (!dept || !dept.parentId || Number(dept.parentId) === 0) return false
    if (Number(dept.parentId) === ancestorId) return true
    currentId = Number(dept.parentId)
  }
  return false
}

function defaultForm() {
  const date = new Date()
  const start = date.toISOString().slice(0, 10)
  return { ruleId: null, ruleName: '', deptId: props.roleCode === 'MANAGER' ? props.currentDeptId : null, effectiveStartDate: start, effectiveEndDate: `${date.getFullYear()}-12-31`, workStartTime: '09:00', workEndTime: '18:00', checkInEndTime: '09:10', checkOutStartTime: '17:30', checkOutEndTime: '22:00', lateThreshold: 10, earlyThreshold: 10, overtimeThreshold: 30, workDays: [1, 2, 3, 4, 5], requireWifi: 0, requireLocation: 0, enabled: 1, wifiList: [], locationList: [] }
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    const [ruleResult, deptResult] = await Promise.all([attendanceApi.rules(), departmentApi.list()])
    rules.value = ruleResult.data || []
    departments.value = deptResult.data || []
  } catch (err) { error.value = err.message } finally { loading.value = false }
}

function openCreate() { dialog.value = { visible: true, mode: 'create', form: defaultForm(), error: '', saving: false } }

async function openEdit(item) {
  if (!canManage(item)) return
  dialog.value = { visible: true, mode: 'edit', form: defaultForm(), error: '', saving: true }
  try {
    const result = await attendanceApi.rule(item.ruleId)
    const detail = result.data || item
    dialog.value.form = { ...defaultForm(), ...detail, deptId: detail.deptId ?? null, effectiveStartDate: dateInput(detail.effectiveStartDate), effectiveEndDate: dateInput(detail.effectiveEndDate), workStartTime: timeInput(detail.workStartTime), workEndTime: timeInput(detail.workEndTime), checkInEndTime: timeInput(detail.checkInEndTime), checkOutStartTime: timeInput(detail.checkOutStartTime), checkOutEndTime: timeInput(detail.checkOutEndTime), workDays: parseWorkDays(detail.workDays), wifiList: (detail.wifiList || []).map((wifi) => ({ wifiSsid: wifi.wifiSsid, wifiBssid: wifi.wifiBssid || '', enabled: 1 })), locationList: (detail.locationList || []).map((location) => ({ locationName: location.locationName, latitude: location.latitude, longitude: location.longitude, radius: location.radius || 100, enabled: 1 })) }
  } catch (err) { dialog.value.error = err.message } finally { dialog.value.saving = false }
}

async function save() {
  const state = dialog.value
  state.error = ''
  if (!state.form.workDays.length) { state.error = '请至少选择一个工作日'; return }
  if (state.form.requireWifi && !state.form.wifiList.some((item) => item.wifiSsid.trim())) { state.error = '启用 WiFi 校验时，请至少添加一个 WiFi'; return }
  if (state.form.requireLocation && !state.form.locationList.some((item) => item.locationName.trim() && item.latitude != null && item.longitude != null)) { state.error = '启用定位校验时，请至少添加一个办公地点'; return }
  state.saving = true
  try {
    const form = state.form
    const payload = { ruleId: form.ruleId || null, ruleName: form.ruleName.trim(), deptId: form.deptId === '' ? null : form.deptId, effectiveStartDate: form.effectiveStartDate || null, effectiveEndDate: form.effectiveEndDate || null, workStartTime: form.workStartTime, workEndTime: form.workEndTime, checkInStartTime: form.workStartTime, checkInEndTime: form.checkInEndTime || null, checkOutStartTime: form.checkOutStartTime || null, checkOutEndTime: form.checkOutEndTime || null, lateThreshold: Number(form.lateThreshold || 0), earlyThreshold: Number(form.earlyThreshold || 0), overtimeThreshold: Number(form.overtimeThreshold || 0), workDays: JSON.stringify([...form.workDays].sort()), requireWifi: Number(form.requireWifi), requireLocation: Number(form.requireLocation), enabled: Number(form.enabled), wifiList: form.requireWifi ? form.wifiList.filter((item) => item.wifiSsid.trim()).map((item) => ({ ...item, enabled: 1 })) : [], locationList: form.requireLocation ? form.locationList.filter((item) => item.locationName.trim() && item.latitude != null && item.longitude != null).map((item) => ({ ...item, enabled: 1 })) : [] }
    await attendanceApi[state.mode === 'create' ? 'createRule' : 'updateRule'](payload)
    state.visible = false
    showNotice(state.mode === 'create' ? '考勤规则创建成功' : '考勤规则修改成功')
    await load()
  } catch (err) { state.error = err.message } finally { state.saving = false }
}

async function remove(item) {
  if (!canManage(item) || !window.confirm(`确定删除“${item.ruleName}”吗？`)) return
  try { await attendanceApi.removeRule(item.ruleId); showNotice('考勤规则已删除'); await load() } catch (err) { error.value = err.message }
}

function addWifi() { dialog.value.form.wifiList.push({ wifiSsid: '', wifiBssid: '', enabled: 1 }) }
function addLocation() { dialog.value.form.locationList.push({ locationName: '', latitude: null, longitude: null, radius: 100, enabled: 1 }) }
function showNotice(message) { notice.value = message; window.setTimeout(() => { if (notice.value === message) notice.value = '' }, 2500) }
function timeInput(value) { return value ? String(value).slice(0, 5) : '' }
function dateInput(value) { return value ? String(value).slice(0, 10) : '' }
function parseWorkDays(value) { try { const result = JSON.parse(value || '[]'); return Array.isArray(result) ? result.map(Number) : [] } catch (_) { return [1, 2, 3, 4, 5] } }
function workDayText(value) { const selected = parseWorkDays(value); return selected.length === 7 ? '每天' : weekdays.filter((item) => selected.includes(item.value)).map((item) => item.label.replace('周', '')).join('、') }
function ruleState(item) { const today = new Date().toISOString().slice(0, 10); if (!Number(item.enabled)) return { value: 'disabled', label: '已停用' }; if (item.effectiveStartDate && today < dateInput(item.effectiveStartDate)) return { value: 'upcoming', label: '待生效' }; if (item.effectiveEndDate && today > dateInput(item.effectiveEndDate)) return { value: 'expired', label: '已过期' }; return { value: 'active', label: '生效中' } }
</script>

<template>
  <section class="rule-management-page">
    <div class="management-heading"><div><button class="back-button" @click="emit('back')">← 返回工作台</button><h1>考勤规则</h1><p>为公司或部门配置班次、有效期和打卡校验方式</p></div><button v-if="can('attendance:rule:add')" class="primary-button" @click="openCreate">＋ 新建规则</button></div>
    <div class="rule-summary-row"><article><span>规则总数</span><strong>{{ rules.length }}</strong></article><article><span>生效中</span><strong>{{ rules.filter((item) => ruleState(item).value === 'active').length }}</strong></article><article><span>覆盖部门</span><strong>{{ new Set(rules.filter((item) => item.deptId).map((item) => item.deptId)).size }}</strong></article></div>
    <div class="rule-toolbar"><select v-model="deptFilter"><option value="">全部部门</option><option :value="0">全公司</option><option v-for="dept in departments" :key="dept.deptId" :value="dept.deptId">{{ dept.deptName }}</option></select><select v-model="statusFilter"><option value="">全部状态</option><option value="active">生效中</option><option value="upcoming">待生效</option><option value="expired">已过期</option><option value="disabled">已停用</option></select><button :disabled="loading" @click="load">↻ {{ loading ? '刷新中' : '刷新' }}</button></div>
    <div v-if="notice" class="attendance-notice">✓ {{ notice }}</div><div v-if="error" class="api-error">{{ error }}</div>
    <div class="rule-grid"><article v-for="item in filteredRules" :key="item.ruleId" class="rule-list-card"><header><div><span>{{ item.deptName || '全公司' }}</span><h2>{{ item.ruleName }}</h2></div><b :class="ruleState(item).value">{{ ruleState(item).label }}</b></header><div class="rule-shift"><strong>{{ timeInput(item.workStartTime) }}</strong><i></i><strong>{{ timeInput(item.workEndTime) }}</strong></div><dl><div><dt>有效期</dt><dd>{{ dateInput(item.effectiveStartDate) || '不限' }} 至 {{ dateInput(item.effectiveEndDate) || '不限' }}</dd></div><div><dt>工作日</dt><dd>{{ workDayText(item.workDays) }}</dd></div><div><dt>签到窗口</dt><dd>{{ timeInput(item.workStartTime) }} - {{ timeInput(item.checkInEndTime) || '未设置' }}</dd></div><div><dt>打卡校验</dt><dd>{{ item.requireWifi ? 'WiFi ' : '' }}{{ item.requireLocation ? '定位' : '' }}{{ !item.requireWifi && !item.requireLocation ? '无需校验' : '' }}</dd></div></dl><footer><button v-if="can('attendance:rule:update') && canManage(item)" @click="openEdit(item)">编辑</button><button v-if="can('attendance:rule:delete') && canManage(item)" class="danger" @click="remove(item)">删除</button></footer></article></div>
    <div v-if="!loading && !filteredRules.length" class="approval-empty"><span>勤</span><strong>暂无符合条件的考勤规则</strong><p>新建规则后，适用部门的员工即可进行签到。</p></div>

    <div v-if="dialog.visible" class="dialog-mask" @click.self="dialog.visible = false"><form class="data-dialog rule-dialog" @submit.prevent="save"><div class="dialog-heading"><div><h2>{{ dialog.mode === 'create' ? '新建' : '编辑' }}考勤规则</h2><p>设置适用范围、班次和打卡要求</p></div><button type="button" @click="dialog.visible = false">×</button></div><div class="form-grid">
      <label>规则名称<input v-model.trim="dialog.form.ruleName" maxlength="100" placeholder="如：前端组标准班次" required /></label><label>适用部门<select v-model="dialog.form.deptId" :disabled="roleCode === 'MANAGER'"><option :value="null">全公司</option><option v-for="dept in departments" :key="dept.deptId" :value="dept.deptId">{{ dept.deptName }}</option></select></label>
      <label>生效开始日期<input v-model="dialog.form.effectiveStartDate" type="date" /></label><label>生效结束日期<input v-model="dialog.form.effectiveEndDate" type="date" :min="dialog.form.effectiveStartDate" /></label>
      <label>上班时间<input v-model="dialog.form.workStartTime" type="time" required /></label><label>下班时间<input v-model="dialog.form.workEndTime" type="time" required /></label><label>正常签到截止<input v-model="dialog.form.checkInEndTime" type="time" required /></label><label>允许签退开始<input v-model="dialog.form.checkOutStartTime" type="time" required /></label><label>签退截止<input v-model="dialog.form.checkOutEndTime" type="time" required /></label><label>迟到宽限（分钟）<input v-model.number="dialog.form.lateThreshold" type="number" min="0" /></label><label>早退宽限（分钟）<input v-model.number="dialog.form.earlyThreshold" type="number" min="0" /></label><label>加班判定（分钟）<input v-model.number="dialog.form.overtimeThreshold" type="number" min="0" /></label>
      <fieldset class="full-field weekday-field"><legend>工作日</legend><label v-for="day in weekdays" :key="day.value"><input v-model="dialog.form.workDays" type="checkbox" :value="day.value" />{{ day.label }}</label></fieldset>
      <div class="full-field rule-switches"><label><input v-model="dialog.form.enabled" type="checkbox" :true-value="1" :false-value="0" />启用规则</label><label><input v-model="dialog.form.requireWifi" type="checkbox" :true-value="1" :false-value="0" />WiFi 校验</label><label><input v-model="dialog.form.requireLocation" type="checkbox" :true-value="1" :false-value="0" />定位校验</label></div>
      <section v-if="dialog.form.requireWifi" class="full-field rule-subsection"><header><div><strong>允许的 WiFi</strong><span>员工需连接以下网络</span></div><button type="button" @click="addWifi">＋ 添加</button></header><div v-for="(wifi, index) in dialog.form.wifiList" :key="index" class="wifi-form-row"><input v-model.trim="wifi.wifiSsid" placeholder="WiFi 名称（SSID）" /><input v-model.trim="wifi.wifiBssid" placeholder="BSSID，可选" /><button type="button" @click="dialog.form.wifiList.splice(index, 1)">删除</button></div></section>
      <section v-if="dialog.form.requireLocation" class="full-field rule-subsection"><header><div><strong>允许的办公地点</strong><span>填写中心坐标和有效半径</span></div><button type="button" @click="addLocation">＋ 添加</button></header><div v-for="(location, index) in dialog.form.locationList" :key="index" class="location-form-row"><input v-model.trim="location.locationName" placeholder="地点名称" /><input v-model.number="location.latitude" type="number" step="0.000001" placeholder="纬度" /><input v-model.number="location.longitude" type="number" step="0.000001" placeholder="经度" /><input v-model.number="location.radius" type="number" min="1" placeholder="半径/米" /><button type="button" @click="dialog.form.locationList.splice(index, 1)">删除</button></div></section>
    </div><div v-if="dialog.error" class="form-error">{{ dialog.error }}</div><div class="dialog-actions"><button type="button" @click="dialog.visible = false">取消</button><button class="primary-button" :disabled="dialog.saving">{{ dialog.saving ? '保存中…' : '保存规则' }}</button></div></form></div>
  </section>
</template>

<style scoped>
.rule-management-page { width: min(1500px, 100%); margin: 0 auto; padding: 36px 42px 60px; }
.rule-summary-row { display: grid; grid-template-columns: repeat(3, 1fr); gap: 13px; margin-bottom: 14px; }
.rule-summary-row article { padding: 18px 20px; border: 1px solid #eaecf0; border-radius: 14px; background: #fff; }
.rule-summary-row span, .rule-summary-row strong { display: block; }.rule-summary-row span { color: #8f959e; font-size: 11px; }.rule-summary-row strong { margin-top: 8px; font-size: 23px; }
.rule-toolbar { display: flex; gap: 9px; margin-bottom: 14px; }.rule-toolbar select, .rule-toolbar button { height: 38px; padding: 0 13px; border: 1px solid #dfe3e9; border-radius: 9px; background: #fff; }.rule-toolbar select { min-width: 150px; }.rule-toolbar button { color: #3370ff; cursor: pointer; }
.rule-grid { display: grid; grid-template-columns: repeat(3, minmax(290px, 1fr)); gap: 14px; }.rule-list-card { padding: 20px; border: 1px solid #e8ebf0; border-radius: 16px; background: #fff; box-shadow: 0 7px 24px rgba(31,35,41,.04); }.rule-list-card header { display: flex; justify-content: space-between; gap: 12px; }.rule-list-card header span { color: #8f959e; font-size: 10px; }.rule-list-card h2 { margin: 6px 0 0; font-size: 17px; }.rule-list-card header b { height: 23px; padding: 5px 9px; border-radius: 12px; font-size: 9px; font-weight: 500; }.rule-list-card header b.active { color: #16865a; background: #e8f8f1; }.rule-list-card header b.upcoming { color: #3370ff; background: #edf3ff; }.rule-list-card header b.expired, .rule-list-card header b.disabled { color: #8f959e; background: #f0f1f2; }
.rule-shift { display: flex; align-items: center; gap: 12px; margin: 22px 0; padding: 14px; border-radius: 11px; background: #f7f8fa; }.rule-shift strong { font-size: 21px; }.rule-shift i { height: 1px; flex: 1; background: #cfd5dd; }
.rule-list-card dl { display: grid; grid-template-columns: 1fr 1fr; gap: 15px; margin: 0; }.rule-list-card dt { margin-bottom: 5px; color: #a2a7ae; font-size: 9px; }.rule-list-card dd { overflow: hidden; margin: 0; color: #50555d; font-size: 11px; text-overflow: ellipsis; white-space: nowrap; }.rule-list-card footer { display: flex; justify-content: flex-end; gap: 5px; margin-top: 18px; padding-top: 14px; border-top: 1px solid #f0f1f3; }.rule-list-card footer button { padding: 5px 8px; border: 0; color: #3370ff; background: transparent; cursor: pointer; }.rule-list-card footer .danger { color: #d83931; }
.rule-dialog { width: min(820px, 100%); }.weekday-field { display: flex; flex-wrap: wrap; gap: 12px; padding: 13px; border: 1px solid #e5e8ed; border-radius: 10px; }.weekday-field legend { padding: 0 5px; color: #50555d; font-size: 12px; font-weight: 600; }.weekday-field label, .rule-switches label { flex-direction: row; align-items: center; gap: 5px; font-weight: 400; }.weekday-field input, .rule-switches input { width: auto; height: auto; }.rule-switches { display: flex; gap: 22px; padding: 13px; border-radius: 10px; background: #f7f8fa; }
.rule-subsection { padding: 15px; border: 1px solid #e8ebf0; border-radius: 11px; }.rule-subsection header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; }.rule-subsection header strong, .rule-subsection header span { display: block; }.rule-subsection header span { margin-top: 4px; color: #8f959e; font-size: 9px; }.rule-subsection header button, .wifi-form-row button, .location-form-row button { border: 0; color: #3370ff; background: transparent; cursor: pointer; }.wifi-form-row { display: grid; grid-template-columns: 1fr 1fr auto; gap: 8px; margin-top: 8px; }.location-form-row { display: grid; grid-template-columns: 1.2fr 1fr 1fr .8fr auto; gap: 8px; margin-top: 8px; }.wifi-form-row button, .location-form-row button { color: #d83931; }
@media (max-width: 1120px) { .rule-grid { grid-template-columns: repeat(2, 1fr); } }
@media (max-width: 760px) { .rule-management-page { padding: 25px 17px 45px; }.rule-summary-row, .rule-grid { grid-template-columns: 1fr; }.rule-toolbar { overflow-x: auto; }.location-form-row, .wifi-form-row { grid-template-columns: 1fr; }.rule-switches { flex-wrap: wrap; } }
</style>
