<script setup>
import { computed, ref, onMounted } from 'vue'
import { attendanceApi, userApi } from '../api/oa'
import request from '../api/request'

const props = defineProps({ permissions: { type: Array, default: () => [] }, currentDeptId: { type: Number, default: null } })
const emit = defineEmits(['back'])

const permissionSet = computed(() => new Set(props.permissions))
const loading = ref(false)
const error = ref('')
const notice = ref('')
const records = ref([])
const users = ref([])
const searched = ref(false)
const selectedMonth = ref(new Date().toISOString().slice(0, 7))
const userIdFilter = ref(null)
const editDialog = ref({ visible: false, record: null, form: {}, error: '', saving: false })

onMounted(load)

async function load() {
  loading.value = true; error.value = ''
  try {
    const [userResult] = await Promise.all([userApi.list()])
    users.value = userResult.data || []
  } catch (e) { error.value = e.message } finally { loading.value = false }
}

async function search() {
  if (userIdFilter.value == null) { error.value = '请选择员工'; return }
  loading.value = true; error.value = ''; searched.value = true
  try {
    const result = await request.get('/attendance/user-month', { params: { userId: userIdFilter.value, month: selectedMonth.value } })
    records.value = (result.data?.calendar || []).filter(r => r.recordId)
  } catch (e) { error.value = e.message; records.value = [] } finally { loading.value = false }
}

function openEdit(record) {
  editDialog.value = {
    visible: true,
    record,
    form: {
      recordId: record.recordId,
      attendanceStatus: record.attendanceStatus || '',
      checkInTime: record.checkInTime ? String(record.checkInTime).slice(0, 16) : '',
      checkOutTime: record.checkOutTime ? String(record.checkOutTime).slice(0, 16) : '',
      workMinutes: record.workMinutes || null,
      overtimeMinutes: record.overtimeMinutes || null,
      remark: '',
    },
    error: '',
    saving: false,
  }
}

async function saveModify() {
  const state = editDialog.value
  state.error = ''; state.saving = true
  try {
    const f = state.form
    await request.put('/attendance/records/modify', {
      recordId: f.recordId,
      attendanceStatus: f.attendanceStatus || null,
      checkInTime: f.checkInTime ? f.checkInTime + ':00' : null,
      checkOutTime: f.checkOutTime ? f.checkOutTime + ':00' : null,
      workMinutes: f.workMinutes ? Number(f.workMinutes) : null,
      overtimeMinutes: f.overtimeMinutes ? Number(f.overtimeMinutes) : null,
      remark: f.remark || null,
    })
    state.visible = false
    showNotice('考勤记录修改成功')
    search()
  } catch (e) { state.error = e.message } finally { state.saving = false }
}

function showNotice(msg) { notice.value = msg; setTimeout(() => { if (notice.value === msg) notice.value = '' }, 2500) }
function formatTime(v) { return v ? String(v).replace('T', ' ').slice(11, 16) : '—' }
function statusLabel(s) { return { NORMAL: '正常', LATE: '迟到', EARLY: '早退', ABSENT: '缺勤', LEAVE: '请假', OVERTIME: '加班' }[s] || s || '—' }
</script>

