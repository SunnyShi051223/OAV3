<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { applicationApi } from '../api/oa'
import ListPagination from './ListPagination.vue'

const props = defineProps({ permissions: { type: Array, default: () => [] }, initialType: { type: String, default: '' } })
const emit = defineEmits(['back', 'preset-consumed'])

const tab = ref('mine')
const myApplications = ref([])
const pendingApplications = ref([])
const handledApplications = ref([])
const applicationTypes = ref([])
const makeupOptions = ref([])
const loading = ref(false)
const error = ref('')
const formDialog = ref({ visible: false, form: {}, error: '', submitting: false })
const detailDialog = ref({ visible: false, data: null, loading: false })
const decisionDialog = ref({ visible: false, action: 'approve', item: null, comment: '', error: '', submitting: false })
const currentPage = ref(1)
const pageSize = 6

const canHandle = computed(() => props.permissions.includes('approval:handle'))
const canSubmit = computed(() => props.permissions.includes('approval:submit'))
const tabs = computed(() => [
  { value: 'mine', label: '我的申请', count: myApplications.value.length },
  ...(canHandle.value ? [
    { value: 'pending', label: '待我审批', count: pendingApplications.value.length },
    { value: 'handled', label: '已处理', count: handledApplications.value.length },
  ] : []),
])
const visibleApplications = computed(() => tab.value === 'pending' ? pendingApplications.value : tab.value === 'handled' ? handledApplications.value : myApplications.value)
const paginatedApplications = computed(() => visibleApplications.value.slice((currentPage.value - 1) * pageSize, currentPage.value * pageSize))
const leaveDuration = computed(() => {
  const form = formDialog.value.form
  if (form.applicationType !== 'LEAVE' || !form.startTime || !form.endTime) return { valid: true, text: '选择开始时间和结束时间后自动计算' }
  const start = new Date(form.startTime).getTime()
  const end = new Date(form.endTime).getTime()
  if (!Number.isFinite(start) || !Number.isFinite(end) || end <= start) return { valid: false, text: '时间范围无效' }
  const hours = (end - start) / 3600000
  const days = hours / 24
  const formatNumber = (value) => Number(value.toFixed(2)).toString()
  return { valid: true, text: `${formatNumber(days)} 天（${formatNumber(hours)} 小时）` }
})

async function load() {
  loading.value = true
  error.value = ''
  try {
    const requests = [applicationApi.mine(), applicationApi.types(), applicationApi.makeupOptions()]
    if (canHandle.value) requests.push(applicationApi.pending(), applicationApi.handled())
    const [mineResult, typeResult, makeupResult, pendingResult, handledResult] = await Promise.all(requests)
    myApplications.value = mineResult.data || []
    applicationTypes.value = typeResult.data || []
    makeupOptions.value = makeupResult.data || []
    pendingApplications.value = pendingResult?.data || []
    handledApplications.value = handledResult?.data || []
  } catch (requestError) {
    error.value = requestError.message
  } finally {
    loading.value = false
  }
}

function openForm(type = '') {
  formDialog.value = { visible: true, form: { applicationType: type, attendanceRecordId: null, startTime: '', endTime: '', reason: '', remark: '' }, error: '', submitting: false }
  if (type === 'MAKEUP') selectMakeupRecord()
}

function changeType() {
  if (formDialog.value.form.applicationType !== 'MAKEUP') formDialog.value.form.attendanceRecordId = null
  formDialog.value.form.startTime = ''
  formDialog.value.form.endTime = ''
}

function selectMakeupRecord() {
  const selected = makeupOptions.value.find((item) => String(item.recordId) === String(formDialog.value.form.attendanceRecordId))
  if (!selected) return
  formDialog.value.form.startTime = dateTimeInput(selected.suggestedStartTime)
  formDialog.value.form.endTime = dateTimeInput(selected.suggestedEndTime)
}

async function submitForm() {
  const state = formDialog.value
  state.error = ''
  if (state.form.applicationType === 'LEAVE' && !leaveDuration.value.valid) {
    state.error = '结束时间必须晚于开始时间'
    return
  }
  state.submitting = true
  try {
    await applicationApi.submit({
      ...state.form,
      attendanceRecordId: state.form.applicationType === 'MAKEUP' ? Number(state.form.attendanceRecordId) : null,
      startTime: state.form.startTime ? `${state.form.startTime}:00` : null,
      endTime: state.form.endTime ? `${state.form.endTime}:00` : null,
    })
    state.visible = false
    tab.value = 'mine'
    await load()
  } catch (requestError) {
    state.error = requestError.message
  } finally {
    state.submitting = false
  }
}

async function cancel(item) {
  if (!window.confirm(`确定撤回“${item.applicationTypeLabel}”申请吗？`)) return
  try {
    await applicationApi.cancel(item.applicationId)
    await load()
  } catch (requestError) {
    error.value = requestError.message
  }
}

async function openDetail(item) {
  detailDialog.value = { visible: true, data: null, loading: true }
  try {
    const result = await applicationApi.detail(item.applicationId)
    detailDialog.value.data = result.data
  } catch (requestError) {
    error.value = requestError.message
    detailDialog.value.visible = false
  } finally {
    detailDialog.value.loading = false
  }
}

function openDecision(item, action) {
  decisionDialog.value = { visible: true, action, item, comment: '', error: '', submitting: false }
}

async function submitDecision() {
  const state = decisionDialog.value
  if (state.action === 'reject' && !state.comment.trim()) {
    state.error = '驳回时请填写审批意见'
    return
  }
  state.submitting = true
  state.error = ''
  try {
    if (state.action === 'approve') await applicationApi.approve(state.item.taskId, state.comment)
    else await applicationApi.reject(state.item.taskId, state.comment)
    state.visible = false
    await load()
  } catch (requestError) {
    state.error = requestError.message
  } finally {
    state.submitting = false
  }
}

function dateTimeInput(value) { return value ? String(value).replace(' ', 'T').slice(0, 16) : '' }
function formatTime(value) { return value ? String(value).replace('T', ' ').slice(0, 16) : '—' }
function initial(name) { return String(name || '同').slice(0, 1) }
function makeupOptionText(item) { return `${item.attendanceDate} · ${item.ruleName || '考勤'} · ${item.statusLabel || '异常'}` }
function statusClass(status) { return String(status || 'PENDING').toLowerCase() }
function decisionLabel(decision) { return ({ APPROVE: '通过', REJECT: '驳回', SUBMIT: '提交' })[decision] || decision || '处理中' }
function shortText(value, length = 24) { return String(value || '—').length > length ? `${String(value).slice(0, length)}…` : String(value || '—') }

async function consumePreset(type) {
  if (!type) return
  if (!applicationTypes.value.length) await load()
  openForm(type)
  emit('preset-consumed')
}

watch(() => props.initialType, consumePreset)
watch(tab, () => { currentPage.value = 1 })
watch(() => visibleApplications.value.length, (total) => {
  currentPage.value = Math.min(currentPage.value, Math.max(1, Math.ceil(total / pageSize)))
})
onMounted(async () => {
  await load()
  await consumePreset(props.initialType)
})
</script>