<template>
  <section class="rule-management-page">
    <div class="management-heading">
      <div><button class="back-button" @click="emit('back')">← 返回工作台</button><h1>考勤记录管理</h1><p>管理员可在此查询并修改员工的考勤记录</p></div>
    </div>

    <div class="rule-toolbar">
      <select v-model="userIdFilter" style="min-width:180px">
        <option :value="null">请选择员工</option>
        <option v-for="u in users" :key="u.userId" :value="u.userId">{{ u.realName }}（{{ u.deptName || '-' }}）</option>
      </select>
      <input v-model="selectedMonth" type="month" />
      <button :disabled="loading || userIdFilter == null" @click="search">{{ loading ? '查询中' : '查询' }}</button>
    </div>

    <div v-if="notice" class="attendance-notice">✓ {{ notice }}</div>
    <div v-if="error" class="api-error">{{ error }}</div>

    <div v-if="records.length" class="data-panel">
      <table>
        <thead><tr><th>日期</th><th>签到</th><th>签退</th><th>状态</th><th>工时(min)</th><th>加班(min)</th><th>操作</th></tr></thead>
        <tbody>
          <tr v-for="r in records" :key="r.recordId">
            <td>{{ r.attendanceDate }}</td>
            <td>{{ formatTime(r.checkInTime) }}</td>
            <td>{{ formatTime(r.checkOutTime) }}</td>
            <td><span :class="['approval-status', r.attendanceStatus === 'NORMAL' ? 'approved' : r.attendanceStatus === 'LATE' ? 'processing' : 'rejected']">{{ statusLabel(r.attendanceStatus) }}</span></td>
            <td>{{ r.workMinutes || 0 }}</td>
            <td>{{ r.overtimeMinutes || 0 }}</td>
            <td><button class="text-action" @click="openEdit(r)">修改</button></td>
          </tr>
        </tbody>
      </table>
    </div>
    <div v-else-if="searched && !loading" class="approval-empty"><span>勤</span><strong>该员工未有考勤记录</strong><p>{{ selectedMonth }} 暂无该员工的考勤数据。</p></div>
    <div v-else-if="!loading" class="approval-empty"><span>勤</span><strong>请选择员工和月份后查询</strong><p>选择员工后点击查询即可查看考勤记录。</p></div>

    <!-- Edit Dialog -->
    <div v-if="editDialog.visible" class="dialog-mask" @click.self="editDialog.visible = false">
      <form class="data-dialog" @submit.prevent="saveModify">
        <div class="dialog-heading">
          <div><h2>修改考勤记录</h2><p>{{ editDialog.record?.attendanceDate }} · {{ editDialog.record?.realName || users.find(u => Number(u.userId) === Number(userIdFilter))?.realName || '员工' }}</p></div>
          <button type="button" @click="editDialog.visible = false">×</button>
        </div>
        <div class="form-grid">
          <label>考勤状态
            <select v-model="editDialog.form.attendanceStatus">
              <option value="">不修改</option>
              <option value="NORMAL">正常</option>
              <option value="LATE">迟到</option>
              <option value="EARLY">早退</option>
              <option value="ABSENT">缺勤</option>
              <option value="LEAVE">请假</option>
              <option value="OVERTIME">加班</option>
            </select>
          </label>
          <label>签到时间<input v-model="editDialog.form.checkInTime" type="datetime-local" /></label>
          <label>签退时间<input v-model="editDialog.form.checkOutTime" type="datetime-local" /></label>
          <label>工作时长(分钟)<input v-model.number="editDialog.form.workMinutes" type="number" /></label>
          <label>加班时长(分钟)<input v-model.number="editDialog.form.overtimeMinutes" type="number" /></label>
          <label class="full-field">修改原因<textarea v-model.trim="editDialog.form.remark" placeholder="请填写修改原因" required></textarea></label>
        </div>
        <div v-if="editDialog.error" class="form-error">{{ editDialog.error }}</div>
        <div class="dialog-actions">
          <button type="button" @click="editDialog.visible = false">取消</button>
          <button class="primary-button" :disabled="editDialog.saving || !editDialog.form.remark.trim()">{{ editDialog.saving ? '保存中' : '确认修改' }}</button>
        </div>
      </form>
    </div>
  </section>
</template>

<style scoped>
.rule-management-page { width: min(1500px, 100%); margin: 0 auto; padding: 36px 42px 60px; }
.rule-toolbar { display: flex; gap: 9px; margin-bottom: 14px; align-items: center; }
.rule-toolbar select, .rule-toolbar input, .rule-toolbar button { height: 38px; padding: 0 13px; border: 1px solid #dfe3e9; border-radius: 9px; background: #fff; }
.rule-toolbar button { color: #3370ff; cursor: pointer; }
.data-panel { background: #fff; border-radius: 14px; border: 1px solid #e8ebf0; overflow: hidden; }
.data-panel table { width: 100%; border-collapse: collapse; }
.data-panel th, .data-panel td { padding: 11px 14px; border-bottom: 1px solid #f0f1f3; text-align: left; font-size: 13px; }
.data-panel th { color: #8f959e; font-size: 11px; font-weight: 600; background: #fafbfc; }
.text-action { border: 0; color: #3370ff; background: transparent; cursor: pointer; font-size: 13px; }
</style>