<template>
  <section class="approval-page">
    <div class="management-heading approval-heading">
      <div><button class="back-button" @click="$emit('back')">← 返回工作台</button><h1>审批中心</h1><p>提交申请并查看审批进度</p></div>
      <div class="approval-heading-actions"><button class="refresh-button" :disabled="loading" @click="load"><span>↻</span>{{ loading ? '刷新中' : '刷新' }}</button><button v-if="canSubmit" class="primary-button" @click="openForm()">＋ 新建申请</button></div>
    </div>
    <div class="approval-tabs"><button v-for="item in tabs" :key="item.value" :class="{ active: tab === item.value }" @click="tab = item.value">{{ item.label }}<span>{{ item.count }}</span></button></div>
    <div v-if="error" class="api-error">{{ error }}</div>
    <div class="data-panel approval-panel">
      <div v-if="loading" class="empty-state">正在加载审批数据…</div>
      <table v-else><thead><tr><th v-if="tab !== 'mine'">申请人</th><th v-if="tab !== 'mine'">部门</th><th>申请类型</th><th>时间范围</th><th>申请原因</th><th>状态</th><th>提交时间</th><th>操作</th></tr></thead><tbody>
        <tr v-for="item in paginatedApplications" :key="item.applicationId">
          <td v-if="tab !== 'mine'" class="name-cell"><span class="table-avatar">{{ initial(item.applicantName) }}</span>{{ item.applicantName }}</td><td v-if="tab !== 'mine'">{{ item.applicantDeptName || '—' }}</td>
          <td class="name-cell">{{ item.applicationTypeLabel || item.applicationType }}</td><td><div class="time-range"><span>{{ formatTime(item.startTime) }}</span><small>至 {{ formatTime(item.endTime) }}</small></div></td><td class="reason-cell" :title="item.reason">{{ shortText(item.reason) }}</td><td><span :class="['approval-status', statusClass(item.status)]">{{ item.statusLabel || item.currentTaskName || '处理中' }}</span></td><td>{{ formatTime(item.createTime) }}</td>
          <td class="actions-cell"><button class="text-action" @click="openDetail(item)">详情</button><template v-if="tab === 'mine'"><button v-if="item.canCancel" class="text-action danger" @click="cancel(item)">撤回</button></template><template v-else-if="tab === 'pending'"><button class="text-action success" @click="openDecision(item, 'approve')">通过</button><button class="text-action danger" @click="openDecision(item, 'reject')">驳回</button></template></td>
        </tr>
      </tbody></table>
      <ListPagination v-if="!loading" v-model:page="currentPage" :total="visibleApplications.length" :page-size="pageSize" />
      <div v-if="!loading && !visibleApplications.length" class="approval-empty"><span>✓</span><strong>{{ tab === 'pending' ? '暂无待审批事项' : tab === 'handled' ? '暂无已处理记录' : '还没有申请记录' }}</strong><p v-if="tab === 'mine'">点击“新建申请”提交请假、加班或补卡申请。</p></div>
    </div>
  </section>

  <div v-if="formDialog.visible" class="dialog-mask" @click.self="formDialog.visible = false">
    <form class="data-dialog application-form-dialog" @submit.prevent="submitForm">
      <div class="dialog-heading"><div><h2>新建申请</h2><p>提交后将进入部门主管审批流程</p></div><button type="button" @click="formDialog.visible = false">×</button></div>
      <div class="form-grid">
        <label class="full-field">申请类型<select v-model="formDialog.form.applicationType" required @change="changeType"><option value="" disabled>请选择申请类型</option><option v-for="item in applicationTypes" :key="item.value" :value="item.value">{{ item.label }}</option></select></label>
        <label v-if="formDialog.form.applicationType === 'MAKEUP'" class="full-field">异常考勤记录<select v-model="formDialog.form.attendanceRecordId" required @change="selectMakeupRecord"><option :value="null" disabled>请选择需要补卡的记录</option><option v-for="item in makeupOptions" :key="item.recordId" :value="item.recordId">{{ makeupOptionText(item) }}</option></select><small class="field-tip">{{ makeupOptions.length ? '只能选择本人尚未申请补卡的异常记录' : '当前没有可补卡的缺勤、迟到、早退或缺卡记录' }}</small></label>
        <label>{{ formDialog.form.applicationType === 'MAKEUP' ? '补卡签到时间' : '开始时间' }}<input v-model="formDialog.form.startTime" type="datetime-local" required /></label><label>{{ formDialog.form.applicationType === 'MAKEUP' ? '补卡签退时间' : '结束时间' }}<input v-model="formDialog.form.endTime" type="datetime-local" :min="formDialog.form.startTime || undefined" required /></label>
        <label v-if="formDialog.form.applicationType === 'LEAVE'" class="full-field">请假天数<input class="leave-duration-input" :class="{ invalid: !leaveDuration.valid }" :value="leaveDuration.text" readonly tabindex="-1" /></label>
        <label class="full-field">申请原因<textarea v-model.trim="formDialog.form.reason" maxlength="1000" placeholder="请详细说明申请原因" required></textarea></label><label class="full-field">备注<textarea v-model.trim="formDialog.form.remark" maxlength="1000" placeholder="选填"></textarea></label>
      </div>
      <div v-if="formDialog.error" class="form-error">{{ formDialog.error }}</div><div class="dialog-actions"><button type="button" @click="formDialog.visible = false">取消</button><button class="primary-button" :disabled="formDialog.submitting || (formDialog.form.applicationType === 'MAKEUP' && !makeupOptions.length)">{{ formDialog.submitting ? '提交中…' : '提交申请' }}</button></div>
    </form>
  </div>

  <div v-if="detailDialog.visible" class="dialog-mask" @click.self="detailDialog.visible = false"><div class="data-dialog application-detail-dialog">
    <div class="dialog-heading"><div><h2>申请详情</h2><p>申请内容与审批记录</p></div><button type="button" @click="detailDialog.visible = false">×</button></div>
    <div v-if="detailDialog.loading" class="empty-state">正在加载详情…</div>
    <template v-else-if="detailDialog.data"><div class="application-detail-grid"><div><span>申请类型</span><strong>{{ detailDialog.data.applicationTypeLabel }}</strong></div><div><span>当前状态</span><strong><i :class="['approval-status', statusClass(detailDialog.data.status)]">{{ detailDialog.data.statusLabel }}</i></strong></div><div><span>申请人</span><strong>{{ detailDialog.data.applicantName }}</strong></div><div><span>所属部门</span><strong>{{ detailDialog.data.applicantDeptName || '—' }}</strong></div><div><span>开始时间</span><strong>{{ formatTime(detailDialog.data.startTime) }}</strong></div><div><span>结束时间</span><strong>{{ formatTime(detailDialog.data.endTime) }}</strong></div><div class="detail-wide"><span>申请原因</span><p>{{ detailDialog.data.reason }}</p></div><div v-if="detailDialog.data.remark" class="detail-wide"><span>备注</span><p>{{ detailDialog.data.remark }}</p></div></div>
      <section v-if="detailDialog.data.approvalHistory?.length" class="approval-history"><h3>审批记录</h3><div v-for="(history, index) in detailDialog.data.approvalHistory" :key="index" class="history-item"><span class="history-dot"></span><div><strong>{{ history.taskName }} · {{ decisionLabel(history.decision) }}</strong><p>{{ history.approverName || history.approverUsername || '待处理' }}<template v-if="history.comment">：{{ history.comment }}</template></p><small>{{ formatTime(history.endTime || history.startTime) }}</small></div></div></section>
    </template>
  </div></div>

  <div v-if="decisionDialog.visible" class="dialog-mask" @click.self="decisionDialog.visible = false"><form class="data-dialog decision-dialog" @submit.prevent="submitDecision">
    <div class="dialog-heading"><div><h2>{{ decisionDialog.action === 'approve' ? '通过申请' : '驳回申请' }}</h2><p>{{ decisionDialog.item?.applicantName }} · {{ decisionDialog.item?.applicationTypeLabel }}</p></div><button type="button" @click="decisionDialog.visible = false">×</button></div>
    <div class="decision-summary"><span>申请时间</span><strong>{{ formatTime(decisionDialog.item?.startTime) }} 至 {{ formatTime(decisionDialog.item?.endTime) }}</strong><span>申请原因</span><p>{{ decisionDialog.item?.reason }}</p></div>
    <label>审批意见<textarea v-model.trim="decisionDialog.comment" maxlength="500" :placeholder="decisionDialog.action === 'reject' ? '驳回时必须填写原因' : '选填'"></textarea></label>
    <div v-if="decisionDialog.error" class="form-error">{{ decisionDialog.error }}</div><div class="dialog-actions"><button type="button" @click="decisionDialog.visible = false">取消</button><button :class="['primary-button', { 'danger-submit': decisionDialog.action === 'reject' }]" :disabled="decisionDialog.submitting">{{ decisionDialog.submitting ? '处理中…' : decisionDialog.action === 'approve' ? '确认通过' : '确认驳回' }}</button></div>
  </form></div>
</template>

<style scoped>
.leave-duration-input { border-color: #e2e5e9 !important; color: #646a73 !important; background: #f2f3f5 !important; box-shadow: none !important; cursor: not-allowed; }
.leave-duration-input.invalid { border-color: #f1b5b1 !important; color: #d83931 !important; }
</style>
